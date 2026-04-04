package at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketbrokerdemo.messaging.GameAction
import at.aau.serg.websocketbrokerdemo.messaging.GameEvent
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MessagingTest {

    @Test
    fun `verify GameAction properties`() {
        val action = GameAction(
            gameId = "g1",
            playerId = "p1",
            action = "ROLL_DICE",
            payload = mapOf("key" to "value")
        )

        assertEquals("g1", action.gameId)
        assertEquals("p1", action.playerId)
        assertEquals("ROLL_DICE", action.action)
        assertEquals("value", action.payload["key"])
    }

    @Test
    fun `verify GameEvent properties`() {
        val event = GameEvent(
            gameId = "g1",
            event = "DICE_ROLLED",
            gameState = "someState",
            message = "Success"
        )

        assertEquals("g1", event.gameId)
        assertEquals("DICE_ROLLED", event.event)
        assertEquals("someState", event.gameState)
        assertEquals("Success", event.message)
    }

    @Test
    fun `GameAction equals and hashCode contract`() {
        EqualsVerifier.forClass(GameAction::class.java)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `GameEvent equals and hashCode contract`() {
        EqualsVerifier.forClass(GameEvent::class.java)
            .usingGetClass()
            .verify()
    }
}
