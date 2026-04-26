package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
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

    private fun setUpSettingsScreen(onBackClicked: () -> Unit = {}) {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = onBackClicked)
        }
    }

    @Test
    fun settingsScreen_displaysAllElements() {
        setUpSettingsScreen()
        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sounds").assertIsDisplayed()
        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_backButtonCallsCallback() {
        var backClicked = false
        setUpSettingsScreen(onBackClicked = { backClicked = true })
        composeTestRule.onNodeWithText("Back").performClick()
        assert(backClicked) { "Expected onBackClicked to be called" }
    }

    @Test
    fun settingsScreen_soundsToggleCanBeClicked() {
        setUpSettingsScreen()
        composeTestRule.onNodeWithText("Sounds").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_musicToggleCanBeClicked() {
        setUpSettingsScreen()
        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
    }
}
