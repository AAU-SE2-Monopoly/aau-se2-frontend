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
 * Manages the create-game and join-game flows for JoinActivity.
 *
 * The actual waiting for server confirmation is handled inside
 * GameService.createGame and GameService.joinGame, so the
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
    private var currentObservedGameId: String? = null

    fun observeGame(gameId: String) {
        if (gameId.isBlank()) return

        // Prevent duplicate subscriptions to the same game
        if (currentObservedGameId == gameId) return

        stopObserving()
        currentObservedGameId = gameId

        stateObservationJob = viewModelScope.launch {
            // FIX Issue 4: Launch the collector BEFORE subscribing to ensure
            // the initial GAME_CREATED/STATE_SNAPSHOT event is not missed.
            launch {
                gameService.events.collect { raw ->
                    // FIX Issue 5: Silence state updates once we are successfully navigating away
                    if (_joinState.value is JoinState.Success) return@collect

                    try {
                        // FIX Issue 6: Fast-path string check to prevent Jackson from parsing irrelevant events
                        if (!raw.contains(gameId)) return@collect

                        val event = objectMapper.readValue(raw, GameEvent::class.java)
                        if (event.gameId == gameId && event.gameState != null) {
                            val taken = event.gameState.players.map { it.iconId }.toSet()
                            _takenIcons.value = taken
                        }
                    } catch (e: Exception) {
                        // FIX Issue 6: Never swallow JSON parsing exceptions silently
                        Log.e("JoinViewModel", "Failed to parse GameEvent. Payload: $raw", e)
                    }
                }
            }

            // Now that the collector is active, trigger the STOMP subscription
            gameService.subscribeToGame(gameId)
        }
    }

    /**
     * Cancels the active observation job and cleans up the STOMP subscription.
     */
    fun stopObserving() {
        stateObservationJob?.cancel()
        stateObservationJob = null

        // If your GameService has an explicit unsubscribe method, call it here:
        // currentObservedGameId?.let { gameService.unsubscribeFromGame(it) }

        currentObservedGameId = null
    }

    override fun onCleared() {
        super.onCleared()
        stopObserving()
    }

    fun createGame(playerName: String, iconId: String) {
        if (_joinState.value is JoinState.Loading) return

        if (!gameService.connectionState.value) {
            _joinState.value = JoinState.Error("Not connected to server. Please wait…")
            return
        }

        _joinState.value = JoinState.Loading

        viewModelScope.launch {
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
        stopObserving()
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