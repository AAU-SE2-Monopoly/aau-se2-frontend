package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
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
    fun `TradeOverlay starts offer after money step changes`() {
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

        composeTestRule.onAllNodesWithText("+100€")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Money: 100€ / 500€").assertExists()
        composeTestRule.onNodeWithText("Start Offer").assertIsEnabled()
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
