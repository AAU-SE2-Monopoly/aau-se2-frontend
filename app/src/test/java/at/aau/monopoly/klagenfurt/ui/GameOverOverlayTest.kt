package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.Player
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    qualifiers = "w360dp-h640dp",
    manifest = Config.NONE
)
class GameOverOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gameOverOverlay_isNotDisplayed_whenNotVisible() {
        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = false,
                activePlayers = listOf(Player(id = "p1", name = "Alice")),
                onBackToLobby = {}
            )
        }

        composeTestRule.onAllNodesWithText("Game Over").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Alice").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Back to Lobby").assertCountEquals(0)
    }

    @Test
    fun gameOverOverlay_displaysWinner_whenOnlyOneActivePlayerExists() {
        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = true,
                activePlayers = listOf(Player(id = "p1", name = "Alice", money = 1500)),
                onBackToLobby = {}
            )
        }

        composeTestRule.onNodeWithText("🏆").assertIsDisplayed()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
        composeTestRule.onNodeWithText("Winner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to Lobby").assertIsDisplayed()
    }

    @Test
    fun gameOverOverlay_displaysActivePlayers_whenMoreThanOnePlayerRemains() {
        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = true,
                activePlayers = listOf(
                    Player(id = "p1", name = "Alice", money = 1500, ownedPropertyIds = mutableListOf(1, 2)),
                    Player(id = "p2", name = "Bob", money = 900, ownedPropertyIds = mutableListOf(3))
                ),
                onBackToLobby = {}
            )
        }

        composeTestRule.onNodeWithText("Players still in game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("1500 € · 🏠 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("900 € · 🏠 1").assertIsDisplayed()
    }

    @Test
    fun gameOverOverlay_displaysUnknownWinner_whenNoActivePlayerExists() {
        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = true,
                activePlayers = emptyList(),
                onBackToLobby = {}
            )
        }

        composeTestRule.onNodeWithText("Players still in game").assertIsDisplayed()
    }

    @Test
    fun gameOverOverlay_callsBackToLobby_whenButtonClicked() {
        var clicked = false

        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = true,
                activePlayers = listOf(Player(id = "p1", name = "Alice")),
                onBackToLobby = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Back to Lobby").performClick()

        assertTrue(clicked)
    }
}