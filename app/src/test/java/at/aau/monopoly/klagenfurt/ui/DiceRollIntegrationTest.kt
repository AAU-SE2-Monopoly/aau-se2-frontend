package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.ServiceLocator
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DiceRollIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeService: FakeGameService
    private lateinit var shakeEvents: MutableSharedFlow<Unit>
    private val objectMapper = JacksonProvider.objectMapper

    @Before
    fun setUp() {
        fakeService = FakeGameService()
        fakeService.setGameId("test-game")
        shakeEvents = MutableSharedFlow(extraBufferCapacity = 1)
        ServiceLocator.injectGameServiceForTest(fakeService)
    }

    @After
    fun tearDown() {
        ServiceLocator.resetForTests()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun emitRollingPhase(playerId: String, playerName: String = "Player") {
        val state = GameState(
            gameId = "test-game",
            fields = listOf(GoField()),
            players = mutableListOf(Player(id = playerId, name = playerName)),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING
        )
        val event = GameEvent(
            gameId = "test-game",
            event = "STATE_SNAPSHOT",
            gameState = state
        )
        runBlocking { fakeService.emitTestEvent(objectMapper.writeValueAsString(event)) }
        composeTestRule.waitForIdle()
    }

    private fun emitDiceResult(playerId: String, die1: Int, die2: Int) {
        val state = GameState(
            gameId = "test-game",
            fields = listOf(GoField()),
            players = mutableListOf(Player(id = playerId, name = "Player")),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            lastDiceRoll = DiceRoll(die1 = die1, die2 = die2)
        )
        val event = GameEvent(
            gameId = "test-game",
            event = "DICE_ROLLED",
            gameState = state,
            message = "Player rolled $die1 + $die2 = ${die1 + die2}"
        )
        runBlocking { fakeService.emitTestEvent(objectMapper.writeValueAsString(event)) }
        composeTestRule.waitForIdle()
    }

    private fun emitGameStarted(playerId: String, playerName: String = "Player") {
        val state = GameState(
            gameId = "test-game",
            fields = listOf(GoField()),
            players = mutableListOf(Player(id = playerId, name = playerName)),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING
        )
        val event = GameEvent(
            gameId = "test-game",
            event = "GAME_STARTED",
            gameState = state,
            message = "$playerName started the game"
        )
        runBlocking { fakeService.emitTestEvent(objectMapper.writeValueAsString(event)) }
        composeTestRule.waitForIdle()
    }

    /**
     * Injects a shake event into the shared flow and lets the main looper
     * process the resulting state changes.
     * Uses tryEmit (non-suspending) so we never block the main thread.
     */
    private fun simulateShake() {
        shakeEvents.tryEmit(Unit)
        composeTestRule.waitForIdle()
    }

    /**
     * Advances both the Robolectric system/looper clock (which drives coroutine
     * [delay] on Dispatchers.Main) and the Compose frame clock (which drives
     * animations), then waits for idle so all pending recompositions settle.
     */
    private fun waitForOverlayMinDuration() {
        org.robolectric.shadows.ShadowLooper.idleMainLooper(
            2_000L, java.util.concurrent.TimeUnit.MILLISECONDS
        )
        composeTestRule.mainClock.advanceTimeBy(2_000L)
        composeTestRule.waitForIdle()
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    fun hostStartsGame_overlayDoesNotOpen() {
        setContentWithViewModel()
        emitGameStarted(fakeService.currentPlayerId, "Host")

        // Roll Dice button should be visible (host is in ROLLING phase)
        composeTestRule.onNodeWithTag("roll_dice_button").assertIsDisplayed()
        // Overlay should NOT be open just from game starting
        composeTestRule.onNodeWithTag("dice_roll_overlay").assertIsNotDisplayed()
        // No roll should have happened
        assertFalse("rollDice must not be called on game start", fakeService.rollDiceCalled)
    }

    @Test
    fun clickRollDiceButton_opensOverlay_butDoesNotCallRollDice() {
        setContentWithViewModel()
        emitGameStarted(fakeService.currentPlayerId, "Host")

        // Click roll dice
        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()

        // Overlay opens
        composeTestRule.onNodeWithTag("dice_roll_overlay").assertIsDisplayed()
        // Idle prompt asking the user to shake
        composeTestRule.onNodeWithTag("dice_instruction_text")
            .assertTextContains("Shake", substring = true, ignoreCase = true)
        // Result total is NOT visible
        composeTestRule.onNodeWithTag("dice_total_text").assertIsNotDisplayed()
        // Critically: rollDice() must NOT be invoked simply because the button was clicked
        assertFalse("rollDice must NOT be called from button click alone", fakeService.rollDiceCalled)
    }

    @Test
    fun shakeAfterOpeningOverlay_triggersRollDiceOnService() {
        setContentWithViewModel()
        emitGameStarted(fakeService.currentPlayerId, "Host")

        // Click button to open overlay
        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()
        assertFalse(fakeService.rollDiceCalled)

        // Now simulate the shake gesture
        simulateShake()

        // rollDice() should now be invoked exactly once
        assertTrue("rollDice must be called after shake", fakeService.rollDiceCalled)
    }

    @Test
    fun multipleShakes_doNotTriggerRollMultipleTimes() {
        setContentWithViewModel()
        emitGameStarted(fakeService.currentPlayerId, "Host")

        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()

        simulateShake()
        assertTrue(fakeService.rollDiceCalled)

        // Reset flag and shake again – should NOT re-trigger because hasShaken guard is set
        fakeService.rollDiceCalled = false
        simulateShake()
        simulateShake()
        assertFalse("Subsequent shakes must not trigger another roll in the same turn",
            fakeService.rollDiceCalled)
    }

    @Test
    fun shakeWithoutOverlayOpen_doesNotTriggerRoll() {
        setContentWithViewModel()
        emitGameStarted(fakeService.currentPlayerId, "Host")

        // Shake before opening overlay
        simulateShake()

        assertFalse("Shake must be ignored while overlay is closed", fakeService.rollDiceCalled)
    }

    @Test
    fun shakeFinishing_showsResultAfterMinDuration() {
        setContentWithViewModel()
        emitRollingPhase(fakeService.currentPlayerId, "Host")

        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()
        // Shake to trigger the roll
        simulateShake()

        // Server responds with dice result
        emitDiceResult(fakeService.currentPlayerId, die1 = 3, die2 = 4)

        // Wait for min overlay duration
        waitForOverlayMinDuration()

        // Result "Total: 7" is now visible
        composeTestRule.onNodeWithTag("dice_total_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dice_total_text").assertTextContains("Total: 7", substring = true, ignoreCase = true)
    }

    @Test
    fun chatLog_hidesDiceRolled_whileOverlayVisible_showsAfterOverlayCloses() {
        setContentWithViewModel()
        emitGameStarted(fakeService.currentPlayerId, "Host")

        // Collapsed chat bar should show the game-started message
        composeTestRule.onNodeWithText("started", substring = true, ignoreCase = true).assertIsDisplayed()

        // Click roll dice
        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()
        simulateShake()

        // Server sends dice result while overlay is visible
        emitDiceResult(fakeService.currentPlayerId, die1 = 5, die2 = 2)
        waitForOverlayMinDuration()

        // While overlay still open, chat should NOT show "rolled"
        // (it should still show "started" because DICE_ROLLED entries are filtered)
        composeTestRule.onNodeWithText("started", substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("rolled", substring = true, ignoreCase = true).assertDoesNotExist()

        // Close the overlay
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        // Now chat bar should show the dice rolled message
        composeTestRule.onNodeWithText("rolled", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun nextPlayerTurn_resetsAndDoesNotShowPreviousResult() {
        setContentWithViewModel()
        emitRollingPhase(fakeService.currentPlayerId, "Host")

        // Host rolls and sees result
        composeTestRule.onNodeWithTag("roll_dice_button").performClick()
        composeTestRule.waitForIdle()
        simulateShake()
        emitDiceResult(fakeService.currentPlayerId, die1 = 1, die2 = 2)
        waitForOverlayMinDuration()

        composeTestRule.onNodeWithTag("dice_total_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dice_total_text").assertTextContains("Total: 3", substring = true, ignoreCase = true)

        // Close overlay
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.waitForIdle()

        // Simulate next player's turn (different player in ROLLING)
        val nextPlayerId = "other-player-1"
        val nextState = GameState(
            gameId = "test-game",
            fields = listOf(GoField()),
            players = mutableListOf(
                Player(id = fakeService.currentPlayerId, name = "Host"),
                Player(id = nextPlayerId, name = "Guest")
            ),
            currentPlayerIndex = 1,
            phase = GamePhase.ROLLING
        )
        runBlocking {
            fakeService.emitTestEvent(objectMapper.writeValueAsString(
                GameEvent(gameId = "test-game", event = "STATE_SNAPSHOT", gameState = nextState)
            ))
        }
        composeTestRule.waitForIdle()

        // Overlay should be closed (it's not host's turn)
        composeTestRule.onNodeWithTag("dice_roll_overlay").assertIsNotDisplayed()
        // Previous result should not linger
        composeTestRule.onNodeWithTag("dice_total_text").assertIsNotDisplayed()
    }

    // ---------------------------------------------------------------------------

    private fun setContentWithViewModel() {
        val viewModel = GameViewModel(fakeService)
        composeTestRule.setContent {
            GameboardScreen(viewModel = viewModel, shakeEventsOverride = shakeEvents)
        }
    }
}