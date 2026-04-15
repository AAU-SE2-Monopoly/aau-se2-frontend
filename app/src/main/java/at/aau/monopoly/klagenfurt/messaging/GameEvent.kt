package at.aau.monopoly.klagenfurt.messaging

import at.aau.monopoly.klagenfurt.model.GameState

data class GameEvent(
    val gameId: String = "",
    val event: String = "",
    val gameState: GameState? = null,
    val message: String? = null
)
