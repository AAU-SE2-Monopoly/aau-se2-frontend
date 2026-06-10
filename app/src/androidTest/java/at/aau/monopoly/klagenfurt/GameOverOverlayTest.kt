package at.aau.monopoly.klagenfurt


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.ui.GameOverOverlay
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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

        composeTestRule.onNodeWithText("🏆 Game Over").assertDoesNotExist()
        composeTestRule.onNodeWithText("Winner: Alice").assertDoesNotExist()
        composeTestRule.onNodeWithText("Back to Lobby").assertDoesNotExist()
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