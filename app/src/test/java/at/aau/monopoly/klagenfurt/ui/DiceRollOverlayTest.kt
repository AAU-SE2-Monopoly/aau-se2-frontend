package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DiceRollOverlayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun diceOverlay_renders_whenCurrentPlayerRollingPhase() {
        val fakeService = FakeGameService()
        fakeService.setGameId("game-1")
        val viewModel = GameViewModel(fakeService)

        composeTestRule.setContent {
            DiceOverlayHost(viewModel)
        }

        val gameState = GameState(
            gameId = "game-1",
            fields = listOf(GoField()),
            players = mutableListOf(
                Player(id = fakeService.currentPlayerId, name = "Tester")
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING
        )

        val eventJson = JacksonProvider.objectMapper.writeValueAsString(
            GameEvent(
                gameId = "game-1",
                event = "STATE_SNAPSHOT",
                gameState = gameState
            )
        )

        runBlocking {
            fakeService.emitTestEvent(eventJson)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dice_roll_overlay").assertIsDisplayed()
    }

    @Test
    fun shakeButton_shown_whenNotShakenAndCallbackProvided() {
        composeTestRule.setContent {
            DiceRollOverlay(
                isVisible = true,
                isRolling = false,
                hasShaken = false,
                onShakeButton = {},
                onClose = {}
            )
        }
        composeTestRule.onNodeWithTag("shake_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shake").assertIsDisplayed()
    }

    @Test
    fun shakeButton_hidden_whenAlreadyShaken() {
        composeTestRule.setContent {
            DiceRollOverlay(
                isVisible = true,
                isRolling = true,
                hasShaken = true,
                onShakeButton = {},
                onClose = {}
            )
        }
        composeTestRule.onNodeWithTag("shake_button").assertDoesNotExist()
    }

    @Test
    fun shakeButton_hidden_whenCallbackNull() {
        composeTestRule.setContent {
            DiceRollOverlay(
                isVisible = true,
                isRolling = false,
                hasShaken = false,
                onShakeButton = null,
                onClose = {}
            )
        }
        composeTestRule.onNodeWithTag("shake_button").assertDoesNotExist()
    }

    @Test
    fun shakeButton_invokesCallback_onClick() {
        var clicked = false
        composeTestRule.setContent {
            DiceRollOverlay(
                isVisible = true,
                isRolling = false,
                hasShaken = false,
                onShakeButton = { clicked = true },
                onClose = {}
            )
        }
        composeTestRule.onNodeWithTag("shake_button").performClick()
        assertTrue(clicked)
    }

    @Test
    fun overlay_notShown_whenNotVisible() {
        composeTestRule.setContent {
            DiceRollOverlay(
                isVisible = false,
                onClose = {}
            )
        }
        composeTestRule.onNodeWithTag("dice_roll_overlay").assertDoesNotExist()
    }

    @Test
    fun overlay_showsInstructionText_whenVisible() {
        composeTestRule.setContent {
            DiceRollOverlay(
                isVisible = true,
                isRolling = false,
                hasShaken = false,
                onClose = {}
            )
        }
        composeTestRule.onNodeWithTag("dice_instruction_text").assertIsDisplayed()
    }
}

@Composable
private fun DiceOverlayHost(viewModel: GameViewModel) {
    val isVisible by viewModel.isRollingPhaseForCurrentPlayer.collectAsState()
    val lastDiceRoll by viewModel.lastDiceRoll.collectAsState()

    DiceRollOverlay(
        isVisible = isVisible,
        diceResult = lastDiceRoll?.let { it.die1 to it.die2 },
        isRolling = isVisible && lastDiceRoll == null,
        onClose = {}
    )
}
