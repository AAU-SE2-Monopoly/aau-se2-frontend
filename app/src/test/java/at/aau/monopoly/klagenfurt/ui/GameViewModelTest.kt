package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.networking.GameService
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

    private lateinit var gameService: GameService
    private lateinit var viewModel: GameViewModel
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gameService = mockk(relaxed = true)
        viewModel = GameViewModel(gameService)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial selectedPlayerForOverlay state should be null`() {
        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `showPlayerOverlay should update state with correct player`() {
        val testPlayer = Player(id = "p1", name = "Spieler 1")

        viewModel.showPlayerOverlay(testPlayer)

        assertEquals(testPlayer, viewModel.selectedPlayerForOverlay.value)
        assertEquals("Spieler 1", viewModel.selectedPlayerForOverlay.value?.name)
    }

    @Test
    fun `hidePlayerOverlay should reset state to null`() {
        val testPlayer = Player(id = "p1", name = "Spieler 1")
        viewModel.showPlayerOverlay(testPlayer)

        viewModel.hidePlayerOverlay()

        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `connect should call gameService connect`() {
        viewModel.connect()
        verify(exactly = 1) { gameService.connect() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `createGame should call gameService createGame`() = runTest(testDispatcher) {
        val playerName = "Lukas"
        viewModel.createGame(playerName)
        advanceUntilIdle()
        coVerify(exactly = 1) { gameService.createGame(playerName) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `joinGame should call gameService joinGame`() = runTest(testDispatcher) {
        val gameId = "game123"
        val playerName = "Lukas"
        viewModel.joinGame(gameId, playerName)
        advanceUntilIdle()
        coVerify(exactly = 1) { gameService.joinGame(gameId, playerName) }
    }

    @Test
    fun `startGame should call gameService startGame`() {
        viewModel.startGame()
        verify(exactly = 1) { gameService.startGame() }
    }

    @Test
    fun `rollDice should call gameService rollDice`() {
        viewModel.rollDice()
        verify(exactly = 1) { gameService.rollDice() }
    }

    @Test
    fun `endTurn should call gameService endTurn`() {
        viewModel.endTurn()
        verify(exactly = 1) { gameService.endTurn() }
    }

    @Test
    fun `requestState should call gameService requestState`() {
        viewModel.requestState()
        verify(exactly = 1) { gameService.requestState() }
    }

    @Test
    fun `setGameId should call gameService setGameId`() {
        val gameId = "game123"
        viewModel.setGameId(gameId)
        verify(exactly = 1) { gameService.setGameId(gameId) }
    }

    // --- FACTORY TEST ---

    @Test
    fun `Factory creates GameViewModel successfully`() {
        val factory = GameViewModel.Factory(gameService)
        val createdViewModel = factory.create(GameViewModel::class.java)

        assertTrue(createdViewModel is GameViewModel)
    }
}