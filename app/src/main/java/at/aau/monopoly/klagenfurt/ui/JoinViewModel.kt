package at.aau.monopoly.klagenfurt.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages the create-game and join-game flows for [JoinActivity].
 *
 * Design decisions:
 * - Collection of the result flow is started via [async] BEFORE the command is sent
 *   to the server. This eliminates the timing race where the server response could
 *   arrive before [waitForJoinResult] started collecting.
 * - All event filtering is scoped to the relevant [gameId] to prevent a replayed or
 *   cross-game event from triggering an incorrect navigation.
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

    // -------------------------------------------------------------------------
    // Create game
    // -------------------------------------------------------------------------

    fun createGame(playerName: String, iconId: String) {
        if (_joinState.value is JoinState.Loading) return
        _joinState.value = JoinState.Loading

        viewModelScope.launch {
            // Start listening for GAME_CREATED *before* sending the command,
            // so we never miss a fast server response.
            val resultDeferred = async {
                gameService.events
                    .mapNotNull { payload -> parseCreatedGameId(payload, gameService.currentPlayerId) }
                    .first()
            }

            gameService.createGame(playerName, iconId)

            val createdGameId = resultDeferred.await()
            gameService.setGameId(createdGameId)
            _joinState.value = JoinState.Success(createdGameId)
        }
    }

    // -------------------------------------------------------------------------
    // Join existing game
    // -------------------------------------------------------------------------

    fun joinGame(gameId: String, playerName: String, iconId: String) {
        if (_joinState.value is JoinState.Loading) return
        _joinState.value = JoinState.Loading

        viewModelScope.launch {
            // Start listening for the result *before* sending the join command,
            // filtering strictly to this gameId to avoid cross-game interference.
            val resultDeferred = async {
                gameService.events
                    .mapNotNull { payload -> parseJoinResult(payload, gameId) }
                    .first()
            }

            gameService.joinGame(gameId, playerName, iconId)

            val result = resultDeferred.await()
            if (result.isSuccess) {
                // Normal path: server confirmed the player joined.
                _joinState.value = JoinState.Success(gameId)
            } else {
                // =====================================================================
                // TEMPORARY REJOIN WORKAROUND
                // The backend does not yet support a rejoin endpoint. When a player
                // who is already part of the game tries to join again the server
                // returns ERROR. In that case we skip the join command and instead
                // re-subscribe to the game topic + request the current state so the
                // player lands back on the board.
                //
                // TODO: Remove this fallback and restore the original error path below
                //       once a proper rejoin endpoint exists on the backend.
                // =====================================================================
                Log.w("JoinViewModel", "Join rejected – attempting rejoin via request-state fallback")
                try {
                    // subscribeToGame() subscribes to /topic/game/{gameId} and
                    // immediately sends /app/game/state to fetch the current board.
                    gameService.subscribeToGame(gameId)

                    // Wait until the STOMP subscription is confirmed ready before
                    // navigating, so the GameViewModel doesn't miss the state event.
                    gameService.subscriptionReady.first { it }

                    // Brief grace period to allow the state response to arrive and
                    // be buffered in the events flow before the board is rendered.
                    delay(500L)

                    _joinState.value = JoinState.Success(gameId)
                } catch (e: Exception) {
                    Log.e("JoinViewModel", "Rejoin (request-state) fallback failed", e)
                    // If even the fallback fails, surface the original server error.
                    _joinState.value = JoinState.Error(
                        result.exceptionOrNull()?.message ?: "Join rejected by server"
                    )
                }

                // === ORIGINAL ERROR PATH – restore when backend rejoin is implemented ===
                // _joinState.value = JoinState.Error(
                //     result.exceptionOrNull()?.message ?: "Join rejected by server"
                // )
            }
        }
    }

    fun resetState() {
        _joinState.value = JoinState.Idle
    }

    // -------------------------------------------------------------------------
    // Parsing helpers – private, pure, easily unit-testable
    // -------------------------------------------------------------------------

    /**
     * Parses a raw JSON payload and returns the new gameId if the event is
     * GAME_CREATED and was initiated by [currentPlayerId], otherwise null.
     */
    private fun parseCreatedGameId(payload: String, currentPlayerId: String): String? {
        return try {
            val event = objectMapper.readValue(payload, GameEvent::class.java)
            if (event.event == "GAME_CREATED" && event.gameId.isNotBlank()) {
                // Confirm this GAME_CREATED belongs to us via the player list
                val isOurGame = event.gameState?.players
                    ?.any { it.id == currentPlayerId } != false
                if (isOurGame) event.gameId else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("JoinViewModel", "parseCreatedGameId error: ${e.message}", e)
            null
        }
    }

    /**
     * Parses a raw JSON payload and returns:
     * - [Result.success] if the event is PLAYER_JOINED for [gameId]
     * - [Result.failure] if the event is ERROR for [gameId]
     * - null for all other events (they are ignored by the flow)
     */
    private fun parseJoinResult(payload: String, gameId: String): Result<Unit>? {
        return try {
            val event = objectMapper.readValue(payload, GameEvent::class.java)
            // Strict gameId guard – ignore events from other games
            if (event.gameId != gameId) return null
            when (event.event) {
                "PLAYER_JOINED" -> Result.success(Unit)
                "ERROR"         -> Result.failure(Exception(event.message ?: "Join rejected by server"))
                else            -> null
            }
        } catch (e: Exception) {
            Log.e("JoinViewModel", "parseJoinResult error: ${e.message}", e)
            null
        }
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
