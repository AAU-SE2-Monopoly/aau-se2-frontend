package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.dtos.GameLobbyInfo
import at.aau.monopoly.klagenfurt.messaging.dtos.LobbyEvent
import at.aau.monopoly.klagenfurt.model.cardStatus
import at.aau.monopoly.klagenfurt.model.sortOrder
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LobbyViewModel(private val gameService: GameService) : ViewModel() {

    private val objectMapper = JacksonProvider.objectMapper

    val currentPlayerId: String get() = gameService.currentPlayerId

    val isConnected: StateFlow<Boolean> = gameService.status
        .map { it.contains("Connected", ignoreCase = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _games = MutableStateFlow<List<GameLobbyInfo>>(emptyList())
    val games: StateFlow<List<GameLobbyInfo>> = _games.asStateFlow()

    /** Tracks the gameId of a game we just created, so LobbyActivity can navigate. */
    private val _createdGameId = MutableStateFlow<String?>(null)
    val createdGameId: StateFlow<String?> = _createdGameId.asStateFlow()

    init {
        observeLobbyEvents()
        observeGameEvents()
        // Defer connection to avoid blocking UI with OkHttp/Okio class verification
        viewModelScope.launch {
            delay(100) // let first frame render
            gameService.connect()
        }
    }

    private fun observeLobbyEvents() {
        viewModelScope.launch {
            gameService.lobbyEvents.collect { raw ->
                try {
                    val lobbyEvent = objectMapper.readValue(raw, LobbyEvent::class.java)
                    // Open games first, then in-progress, full, finished
                    _games.value = lobbyEvent.games.sortedBy { it.cardStatus().sortOrder }
                } catch (e: Exception) {
                    Log.e("LobbyViewModel", "Error parsing lobby event: ${e.message}", e)
                }
            }
        }
    }

    private fun observeGameEvents() {
        viewModelScope.launch {
            gameService.events.collect { raw ->
                try {
                    val node = objectMapper.readTree(raw)
                    val event = node.get("event")?.asText() ?: ""
                    if (event == "GAME_CREATED") {
                        val gameId = node.get("gameId")?.asText() ?: ""
                        if (gameId.isNotEmpty()) {
                            _createdGameId.value = gameId
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LobbyViewModel", "Error parsing game event: ${e.message}", e)
                }
            }
        }
    }

    fun onConnected() {
        gameService.subscribeToLobby()
        gameService.requestGameList()
    }

    fun createGame(playerName: String) {
        _createdGameId.value = null
        gameService.createGame(playerName)
    }

    fun closeGame(gameId: String) {
        gameService.closeGame(gameId)
    }

    fun clearCreatedGameId() {
        _createdGameId.value = null
    }

    class Factory(private val gameService: GameService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LobbyViewModel(gameService) as T
        }
    }
}

