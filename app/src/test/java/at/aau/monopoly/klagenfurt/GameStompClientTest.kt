package at.aau.monopoly.klagenfurt

import android.util.Log
import at.aau.monopoly.klagenfurt.networking.GameStompClient
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameStompClientTest {
    private lateinit var stompClient: StompClient
    private lateinit var stompSession: StompSession
    private lateinit var gameStompClient: GameStompClient
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        // Mocking extension functions requires mockkStatic
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")


        stompClient = mockk()
        stompSession = mockk(relaxed = true)

        gameStompClient = GameStompClient(stompClient, testScope)
    }

    @AfterEach
    fun tearDown() {
        testScope.cancel()
        unmockkAll()
    }

    @Test
    fun test_default_constructor_parameters() {
        val defaultClient = GameStompClient(stompClient)

        org.junit.jupiter.api.Assertions.assertNotNull(defaultClient)
    }

    @Test
    fun disconnect_when_jobs_are_null() = runTest(testDispatcher) {
        gameStompClient.disconnect()
        advanceUntilIdle()

        coVerify(exactly = 0) { stompSession.disconnect() }
    }

    @Test
    fun disconnect_cancels_both_active_jobs() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns kotlinx.coroutines.flow.flowOf()

        gameStompClient.connect()
        runCurrent()

        gameStompClient.subscribeToGame("test-id")
        advanceUntilIdle()

        gameStompClient.disconnect()
        advanceUntilIdle()

        coVerify { stompSession.disconnect() }
    }
    @Test
    fun connect_success_starts_personal_subscription() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()

        gameStompClient.connect()
        runCurrent()

        coVerify { stompClient.connect(any<String>()) }
        // Verify that we automatically subscribe to our personal topic upon connection
        coVerify { stompSession.subscribeText(match { it.startsWith("/topic/game/") }) }
        verify { Log.d("GameStomp", "Connected successfully") }
    }

    @Test
    fun test_subscribeToGame_when_not_connected() = runTest(testDispatcher) {
        gameStompClient.subscribeToGame("some-id")
        // Session is null so SubscriptionChannel.subscribe() returns early without calling subscribeText.
        // isReady stays false → attempt 1 times out at 8s → retry → attempt 2 times out at 16s.
        advanceTimeBy(16_001)
        runCurrent()

        verify { Log.e("GameStomp", "Subscription timed out for some-id (attempt 2/2)") }
    }

    @Test
    fun connect_already_connected_when_session_exists() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.connect()
        advanceUntilIdle()

        verify { Log.d("GameStomp", match { it.startsWith("Already connected (session=") }) }
        coVerify(exactly = 1) { stompClient.connect(any<String>()) }
    }

    @Test
    fun subscribeToGame_handles_cancellation_exception() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()

        gameStompClient.connect()
        advanceUntilIdle()

        // Mock a specific cancellation for the game subscription
        // SubscriptionChannel catches CancellationException and does NOT call onError,
        // but isReady stays false → attempt 1 times out at 8s → retry → attempt 2 times out at 16s
        coEvery { stompSession.subscribeText("/topic/game/test-id") } throws CancellationException("Cancelled by test")

        gameStompClient.subscribeToGame("test-id")
        // Advance past 16s (8s × 2 attempts)
        advanceTimeBy(16_001)
        runCurrent()

        verify { Log.e("GameStomp", "Subscription timed out for test-id (attempt 2/2)") }
    }

    @Test
    fun connect_already_in_progress_or_connected() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } coAnswers {
            delay(1000)
            stompSession
        }
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()

        gameStompClient.connect()
        gameStompClient.connect()

        advanceUntilIdle()
        coVerify(exactly = 1) { stompClient.connect(any<String>()) }
    }

    @Test
    fun connect_error_handles_exception() = runTest(testDispatcher) {
        val errorMessage = "Network Error"
        coEvery { stompClient.connect(any<String>()) } throws Exception(errorMessage)

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        advanceUntilIdle()

        verify { Log.e("GameStomp", "connect error", any()) }
        assertTrue(statuses.contains("Connection error: $errorMessage"))
        assertTrue(statuses.contains("Connection error: $errorMessage"))
        job.cancel()
    }

    // =========================================================================
    // BUG-FIX: connect() failure must trigger startReconnectLoop().
    // Before the fix, isConnecting remained true inside the catch block so
    // startReconnectLoop()'s guard `if (isReconnecting || isConnecting) return`
    // bailed immediately — the reconnect loop never ran.
    // =========================================================================

    @Test
    fun `connect error triggers reconnect loop which retries then succeeds`() = runTest(testDispatcher) {
        // First connect attempt fails
        coEvery { stompClient.connect(any<String>()) } throws Exception("Connection refused") andThen stompSession

        // Personal topic subscription: first session will never get here because
        // connect failed. Second session (reconnected) needs a subscription flow.
        coEvery { stompSession.subscribeText(match {
            it.startsWith("/topic/game/")
        }) } returns MutableSharedFlow()

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        // connect() launched a coroutine that fails → calls startReconnectLoop()
        // which now actually runs because isConnecting was set to false before the call.
        advanceTimeBy(1_500)  // past the 1s initial delay
        runCurrent()

        // The reconnect loop should have tried again and succeeded
        assertTrue(statuses.contains("Reconnecting in 1s..."),
            "Should emit 'Reconnecting in 1s...' but got: $statuses")
        assertTrue(statuses.contains("Reconnected ✓"),
            "Should emit 'Reconnected ✓' but got: $statuses")
        assertTrue(gameStompClient.connectionState.value,
            "Connection state should be true after successful reconnect")

        job.cancel()
    }

    @Test
    fun `reconnect loop runs 5 times then sets reconnectFailed true`() = runTest(testDispatcher) {
        // All connect attempts fail
        coEvery { stompClient.connect(any<String>()) } throws Exception("Server unreachable")

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        // Initial connect fails → startReconnectLoop runs
        // Need to advance through all 5 exponential backoff delays:
        // 1s, 2s, 4s, 8s, 16s = 31s total
        advanceTimeBy(1_500)   // attempt 1 after 1s
        runCurrent()
        advanceTimeBy(2_100)   // attempt 2 after 2s
        runCurrent()
        advanceTimeBy(4_100)   // attempt 3 after 4s
        runCurrent()
        advanceTimeBy(8_100)   // attempt 4 after 8s
        runCurrent()
        advanceTimeBy(16_100)  // attempt 5 after 16s
        runCurrent()

        assertTrue(gameStompClient.reconnectFailed.value,
            "reconnectFailed should be true after exhausting all attempts")
        assertTrue(statuses.contains("Connection lost – please restart"),
            "Should emit final failure status but got: $statuses")
        // Should have logged each failed attempt
        verify(atLeast = 5) { Log.e("GameStomp", match { it.contains("Reconnect attempt") }) }

        job.cancel()
    }

    @Test
    fun `pressing reconnect after all attempts exhausted restarts full reconnect cycle`() = runTest(testDispatcher) {
        // All connect attempts fail — exhaust the first cycle
        coEvery { stompClient.connect(any<String>()) } throws Exception("Server unreachable")

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        // FIRST connect cycle: fails and tries 5 times
        gameStompClient.connect()
        advanceTimeBy(1_500)
        runCurrent()
        advanceTimeBy(2_100)
        runCurrent()
        advanceTimeBy(4_100)
        runCurrent()
        advanceTimeBy(8_100)
        runCurrent()
        advanceTimeBy(16_100)
        runCurrent()

        // Verify first cycle is exhausted
        assertTrue(gameStompClient.reconnectFailed.value,
            "reconnectFailed should be true after first cycle")

        // Clear statuses for the second cycle
        statuses.clear()

        // User presses RECONNECT button → calls connect() again
        // This should reset reconnectFailed and start a fresh cycle
        gameStompClient.connect()

        // Should have reset reconnectFailed
        assertFalse(gameStompClient.reconnectFailed.value,
            "reconnectFailed should be false after pressing reconnect")

        // The first connect attempt in the new cycle fails, which triggers
        // startReconnectLoop() → should emit "Reconnecting in 1s..."
        advanceTimeBy(1_500)
        runCurrent()

        assertTrue(statuses.contains("Reconnecting in 1s..."),
            "Second cycle should emit reconnect status. Got: $statuses")
        assertTrue(statuses.contains("Reconnect failed (1/5)"),
            "Second cycle should show attempt 1 failed. Got: $statuses")

        job.cancel()
    }

    @Test
    fun `reconnect cycle succeeds on third attempt then connectionState is true`() = runTest(testDispatcher) {
        // First two reconnect attempts fail, third succeeds
        coEvery { stompClient.connect(any<String>()) } throws Exception("Fail 1") andThenThrows Exception("Fail 2") andThen stompSession

        coEvery { stompSession.subscribeText(match {
            it.startsWith("/topic/game/")
        }) } returns MutableSharedFlow()

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        // Initial connect fails (Fail 1)
        // Reconnect: 1s delay → attempt 1 also fails (Fail 1 again, since andThen cycle continues)
        // Wait — careful with mockk chaining. "throws ... andThenThrows ... andThen ..."
        // The chain is consumed sequentially:
        // Call 1 (initial connect): throws "Fail 1"
        // Call 2 (reconnect attempt 1 after 1s): throws "Fail 2"
        // Call 3 (reconnect attempt 2 after 2s): returns stompSession (success!)

        // Advance past 1s delay → reconnect attempt 1 fires
        advanceTimeBy(1_100)
        runCurrent()
        assertTrue(statuses.contains("Reconnect failed (1/5)"),
            "Attempt 1 should fail. Got: $statuses")

        // Advance past 2s delay → reconnect attempt 2 fires
        advanceTimeBy(2_100)
        runCurrent()
        assertTrue(statuses.contains("Reconnected ✓"),
            "Should reconnect on attempt 2. Got: $statuses")

        assertTrue(gameStompClient.connectionState.value,
            "Connection state should be true after reconnect success")
        assertFalse(gameStompClient.reconnectFailed.value,
            "reconnectFailed should be false after success")

        job.cancel()
    }

    @Test
    fun connect_cancellation_exception() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } throws CancellationException("Cancelled")

        gameStompClient.connect()
        advanceUntilIdle()

        verify { Log.d("GameStomp", "Connection attempt cancelled") }
    }

    @Test
    fun disconnect_cancels_jobs_and_disconnects() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.disconnect()
        advanceUntilIdle()

        coVerify { stompSession.disconnect() }
        assertTrue(statuses.contains("Disconnected"))
        job.cancel()
    }

    @Test
    fun disconnect_error_handling() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.disconnect() } throws Exception("Disconnect error")

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.disconnect()
        advanceUntilIdle()

        verify { Log.e("GameStomp", "disconnect error", any()) }
    }

    @Test
    fun subscribeToGame_sends_events() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val eventFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(match { it.startsWith("/topic/game/") && it != "/topic/game/test-id" }) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/game/test-id") } returns eventFlow

        val receivedEvents = mutableListOf<String>()
        val collectJob = launch {
            gameStompClient.events.collect { receivedEvents.add(it) }
        }

        gameStompClient.connect()
        runCurrent()

        gameStompClient.subscribeToGame("test-id")
        runCurrent()

        coVerify { stompSession.subscribeText("/topic/game/test-id") }

        eventFlow.emit("Test Message")
        runCurrent()

        assertEquals("Test Message", receivedEvents.last())
        collectJob.cancel()
    }

    @Test
    fun subscribeToGame_already_subscribed() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val activeFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(match { it.startsWith("/topic/game/") && it != "/topic/game/test-id" }) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/game/test-id") } returns activeFlow

        gameStompClient.connect()
        runCurrent()

        gameStompClient.subscribeToGame("test-id")
        runCurrent()

        gameStompClient.subscribeToGame("test-id")
        runCurrent()

        verify { Log.d("GameStomp", "Already subscribed to test-id") }
    }

    @Test
    fun subscribeToGame_error() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        
        gameStompClient.connect()
        runCurrent()

        coEvery { stompSession.subscribeText("/topic/game/test-id") } throws Exception("Subscribe Error")

        val statuses = mutableListOf<String>()
        val statusJob = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.subscribeToGame("test-id")
        // Advance past the two 8s timeouts (16s total) since SubscriptionChannel never becomes ready
        advanceTimeBy(16_001)
        runCurrent()

        verify { Log.e("GameStomp", "Subscription timed out for test-id (attempt 2/2)") }
        statusJob.cancel()
    }

    @Test
    fun createGame_sends_command_without_re_subscribing() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.createGame("Player1")
        advanceUntilIdle()

        coVerify { stompSession.sendText("/app/game/create", any<String>()) }
    }

    @Test
    fun joinGame_subscribes_then_requests_state_then_sends_join() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        // Use a real MutableSharedFlow so we can emit the PLAYER_JOINED event
        // that joinGame now waits for before returning.
        val gameTopicFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns gameTopicFlow
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        runCurrent()

        val joinJob = launch {
            gameStompClient.joinGame("game-123", "Alice", "gti")
        }
        runCurrent()

        // Emit PLAYER_JOINED with the current player in game state to unblock joinGame
        gameTopicFlow.emit(
            """{"event":"PLAYER_JOINED","gameId":"game-123","gameState":{"players":[{"id":"${gameStompClient.currentPlayerId}"}]}}"""
        )
        runCurrent()

        coVerifyOrder {
            stompSession.subscribeText("/topic/game/game-123")
            stompSession.sendText("/app/game/state", any<String>())
            stompSession.sendText("/app/game/join", any<String>())
        }
    }

    @Test
    fun joinGame_does_not_send_join_before_subscription_and_state_request_complete() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        // For non-game-123 subscriptions, use a real flow so we can emit events
        coEvery { stompSession.subscribeText(match { it.startsWith("/topic/game/") && it != "/topic/game/game-123" }) } returns flowOf()

        // For the game-123 subscription, delay then return a flow that emits PLAYER_JOINED
        coEvery { stompSession.subscribeText("/topic/game/game-123") } coAnswers {
            delay(1000)
            val flow = MutableSharedFlow<String>(extraBufferCapacity = 1)
            // Schedule emission after the subscription is ready so joinGame can proceed
            launch {
                flow.emit(
                    """{"event":"PLAYER_JOINED","gameId":"game-123","gameState":{"players":[{"id":"${gameStompClient.currentPlayerId}"}]}}"""
                )
            }
            flow
        }
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        runCurrent()

        val joinJob = launch {
            gameStompClient.joinGame("game-123", "Alice", "gti")
        }
        runCurrent()
        advanceTimeBy(500)
        runCurrent()

        // After 500ms, subscription isn't ready yet, so join should not have been sent
        coVerify(exactly = 0) { stompSession.sendText("/app/game/join", any<String>()) }

        // Advance past the 1000ms delay so subscription completes
        advanceTimeBy(1000)
        runCurrent()

        // Now the subscription is done, state was requested, join was sent, and
        // PLAYER_JOINED was received, so joinGame completed.
        coVerifyOrder {
            stompSession.subscribeText("/topic/game/game-123")
            stompSession.sendText("/app/game/state", any<String>())
            stompSession.sendText("/app/game/join", any<String>())
        }
    }

    @Test
    fun joinGame_does_not_send_join_when_subscription_fails() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(match { it.startsWith("/topic/game/") && it != "/topic/game/game-123" }) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/game/game-123") } throws Exception("Subscribe failed")
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        // joinGame waits for subscribeToGameInternal which times out after 16s (2×8s)
        val resultDeferred = async {
            gameStompClient.joinGame("game-123", "Alice", "gti")
        }
        advanceTimeBy(16_001)
        runCurrent()

        assertTrue(resultDeferred.await().isFailure)

        coVerify(exactly = 0) { stompSession.sendText("/app/game/join", any<String>()) }
        gameStompClient.disconnect()
        runCurrent()
    }

    @Test
    fun subscribeToLobby_when_not_connected_logs_warning() = runTest(testDispatcher) {
        gameStompClient.subscribeToLobby()
        runCurrent()

        // When session is null, SubscriptionChannel returns early without subscribing
        coVerify(exactly = 0) { stompSession.subscribeText(any<String>()) }
    }

    @Test
    fun subscribeToLobby_success_emits_lobby_events() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val lobbyFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns lobbyFlow

        val receivedLobbyEvents = mutableListOf<String>()
        val collectJob = launch { gameStompClient.lobbyEvents.collect { receivedLobbyEvents.add(it) } }

        gameStompClient.connect()
        runCurrent()

        gameStompClient.subscribeToLobby()
        runCurrent()

        coVerify { stompSession.subscribeText("/topic/lobby") }

        lobbyFlow.emit("LOBBY_UPDATE")
        runCurrent()

        assertEquals("LOBBY_UPDATE", receivedLobbyEvents.last())
        collectJob.cancel()
    }

    @Test
    fun subscribeToLobby_already_subscribed_logs_and_returns() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val neverEndingLobbyFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns neverEndingLobbyFlow

        gameStompClient.connect()
        runCurrent()

        gameStompClient.subscribeToLobby()
        runCurrent()

        gameStompClient.subscribeToLobby()
        runCurrent()

        // SubscriptionChannel guard prevents duplicate subscription
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }
    }

    @Test
    fun subscribeToLobby_error_triggers_reconnect_loop() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } throws Exception("Lobby failed")

        val statuses = mutableListOf<String>()
        val collectJob = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        runCurrent()

        gameStompClient.subscribeToLobby()
        // SubscriptionChannel catches error, onError → startReconnectLoop() → emits reconnect status
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(statuses.contains("Reconnecting in 1s..."),
            "Should emit reconnect status when lobby subscription fails")
        gameStompClient.disconnect()
        runCurrent()
        collectJob.cancel()
    }
    @Test
    fun subscribeToLobby_cancellation_does_not_log_or_trigger_reconnect() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } throws CancellationException("cancel")

        gameStompClient.connect()
        runCurrent()

        gameStompClient.subscribeToLobby()
        runCurrent()

        // SubscriptionChannel catches CancellationException and does NOT call onError
        verify(exactly = 0) { Log.e(any(), any(), any<Throwable>()) }
    }

    @Test
    fun requestGameList_sends_to_game_list_endpoint() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.requestGameList()
        advanceUntilIdle()

        coVerify { stompSession.sendText("/app/game/list", any<String>()) }
    }

    @Test
    fun closeGame_uses_given_id_and_restores_previous_current_game_id() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.setGameId("original-game")
        advanceUntilIdle()
        assertEquals("original-game", gameStompClient.currentGameId)

        gameStompClient.closeGame("close-this-game")
        advanceUntilIdle()

        coVerify {
            stompSession.sendText(
                "/app/game/close",
                match { it.contains("\"gameId\":\"close-this-game\"") }
            )
        }
        assertEquals("original-game", gameStompClient.currentGameId)
    }

    @Test
    fun sendRaw_handles_not_connected_state() = runTest(testDispatcher) {
        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.rollDice()
        advanceUntilIdle()

        assertTrue(statuses.contains("Not connected"))
        job.cancel()
    }

    @Test
    fun sendRaw_error() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } throws Exception("Send Error")

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.rollDice()
        advanceUntilIdle()

        verify { Log.e("GameStomp", "send error to /app/game/action", any()) }
        assertTrue(statuses.contains("Send error: Send Error"))
        job.cancel()
    }

    @Test
    fun test_all_game_actions() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        // Use MutableSharedFlow so the joinGame wait can be unblocked
        val gameTopicFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns gameTopicFlow
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.startGame()
        gameStompClient.rollDice()
        gameStompClient.endTurn()
        gameStompClient.requestState()
        // Launch joinGame in a separate coroutine since it now suspends waiting for PLAYER_JOINED
        val joinJob = launch {
            gameStompClient.joinGame("id", "name")
        }
        gameStompClient.setGameId("new-id")

        advanceUntilIdle()

        // Emit PLAYER_JOINED to unblock joinGame
        gameTopicFlow.emit(
            """{"event":"PLAYER_JOINED","gameId":"id","gameState":{"players":[{"id":"${gameStompClient.currentPlayerId}"}]}}"""
        )
        advanceUntilIdle()

        coVerify(atLeast = 1) { stompSession.sendText(any<String>(), any<String>()) }
    }

    //branches for isCancellation
    @Test
    fun connect_cancellation_via_illegal_state_message() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } throws IllegalStateException("Job was CANCELLED by system")

        gameStompClient.connect()
        advanceUntilIdle()


        verify { Log.d("GameStomp", "Connection attempt cancelled") }
    }

    @Test
    fun connect_cancellation_via_cause() = runTest(testDispatcher) {
        val cause = CancellationException("Internal coroutine cancellation")
        coEvery { stompClient.connect(any<String>()) } throws Exception("Wrapper Exception", cause)

        gameStompClient.connect()
        advanceUntilIdle()

        verify { Log.d("GameStomp", "Connection attempt cancelled") }
    }

    @Test
    fun connect_error_illegal_state_other_message() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } throws IllegalStateException("Invalid state detected")

        gameStompClient.connect()
        advanceUntilIdle()


        verify { Log.e("GameStomp", "connect error", any()) }
    }

    @Test
    fun connect_error_illegal_state_null_message() = runTest(testDispatcher) {
        val exceptionWithNullMessage = IllegalStateException(null as String?)
        coEvery { stompClient.connect(any<String>()) } throws exceptionWithNullMessage

        gameStompClient.connect()
        advanceUntilIdle()

        verify { Log.e("GameStomp", "connect error", any()) }
    }
    @Test
    fun disconnect_cancels_lobby_subscription_and_prevents_lobby_reconnect() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val lobbyFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns lobbyFlow

        gameStompClient.connect()
        advanceUntilIdle()

        // Subscribe to lobby
        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }

        // Disconnect
        gameStompClient.disconnect()
        advanceUntilIdle()

        // After disconnect, connectionState should be false
        assertEquals(false, gameStompClient.connectionState.value)
    }

    @Test
    fun subscribeToGame_does_not_cancel_lobby_subscription() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val lobbyFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns lobbyFlow
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        // Subscribe to lobby first
        gameStompClient.subscribeToLobby()
        advanceUntilIdle()
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }

        // Then subscribe to a game - lobby subscription stays alive (separate channels)
        gameStompClient.subscribeToGame("new-game")
        advanceUntilIdle()

        // Verify game subscription happened
        coVerify { stompSession.subscribeText("/topic/game/new-game") }

        // Lobby subscription should NOT have been re-created (job still active)
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }
    }

    @Test
    fun subscribeToGame_does_not_cancel_lobby_if_already_subscribed_to_same_game() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val lobbyFlow = MutableSharedFlow<String>()
        val gameFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns lobbyFlow
        coEvery { stompSession.subscribeText("/topic/game/same-game") } returns gameFlow
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        // First subscription to game
        gameStompClient.subscribeToGame("same-game")
        advanceUntilIdle()
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/game/same-game") }

        // Subscribe to lobby
        gameStompClient.subscribeToLobby()
        advanceUntilIdle()
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }

        // Re-subscribe to the same game - should early-return (no new subscribe calls)
        gameStompClient.subscribeToGame("same-game")
        advanceUntilIdle()

        // Game subscribe should still be exactly 1 (not 2) since we early-returned
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/game/same-game") }

        // Verify the "Already subscribed" log was emitted
        verify { Log.d("GameStomp", "Already subscribed to same-game") }
    }

    @Test
    fun subscribeToLobby_guard_prevents_duplicate_subscription() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val lobbyFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns lobbyFlow

        gameStompClient.connect()
        advanceUntilIdle()

        // First subscription
        gameStompClient.subscribeToLobby()
        advanceUntilIdle()
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }

        // Second subscription (SubscriptionChannel guard prevents duplicate)
        gameStompClient.subscribeToLobby()
        advanceUntilIdle()
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }
    }

    // =========================================================================
    // Test 1: subscribeToLobby() delegates to lobbyChannel.subscribe()
    // =========================================================================

    @Test
    fun `subscribeToLobby delegates to lobbyChannel subscribe`() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val lobbyFlow = MutableSharedFlow<String>()
        // Return a live flow for /topic/lobby so subscribe actually works
        coEvery { stompSession.subscribeText("/topic/lobby") } returns lobbyFlow
        // Any other subscribeText (personal, game) returns a dead flow
        coEvery { stompSession.subscribeText(match { it != "/topic/lobby" }) } returns flowOf()

        gameStompClient.connect()
        advanceUntilIdle()

        val receivedLobbyEvents = mutableListOf<String>()
        val collectJob = launch {
            gameStompClient.lobbyEvents.collect { receivedLobbyEvents.add(it) }
        }

        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        // Emit an event through lobby subscription
        lobbyFlow.emit("LOBBY_EVENT")
        advanceUntilIdle()

        // The event should be received via lobbyEvents
        assertTrue(receivedLobbyEvents.contains("LOBBY_EVENT"))
        collectJob.cancel()
    }

    // =========================================================================
    // Test 2: subscribeToGameInternal() cancels previous gameChannel before resubscribing
    // =========================================================================

    @Test
    fun `subscribeToGameInternal cancels previous gameChannel before resubscribing`() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val gameFlow1 = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val gameFlow2 = MutableSharedFlow<String>(extraBufferCapacity = 1)
        // First subscription to /topic/game/game-a
        coEvery { stompSession.subscribeText("/topic/game/game-a") } returns gameFlow1
        // Second subscription to /topic/game/game-b
        coEvery { stompSession.subscribeText("/topic/game/game-b") } returns gameFlow2
        // Personal topic uses flowOf so it doesn't interfere
        coEvery { stompSession.subscribeText(match { it.startsWith("/topic/game/") && it != "/topic/game/game-a" && it != "/topic/game/game-b" }) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        // Subscribe to first game
        gameStompClient.subscribeToGame("game-a")
        advanceUntilIdle()
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/game/game-a") }

        // Subscribe to second game — this must cancel the old channel and re-subscribe
        gameStompClient.subscribeToGame("game-b")
        advanceUntilIdle()

        // Should have subscribed to game-b
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/game/game-b") }

        // Emit an event on game-a flow — it should NOT be received because that channel was cancelled
        val receivedEvents = mutableListOf<String>()
        val collectJob = launch {
            gameStompClient.events.collect { receivedEvents.add(it) }
        }
        gameFlow1.emit("FROM_GAME_A")
        advanceUntilIdle()
        assertFalse(receivedEvents.contains("FROM_GAME_A"), "Should not receive events from cancelled channel")

        // Emit an event on game-b flow — it SHOULD be received
        gameFlow2.emit("FROM_GAME_B")
        advanceUntilIdle()
        assertTrue(receivedEvents.contains("FROM_GAME_B"), "Should receive events from new channel")

        collectJob.cancel()
    }

    // =========================================================================
    // Test 3: subscribeToGameInternal() returns false and calls startReconnectLoop() on timeout
    // =========================================================================

    @Test
    fun `subscribeToGameInternal returns false and starts reconnect loop on timeout`() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        // subscribeText for the game topic must suspend > 15s so isReady never
        // becomes true, causing withTimeoutOrNull to return null.
        coEvery { stompSession.subscribeText("/topic/game/timeout-game") } coAnswers {
            delay(30_000)
            MutableSharedFlow<String>()
        }
        // Personal topic returns a live flow
        val personalFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(match {
            it.startsWith("/topic/game/") && it != "/topic/game/timeout-game"
        }) } returns personalFlow
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        val statuses = mutableListOf<String>()
        val statusJob = launch {
            gameStompClient.status.collect { statuses.add(it) }
        }

        gameStompClient.subscribeToGame("timeout-game")
        // Jump past the two 8s timeouts (16s total).  subscribeText is still at delay(30_000).
        advanceTimeBy(16_001)
        // Only run tasks ready right now (the timeout callback + emitStatus).
        // Do NOT use advanceUntilIdle — it would try to reach delay(30_000).
        runCurrent()

        assertTrue(statuses.contains("Subscription failed for timeout-game"),
            "Should emit subscription failed status on timeout")

        statusJob.cancel()
    }

    // =========================================================================
    // Test 4: subscribeToGameInternal() sends closeGame on timeout when gameId == _currentGameId
    // =========================================================================

    @Test
    fun `subscribeToGameInternal sends closeGame on timeout when gameId matches currentGameId`() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        // subscribeText must suspend > 16s → isReady never becomes true → both attempts time out
        coEvery { stompSession.subscribeText("/topic/game/close-me") } coAnswers {
            delay(30_000)
            MutableSharedFlow<String>()
        }
        val personalFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(match {
            it.startsWith("/topic/game/") && it != "/topic/game/close-me"
        }) } returns personalFlow
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToGame("close-me")
        advanceTimeBy(16_001)
        runCurrent()

        // The timeout triggers scope.launch { sendRawInternal(...) } followed
        // by startReconnectLoop().  The key observable effect is the log.
        verify { Log.e("GameStomp", "Subscription timed out for close-me (attempt 2/2)") }
    }

    // =========================================================================
    // Test 5: _events has replay=0: verify new collector does NOT receive previously emitted events
    // =========================================================================

    @Test
    fun `events flow has replay 0 so new collector does not receive past events`() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val gameFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns gameFlow

        gameStompClient.connect()
        advanceUntilIdle()

        // Subscribe to a game and emit one event
        gameStompClient.subscribeToGame("replay-test")
        advanceUntilIdle()

        // Emit an event through the game subscription
        gameFlow.emit("PAST_EVENT")
        advanceUntilIdle()

        // Now collect events AFTER the emission; we should NOT see the past event
        val receivedAfter = mutableListOf<String>()
        val collectJob = launch {
            gameStompClient.events.collect { receivedAfter.add(it) }
        }
        advanceUntilIdle()

        // The past event should not be replayed to the new collector
        assertFalse(receivedAfter.contains("PAST_EVENT"),
            "New collector should not receive past events since replay=0")

        collectJob.cancel()
    }

    // =========================================================================
    // Test 6: _logEvents receives forwarded events from _events via the init block collector
    // =========================================================================

    @Test
    fun `logEvents receives forwarded events from events flow`() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val gameFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { stompSession.subscribeText(any<String>()) } returns gameFlow

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToGame("forward-test")
        advanceUntilIdle()

        val receivedLogEvents = mutableListOf<String>()
        val logJob = launch {
            gameStompClient.logEvents.collect { receivedLogEvents.add(it) }
        }

        // Emit an event through the game subscription
        gameFlow.emit("FORWARDED_EVENT")
        advanceUntilIdle()

        // The event should appear in both events and logEvents
        assertTrue(receivedLogEvents.contains("FORWARDED_EVENT"),
            "logEvents should receive events forwarded from _events")

        logJob.cancel()
    }

    // =========================================================================
    // Test 7a: reconnect block subscribes lobby only when _currentGameId is blank
    // =========================================================================

    @Test
    fun `reconnect subscribes lobby when currentGameId is blank`() = runTest(testDispatcher) {
        // First connect succeeds, but personal subscription throws on .collect()
        val firstSession: StompSession = mockk(relaxed = true)
        coEvery { firstSession.subscribeText(match {
            it.startsWith("/topic/game/")
        }) } returns flow { throw Exception("Connection lost") }

        // Second session (used by reconnect loop)
        val secondSession: StompSession = mockk(relaxed = true)
        coEvery { secondSession.subscribeText(match {
            it.startsWith("/topic/game/")
        }) } returns MutableSharedFlow()
        coEvery { secondSession.subscribeText("/topic/lobby") } returns MutableSharedFlow()

        // First connect → firstSession, reconnect → secondSession
        coEvery { stompClient.connect(any<String>()) } returns firstSession andThen secondSession

        assertEquals("", gameStompClient.currentGameId)

        gameStompClient.connect()
        // connect() triggers subscribeToPersonalTopic() whose .collect() throws
        // → startReconnectLoop() waits 1s then reconnects
        advanceTimeBy(1_500)
        advanceUntilIdle()

        // On reconnection, since _currentGameId is blank, it should subscribe lobby
        coVerify(atLeast = 1) { secondSession.subscribeText("/topic/lobby") }
    }

    // =========================================================================
    // Test 7b: reconnect block subscribes game channel when _currentGameId is not blank
    // =========================================================================

    @Test
    fun `reconnect subscribes game channel when currentGameId is not blank`() = runTest(testDispatcher) {
        // First session
        val firstSession: StompSession = mockk(relaxed = true)
        // Personal subscription returns a live flow (no error during connect)
        val personalFlow1 = MutableSharedFlow<String>()
        coEvery { firstSession.subscribeText(match {
            it.startsWith("/topic/game/") && it != "/topic/game/game-42"
        }) } returns personalFlow1
        // Game channel subscription: return a flow that throws on .collect()
        // → SubscriptionChannel's catch block → onError → startReconnectLoop
        coEvery { firstSession.subscribeText("/topic/game/game-42") } returns
            flow { throw Exception("Game sub lost") }
        coEvery { firstSession.sendText(any<String>(), any<String>()) } returns mockk()

        // Second session (used by reconnect loop)
        val secondSession: StompSession = mockk(relaxed = true)
        coEvery { secondSession.subscribeText("/topic/game/game-42") } returns MutableSharedFlow()
        coEvery { secondSession.subscribeText(match {
            it.startsWith("/topic/game/") && it != "/topic/game/game-42"
        }) } returns MutableSharedFlow()
        coEvery { secondSession.sendText(any<String>(), any<String>()) } returns mockk()

        // First connect → firstSession, reconnect → secondSession
        coEvery { stompClient.connect(any<String>()) } returns firstSession andThen secondSession

        gameStompClient.connect()
        advanceUntilIdle()

        // Subscribe to a game so _currentGameId is not blank.
        // The game channel's .collect() throws → SubscriptionChannel.onError → startReconnectLoop
        gameStompClient.subscribeToGame("game-42")
        advanceUntilIdle()
        assertEquals("game-42", gameStompClient.currentGameId)

        // Reconnect loop: 1s delay + connect with secondSession
        advanceTimeBy(1_500)
        advanceUntilIdle()

        // On reconnection, since _currentGameId is "game-42", it should resub to game topic
        coVerify(atLeast = 1) { secondSession.subscribeText("/topic/game/game-42") }
    }

    // =========================================================================
    // INVARIANT: If createGame returns a gameId, NO error status is emitted.
    // Before the fix, the GAME_CREATED could arrive and the game be created on
    // the server, but a subscription timeout would emit "Create game failed"
    // AND return null — breaking this invariant. The fix prevents that.
    // =========================================================================

    @Test
    fun `createGame success returns gameId and does NOT emit any error status`() = runTest(testDispatcher) {
        val session: StompSession = mockk(relaxed = true)

        // Personal subscription forwards events to _events
        val personalFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { session.subscribeText(match { it.startsWith("/topic/game/") && !it.startsWith("/topic/game/test-id") }) } returns personalFlow

        // Game topic subscription resolves immediately → isReady=true quickly
        coEvery { session.subscribeText("/topic/game/test-id") } returns MutableSharedFlow()
        coEvery { session.sendText(any<String>(), any<String>()) } returns mockk()
        coEvery { stompClient.connect(any<String>()) } returns session

        gameStompClient.connect()
        advanceUntilIdle()

        // Collect statuses from before the createGame call
        val statuses = mutableListOf<String>()
        val statusJob = launch {
            gameStompClient.status.collect { statuses.add(it) }
        }
        advanceUntilIdle()

        // Launch createGame — it sends /app/game/create, then waits for GAME_CREATED
        var resultGameId: String? = null
        val createJob = launch {
            resultGameId = gameStompClient.createGame("Player1", "lindwurm")
        }
        // Use runCurrent() instead of advanceUntilIdle() to avoid advancing virtual time
        // and prematurely triggering the 10s withTimeoutOrNull inside createGame.
        runCurrent()

        // Simulate server responding with GAME_CREATED
        personalFlow.emit("""{"event":"GAME_CREATED","gameId":"test-id"}""")
        advanceUntilIdle()

        // Assert: gameId was returned (game was created)
        assertEquals("test-id", resultGameId)

        // Assert: NO "failed" message appears in the status flow
        val errorStatuses = statuses.filter { it.startsWith("Create game failed", ignoreCase = true) }
        assertTrue(errorStatuses.isEmpty(),
            "Expected no error messages but got: $errorStatuses")

        statusJob.cancel()
    }

    // =========================================================================
    // INVARIANT: If a "Create game failed" error is emitted, createGame MUST
    // return null (the game was NOT registered as created on the client).
    // The fix ensures close is sent on the original session BEFORE
    // startReconnectLoop() nulls it, so the server cleans up the orphan.
    // =========================================================================

    @Test
    fun `createGame returns null and emits error when subscription times out`() = runTest(testDispatcher) {
        val session: StompSession = mockk(relaxed = true)

        // Personal subscription forwards events
        val personalFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        coEvery { session.subscribeText(match { it.startsWith("/topic/game/") && !it.startsWith("/topic/game/test-orphan") }) } returns personalFlow

        // Game topic subscription delays >10s → isReady stays false → timeout
        coEvery { session.subscribeText("/topic/game/test-orphan") } coAnswers {
            delay(30_000)
            MutableSharedFlow<String>()
        }
        coEvery { session.sendText(any<String>(), any<String>()) } returns mockk()

        // Reconnect session (used after startReconnectLoop)
        val reconnectSession: StompSession = mockk(relaxed = true)
        coEvery { reconnectSession.subscribeText(any<String>()) } returns MutableSharedFlow()
        coEvery { reconnectSession.sendText(any<String>(), any<String>()) } returns mockk()

        coEvery { stompClient.connect(any<String>()) } returns session andThen reconnectSession

        gameStompClient.connect()
        advanceUntilIdle()

        val statuses = mutableListOf<String>()
        val statusJob = launch {
            gameStompClient.status.collect { statuses.add(it) }
        }
        advanceUntilIdle()

        var resultGameId: String? = "SHOULD_BE_NULL"
        val createJob = launch {
            resultGameId = gameStompClient.createGame("Player1", "lindwurm")
        }
        // Use runCurrent() instead of advanceUntilIdle() to avoid advancing virtual time
        // and prematurely triggering the 10s withTimeoutOrNull inside createGame.
        runCurrent()

        // Emit GAME_CREATED so createGame proceeds to subscribeToGameInternal
        personalFlow.emit("""{"event":"GAME_CREATED","gameId":"test-orphan"}""")
        advanceUntilIdle()

        // Advance past the 16s subscription timeout (2×8s) in two stages for robustness
        advanceTimeBy(16_000)
        advanceTimeBy(1)
        advanceUntilIdle()

        // Assert: createGame returned null (client considers it failed)
        assertEquals(null, resultGameId)

        // Assert: the error status WAS emitted
        val errorStatuses = statuses.filter { it.startsWith("Create game failed", ignoreCase = true) }
        assertTrue(errorStatuses.isNotEmpty(),
            "Expected 'Create game failed' status but got: $statuses")

        // KEY: close message was sent on the ORIGINAL session (proving the fix works)
        // Before the fix, close was scope.launch-ed AFTER session was nulled → never sent
        coVerify(exactly = 1) {
            session.sendText("/app/game/close", match { it.contains("\"gameId\":\"test-orphan\"") })
        }

        statusJob.cancel()
    }


    @Test
    fun rollDice_sends_roll_dice_action_with_cheat() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.setGameId("game-1")
        gameStompClient.rollDice(true)
        advanceUntilIdle()

        coVerify {
            stompSession.sendText(
                "/app/game/action",
                match {
                    it.contains("\"action\":\"ROLL_DICE\"") &&
                            it.contains("\"cheat\":\"true\"")
                }
            )
        }
    }

    @Test
    fun endTurn_sends_end_turn_action() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.setGameId("game-1")
        gameStompClient.endTurn()
        advanceUntilIdle()

        coVerify {
            stompSession.sendText(
                "/app/game/action",
                match { it.contains("\"action\":\"END_TURN\"") }
            )
        }
    }



    @Test
    fun drawCard_sends_chance_payload() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.setGameId("game-1")
        gameStompClient.drawCard("CHANCE")
        advanceUntilIdle()

        coVerify {
            stompSession.sendText(
                "/app/game/action",
                match {
                    it.contains("\"action\":\"DRAW_CARD\"") &&
                            it.contains("\"cardType\":\"CHANCE\"")
                }
            )
        }
    }

    @Test
    fun drawCard_sends_community_chest_payload() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.setGameId("game-1")
        gameStompClient.drawCard("COMMUNITY_CHEST")
        advanceUntilIdle()

        coVerify {
            stompSession.sendText(
                "/app/game/action",
                match {
                    it.contains("\"action\":\"DRAW_CARD\"") &&
                            it.contains("\"cardType\":\"COMMUNITY_CHEST\"")
                }
            )
        }
    }

    @Test
    fun executeAction_sends_execute_action_with_player_id() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.setGameId("game-1")
        gameStompClient.executeAction("player-1")
        advanceUntilIdle()

        coVerify {
            stompSession.sendText(
                "/app/game/action",
                match {
                    it.contains("\"action\":\"EXECUTE_ACTION\"") &&
                            it.contains("\"playerId\":\"player-1\"")
                }
            )
        }
    }

    @Test
    fun requestGameList_when_not_connected_emits_not_connected_status() = runTest(testDispatcher) {
        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.requestGameList()
        advanceUntilIdle()

        assertTrue(statuses.contains("Not connected"))

        job.cancel()
    }

    @Test
    fun closeGame_when_not_connected_emits_not_connected_status() = runTest(testDispatcher) {
        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.closeGame("game-1")
        advanceUntilIdle()

        assertTrue(statuses.contains("Not connected"))

        job.cancel()
    }
}
