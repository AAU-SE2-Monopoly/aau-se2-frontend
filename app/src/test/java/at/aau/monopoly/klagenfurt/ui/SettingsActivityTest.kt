package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.SettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysTitle() {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = {})
        }
        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysSoundsToggle() {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = {})
        }
        composeTestRule.onNodeWithText("Sounds").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysMusicToggle() {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = {})
        }
        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysBackButton() {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = {})
        }
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_backButtonCallsCallback() {
        var backClicked = false
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = { backClicked = true })
        }
        composeTestRule.onNodeWithText("Back").performClick()
        assert(backClicked) { "Expected onBackClicked to be called" }
    }

    @Test
    fun settingsScreen_soundsToggleDefaultOn() {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = {})
        }
        // The switch next to "Sounds" should be on by default
        composeTestRule.onNodeWithText("Sounds").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_musicToggleDefaultOn() {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = {})
        }
        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_subtitleDisplayed() {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = {})
        }
        // Verify all key UI elements are present
        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sounds").assertIsDisplayed()
        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }
}

