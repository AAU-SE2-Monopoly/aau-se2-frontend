package at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketbrokerdemo.model.card.ChanceCard
import at.aau.serg.websocketbrokerdemo.model.card.CommunityChestCard
import at.aau.serg.websocketbrokerdemo.model.enums.CardAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CardTest {

    @Test
    fun `verify ChanceCard properties`() {
        val card = ChanceCard(
            id = 1,
            description = "Go to Jail",
            action = CardAction.MOVE_TO,
            targetFieldId = 10
        )

        assertEquals(1, card.id)
        assertEquals("Go to Jail", card.description)
        assertEquals(CardAction.MOVE_TO, card.action)
        assertEquals(10, card.targetFieldId)
        assertEquals(0, card.amount)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `verify CommunityChestCard properties`() {
        val card = CommunityChestCard(
            id = 2,
            description = "Bank error in your favor",
            action = CardAction.COLLECT_MONEY,
            amount = 200
        )

        assertEquals(2, card.id)
        assertEquals("Bank error in your favor", card.description)
        assertEquals(CardAction.COLLECT_MONEY, card.action)
        assertEquals(200, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }


}
