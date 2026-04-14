package at.aau.monopoly.klagenfurt.networking

import kotlinx.coroutines.flow.SharedFlow

interface GameService {
    val events: SharedFlow<String>
    val status: SharedFlow<String>
    fun getCurrentUserId(): String
    fun getCurrentPlayerName(): String
    fun getGameId(): String

    fun connect()
    fun disconnect()
    fun subscribeToGame(gameId: String)
    fun createGame(playerName: String)
    fun joinGame(gameId: String, playerName: String)
    fun startGame()
    fun rollDice()
    fun endTurn()
    fun requestState()
    fun setGameId(gameId: String)
}
