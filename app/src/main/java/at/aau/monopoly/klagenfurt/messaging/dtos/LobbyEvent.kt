package at.aau.monopoly.klagenfurt.messaging.dtos

data class LobbyEvent(
    val event: String = "",
    val games: List<GameLobbyInfo> = emptyList(),
    val message: String? = null
)

