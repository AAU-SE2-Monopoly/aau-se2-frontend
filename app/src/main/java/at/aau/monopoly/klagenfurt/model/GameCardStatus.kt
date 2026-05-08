package at.aau.monopoly.klagenfurt.model

import at.aau.monopoly.klagenfurt.messaging.dtos.GameLobbyInfo

sealed class GameCardStatus {
    object Open : GameCardStatus()
    object Full : GameCardStatus()
    object InProgress : GameCardStatus()
    object Finished : GameCardStatus()
}

/** Natural display order: joinable games first, unavailable last. */
val GameCardStatus.sortOrder: Int get() = when (this) {
    is GameCardStatus.Open       -> 0
    is GameCardStatus.InProgress -> 1
    is GameCardStatus.Full       -> 2
    is GameCardStatus.Finished   -> 3
}

/**
 * Derives the visual status of a lobby game card from its data.
 */
fun GameLobbyInfo.cardStatus(): GameCardStatus = when {
    phase == "FINISHED"                      -> GameCardStatus.Finished
    playerCount >= maxPlayers                -> GameCardStatus.Full
    phase != "WAITING"                       -> GameCardStatus.InProgress
    else                                     -> GameCardStatus.Open
}
