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

            // Prüft, ob der Titel korrekt ist
            composeTestRule.onNode(hasTestTag("ScreenTitle") and hasText("JOIN GAME")).assertExists()

            // Prüft, ob die Game-ID korrekt abgeschnitten wird (take(8))
            composeTestRule.onNodeWithText("Game: test-gam…").assertIsDisplayed()

            // Eingabe und Klick
            composeTestRule.onNodeWithTag("PlayerNameInput").performTextInput("Alice")
            composeTestRule.onNodeWithTag("ActionButton").performClick()

            // Service Aufrufe verifizieren
            assertEquals(1, fakeService.joinGameCalls)
            assertEquals("Alice", fakeService.lastJoinedPlayerName)
            assertEquals("test-game-123456", fakeService.lastJoinedGameId)

            // Icon Index 0 ist Default = lindwurm
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

            // Absichtlich keine Eingabe tätigen -> Fallback Logik `ifBlank` greift
            composeTestRule.onNodeWithTag("ActionButton").performClick()

            assertEquals(1, fakeService.createGameCalls)
            assertEquals("Player", fakeService.lastCreatedPlayerName)

            // Verifizieren, dass die Activity beendet und GameboardUI gestartet wurde
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

            // Back-Button über den Text finden und auslösen
            composeTestRule.onNodeWithText("Back").performClick()

            // Verifizieren, dass Activity.finish() aufgerufen wurde
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test
    fun testIconCycling_andMissingGameIdFallback() {
        // Intent ohne "gameId" -> testet Fallback `getStringExtra("gameId") ?: ""`
        val intent = Intent(ApplicationProvider.getApplicationContext(), JoinActivity::class.java).apply {
            putExtra("isNewGame", false)
        }

        ActivityScenario.launch<JoinActivity>(intent).use {
            composeTestRule.waitForIdle()

            // Durchlaufen der Icons (3 Klicks)
            val iconButton = composeTestRule.onNodeWithContentDescription("Selected Icon")
            iconButton.performClick() // Index 1: woerthersee
            iconButton.performClick() // Index 2: gti
            iconButton.performClick() // Index 3: ironman

            composeTestRule.onNodeWithTag("ActionButton").performClick()

            assertEquals(1, fakeService.joinGameCalls)
            assertEquals("", fakeService.lastJoinedGameId)

            // Prüft, ob das korrekte Icon aus dem `when`-Block gemappt wurde
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

                // Klicke entsprechend dem aktuellen Index (0 bis 4 mal) auf das Icon
                val iconButton = composeTestRule.onNodeWithContentDescription("Selected Icon")
                for (click in 0 until index) {
                    iconButton.performClick()
                }

                // Button klicken, um das onJoin Lambda auszulösen
                composeTestRule.onNodeWithTag("ActionButton").performClick()

                // Verifizieren, ob die richtige ID an den Service übergeben wurde
                assertEquals(expectedIconId, fakeService.lastCreatedIconId)
            }
        }
    }

    @Test
    fun testIconMapping_elseBranch_coverage() {
        // Testet explizit den unerreichbaren else-Zweig für SonarQube.
        // Setzt voraus, dass JoinActivity.mapIndexToIconId existiert.
        val fallbackIcon = JoinActivity.mapIndexToIconId(99)
        assertEquals("lindwurm", fallbackIcon)
    }
}