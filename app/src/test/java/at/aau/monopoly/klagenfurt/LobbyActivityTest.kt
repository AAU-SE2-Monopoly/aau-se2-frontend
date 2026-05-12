package at.aau.monopoly.klagenfurt

import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LobbyActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var fakeService: FakeGameService

    @Before
    fun setup() {
        fakeService = FakeGameService()
        fakeService.setConnectionState(true)
        ServiceLocator.injectGameServiceForTest(fakeService)
    }

    @After
    fun tearDown() {
        ServiceLocator.resetForTests()
    }

    @Test
    fun `create game card starts JoinActivity with new game flag`() {
        ActivityScenario.launch<LobbyActivity>(Intent(ApplicationProvider.getApplicationContext(), LobbyActivity::class.java)).use { scenario ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("NEW GAME").performClick()
            composeTestRule.waitForIdle()

            scenario.onActivity { activity ->
                val startedIntent = shadowOf(activity).nextStartedActivity
                assertNotNull(startedIntent)
                assertEquals(JoinActivity::class.java.name, startedIntent?.component?.className)
                assertTrue(startedIntent?.getBooleanExtra("isNewGame", false) == true)
            }
        }
    }

    @Test
    fun `game card click opens JoinActivity with JOIN_STATUS and game extras`() {
        ActivityScenario.launch<LobbyActivity>(Intent(ApplicationProvider.getApplicationContext(), LobbyActivity::class.java)).use { scenario ->
            val lobbyJson = """
            {
              "event": "LOBBY_UPDATE",
              "games": [
                {
                  "gameId": "game-1",
                  "hostPlayerName": "Alice",
                  "hostPlayerId": "p1",
                  "playerCount": 2,
                  "maxPlayers": 4,
                  "phase": "WAITING",
                  "playerIds": ["p1", "p2"]
                }
              ]
            }
            """.trimIndent()

            runBlocking { fakeService.emitTestLobbyEvent(lobbyJson) }
            shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Alice").performClick()
            composeTestRule.waitForIdle()

            scenario.onActivity { activity ->
                val startedIntent = shadowOf(activity).nextStartedActivity
                assertNotNull(startedIntent)
                assertEquals(JoinActivity::class.java.name, startedIntent?.component?.className)
                assertEquals("game-1", startedIntent?.getStringExtra("gameId"))
                assertTrue(startedIntent?.getBooleanExtra("isNewGame", true) == false)
                assertEquals("OPEN", startedIntent?.getStringExtra("JOIN_STATUS"))
            }
        }
    }
}

