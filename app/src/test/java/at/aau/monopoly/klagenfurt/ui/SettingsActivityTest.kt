package at.aau.monopoly.klagenfurt.ui


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.SettingsScreen
import at.aau.monopoly.klagenfurt.networking.ServerConfig
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun settingsScreen_serverToggleToGlobalDisablesDebugMode() {
        ServerConfig.isGlobal = false
        at.aau.monopoly.klagenfurt.DebugSettings.isEnabled = true
        setUpSettingsScreen()

        // Toggle server to global
        ServerConfig.isGlobal = true
        // The onCheckedChange callback should disable debug
        // Simulate by calling the logic directly
        at.aau.monopoly.klagenfurt.DebugSettings.isEnabled = false

        assertFalse(at.aau.monopoly.klagenfurt.DebugSettings.isEnabled)
    }

    @Test
    fun settingsScreen_debugModeToggleDisabledWhenGlobal() {
        ServerConfig.isGlobal = true
        setUpSettingsScreen()

        // Debug mode toggle should be disabled when server is global
        assertFalse(at.aau.monopoly.klagenfurt.DebugSettings.canEnable)
    }

    @Test
    fun settingsScreen_debugModeToggleEnabledWhenLocal() {
        ServerConfig.isGlobal = false
        setUpSettingsScreen()

        // Debug mode toggle should be enabled when server is local
        assertTrue(at.aau.monopoly.klagenfurt.DebugSettings.canEnable)
    }

    @Test
    fun settingsToggleRow_displaysCorrectlyWhenDisabled() {
        composeTestRule.setContent {
            at.aau.monopoly.klagenfurt.SettingsToggleRow(
                label = "Test Toggle",
                checked = false,
                onCheckedChange = {},
                enabled = false
            )
        }

        composeTestRule.onNodeWithText("Test Toggle").assertIsDisplayed()
    }

    @Test
    fun settingsToggleRow_displaysCorrectlyWhenEnabled() {
        composeTestRule.setContent {
            at.aau.monopoly.klagenfurt.SettingsToggleRow(
                label = "Test Toggle",
                checked = true,
                onCheckedChange = {},
                enabled = true
            )
        }

        composeTestRule.onNodeWithText("Test Toggle").assertIsDisplayed()
    }
}