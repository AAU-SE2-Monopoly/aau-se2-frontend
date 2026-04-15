package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.messaging.dtos.GameLobbyInfo
import at.aau.monopoly.klagenfurt.messaging.dtos.LobbyEvent
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LobbyDtoTest {

    private val objectMapper = JacksonProvider.objectMapper

    @Test
    fun `GameLobbyInfo defaults are correct`() {
        val info = GameLobbyInfo()
        assertEquals("", info.gameId)
        assertEquals("", info.hostPlayerName)
        assertEquals("", info.hostPlayerId)
        assertEquals(0, info.playerCount)
        assertEquals(4, info.maxPlayers)
        assertEquals("WAITING", info.phase)
    }

    @Test
    fun `GameLobbyInfo can be created with values`() {
        val info = GameLobbyInfo(
            gameId = "abc-123",
            hostPlayerName = "Alice",
            hostPlayerId = "player-1",
            playerCount = 2,
            maxPlayers = 4,
            phase = "WAITING"
        )
        assertEquals("abc-123", info.gameId)
        assertEquals("Alice", info.hostPlayerName)
        assertEquals("player-1", info.hostPlayerId)
        assertEquals(2, info.playerCount)
        assertEquals(4, info.maxPlayers)
    }

    @Test
    fun `LobbyEvent defaults are correct`() {
        val event = LobbyEvent()
        assertEquals("", event.event)
        assertEquals(emptyList<GameLobbyInfo>(), event.games)
        assertNull(event.message)
    }

    @Test
    fun `LobbyEvent deserializes from JSON`() {
        val json = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "game-1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "p1",
                    "playerCount": 1,
                    "maxPlayers": 4,
                    "phase": "WAITING"
                },
                {
                    "gameId": "game-2",
                    "hostPlayerName": "Bob",
                    "hostPlayerId": "p2",
                    "playerCount": 3,
                    "maxPlayers": 4,
                    "phase": "WAITING"
                }
            ],
            "message": "Lobby updated"
        }
        """.trimIndent()

        val event = objectMapper.readValue(json, LobbyEvent::class.java)
        assertEquals("LOBBY_UPDATE", event.event)
        assertEquals(2, event.games.size)
        assertEquals("game-1", event.games[0].gameId)
        assertEquals("Alice", event.games[0].hostPlayerName)
        assertEquals("p1", event.games[0].hostPlayerId)
        assertEquals(1, event.games[0].playerCount)
        assertEquals("game-2", event.games[1].gameId)
        assertEquals("Bob", event.games[1].hostPlayerName)
        assertEquals(3, event.games[1].playerCount)
        assertEquals("Lobby updated", event.message)
    }

    @Test
    fun `LobbyEvent deserializes with empty games list`() {
        val json = """
        {
            "event": "LOBBY_UPDATE",
            "games": [],
            "message": null
        }
        """.trimIndent()

        val event = objectMapper.readValue(json, LobbyEvent::class.java)
        assertEquals("LOBBY_UPDATE", event.event)
        assertEquals(0, event.games.size)
        assertNull(event.message)
    }

    @Test
    fun `LobbyEvent deserializes with unknown fields ignored`() {
        val json = """
        {
            "event": "LOBBY_UPDATE",
            "games": [],
            "message": null,
            "unknownField": "value"
        }
        """.trimIndent()

        val event = objectMapper.readValue(json, LobbyEvent::class.java)
        assertEquals("LOBBY_UPDATE", event.event)
    }

    @Test
    fun `GameLobbyInfo serializes to JSON correctly`() {
        val info = GameLobbyInfo(
            gameId = "test-id",
            hostPlayerName = "TestHost",
            hostPlayerId = "host-id",
            playerCount = 2,
            maxPlayers = 4,
            phase = "WAITING"
        )
        val json = objectMapper.writeValueAsString(info)
        val deserialized = objectMapper.readValue(json, GameLobbyInfo::class.java)
        assertEquals(info, deserialized)
    }
}

