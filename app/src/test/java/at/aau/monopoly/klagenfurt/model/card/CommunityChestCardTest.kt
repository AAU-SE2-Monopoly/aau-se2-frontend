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

        // Check default values
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test community chest card data class equality`() {
        val card1 = CommunityChestCard(1, "Arztgebühr", CardAction.PAY_MONEY, amount = 50)
        val card2 = CommunityChestCard(1, "Arztgebühr", CardAction.PAY_MONEY, amount = 50)
        val card3 = CommunityChestCard(2, "Arztgebühr", CardAction.PAY_MONEY, amount = 50)

        // Tests the generated equals() and hashCode() methods
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
        val card = CommunityChestCard(1, "Inheritance", CardAction.COLLECT_MONEY, amount = 100)
        val stringRepresentation = card.toString()

        // Ensures that toString() correctly represents the data class
        assertTrue(stringRepresentation.contains("CommunityChestCard"))
        assertTrue(stringRepresentation.contains("id=1"))
        assertTrue(stringRepresentation.contains("amount=100"))
    }
    @Test
    fun `test community chest card with target field`() {
        val card = CommunityChestCard(
            id = 11,
            description = "Advance to Go",
            action = CardAction.MOVE_TO,
            targetFieldId = 0
        )

        assertEquals(11, card.id)
        assertEquals(CardAction.MOVE_TO, card.action)
        assertEquals(0, card.targetFieldId)
        assertEquals(0, card.amount)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `test community chest card with move spaces`() {
        val card = CommunityChestCard(
            id = 12,
            description = "Move forward 3 spaces",
            action = CardAction.MOVE_FORWARD,
            moveSpaces = 3
        )

        assertEquals(12, card.id)
        assertEquals(CardAction.MOVE_FORWARD, card.action)
        assertEquals(3, card.moveSpaces)
        assertEquals(0, card.amount)
        assertNull(card.targetFieldId)
    }

    @Test
    fun `test community chest card get out of jail action`() {
        val card = CommunityChestCard(
            id = 13,
            description = "Get out of jail free",
            action = CardAction.GET_OUT_OF_JAIL
        )

        assertEquals(CardAction.GET_OUT_OF_JAIL, card.action)
        assertEquals(0, card.amount)
        assertEquals(0, card.moveSpaces)
        assertNull(card.targetFieldId)
    }

    @Test
    fun `test community chest card pay each player action`() {
        val card = CommunityChestCard(
            id = 14,
            description = "Pay each player",
            action = CardAction.PAY_EACH_PLAYER,
            amount = 50
        )

        assertEquals(CardAction.PAY_EACH_PLAYER, card.action)
        assertEquals(50, card.amount)
    }

    @Test
    fun `test community chest card collect from each player action`() {
        val card = CommunityChestCard(
            id = 15,
            description = "Collect from each player",
            action = CardAction.COLLECT_FROM_EACH,
            amount = 25
        )

        assertEquals(CardAction.COLLECT_FROM_EACH, card.action)
        assertEquals(25, card.amount)
    }

    @Test
    fun `test community chest card component functions`() {
        val card = CommunityChestCard(
            id = 16,
            description = "Test card",
            action = CardAction.COLLECT_MONEY,
            amount = 100,
            targetFieldId = 5,
            moveSpaces = 2
        )

        assertEquals(16, card.component1())
        assertEquals("Test card", card.component2())
        assertEquals(CardAction.COLLECT_MONEY, card.component3())
        assertEquals(100, card.component4())
        assertEquals(5, card.component5())
        assertEquals(2, card.component6())
    }

    @Test
    fun `test community chest card copy with all fields changed`() {
        val original = CommunityChestCard(
            id = 1,
            description = "Original",
            action = CardAction.COLLECT_MONEY,
            amount = 100,
            targetFieldId = 0,
            moveSpaces = 1
        )

        val copy = original.copy(
            id = 2,
            description = "Changed",
            action = CardAction.MOVE_FORWARD,
            amount = 0,
            targetFieldId = null,
            moveSpaces = 4
        )

        assertEquals(2, copy.id)
        assertEquals("Changed", copy.description)
        assertEquals(CardAction.MOVE_FORWARD, copy.action)
        assertEquals(0, copy.amount)
        assertNull(copy.targetFieldId)
        assertEquals(4, copy.moveSpaces)
    }
}