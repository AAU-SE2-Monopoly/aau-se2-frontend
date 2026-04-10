package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommunityChestCardTest {

    @Test
    fun `test community chest card initialization`() {
        val card = CommunityChestCard(
            id = 10,
            description = "Bankirrtum zu deinen Gunsten",
            action = CardAction.COLLECT_MONEY,
            amount = 200
        )

        assertEquals(10, card.id)
        assertEquals("Bankirrtum zu deinen Gunsten", card.description)
        assertEquals(CardAction.COLLECT_MONEY, card.action)
        assertEquals(200, card.amount)

        // Default-Werte prüfen
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test community chest card data class equality`() {
        val card1 = CommunityChestCard(1, "Arztgebühr", CardAction.PAY_MONEY, amount = 50)
        val card2 = CommunityChestCard(1, "Arztgebühr", CardAction.PAY_MONEY, amount = 50)
        val card3 = CommunityChestCard(2, "Arztgebühr", CardAction.PAY_MONEY, amount = 50)

        // Testet die generierten equals() und hashCode() Methoden
        assertEquals(card1, card2)
        assertNotEquals(card1, card3)
        assertEquals(card1.hashCode(), card2.hashCode())
    }

    @Test
    fun `test community chest card copy functionality`() {
        val original = CommunityChestCard(5, "Schulgeld", CardAction.PAY_MONEY, amount = 100)
        val copy = original.copy(amount = 150)

        assertEquals(5, copy.id)
        assertEquals("Schulgeld", copy.description)
        assertEquals(150, copy.amount)
    }

    @Test
    fun `test community chest card toString`() {
        val card = CommunityChestCard(1, "Erbschaft", CardAction.COLLECT_MONEY, amount = 100)
        val stringRepresentation = card.toString()

        // Stellt sicher, dass toString() die Datenklasse korrekt repräsentiert
        assertTrue(stringRepresentation.contains("CommunityChestCard"))
        assertTrue(stringRepresentation.contains("id=1"))
        assertTrue(stringRepresentation.contains("amount=100"))
    }
}