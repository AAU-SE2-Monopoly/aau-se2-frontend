package at.aau.monopoly.klagenfurt.messaging.dtos

import at.aau.monopoly.klagenfurt.model.GameJoinStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LobbyEventTest {

    @Test
    fun `default values`() {
        val event = LobbyEvent()
        assertEquals("", event.event)
        assertTrue(event.games.isEmpty())
        assertNull(event.message)
    }

    @Test
    fun `with games`() {
        val info = GameLobbyInfo(gameId = "g1", hostPlayerName = "Alice", playerCount = 2)
        val event = LobbyEvent(event = "LOBBY_UPDATE", games = listOf(info), message = "ok")
        assertEquals(1, event.games.size)
        assertEquals("Alice", event.games[0].hostPlayerName)
        assertEquals("ok", event.message)
    }
}

class GameLobbyInfoTest {

    @Test
    fun `default values`() {
        val info = GameLobbyInfo()
        assertEquals("", info.gameId)
        assertEquals("", info.hostPlayerName)
        assertEquals("", info.hostPlayerId)
        assertEquals(0, info.playerCount)
        assertEquals(6, info.maxPlayers)
        assertEquals("WAITING", info.phase)
        assertTrue(info.playerIds.isEmpty())
    }

    @Test
    fun `joinStatusFor returns OPEN for waiting game`() {
        val info = GameLobbyInfo(
            phase = "WAITING", playerCount = 2, maxPlayers = 6,
            playerIds = listOf("p1", "p2")
        )
        assertEquals(GameJoinStatus.OPEN, info.joinStatusFor("p3"))
    }

    @Test
    fun `joinStatusFor returns FULL when at capacity`() {
        val info = GameLobbyInfo(
            phase = "WAITING", playerCount = 6, maxPlayers = 6,
            playerIds = listOf("p1")
        )
        assertEquals(GameJoinStatus.FULL, info.joinStatusFor("p99"))
    }

    @Test
    fun `joinStatusFor returns IN_PROGRESS for started game`() {
        val info = GameLobbyInfo(phase = "ROLLING", playerCount = 3, maxPlayers = 6)
        assertEquals(GameJoinStatus.IN_PROGRESS, info.joinStatusFor("p1"))
    }

    @Test
    fun `joinStatusFor returns FINISHED`() {
        val info = GameLobbyInfo(phase = "FINISHED")
        assertEquals(GameJoinStatus.FINISHED, info.joinStatusFor("p1"))
    }

    @Test
    fun `joinStatusFor returns OPEN when already in game`() {
        val info = GameLobbyInfo(
            phase = "WAITING", playerCount = 6, maxPlayers = 6,
            playerIds = listOf("p1")
        )
        assertEquals(GameJoinStatus.OPEN, info.joinStatusFor("p1"))
    }
}

