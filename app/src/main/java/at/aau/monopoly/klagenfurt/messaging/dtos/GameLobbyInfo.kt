package at.aau.monopoly.klagenfurt.messaging.dtos

import at.aau.monopoly.klagenfurt.model.GameJoinStatus

data class GameLobbyInfo(
    val gameId: String = "",
    val hostPlayerName: String = "",
    val hostPlayerId: String = "",
    val playerCount: Int = 0,
    val maxPlayers: Int = 6,
    val phase: String = "WAITING",
    val playerIds: List<String> = emptyList()
)
fun GameLobbyInfo.joinStatusFor(currentPlayerId:String): GameJoinStatus = GameJoinStatus.compute(
    phase = phase,
    playerCount = playerCount,
    maxPlayers = maxPlayers,
    playerIds = playerIds,
    currentPlayerId = currentPlayerId
)

