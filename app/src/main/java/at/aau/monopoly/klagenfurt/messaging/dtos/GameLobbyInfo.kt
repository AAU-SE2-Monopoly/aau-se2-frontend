package at.aau.monopoly.klagenfurt.messaging.dtos

data class GameLobbyInfo(
    val gameId: String = "",
    val hostPlayerName: String = "",
    val hostPlayerId: String = "",
    val playerCount: Int = 0,
    val maxPlayers: Int = 6,
    val phase: String = "WAITING",
    val playerIds: List<String> = emptyList()
)

