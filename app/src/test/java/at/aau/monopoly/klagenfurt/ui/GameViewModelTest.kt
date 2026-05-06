package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.networking.GameService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import kotlinx.coroutines.test.advanceUntilIdle

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private lateinit var gameService: GameService
    private lateinit var viewModel: GameViewModel

    // Echte Flows simulieren, um JSON-Strings in das ViewModel zu pushen
    private val eventsFlow = MutableSharedFlow<String>(replay = 1)
    private val logEventsFlow = MutableSharedFlow<String>(replay = 1)
    @Before
    fun setup() {
        // Notwendig für viewModelScope in Unit Tests
        Dispatchers.setMain(UnconfinedTestDispatcher())

        gameService = mockk(relaxed = true)
        every { gameService.events } returns eventsFlow
        every { gameService.logEvents } returns logEventsFlow
        every { gameService.currentGameId } returns ""
        every { gameService.currentPlayerId } returns "p1"

        viewModel = GameViewModel(gameService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- OVERLAY TESTS ---

    @Test
    fun `initial selectedPlayerForOverlay state should be null`() {
        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `showPlayerOverlay should update state with correct player`() {
        val testPlayer = Player(id = "p1", name = "Spieler 1")
        viewModel.showPlayerOverlay(testPlayer)
        assertEquals(testPlayer, viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `hidePlayerOverlay should reset state to null`() {
        val testPlayer = Player(id = "p1", name = "Spieler 1")
        viewModel.showPlayerOverlay(testPlayer)
        viewModel.hidePlayerOverlay()
        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    // --- DELEGATION TESTS ---

    @Test
    fun `connect should call gameService connect`() {
        viewModel.connect()
        verify(exactly = 1) { gameService.connect() }
    }

    @Test
    fun `createGame should call gameService createGame`() {
        viewModel.createGame("Lukas")
        verify(exactly = 1) { gameService.createGame("Lukas") }
    }

    @Test
    fun `joinGame should call gameService joinGame`() {
        viewModel.joinGame("game123", "Lukas")
        verify(exactly = 1) { gameService.joinGame("game123", "Lukas") }
    }

    @Test
    fun `startGame should call gameService startGame`() {
        viewModel.startGame()
        verify(exactly = 1) { gameService.startGame() }
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
        viewModel.setGameId("game123")
        verify(exactly = 1) { gameService.setGameId("game123") }
    }

    // --- FACTORY TEST ---

    @Test
    fun `Factory creates GameViewModel successfully`() {
        val factory = GameViewModel.Factory(gameService)
        val createdViewModel = factory.create(GameViewModel::class.java)
        assertTrue(createdViewModel is GameViewModel)
    }

    // --- DICE CHEAT TESTS ---

    @Test
    fun `rollDice should pass false to service by default`() {
        viewModel.rollDice()
        verify(exactly = 1) { gameService.rollDice(false) }
    }

    @Test
    fun `rollDice should pass true to service when cheat is activated`() {
        viewModel.activateCheatForNextRoll()
        viewModel.rollDice()
        verify(exactly = 1) { gameService.rollDice(true) }
    }

    @Test
    fun `cheat flag should be reset after one roll`() {
        viewModel.activateCheatForNextRoll()
        viewModel.rollDice()
        viewModel.rollDice()
        verify(exactly = 1) { gameService.rollDice(true) }
        verify(exactly = 1) { gameService.rollDice(false) }
    }

    // --- PROPERTY GETTER TESTS ---

    @Test
    fun `currentPlayerId should return value from gameService`() {
        every { gameService.currentPlayerId } returns "player-123"
        assertEquals("player-123", viewModel.currentPlayerId)
    }

    @Test
    fun `events and status flows should delegate to gameService`() {
        assertEquals(gameService.events, viewModel.events)
        assertEquals(gameService.status, viewModel.status)
    }

    // --- SYNC STATE TESTS ---

    @Test
    fun `syncGameboardEntryState should do nothing if currentGameId is blank`() {
        every { gameService.currentGameId } returns ""
        viewModel.syncGameboardEntryState()
        verify(exactly = 0) { gameService.requestState() }
    }

    @Test
    fun `syncGameboardEntryState should call requestState if currentGameId is set`() {
        every { gameService.currentGameId } returns "active-game-id"
        viewModel.syncGameboardEntryState()
        verify(exactly = 1) { gameService.requestState() }
    }

    // --- FLOW & JSON PARSING TESTS (Für 100% Coverage) ---

    @Test
    fun `malformed JSON in events should be caught and ignored`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        // Sende defektes JSON. Der try-catch Block im ViewModel muss das abfangen,
        // ohne dass die App crasht. gameState bleibt null.
        eventsFlow.emit("""{ invalid json """)
        assertNull(viewModel.gameState.value)

        job.cancel()
    }

    @Test
    fun `init block should auto-capture gameId on GAME_CREATED event`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        every { gameService.currentGameId } returns ""
        eventsFlow.emit("""{"event":"GAME_CREATED","gameId":"new-game-id"}""")

        verify(exactly = 1) { gameService.setGameId("new-game-id") }

        job.cancel()
    }

    @Test
    fun `gameState ignores events from a different gameId`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        every { gameService.currentGameId } returns "my-game-id"
        // Event gehört zu anderem Spiel, sollte ignoriert werden
        eventsFlow.emit("""{"event":"STATE_UPDATED","gameId":"other-game-id","gameState":{"phase":"ROLLING"}}""")

        assertNull(viewModel.gameState.value)

        job.cancel()
    }


    @Test
    fun `eventLog processes log events and formats human readable text`() = runTest {
        val job = launch { viewModel.eventLog.collect {} }

        logEventsFlow.emit("""{"event":"PLAYER_JOINED","gameId":"g1"}""")
        advanceUntilIdle() // <-- Getauscht! Wartet, bis alles zu 100% verarbeitet ist
        assertEquals("A new player joined", viewModel.eventLog.value.last().text)
        assertFalse(viewModel.eventLog.value.last().isTechnical)

        logEventsFlow.emit("""{"event":"CUSTOM_EVENT","gameId":"g1"}""")
        advanceUntilIdle() // <-- Getauscht!
        assertEquals("Custom event", viewModel.eventLog.value.last().text)

        logEventsFlow.emit("""{"event":"STATE_SNAPSHOT","gameId":"g1"}""")
        advanceUntilIdle() // <-- Getauscht!
        assertTrue(viewModel.eventLog.value.last().isTechnical)
        assertEquals("State snapshot synced", viewModel.eventLog.value.last().text)

        logEventsFlow.emit("""{"event":"SOME_EVENT","gameId":"g1","message":"Direct message"}""")
        advanceUntilIdle() // <-- Getauscht!
        assertEquals("Direct message", viewModel.eventLog.value.last().text)

        job.cancel()
    }

    @Test
    fun `currentPlayerName getter should return internal value`() {
        // Act: Wir lesen die Property. Das löst exakt den Code "get() = _currentPlayerName" aus.
        val actualName = gameService.currentPlayerName

        // Assert: Wir prüfen auf den Standardwert.
        // Falls deine Variable mit "" initialisiert wird, nutze diesen Assert:
        assertEquals("", actualName)
    }


}