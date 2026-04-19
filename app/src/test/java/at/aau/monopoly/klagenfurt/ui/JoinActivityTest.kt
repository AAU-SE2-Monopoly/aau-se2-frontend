package at.aau.monopoly.klagenfurt.ui

import android.content.Intent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.JoinActivity
import at.aau.monopoly.klagenfurt.ServiceLocator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JoinActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var fakeService: FakeGameService

    @Before
    fun setup() {
        fakeService = FakeGameService()
        ServiceLocator.injectGameServiceForTest(fakeService)
    }

    @After
    fun tearDown() {
        ServiceLocator.resetForTests()
    }

    @Test
    fun testJoinExistingGame_scenario() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
            putExtra("gameId", "test-game-123")
            putExtra("isNewGame", false)
        }
        
        ActivityScenario.launch<JoinActivity>(intent).use {
            composeTestRule.waitForIdle()

            // Check title via Tag AND Text to be unique
            composeTestRule.onNode(hasTestTag("ScreenTitle") and hasText("JOIN GAME")).assertExists()
            
            // Input name
            composeTestRule.onNodeWithTag("PlayerNameInput").performTextInput("Alice")
            
            composeTestRule.waitForIdle()

            // Specifically click the ActionButton
            composeTestRule.onNodeWithTag("ActionButton").assertExists().performClick()
            
            // Verify service calls
            assertEquals(1, fakeService.joinGameCalls)
            assertEquals("Alice", fakeService.lastJoinedPlayerName)
            assertEquals("test-game-123", fakeService.lastJoinedGameId)
        }
    }

    @Test
    fun testCreateNewGame_scenario() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
            putExtra("isNewGame", true)
        }
        
        ActivityScenario.launch<JoinActivity>(intent).use {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("ScreenTitle").assertExists()
            
            // Input name
            composeTestRule.onNodeWithTag("PlayerNameInput").performTextInput("Bob")
            
            // Click create via tag
            composeTestRule.onNodeWithTag("ActionButton").performClick()
            
            // Verify service calls
            assertEquals(1, fakeService.createGameCalls)
            assertEquals("Bob", fakeService.lastCreatedPlayerName)
        }
    }

    @Test
    fun testIconCycling_logic() {
        ActivityScenario.launch(JoinActivity::class.java).use {
            composeTestRule.onNodeWithText("Tap to change icon").assertExists()
        }
    }
}
