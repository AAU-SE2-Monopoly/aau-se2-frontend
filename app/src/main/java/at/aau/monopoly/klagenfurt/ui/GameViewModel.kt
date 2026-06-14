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
import kotlinx.coroutines.flow.merge
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

    enum class TurnPresentationPhase {
        IDLE,
        WAITING_FOR_ROLL_INPUT,
        ROLLING_DICE,
        SHOWING_DICE_RESULT,
        MOVING_TOKEN,
        REVEALING_LANDING_EFFECT,
        READY_FOR_ACTION
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
        val canRollAgainAfterDouble: Boolean = false,
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
        val cardDrawInFlight: Boolean,
        val actionExecutionInFlight: Boolean,
        val bankruptcyConfirm: Boolean,
        val buildingPending: Boolean,
        val doubleRollAdvanceInFlight: Boolean,
        val reportCheaterInFlight: Boolean,
        val tradeActionInFlight: Boolean
    )

    private data class PresentationPath(
        val playerId: String,
        val startPosition: Int,
        val path: List<Int>,
        val hardSyncOnly: Boolean = false
    )

    private val objectMapper = JacksonProvider.objectMapper

    private val localGameEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 1)

    private val gameEventFlow: SharedFlow<GameEvent> = merge(
        gameService.events
            .mapNotNull { jsonString ->
                try {
                    objectMapper.readValue(jsonString, GameEvent::class.java)
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Parsing error: ${e.message}", e)
                    null
                }
            },
        localGameEvents
    )
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
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

    private val _cardDrawInFlight = MutableStateFlow(false)

    private val _doubleRollAdvanceInFlight = MutableStateFlow(false)

    private val _reportCheaterInFlight = MutableStateFlow(false)

    private val _tradeActionInFlight = MutableStateFlow(false)
    val tradeActionInFlight: StateFlow<Boolean> = _tradeActionInFlight.asStateFlow()

    private var paymentActionToken: Long = 0
    private var propertyActionToken: Long = 0
    private var cardDrawActionToken: Long = 0
    private var actionExecutionToken: Long = 0
    private var doubleRollAdvanceToken: Long = 0
    private var reportCheaterActionToken: Long = 0
    private var tradeActionToken: Long = 0
    private var tradeUiToken: Long = 0

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

    private fun startCardDrawAction() {
        val token = ++cardDrawActionToken
        _cardDrawInFlight.value = true
        viewModelScope.launch {
            delay(ACTION_TIMEOUT_MS)
            if (cardDrawActionToken == token) {
                _cardDrawInFlight.value = false
            }
        }
    }

    private fun finishCardDrawAction() {
        _cardDrawInFlight.value = false
    }

    private fun startActionExecution() {
        val token = ++actionExecutionToken
        _isExecutingAction.value = true
        viewModelScope.launch {
            delay(ACTION_TIMEOUT_MS)
            if (actionExecutionToken == token) {
                _isExecutingAction.value = false
            }
        }
    }

    private fun finishActionExecution() {
        _isExecutingAction.value = false
    }

    private fun startDoubleRollAdvance() {
        val token = ++doubleRollAdvanceToken
        _doubleRollAdvanceInFlight.value = true
        viewModelScope.launch {
            delay(ACTION_TIMEOUT_MS)
            if (doubleRollAdvanceToken == token) {
                _doubleRollAdvanceInFlight.value = false
                rollAfterDoubleAdvancePending = false
            }
        }
    }

    private fun finishDoubleRollAdvance() {
        _doubleRollAdvanceInFlight.value = false
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

    private fun startTradeAction() {
        val token = ++tradeActionToken
        _tradeActionInFlight.value = true
        viewModelScope.launch {
            delay(TRADE_ACTION_TIMEOUT_MS)
            if (tradeActionToken == token) {
                _tradeActionInFlight.value = false
            }
        }
    }

    private fun finishTradeAction() {
        _tradeActionInFlight.value = false
    }

    private fun startTradeUiTimeout() {
        val token = ++tradeUiToken
        viewModelScope.launch {
            delay(TRADE_UI_TIMEOUT_MS)
            if (tradeUiToken == token && gameState.value?.pendingTradeOffer == null) {
                _selectedPlayerForTrade.value = null
            }
        }
    }

    private val _currentActionCard = MutableStateFlow<Card?>(null)
    val currentActionCard: StateFlow<Card?> = _currentActionCard.asStateFlow()

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
    val eventLog: StateFlow<List<LogEntry>> = presentedEventLog

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
    private var timedOutRollPlayerId: String? = null

    val gameState: StateFlow<GameState?> = gameEventFlow
        .runningFold<GameEvent, GameState?>(null) { lastState, event ->
            if (event.event == LOCAL_GAME_SWITCH_EVENT) {
                return@runningFold null
            }

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
        _cardDrawInFlight,
        _isExecutingAction,
        _showBankruptcyConfirmation,
        _buildingActionPending,
        _doubleRollAdvanceInFlight,
        _reportCheaterInFlight,
        _tradeActionInFlight
    ) { values ->
        ActionGateLocks(
            paymentInFlight = values[0],
            propertyInFlight = values[1],
            cardDrawInFlight = values[2],
            actionExecutionInFlight = values[3],
            bankruptcyConfirm = values[4],
            buildingPending = values[5],
            doubleRollAdvanceInFlight = values[6],
            reportCheaterInFlight = values[7],
            tradeActionInFlight = values[8]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ActionGateLocks(false, false, false, false, false, false, false, false, false)
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
        .map { card -> card != null }
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
    private var rollAfterDoubleAdvancePending = false


    fun connect() = gameService.connect()

    fun createGame(playerName: String) {
        viewModelScope.launch {
            gameService.createGame(playerName)
        }
    }

    fun joinGame(gameId: String, playerName: String) {
        viewModelScope.launch {
            val result = gameService.joinGame(gameId, playerName)
            if (result.isFailure) {
                resetForGameSwitch(gameService.currentGameId)
                if (gameService.currentGameId.isNotBlank()) {
                    gameService.requestState()
                }
            }
        }
    }

    fun startGame() = gameService.startGame()

    private fun guardAction(actionName: String, canRun: Boolean): Boolean {
        val state = gameState.value
        if (state == null) {
            Log.d("GameViewModel", "$actionName ignored before game state is synced")
            return false
        }

        val currentGameId = gameService.currentGameId
        if (
            currentGameId.isNotBlank() &&
            state.gameId.isNotBlank() &&
            state.gameId != currentGameId
        ) {
            Log.d("GameViewModel", "$actionName ignored for stale game state ${state.gameId}")
            return false
        }

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
        val currentPhase = gameState.value?.phase
        if (currentPhase != null && currentPhase != GamePhase.ROLLING) {
            Log.d("GameViewModel", "rollDice ignored outside ROLLING phase")
            return
        }
        startRollRequest()
    }

    private fun startRollRequest() {
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
                if (_activeDicePresentation.value?.isRolling == true) {
                    timedOutRollPlayerId = gameState.value?.currentPlayer
                        ?.id
                        ?.takeIf { it == gameService.currentPlayerId }
                    _activeDicePresentation.value = null
                    _presentationPhase.value = phaseFromRawState(gameState.value)
                }
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

    fun rollAgainAfterDouble() {
        if (!guardAction("rollAgainAfterDouble", actionGates.value.canRollAgainAfterDouble)) return
        if (_doubleRollAdvanceInFlight.value) return
        rollAfterDoubleAdvancePending = true
        startDoubleRollAdvance()
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
        if (!guardPropertySpendAction("unmortgageProperty")) return
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
        if (gameState.value == null || _visiblePaymentState.value == null) {
            Log.d("GameViewModel", "showPayRentOverlay ignored — state=${gameState.value != null} payment=${_visiblePaymentState.value != null}")
            return
        }
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
        clearVisibleBankruptcy()
    }

    fun acceptBankruptcyResolution() {
        _showBankruptcyOverlay.value = false
        clearVisibleBankruptcy()
    }

    fun requestState() = gameService.requestState()

    fun setGameId(gameId: String) {
        if (gameId != gameService.currentGameId) {
            resetForGameSwitch(gameId)
        }
        gameService.setGameId(gameId)
        gameService.subscribeToGame(gameId)
    }

    fun reportCheater(reportedPlayerId: String) {
        val state = gameState.value ?: return
        if (state.currentPlayer?.id != reportedPlayerId || state.lastDiceRoll == null) return
        val reportedPlayer = state.players.find { it.id == reportedPlayerId }
        if (reportedPlayer?.isBankrupt() == true) return
        if (!guardAction("reportCheater", actionGates.value.canReportCheater)) return
        if (_reportCheaterInFlight.value) return
        startReportCheaterAction()
        gameService.reportCheater(reportedPlayerId)
    }

    private fun guardPropertySpendAction(actionName: String): Boolean {
        if (!guardAction(actionName, actionGates.value.canManageProperties)) return false
        if (_visiblePaymentState.value != null || gameState.value?.pendingPayment != null) {
            Log.d("GameViewModel", "$actionName ignored while payment is pending")
            return false
        }
        return true
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
        if (_cardDrawInFlight.value) return
        startCardDrawAction()
        gameService.drawCard(cardType)
    }

    fun executeAction() {
        if (!guardAction("executeAction", actionGates.value.canExecuteCard)) return
        if (_isExecutingAction.value) return
        startActionExecution()
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
        val me = gameState.value?.players?.find { it.id == currentPlayerId }
        if (me?.isBankrupt() == true || player.isBankrupt()) return

        _selectedPlayerForOverlay.value = null
        _selectedPlayerForTrade.value = player
        startTradeUiTimeout()
    }

    fun hideTradeOverlay() {
        tradeUiToken++
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
        val canStartOrUpdateTrade = actionGates.value.canTrade || gameState.value?.pendingTradeOffer != null
        if (!guardAction("proposeTrade", canStartOrUpdateTrade)) return
        val state = gameState.value
        if (state != null) {
            val me = state.players.find { it.id == currentPlayerId }
            val tradePartner = state.players.find { it.id == toPlayerId }
            if (me == null ||
                tradePartner == null ||
                me.isBankrupt() ||
                tradePartner.isBankrupt()
            ) return
        }

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
        if (_tradeActionInFlight.value) return
        startTradeAction()
        gameService.acceptTrade(tradeId)
    }

    fun rejectTrade(tradeId: String) {
        if (!guardAction("rejectTrade", actionGates.value.canTrade || gameState.value?.pendingTradeOffer != null)) return
        if (_tradeActionInFlight.value) return
        startTradeAction()
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
        val rawBankruptcyPending =
            state.phase == GamePhase.BANKRUPTCY || state.bankruptcyPlayerId.isNotBlank()
        val hasVisibleBlockingOverlay =
            payment != null ||
                    base.visibleActionCard != null ||
                    base.visibleBankruptcy != null ||
                    locks.bankruptcyConfirm
        val hasBlockingState = hasVisibleBlockingOverlay || rawBankruptcyPending
        val currentField = base.visibleCurrentField
        val isBuyableField = currentField is OwnableField
        val isUnownedField = (currentField as? OwnableField)?.ownerId == null
        val isOnChance = currentField is ChanceField
        val isOnCommunityChest = currentField is CommunityChestField
        val canRoll = state.phase == GamePhase.ROLLING &&
                isCurrentPlayer &&
                rollInputReady &&
                !rawBankruptcyPending &&
                !rollRequestInFlight
        val doubleRollPending = state.lastDiceRoll?.isDouble == true &&
                isCurrentPlayer &&
                (state.phase == GamePhase.BUYING || state.phase == GamePhase.TURN_END)
        val canRollAgainAfterDouble = doubleRollPending &&
                presentationReady &&
                !hasBlockingState &&
                state.pendingPayment == null &&
                !locks.doubleRollAdvanceInFlight
        val canRollDice = canRoll
        val canUseJailAction = canRoll &&
                currentPlayer?.inJail == true &&
                !locks.paymentInFlight
        val canEndTurn = (state.phase == GamePhase.BUYING || state.phase == GamePhase.TURN_END) &&
                isCurrentPlayer &&
                presentationReady &&
                !hasBlockingState &&
                state.pendingPayment == null &&
                !doubleRollPending
        val canBuyProperty = state.phase == GamePhase.BUYING &&
                isCurrentPlayer &&
                presentationReady &&
                !hasBlockingState &&
                isBuyableField &&
                isUnownedField
        val canDrawChance = state.phase == GamePhase.BUYING &&
                isCurrentPlayer &&
                presentationReady &&
                !hasBlockingState &&
                isOnChance &&
                !state.hasDrawnCardThisTurn &&
                !locks.cardDrawInFlight
        val canDrawCommunityChest = state.phase == GamePhase.BUYING &&
                isCurrentPlayer &&
                presentationReady &&
                !hasBlockingState &&
                isOnCommunityChest &&
                !state.hasDrawnCardThisTurn &&
                !locks.cardDrawInFlight
        val canExecuteCard = isCurrentPlayer &&
                presentationReady &&
                base.visibleActionCard != null &&
                !rawBankruptcyPending &&
                !locks.actionExecutionInFlight
        val canPayRent = isCurrentPlayer &&
                presentationReady &&
                payment != null &&
                payment.source != PaymentSource.TAX &&
                !rawBankruptcyPending &&
                !locks.paymentInFlight &&
                (localPlayer?.money ?: 0) >= payment.amount
        val canPayTax = isCurrentPlayer &&
                presentationReady &&
                payment?.source == PaymentSource.TAX &&
                !rawBankruptcyPending &&
                !locks.paymentInFlight &&
                (localPlayer?.money ?: 0) >= payment.amount
        val paymentForCurrentPlayer = payment != null && isCurrentPlayer
        val canManageProperties = localPlayerActive &&
                !rawBankruptcyPending &&
                !locks.propertyInFlight &&
                !locks.buildingPending &&
                (payment == null || paymentForCurrentPlayer)
        val canDeclareBankruptcy = isCurrentPlayer &&
                presentationReady &&
                payment != null &&
                !rawBankruptcyPending &&
                !locks.paymentInFlight &&
                !locks.bankruptcyConfirm
        val canTrade = localPlayerActive &&
                isCurrentPlayer &&
                presentationReady &&
                !hasBlockingState &&
                !locks.tradeActionInFlight &&
                state.pendingTradeOffer == null
        val currentPlayerRolled = state.lastDiceRoll != null
        val canReport = localPlayerActive &&
                !isCurrentPlayer &&
                currentPlayerRolled &&
                presentationReady &&
                !rawBankruptcyPending &&
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
            canRollAgainAfterDouble = canRollAgainAfterDouble,
            canReportCheater = canReport
        )
    }

    private fun handleIncomingGameEvent(event: GameEvent) {
        if (event.event == LOCAL_GAME_SWITCH_EVENT) {
            previousGameState = null
            hardSyncPresentation(null)
            _presentedEventLog.value = emptyList()
            _showGameOverOverlay.value = false
            _hostEndedGame.value = false
            return
        }

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
                val timedOutPlayerId = timedOutRollPlayerId
                val activeDice = _activeDicePresentation.value
                if (
                    timedOutPlayerId != null &&
                    (oldState?.currentPlayer?.id == timedOutPlayerId ||
                            event.gameState?.currentPlayer?.id == timedOutPlayerId)
                ) {
                    timedOutRollPlayerId = null
                    Log.d("GameViewModel", "Hard syncing late DICE_ROLLED for timed-out local roll")
                    hardSyncPresentation(event.gameState, event)
                    appendPresentedLog(event)
                } else if (activeDice != null && !activeDice.isRolling && presentationJob?.isActive == true) {
                    Log.d("GameViewModel", "Ignoring stale DICE_ROLLED while dice result already shown")
                    appendOrBufferPresentedLog(event)
                } else {
                    timedOutRollPlayerId = null
                    startTurnPresentation(event, oldState, event.gameState, includeDice = true)
                }
            }

            event.event == "ACTION_EXECUTED" -> {
                val wasExecutingAction = _isExecutingAction.value
                finishActionExecution()
                val card = if (wasExecutingAction) {
                    _currentActionCard.value ?: oldState?.currentActionCard
                } else {
                    oldState?.currentActionCard
                }
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
                finishCardDrawAction()
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
                            revealBankruptcyState(state)
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
                preservePendingPaymentForRetry(event.gameState ?: gameState.value)
                appendPresentedLog(event)
            }

            event.gameState?.pendingPayment != null -> {
                if (isPresentationBlocking()) {
                    bufferPresentedLog(event)
                } else {
                    revealLandingForState(event.gameState)
                    appendPresentedLog(event)
                }
            }

            event.event == "TURN_ENDED" -> {
                rollRequestInFlight = false
                _buildingActionPending.value = false
                finishCardDrawAction()
                finishDoubleRollAdvance()
                lastCurrentPlayerIdForCardDraw = null
                clearVisiblePayment()
                _visibleActionCard.value = null
                _currentActionCard.value = null
                hardSyncPresentation(event.gameState, event, cancelJob = true)
                appendPresentedLog(event)
                startQueuedDoubleRollIfReady(event.gameState)
            }

            event.event in PROPERTY_BUILD_TRADE_EVENTS -> {
                if (event.event == "TRADE_COMPLETED" || event.event == "TRADE_REJECTED") {
                    finishTradeAction()
                    tradeUiToken++
                    _selectedPlayerForTrade.value = null
                    gameService.requestState()
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
                    timedOutRollPlayerId = null
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
        _visibleCurrentField.value = visiblePosition?.let { position ->
            state.fields.firstOrNull { it.id == position } ?: state.fields.getOrNull(position)
        }

        if (state.currentActionCard == null) {
            _currentActionCard.value = null
            _visibleActionCard.value = null
        } else {
            _currentActionCard.value = state.currentActionCard
            _visibleActionCard.value = state.currentActionCard
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
        rollAfterDoubleAdvancePending = false
        timedOutRollPlayerId = null
        _isExecutingAction.value = false
        showTransientError(event.message ?: "An unknown error occurred")
        finishPaymentAction()
        finishPropertyAction()
        finishCardDrawAction()
        finishDoubleRollAdvance()
        finishReportCheaterAction()
        finishTradeAction()
        if (isPresentationBlocking()) {
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

        bufferPresentedLog(event)
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
                    flushBufferedPresentedLogs { it.eventType == "DICE_ROLLED" }
                }
            }

            if (movementPath.path.isNotEmpty()) {
                _presentationPhase.value = TurnPresentationPhase.MOVING_TOKEN
                runMovementPresentation(newState, movementPath)
            } else {
                _movementAnimation.value = null
            }

            if (presentationSequenceId != sequenceId) return@launch

            val landingState = previousGameState ?: newState
            _presentedBoardPlayers.value = copyPlayersForPresentation(landingState.players)
            _visibleCurrentField.value = currentFieldForState(landingState)
            _presentationPhase.value = TurnPresentationPhase.REVEALING_LANDING_EFFECT
            revealLandingForState(landingState)
            flushBufferedPresentedLogs()
            delay(LANDING_REVEAL_PRESENTATION_MS)

            if (presentationSequenceId == sequenceId) {
                _activeDicePresentation.value = null
                _presentationPhase.value = phaseFromRawState(landingState)
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
        flushBufferedPresentedLogs()
        clearPresentationBuffers()
        renderRawState(state)
        _presentationPhase.value = phaseFromRawState(state)
        rollRequestInFlight = false
        clearTimedOutRollIfStateAdvanced(state)
        finishPaymentAction()
        finishPropertyAction()
        finishCardDrawAction()
        finishDoubleRollAdvance()
        finishReportCheaterAction()
        finishTradeAction()

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
            clearVisibleBankruptcy()
            _showBankruptcyOverlay.value = false
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
            clearVisibleBankruptcy()
            _showBankruptcyOverlay.value = false
        }
        _presentationPhase.value = phaseFromRawState(state)
    }

    private fun startQueuedDoubleRollIfReady(state: GameState?) {
        if (!rollAfterDoubleAdvancePending) return

        val canStartQueuedRoll =
            state?.phase == GamePhase.ROLLING &&
                    state.currentPlayer?.id == gameService.currentPlayerId

        rollAfterDoubleAdvancePending = false
        if (canStartQueuedRoll) {
            startRollRequest()
        }
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

    private fun clearVisibleBankruptcy() {
        _visibleBankruptcyState.value = null
        _bankruptcyPlayerId.value = ""
        _bankruptcyPlayerName.value = ""
        _bankruptcyTotalAssets.value = 0
        _bankruptcyTotalDebt.value = 0
        _bankruptcyPropertiesOwned.value = emptyList()
    }

    private fun clearVisiblePayment() {
        _showPayRentOverlay.value = false
        _visiblePaymentState.value = null
    }

    private fun clearTimedOutRollIfStateAdvanced(state: GameState?) {
        val timedOutPlayerId = timedOutRollPlayerId ?: return
        if (
            state == null ||
            state.currentPlayer?.id != timedOutPlayerId ||
            state.phase != GamePhase.ROLLING
        ) {
            timedOutRollPlayerId = null
        }
    }

    private fun preservePendingPaymentForRetry(state: GameState?) {
        val pending = state?.pendingPayment
        if (pending == null || pending.amount <= 0) {
            clearVisiblePayment()
            return
        }

        _hasPendingPayment.value = true
        _currentRentAmount.value = pending.amount
        _currentRentOwnerId.value = pending.creditorPlayerId
        _currentRentFieldId.value = pending.sourceFieldId
        _lastDiceTotalForRent.value = state.lastDiceRoll?.total ?: 0
        _visiblePaymentState.value = VisiblePaymentState(pending)
        _showPayRentOverlay.value = false
        lastPendingPaymentKey = "${pending.source}:${pending.sourceFieldId}:${pending.amount}:${pending.creditorPlayerId}"
    }

    private fun resetForGameSwitch(gameId: String) {
        localGameEvents.tryEmit(GameEvent(gameId = gameId, event = LOCAL_GAME_SWITCH_EVENT))
        previousGameState = null
        rollRequestInFlight = false
        rollAfterDoubleAdvancePending = false
        _isExecutingAction.value = false
        finishPaymentAction()
        finishPropertyAction()
        finishCardDrawAction()
        finishDoubleRollAdvance()
        finishReportCheaterAction()
        finishTradeAction()
        hardSyncPresentation(null)
        _presentedEventLog.value = emptyList()
        _showGameOverOverlay.value = false
        _hostEndedGame.value = false
    }

    private fun clearPresentationBuffers() {
        bufferedPresentedLogs.clear()
        _activeDicePresentation.value = null
        _visibleActionCard.value = null
        _currentActionCard.value = null
        clearVisiblePayment()
        _showMortgageOverlay.value = false
        _showBankruptcyConfirmation.value = false
        clearVisibleBankruptcy()
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
        return state.fields.firstOrNull { it.id == player.position }
            ?: state.fields.getOrNull(player.position)
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

    private fun flushBufferedPresentedLogs(shouldFlush: (LogEntry) -> Boolean = { true }) {
        if (bufferedPresentedLogs.isEmpty()) return
        val logsToFlush = bufferedPresentedLogs.filter(shouldFlush)
        if (logsToFlush.isEmpty()) return
        bufferedPresentedLogs.removeAll(logsToFlush)
        _presentedEventLog.value = (_presentedEventLog.value + logsToFlush)
            .takeLast(MAX_LOG_ENTRIES)
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
        private const val TRADE_ACTION_TIMEOUT_MS = 20_000L
        private const val TRADE_UI_TIMEOUT_MS = 60_000L
        private const val LOCAL_GAME_SWITCH_EVENT = "LOCAL_GAME_SWITCH"
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
        if (!guardPropertySpendAction("buyHouse")) return
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.buyHouse(fieldId)
    }

    fun buyHotel(fieldId: Int) {
        if (!guardPropertySpendAction("buyHotel")) return
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
