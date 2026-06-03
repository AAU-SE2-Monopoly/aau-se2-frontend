package at.aau.monopoly.klagenfurt.ui


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.SettingsScreen
import at.aau.monopoly.klagenfurt.networking.ServerConfig
import org.junit.After
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

    @After
    fun tearDown() {
        ServerConfig.isGlobal = false
    }

    private fun setUpSettingsScreen(onBackClicked: () -> Unit = {}) {
        composeTestRule.setContent {
            SettingsScreen(onBackClicked = onBackClicked)
        }
    }

    @Test
    fun settingsScreen_displaysAllElements() {
        setUpSettingsScreen()
        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server: Local", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Debug Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()


        composeTestRule.onNodeWithText("Show Cheating Tutorial").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_serverToggleSwitchesToGlobal() {
        ServerConfig.isGlobal = false
        setUpSettingsScreen()
        composeTestRule.onNodeWithText("Server: Local", substring = true).assertIsDisplayed()
        // Programmatically toggle
        ServerConfig.isGlobal = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Server: Global", substring = true).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_backButtonCallsCallback() {
        var backClicked = false
        setUpSettingsScreen(onBackClicked = { backClicked = true })
        composeTestRule.onNodeWithText("Back").performClick()
        assert(backClicked) { "Expected onBackClicked to be called" }
    }

    @Test
    fun settingsScreen_debugModeToggleCanBeClicked() {
        setUpSettingsScreen()
        composeTestRule.onNodeWithText("Debug Mode").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_debugModeInfoTextDisplayed() {
        setUpSettingsScreen()
        composeTestRule.onNodeWithText("Debug mode is only available on local environment").assertIsDisplayed()
    }


    @Test
    fun settingsScreen_clickingCheatTutorialShowsDialog() {
        setUpSettingsScreen()


        composeTestRule.onNodeWithText("Cheat Code").assertDoesNotExist()


        composeTestRule.onNodeWithText("Show Cheating Tutorial").performClick()

        composeTestRule.onNodeWithText("Cheat Code").assertIsDisplayed()
        composeTestRule.onNodeWithText("Press the Volume Up button during your turn to automatically roll a double 6!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Got it").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_dismissingCheatTutorialHidesDialog() {
        setUpSettingsScreen()


        composeTestRule.onNodeWithText("Show Cheating Tutorial").performClick()
        composeTestRule.onNodeWithText("Cheat Code").assertIsDisplayed()


        composeTestRule.onNodeWithText("Got it").performClick()

        composeTestRule.onNodeWithText("Cheat Code").assertDoesNotExist()
    }
}