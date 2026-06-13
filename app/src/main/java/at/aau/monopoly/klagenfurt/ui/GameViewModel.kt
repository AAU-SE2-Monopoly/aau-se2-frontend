package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.PendingPayment
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.PaymentSource
import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.OwnableField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import at.aau.monopoly.klagenfurt.ui.board.MovementAnimationState
import at.aau.monopoly.klagenfurt.ui.board.computeBackwardMovementPath
import at.aau.monopoly.klagenfurt.ui.board.computeDirectMovementPath
import at.aau.monopoly.klagenfurt.ui.board.computeMovementPath
import at.aau.monopoly.klagenfurt.ui.util.ownerIdFromField
import at.aau.monopoly.klagenfurt.ui.util.toManageableProperty
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameService: GameService,
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    data class LogEntry(
        val text: String,
        val eventType: String,
        val isTechnical: Boolean = false,
        val timestampMs: Long = System.currentTimeMillis()
    )

    internal data class LogAccumulator(
        val gameId: String,
        val entries: List<LogEntry>
    )

    enum class TurnPresentationPhase {
        IDLE,
        WAITING_FOR_ROLL_INPUT,
        ROLLING_DICE,
        SHOWING_DICE_RESULT,
        MOVING_TOKEN,
        REVEALING_LANDING_EFFECT,
        READY_FOR_ACTION
    }

    sealed class PresentationTask {
        data class ShowDice(val sequenceId: Long, val roll: DiceRoll) : PresentationTask()
        data class MoveToken(
            val sequenceId: Long,
            val playerId: String,
            val path: List<Int>
        ) : PresentationTask()

        data class RevealLanding(val sequenceId: Long, val event: GameEvent?) : PresentationTask()
        data class RevealLog(val sequenceId: Long, val entries: List<LogEntry>) : PresentationTask()
        data class SyncSnapshot(val sequenceId: Long) : PresentationTask()
    }

    data class ActiveDicePresentation(
        val sequenceId: Long,
        val diceRoll: DiceRoll? = null,
        val isRolling: Boolean = false
    )

    data class VisiblePaymentState(
        val pendingPayment: PendingPayment,
        val amount: Int = pendingPayment.amount,
        val source: PaymentSource = pendingPayment.source,
        val sourceFieldId: Int? = pendingPayment.sourceFieldId,
        val creditorPlayerId: String? = pendingPayment.creditorPlayerId
    )

    data class VisibleBankruptcyState(
        val playerId: String,
        val playerName: String,
        val totalAssets: Int,
        val totalDebt: Int,
        val propertiesOwned: List<ManageableProperty>
    )

    data class ActionGates(
        val canRollDice: Boolean = false,
        val canActivateCheat: Boolean = false,
        val canEndTurn: Boolean = false,
        val canBuyProperty: Boolean = false,
        val canDrawCard: Boolean = false,
        val canDrawChance: Boolean = false,
        val canDrawCommunityChest: Boolean = false,
        val canExecuteCard: Boolean = false,
        val canPayRent: Boolean = false,
        val canPayTax: Boolean = false,
        val canUseJailAction: Boolean = false,
        val canManageProperties: Boolean = false,
        val canDeclareBankruptcy: Boolean = false,
        val canConfirmDeclareBankruptcy: Boolean = false,
        val canTrade: Boolean = false,
        val canReportCheater: Boolean = false
    )

    private data class ActionGateBase(
        val state: GameState?,
        val phase: TurnPresentationPhase,
        val visiblePayment: VisiblePaymentState?,
        val visibleActionCard: Card?,
        val visibleBankruptcy: VisibleBankruptcyState?,
        val visibleCurrentField: Field?
    )

    private data class ActionGateLocks(
        val paymentInFlight: Boolean,
        val propertyInFlight: Boolean,
        val bankruptcyConfirm: Boolean,
        val buildingPending: Boolean,
        val reportCheaterInFlight: Boolean
    )

    private data class PresentationPath(
        val playerId: String,
        val startPosition: Int,
        val path: List<Int>,
        val hardSyncOnly: Boolean = false
    )

    private val objectMapper = JacksonProvider.objectMapper

    private val gameEventFlow: SharedFlow<GameEvent> = gameService.events
        .mapNotNull { jsonString ->
            try {
                objectMapper.readValue(jsonString, GameEvent::class.java)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Parsing error: ${e.message}", e)
                null
            }
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    private val logEventFlow: SharedFlow<GameEvent> = gameService.logEvents
        .mapNotNull { jsonString ->
            try {
                objectMapper.readValue(jsonString, GameEvent::class.java)
            } catch (e: Exception) {
                Log.e("GameViewModel", "logEventFlow parse error: ${e.message}", e)
                null
            }
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 80
        )

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _dramaEvent = MutableSharedFlow<String>()
    val dramaEvent: SharedFlow<String> = _dramaEvent.asSharedFlow()

    private var errorToken: Long = 0

    private fun showTransientError(message: String) {
        val token = ++errorToken
        _errorMessage.value = message
        viewModelScope.launch {
            delay(ERROR_DISPLAY_MS)
            if (errorToken == token) {
                _errorMessage.value = null
            }
        }
    }

    private val _paymentActionInFlight = MutableStateFlow(false)
    val paymentActionInFlight: StateFlow<Boolean> = _paymentActionInFlight.asStateFlow()

    private val _propertyActionInFlight = MutableStateFlow(false)
    val propertyActionInFlight: StateFlow<Boolean> = _propertyActionInFlight.asStateFlow()

    private val _reportCheaterInFlight = MutableStateFlow(false)
    val reportCheaterInFlight: StateFlow<Boolean> = _reportCheaterInFlight.asStateFlow()

    private var paymentActionToken: Long = 0
    private var propertyActionToken: Long = 0
    private var reportCheaterActionToken: Long = 0

    private fun startPaymentAction() {
        val token = ++paymentActionToken
        _paymentActionInFlight.value = true
        viewModelScope.launch {
            delay(ACTION_TIMEOUT_MS)
            if (paymentActionToken == token) {
                _paymentActionInFlight.value = false
            }
        }
    }

    private fun finishPaymentAction() {
        _paymentActionInFlight.value = false
    }

    private fun startPropertyAction() {
        val token = ++propertyActionToken
        _propertyActionInFlight.value = true
        viewModelScope.launch {
            delay(ACTION_TIMEOUT_MS)
            if (propertyActionToken == token) {
                _propertyActionInFlight.value = false
                _buildingActionPending.value = false
            }
        }
    }

    private fun finishPropertyAction() {
        _propertyActionInFlight.value = false
    }

    private fun startReportCheaterAction() {
        val token = ++reportCheaterActionToken
        _reportCheaterInFlight.value = true
        viewModelScope.launch {
            delay(ACTION_TIMEOUT_MS)
            if (reportCheaterActionToken == token) {
                _reportCheaterInFlight.value = false
            }
        }
    }

    private fun finishReportCheaterAction() {
        _reportCheaterInFlight.value = false
    }

    private val _currentActionCard = MutableStateFlow<Card?>(null)
    val currentActionCard: StateFlow<Card?> get() = _visibleActionCard.asStateFlow()

    private val _isExecutingAction = MutableStateFlow(false)
    val isExecutingAction: StateFlow<Boolean> = _isExecutingAction.asStateFlow()

    private val _selectedPlayerForOverlay = MutableStateFlow<Player?>(null)
    val selectedPlayerForOverlay: StateFlow<Player?> = _selectedPlayerForOverlay.asStateFlow()

    private val _selectedPlayerForTrade = MutableStateFlow<Player?>(null)
    val selectedPlayerForTrade: StateFlow<Player?> = _selectedPlayerForTrade.asStateFlow()



    private val _presentationPhase = MutableStateFlow(TurnPresentationPhase.IDLE)
    val presentationPhase: StateFlow<TurnPresentationPhase> = _presentationPhase.asStateFlow()

    private val _presentedBoardPlayers = MutableStateFlow<List<Player>>(emptyList())
    val presentedBoardPlayers: StateFlow<List<Player>> = _presentedBoardPlayers.asStateFlow()

    private val _presentedEventLog = MutableStateFlow<List<LogEntry>>(emptyList())
    val presentedEventLog: StateFlow<List<LogEntry>> = _presentedEventLog.asStateFlow()

    private val _visibleCurrentField = MutableStateFlow<Field?>(null)
    val visibleCurrentField: StateFlow<Field?> = _visibleCurrentField.asStateFlow()

    private val _visibleActionCard = MutableStateFlow<Card?>(null)
    val visibleActionCard: StateFlow<Card?> = _visibleActionCard.asStateFlow()

    private val _visiblePaymentState = MutableStateFlow<VisiblePaymentState?>(null)
    val visiblePaymentState: StateFlow<VisiblePaymentState?> = _visiblePaymentState.asStateFlow()

    private val _visibleBankruptcyState = MutableStateFlow<VisibleBankruptcyState?>(null)
    val visibleBankruptcyState: StateFlow<VisibleBankruptcyState?> = _visibleBankruptcyState.asStateFlow()

    private val _activeDicePresentation = MutableStateFlow<ActiveDicePresentation?>(null)
    val activeDicePresentation: StateFlow<ActiveDicePresentation?> = _activeDicePresentation.asStateFlow()

    private val _showGameOverOverlay = MutableStateFlow(false)
    val showGameOverOverlay: StateFlow<Boolean> = _showGameOverOverlay.asStateFlow()

    private val _hostEndedGame = MutableStateFlow(false)
    val hostEndedGame: StateFlow<Boolean> = _hostEndedGame.asStateFlow()

    private var lastCurrentPlayerIdForCardDraw: String? = null

    private val _buildingActionPending = MutableStateFlow(false)
    val buildingActionPending: StateFlow<Boolean> = _buildingActionPending.asStateFlow()

    private var rollRequestInFlight = false
    private var rollActionToken: Long = 0

    val gameState: StateFlow<GameState?> = gameEventFlow
        .runningFold<GameEvent, GameState?>(null) { lastState, event ->
            val eventGameId = event.gameId

            val isDifferentGame = eventGameId.isNotBlank() &&
                    gameService.currentGameId.isNotBlank() &&
                    eventGameId != gameService.currentGameId

            if (isDifferentGame) {
                lastState
            } else {
                event.gameState ?: lastState
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val fields: StateFlow<List<Field>> = gameState
        .map { it?.fields ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isGameReady: StateFlow<Boolean> = gameState
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canStartGame: StateFlow<Boolean> = gameState
        .map { state ->
            val players = state?.players.orEmpty()
            val isHost = players.firstOrNull()?.id == gameService.currentPlayerId
            val isWaiting = state?.phase == GamePhase.WAITING

            isHost && isWaiting && players.size >= 2
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val eventLog: StateFlow<List<LogEntry>> = logEventFlow
        .runningFold(LogAccumulator(gameId = "", entries = emptyList())) { acc, event ->
            val eventGameId = event.gameId
            val incomingGameId = when {
                eventGameId.isNotBlank() -> eventGameId
                gameService.currentGameId.isNotBlank() -> gameService.currentGameId
                else -> acc.gameId
            }

            val shouldIgnore =
                event.event == "ERROR" ||
                        (event.event != "GAME_CREATED" &&
                                incomingGameId.isNotBlank() &&
                                gameService.currentGameId.isNotBlank() &&
                                incomingGameId != gameService.currentGameId)

            if (shouldIgnore) {
                if (event.event == "ERROR") {
                    Log.w("GameViewModel", "Server ERROR [game=${event.gameId}]: ${event.message}")
                }
                acc
            } else {
                val isGameSwitch =
                    incomingGameId.isNotBlank() &&
                            acc.gameId.isNotBlank() &&
                            incomingGameId != acc.gameId

                val baseEntries =
                    if (isGameSwitch || event.event == "GAME_CREATED") {
                        emptyList()
                    } else {
                        acc.entries
                    }

                val isTechnical =
                    event.event == "STATE_SNAPSHOT" ||
                            event.event == "STATE_UPDATED"

                val entryText =
                    event.message?.takeIf { it.isNotBlank() }
                        ?: humanReadableEvent(event.event, event.gameId)

                if (entryText.isBlank()) {
                    LogAccumulator(gameId = incomingGameId, entries = baseEntries)
                } else {
                    val entry = LogEntry(
                        text = entryText,
                        eventType = event.event.ifBlank { "UNKNOWN" },
                        isTechnical = isTechnical,
                        timestampMs = currentTimeProvider()
                    )

                    LogAccumulator(
                        gameId = incomingGameId,
                        entries = (baseEntries + entry).takeLast(MAX_LOG_ENTRIES)
                    )
                }
            }
        }
        .map { it.entries }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastDiceRoll: StateFlow<DiceRoll?> = gameState
        .map { it?.lastDiceRoll }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isRollingPhaseForCurrentPlayer: StateFlow<Boolean> = gameState
        .map { state ->
            state?.phase == GamePhase.ROLLING &&
                    state.currentPlayer?.id == gameService.currentPlayerId
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBuyingPhaseForCurrentPlayer: StateFlow<Boolean> = gameState
        .map { state ->
            state?.phase == GamePhase.BUYING &&
                    state.currentPlayer?.id == gameService.currentPlayerId
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showPayRentOverlay = MutableStateFlow(false)
    val showPayRentOverlay: StateFlow<Boolean> = _showPayRentOverlay.asStateFlow()

    private val _hasPendingPayment = MutableStateFlow(false)
    val hasPendingPayment: StateFlow<Boolean> = _hasPendingPayment.asStateFlow()

    private val _showBankruptcyOverlay = MutableStateFlow(false)
    val showBankruptcyOverlay: StateFlow<Boolean> = _showBankruptcyOverlay.asStateFlow()
    private val _showBankruptcyConfirmation = MutableStateFlow(false)
    val showBankruptcyConfirmation: StateFlow<Boolean> = _showBankruptcyConfirmation.asStateFlow()

    private val actionGateVisuals: StateFlow<ActionGateBase> = combine(
        _presentationPhase,
        _visiblePaymentState,
        _visibleActionCard,
        _visibleBankruptcyState,
        _visibleCurrentField
    ) { phase, payment, actionCard, bankruptcy, visibleField ->
        ActionGateBase(
            state = null,
            phase = phase,
            visiblePayment = payment,
            visibleActionCard = actionCard,
            visibleBankruptcy = bankruptcy,
            visibleCurrentField = visibleField
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ActionGateBase(null, TurnPresentationPhase.IDLE, null, null, null, null)
    )

    private val actionGateBase: StateFlow<ActionGateBase> = combine(
        gameState,
        actionGateVisuals
    ) { state, visuals ->
        visuals.copy(state = state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ActionGateBase(null, TurnPresentationPhase.IDLE, null, null, null, null)
    )

    private val actionGateLocks: StateFlow<ActionGateLocks> = combine(
        _paymentActionInFlight,
        _propertyActionInFlight,
        _showBankruptcyConfirmation,
        _buildingActionPending,
        _reportCheaterInFlight
    ) { paymentInFlight, propertyInFlight, bankruptcyConfirm, buildingPending, reportCheaterInFlight ->
        ActionGateLocks(
            paymentInFlight = paymentInFlight,
            propertyInFlight = propertyInFlight,
            bankruptcyConfirm = bankruptcyConfirm,
            buildingPending = buildingPending,
            reportCheaterInFlight = reportCheaterInFlight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ActionGateLocks(false, false, false, false, false)
    )

    val actionGates: StateFlow<ActionGates> = combine(
        actionGateBase,
        actionGateLocks
    ) { base, locks ->
        buildActionGates(
            base = base,
            locks = locks
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ActionGates()
    )

    val canEndTurnForCurrentPlayer: StateFlow<Boolean> =
        actionGates.map { it.canEndTurn }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val diceResultForCurrentPlayer: StateFlow<DiceRoll?> = gameState
        .map { state ->
            if (
                state?.phase == GamePhase.BUYING &&
                state.currentPlayer?.id == gameService.currentPlayerId
            ) {
                state.lastDiceRoll
            } else {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val showDiceOverlayForCurrentPlayer: StateFlow<Boolean> = gameState
        .map { state ->
            val isCurrentPlayer = state?.currentPlayer?.id == gameService.currentPlayerId
            val isRollingPhase = state?.phase == GamePhase.ROLLING
            val hasResult = state?.phase == GamePhase.BUYING && state.lastDiceRoll != null
            val isTurnEnd = state?.phase == GamePhase.TURN_END
            isCurrentPlayer && !isTurnEnd && (isRollingPhase || hasResult)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showActionCardOverlay: StateFlow<Boolean> = visibleActionCard
        .map { card ->
            val isCurrentPlayer =
                gameState.value?.currentPlayer?.id == gameService.currentPlayerId

            isCurrentPlayer && card != null
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showMortgageOverlay = MutableStateFlow(false)
    val showMortgageOverlay: StateFlow<Boolean> = _showMortgageOverlay.asStateFlow()

    private val _currentRentAmount = MutableStateFlow(0)
    val currentRentAmount: StateFlow<Int> = _currentRentAmount.asStateFlow()

    private val _currentRentOwnerId = MutableStateFlow<String?>(null)
    val currentRentOwnerId: StateFlow<String?> = _currentRentOwnerId.asStateFlow()

    private val _currentRentFieldId = MutableStateFlow<Int?>(null)
    val currentRentFieldId: StateFlow<Int?> = _currentRentFieldId.asStateFlow()

    private val _lastDiceTotalForRent = MutableStateFlow(0)

    private var lastPendingPaymentKey: String? = null

    init {
        gameState
            .onEach { state -> updatePendingPaymentState(state) }
            .launchIn(viewModelScope)
    }

    val canPayRent: StateFlow<Boolean> = actionGates
        .map { it.canPayRent || it.canPayTax }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canRaiseFunds: StateFlow<Boolean> = gameState
        .map { state ->
            state?.pendingPayment?.debtorCanPayAfterAssets ?: false
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val manageableProperties: StateFlow<List<ManageableProperty>> = gameState
        .map { state ->
            val currentPlayerId = gameService.currentPlayerId
            val allFields = state?.fields ?: emptyList()
            allFields
                .filter { field ->
                    field is PropertyField || field is RailroadField || field is UtilityField
                }
                .filter { field -> field.ownerIdFromField() == currentPlayerId }
                .map { field -> field.toManageableProperty(allFields) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bankruptcyPlayerId = MutableStateFlow("")
    val bankruptcyPlayerId: StateFlow<String> = _bankruptcyPlayerId.asStateFlow()
    private val _bankruptcyPlayerName = MutableStateFlow("")
    val bankruptcyPlayerName: StateFlow<String> = _bankruptcyPlayerName.asStateFlow()

    private val _bankruptcyTotalAssets = MutableStateFlow(0)
    val bankruptcyTotalAssets: StateFlow<Int> = _bankruptcyTotalAssets.asStateFlow()

    private val _bankruptcyTotalDebt = MutableStateFlow(0)
    val bankruptcyTotalDebt: StateFlow<Int> = _bankruptcyTotalDebt.asStateFlow()

    private val _bankruptcyPropertiesOwned = MutableStateFlow<List<ManageableProperty>>(emptyList())
    val bankruptcyPropertiesOwned: StateFlow<List<ManageableProperty>> = _bankruptcyPropertiesOwned.asStateFlow()

    val events: SharedFlow<String> = gameService.events
    val status: SharedFlow<String> = gameService.status
    val currentPlayerId: String get() = gameService.currentPlayerId

    private var isCheatActive = false


    fun connect() = gameService.connect()

    fun createGame(playerName: String) {
        viewModelScope.launch {
            gameService.createGame(playerName)
        }
    }

    fun joinGame(gameId: String, playerName: String) {
        viewModelScope.launch {
            gameService.joinGame(gameId, playerName)
        }
    }

    fun startGame() = gameService.startGame()

private fun guardAction(actionName: String, canRun: Boolean): Boolean {
        if (gameState.value == null) return true
        if (!canRun) {
            Log.d("GameViewModel", "$actionName ignored by action gate")
            return false
        }
        return true
    }

    fun activateCheatForNextRoll() {
        if (!guardAction("activateCheatForNextRoll", actionGates.value.canActivateCheat)) {
            Log.d("DiceDebug", "Cheat ignored: Player already Rolling Dice.")
            return
        }

        isCheatActive = true
        Log.d("DiceDebug", "Cheating activated.")
    }

    fun rollDice() {
        if (!guardAction("rollDice", actionGates.value.canRollDice)) return
        if (rollRequestInFlight) return
        rollRequestInFlight = true
        val sequenceId = nextPresentationSequenceId()
        _presentationPhase.value = TurnPresentationPhase.ROLLING_DICE
        _activeDicePresentation.value = ActiveDicePresentation(
            sequenceId = sequenceId,
            diceRoll = null,
            isRolling = true
        )

        val token = ++rollActionToken

        viewModelScope.launch {
            delay(5000L)
            if (rollActionToken == token) {
                rollRequestInFlight = false
            }
        }

        gameService.rollDice(isCheating = isCheatActive)
        isCheatActive = false
    }

    fun onDiceResultDisplayed(sequenceId: Long) {
        if (sequenceId != presentationSequenceId) {
            Log.d("GameViewModel", "Ignoring stale dice result callback for sequence=$sequenceId")
        }
    }

    fun onDiceDismissed(sequenceId: Long) {
        if (sequenceId != presentationSequenceId) {
            Log.d("GameViewModel", "Ignoring stale dice dismiss callback for sequence=$sequenceId")
        }
    }

    fun endTurn() {
        if (!guardAction("endTurn", actionGates.value.canEndTurn)) return
        gameService.endTurn()
    }



    fun payJailFine() {
        if (!guardAction("payJailFine", actionGates.value.canUseJailAction)) return
        gameService.payJailFine()
    }

    fun useJailCard() {
        if (!guardAction("useJailCard", actionGates.value.canUseJailAction)) return
        gameService.useJailCard()
    }

    fun payRent() {
        Log.d("GameViewModel", "payRent() called, inFlight=${_paymentActionInFlight.value}, fieldId=${currentRentFieldId.value}, money=${(gameState.value?.players?.find { it.id == gameService.currentPlayerId }?.money)}")
        if (!guardAction("payRent", actionGates.value.canPayRent)) return
        if (_paymentActionInFlight.value) return
        val fieldId = currentRentFieldId.value
        val diceTotal = _lastDiceTotalForRent.value
        startPaymentAction()
        gameService.payRent(fieldId, diceTotal)
    }

    fun payTax() {
        Log.d(
            "GameViewModel",
            "payTax() called, inFlight=${_paymentActionInFlight.value}, fieldId=${currentRentFieldId.value}"
        )

        if (!guardAction("payTax", actionGates.value.canPayTax)) return
        if (_paymentActionInFlight.value) return

        val fieldId = currentRentFieldId.value ?: return

        startPaymentAction()

        gameService.payTax(fieldId)
    }

    fun mortgageProperty(fieldId: Int) {
        if (!guardAction("mortgageProperty", actionGates.value.canManageProperties)) return
        if (_propertyActionInFlight.value) return
        startPropertyAction()
        gameService.mortgageProperty(fieldId)
    }

    fun unmortgageProperty(fieldId: Int) {
        if (!guardAction("unmortgageProperty", actionGates.value.canManageProperties)) return
        if (_propertyActionInFlight.value) return
        startPropertyAction()
        gameService.unmortgageProperty(fieldId)
    }

    fun sellHouse(fieldId: Int) {
        if (!guardAction("sellHouse", actionGates.value.canManageProperties)) return
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.sellHouse(fieldId)
    }

    fun declareBankruptcy() {
        if (!guardAction("declareBankruptcy", actionGates.value.canDeclareBankruptcy)) return
        if (_paymentActionInFlight.value || _showBankruptcyConfirmation.value) return
        _showPayRentOverlay.value = false
        _showBankruptcyConfirmation.value = true
    }

    fun confirmDeclareBankruptcy() {
        if (!guardAction("confirmDeclareBankruptcy", actionGates.value.canConfirmDeclareBankruptcy)) return
        if (_paymentActionInFlight.value) return
        _showBankruptcyConfirmation.value = false
        startPaymentAction()
        gameService.declareBankruptcy()
    }

    fun cancelDeclareBankruptcy() {
        _showBankruptcyConfirmation.value = false
        _showPayRentOverlay.value = true
    }

    fun debugForwardGame() {
        gameService.debugForwardGame()
    }

    fun debugSetupBankruptcy() {
        gameService.debugSetupBankruptcy()
    }

    fun showPayRentOverlay(
        amount: Int,
        ownerId: String?,
        fieldId: Int?
    ) {
        if (!guardAction("showPayRentOverlay", _visiblePaymentState.value != null)) return
        _currentRentAmount.value = amount
        _currentRentOwnerId.value = ownerId
        _currentRentFieldId.value = fieldId
        _showPayRentOverlay.value = true
    }

    fun dismissPayRentOverlay() {
        _showPayRentOverlay.value = false
    }

    fun showMortgageManagementOverlay() {
        _showMortgageOverlay.value = true
    }

    fun dismissMortgageOverlay() {
        _showMortgageOverlay.value = false
    }

    fun showBankruptcyOverlay() {
        _showBankruptcyOverlay.value = true
    }

    fun dismissBankruptcyOverlay() {
        _showBankruptcyOverlay.value = false
    }

    fun acceptBankruptcyResolution() {
        _showBankruptcyOverlay.value = false
        _bankruptcyPlayerId.value = ""
        _bankruptcyPlayerName.value = ""
    }

    fun requestState() = gameService.requestState()

    fun setGameId(gameId: String) = gameService.setGameId(gameId)

    fun reportCheater(reportedPlayerId: String) {
        if (!guardAction("reportCheater", actionGates.value.canReportCheater)) return
        if (_reportCheaterInFlight.value) return
        startReportCheaterAction()
        gameService.reportCheater(reportedPlayerId)
    }

    private fun updatePendingPaymentState(state: GameState?, reveal: Boolean = false) {
        if (state == null) {
            Log.d("GameViewModel", "updatePendingPaymentState: state=null, clearing fieldId")
            _hasPendingPayment.value = false
            _showPayRentOverlay.value = false
            _visiblePaymentState.value = null
            _currentRentAmount.value = 0
            _currentRentOwnerId.value = null
            _currentRentFieldId.value = null
            _lastDiceTotalForRent.value = 0
            lastPendingPaymentKey = null
            return
        }

        val pending = state.pendingPayment
        val pendingKey = pending?.let { p ->
            "${p.source}:${p.sourceFieldId}:${p.amount}:${p.creditorPlayerId}"
        }

        if (pendingKey == null || pending.amount <= 0) {
            Log.d("GameViewModel", "updatePendingPaymentState: pending null/empty, clearing fieldId. pendingKey=$pendingKey")
            _hasPendingPayment.value = false
            _showPayRentOverlay.value = false
            _visiblePaymentState.value = null
            _currentRentAmount.value = 0
            _currentRentOwnerId.value = null
            _currentRentFieldId.value = null
            _lastDiceTotalForRent.value = 0
            lastPendingPaymentKey = null
            return
        }

        _hasPendingPayment.value = true

        _currentRentAmount.value = pending.amount
        _currentRentOwnerId.value = pending.creditorPlayerId
        _currentRentFieldId.value = pending.sourceFieldId
        Log.d("GameViewModel", "updatePendingPaymentState: set fieldId=${pending.sourceFieldId}, amount=${pending.amount}, source=${pending.source}")

        _lastDiceTotalForRent.value = state.lastDiceRoll?.total ?: 0

        if (reveal) {
            _visiblePaymentState.value = VisiblePaymentState(pending)
        }

        if (reveal && pendingKey != lastPendingPaymentKey) {
            Log.d("GameViewModel", "updatePendingPaymentState: new pendingKey=$pendingKey, revealing overlay")
            _showPayRentOverlay.value = true
            lastPendingPaymentKey = pendingKey
        }
    }

    private val _movementAnimation = MutableStateFlow<MovementAnimationState?>(null)
    val movementAnimation: StateFlow<MovementAnimationState?> = _movementAnimation

    private var previousGameState: GameState? = null
    private var animationJob: Job? = null
    private var presentationJob: Job? = null
    private var presentationSequenceId: Long = 0L
    private val bufferedPresentedLogs = mutableListOf<LogEntry>()

    init {
        gameEventFlow
            .onEach { event -> handleIncomingGameEvent(event) }
            .launchIn(viewModelScope)
    }

    val winner: StateFlow<Player?> = gameState
        .map { state ->
            state?.players?.firstOrNull { !it.eliminated && !it.isBankrupt() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    fun drawCard(cardType: String = "CHANCE") {
        val gates = actionGates.value
        val canDraw = when (cardType) {
            "CHANCE" -> gates.canDrawChance
            "COMMUNITY_CHEST" -> gates.canDrawCommunityChest
            else -> gates.canDrawCard
        }
        if (!guardAction("drawCard", canDraw)) return
        gameService.drawCard(cardType)
    }

    fun executeAction() {
        if (!guardAction("executeAction", actionGates.value.canExecuteCard)) return
        _isExecutingAction.value = true
        Log.d("ActionCard", "Executing action for player: $currentPlayerId")
        gameService.executeAction(currentPlayerId)
    }

    fun setCurrentActionCard(card: Card?) {
        Log.d("ActionCard", "Setting current action card: ${card?.description}")
        _currentActionCard.value = card
        _visibleActionCard.value = card
    }

    fun dismissActionCard() {
        _visibleActionCard.value = null
    }

    fun showPlayerOverlay(player: Player) {
        _selectedPlayerForOverlay.value = player
    }

    fun hidePlayerOverlay() {
        _selectedPlayerForOverlay.value = null
    }

    fun showTradeOverlay(player: Player) {
        _selectedPlayerForOverlay.value = null
        _selectedPlayerForTrade.value = player
    }

    fun hideTradeOverlay() {
        _selectedPlayerForTrade.value = null
    }

    fun proposeTrade(
        toPlayerId: String,
        offerMoney: Int,
        requestMoney: Int,
        offerPropertyIds: List<Int>,
        requestPropertyIds: List<Int>,
        offerJailCards: Int,
        requestJailCards: Int
    ) {
        if (!guardAction("proposeTrade", actionGates.value.canTrade)) return
        gameService.proposeTrade(
            toPlayerId = toPlayerId,
            offerMoney = offerMoney,
            requestMoney = requestMoney,
            offerPropertyIds = offerPropertyIds,
            requestPropertyIds = requestPropertyIds,
            offerJailCards = offerJailCards,
            requestJailCards = requestJailCards
        )
    }

    fun acceptTrade(tradeId: String) {
        if (!guardAction("acceptTrade", actionGates.value.canTrade || gameState.value?.pendingTradeOffer != null)) return
        gameService.acceptTrade(tradeId)
    }

    fun rejectTrade(tradeId: String) {
        if (!guardAction("rejectTrade", actionGates.value.canTrade || gameState.value?.pendingTradeOffer != null)) return
        gameService.rejectTrade(tradeId)
    }

    fun syncGameboardEntryState() {
        val currentGameId = gameService.currentGameId

        if (currentGameId.isBlank()) return

        gameService.requestState()
    }

    private fun buildActionGates(
        base: ActionGateBase,
        locks: ActionGateLocks
    ): ActionGates {
        val state = base.state ?: return ActionGates()
        val currentPlayer = state.currentPlayer
        val localPlayer = state.players.find { it.id == gameService.currentPlayerId }
        val isCurrentPlayer = currentPlayer?.id == gameService.currentPlayerId
        val gameStarted = state.phase != GamePhase.WAITING && state.phase != GamePhase.FINISHED
        val localPlayerActive = localPlayer != null && !localPlayer.eliminated && gameStarted
        val presentationReady = base.phase == TurnPresentationPhase.READY_FOR_ACTION ||
                base.phase == TurnPresentationPhase.IDLE
        val rollInputReady = base.phase == TurnPresentationPhase.WAITING_FOR_ROLL_INPUT
        val payment = base.visiblePayment
        val hasVisibleBlockingOverlay =
            payment != null ||
                    base.visibleActionCard != null ||
                    base.visibleBankruptcy != null ||
                    locks.bankruptcyConfirm
        val currentField = base.visibleCurrentField
        val isBuyableField = currentField is OwnableField
        val isUnownedField = (currentField as? OwnableField)?.ownerId == null
        val isOnChance = currentField is ChanceField
        val isOnCommunityChest = currentField is CommunityChestField
        val canRoll = state.phase == GamePhase.ROLLING &&
                isCurrentPlayer &&
                rollInputReady &&
                !rollRequestInFlight
        val doubleRollPending = state.lastDiceRoll?.isDouble == true &&
                isCurrentPlayer &&
                (state.phase == GamePhase.BUYING || state.phase == GamePhase.TURN_END)
        val canRollAgainAfterDouble = doubleRollPending &&
                presentationReady &&
                !hasVisibleBlockingOverlay &&
                state.pendingPayment == null &&
                !rollRequestInFlight
        val canRollDice = canRoll || canRollAgainAfterDouble
        val canUseJailAction = canRoll &&
                currentPlayer?.inJail == true &&
                !locks.paymentInFlight
        val canEndTurn = (state.phase == GamePhase.BUYING || state.phase == GamePhase.TURN_END) &&
                isCurrentPlayer &&
                presentationReady &&
                !hasVisibleBlockingOverlay &&
                state.pendingPayment == null &&
                !doubleRollPending
        val canBuyProperty = state.phase == GamePhase.BUYING &&
                isCurrentPlayer &&
                presentationReady &&
                !hasVisibleBlockingOverlay &&
                isBuyableField &&
                isUnownedField
        val canDrawChance = state.phase == GamePhase.BUYING &&
                isCurrentPlayer &&
                presentationReady &&
                !hasVisibleBlockingOverlay &&
                isOnChance &&
                !state.hasDrawnCardThisTurn
        val canDrawCommunityChest = state.phase == GamePhase.BUYING &&
                isCurrentPlayer &&
                presentationReady &&
                !hasVisibleBlockingOverlay &&
                isOnCommunityChest &&
                !state.hasDrawnCardThisTurn
        val canExecuteCard = isCurrentPlayer &&
                presentationReady &&
                base.visibleActionCard != null &&
                !_isExecutingAction.value
        val canPayRent = isCurrentPlayer &&
                presentationReady &&
                payment?.source == PaymentSource.RENT &&
                !locks.paymentInFlight &&
                (localPlayer?.money ?: 0) >= payment.amount
        val canPayTax = isCurrentPlayer &&
                presentationReady &&
                payment?.source == PaymentSource.TAX &&
                !locks.paymentInFlight &&
                (localPlayer?.money ?: 0) >= payment.amount
        val canManageProperties = localPlayerActive &&
                !locks.propertyInFlight &&
                !locks.buildingPending &&
                payment == null
        val canDeclareBankruptcy = isCurrentPlayer &&
                presentationReady &&
                payment != null &&
                !locks.paymentInFlight &&
                !locks.bankruptcyConfirm
        val canTrade = isCurrentPlayer &&
                presentationReady &&
                !hasVisibleBlockingOverlay &&
                state.pendingTradeOffer == null
        val canReport = localPlayerActive &&
                presentationReady &&
                !locks.reportCheaterInFlight &&
                (localPlayer?.money ?: 0) > 500

        return ActionGates(
            canRollDice = canRollDice,
            canActivateCheat = canRoll,
            canEndTurn = canEndTurn,
            canBuyProperty = canBuyProperty,
            canDrawCard = canDrawChance || canDrawCommunityChest,
            canDrawChance = canDrawChance,
            canDrawCommunityChest = canDrawCommunityChest,
            canExecuteCard = canExecuteCard,
            canPayRent = canPayRent,
            canPayTax = canPayTax,
            canUseJailAction = canUseJailAction,
            canManageProperties = canManageProperties,
            canDeclareBankruptcy = canDeclareBankruptcy,
            canConfirmDeclareBankruptcy = locks.bankruptcyConfirm && !locks.paymentInFlight,
            canTrade = canTrade,
            canReportCheater = canReport
        )
    }

    private fun handleIncomingGameEvent(event: GameEvent) {
        if (
            event.event == "GAME_CREATED" &&
            event.gameId.isNotBlank() &&
            gameService.currentGameId.isBlank()
        ) {
            gameService.setGameId(event.gameId)
        }

        if (shouldIgnoreEvent(event)) return

        if (event.event == "CHEATER_REPORTED" || event.event == "CHEATER_REPORT_FAILED") {
            finishReportCheaterAction()
            event.message?.let { msg ->
                viewModelScope.launch { _dramaEvent.emit(msg) }
            }
        }

        val oldState = previousGameState
        event.gameState?.let { state ->
            previousGameState = state
            if (_presentedBoardPlayers.value.isEmpty()) {
                _presentedBoardPlayers.value = copyPlayersForPresentation(state.players)
            }
        }

        handleImmediateRequestState(event)

        when {
            isHardSyncEvent(event) -> {
                hardSyncPresentation(event.gameState, event)
                appendPresentedLog(event)
            }

            event.event == "ERROR" -> {
                handleNonFatalError(event)
            }

            event.event == "DICE_ROLLED" -> {
                rollRequestInFlight = false
                startTurnPresentation(event, oldState, event.gameState, includeDice = true)
            }

            event.event == "ACTION_EXECUTED" -> {
                _isExecutingAction.value = false
                val card = _currentActionCard.value ?: oldState?.currentActionCard
                _currentActionCard.value = null
                _visibleActionCard.value = null
                startTurnPresentation(event, oldState, event.gameState, includeDice = false, actionCard = card)
            }

            event.event == "PLAYER_JAILED" -> {
                if (isPresentationBlocking()) {
                    bufferPresentedLog(event)
                } else {
                    event.gameState?.let { hardSyncPresentation(it, event) }
                    appendPresentedLog(event)
                }
            }

            event.event == "ACTION_DRAWN" -> {
                val card = event.gameState?.currentActionCard
                if (card != null) {
                    _currentActionCard.value = card
                    lastCurrentPlayerIdForCardDraw = event.gameState.currentPlayer?.id
                    if (isPresentationBlocking()) {
                        bufferPresentedLog(event)
                    } else {
                        revealActionCard(card)
                        appendPresentedLog(event)
                    }
                } else {
                    appendOrBufferPresentedLog(event)
                }
            }

            event.event in JAIL_ACTION_EVENTS -> {
                handleJailActionEvent(event)
            }

            event.event in LANDING_REVEAL_EVENTS -> {
                if (event.event == "BANKRUPTCY_DECLARED") {
                    finishPaymentAction()
                    if (!isPresentationBlocking()) {
                        event.gameState?.let { state ->
                            clearVisiblePayment()
                            val gs = state
                            _bankruptcyPlayerId.value = gs.bankruptcyPlayerId
                            _bankruptcyPlayerName.value = gs.players.find { it.id == gs.bankruptcyPlayerId }?.name ?: ""
                            _bankruptcyTotalAssets.value = gs.bankruptcyTotalAssets
                            _bankruptcyTotalDebt.value = gs.bankruptcyTotalDebt
                            _bankruptcyPropertiesOwned.value = gs.let { s ->
                                val allFields = s.fields
                                allFields
                                    .filter { it.id in s.bankruptcyOwnedFieldIds }
                                    .filter { it is PropertyField || it is RailroadField || it is UtilityField }
                                    .map { field -> field.toManageableProperty(allFields) }
                            } ?: emptyList()
                            _visibleBankruptcyState.value = VisibleBankruptcyState(
                                playerId = gs.bankruptcyPlayerId,
                                playerName = _bankruptcyPlayerName.value,
                                totalAssets = gs.bankruptcyTotalAssets,
                                totalDebt = gs.bankruptcyTotalDebt,
                                propertiesOwned = _bankruptcyPropertiesOwned.value
                            )
                            _showBankruptcyOverlay.value = true
                        }
                        appendPresentedLog(event)
                    } else {
                        bufferPresentedLog(event)
                    }
                } else if (isPresentationBlocking()) {
                    bufferPresentedLog(event)
                } else {
                    revealLandingForState(event.gameState ?: previousGameState)
                    appendPresentedLog(event)
                }
            }

            event.event == "RENT_PAID" || event.event == "TAX_PAID" -> {
                finishPaymentAction()
                clearVisiblePayment()
                appendPresentedLog(event)
            }

            event.event == "PAYMENT_FAILED" -> {
                showTransientError(event.message ?: "Payment failed")
                finishPaymentAction()
                appendPresentedLog(event)
            }

            event.event == "TURN_ENDED" -> {
                rollRequestInFlight = false
                _buildingActionPending.value = false
                lastCurrentPlayerIdForCardDraw = null
                clearVisiblePayment()
                _visibleActionCard.value = null
                _currentActionCard.value = null
                hardSyncPresentation(event.gameState, event, cancelJob = false)
                appendPresentedLog(event)
            }

            event.event in PROPERTY_BUILD_TRADE_EVENTS -> {
                if (event.event == "TRADE_COMPLETED" || event.event == "TRADE_REJECTED") {
                    _selectedPlayerForTrade.value = null
                }
                if (event.event in BUILDING_EVENTS) {
                    Log.i("GameViewModel", "Building action completed - refreshing state")
                    finishPropertyAction()
                } else if (event.event == "PROPERTY_MORTGAGED" || event.event == "PROPERTY_UNMORTGAGED") {
                    Log.i("GameViewModel", "Property mortgage action completed - refreshing state")
                    finishPropertyAction()
                }

                if (isPresentationBlocking() && event.gameState?.currentPlayer?.id == previousGameState?.currentPlayer?.id) {
                    bufferPresentedLog(event)
                } else {
                    revealLandingForState(event.gameState ?: previousGameState, revealPayment = false)
                    appendPresentedLog(event)
                }
            }

            else -> {
                if (event.event == "GAME_STARTED") {
                    clearPresentationBuffers()
                    _showGameOverOverlay.value = false
                    _hostEndedGame.value = false
                }
                event.gameState?.let { state ->
                    if (!isPresentationBlocking()) {
                        renderRawState(state)
                    }
                }
                appendOrBufferPresentedLog(event)
            }
        }
    }

    private fun handleJailActionEvent(event: GameEvent) {
        if (isPresentationBlocking()) {
            bufferPresentedLog(event)
            return
        }

        event.gameState?.let { state ->
            renderNonBoardStatePreservingTokens(state)
        }
        appendPresentedLog(event)
    }

    private fun renderNonBoardStatePreservingTokens(state: GameState) {
        val currentPresentedPlayers = _presentedBoardPlayers.value
        val presentedPositions = currentPresentedPlayers.associate { it.id to it.position }
        _presentedBoardPlayers.value =
            if (currentPresentedPlayers.isEmpty()) {
                copyPlayersForPresentation(state.players)
            } else {
                state.players.map { player ->
                    player.copy(position = presentedPositions[player.id] ?: player.position)
                }
            }

        val currentPlayerId = state.currentPlayer?.id
        val visiblePosition = currentPlayerId
            ?.let { id -> _presentedBoardPlayers.value.find { it.id == id }?.position }
            ?: state.currentPlayer?.position
        _visibleCurrentField.value = visiblePosition?.let { position -> state.fields.getOrNull(position) }

        if (state.currentActionCard == null) {
            _currentActionCard.value = null
            _visibleActionCard.value = null
        } else {
            _currentActionCard.value = state.currentActionCard
        }

        updatePendingPaymentState(state, reveal = false)
        _presentationPhase.value = phaseFromRawState(state)
    }

    private fun handleImmediateRequestState(event: GameEvent) {
        if (
            event.event == "HOUSE_BOUGHT" ||
            event.event == "HOTEL_BOUGHT" ||
            event.event == "HOUSE_SOLD" ||
            event.event == "HOTEL_SOLD" ||
            event.event == "ERROR"
        ) {
            _buildingActionPending.value = false
        }
    }

    private fun shouldIgnoreEvent(event: GameEvent): Boolean {
        val eventGameId = event.gameId
        val currentGameId = gameService.currentGameId
        return event.event != "GAME_CREATED" &&
                eventGameId.isNotBlank() &&
                currentGameId.isNotBlank() &&
                eventGameId != currentGameId
    }

    private fun isHardSyncEvent(event: GameEvent): Boolean =
        event.event == "STATE_SNAPSHOT" ||
                event.event == "GAME_OVER" ||
                event.event == "GAME_CLOSED" ||
                event.event == "TURN_TIMEOUT" ||
                event.gameState?.phase == GamePhase.FINISHED

    private fun handleNonFatalError(event: GameEvent) {
        rollRequestInFlight = false
        showTransientError(event.message ?: "An unknown error occurred")
        finishPaymentAction()
        finishPropertyAction()
        finishReportCheaterAction()
        if (_presentationPhase.value == TurnPresentationPhase.ROLLING_DICE) {
            presentationJob?.cancel()
            animationJob?.cancel()
            _activeDicePresentation.value = null
            _movementAnimation.value = null
            renderRawState(previousGameState)
        }
        flushBufferedPresentedLogs()
        appendPresentedLog(event)
    }

    private fun startTurnPresentation(
        event: GameEvent,
        oldState: GameState?,
        newState: GameState?,
        includeDice: Boolean,
        actionCard: Card? = null
    ) {
        if (newState == null) {
            appendPresentedLog(event)
            return
        }

        val sequenceId = if (includeDice && _activeDicePresentation.value != null) {
            _activeDicePresentation.value!!.sequenceId
        } else {
            nextPresentationSequenceId()
        }

        val movementPath = inferPresentationPath(event, oldState, newState, actionCard)
        if (movementPath.hardSyncOnly) {
            hardSyncPresentation(newState, event)
            appendPresentedLog(event)
            return
        }

        presentationJob?.cancel()
        animationJob?.cancel()
        presentationJob = viewModelScope.launch {
            if (includeDice) {
                val roll = newState.lastDiceRoll
                if (roll != null) {
                    _presentationPhase.value = TurnPresentationPhase.SHOWING_DICE_RESULT
                    _activeDicePresentation.value = ActiveDicePresentation(
                        sequenceId = sequenceId,
                        diceRoll = roll,
                        isRolling = false
                    )
                    freezePresentedPlayerAtStart(newState, movementPath)
                    delay(DICE_RESULT_PRESENTATION_MS)
                }
            }

            if (movementPath.path.isNotEmpty()) {
                _presentationPhase.value = TurnPresentationPhase.MOVING_TOKEN
                runMovementPresentation(newState, movementPath)
            } else {
                _movementAnimation.value = null
            }

            if (presentationSequenceId != sequenceId) return@launch

            _presentedBoardPlayers.value = copyPlayersForPresentation(newState.players)
            _visibleCurrentField.value = currentFieldForState(newState)
            _presentationPhase.value = TurnPresentationPhase.REVEALING_LANDING_EFFECT
            revealLandingForState(previousGameState ?: newState)
            appendPresentedLog(event)
            flushBufferedPresentedLogs()
            delay(LANDING_REVEAL_PRESENTATION_MS)

            if (presentationSequenceId == sequenceId) {
                _presentationPhase.value = phaseFromRawState(previousGameState ?: newState)
            }
        }
    }

    private suspend fun runMovementPresentation(state: GameState, movementPath: PresentationPath) {
        val path = movementPath.path
        _movementAnimation.value = MovementAnimationState(
            playerId = movementPath.playerId,
            startPosition = movementPath.startPosition,
            path = path,
            currentStepIndex = -1,
            isComplete = false
        )

        path.forEachIndexed { stepIndex, position ->
            delay(MOVEMENT_STEP_MS)
            _presentedBoardPlayers.value = overridePresentedPlayer(
                state = state,
                playerId = movementPath.playerId,
                position = position
            )
            _movementAnimation.value = _movementAnimation.value?.copy(
                currentStepIndex = stepIndex
            )
        }

        _movementAnimation.value = _movementAnimation.value?.copy(
            currentStepIndex = path.size,
            isComplete = true
        )
    }

    private fun inferPresentationPath(
        event: GameEvent,
        oldState: GameState?,
        newState: GameState,
        actionCard: Card?
    ): PresentationPath {
        val boardSize = newState.fields.size.takeIf { it > 0 } ?: 40
        val playerId = newState.currentPlayer?.id
            ?: oldState?.currentPlayer?.id
            ?: return PresentationPath("", 0, emptyList())
        val oldPlayer = oldState?.players?.find { it.id == playerId }
        val newPlayer = newState.players.find { it.id == playerId }
        if (oldPlayer == null || newPlayer == null) {
            return PresentationPath(playerId, newPlayer?.position ?: 0, emptyList(), hardSyncOnly = true)
        }

        val becameJailed = !oldPlayer.inJail && newPlayer.inJail
        val jailPosition = 10
        val goToJailPosition = 30

        if (becameJailed || newPlayer.position == jailPosition && newPlayer.inJail) {
            val diceTotal = newState.lastDiceRoll?.total ?: 0
            val diceLanding = (oldPlayer.position + diceTotal).floorMod(boardSize)
            val path = when {
                event.event == "DICE_ROLLED" && diceTotal > 0 && diceLanding == goToJailPosition ->
                    computeMovementPath(oldPlayer.position, diceTotal, boardSize) + jailPosition
                else -> listOf(jailPosition)
            }
            return PresentationPath(playerId, oldPlayer.position, path)
        }

        if (oldPlayer.position == newPlayer.position) {
            return PresentationPath(playerId, oldPlayer.position, emptyList())
        }

        if (event.event == "ACTION_EXECUTED") {
            val actionPath = when (actionCard?.action) {
                CardAction.MOVE_FORWARD -> {
                    val spaces = actionCard.moveSpaces
                    when {
                        spaces > 0 -> computeMovementPath(oldPlayer.position, spaces, boardSize)
                        spaces < 0 -> computeBackwardMovementPath(oldPlayer.position, -spaces, boardSize)
                        else -> emptyList()
                    }
                }
                CardAction.MOVE_TO -> actionCard.targetFieldId?.let { target ->
                    computeDirectMovementPath(oldPlayer.position, target, boardSize)
                } ?: emptyList()
                CardAction.GO_TO_JAIL -> listOf(jailPosition)
                else -> computeDirectMovementPath(oldPlayer.position, newPlayer.position, boardSize)
            }

            val endsCorrectly = actionPath.lastOrNull() == newPlayer.position
            return if (actionPath.isEmpty() || endsCorrectly) {
                PresentationPath(playerId, oldPlayer.position, actionPath)
            } else {
                PresentationPath(playerId, oldPlayer.position, emptyList(), hardSyncOnly = true)
            }
        }

        if (event.event == "DICE_ROLLED") {
            val total = newState.lastDiceRoll?.total
                ?: (newPlayer.position - oldPlayer.position).floorMod(boardSize)
            val path = computeMovementPath(oldPlayer.position, total, boardSize)
            return if (path.lastOrNull() == newPlayer.position) {
                PresentationPath(playerId, oldPlayer.position, path)
            } else {
                PresentationPath(playerId, oldPlayer.position, emptyList(), hardSyncOnly = true)
            }
        }

        return PresentationPath(
            playerId = playerId,
            startPosition = oldPlayer.position,
            path = computeDirectMovementPath(oldPlayer.position, newPlayer.position, boardSize)
        )
    }

    private fun hardSyncPresentation(
        state: GameState?,
        event: GameEvent? = null,
        cancelJob: Boolean = true
    ) {
        if (cancelJob) {
            presentationJob?.cancel()
            animationJob?.cancel()
        }
        nextPresentationSequenceId()
        _movementAnimation.value = null
        clearPresentationBuffers()
        renderRawState(state)
        _presentationPhase.value = phaseFromRawState(state)
        rollRequestInFlight = false
        finishPaymentAction()
        finishPropertyAction()
        finishReportCheaterAction()

        when {
            event?.event == "GAME_OVER" || state?.phase == GamePhase.FINISHED -> {
                _showGameOverOverlay.value = true
                _hostEndedGame.value = false
            }
            event?.event == "GAME_CLOSED" -> {
                _hostEndedGame.value = true
                _showGameOverOverlay.value = false
            }
        }
    }

    private fun renderRawState(state: GameState?) {
        if (state == null) {
            _presentedBoardPlayers.value = emptyList()
            _visibleCurrentField.value = null
            _visibleActionCard.value = null
            _currentActionCard.value = null
            clearVisiblePayment()
            _visibleBankruptcyState.value = null
            _presentationPhase.value = TurnPresentationPhase.IDLE
            return
        }

        _presentedBoardPlayers.value = copyPlayersForPresentation(state.players)
        _visibleCurrentField.value = currentFieldForState(state)
        _currentActionCard.value = state.currentActionCard
        _visibleActionCard.value = state.currentActionCard
        updatePendingPaymentState(state, reveal = state.pendingPayment != null)
        if (state.phase == GamePhase.BANKRUPTCY || state.bankruptcyPlayerId.isNotBlank()) {
            revealBankruptcyState(state)
        } else {
            _visibleBankruptcyState.value = null
            _showBankruptcyOverlay.value = false
        }
        _presentationPhase.value = phaseFromRawState(state)
    }

    private fun revealLandingForState(state: GameState?, revealPayment: Boolean = true) {
        if (state == null) return
        _visibleCurrentField.value = currentFieldForState(state)

        state.currentActionCard?.let { card ->
            _currentActionCard.value = card
            revealActionCard(card)
        }

        if (revealPayment) {
            updatePendingPaymentState(state, reveal = state.pendingPayment != null)
        }

        if (state.phase == GamePhase.BANKRUPTCY || state.bankruptcyPlayerId.isNotBlank()) {
            revealBankruptcyState(state)
        }
    }

    private fun revealActionCard(card: Card) {
        _visibleActionCard.value = card
    }

    private fun revealBankruptcyState(state: GameState) {
        val playerId = state.bankruptcyPlayerId
        val properties = state.fields
            .filter { it.id in state.bankruptcyOwnedFieldIds }
            .filter { it is PropertyField || it is RailroadField || it is UtilityField }
            .map { field -> field.toManageableProperty(state.fields) }
        val visible = VisibleBankruptcyState(
            playerId = playerId,
            playerName = state.players.find { it.id == playerId }?.name ?: "",
            totalAssets = state.bankruptcyTotalAssets,
            totalDebt = state.bankruptcyTotalDebt,
            propertiesOwned = properties
        )
        _visibleBankruptcyState.value = visible
        _bankruptcyPlayerId.value = visible.playerId
        _bankruptcyPlayerName.value = visible.playerName
        _bankruptcyTotalAssets.value = visible.totalAssets
        _bankruptcyTotalDebt.value = visible.totalDebt
        _bankruptcyPropertiesOwned.value = visible.propertiesOwned
        _showBankruptcyOverlay.value = true
    }

    private fun clearVisiblePayment() {
        _showPayRentOverlay.value = false
        _visiblePaymentState.value = null
    }

    private fun clearPresentationBuffers() {
        bufferedPresentedLogs.clear()
        _activeDicePresentation.value = null
        _visibleActionCard.value = null
        _currentActionCard.value = null
        clearVisiblePayment()
        _showMortgageOverlay.value = false
        _showBankruptcyOverlay.value = false
        _showBankruptcyConfirmation.value = false
        _visibleBankruptcyState.value = null
        _currentRentAmount.value = 0
        _currentRentOwnerId.value = null
        _currentRentFieldId.value = null
        _lastDiceTotalForRent.value = 0
        lastPendingPaymentKey = null
    }

    private fun freezePresentedPlayerAtStart(state: GameState, movementPath: PresentationPath) {
        if (movementPath.playerId.isBlank()) {
            _presentedBoardPlayers.value = copyPlayersForPresentation(state.players)
            return
        }
        _presentedBoardPlayers.value = overridePresentedPlayer(
            state = state,
            playerId = movementPath.playerId,
            position = movementPath.startPosition
        )
    }

    private fun overridePresentedPlayer(
        state: GameState,
        playerId: String,
        position: Int
    ): List<Player> =
        state.players.map { player ->
            if (player.id == playerId) {
                player.copy(position = position)
            } else {
                player.copy()
            }
        }

    private fun copyPlayersForPresentation(players: List<Player>): List<Player> =
        players.map { it.copy() }

    private fun currentFieldForState(state: GameState?): Field? {
        val player = state?.currentPlayer ?: return null
        return state.fields.getOrNull(player.position)
    }

    private fun phaseFromRawState(state: GameState?): TurnPresentationPhase {
        if (state == null) return TurnPresentationPhase.IDLE
        return when (state.phase) {
            GamePhase.WAITING,
            GamePhase.FINISHED -> TurnPresentationPhase.IDLE
            GamePhase.ROLLING -> {
                if (state.currentPlayer?.id == gameService.currentPlayerId) {
                    TurnPresentationPhase.WAITING_FOR_ROLL_INPUT
                } else {
                    TurnPresentationPhase.READY_FOR_ACTION
                }
            }
            else -> TurnPresentationPhase.READY_FOR_ACTION
        }
    }

    private fun appendOrBufferPresentedLog(event: GameEvent) {
        if (isPresentationBlocking() && event.event in PRESENTATION_BUFFERED_LOG_EVENTS) {
            bufferPresentedLog(event)
        } else {
            appendPresentedLog(event)
        }
    }

    private fun bufferPresentedLog(event: GameEvent) {
        createLogEntry(event)?.let { bufferedPresentedLogs.add(it) }
    }

    private fun flushBufferedPresentedLogs() {
        if (bufferedPresentedLogs.isEmpty()) return
        _presentedEventLog.value = (_presentedEventLog.value + bufferedPresentedLogs)
            .takeLast(MAX_LOG_ENTRIES)
        bufferedPresentedLogs.clear()
    }

    private fun appendPresentedLog(event: GameEvent) {
        createLogEntry(event)?.let { entry ->
            val isGameSwitch =
                event.gameId.isNotBlank() &&
                        _presentedEventLog.value.isNotEmpty() &&
                        event.event == "GAME_CREATED"
            val baseEntries = if (isGameSwitch) emptyList() else _presentedEventLog.value
            _presentedEventLog.value = (baseEntries + entry).takeLast(MAX_LOG_ENTRIES)
        }
    }

    private fun createLogEntry(event: GameEvent): LogEntry? {
        if (event.event == "ERROR") {
            Log.w("GameViewModel", "Server ERROR [game=${event.gameId}]: ${event.message}")
            return null
        }

        val isTechnical =
            event.event == "STATE_SNAPSHOT" ||
                    event.event == "STATE_UPDATED"

        val text = event.message?.takeIf { it.isNotBlank() }
            ?: humanReadableEvent(event.event, event.gameId)

        if (text.isBlank()) return null

        return LogEntry(
            text = text,
            eventType = event.event.ifBlank { "UNKNOWN" },
            isTechnical = isTechnical,
            timestampMs = currentTimeProvider()
        )
    }

    private fun isPresentationBlocking(): Boolean =
        _presentationPhase.value == TurnPresentationPhase.ROLLING_DICE ||
                _presentationPhase.value == TurnPresentationPhase.SHOWING_DICE_RESULT ||
                _presentationPhase.value == TurnPresentationPhase.MOVING_TOKEN ||
                _presentationPhase.value == TurnPresentationPhase.REVEALING_LANDING_EFFECT

    private fun nextPresentationSequenceId(): Long {
        presentationSequenceId += 1
        return presentationSequenceId
    }

    private fun Int.floorMod(modulus: Int): Int =
        ((this % modulus) + modulus) % modulus

    private fun humanReadableEvent(eventType: String, gameId: String): String {
        eventFallbackLabels[eventType]?.let { return it }

        return when (eventType) {
            "GAME_CREATED" -> "Game created: $gameId"
            "PLAYER_JOINED" -> "A new player joined"
            "GAME_STARTED" -> "Game started!"
            "DICE_ROLLED" -> "Dice rolled"
            "TURN_ENDED" -> "Turn ended"
            "STATE_UPDATED" -> "Game state updated"
            "STATE_SNAPSHOT" -> "State snapshot synced"
            "JAIL_FINE_PAID" -> "Bail paid: 50€"
            "JAIL_CARD_USED" -> "Used 'Get out of jail free' card"
            "PLAYER_JAILED" -> "Player went to jail!"
            "ACTION_DRAWN" -> "Action card drawn!"
            "ACTION_EXECUTED" -> "Action executed"
            "RENT_DUE" -> "Rent is due!"
            "RENT_PAID" -> "Rent paid"
            "TAX_DUE" -> "Tax is due!"
            "TAX_PAID" -> "Tax paid"
            "FREE_PARKING_COLLECTED" -> "Free Parking jackpot collected!"
            "PAYMENT_FAILED" -> "Payment failed"
            "BANKRUPTCY_DECLARED" -> "Player went bankrupt!"
            "GAME_OVER" -> "Game Over!"
            "CHEATER_REPORTED" -> "🚨 Cheater successfully reported!"
            "CHEATER_REPORT_FAILED" -> "🚨 False cheater accusation!"
            else -> eventType.replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }

    class Factory(private val gameService: GameService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(gameService) as T
        }
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 80
        private const val ERROR_DISPLAY_MS = 5_000L
        private const val ACTION_TIMEOUT_MS = 5_000L
        private const val DICE_RESULT_PRESENTATION_MS = 2_000L
        private const val MOVEMENT_STEP_MS = 250L
        private const val LANDING_REVEAL_PRESENTATION_MS = 500L
        private val LANDING_REVEAL_EVENTS = setOf(
            "RENT_DUE",
            "TAX_DUE",
            "FREE_PARKING_COLLECTED",
            "BANKRUPTCY_DECLARED"
        )
        private val JAIL_ACTION_EVENTS = setOf(
            "JAIL_FINE_PAID",
            "JAIL_CARD_USED"
        )
        private val PRESENTATION_BUFFERED_LOG_EVENTS = setOf(
            "DICE_ROLLED",
            "ACTION_DRAWN",
            "ACTION_EXECUTED",
            "RENT_DUE",
            "TAX_DUE",
            "FREE_PARKING_COLLECTED",
            "BANKRUPTCY_DECLARED",
            "PLAYER_JAILED",
            "JAIL_FINE_PAID",
            "JAIL_CARD_USED"
        )
        private val BUILDING_EVENTS = setOf(
            "HOUSE_BOUGHT",
            "HOTEL_BOUGHT",
            "HOUSE_SOLD",
            "HOTEL_SOLD"
        )
        private val PROPERTY_BUILD_TRADE_EVENTS = setOf(
            "PROPERTY_BOUGHT",
            "PROPERTY_MORTGAGED",
            "PROPERTY_UNMORTGAGED",
            "HOUSE_BOUGHT",
            "HOTEL_BOUGHT",
            "HOUSE_SOLD",
            "HOTEL_SOLD",
            "TRADE_COMPLETED",
            "TRADE_REJECTED"
        )
        private val eventFallbackLabels = mapOf(
            "PROPERTY_BOUGHT" to "Property bought",
            "PROPERTY_MORTGAGED" to "Property mortgaged",
            "PROPERTY_UNMORTGAGED" to "Property unmortgaged",
            "HOUSE_BOUGHT" to "House bought",
            "HOTEL_BOUGHT" to "Hotel bought",
            "HOUSE_SOLD" to "House sold",
            "HOTEL_SOLD" to "Hotel sold",
            "GAME_CLOSED" to "Game closed by host",
            "TURN_TIMEOUT" to "Turn timed out",
        )
    }

    fun buyProperty(fieldId: Int) {
        if (!guardAction("buyProperty", actionGates.value.canBuyProperty)) return
        gameService.buyProperty(fieldId)
    }

    fun buyHouse(fieldId: Int) {
        if (!guardAction("buyHouse", actionGates.value.canManageProperties)) return
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.buyHouse(fieldId)
    }

    fun buyHotel(fieldId: Int) {
        if (!guardAction("buyHotel", actionGates.value.canManageProperties)) return
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.buyHotel(fieldId)
    }

    fun sellHotel(fieldId: Int) {
        if (!guardAction("sellHotel", actionGates.value.canManageProperties)) return
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.sellHotel(fieldId)
    }

}
