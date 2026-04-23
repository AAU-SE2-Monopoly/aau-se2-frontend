package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
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

class GameViewModel(private val gameService: GameService) : ViewModel() {

    data class LogEntry(
        val text: String,
        val eventType: String,
        val timestampMs: Long = System.currentTimeMillis()
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

    init {
        gameEventFlow
            .onEach { event ->
                if (
                    event.event == "GAME_CREATED" &&
                    event.gameId.isNotBlank() &&
                    event.gameId != gameService.currentGameId
                ) {
                    gameService.setGameId(event.gameId)
                }
            }
            .launchIn(viewModelScope)
    }

    val gameState: StateFlow<GameState?> = gameEventFlow
        .mapNotNull { it.gameState }
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

    val eventLog: StateFlow<List<LogEntry>> = gameEventFlow
        .map { event ->
            LogEntry(
                text = event.message?.takeIf { it.isNotBlank() }
                    ?: humanReadableEvent(event.event, event.gameId),
                eventType = event.event.ifBlank { "UNKNOWN" }
            )
        }
        .runningFold(emptyList<LogEntry>()) { acc, entry ->
            (acc + entry).takeLast(MAX_LOG_ENTRIES)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: SharedFlow<String> = gameService.events
    val status: SharedFlow<String> = gameService.status

    fun connect() = gameService.connect()

    fun createGame(playerName: String) = gameService.createGame(playerName)

    fun joinGame(gameId: String, playerName: String) = gameService.joinGame(gameId, playerName)

    fun startGame() = gameService.startGame()

    fun rollDice() = gameService.rollDice()

    fun endTurn() = gameService.endTurn()

    fun requestState() = gameService.requestState()

    fun setGameId(gameId: String) = gameService.setGameId(gameId)

    private fun humanReadableEvent(eventType: String, gameId: String): String {
        return when (eventType) {
            "GAME_CREATED" -> "Game created: $gameId"
            "PLAYER_JOINED" -> "A player joined the game"
            "GAME_STARTED" -> "Game started"
            "DICE_ROLLED" -> "Dice rolled"
            "TURN_ENDED" -> "Turn ended"
            "STATE_UPDATED" -> "Game state updated"
            else -> eventType.ifBlank { "Unknown game event" }
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
