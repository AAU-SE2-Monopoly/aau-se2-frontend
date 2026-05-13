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
    fun `onConnected calls subscribeToLobby and requestGameList`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)
        viewModel.onConnected()
        runCurrent() // let the launched coroutine in refreshLobby run
        assertTrue(fakeService.subscribeToLobbyCalled)
        // requestGameList should not have been called yet — waiting for lobbySubscriptionReady
        assertFalse(fakeService.requestGameListCalled)

        // Now make subscription ready
        fakeService.setLobbySubscriptionReady(true)
        advanceUntilIdle()

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

    // ── Reconnect flow: connection drop → 5 attempts fail → reconnect() → success ──

    @Test
    fun `reconnect full flow after 5 failed attempts`() = runTest(testDispatcher) {
        // ============================================================
        // SCENARIO:
        // 1. User is on LobbyScreen, connected
        // 2. Connection drops, 5 reconnect attempts fail
        // 3. reconnectFailed = true, isConnected = false → "Reconnect" button shown
        // 4. User clicks "Reconnect" → viewModel.reconnect()
        // 5. Server starts at this moment
        // 6. Connect succeeds → isConnected = true → Connected ✓ shown
        // 7. onConnected() triggers → subscribeToLobby() called
        // 8. lobbySubscriptionReady becomes true → requestGameList() called
        // ============================================================

        // Subscribe to flows FIRST. stateIn(WhileSubscribed) only propagates
        // upstream values while there is at least one subscriber.
        val isConnectedValues = mutableListOf<Boolean>()
        val reconnectFailedValues = mutableListOf<Boolean>()
        val connectedJob = launch { viewModel.isConnected.collect { isConnectedValues.add(it) } }
        val reconnectJob = launch { viewModel.reconnectFailed.collect { reconnectFailedValues.add(it) } }
        advanceUntilIdle()

        // --- Phase 1: Initially connected ---
        fakeService.setConnectionState(true)
        advanceUntilIdle()
        assertTrue(isConnectedValues.contains(true), "Phase 1: should become connected")
        assertFalse(reconnectFailedValues.contains(true), "Phase 1: reconnectFailed should be false")

        // --- Phase 2: Connection drops, all 5 reconnect attempts fail ---
        fakeService.setConnectionState(false)
        fakeService.setReconnectFailed(true)
        advanceUntilIdle()

        // Assert: Connection is lost
        assertFalse(isConnectedValues.last(), "Phase 2: isConnected should be false")
        // Assert: Reconnect failed indicator is shown
        assertTrue(reconnectFailedValues.contains(true), "Phase 2: reconnectFailed should be true")
        // This is the state where LobbyScreen renders the "Reconnect" button
        // (in LobbyScreen: if (reconnectFailed && !isConnected) → show Reconnect Button)

        // Record connect calls before user action
        val connectCallsBefore = fakeService.connectCalls

        // --- Phase 3: User clicks "Reconnect" ---
        viewModel.reconnect()
        advanceUntilIdle()

        // Assert: connect() was called on the GameService
        assertTrue(fakeService.connectCalls == connectCallsBefore + 1,
            "Phase 3: Reconnect should call connect() on GameService"
        )

        // --- Phase 4: Server comes up, connection succeeds ---
        fakeService.setConnectionState(true)
        fakeService.setReconnectFailed(false)
        advanceUntilIdle()

        // Assert: isConnected is true → LobbyScreen shows "Connected ✓"
        assertTrue(isConnectedValues.last(),
            "Phase 4: After successful reconnect, isConnected should be true"
        )
        // Assert: reconnectFailed is cleared
        assertFalse(reconnectFailedValues.last(),
            "Phase 4: After successful reconnect, reconnectFailed should be false"
        )

        // --- Phase 5: LaunchedEffect(isConnected) calls onConnected() ---
        // The LobbyScreen's LaunchedEffect(isConnected) would call viewModel.onConnected()
        // when isConnected becomes true. We simulate that here.
        viewModel.onConnected()
        runCurrent()

        // Assert: subscribeToLobby was called
        assertTrue(fakeService.subscribeToLobbyCalled, "Phase 5: subscribeToLobby should be called")

        // --- Phase 6: Lobby subscription becomes ready → requestGameList ---
        fakeService.setLobbySubscriptionReady(true)
        advanceUntilIdle()

        // Assert: requestGameList was called after lobby subscription is ready
        assertTrue(fakeService.requestGameListCalled,
            "Phase 6: After lobby subscription is ready, requestGameList should be called"
        )

        // --- Phase 7: Verify the full end state ---
        assertTrue(isConnectedValues.last(), "Final state: isConnected = true")
        assertFalse(reconnectFailedValues.last(), "Final state: reconnectFailed = false")
        assertTrue(fakeService.subscribeToLobbyCalled, "Final state: subscribeToLobby called")
        assertTrue(fakeService.requestGameListCalled, "Final state: requestGameList called")

        connectedJob.cancel()
        reconnectJob.cancel()
    }

    @Test
    fun `reconnect calls connect on gameService when in failed state`() = runTest(testDispatcher) {
        // This test verifies that clicking "Reconnect" calls connect() on the GameService
        // when in the failed state (reconnectFailed = true, isConnected = false).

        // Subscribe to flows first
        val connectedJob = launch { viewModel.isConnected.collect {} }
        val reconnectJob = launch { viewModel.reconnectFailed.collect {} }
        advanceUntilIdle()

        // Simulate failed state
        fakeService.setConnectionState(false)
        fakeService.setReconnectFailed(true)
        advanceUntilIdle()

        assertTrue(viewModel.reconnectFailed.value, "Should be in reconnectFailed state")
        assertFalse(viewModel.isConnected.value, "Should be disconnected")

        val callsBefore = fakeService.connectCalls

        // User clicks reconnect
        viewModel.reconnect()
        advanceUntilIdle()

        assertTrue(fakeService.connectCalls == callsBefore + 1,
            "connect() should be called on reconnect")

        connectedJob.cancel()
        reconnectJob.cancel()
    }

    @Test
    fun `reconnectFailed becomes true after reconnect loop exhaustion`() = runTest(testDispatcher) {
        // This test proves the state transition from connected → disconnected
        // → reconnectFailed = true (simulating 5 failed attempts)

        // Subscribe to flows first
        val isConnectedValues = mutableListOf<Boolean>()
        val reconnectFailedValues = mutableListOf<Boolean>()
        val connectedJob = launch { viewModel.isConnected.collect { isConnectedValues.add(it) } }
        val reconnectJob = launch { viewModel.reconnectFailed.collect { reconnectFailedValues.add(it) } }
        advanceUntilIdle()

        // Step 1: Start connected
        fakeService.setConnectionState(true)
        advanceUntilIdle()
        assertTrue(isConnectedValues.contains(true), "Step 1: should be connected")
        assertFalse(reconnectFailedValues.contains(true), "Step 1: reconnectFailed should be false")

        // Step 2: Connection drops
        fakeService.setConnectionState(false)
        advanceUntilIdle()
        assertFalse(isConnectedValues.last(), "Step 2: isConnected should be false")

        // Step 3: All 5 reconnect attempts have been exhausted
        fakeService.setReconnectFailed(true)
        advanceUntilIdle()

        assertTrue(reconnectFailedValues.contains(true), "Step 3: reconnectFailed should be true")
        assertFalse(isConnectedValues.last(), "Step 3: isConnected should stay false")

        // This is the exact state where LobbyScreen shows the "Reconnect" button
        // because of: if (reconnectFailed && !isConnected) → button

        connectedJob.cancel()
        reconnectJob.cancel()
    }

    @Test
    fun `reconnect multiple times re-establishes connection each time`() = runTest(testDispatcher) {
        // This test proves that the reconnect button works repeatedly
        // (e.g. if the server keeps going down)

        // Subscribe to flows first
        val isConnectedValues = mutableListOf<Boolean>()
        val reconnectFailedValues = mutableListOf<Boolean>()
        val connectedJob = launch { viewModel.isConnected.collect { isConnectedValues.add(it) } }
        val reconnectJob = launch { viewModel.reconnectFailed.collect { reconnectFailedValues.add(it) } }
        advanceUntilIdle()

        // --- First failure + reconnect cycle ---
        // Initial connection
        fakeService.setConnectionState(true)
        advanceUntilIdle()
        assertTrue(isConnectedValues.contains(true), "Initial connection")

        // Drop and exhaust reconnects
        fakeService.setConnectionState(false)
        fakeService.setReconnectFailed(true)
        advanceUntilIdle()
        assertTrue(reconnectFailedValues.contains(true), "After first drop, reconnectFailed should be true")

        // Reconnect #1
        val callsBefore1 = fakeService.connectCalls
        viewModel.reconnect()
        advanceUntilIdle()
        assertEquals(callsBefore1 + 1, fakeService.connectCalls, "First reconnect should call connect()")

        fakeService.setConnectionState(true)
        fakeService.setReconnectFailed(false)
        advanceUntilIdle()
        assertTrue(isConnectedValues.last(), "After first reconnect, isConnected should be true")
        assertFalse(reconnectFailedValues.last(), "After first reconnect, reconnectFailed should be false")

        // --- Second failure + reconnect cycle ---
        fakeService.setConnectionState(false)
        fakeService.setReconnectFailed(true)
        advanceUntilIdle()
        assertTrue(reconnectFailedValues.contains(true), "After second drop, reconnectFailed should be true")

        // Reconnect #2
        val callsBefore2 = fakeService.connectCalls
        viewModel.reconnect()
        advanceUntilIdle()
        assertEquals(callsBefore2 + 1, fakeService.connectCalls, "Second reconnect should call connect()")

        fakeService.setConnectionState(true)
        fakeService.setReconnectFailed(false)
        advanceUntilIdle()
        assertTrue(isConnectedValues.last(), "After second reconnect, isConnected should be true")
        assertFalse(reconnectFailedValues.last(), "After second reconnect, reconnectFailed should be false")

        connectedJob.cancel()
        reconnectJob.cancel()
    }

    // ── rejoinGame() tests ──────────────────────────────────────────────────

    @Test
    fun `rejoinGame calls joinGame on gameService`() = runTest(testDispatcher) {
        fakeService.setConnectionState(true)
        viewModel.rejoinGame("game-42")
        advanceUntilIdle()
        assertEquals(1, fakeService.joinGameCalls)
        assertEquals("game-42", fakeService.lastJoinedGameId)
        assertEquals("test-player-name", fakeService.lastJoinedPlayerName)
        assertEquals("lindwurm", fakeService.lastJoinedIconId)
    }

    @Test
    fun `rejoinGame logs on success`() = runTest(testDispatcher) {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        fakeService.setConnectionState(true)
        fakeService.joinGameSuccess = true
        viewModel.rejoinGame("game-42")
        advanceUntilIdle()

        verify(exactly = 1) { Log.d("LobbyViewModel", match { it.startsWith("Rejoined game game-42") }) }
    }

    @Test
    fun `rejoinGame logs warning on failure`() = runTest(testDispatcher) {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        fakeService.setConnectionState(true)
        fakeService.joinGameSuccess = false
        viewModel.rejoinGame("game-42")
        advanceUntilIdle()

        verify(exactly = 1) { Log.w(eq("LobbyViewModel"), match<String> { it.startsWith("Failed to rejoin game:") }) }
    }

    @Test
    fun `rejoinGame emits error when not connected`() = runTest(testDispatcher) {
        val messages = mutableListOf<String>()
        val job = launch { viewModel.rejoinErrors.collect { messages.add(it) } }
        advanceUntilIdle()

        viewModel.rejoinGame("game-42")
        advanceUntilIdle()

        assertEquals(listOf("Not connected to server. Please wait..."), messages)
        assertEquals(0, fakeService.joinGameCalls)
        job.cancel()
    }

    @Test
    fun `rejoinGame emits error when joinGame fails`() = runTest(testDispatcher) {
        val messages = mutableListOf<String>()
        val job = launch { viewModel.rejoinErrors.collect { messages.add(it) } }
        advanceUntilIdle()

        fakeService.setConnectionState(true)
        fakeService.joinGameSuccess = false
        viewModel.rejoinGame("game-42")
        advanceUntilIdle()

        assertEquals(listOf("Join rejected by server"), messages)
        job.cancel()
    }

    @Test
    fun `rejoinGame emits navigation on success`() = runTest(testDispatcher) {
        val gameIds = mutableListOf<String>()
        val job = launch { viewModel.rejoinNavigation.collect { gameIds.add(it) } }
        advanceUntilIdle()

        fakeService.setConnectionState(true)
        fakeService.joinGameSuccess = true
        viewModel.rejoinGame("game-42")
        advanceUntilIdle()

        assertEquals(listOf("game-42"), gameIds)
        job.cancel()
    }

    // ── Game card filtering tests ──────────────────────────────────────────

    @Test
    fun `lobby filtering shows open games always`() = runTest(testDispatcher) {
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
                    "phase": "WAITING",
                    "playerIds": ["p1"]
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        val games = viewModel.games.value
        assertEquals(1, games.size)
        assertEquals("g1", games[0].gameId)
    }

    @Test
    fun `lobby filtering hides finished games`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "p1",
                    "playerCount": 3,
                    "maxPlayers": 4,
                    "phase": "FINISHED",
                    "playerIds": ["p1", "p2", "p3"]
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        assertEquals(0, viewModel.games.value.size)
    }

    @Test
    fun `lobby filtering shows full game when player is member`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "test-player-id",
                    "playerCount": 4,
                    "maxPlayers": 4,
                    "phase": "WAITING",
                    "playerIds": ["test-player-id", "p2", "p3", "p4"]
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        val games = viewModel.games.value
        assertEquals(1, games.size)
        assertEquals("g1", games[0].gameId)
    }

    @Test
    fun `lobby filtering hides full game when player is not member`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "p1",
                    "playerCount": 4,
                    "maxPlayers": 4,
                    "phase": "WAITING",
                    "playerIds": ["p1", "p2", "p3", "p4"]
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        assertEquals(0, viewModel.games.value.size)
    }

    @Test
    fun `lobby filtering shows in-progress game when player is member`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "test-player-id",
                    "playerCount": 2,
                    "maxPlayers": 4,
                    "phase": "ROLLING",
                    "playerIds": ["test-player-id", "p2"]
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        val games = viewModel.games.value
        assertEquals(1, games.size)
        assertEquals("g1", games[0].gameId)
    }

    @Test
    fun `lobby filtering hides in-progress game when player is not member`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g1",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "p1",
                    "playerCount": 2,
                    "maxPlayers": 4,
                    "phase": "ROLLING",
                    "playerIds": ["p1", "p2"]
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        assertEquals(0, viewModel.games.value.size)
    }

    @Test
    fun `lobby filtering sorts games open first then in progress`() = runTest(testDispatcher) {
        val lobbyJson = """
        {
            "event": "LOBBY_UPDATE",
            "games": [
                {
                    "gameId": "g-in-progress",
                    "hostPlayerName": "Bob",
                    "hostPlayerId": "test-player-id",
                    "playerCount": 2,
                    "maxPlayers": 4,
                    "phase": "ROLLING",
                    "playerIds": ["test-player-id", "p1"]
                },
                {
                    "gameId": "g-open",
                    "hostPlayerName": "Alice",
                    "hostPlayerId": "p2",
                    "playerCount": 1,
                    "maxPlayers": 4,
                    "phase": "WAITING",
                    "playerIds": ["p2"]
                }
            ]
        }
        """.trimIndent()

        fakeService.emitTestLobbyEvent(lobbyJson)
        advanceUntilIdle()

        val games = viewModel.games.value
        assertEquals(2, games.size)
        assertEquals("g-open", games[0].gameId)
        assertEquals("g-in-progress", games[1].gameId)
    }

    // ── End of lobby filtering tests ──────────────────────────────────────
}
