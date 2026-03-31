package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CardTest {

    // Da Card abstrakt ist, erstellen wir eine konkrete Implementierung für den Test
    class TestCard(
        override val id: Int,
        override val description: String,
        override val action: CardAction,
        override val amount: Int = 0,
        override val targetFieldId: Int? = null,
        override val moveSpaces: Int = 0
    ) : Card(id, description, action, amount, targetFieldId, moveSpaces)

    @Test
    fun `test card initialization with required parameters`() {
        val card = TestCard(
            id = 1,
            description = "Gehe vor auf Start",
            action = CardAction.MOVE_TO,
            targetFieldId = 0
        )

        assertEquals(1, card.id)
        assertEquals("Gehe vor auf Start", card.description)
        assertEquals(CardAction.MOVE_TO, card.action)
        assertEquals(0, card.targetFieldId)
        // Überprüfung der Default-Werte
        assertEquals(0, card.amount)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test card initialization with all parameters`() {
        val card = TestCard(
            id = 99,
            description = "Renovierungskosten",
            action = CardAction.PAY_MONEY,
            amount = 150,
            moveSpaces = 5
        )

        assertEquals(99, card.id)
        assertEquals(150, card.amount)
        assertEquals(5, card.moveSpaces)
        assertNull(card.targetFieldId)
    }

    @Test
    fun `test card equality with different IDs`() {
        val card1 = TestCard(1, "Desc", CardAction.PAY_MONEY)
        val card2 = TestCard(2, "Desc", CardAction.PAY_MONEY)

        assertNotEquals(card1.id, card2.id)
    }
}