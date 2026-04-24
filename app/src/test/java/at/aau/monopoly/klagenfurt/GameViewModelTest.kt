package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import at.aau.monopoly.klagenfurt.ui.GameViewModel
import org.junit.jupiter.api.BeforeEach
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.GameState

class GameViewModelTest {
    private lateinit var viewModel: GameViewModel
    private lateinit var gameService: GameService
    private lateinit var mockEvents: MutableSharedFlow<String>
    private lateinit var mockStatus: MutableSharedFlow<String>

    private val testDispatcher = StandardTestDispatcher()
    private val objectMapper = JacksonProvider.objectMapper

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        gameService = mockk<GameService>(relaxed = true)
        mockEvents = MutableSharedFlow()
        mockStatus = MutableSharedFlow()

        every { gameService.events } returns mockEvents
        every { gameService.status } returns mockStatus
        every { gameService.currentGameId } returns ""

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun eventLog_resets_when_new_game_is_created() = runTest(testDispatcher) {
        val collector = launch { viewModel.eventLog.collect { } }
        advanceUntilIdle()

        mockEvents.emit(jsonEvent(GameEvent(gameId = "game-1", event = "PLAYER_JOINED", message = "Alice joined")))
        mockEvents.emit(jsonEvent(GameEvent(gameId = "game-1", event = "TURN_ENDED", message = "Turn done")))
        advanceUntilIdle()
        assertEquals(2, viewModel.eventLog.value.size)

        mockEvents.emit(jsonEvent(GameEvent(gameId = "game-2", event = "GAME_CREATED", message = "Game created: game-2")))
        advanceUntilIdle()

        val log = viewModel.eventLog.value
        assertEquals(1, log.size)
        assertEquals("Game created: game-2", log.last().text)

        collector.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun eventLog_ignores_events_from_other_game_when_current_game_is_set() = runTest(testDispatcher) {
        every { gameService.currentGameId } returns "game-1"
        viewModel = GameViewModel(gameService)
        val collector = launch { viewModel.eventLog.collect { } }
        advanceUntilIdle()

        mockEvents.emit(jsonEvent(GameEvent(gameId = "game-2", event = "PLAYER_JOINED", message = "Bob joined")))
        advanceUntilIdle()
        assertTrue(viewModel.eventLog.value.isEmpty())

        mockEvents.emit(jsonEvent(GameEvent(gameId = "game-1", event = "PLAYER_JOINED", message = "Alice joined")))
        advanceUntilIdle()
        assertEquals(1, viewModel.eventLog.value.size)
        assertEquals("Alice joined", viewModel.eventLog.value.last().text)

        collector.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun eventLog_keeps_only_last_80_entries() = runTest(testDispatcher) {
        val collector = launch { viewModel.eventLog.collect { } }
        advanceUntilIdle()

        for (i in 1..85) {
            mockEvents.emit(jsonEvent(GameEvent(gameId = "game-1", event = "TURN_ENDED", message = "Entry $i")))
        }
        advanceUntilIdle()

        val log = viewModel.eventLog.value
        assertEquals(80, log.size)
        assertEquals("Entry 6", log.first().text)
        assertEquals("Entry 85", log.last().text)

        collector.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun gameState_ignores_state_from_other_game_id() = runTest(testDispatcher) {
        every { gameService.currentGameId } returns "game-1"
        viewModel = GameViewModel(gameService)
        val collector = launch { viewModel.gameState.collect { } }
        advanceUntilIdle()

        val foreignState = GameState(gameId = "game-2", fields = emptyList())
        mockEvents.emit(jsonEvent(GameEvent(gameId = "game-2", event = "STATE_UPDATED", gameState = foreignState)))
        advanceUntilIdle()
        assertEquals(null, viewModel.gameState.value)

        val currentState = GameState(gameId = "game-1", fields = emptyList())
        mockEvents.emit(jsonEvent(GameEvent(gameId = "game-1", event = "STATE_UPDATED", gameState = currentState)))
        advanceUntilIdle()
        assertEquals(currentState, viewModel.gameState.value)

        collector.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun eventLog_maps_known_event_types_to_human_readable_text() = runTest(testDispatcher) {
        val collector = launch { viewModel.eventLog.collect { } }
        advanceUntilIdle()

        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "GAME_CREATED", message = null)))
        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "PLAYER_JOINED", message = null)))
        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "GAME_STARTED", message = null)))
        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "DICE_ROLLED", message = null)))
        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "TURN_ENDED", message = null)))
        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "STATE_UPDATED", message = null)))
        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "STATE_SNAPSHOT", message = null)))
        advanceUntilIdle()

        val texts = viewModel.eventLog.value.map { it.text }
        assertTrue(texts.contains("Game created: g1"))
        assertTrue(texts.contains("A new player joined"))
        assertTrue(texts.contains("Game started!"))
        assertTrue(texts.contains("Dice rolled"))
        assertTrue(texts.contains("Turn ended"))
        assertTrue(texts.contains("Game state updated"))
        assertTrue(texts.contains("State snapshot synced"))

        collector.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun eventLog_maps_unknown_event_type_to_title_case() = runTest(testDispatcher) {
        val collector = launch { viewModel.eventLog.collect { } }
        advanceUntilIdle()

        mockEvents.emit(jsonEvent(GameEvent(gameId = "g1", event = "PLAYER_WENT_BANKRUPT", message = null)))
        advanceUntilIdle()

        assertEquals("Player went bankrupt", viewModel.eventLog.value.last().text)

        collector.cancel()
    }

    private fun jsonEvent(event: GameEvent): String = objectMapper.writeValueAsString(event)
}
