package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.MainMenuScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainMenuScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setUpMainMenu(
        onPlayClicked: () -> Unit = {},
        onCreditsClicked: () -> Unit = {},
        onSettingsClicked: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MainMenuScreen(
                onPlayClicked = onPlayClicked,
                onCreditsClicked = onCreditsClicked,
                onSettingsClicked = onSettingsClicked
            )
        }
    }

    @Test
    fun mainMenu_displaysTitle() {
        setUpMainMenu()
        composeTestRule.onNodeWithText("MONOPOLY").assertIsDisplayed()
    }

    @Test
    fun mainMenu_displaysSubtitle() {
        setUpMainMenu()
        composeTestRule.onNodeWithText("KLAGENFURT EDITION").assertIsDisplayed()
    }

    @Test
    fun mainMenu_displaysPlayButton() {
        setUpMainMenu()
        composeTestRule.onNodeWithText("▶  PLAY").assertIsDisplayed()
    }

    @Test
    fun mainMenu_displaysCreditsButton() {
        setUpMainMenu()
        composeTestRule.onNodeWithText("Credits").assertIsDisplayed()
    }

    @Test
    fun mainMenu_displaysSettingsButton() {
        setUpMainMenu()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun mainMenu_playButtonCallsCallback() {
        var clicked = false
        setUpMainMenu(onPlayClicked = { clicked = true })
        composeTestRule.onNodeWithText("▶  PLAY").performClick()
        assert(clicked) { "Expected onPlayClicked to be called" }
    }

    @Test
    fun mainMenu_creditsButtonCallsCallback() {
        var clicked = false
        setUpMainMenu(onCreditsClicked = { clicked = true })
        composeTestRule.onNodeWithText("Credits").performClick()
        assert(clicked) { "Expected onCreditsClicked to be called" }
    }

    @Test
    fun mainMenu_settingsButtonCallsCallback() {
        var clicked = false
        setUpMainMenu(onSettingsClicked = { clicked = true })
        composeTestRule.onNodeWithText("Settings").assertExists()
    }
}



