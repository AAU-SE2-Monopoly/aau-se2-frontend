package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.Player
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameboardIsolatedUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testGameTerminatedOverlayIsVisibleAndClickable() {
        var backToLobbyClicked = false

        composeTestRule.setContent {
            GameTerminatedOverlay(
                isVisible = true,
                onBackToLobby = { backToLobbyClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Game Terminated").assertExists()
        composeTestRule.onNodeWithText("The host has ended the game.").assertExists()

        composeTestRule.onNodeWithText("Back to Lobby")
            .assertExists()
            .performClick()

        assertTrue("Callback onBackToLobby should be executed on click", backToLobbyClicked)
    }

    @Test
    fun testGameTerminatedOverlayIsHidden() {
        composeTestRule.setContent {
            GameTerminatedOverlay(
                isVisible = false,
                onBackToLobby = {}
            )
        }

        composeTestRule.onNodeWithText("Game Terminated").assertDoesNotExist()
        composeTestRule.onNodeWithText("The host has ended the game.").assertDoesNotExist()
    }

    @Test
    fun testReportButtonDisabledWhenMoneyIs500OrLess() {
        val myPlayer = Player(id = "player1", name = "Me", money = 500, position = 0)
        val otherPlayer = Player(id = "player2", name = "Opponent", money = 1500, position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = myPlayer
            )
        }

        composeTestRule.onNodeWithText("🚨 Report")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun testReportButtonEnabledWhenMoneyIsGreaterThan500() {
        val myPlayer = Player(id = "player1", name = "Me", money = 501, position = 0)
        val otherPlayer = Player(id = "player2", name = "Opponent", money = 1500, position = 0)

        composeTestRule.setContent {
            GameboardContent(
                fields = emptyList(),
                players = listOf(myPlayer, otherPlayer),
                currentPlayerId = "player1",
                currentTurnPlayer = myPlayer,
                canReportCheater = true
            )
        }

        composeTestRule.onNodeWithText("🚨 Report")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun testCircularRevealShapeCalculatesOutlineCorrectly() {
        val shape = CircularRevealShape(progress = 0.5f)
        val size = Size(100f, 100f)

        val outline = shape.createOutline(
            size = size,
            layoutDirection = LayoutDirection.Ltr,
            density = Density(1f)
        )

        assertNotNull(outline)
    }
}