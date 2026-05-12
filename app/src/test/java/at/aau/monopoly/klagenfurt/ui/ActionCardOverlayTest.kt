package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.card.CommunityChestCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ActionCardOverlayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // ============ VISIBILITY TESTS ============

    @Test
    fun actionCardOverlay_notDisplayed_whenNotVisible() {
        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = false,
                card = null,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        // Should not find any card elements
        composeTestRule.onNodeWithText("CHANCE CARD").assertIsNotDisplayed()
    }

    @Test
    fun actionCardOverlay_displayed_whenVisible_withCard() {
        val card = ChanceCard(
            id = 1,
            description = "Advance to Go",
            action = CardAction.COLLECT_MONEY,
            amount = 200
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("🎰 CHANCE CARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Advance to Go").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_notDisplayed_whenCardIsNull() {
        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = null,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        // Overlay should not show for null card
        composeTestRule.onNodeWithText("CHANCE CARD").assertIsNotDisplayed()
    }

    // ============ CARD TYPE DISPLAY TESTS ============

    @Test
    fun actionCardOverlay_displays_chanceCardHeader() {
        val card = ChanceCard(
            id = 1,
            description = "Test Chance",
            action = CardAction.MOVE_FORWARD,
            moveSpaces = 5
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("🎰 CHANCE CARD").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_communityChestCardHeader() {
        val card = CommunityChestCard(
            id = 2,
            description = "Test Community Chest",
            action = CardAction.PAY_MONEY,
            amount = 100
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("⭐ COMMUNITY CHEST").assertIsDisplayed()
    }

    // ============ ACTION DETAILS DISPLAY TESTS ============

    @Test
    fun actionCardOverlay_displays_collectMoney_action() {
        val card = ChanceCard(
            id = 1,
            description = "Collect money",
            action = CardAction.COLLECT_MONEY,
            amount = 200
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Collect Money").assertIsDisplayed()
        composeTestRule.onNodeWithText("+\$200").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_payMoney_action() {
        val card = ChanceCard(
            id = 1,
            description = "Pay money",
            action = CardAction.PAY_MONEY,
            amount = 150
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pay Money").assertIsDisplayed()
        composeTestRule.onNodeWithText("-\$150").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_collectFromEach_action() {
        val card = CommunityChestCard(
            id = 2,
            description = "Collect from players",
            action = CardAction.COLLECT_FROM_EACH,
            amount = 50
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Collect From Each Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("+\$50 each").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_payEachPlayer_action() {
        val card = ChanceCard(
            id = 1,
            description = "Pay each player",
            action = CardAction.PAY_EACH_PLAYER,
            amount = 40
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pay Each Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("-\$40 each").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_moveTo_action() {
        val card = ChanceCard(
            id = 1,
            description = "Move to field",
            action = CardAction.MOVE_TO,
            targetFieldId = 39
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Advance to Field").assertIsDisplayed()
        composeTestRule.onNodeWithText("#39").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_moveForward_action() {
        val card = ChanceCard(
            id = 1,
            description = "Move forward",
            action = CardAction.MOVE_FORWARD,
            moveSpaces = 3
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Move Forward").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 spaces").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_goToJail_action() {
        val card = ChanceCard(
            id = 1,
            description = "Go to jail",
            action = CardAction.GO_TO_JAIL
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("🚔").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go to Jail").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_getOutOfJail_action() {
        val card = CommunityChestCard(
            id = 2,
            description = "Get out of jail",
            action = CardAction.GET_OUT_OF_JAIL
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("🔓").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Out of Jail Free").assertIsDisplayed()
    }

    // ============ BUTTON STATE TESTS ============

    @Test
    fun actionCardOverlay_button_enabled_whenNotExecuting() {
        val card = ChanceCard(
            id = 1,
            description = "Test card",
            action = CardAction.COLLECT_MONEY,
            amount = 100
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("✓ Execute Action").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_button_disabled_whenExecuting() {
        val card = ChanceCard(
            id = 1,
            description = "Test card",
            action = CardAction.COLLECT_MONEY,
            amount = 100
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = true,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("⏳ Executing...").assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_button_click_triggers_executeAction() {
        var executeCallCount = 0
        val card = ChanceCard(
            id = 1,
            description = "Test card",
            action = CardAction.COLLECT_MONEY,
            amount = 100
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = { executeCallCount++ }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("✓ Execute Action").performClick()

        assert(executeCallCount == 1) { "Execute action should be called once" }
    }

    // ============ DESCRIPTION DISPLAY TEST ============

    @Test
    fun actionCardOverlay_displays_card_description() {
        val description = "You have won a beauty contest. Collect $100 from the bank."
        val card = CommunityChestCard(
            id = 2,
            description = description,
            action = CardAction.COLLECT_MONEY,
            amount = 100
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(description).assertIsDisplayed()
    }

    // ============ INSTRUCTION TEXT TEST ============

    @Test
    fun actionCardOverlay_displays_instruction_text() {
        val card = ChanceCard(
            id = 1,
            description = "Test",
            action = CardAction.COLLECT_MONEY,
            amount = 50
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = false,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Press the button above to execute this action")
            .assertIsDisplayed()
    }

    @Test
    fun actionCardOverlay_displays_executing_status_text() {
        val card = ChanceCard(
            id = 1,
            description = "Test",
            action = CardAction.COLLECT_MONEY,
            amount = 50
        )

        composeTestRule.setContent {
            ActionCardOverlay(
                isVisible = true,
                card = card,
                isExecuting = true,
                onExecuteAction = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Executing action...").assertIsDisplayed()
    }
}




