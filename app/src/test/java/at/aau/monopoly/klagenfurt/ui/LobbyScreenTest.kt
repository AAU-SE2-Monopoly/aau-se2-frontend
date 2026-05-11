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
}
