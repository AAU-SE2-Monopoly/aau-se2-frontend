package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.flow.MutableStateFlow
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
            replay = 80
        )

    // ---------------------------------------------------------------------------
    // Error handling – derived from the already-parsed gameEventFlow.
    // No extra parsing, no changes to GameStompClient.
    // ---------------------------------------------------------------------------
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
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

     // ============ ACTION CARD STATES (mutable) ============
     /**
      * Internal mutable state for current action card (set via ACTION_DRAWN events).
      */
     private val _currentActionCard = MutableStateFlow<Card?>(null)

     /**
      * Whether an action is currently being executed.
      */
     private val _isExecutingAction = MutableStateFlow(false)

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

                 // Handle ACTION_DRAWN events
                 if (event.event == "ACTION_DRAWN" && event.gameState?.currentActionCard != null) {
                     _currentActionCard.value = event.gameState.currentActionCard
                 }
             }
             .launchIn(viewModelScope)
     }
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
            }
            .launchIn(viewModelScope)

        gameEventFlow
            .onEach { event ->
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

    private var isCheatActive = false
    private var lastDiceRollTimestamp = 0L

    fun activateCheatForNextRoll() {
        isCheatActive = true
    }

    fun rollDice() {
        val now = currentTimeProvider()
        // Prevent double-fires from button tap + simultaneous shake sensor event
        if (now - lastDiceRollTimestamp < 1500L) return
        lastDiceRollTimestamp = now
        gameService.rollDice(isCheating = isCheatActive)
        isCheatActive = false
    }

    fun endTurn() = gameService.endTurn()

    fun requestState() = gameService.requestState()

    fun setGameId(gameId: String) = gameService.setGameId(gameId)

    fun drawCard(cardType: String = "CHANCE") = gameService.drawCard(cardType)

     private val _selectedPlayerForOverlay = kotlinx.coroutines.flow.MutableStateFlow<at.aau.monopoly.klagenfurt.model.Player?>(null)
     val selectedPlayerForOverlay: StateFlow<at.aau.monopoly.klagenfurt.model.Player?> = _selectedPlayerForOverlay

     fun showPlayerOverlay(player: at.aau.monopoly.klagenfurt.model.Player) {
         _selectedPlayerForOverlay.value = player
     }

     fun hidePlayerOverlay() {
         _selectedPlayerForOverlay.value = null
     }

      // ============ ACTION CARD STATES ============

      /**
       * Current action card being displayed.
       * Populated when ACTION_DRAWN event is received from backend.
       */
      val currentActionCard: StateFlow<Card?> = _currentActionCard.asStateFlow()

      /**
       * Public read-only access to action executing state.
       */
      val isExecutingAction: StateFlow<Boolean> = _isExecutingAction.asStateFlow()

      /**
       * Whether the action card overlay should be visible for the current player.
       */
      val showActionCardOverlay: StateFlow<Boolean> = currentActionCard
          .map { card ->
              val isCurrentPlayer = gameState.value?.currentPlayer?.id == gameService.currentPlayerId
              val hasCard = card != null
              isCurrentPlayer && hasCard
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

      /**
       * Execute the current action and notify the backend.
       */
      fun executeAction() {
          _isExecutingAction.value = true
          Log.d("ActionCard", "Executing action for player: $currentPlayerId")

          // Send action execution to backend
          gameService.executeAction(currentPlayerId)

          // Reset state after a short delay
          viewModelScope.launch {
              kotlinx.coroutines.delay(500)
              _isExecutingAction.value = false
              _currentActionCard.value = null
          }
      }

      /**
       * Set the current action card (used for backend updates).
       */
      fun setCurrentActionCard(card: Card?) {
          Log.d("ActionCard", "Setting current action card: ${card?.description}")
          _currentActionCard.value = card
      }

      fun dismissActionCard() {
          _currentActionCard.value = null
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
             "ACTION_DRAWN" -> "Action card drawn!"
             "ACTION_EXECUTED" -> "Action executed"
             else -> eventType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
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
}