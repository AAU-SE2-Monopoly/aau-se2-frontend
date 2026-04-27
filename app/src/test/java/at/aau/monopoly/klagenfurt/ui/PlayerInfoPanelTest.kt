package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.card.CommunityChestCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlayerInfoPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val player = Player(id = "p1", name = "Alice", money = 1200, iconId = "lindwurm")

    @Test
    fun `shows player name`() {
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = emptyList())
        }
        composeTestRule.onNodeWithText("Alice").assertExists()
    }

    @Test
    fun `shows player money`() {
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = emptyList())
        }
        composeTestRule.onNodeWithText("$1200", substring = true).assertExists()
    }

    @Test
    fun `shows current turn indicator when isCurrentTurn`() {
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = emptyList(), isCurrentTurn = true)
        }
        composeTestRule.onNodeWithText("⭐ Current Turn").assertExists()
    }

    @Test
    fun `does not show current turn indicator when not current turn`() {
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = emptyList(), isCurrentTurn = false)
        }
        composeTestRule.onNodeWithText("⭐ Current Turn").assertDoesNotExist()
    }

    @Test
    fun `shows in jail status when player is in jail`() {
        val jailedPlayer = player.copy(inJail = true, jailTurns = 2)
        composeTestRule.setContent {
            PlayerInfoPanel(player = jailedPlayer, fields = emptyList())
        }
        composeTestRule.onNodeWithText("🔒 In Jail (2)").assertExists()
    }

    @Test
    fun `does not show jail when player not in jail`() {
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = emptyList())
        }
        composeTestRule.onNodeWithText("🔒 In Jail (0)").assertDoesNotExist()
    }

    @Test
    fun `shows bankrupt text when player is bankrupt`() {
        val bankruptPlayer = Player(id = "p1", name = "Broke", money = 0, ownedPropertyIds = mutableListOf())
        composeTestRule.setContent {
            PlayerInfoPanel(player = bankruptPlayer, fields = emptyList())
        }
        composeTestRule.onNodeWithText("💀 BANKRUPT").assertExists()
    }

    @Test
    fun `does not show bankrupt when player has money`() {
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = emptyList())
        }
        composeTestRule.onNodeWithText("💀 BANKRUPT").assertDoesNotExist()
    }

    @Test
    fun `renders owned property cards`() {
        val property = PropertyField(
            id = 1, name = "Herrengasse", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50, ownerId = "p1"
        )
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = listOf(property))
        }
        composeTestRule.onNodeWithText("Herrengasse").assertExists()
    }

    @Test
    fun `renders owned railroad cards`() {
        val railroad = RailroadField(id = 5, name = "Hauptbahnhof", ownerId = "p1")
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = listOf(railroad))
        }
        composeTestRule.onNodeWithText("Hauptbahnhof").assertExists()
    }

    @Test
    fun `renders owned utility cards`() {
        val utility = UtilityField(id = 12, name = "Kelag Klagenfurt", ownerId = "p1")
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = listOf(utility))
        }
        composeTestRule.onNodeWithText("Kelag Klagenfurt").assertExists()
    }

    @Test
    fun `does not show fields owned by other players`() {
        val otherProperty = PropertyField(
            id = 1, name = "Herrengasse", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2), houseCost = 50, hotelCost = 50, ownerId = "other"
        )
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = listOf(otherProperty))
        }
        composeTestRule.onNodeWithText("Herrengasse").assertDoesNotExist()
    }

    @Test
    fun `renders with cards list`() {
        val chanceCard = ChanceCard(id = 1, description = "Get out of Jail Free", action = CardAction.GET_OUT_OF_JAIL)
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = emptyList(), cards = listOf(chanceCard))
        }
        composeTestRule.onNodeWithText("Get out of Jail Free").assertExists()
    }

    @Test
    fun `renders as own player without crash`() {
        val property = PropertyField(
            id = 1, name = "Herrengasse", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2), houseCost = 50, hotelCost = 50, ownerId = "p1"
        )
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = listOf(property), isOwnPlayer = true)
        }
        composeTestRule.onNodeWithText("Alice").assertExists()
        composeTestRule.onNodeWithText("Herrengasse").assertExists()
    }

    @Test
    fun `renders multiple grouped properties`() {
        val brown1 = PropertyField(id = 1, name = "Herrengasse", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2), houseCost = 50, hotelCost = 50, ownerId = "p1")
        val blue1 = PropertyField(id = 6, name = "Neuer Platz", color = PropertyColor.LIGHT_BLUE,
            price = 100, rent = listOf(6), houseCost = 50, hotelCost = 50, ownerId = "p1")
        composeTestRule.setContent {
            PlayerInfoPanel(player = player, fields = listOf(brown1, blue1))
        }
        composeTestRule.onNodeWithText("Herrengasse").assertExists()
        composeTestRule.onNodeWithText("Neuer Platz").assertExists()
    }
}
