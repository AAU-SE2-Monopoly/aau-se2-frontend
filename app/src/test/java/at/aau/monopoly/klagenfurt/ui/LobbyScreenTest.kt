package at.aau.monopoly.klagenfurt.ui


import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.LobbyScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LobbyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `reconnect button reconnects and shows connected status without leaving screen`() {
        // ...existing test body...
        val fakeService = FakeGameService()
        fakeService.ignoreFirstConnectCall = true
        val viewModel = LobbyViewModel(fakeService)

        // Initial failed reconnect state
        fakeService.setConnectionState(false)
        fakeService.setReconnectFailed(true)
        fakeService.connectCalled = false

        composeTestRule.setContent {
            LobbyScreen(
                viewModel = viewModel,
                onBackClicked = {},
                onGameClicked = {},
                onCreateGame = {}
            )
        }

        shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()

        val connectCallsBefore = fakeService.connectCalls

        composeTestRule.onNodeWithText("Reconnect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reconnect").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Reconnect should call connect()", fakeService.connectCalls == connectCallsBefore + 1)

        // Simulate successful connection after reconnect
        fakeService.setLobbySubscriptionReady(true)
        fakeService.setReconnectFailed(false)
        fakeService.setConnectionState(true)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Connecting…").assertCountEquals(0)

        composeTestRule.runOnIdle {
            assertTrue("subscribeToLobby should be called", fakeService.subscribeToLobbyCalled)
            assertTrue("requestGameList should be called", fakeService.requestGameListCalled)
        }
    }

    @Test
    fun `lobby shows loading spinner and looking for games text when not connected`() {
        val fakeService = FakeGameService()
        fakeService.ignoreFirstConnectCall = true
        val viewModel = LobbyViewModel(fakeService)

        // Simulate not connected, not failed (initial connecting state)
        fakeService.setConnectionState(false)
        fakeService.setReconnectFailed(false)

        composeTestRule.setContent {
            LobbyScreen(
                viewModel = viewModel,
                onBackClicked = {},
                onGameClicked = {},
                onCreateGame = {}
            )
        }

        shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()

        // Should show the "Looking for games…" text (new code)
        composeTestRule.onNodeWithText("Looking for games…").assertIsDisplayed()
    }

    @Test
    fun `lobby shows create game card when connected`() {
        val fakeService = FakeGameService()
        fakeService.ignoreFirstConnectCall = true
        val viewModel = LobbyViewModel(fakeService)

        // Simulate connected state
        fakeService.setConnectionState(true)
        fakeService.setLobbySubscriptionReady(true)
        fakeService.setReconnectFailed(false)

        composeTestRule.setContent {
            LobbyScreen(
                viewModel = viewModel,
                onBackClicked = {},
                onGameClicked = {},
                onCreateGame = {}
            )
        }

        shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()

        // When connected, the LazyRow with CreateGameCard should be displayed
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        // "Looking for games…" should NOT be shown when connected
        composeTestRule.onAllNodesWithText("Looking for games…").assertCountEquals(0)
    }
}
