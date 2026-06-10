package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GameOverOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gameOverOverlay_isNotDisplayed_whenNotVisible() {
        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = false,
                winnerName = "Alice",
                onBackToLobby = {}
            )
        }

        composeTestRule.onAllNodesWithText("🏆 Game Over").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Winner: Alice").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Back to Lobby").assertCountEquals(0)
    }

    @Test
    fun gameOverOverlay_displaysWinnerName_whenVisible() {
        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = true,
                winnerName = "Alice",
                onBackToLobby = {}
            )
        }

        composeTestRule.onNodeWithText("🏆 Game Over").assertIsDisplayed()
        composeTestRule.onNodeWithText("Winner: Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to Lobby").assertIsDisplayed()
    }

    @Test
    fun gameOverOverlay_displaysUnknown_whenWinnerNameIsNull() {
        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = true,
                winnerName = null,
                onBackToLobby = {}
            )
        }

        composeTestRule.onNodeWithText("Winner: Unknown").assertIsDisplayed()
    }

    @Test
    fun gameOverOverlay_callsBackToLobby_whenButtonClicked() {
        var clicked = false

        composeTestRule.setContent {
            GameOverOverlay(
                isVisible = true,
                winnerName = "Alice",
                onBackToLobby = {
                    clicked = true
                }
            )
        }

        composeTestRule.onNodeWithText("Back to Lobby").performClick()

        assertTrue(clicked)
    }
}