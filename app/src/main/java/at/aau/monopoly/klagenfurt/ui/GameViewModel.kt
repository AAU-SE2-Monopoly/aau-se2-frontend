package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.card.Card
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
import at.aau.monopoly.klagenfurt.ui.board.computeMovementPath
import at.aau.monopoly.klagenfurt.ui.util.ownerIdFromField
import at.aau.monopoly.klagenfurt.ui.util.toManageableProperty
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    private var paymentActionToken: Long = 0
    private var propertyActionToken: Long = 0

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

    private val _currentActionCard = MutableStateFlow<Card?>(null)
    val currentActionCard: StateFlow<Card?> = _currentActionCard.asStateFlow()

    private val _isExecutingAction = MutableStateFlow(false)
    val isExecutingAction: StateFlow<Boolean> = _isExecutingAction.asStateFlow()

    private val _selectedPlayerForOverlay = MutableStateFlow<Player?>(null)
    val selectedPlayerForOverlay: StateFlow<Player?> = _selectedPlayerForOverlay.asStateFlow()

    private val _pendingDoubleAutoEnd = MutableStateFlow(false)
    val pendingDoubleAutoEnd: StateFlow<Boolean> = _pendingDoubleAutoEnd.asStateFlow()

    private var lastCurrentPlayerIdForCardDraw: String? = null

    private val _buildingActionPending = MutableStateFlow(false)
    val buildingActionPending: StateFlow<Boolean> = _buildingActionPending.asStateFlow()

    init {
        gameEventFlow
            .onEach { event ->
                if (
                    event.event == "GAME_CREATED" &&
                    event.gameId.isNotBlank() &&
                    gameService.currentGameId.isBlank()
                ) {
                    gameService.setGameId(event.gameId)
                }

                // Capture old state before updating, then remember the new state.
                val oldState = previousGameState
                event.gameState?.let { previousGameState = it }

                // Detect position changes on DICE_ROLLED events and drive animation.
                if (event.event == "DICE_ROLLED") {
                    val newState = event.gameState ?: return@onEach

                    if (oldState != null) {
                        val currentPlayerId = newState.currentPlayer?.id ?: return@onEach
                        val prevPlayer =
                            oldState.players.find { it.id == currentPlayerId } ?: return@onEach
                        val newPlayer =
                            newState.players.find { it.id == currentPlayerId } ?: return@onEach

                        if (prevPlayer.position != newPlayer.position) {
                            val diceTotal = newState.lastDiceRoll?.total
                                ?: ((newPlayer.position - prevPlayer.position + newState.fields.size) % newState.fields.size)
                            val path = computeMovementPath(
                                prevPlayer.position,
                                diceTotal,
                                newState.fields.size
                            )

                            animationJob?.cancel()
                            animationJob = viewModelScope.launch {
                                _movementAnimation.value = MovementAnimationState(
                                    playerId = currentPlayerId,
                                    startPosition = prevPlayer.position,
                                    path = path,
                                    currentStepIndex = -1,
                                    isComplete = false
                                )
                                path.forEachIndexed { stepIdx, _ ->
                                    delay(250)
                                    _movementAnimation.value = _movementAnimation.value?.copy(
                                        currentStepIndex = stepIdx
                                    )
                                }
                                _movementAnimation.value = _movementAnimation.value?.copy(
                                    currentStepIndex = path.size,
                                    isComplete = true
                                )
                            }
                        }
                    }
                }

                if (event.event == "ACTION_DRAWN" && event.gameState?.currentActionCard != null) {
                    _currentActionCard.value = event.gameState.currentActionCard

                    lastCurrentPlayerIdForCardDraw = event.gameState.currentPlayer?.id
                }

                if (event.event == "ACTION_EXECUTED") {
                    _currentActionCard.value = null
                    _isExecutingAction.value = false
                }

                if (
                    event.event == "HOUSE_BOUGHT" ||
                    event.event == "HOTEL_BOUGHT" ||
                    event.event == "HOUSE_SOLD" ||
                    event.event == "HOTEL_SOLD" ||
                    event.event == "ERROR"
                ) {
                    _buildingActionPending.value = false
                }

                if (event.event == "TURN_ENDED") {
                    _buildingActionPending.value = false
                    _pendingDoubleAutoEnd.value = false
                    lastCurrentPlayerIdForCardDraw = null
                }

                // Track doubles for auto-end after dice overlay closes
                if (event.event == "DICE_ROLLED") {
                    val state = event.gameState
                    val diceRoll = state?.lastDiceRoll
                    if (diceRoll != null && diceRoll.isDouble &&
                        state.currentPlayer?.id == gameService.currentPlayerId
                    ) {
                        _pendingDoubleAutoEnd.value = true
                    }
                }



                if (event.event == "ERROR") {
                    showTransientError(event.message ?: "An unknown error occurred")
                    finishPaymentAction()
                    finishPropertyAction()
                }

                // reset overlay states on GAME_STARTED
                if (event.event == "GAME_STARTED") {
                    _showPayRentOverlay.value = false
                    _showMortgageOverlay.value = false
                    _showBankruptcyOverlay.value = false
                    _currentRentAmount.value = 0
                    _currentRentOwnerId.value = null
                    _currentRentFieldId.value = null
                    _bankruptcyPlayerName.value = ""
                    _bankruptcyTotalAssets.value = 0
                    _bankruptcyTotalDebt.value = 0
                    _bankruptcyPropertiesOwned.value = emptyList()
                }

                if (event.event == "RENT_PAID") {
                    finishPaymentAction()
                }

                if (event.event == "PAYMENT_FAILED") {
                    showTransientError(event.message ?: "Payment failed")
                    finishPaymentAction()
                }

                if (event.event == "BANKRUPTCY_DECLARED") {
                    val gs = event.gameState
                    _bankruptcyPlayerId.value = gs?.bankruptcyPlayerId ?: ""
                    _bankruptcyPlayerName.value = gs?.players?.find { it.id == gs.bankruptcyPlayerId }?.name ?: ""
                    _bankruptcyTotalAssets.value = gs?.bankruptcyTotalAssets ?: 0
                    _bankruptcyTotalDebt.value = gs?.bankruptcyTotalDebt ?: 0
                    _bankruptcyPropertiesOwned.value = gs?.let { state ->
                        val allFields = state.fields
                        allFields
                            .filter { it.id in state.bankruptcyOwnedFieldIds }
                            .filter { it is PropertyField || it is RailroadField || it is UtilityField }
                            .map { field -> field.toManageableProperty(allFields) }
                    } ?: emptyList()
                    _showBankruptcyOverlay.value = true
                    finishPaymentAction()
                }

                // handle previously unhandled backend events
                if (event.event == "PROPERTY_MORTGAGED") {
                    Log.i("GameViewModel", "Property mortgaged - refreshing state")
                    finishPropertyAction()
                }

                if (event.event == "PROPERTY_UNMORTGAGED") {
                    Log.i("GameViewModel", "Property unmortgaged - refreshing state")
                    finishPropertyAction()
                }

                if (event.event == "HOUSE_BOUGHT" || event.event == "HOTEL_BOUGHT" ||
                    event.event == "HOUSE_SOLD" || event.event == "HOTEL_SOLD") {
                    Log.i("GameViewModel", "Building action completed - refreshing state")
                    finishPropertyAction()
                }
            }
            .launchIn(viewModelScope)
    }

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

    // Payment overlay state flows — must be declared before canEndTurnForCurrentPlayer
    private val _showPayRentOverlay = MutableStateFlow(false)
    val showPayRentOverlay: StateFlow<Boolean> = _showPayRentOverlay.asStateFlow()

    private val _hasPendingPayment = MutableStateFlow(false)
    val hasPendingPayment: StateFlow<Boolean> = _hasPendingPayment.asStateFlow()

    private val _showBankruptcyOverlay = MutableStateFlow(false)
    val showBankruptcyOverlay: StateFlow<Boolean> = _showBankruptcyOverlay.asStateFlow()
    private val _showBankruptcyConfirmation = MutableStateFlow(false)
    val showBankruptcyConfirmation: StateFlow<Boolean> = _showBankruptcyConfirmation.asStateFlow()

    val canEndTurnForCurrentPlayer: StateFlow<Boolean> =
        combine(gameState, _showPayRentOverlay, _showBankruptcyOverlay, _showBankruptcyConfirmation, _hasPendingPayment) { state, payOverlay, bankruptcyOverlay, bankruptcyConfirm, hasPending ->
            (state?.phase == GamePhase.BUYING ||
                    state?.phase == GamePhase.TURN_END) &&
                    state?.currentPlayer?.id == gameService.currentPlayerId &&
                    !payOverlay &&
                    !bankruptcyOverlay &&
                    !bankruptcyConfirm &&
                    !hasPending
        }
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

    val showActionCardOverlay: StateFlow<Boolean> = currentActionCard
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

    // store last dice total for utility rent calculation
    private val _lastDiceTotalForRent = MutableStateFlow(0)

    private var lastPendingPaymentKey: String? = null

    init {
        gameState
            .onEach { state -> updatePendingPaymentState(state) }
            .launchIn(viewModelScope)
    }

    // Whether the player has enough cash right now (enables Pay Rent button)
    val canPayRent: StateFlow<Boolean> = combine(
        gameState, _currentRentAmount
    ) { state, rentAmount ->
        val amount = rentAmount
        val player = state?.players?.find { it.id == gameService.currentPlayerId }
        if (player == null || amount <= 0) false
        else player.money >= amount
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Whether total assets (cash + mortgage value + building sellback) cover the rent,
    // computed by the backend to avoid duplicated logic drift.
    // If false, bankruptcy is the only option.
    val canRaiseFunds: StateFlow<Boolean> = gameState
        .map { state ->
            state?.pendingPayment?.debtorCanPayAfterAssets ?: false
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // m4: Manageable properties for mortgage management overlay — includes RailroadField/UtilityField
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

    // Bankruptcy overlay state flows
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
    private var lastDiceRollTimestamp = 0L

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

    fun activateCheatForNextRoll() {
        isCheatActive = true
    }

    fun rollDice() {
        val now = currentTimeProvider()

        if (now - lastDiceRollTimestamp < 1500L) return

        lastDiceRollTimestamp = now
        gameService.rollDice(isCheating = isCheatActive)
        isCheatActive = false
    }

    fun endTurn() = gameService.endTurn()

    fun consumeDoubleAutoEnd() {
        if (!_pendingDoubleAutoEnd.value) return
        if (_pendingDoubleAutoEnd.compareAndSet(expect = true, update = false)) {
            gameService.endTurn()
        }
    }

    fun payJailFine() = gameService.payJailFine()
    fun useJailCard() = gameService.useJailCard()

    // Payment/mortgage/bankrupcty
    fun payRent() {
        Log.d("GameViewModel", "payRent() called, inFlight=${_paymentActionInFlight.value}, fieldId=${currentRentFieldId.value}, money=${(gameState.value?.players?.find { it.id == gameService.currentPlayerId }?.money)}")
        if (_paymentActionInFlight.value) return
        val fieldId = currentRentFieldId.value
        val diceTotal = _lastDiceTotalForRent.value
        startPaymentAction()
        gameService.payRent(fieldId, diceTotal)
        // wait for RENT_PAID event before dismissing
    }

    fun mortgageProperty(fieldId: Int) {
        if (_propertyActionInFlight.value) return
        startPropertyAction()
        gameService.mortgageProperty(fieldId)
    }

    fun unmortgageProperty(fieldId: Int) {
        if (_propertyActionInFlight.value) return
        startPropertyAction()
        gameService.unmortgageProperty(fieldId)
    }

    fun sellHouse(fieldId: Int) {
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.sellHouse(fieldId)
    }

    fun declareBankruptcy() {
        if (_paymentActionInFlight.value || _showBankruptcyConfirmation.value) return
        _showPayRentOverlay.value = false
        _showBankruptcyConfirmation.value = true
    }

    fun confirmDeclareBankruptcy() {
        if (_paymentActionInFlight.value) return
        _showBankruptcyConfirmation.value = false
        startPaymentAction()
        gameService.declareBankruptcy()
    }

    fun cancelDeclareBankruptcy() {
        _showBankruptcyConfirmation.value = false
        _showPayRentOverlay.value = true
    }

    /** DEBUG remove this block of code to remove */
    fun debugForwardGame() {
        gameService.debugForwardGame()
    }

    /** DEBUG remove this block of code to remove */
    fun debugSetupBankruptcy() {
        gameService.debugSetupBankruptcy()
    }

    fun showPayRentOverlay(
        amount: Int,
        ownerId: String?,
        fieldId: Int?
    ) {
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

    private fun updatePendingPaymentState(state: GameState?) {
        if (state == null) {
            Log.d("GameViewModel", "updatePendingPaymentState: state=null, clearing fieldId")
            _hasPendingPayment.value = false
            _showPayRentOverlay.value = false
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

        if (pendingKey != lastPendingPaymentKey) {
            Log.d("GameViewModel", "updatePendingPaymentState: new pendingKey=$pendingKey, showing overlay")
            _showPayRentOverlay.value = true
            lastPendingPaymentKey = pendingKey
        }
    }

    private val _movementAnimation = MutableStateFlow<MovementAnimationState?>(null)
    val movementAnimation: StateFlow<MovementAnimationState?> = _movementAnimation

    private var previousGameState: GameState? = null
    private var animationJob: Job? = null


    fun drawCard(cardType: String = "CHANCE") =
        gameService.drawCard(cardType)

    fun executeAction() {
        _isExecutingAction.value = true
        Log.d("ActionCard", "Executing action for player: $currentPlayerId")
        gameService.executeAction(currentPlayerId)
    }

    fun setCurrentActionCard(card: Card?) {
        Log.d("ActionCard", "Setting current action card: ${card?.description}")
        _currentActionCard.value = card
    }

    fun dismissActionCard() {
        _currentActionCard.value = null
    }

    fun showPlayerOverlay(player: Player) {
        _selectedPlayerForOverlay.value = player
    }

    fun hidePlayerOverlay() {
        _selectedPlayerForOverlay.value = null
    }

    fun syncGameboardEntryState() {
        val currentGameId = gameService.currentGameId

        if (currentGameId.isBlank()) return

        gameService.requestState()
    }

    private fun humanReadableEvent(eventType: String, gameId: String): String {
        return when (eventType) {
            "GAME_CREATED" -> "Game created: $gameId"
            "PLAYER_JOINED" -> "A new player joined"
            "GAME_STARTED" -> "Game started!"
            "DICE_ROLLED" -> "Dice rolled"
            "TURN_ENDED" -> "Turn ended"
            "STATE_UPDATED" -> "Game state updated"
            "STATE_SNAPSHOT" -> "State snapshot synced"


            "JAIL_FINE_PAID" -> "Bail paid: 50M"
            "JAIL_CARD_USED" -> "Used 'Get out of jail free' card"
            "PLAYER_JAILED" -> "Player went to jail!"
            "ACTION_DRAWN" -> "Action card drawn!"
            "ACTION_EXECUTED" -> "Action executed"
            "RENT_DUE" -> "Rent is due!"
            "RENT_PAID" -> "Rent paid"
            "PAYMENT_FAILED" -> "Payment failed"
            "BANKRUPTCY_DECLARED" -> "Player went bankrupt!"
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
    }

    fun buyProperty(fieldId: Int) {
        gameService.buyProperty(fieldId)
    }

    fun buyHouse(fieldId: Int) {
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.buyHouse(fieldId)
    }

    fun buyHotel(fieldId: Int) {
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.buyHotel(fieldId)
    }

    fun sellHotel(fieldId: Int) {
        if (_propertyActionInFlight.value || _buildingActionPending.value) return
        startPropertyAction()
        _buildingActionPending.value = true
        gameService.sellHotel(fieldId)
    }

}
