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
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import at.aau.monopoly.klagenfurt.ui.board.MovementAnimationState
import at.aau.monopoly.klagenfurt.ui.board.computeMovementPath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _currentActionCard = MutableStateFlow<Card?>(null)
    val currentActionCard: StateFlow<Card?> = _currentActionCard.asStateFlow()

    private val _isExecutingAction = MutableStateFlow(false)
    val isExecutingAction: StateFlow<Boolean> = _isExecutingAction.asStateFlow()

    private val _selectedPlayerForOverlay = MutableStateFlow<Player?>(null)
    val selectedPlayerForOverlay: StateFlow<Player?> = _selectedPlayerForOverlay.asStateFlow()

    private val _chanceCardDrawnThisTurn = MutableStateFlow(false)
    val chanceCardDrawnThisTurn: StateFlow<Boolean> = _chanceCardDrawnThisTurn.asStateFlow()

    private val _communityChestCardDrawnThisTurn = MutableStateFlow(false)
    val communityChestCardDrawnThisTurn: StateFlow<Boolean> = _communityChestCardDrawnThisTurn.asStateFlow()

    private var lastCurrentPlayerIdForCardDraw: String? = null

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

                event.gameState?.let { state ->
                    _chanceCardDrawnThisTurn.value = state.hasDrawnChanceCardThisTurn
                    _communityChestCardDrawnThisTurn.value = state.hasDrawnCommunityChestCardThisTurn
                }

                // Detect position changes on DICE_ROLLED events and drive animation.
                if (event.event == "DICE_ROLLED") {
                    val newState = event.gameState ?: return@onEach

                    if (oldState != null) {
                        val currentPlayerId = newState.currentPlayer?.id ?: return@onEach
                        val prevPlayer = oldState.players.find { it.id == currentPlayerId } ?: return@onEach
                        val newPlayer = newState.players.find { it.id == currentPlayerId } ?: return@onEach

                        if (prevPlayer.position != newPlayer.position) {
                            val diceTotal = newState.lastDiceRoll?.total
                                ?: ((newPlayer.position - prevPlayer.position + newState.fields.size) % newState.fields.size)
                            val path = computeMovementPath(prevPlayer.position, diceTotal, newState.fields.size)

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

                    event.gameState.let { state ->
                        _chanceCardDrawnThisTurn.value = state.hasDrawnChanceCardThisTurn
                        _communityChestCardDrawnThisTurn.value = state.hasDrawnCommunityChestCardThisTurn
                    }

                    lastCurrentPlayerIdForCardDraw = event.gameState.currentPlayer?.id
                }

                if (event.event == "ACTION_EXECUTED") {
                    _currentActionCard.value = null
                    _isExecutingAction.value = false
                }

                if (event.event == "TURN_ENDED") {
                    _chanceCardDrawnThisTurn.value = false
                    _communityChestCardDrawnThisTurn.value = false
                    lastCurrentPlayerIdForCardDraw = null
                }



                if (event.event == "ERROR") {
                    _errorMessage.value = event.message ?: "An unknown error occurred"
                    delay(5_000)
                    _errorMessage.value = null
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

    val canEndTurnForCurrentPlayer: StateFlow<Boolean> = gameState
        .map { state ->
            (state?.phase == GamePhase.BUYING ||
                    state?.phase == GamePhase.TURN_END) &&
                    state.currentPlayer?.id == gameService.currentPlayerId
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

    fun payJailFine() = gameService.payJailFine()
    fun useJailCard() = gameService.useJailCard()


    fun requestState() = gameService.requestState()

    fun setGameId(gameId: String) = gameService.setGameId(gameId)

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
    }
    fun buyProperty(fieldId: Int) {
        gameService.buyProperty(fieldId)
    }

    fun buyHouse(fieldId: Int) {
        gameService.buyHouse(fieldId)
    }

    fun buyHotel(fieldId: Int) {
        gameService.buyHotel(fieldId)
    }

    fun sellHouse(fieldId: Int) {
        gameService.sellHouse(fieldId)
    }

    fun sellHotel(fieldId: Int) {
        gameService.sellHotel(fieldId)
    }
}
