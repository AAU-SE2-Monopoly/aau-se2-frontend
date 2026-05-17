package at.aau.monopoly.klagenfurt.messaging

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameActionTest {

    @Test
    fun `default values`() {
        val action = GameAction()
        assertEquals("", action.gameId)
        assertEquals("", action.playerId)
        assertEquals("", action.action)
        assertTrue(action.payload.isEmpty())
    }

    @Test
    fun `custom values`() {
        val action = GameAction(
            gameId = "g1", playerId = "p1",
            action = "ROLL_DICE", payload = mapOf("name" to "Alice")
        )
        assertEquals("g1", action.gameId)
        assertEquals("ROLL_DICE", action.action)
        assertEquals("Alice", action.payload["name"])
    }

    @Test
    fun `data class copy and equality`() {
        val a = GameAction(gameId = "g1")
        val b = a.copy(action = "END_TURN")
        assertNotEquals(a, b)
        assertEquals("END_TURN", b.action)
    }
}

class GameEventTest {

    @Test
    fun `default values`() {
        val event = GameEvent()
        assertEquals("", event.gameId)
        assertEquals("", event.event)
        assertNull(event.gameState)
        assertNull(event.message)
    }

    @Test
    fun `custom values`() {
        val event = GameEvent(
            gameId = "g1", event = "GAME_CREATED",
            message = "Game created"
        )
        assertEquals("g1", event.gameId)
        assertEquals("GAME_CREATED", event.event)
        assertEquals("Game created", event.message)
        assertNull(event.gameState)
    }

    @Test
    fun `equality`() {
        val e1 = GameEvent(gameId = "g1", event = "X")
        val e2 = GameEvent(gameId = "g1", event = "X")
        assertEquals(e1, e2)
    }
}


