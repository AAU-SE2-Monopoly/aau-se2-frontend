package at.aau.monopoly.klagenfurt.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.JoinActivity
import at.aau.monopoly.klagenfurt.ServiceLocator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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
    fun testJoinExistingGame_withValidName_andGameIdDisplay() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
            putExtra("gameId", "test-game-123456")
            putExtra("isNewGame", false)
        }

        ActivityScenario.launch<JoinActivity>(intent).use {
            composeTestRule.waitForIdle()

            // Checks if the title is correct
            composeTestRule.onNode(hasTestTag("ScreenTitle") and hasText("JOIN GAME")).assertExists()

            // Checks if the Game-ID is correctly truncated (take(8))
            composeTestRule.onNodeWithText("Game: test-gam…").assertIsDisplayed()

            // Input and click
            composeTestRule.onNodeWithTag("PlayerNameInput").performTextInput("Alice")
            composeTestRule.onNodeWithTag("ActionButton").performClick()

            // Verify service calls
            assertEquals(1, fakeService.joinGameCalls)
            assertEquals("Alice", fakeService.lastJoinedPlayerName)
            assertEquals("test-game-123456", fakeService.lastJoinedGameId)

            // Icon index 0 is default = lindwurm
            assertEquals("lindwurm", fakeService.lastJoinedIconId)
        }
    }

    @Test
    fun testCreateNewGame_withValidName() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
            putExtra("isNewGame", true)
        }

        ActivityScenario.launch<JoinActivity>(intent).use {
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("ScreenTitle").assertTextEquals("CREATE GAME")

            composeTestRule.onNodeWithTag("PlayerNameInput").performTextInput("Bob")
            composeTestRule.onNodeWithTag("ActionButton").performClick()

            assertEquals(1, fakeService.createGameCalls)
            assertEquals("Bob", fakeService.lastCreatedPlayerName)
            assertEquals("lindwurm", fakeService.lastCreatedIconId)
        }
    }

    @Test
    fun testEmptyPlayerName_defaultsToPlayer_andStartsGameboardUI() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
            putExtra("isNewGame", true)
        }

        ActivityScenario.launch<JoinActivity>(intent).use { scenario ->
            composeTestRule.waitForIdle()

            // Intentionally no input -> fallback logic `ifBlank` applies
            composeTestRule.onNodeWithTag("ActionButton").performClick()

            assertEquals(1, fakeService.createGameCalls)
            assertEquals("Player", fakeService.lastCreatedPlayerName)

            runBlocking {
                fakeService.emitTestEvent("""{"event":"GAME_CREATED","gameId":"created-123"}""")
            }
            composeTestRule.waitForIdle()

            // Verify that the activity is finished and GameboardUI was started
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                val expectedIntent = shadowActivity.nextStartedActivity

                assertEquals(GameboardUI::class.java.name, expectedIntent?.component?.className)
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test
    fun testBackButton_finishesActivity() {
        ActivityScenario.launch(JoinActivity::class.java).use { scenario ->
            composeTestRule.waitForIdle()

            // Find back button by text and trigger it
            composeTestRule.onNodeWithText("Back").performClick()

            // Verify that Activity.finish() was called
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test
    fun testIconCycling_andMissingGameIdFallback() {
        // Intent without "gameId" -> tests fallback `getStringExtra("gameId") ?: ""`
        val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
            putExtra("isNewGame", false)
        }

        ActivityScenario.launch<JoinActivity>(intent).use {
            composeTestRule.waitForIdle()

            // Cycle through the icons (3 clicks)
            val iconButton = composeTestRule.onNodeWithContentDescription("Selected Icon")
            iconButton.performClick() // Index 1: woerthersee
            iconButton.performClick() // Index 2: gti
            iconButton.performClick() // Index 3: ironman

            composeTestRule.onNodeWithTag("ActionButton").performClick()

            assertEquals(1, fakeService.joinGameCalls)
            assertEquals("", fakeService.lastJoinedGameId)

            // Checks if the correct icon is mapped from the `when`-block
            assertEquals("ironman", fakeService.lastJoinedIconId)
        }
    }

    @Test
    fun testAllIconMappings_coverage() {
        val expectedIcons = listOf("lindwurm", "woerthersee", "gti", "ironman", "josef")

        expectedIcons.forEachIndexed { index, expectedIconId ->
            val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
                putExtra("isNewGame", true)
            }

            ActivityScenario.launch<JoinActivity>(intent).use {
                composeTestRule.waitForIdle()

                // Click according to the current index (0 to 4 times) on the icon
                val iconButton = composeTestRule.onNodeWithContentDescription("Selected Icon")
                for (click in 0 until index) {
                    iconButton.performClick()
                }

                // Click button to trigger the onJoin lambda
                composeTestRule.onNodeWithTag("ActionButton").performClick()

                // Verify that the correct ID was passed to the service
                assertEquals(expectedIconId, fakeService.lastCreatedIconId)
            }
        }
    }

    @Test
    fun testIconMapping_elseBranch_coverage() {
        // Explicitly tests the unreachable else-branch for SonarQube.
        // Assumes that JoinActivity.mapIndexToIconId exists.
        val fallbackIcon = JoinActivity.mapIndexToIconId(99)
        assertEquals("lindwurm", fallbackIcon)
    }
}