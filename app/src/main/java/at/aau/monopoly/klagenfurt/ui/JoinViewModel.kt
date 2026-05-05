package at.aau.monopoly.klagenfurt.ui
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    // -------------------------------------------------------------------------
    // Create game
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Join existing game
    // -------------------------------------------------------------------------

    fun joinGame(gameId: String, playerName: String, iconId: String) {
        if (_joinState.value is JoinState.Loading) return

        // Guard: refuse to send commands when disconnected
        if (!gameService.connectionState.value) {
            _joinState.value = JoinState.Error("Not connected to server. Please wait…")
            return
        }

        _joinState.value = JoinState.Loading

        viewModelScope.launch {
            // GameStompClient.joinGame() subscribes to the game topic, sends the join
            // command, then waits for PLAYER_JOINED (success, filtered by player ID) or
            // ERROR (failure with the server's message). Returns Result<GameEvent>.
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

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    class Factory(private val gameService: GameService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return JoinViewModel(gameService) as T
        }
    }
}
