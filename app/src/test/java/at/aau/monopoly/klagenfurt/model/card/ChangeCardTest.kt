package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChanceCardTest {

    @Test
    fun `test chance card inheritance and properties`() {
        val description = "Rücke vor bis auf die Wiener Straße"
        val chanceCard = ChanceCard(
            id = 5,
            description = description,
            action = CardAction.MOVE_TO,
            targetFieldId = 24
        )

        // Teste die Properties (Vererbung von Card)
        assertEquals(5, chanceCard.id)
        assertEquals(description, chanceCard.description)
        assertEquals(CardAction.MOVE_TO, chanceCard.action)
        assertEquals(24, chanceCard.targetFieldId)

        // Teste Default-Werte
        assertEquals(0, chanceCard.amount)
        assertEquals(0, chanceCard.moveSpaces)
    }

    @Test
    fun `test data class copy method`() {
        val original = ChanceCard(1, "Original", CardAction.COLLECT_MONEY, amount = 200)

        // Nutze die Kotlin copy-Funktion
        val copied = original.copy(id = 2, description = "Kopie")

        assertEquals(2, copied.id)
        assertEquals("Kopie", copied.description)
        assertEquals(original.action, copied.action) // Bleibt gleich
        assertEquals(200, copied.amount)           // Bleibt gleich
    }

    @Test
    fun `test data class equality`() {
        val card1 = ChanceCard(10, "Gleiche Karte", CardAction.PAY_MONEY, amount = 50)
        val card2 = ChanceCard(10, "Gleiche Karte", CardAction.PAY_MONEY, amount = 50)
        val card3 = ChanceCard(11, "Andere Karte", CardAction.PAY_MONEY, amount = 50)

        // Testet equals() und hashCode() automatisch
        assertEquals(card1, card2)
        assertNotEquals(card1, card3)
        assertEquals(card1.hashCode(), card2.hashCode())
    }

    @Test
    fun `test toString implementation`() {
        val card = ChanceCard(1, "Test", CardAction.MOVE_FORWARD, moveSpaces = 3)
        val toString = card.toString()

        // Prüft, ob die wichtigsten Infos im String enthalten sind (gut für Debugging/Coverage)
        assertTrue(toString.contains("id=1"))
        assertTrue(toString.contains("description=Test"))
        assertTrue(toString.contains("moveSpaces=3"))
    }
}