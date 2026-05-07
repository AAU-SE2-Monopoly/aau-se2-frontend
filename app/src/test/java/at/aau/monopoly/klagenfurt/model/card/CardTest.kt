package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CardTest {

    // Since Card is abstract, we create a concrete implementation for the test
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
        // Check default values
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

    @Test
    fun `test card collect money action`() {
        val card = TestCard(
            id = 10,
            description = "Collect money",
            action = CardAction.COLLECT_MONEY,
            amount = 200
        )

        assertEquals(CardAction.COLLECT_MONEY, card.action)
        assertEquals(200, card.amount)
    }

    @Test
    fun `test card move forward action`() {
        val card = TestCard(
            id = 11,
            description = "Move forward",
            action = CardAction.MOVE_FORWARD,
            moveSpaces = 3
        )

        assertEquals(CardAction.MOVE_FORWARD, card.action)
        assertEquals(3, card.moveSpaces)
    }

    @Test
    fun `test card go to jail action`() {
        val card = TestCard(
            id = 12,
            description = "Go to jail",
            action = CardAction.GO_TO_JAIL
        )

        assertEquals(CardAction.GO_TO_JAIL, card.action)
        assertEquals(0, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test card get out of jail action`() {
        val card = TestCard(
            id = 13,
            description = "Get out of jail",
            action = CardAction.GET_OUT_OF_JAIL
        )

        assertEquals(CardAction.GET_OUT_OF_JAIL, card.action)
    }

    @Test
    fun `test card pay each player action`() {
        val card = TestCard(
            id = 14,
            description = "Pay each player",
            action = CardAction.PAY_EACH_PLAYER,
            amount = 50
        )

        assertEquals(CardAction.PAY_EACH_PLAYER, card.action)
        assertEquals(50, card.amount)
    }

    @Test
    fun `test card collect from each player action`() {
        val card = TestCard(
            id = 15,
            description = "Collect from each player",
            action = CardAction.COLLECT_FROM_EACH,
            amount = 25
        )

        assertEquals(CardAction.COLLECT_FROM_EACH, card.action)
        assertEquals(25, card.amount)
    }

    @Test
    fun `test two cards with same values have same properties`() {
        val card1 = TestCard(1, "Same", CardAction.COLLECT_MONEY, amount = 100)
        val card2 = TestCard(1, "Same", CardAction.COLLECT_MONEY, amount = 100)

        assertEquals(card1.id, card2.id)
        assertEquals(card1.description, card2.description)
        assertEquals(card1.action, card2.action)
        assertEquals(card1.amount, card2.amount)
    }
}