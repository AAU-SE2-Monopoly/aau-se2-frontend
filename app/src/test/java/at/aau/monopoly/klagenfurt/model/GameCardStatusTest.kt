package at.aau.monopoly.klagenfurt.model

import at.aau.monopoly.klagenfurt.messaging.dtos.GameLobbyInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameCardStatusTest {

    @Test
    fun `sortOrder returns expected rank per status`() {
        assertEquals(0, GameCardStatus.Open.sortOrder)
        assertEquals(1, GameCardStatus.InProgress.sortOrder)
        assertEquals(2, GameCardStatus.Full.sortOrder)
        assertEquals(3, GameCardStatus.Finished.sortOrder)
    }

    @Test
    fun `cardStatus respects phase and player capacity`() {
        val finished = GameLobbyInfo(phase = "FINISHED", playerCount = 4, maxPlayers = 4)
        assertTrue(finished.cardStatus() is GameCardStatus.Finished)

        val full = GameLobbyInfo(phase = "WAITING", playerCount = 4, maxPlayers = 4)
        assertTrue(full.cardStatus() is GameCardStatus.Full)

        val inProgress = GameLobbyInfo(phase = "ROLLING", playerCount = 2, maxPlayers = 4)
        assertTrue(inProgress.cardStatus() is GameCardStatus.InProgress)

        val open = GameLobbyInfo(phase = "WAITING", playerCount = 1, maxPlayers = 4)
        assertTrue(open.cardStatus() is GameCardStatus.Open)
    }
}

