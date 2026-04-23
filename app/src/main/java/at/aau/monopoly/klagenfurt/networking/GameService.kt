package at.aau.monopoly.klagenfurt.networking

import kotlinx.coroutines.flow.SharedFlow

interface GameService {
    val events: SharedFlow<String>
    val status: SharedFlow<String>
    val lobbyEvents: SharedFlow<String>
    val currentPlayerId: String
    val currentPlayerName: String
    val currentGameId: String

    fun connect()
    fun disconnect()
    fun subscribeToGame(gameId: String)
    fun subscribeToLobby()
    fun requestGameList()
    fun closeGame(gameId: String)
    fun createGame(playerName: String, iconId: String = "lindwurm")
    fun joinGame(gameId: String, playerName: String, iconId: String = "lindwurm")
    fun startGame()
    fun rollDice()
    fun endTurn()
    fun requestState()
    fun setGameId(gameId: String)
}
