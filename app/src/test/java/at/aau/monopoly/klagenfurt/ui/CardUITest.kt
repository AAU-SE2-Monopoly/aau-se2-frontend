package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class CardUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ChanceCard shows CHANCE header`() {
        val card = ChanceCard(id = 1, description = "Advance to Go", action = CardAction.COLLECT_MONEY, amount = 200)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("CHANCE").assertExists()
    }

    @Test
    fun `CommunityChestCard shows COMMUNITY CHEST header`() {
        val card = CommunityChestCard(id = 2, description = "Pay doctor fee", action = CardAction.PAY_MONEY, amount = 50)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("COMMUNITY CHEST").assertExists()
    }

    @Test
    fun `ChanceCard shows description text`() {
        val card = ChanceCard(id = 1, description = "Advance to Go. Collect $200.", action = CardAction.COLLECT_MONEY, amount = 200)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Advance to Go. Collect $200.").assertExists()
    }

    @Test
    fun `COLLECT_MONEY action shows collect and amount`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.COLLECT_MONEY, amount = 200)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Collect").assertExists()
        composeTestRule.onNodeWithText("+\$\$200", substring = true).assertExists()
    }

    @Test
    fun `PAY_MONEY action shows pay and amount`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.PAY_MONEY, amount = 50)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Pay").assertExists()
        composeTestRule.onNodeWithText("-\$\$50", substring = true).assertExists()
    }

    @Test
    fun `COLLECT_FROM_EACH action shows from each`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.COLLECT_FROM_EACH, amount = 10)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("From each").assertExists()
        composeTestRule.onNodeWithText("+\$\$10", substring = true).assertExists()
    }

    @Test
    fun `PAY_EACH_PLAYER action shows pay each`() {
        val card = CommunityChestCard(id = 1, description = "Test", action = CardAction.PAY_EACH_PLAYER, amount = 50)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Pay each").assertExists()
        composeTestRule.onNodeWithText("-\$\$50", substring = true).assertExists()
    }

    @Test
    fun `MOVE_TO action shows advance to`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.MOVE_TO, targetFieldId = 5)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Advance to").assertExists()
        composeTestRule.onNodeWithText("Field #5").assertExists()
    }

    @Test
    fun `MOVE_TO with null target shows question mark`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.MOVE_TO)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Field #?").assertExists()
    }

    @Test
    fun `MOVE_FORWARD action shows spaces`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.MOVE_FORWARD, moveSpaces = 3)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Move").assertExists()
        composeTestRule.onNodeWithText("3 spaces").assertExists()
    }

    @Test
    fun `GO_TO_JAIL action shows go to jail`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.GO_TO_JAIL)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Go to Jail").assertExists()
    }

    @Test
    fun `GET_OUT_OF_JAIL action shows jail free`() {
        val card = ChanceCard(id = 1, description = "Test", action = CardAction.GET_OUT_OF_JAIL)
        composeTestRule.setContent { CardUI(card = card) }
        composeTestRule.onNodeWithText("Jail Free").assertExists()
    }
}

