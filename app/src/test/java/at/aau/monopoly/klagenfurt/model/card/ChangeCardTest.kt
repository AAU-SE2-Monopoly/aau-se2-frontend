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
}