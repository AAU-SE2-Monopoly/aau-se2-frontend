package at.aau.monopoly.klagenfurt.messaging

data class GameEvent(
    val gameId: String = "",
    val event: String = "",
    val gameState: Any? = null,
    val message: String? = null
)

