package at.aau.serg.websocketbrokerdemo.messaging

data class GameAction(
    val gameId: String = "",
    val playerId: String = "",
    val action: String = "",
    val payload: Map<String, String> = emptyMap()
)

