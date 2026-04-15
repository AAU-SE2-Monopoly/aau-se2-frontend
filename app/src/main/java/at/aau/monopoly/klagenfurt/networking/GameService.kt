package at.aau.monopoly.klagenfurt.networking

import kotlinx.coroutines.flow.SharedFlow

interface GameService {
    val events: SharedFlow<String>
    val status: SharedFlow<String>
    val lobbyEvents: SharedFlow<String>
    val currentPlayerId: String

    fun connect()
    fun disconnect()
    fun subscribeToGame(gameId: String)
    fun subscribeToLobby()
    fun requestGameList()
    fun closeGame(gameId: String)
    fun createGame(playerName: String)
    fun joinGame(gameId: String, playerName: String)
    fun startGame()
    fun rollDice()
    fun endTurn()
    fun requestState()
    fun setGameId(gameId: String)
}
