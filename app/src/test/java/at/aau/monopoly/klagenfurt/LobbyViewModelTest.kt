package at.aau.monopoly.klagenfurt

import android.util.Log
import at.aau.monopoly.klagenfurt.ui.LobbyViewModel
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: LobbyViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeGameService()
        viewModel = LobbyViewModel(fakeService)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls connect on gameService`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(fakeService.connectCalled)
    }

    @Test
    fun `currentPlayerId delegates to gameService`() {
        assertEquals("test-player-id", viewModel.currentPlayerId)
    }

    @Test
    fun `isConnected is false initially`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(false, viewModel.isConnected.value)
    }

    @Test
    fun `isConnected becomes true when connectionState becomes true`() = runTest(testDispatcher) {
        val values = mutableListOf<Boolean>()
        val job = launch { viewModel.isConnected.collect { values.add(it) } }
        advanceUntilIdle()

        fakeService.setConnectionState(true)
        advanceUntilIdle()

        assertTrue(values.contains(true))
        job.cancel()
    }

    @Test
    fun `isConnected becomes false when connectionState becomes false`() = runTest(testDispatcher) {
        val job = launch { viewModel.isConnected.collect {} }
        advanceUntilIdle()

        fakeService.setConnectionState(false)
        advanceUntilIdle()
        assertEquals(false, viewModel.isConnected.value)
        job.cancel()
    }

    @Test
    fun `games is empty initially`() {
        assertEquals(emptyList<Any>(), viewModel.games.value)
    }

    @Test
    fun `games updates when lobby event is received`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "p1",
                    "playerCount": 1,
                    "maxPlayers": 4,
                    "phase": "WAITING"
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        val games = viewModel.games.value
        assertEquals(1, games.size)
        assertEquals("g1", games[0].gameId)
        assertEquals("Alice", games[0].hostPlayerName)
        assertEquals(1, games[0].playerCount)
    }

    @Test
    fun `games updates to empty when empty lobby event received`() = runTest(testDispatcher) {
        // First populate
        val lobbyJson1 = """{"event":"LOBBY_UPDATE","games":[{"gameId":"g1","hostPlayerName":"A","hostPlayerId":"p","playerCount":1,"maxPlayers":4,"phase":"WAITING"}]}"""
        fakeService.emitTestLobbyEvent(lobbyJson1)
        advanceUntilIdle()
        assertEquals(1, viewModel.games.value.size)

        // Then empty
        val lobbyJson2 = """{"event":"LOBBY_UPDATE","games":[]}"""
        fakeService.emitTestLobbyEvent(lobbyJson2)
        advanceUntilIdle()
        assertEquals(0, viewModel.games.value.size)
    }

    @Test
    fun `onConnected calls subscribeToLobby and requestGameList`() {
        viewModel.onConnected()
        assertTrue(fakeService.subscribeToLobbyCalled)
        assertTrue(fakeService.requestGameListCalled)
    }

    @Test
    fun `createGame delegates to gameService`() = runTest(testDispatcher) {
        viewModel.createGame("TestPlayer")
        advanceUntilIdle()
        assertEquals(1, fakeService.createGameCalls)
        assertEquals("TestPlayer", fakeService.lastCreatedPlayerName)
    }

    @Test
    fun `closeGame delegates to gameService`() {
        viewModel.closeGame("game-123")
        assertEquals(1, fakeService.closeGameCalls)
        assertEquals("game-123", fakeService.lastClosedGameId)
    }

    @Test
    fun `createdGameId is null initially`() {
        assertNull(viewModel.createdGameId.value)
    }

    @Test
    fun `createdGameId updates when createGame succeeds`() = runTest(testDispatcher) {
        viewModel.createGame("Alice")
        advanceUntilIdle()
        assertEquals("test-created-game-id", viewModel.createdGameId.value)
    }

    @Test
    fun `clearCreatedGameId resets to null`() = runTest(testDispatcher) {
        viewModel.createGame("Alice")
        advanceUntilIdle()
        assertEquals("test-created-game-id", viewModel.createdGameId.value)

        viewModel.clearCreatedGameId()
        assertNull(viewModel.createdGameId.value)
    }

    @Test
    fun `createGame resets createdGameId before creating`() = runTest(testDispatcher) {
        viewModel.createGame("OldPlayer")
        advanceUntilIdle()
        assertEquals("test-created-game-id", viewModel.createdGameId.value)

        viewModel.createGame("NewPlayer")
        assertNull(viewModel.createdGameId.value)

        advanceUntilIdle()
        assertEquals("test-created-game-id", viewModel.createdGameId.value)
    }

    @Test
    fun `invalid lobby JSON does not crash`() = runTest(testDispatcher) {
        fakeService.emitTestLobbyEvent("not valid json {{{")
        advanceUntilIdle()
        // Should still have empty games, not crash
        assertEquals(emptyList<Any>(), viewModel.games.value)
    }

    @Test
    fun `invalid game event JSON does not crash`() = runTest(testDispatcher) {
        fakeService.emitTestEvent("not valid json {{{")
        advanceUntilIdle()
        // Should not crash, createdGameId stays null
        assertNull(viewModel.createdGameId.value)
    }
    // ── refreshLobby() tests ────────────────────────────────────────────────────

    @Test
    fun `refreshLobby does nothing when connectionState is false`() = runTest(testDispatcher) {
        fakeService.setConnectionState(false)
        advanceUntilIdle()

        // Reset counters from init
        fakeService.subscribeToLobbyCalled = false
        fakeService.requestGameListCalled = false

        viewModel.refreshLobby()
        advanceUntilIdle()

        assertFalse(fakeService.subscribeToLobbyCalled)
        assertFalse(fakeService.requestGameListCalled)
    }

    @Test
    fun `refreshLobby calls subscribeToLobby then waits for lobbySubscriptionReady`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)
        advanceUntilIdle()

        // Reset counters from init
        fakeService.subscribeToLobbyCalled = false
        fakeService.requestGameListCalled = false

        // Do NOT set lobbySubscriptionReady yet
        fakeService.setLobbySubscriptionReady(false)
        advanceUntilIdle()

        viewModel.refreshLobby()
        advanceUntilIdle()

        // subscribeToLobby should have been called
        assertTrue(fakeService.subscribeToLobbyCalled)
        // requestGameList should NOT have been called yet because subscription is not ready
        assertFalse(fakeService.requestGameListCalled)
    }

    @Test
    fun `refreshLobby calls requestGameList only after lobbySubscriptionReady emits true`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)

        viewModel.refreshLobby()
        runCurrent() // let refreshLobby launch and reach the first { it } suspension

        assertFalse(fakeService.requestGameListCalled)

        // Unblock the waiting coroutine
        fakeService.setLobbySubscriptionReady(true)
        advanceUntilIdle()

        assertTrue(fakeService.subscribeToLobbyCalled)
        assertTrue(fakeService.requestGameListCalled)
    }

    @Test
    fun `refreshLobby logs warning and returns when subscription times out after 2000ms`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)
        advanceUntilIdle()

        fakeService.subscribeToLobbyCalled = false
        fakeService.requestGameListCalled = false
        fakeService.setLobbySubscriptionReady(false)
        advanceUntilIdle()

        // Mock Log.w to capture the warning
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0

        viewModel.refreshLobby()

        // Advance time by just under the 2000ms timeout — should not have timed out yet
        advanceTimeBy(1999L)
        advanceUntilIdle()
        // requestGameList should NOT have been called (no ready signal, not timed out yet)
        assertFalse(fakeService.requestGameListCalled)

        // Advance past the 2000ms timeout
        advanceTimeBy(2L)
        advanceUntilIdle()

        verify(exactly = 1) { Log.w("LobbyViewModel", "Lobby subscription timed out") }
        assertTrue(fakeService.subscribeToLobbyCalled)
        // requestGameList must NOT be called on timeout
        assertFalse(fakeService.requestGameListCalled)
    }

    @Test
    fun `refreshLobby does NOT call requestGameList on timeout`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)
        advanceUntilIdle()

        fakeService.subscribeToLobbyCalled = false
        fakeService.requestGameListCalled = false
        fakeService.setLobbySubscriptionReady(false)
        advanceUntilIdle()

        viewModel.refreshLobby()

        // Advance past the 2000ms timeout
        advanceTimeBy(2500L)
        advanceUntilIdle()

        // subscribeToLobby should have been called
        assertTrue(fakeService.subscribeToLobbyCalled)
        // requestGameList must NOT be called
        assertFalse(fakeService.requestGameListCalled)
    }

    // ── Connection indicator ("Connecting to server…") tests ──────────────────
    @Test
    fun `shows connecting text when not connected`() = runTest(testDispatcher) {
        val job = launch { viewModel.isConnected.collect {} }
        // FakeGameService starts with connectionState = false
        // After init, connect() is called but does NOT set connectionState to true
        // (the real FakeGameService.connect() only sets connectCalled = true)
        advanceUntilIdle()
        // isConnected remains false => LobbyScreen renders "Connecting to server…"
        assertEquals(false,viewModel.isConnected.value)
        job.cancel()
    }
    @Test
    fun `hides connecting text when connection established`() = runTest(testDispatcher) {
        val job = launch { viewModel.isConnected.collect {} }
        advanceUntilIdle()
        // Simulate successful connection
        fakeService.setConnectionState(true)
        advanceUntilIdle()
        // isConnected is true => LobbyScreen hides "Connecting to server…"
        assertTrue(viewModel.isConnected.value)
        job.cancel()
    }
    @Test
    fun `remains connected after leaving GameboardUI and returning to Lobby`() = runTest(testDispatcher) {
        val job = launch { viewModel.isConnected.collect {} }
        // Step 1: Establish connection (simulates successful WebSocket handshake)
        fakeService.setConnectionState(true)
        advanceUntilIdle()
        assertTrue(viewModel.isConnected.value)
        // Step 2: Simulate GameboardUI Activity – the GameService singleton stays alive
        // and retains connectionState = true (GameboardUI does NOT call disconnect())
        // Step 3: User returns to Lobby – a NEW LobbyViewModel is created,
        // observing the SAME GameService which is still connected
        fakeService.connectCalled = false  // reset for verification
        val newViewModel = LobbyViewModel(fakeService)
        val job2 = launch { newViewModel.isConnected.collect {} }
        advanceUntilIdle()
        // The new ViewModel should immediately see connectionState = true
        // because FakeGameService.connectionState still holds the value set in step 1
        assertTrue(newViewModel.isConnected.value)
        // The "Connecting to server…" text will NOT appear in LobbyScreen
        job.cancel()
        job2.cancel()
    }
    @Test
    fun `connecting text reappears after connection loss`() = runTest(testDispatcher) {
        val job = launch { viewModel.isConnected.collect {} }
        // Step 1: Simulate initially connected
        fakeService.setConnectionState(true)
        advanceUntilIdle()
        assertTrue(viewModel.isConnected.value)
        // Step 2: Connection drops (server down, network lost)
        fakeService.setConnectionState(false)
        advanceUntilIdle()
        // isConnected becomes false => LobbyScreen renders "Connecting to server…"
        assertEquals(false,viewModel.isConnected.value)
        job.cancel()
    }
    @Test
    fun `connecting text reappears after reconnect loop gives up`() = runTest(testDispatcher) {
        val job = launch { viewModel.isConnected.collect {} }
        // Step 1: Simulate initial connection
        fakeService.setConnectionState(true)
        advanceUntilIdle()
        assertTrue(viewModel.isConnected.value)
        // Step 2: Connection drops and reconnect fails
        fakeService.setConnectionState(false)
        advanceUntilIdle()
        assertEquals(false,viewModel.isConnected.value)
        // Step 3: Simulate reconnect max attempts reached – still disconnected
        // FakeGameService does NOT implement reconnect, so state stays false
        // => LobbyScreen shows "Connecting to server…" permanently
        assertEquals(false,viewModel.isConnected.value)
        job.cancel()
    }
    // ── End of connection indicator tests ────────────────────────────────────

}
