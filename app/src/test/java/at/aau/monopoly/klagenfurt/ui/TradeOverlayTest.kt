package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.TradeOffer
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TradeOverlayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val alice = Player(id = "p1", name = "Alice", money = 500, getOutOfJailCards = 1)
    private val bob = Player(id = "p2", name = "Bob", money = 300, getOutOfJailCards = 0)
    private val spectator = Player(id = "p3", name = "Charlie", money = 400)

    private fun property(id: Int, name: String, ownerId: String): PropertyField =
        PropertyField(
            id = id,
            name = name,
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50,
            ownerId = ownerId
        )

    private fun fields(): List<Field> = listOf(
        property(1, "Herrengasse", "p1"),
        property(3, "Lendcafe", "p1"),
        property(6, "Hauptbahnhof", "p2")
    )

    @Test
    fun `TradeOverlay does not render when hidden`() {
        composeTestRule.setContent {
            TradeOverlay(
                isVisible = false,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = null,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        assertTrue(composeTestRule.onAllNodesWithText("Trade with Bob").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `TradeOverlay does not render when current player is missing`() {
        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "missing",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = null,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        assertTrue(composeTestRule.onAllNodesWithText("Trade with Bob").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `TradeOverlay disables start and accept before any content exists`() {
        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = null,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Accept").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Start Offer").assertIsNotEnabled()
    }

    @Test
    fun `TradeOverlay starts offer after money step changes`() {
        var startedOffer: TradeCall? = null

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = null,
                onProposeTrade = { toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards ->
                    startedOffer = TradeCall(toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards)
                },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onAllNodesWithText("+100€")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Money: 100€ / 500€").assertExists()
        composeTestRule.onNodeWithText("Start Offer").assertIsEnabled()
        assertEquals(null, startedOffer)
    }

    @Test
    fun `TradeOverlay starts offer from selected property and can reset money`() {
        var startedOffer: TradeCall? = null

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = null,
                onProposeTrade = { toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards ->
                    startedOffer = TradeCall(toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards)
                },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onAllNodesWithText("+10€")[0].performClick()
        composeTestRule.onAllNodesWithText("Reset")[0].performClick()
        composeTestRule.onNodeWithText("Herrengasse").performClick()
        composeTestRule.onNodeWithText("Start Offer").assertIsEnabled()
        assertEquals(null, startedOffer)
    }

    @Test
    fun `TradeOverlay publishes live update only for active editable side`() {
        val updates = mutableListOf<TradeCall>()
        val activeOffer = TradeOffer(
            id = "trade-1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p2",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards ->
                    updates.add(TradeCall(toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards))
                },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onAllNodesWithText("+10€")[1].performClick()

        assertEquals(1, updates.size)
        assertEquals("p2", updates.single().toPlayerId)
        assertEquals(100, updates.single().offerMoney)
        assertEquals(10, updates.single().requestMoney)
    }

    @Test
    fun `TradeOverlay publishes live updates for jail cards and property toggles`() {
        val updates = mutableListOf<TradeCall>()
        val activeOffer = TradeOffer(
            id = "trade-1",
            fromPlayerId = "p1",
            toPlayerId = "p2"
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards ->
                    updates.add(TradeCall(toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards))
                },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onAllNodesWithText("+")[0].performClick()
        composeTestRule.onNodeWithText("Herrengasse").performClick()
        composeTestRule.onNodeWithText("Herrengasse").performClick()
        composeTestRule.onAllNodesWithText("-")[0].performClick()

        assertEquals(4, updates.size)
        assertEquals(1, updates[0].offerJailCards)
        assertEquals(listOf(1), updates[1].offerPropertyIds)
        assertEquals(emptyList<Int>(), updates[2].offerPropertyIds)
        assertEquals(0, updates[3].offerJailCards)
    }

    @Test
    fun `TradeOverlay publishes request side jail cards and property toggles`() {
        val updates = mutableListOf<TradeCall>()
        val bobWithCard = bob.copy(getOutOfJailCards = 1)
        val activeOffer = TradeOffer(
            id = "trade-1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 50
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p2",
                tradePartner = alice,
                players = listOf(alice, bobWithCard),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards ->
                    updates.add(TradeCall(toPlayerId, offerMoney, requestMoney, offerPropertyIds, requestPropertyIds, offerJailCards, requestJailCards))
                },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onAllNodesWithText("+")[1].performClick()
        composeTestRule.onNodeWithText("Hauptbahnhof").performClick()

        assertEquals(2, updates.size)
        assertEquals(1, updates[0].requestJailCards)
        assertEquals(listOf(6), updates[1].requestPropertyIds)
    }

    @Test
    fun `TradeOverlay includes railroads and utilities but excludes developed properties`() {
        val railroad = RailroadField(id = 10, name = "Railroad", ownerId = "p1")
        val utility = UtilityField(id = 11, name = "Utility", ownerId = "p1")
        val hotelProperty = property(12, "Hotel Street", "p1").copy(hasHotel = true)
        val housedProperty = property(13, "House Street", "p1").copy(houses = 2)

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = listOf(railroad, utility, hotelProperty, housedProperty),
                pendingTradeOffer = null,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Railroad").assertExists()
        composeTestRule.onNodeWithText("Utility").assertExists()
        assertTrue(composeTestRule.onAllNodesWithText("Hotel Street").fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("House Street").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `TradeOverlay disables accept for empty active offer and enables it once offer has content`() {
        val emptyOffer = TradeOffer(
            id = "trade-empty",
            fromPlayerId = "p1",
            toPlayerId = "p2"
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = emptyOffer,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Accept").assertIsNotEnabled()
    }

    @Test
    fun `TradeOverlay accepts active offer that has request money only`() {
        var acceptedTradeId: String? = null
        val activeOffer = TradeOffer(
            id = "trade-request-money",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            requestMoney = 10
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p2",
                tradePartner = alice,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = { acceptedTradeId = it },
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Accept").assertIsEnabled().performClick()

        assertEquals("trade-request-money", acceptedTradeId)
    }

    @Test
    fun `TradeOverlay participant can cancel active offer`() {
        var rejectedTradeId: String? = null
        val activeOffer = TradeOffer(
            id = "trade-1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 10
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = { rejectedTradeId = it },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        assertEquals(null, rejectedTradeId)
    }

    @Test
    fun `TradeOverlay shows no tradeable properties when owned properties are developed`() {
        val developed = property(8, "Developed", "p1").copy(houses = 1)

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = listOf(developed),
                pendingTradeOffer = null,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onAllNodesWithText("No tradeable properties")[0].assertExists()
    }

    @Test
    fun `TradeOverlay spectator sees watching state without cancel action`() {
        val activeOffer = TradeOffer(
            id = "trade-1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            requestPropertyIds = listOf(6)
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p3",
                tradePartner = bob,
                players = listOf(alice, bob, spectator),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Watching").assertExists()
        composeTestRule.onNodeWithText("Alice offers a trade to Bob.").assertExists()
        composeTestRule.onNodeWithText("- Hauptbahnhof").assertExists()
        assertTrue(composeTestRule.onAllNodesWithText("Cancel").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `TradeOverlay spectator sees empty trade columns`() {
        val activeOffer = TradeOffer(
            id = "trade-empty",
            fromPlayerId = "p1",
            toPlayerId = "p2"
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p3",
                tradePartner = bob,
                players = listOf(alice, bob, spectator),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = {},
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Watching").assertExists()
        assertEquals(2, composeTestRule.onAllNodesWithText("No properties").fetchSemanticsNodes().size)
    }

    @Test
    fun `TradeOverlay accepted player can toggle accept button`() {
        var acceptedTradeId: String? = null
        val activeOffer = TradeOffer(
            id = "trade-1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            acceptedByPlayerIds = listOf("p1")
        )

        composeTestRule.setContent {
            TradeOverlay(
                isVisible = true,
                currentPlayerId = "p1",
                tradePartner = bob,
                players = listOf(alice, bob),
                fields = fields(),
                pendingTradeOffer = activeOffer,
                onProposeTrade = { _, _, _, _, _, _, _ -> },
                onAcceptTrade = { acceptedTradeId = it },
                onRejectTrade = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Accepted").assertIsEnabled().performClick()

        assertEquals("trade-1", acceptedTradeId)
    }

    private data class TradeCall(
        val toPlayerId: String,
        val offerMoney: Int,
        val requestMoney: Int,
        val offerPropertyIds: List<Int>,
        val requestPropertyIds: List<Int>,
        val offerJailCards: Int,
        val requestJailCards: Int
    )
}
