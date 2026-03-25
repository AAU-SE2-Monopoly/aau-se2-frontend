package at.aau.serg.websocketbrokerdemo.messaging

data class GameEvent(
    val gameId: String = "",
    val event: String = "",
    val gameState: Any? = null,
    val message: String? = null
)

