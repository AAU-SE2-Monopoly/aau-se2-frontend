package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelDoubleAutoEndTest {

    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: GameViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeGameService()
        fakeService.currentPlayerId = "p1"
        fakeService.currentGameId = "game1"
        viewModel = GameViewModel(fakeService, currentTimeProvider = { 5000L })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildDiceRolledJson(isDouble: Boolean, currentPlayerId: String = "p1"): String {
        val diceRoll = if (isDouble) DiceRoll(3, 3) else DiceRoll(2, 5)
        val gameState = GameState(
            gameId = "game1",
            fields = emptyList(),
            players = mutableListOf(Player(id = currentPlayerId, name = "Alice")),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING,
            lastDiceRoll = diceRoll
        )
        val event = GameEvent(
            gameId = "game1",
            event = "DICE_ROLLED",
            gameState = gameState
        )
        return JacksonProvider.objectMapper.writeValueAsString(event)
    }

    @Test
    fun `pendingDoubleAutoEnd is initially false`() {
        assertFalse(viewModel.pendingDoubleAutoEnd.value)
    }

    @Test
    fun `DICE_ROLLED with double sets pendingDoubleAutoEnd to true`() = runTest(testDispatcher) {
        fakeService.emitTestEvent(buildDiceRolledJson(isDouble = true))
        advanceUntilIdle()
        assertTrue(viewModel.pendingDoubleAutoEnd.value)
    }

    @Test
    fun `DICE_ROLLED without double does not set pendingDoubleAutoEnd`() = runTest(testDispatcher) {
        fakeService.emitTestEvent(buildDiceRolledJson(isDouble = false))
        advanceUntilIdle()
        assertFalse(viewModel.pendingDoubleAutoEnd.value)
    }

    @Test
    fun `DICE_ROLLED with double but different player does not set pendingDoubleAutoEnd`() = runTest(testDispatcher) {
        fakeService.emitTestEvent(buildDiceRolledJson(isDouble = true, currentPlayerId = "p2"))
        advanceUntilIdle()
        assertFalse(viewModel.pendingDoubleAutoEnd.value)
    }

    @Test
    fun `DICE_ROLLED with null gameState does not set pendingDoubleAutoEnd`() = runTest(testDispatcher) {
        val event = GameEvent(gameId = "game1", event = "DICE_ROLLED", gameState = null)
        val json = JacksonProvider.objectMapper.writeValueAsString(event)
        fakeService.emitTestEvent(json)
        advanceUntilIdle()
        assertFalse(viewModel.pendingDoubleAutoEnd.value)
    }

    @Test
    fun `DICE_ROLLED with null lastDiceRoll does not set pendingDoubleAutoEnd`() = runTest(testDispatcher) {
        val gameState = GameState(
            gameId = "game1",
            fields = emptyList(),
            players = mutableListOf(Player(id = "p1", name = "Alice")),
            currentPlayerIndex = 0,
            lastDiceRoll = null
        )
        val event = GameEvent(gameId = "game1", event = "DICE_ROLLED", gameState = gameState)
        val json = JacksonProvider.objectMapper.writeValueAsString(event)
        fakeService.emitTestEvent(json)
        advanceUntilIdle()
        assertFalse(viewModel.pendingDoubleAutoEnd.value)
    }

    @Test
    fun `consumeDoubleAutoEnd calls endTurn and resets flag`() = runTest(testDispatcher) {
        // First trigger the double
        fakeService.emitTestEvent(buildDiceRolledJson(isDouble = true))
        advanceUntilIdle()
        assertTrue(viewModel.pendingDoubleAutoEnd.value)

        // Consume it
        viewModel.consumeDoubleAutoEnd()
        advanceUntilIdle()

        assertFalse(viewModel.pendingDoubleAutoEnd.value)
        assertTrue(fakeService.endTurnCalled)
    }

    @Test
    fun `consumeDoubleAutoEnd does nothing when flag is false`() = runTest(testDispatcher) {
        assertFalse(viewModel.pendingDoubleAutoEnd.value)
        viewModel.consumeDoubleAutoEnd()
        advanceUntilIdle()
        assertFalse(fakeService.endTurnCalled)
    }
}

