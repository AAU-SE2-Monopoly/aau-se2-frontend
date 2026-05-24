package at.aau.monopoly.klagenfurt.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.ui.GameViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngameChatTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun eventLogOverlay_switches_from_collapsed_to_expanded_and_shows_line() {
        val entries = listOf(
            GameViewModel.LogEntry(text = "State snapshot synced", eventType = "STATE_SNAPSHOT", isTechnical = true),
            GameViewModel.LogEntry(text = "Player Alice joined", eventType = "PLAYER_JOINED", isTechnical = false)
        )

        composeTestRule.setContent {
            EventLogOverlay(entries = entries)
        }

        composeTestRule.onNodeWithTag("ingame_chat_collapsed").assertExists()
        composeTestRule.onNodeWithTag("ingame_chat_expanded").assertDoesNotExist()

        composeTestRule.onNodeWithTag("ingame_chat_collapsed").performTouchInput {
            click()
        }

        composeTestRule.onNodeWithTag("ingame_chat_expanded").assertExists()
        composeTestRule.onNodeWithTag("ingame_chat_lines", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("• Player Alice joined", substring = true).assertExists()
    }
}

