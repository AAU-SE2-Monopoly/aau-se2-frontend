package at.aau.monopoly.klagenfurt.messaging

data class GameAction(
    val gameId: String = "",
    val playerId: String = "",
    val action: String = "",
    val payload: Map<String, String> = emptyMap()
)

