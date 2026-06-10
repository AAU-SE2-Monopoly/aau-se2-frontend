package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.model.TradeOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for TradeOffer extensions and trade logic from GameboardUI
 */
class TradeOfferUtilsTest {

    @Test
    fun `testHasTradeContentsWithMoney`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            requestMoney = 0,
            offerPropertyIds = emptyList(),
            requestPropertyIds = emptyList(),
            offerJailCards = 0,
            requestJailCards = 0
        )

        assertTrue(offer.hasTradeContents())
    }

    @Test
    fun `testHasTradeContentsWithProperties`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 0,
            requestMoney = 0,
            offerPropertyIds = listOf(1, 2),
            requestPropertyIds = emptyList(),
            offerJailCards = 0,
            requestJailCards = 0
        )

        assertTrue(offer.hasTradeContents())
    }

    @Test
    fun `testHasTradeContentsWithJailCards`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 0,
            requestMoney = 0,
            offerPropertyIds = emptyList(),
            requestPropertyIds = emptyList(),
            offerJailCards = 1,
            requestJailCards = 0
        )

        assertTrue(offer.hasTradeContents())
    }

    @Test
    fun `testHasTradeContentsEmpty`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 0,
            requestMoney = 0,
            offerPropertyIds = emptyList(),
            requestPropertyIds = emptyList(),
            offerJailCards = 0,
            requestJailCards = 0
        )

        assertFalse(offer.hasTradeContents())
    }

    @Test
    fun `testTradeOfferBothSidesOffer`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            requestMoney = 50,
            offerPropertyIds = listOf(1),
            requestPropertyIds = listOf(2),
            offerJailCards = 0,
            requestJailCards = 1
        )

        assertTrue(offer.hasTradeContents())
        assertTrue(offer.offerMoney > 0)
        assertTrue(offer.requestMoney > 0)
        assertEquals(1, offer.offerPropertyIds.size)
        assertEquals(1, offer.requestPropertyIds.size)
    }

    @Test
    fun `testIntSetToggle_AddToSet`() {
        val set = setOf(1, 2, 3)
        val toggled = set.toggle(4)

        assertTrue(4 in toggled)
        assertEquals(4, toggled.size)
    }

    @Test
    fun `testIntSetToggle_RemoveFromSet`() {
        val set = setOf(1, 2, 3)
        val toggled = set.toggle(2)

        assertFalse(2 in toggled)
        assertEquals(2, toggled.size)
    }

    @Test
    fun `testIntSetToggle_EmptySet`() {
        val set: Set<Int> = emptySet()
        val toggled = set.toggle(1)

        assertTrue(1 in toggled)
        assertEquals(1, toggled.size)
    }

    @Test
    fun `testTradeOfferDefaults`() {
        val offer = TradeOffer()

        assertEquals("", offer.id)
        assertEquals("", offer.fromPlayerId)
        assertEquals("", offer.toPlayerId)
        assertEquals(0, offer.offerMoney)
        assertEquals(0, offer.requestMoney)
        assertEquals(emptyList<Int>(), offer.offerPropertyIds)
        assertEquals(emptyList<Int>(), offer.requestPropertyIds)
        assertEquals(0, offer.offerJailCards)
        assertEquals(0, offer.requestJailCards)
        assertEquals(emptyList<String>(), offer.acceptedByPlayerIds)
    }

    @Test
    fun `testTradeOfferWithAcceptsFromBothPlayers`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            requestMoney = 50,
            acceptedByPlayerIds = listOf("p1", "p2")
        )

        assertTrue(offer.acceptedByPlayerIds.contains("p1"))
        assertTrue(offer.acceptedByPlayerIds.contains("p2"))
        assertEquals(2, offer.acceptedByPlayerIds.size)
    }

    @Test
    fun `testTradeOfferWithPartialAccepts`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            requestMoney = 50,
            acceptedByPlayerIds = listOf("p1")
        )

        assertTrue(offer.acceptedByPlayerIds.contains("p1"))
        assertFalse(offer.acceptedByPlayerIds.contains("p2"))
        assertEquals(1, offer.acceptedByPlayerIds.size)
    }

    @Test
    fun `testTradeOfferNoAccepts`() {
        val offer = TradeOffer(
            id = "t1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            requestMoney = 50
        )

        assertEquals(emptyList<String>(), offer.acceptedByPlayerIds)
    }
}

// Extension functions copied from GameboardUI.kt for testing
private fun Set<Int>.toggle(fieldId: Int): Set<Int> =
    if (fieldId in this) this - fieldId else this + fieldId

private fun TradeOffer.hasTradeContents(): Boolean =
    offerMoney > 0 ||
        requestMoney > 0 ||
        offerPropertyIds.isNotEmpty() ||
        requestPropertyIds.isNotEmpty() ||
        offerJailCards > 0 ||
        requestJailCards > 0
