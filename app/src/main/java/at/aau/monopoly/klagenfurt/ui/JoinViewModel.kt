package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the create-game and join-game flows for [JoinActivity].
 *
 * The actual waiting for server confirmation is handled inside
 * [GameService.createGame] and [GameService.joinGame], so the
 * ViewModel only needs to react to the final Result.
 */
class JoinViewModel(private val gameService: GameService) : ViewModel() {

    private val objectMapper = JacksonProvider.objectMapper

    sealed class JoinState {
        object Idle    : JoinState()
        object Loading : JoinState()
        data class Success(val gameId: String) : JoinState()
        data class Error(val message: String)  : JoinState()
    }

    private val _joinState = MutableStateFlow<JoinState>(JoinState.Idle)
    val joinState: StateFlow<JoinState> = _joinState.asStateFlow()

    val isConnected: StateFlow<Boolean> = gameService.connectionState

    val reconnectFailed: StateFlow<Boolean> = gameService.reconnectFailed

    private val _takenIcons = MutableStateFlow<Set<String>>(emptySet())
    val takenIcons: StateFlow<Set<String>> = _takenIcons.asStateFlow()

    private var stateObservationJob: Job? = null

    fun observeGame(gameId: String) {
        if (gameId.isBlank()) return
        stateObservationJob?.cancel()
        stateObservationJob = viewModelScope.launch {
            gameService.subscribeToGame(gameId)

            gameService.events.collect { raw ->
                try {
                    val event = objectMapper.readValue(raw, GameEvent::class.java)
                    if (event.gameId == gameId && event.gameState != null) {
                        val taken = event.gameState.players.map { it.iconId }.toSet()
                        _takenIcons.value = taken
                    }
                } catch (_: Exception) {
                    // Ignore parse errors from other event types
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stateObservationJob?.cancel()
    }



    fun createGame(playerName: String, iconId: String) {
        if (_joinState.value is JoinState.Loading) return

        // Guard: refuse to send commands when disconnected
        if (!gameService.connectionState.value) {
            _joinState.value = JoinState.Error("Not connected to server. Please wait…")
            return
        }

        _joinState.value = JoinState.Loading

        viewModelScope.launch {
            // GameStompClient.createGame() sends the request, waits for GAME_CREATED
            // on the personal topic, subscribes to the game topic, and returns the
            // gameId (or null on failure).
            val createdGameId = gameService.createGame(playerName, iconId)

            if (createdGameId != null) {
                _joinState.value = JoinState.Success(createdGameId)
            } else {
                _joinState.value = JoinState.Error("Failed to create game – no response from server")
            }
        }
    }


    fun joinGame(gameId: String, playerName: String, iconId: String) {
        if (_joinState.value is JoinState.Loading) return

        // Guard: refuse to send commands when disconnected
        if (!gameService.connectionState.value) {
            _joinState.value = JoinState.Error("Not connected to server. Please wait…")
            return
        }

        _joinState.value = JoinState.Loading

        viewModelScope.launch {

            val result = gameService.joinGame(gameId, playerName, iconId)

            result.fold(
                onSuccess = { _joinState.value = JoinState.Success(gameId) },
                onFailure = { error ->
                    Log.w("JoinViewModel", "Join rejected: ${error.message}")
                    _joinState.value = JoinState.Error(error.message ?: "Join rejected by server")
                }
            )
        }
    }

    fun resetState() {
        _joinState.value = JoinState.Idle
    }

    fun reconnect() {
        gameService.connect()
    }


    class Factory(private val gameService: GameService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return JoinViewModel(gameService) as T
        }
    }
}