package at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketbrokerdemo.networking.GameService
import at.aau.serg.websocketbrokerdemo.ui.GameViewModel
import org.junit.jupiter.api.BeforeEach
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue

class GameViewModelTest {
    private lateinit var viewModel: GameViewModel
    private lateinit var gameService: GameService
    private lateinit var mockEvents: MutableSharedFlow<String>
    private lateinit var mockStatus: MutableSharedFlow<String>

    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        gameService = mockk<GameService>(relaxed = true)
        mockEvents = MutableSharedFlow()
        mockStatus = MutableSharedFlow()

        every { gameService.events } returns mockEvents
        every { gameService.status } returns mockStatus

        viewModel = GameViewModel(gameService)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {

        Dispatchers.resetMain()
    }

    @Test
    fun test_eventsSharedFlow_is_returned_by_gameService() {
        assertEquals(mockEvents, viewModel.events)
        assertEquals(mockStatus, viewModel.status)
    }

    @Test
    fun test_connect_Calls_service_connect() {
        viewModel.connect()
        verify { gameService.connect() }
    }

    @Test
    fun test_createGame_calls_service_createGame() {
        val name = "Player1"
        viewModel.createGame(name)
        verify { gameService.createGame(name) }
    }
    @Test
    fun test_startGame_calls_service_startGame() {
        viewModel.startGame()
        verify { gameService.startGame() }
    }


    @Test
    fun test_joinGame_calls_service_joinGame() {
        viewModel.joinGame("room123", "Player1")
        verify { gameService.joinGame("room123", "Player1") }
    }

    @Test
    fun test_rollDice_calls_service_rollDice() {
        viewModel.rollDice()
        verify { gameService.rollDice() }
    }

    @Test
    fun test_endTurn_calls_service_endTurn() {
        viewModel.endTurn()
        verify { gameService.endTurn() }
    }

    @Test
    fun test_requestState_calls_service_requestState() {
        viewModel.requestState()
        verify { gameService.requestState() }
    }

    @Test
    fun test_setGameId_calls_service_setGameId() {
        viewModel.setGameId("game123")
        verify { gameService.setGameId("game123") }
    }

    @Test
    fun test_Factory_creates_GameViewModel() {
        val factory = GameViewModel.Factory(gameService)
        val createdViewModel = factory.create(GameViewModel::class.java)
        assertEquals(viewModel.events, createdViewModel.events)
        assertTrue(createdViewModel is GameViewModel)
    }
}
