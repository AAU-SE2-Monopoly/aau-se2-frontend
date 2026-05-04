package at.aau.monopoly.klagenfurt.networking

import at.aau.monopoly.klagenfurt.messaging.GameEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GameService {
    val events: SharedFlow<String>
    val logEvents: SharedFlow<String>
        get() = events
    val status: SharedFlow<String>
    val lobbyEvents: SharedFlow<String>
    val currentPlayerId: String
    val currentPlayerName: String
    val currentGameId: String

    /** Emits `true` once the STOMP subscription for the current game topic is active. */
    val subscriptionReady: StateFlow<Boolean>

    /** Emits `true` once the STOMP subscription for the lobby topic is active. */
    val lobbySubscriptionReady: StateFlow<Boolean>

    /** Emits `true` when the WebSocket session is established, `false` on disconnect. */
    val connectionState: StateFlow<Boolean>

    fun connect()
    fun disconnect()
    fun subscribeToGame(gameId: String)
    fun subscribeToLobby()
    fun requestGameList()
    fun closeGame(gameId: String)
    suspend fun createGame(playerName: String, iconId: String = "lindwurm"): String?
    suspend fun joinGame(gameId: String, playerName: String, iconId: String = "lindwurm"): Result<GameEvent>
    fun startGame()
    fun rollDice()
    fun endTurn()
    fun requestState()
    fun setGameId(gameId: String)
}
