package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(private val gameService: GameService) : ViewModel() {

    data class LogEntry(
        val text: String,
        val eventType: String,
        val isTechnical: Boolean = false,
        val timestampMs: Long = System.currentTimeMillis()
    )

    private data class LogAccumulator(
        val gameId: String,
        val entries: List<LogEntry>
    )

    private val objectMapper = JacksonProvider.objectMapper

    /**
     * Primary flow for game state changes. Low replay to avoid re-processing
     * old technical snapshots for current UI state.
     */
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

    /**
     * Dedicated log source from the networking layer. This keeps log replay
     * independent from state replay.
     */
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
            replay = 1
        )

    init {
        gameEventFlow
            .onEach { event ->
                // Only auto-capture the gameId if we don't have one yet.
                // This prevents replayed events from switching the game context unexpectedly.
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isGameReady: StateFlow<Boolean> = gameState
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isGameStarted: StateFlow<Boolean> = gameState
        .map { it?.phase != null && it.phase != GamePhase.WAITING }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isHost: StateFlow<Boolean> = gameState
        .map { state ->
            state?.players?.firstOrNull()?.id == gameService.currentPlayerId
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
    * Displays user-facing game events such as joining, creating a game, and rolling dice.
    *
    * State snapshots and other technical log entries are excluded from the normal log.
    * Technical logs are shown separately in the expanded chat bar view.
    */

    val eventLog: StateFlow<List<LogEntry>> = logEventFlow
        .runningFold(LogAccumulator(gameId = "", entries = emptyList())) { acc, event ->
            val eventGameId = event.gameId
            val incomingGameId = when {
                eventGameId.isNotBlank() -> eventGameId
                gameService.currentGameId.isNotBlank() -> gameService.currentGameId
                else -> acc.gameId
            }

            val shouldIgnore =
                event.event != "GAME_CREATED" &&
                incomingGameId.isNotBlank() &&
                    gameService.currentGameId.isNotBlank() &&
                    incomingGameId != gameService.currentGameId

            if (shouldIgnore) {
                acc
            } else {
                val isGameSwitch = incomingGameId.isNotBlank() &&
                        acc.gameId.isNotBlank() &&
                        incomingGameId != acc.gameId

                val baseEntries = if (isGameSwitch || event.event == "GAME_CREATED") emptyList() else acc.entries

                val isTechnical = event.event == "STATE_SNAPSHOT" || event.event == "STATE_UPDATED"
                val entryText = event.message?.takeIf { it.isNotBlank() }
                    ?: humanReadableEvent(event.event, event.gameId)

                if (entryText.isBlank()) {
                    LogAccumulator(gameId = incomingGameId, entries = baseEntries)
                } else {
                    val entry = LogEntry(
                        text = entryText,
                        eventType = event.event.ifBlank { "UNKNOWN" },
                        isTechnical = isTechnical
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

    //  Dice Roll states
    val lastDiceRoll: StateFlow<DiceRoll?> = gameState
        .map { it?.lastDiceRoll }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    val isRollingPhaseForCurrentPlayer: StateFlow<Boolean> = gameState
        .map { state ->
            state?.phase == GamePhase.ROLLING &&
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
            isCurrentPlayer && (isRollingPhase || hasResult)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val events: SharedFlow<String> = gameService.events
    val status: SharedFlow<String> = gameService.status
    val currentPlayerId: String get() = gameService.currentPlayerId

    fun connect() = gameService.connect()

    fun createGame(playerName: String) = gameService.createGame(playerName)

    fun joinGame(gameId: String, playerName: String) = gameService.joinGame(gameId, playerName)

    fun startGame() = gameService.startGame()

    // Use the simpler main-branch behavior for rolling the dice: just forward to service.
    private var isCheatActive = false

    // NEU: Wird von der Activity aufgerufen
    fun activateCheatForNextRoll() {
        isCheatActive = true
    }

    // GE�NDERT: rollDice gibt den Cheat-Status mit und setzt ihn dann zur�ck
    fun rollDice() {
        gameService.rollDice(isCheating = isCheatActive)
        isCheatActive = false // Cheat nach dem W�rfeln sofort wieder deaktivieren
    }


    fun endTurn() = gameService.endTurn()

    fun requestState() = gameService.requestState()

    fun setGameId(gameId: String) = gameService.setGameId(gameId)

    private val _movementAnimation = MutableStateFlow<MovementAnimationState?>(null)
    val movementAnimation: StateFlow<MovementAnimationState?> = _movementAnimation

    private var previousGameState: GameState? = null
    private var animationJob: Job? = null

    private val _selectedPlayerForOverlay = kotlinx.coroutines.flow.MutableStateFlow<at.aau.monopoly.klagenfurt.model.Player?>(null)
    val selectedPlayerForOverlay: StateFlow<at.aau.monopoly.klagenfurt.model.Player?> = _selectedPlayerForOverlay

    fun showPlayerOverlay(player: at.aau.monopoly.klagenfurt.model.Player) {
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
            else -> eventType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    // Factory to create ViewModel with dependencies
    class Factory(private val gameService: GameService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(gameService) as T
        }
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 80
    }
}
