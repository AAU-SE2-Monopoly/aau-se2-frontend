package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChanceCardTest {

    @Test
    fun `test chance card inheritance and properties`() {
        val description = "Advance to Wiener Straße"
        val chanceCard = ChanceCard(
            id = 5,
            description = description,
            action = CardAction.MOVE_TO,
            targetFieldId = 24
        )

        // Test properties (inherited from Card)
        assertEquals(5, chanceCard.id)
        assertEquals(description, chanceCard.description)
        assertEquals(CardAction.MOVE_TO, chanceCard.action)
        assertEquals(24, chanceCard.targetFieldId)

        // Test default values
        assertEquals(0, chanceCard.amount)
        assertEquals(0, chanceCard.moveSpaces)
    }

    @Test
    fun `test data class copy method`() {
        val original = ChanceCard(1, "Original", CardAction.COLLECT_MONEY, amount = 200)

        // Use the Kotlin copy function
        val copied = original.copy(id = 2, description = "Kopie")

        assertEquals(2, copied.id)
        assertEquals("Kopie", copied.description)
        assertEquals(original.action, copied.action) // Stays the same
        assertEquals(200, copied.amount)           // Stays the same
    }

    @Test
    fun `test data class equality`() {
        val card1 = ChanceCard(10, "Same Card", CardAction.PAY_MONEY, amount = 50)
        val card2 = ChanceCard(10, "Same Card", CardAction.PAY_MONEY, amount = 50)
        val card3 = ChanceCard(11, "Different Card", CardAction.PAY_MONEY, amount = 50)

        // Tests equals() and hashCode() automatically
        assertEquals(card1, card2)
        assertNotEquals(card1, card3)
        assertEquals(card1.hashCode(), card2.hashCode())
    }

    @Test
    fun `test toString implementation`() {
        val card = ChanceCard(1, "Test", CardAction.MOVE_FORWARD, moveSpaces = 3)
        val toString = card.toString()

        // Checks if the most important info is contained in the string (good for debugging/coverage)
        assertTrue(toString.contains("id=1"))
        assertTrue(toString.contains("description=Test"))
        assertTrue(toString.contains("moveSpaces=3"))
    }
    @Test
    fun `test chance card with pay money action`() {
        val card = ChanceCard(
            id = 20,
            description = "Pay tax",
            action = CardAction.PAY_MONEY,
            amount = 100
        )

        assertEquals(CardAction.PAY_MONEY, card.action)
        assertEquals(100, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test chance card with collect from each player action`() {
        val card = ChanceCard(
            id = 21,
            description = "Collect from each player",
            action = CardAction.COLLECT_FROM_EACH,
            amount = 50
        )

        assertEquals(CardAction.COLLECT_FROM_EACH, card.action)
        assertEquals(50, card.amount)
    }

    @Test
    fun `test chance card with pay each player action`() {
        val card = ChanceCard(
            id = 22,
            description = "Pay each player",
            action = CardAction.PAY_EACH_PLAYER,
            amount = 25
        )

        assertEquals(CardAction.PAY_EACH_PLAYER, card.action)
        assertEquals(25, card.amount)
    }

    @Test
    fun `test chance card go to jail action`() {
        val card = ChanceCard(
            id = 23,
            description = "Go to jail",
            action = CardAction.GO_TO_JAIL
        )

        assertEquals(CardAction.GO_TO_JAIL, card.action)
        assertEquals(0, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test chance card get out of jail action`() {
        val card = ChanceCard(
            id = 24,
            description = "Get out of jail free",
            action = CardAction.GET_OUT_OF_JAIL
        )

        assertEquals(CardAction.GET_OUT_OF_JAIL, card.action)
        assertEquals(0, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test chance card component functions`() {
        val card = ChanceCard(
            id = 25,
            description = "Move forward",
            action = CardAction.MOVE_FORWARD,
            amount = 10,
            targetFieldId = 4,
            moveSpaces = 3
        )

        assertEquals(25, card.component1())
        assertEquals("Move forward", card.component2())
        assertEquals(CardAction.MOVE_FORWARD, card.component3())
        assertEquals(10, card.component4())
        assertEquals(4, card.component5())
        assertEquals(3, card.component6())
    }

    @Test
    fun `test chance card copy with all fields changed`() {
        val original = ChanceCard(
            id = 1,
            description = "Original",
            action = CardAction.COLLECT_MONEY,
            amount = 100,
            targetFieldId = null,
            moveSpaces = 0
        )

        val copy = original.copy(
            id = 2,
            description = "Changed",
            action = CardAction.MOVE_TO,
            amount = 0,
            targetFieldId = 10,
            moveSpaces = 0
        )

        assertEquals(2, copy.id)
        assertEquals("Changed", copy.description)
        assertEquals(CardAction.MOVE_TO, copy.action)
        assertEquals(0, copy.amount)
        assertEquals(10, copy.targetFieldId)
        assertEquals(0, copy.moveSpaces)
    }
}