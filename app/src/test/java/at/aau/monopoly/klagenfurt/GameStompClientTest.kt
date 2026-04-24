package at.aau.monopoly.klagenfurt

import android.util.Log
import at.aau.monopoly.klagenfurt.networking.GameStompClient
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
        advanceUntilIdle()

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
        advanceUntilIdle()

        coVerify { stompClient.connect(any<String>()) }
        // Verify that we automatically subscribe to our personal topic upon connection
        coVerify { stompSession.subscribeText(match { it.startsWith("/topic/game/") }) }
        verify { Log.d("GameStomp", "Connected successfully") }
    }

    @Test
    fun test_subscribeToGame_when_not_connected() = runTest(testDispatcher) {
        gameStompClient.subscribeToGame("some-id")
        advanceUntilIdle()

        verify { Log.w("GameStomp", "Cannot subscribe: not connected") }
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

        // Mock a specific cancellation for the SECOND subscription
        coEvery { stompSession.subscribeText("/topic/game/test-id") } throws CancellationException("Cancelled by test")

        gameStompClient.subscribeToGame("test-id")
        advanceUntilIdle()

        verify { Log.d("GameStomp", "Subscription job cancelled for test-id") }
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
        val eventFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(any<String>()) } returns eventFlow

        val receivedEvents = mutableListOf<String>()
        val collectJob = launch {
            gameStompClient.events.collect { receivedEvents.add(it) }
        }

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToGame("test-id")
        advanceUntilIdle()

        coVerify { stompSession.subscribeText("/topic/game/test-id") }

        eventFlow.emit("Test Message")
        advanceUntilIdle()

        assertEquals("Test Message", receivedEvents.last())
        collectJob.cancel()
    }

    @Test
    fun subscribeToGame_already_subscribed() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val activeFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(any<String>()) } returns activeFlow

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToGame("test-id")
        advanceUntilIdle()

        gameStompClient.subscribeToGame("test-id")
        advanceUntilIdle()

        verify { Log.d("GameStomp", "Already subscribed to test-id") }
    }

    @Test
    fun subscribeToGame_error() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        
        gameStompClient.connect()
        advanceUntilIdle()

        coEvery { stompSession.subscribeText("/topic/game/test-id") } throws Exception("Subscribe Error")

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.subscribeToGame("test-id")
        advanceUntilIdle()

        verify { Log.e("GameStomp", "subscription error", any()) }
        assertTrue(statuses.contains("Subscription error: Subscribe Error"))
        job.cancel()
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
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.joinGame("game-123", "Alice", "gti")
        advanceUntilIdle()

        coVerifyOrder {
            stompSession.subscribeText("/topic/game/game-123")
            stompSession.sendText("/app/game/state", any<String>())
            stompSession.sendText("/app/game/join", any<String>())
        }
    }

    @Test
    fun subscribeToLobby_when_not_connected_logs_warning() = runTest(testDispatcher) {
        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        verify { Log.w("GameStomp", "Cannot subscribe to lobby: not connected") }
    }

    @Test
    fun subscribeToLobby_success_emits_lobby_events() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val lobbyFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns lobbyFlow

        val receivedLobbyEvents = mutableListOf<String>()
        val collectJob = launch { gameStompClient.lobbyEvents.collect { receivedLobbyEvents.add(it) } }

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        coVerify { stompSession.subscribeText("/topic/lobby") }
        verify { Log.d("GameStomp", "Subscribing to /topic/lobby") }

        lobbyFlow.emit("LOBBY_UPDATE")
        advanceUntilIdle()

        assertEquals("LOBBY_UPDATE", receivedLobbyEvents.last())
        collectJob.cancel()
    }

    @Test
    fun subscribeToLobby_already_subscribed_logs_and_returns() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        val neverEndingLobbyFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } returns neverEndingLobbyFlow

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        verify { Log.d("GameStomp", "Already subscribed to lobby") }
    }

    @Test
    fun subscribeToLobby_error_emits_status_message() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } throws Exception("Lobby failed")

        val statuses = mutableListOf<String>()
        val collectJob = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        verify { Log.e("GameStomp", "lobby subscription error", any()) }
        assertTrue(statuses.contains("Lobby subscription error: Lobby failed"))
        collectJob.cancel()
    }

    @Test
    fun subscribeToLobby_cancellation_logs_cancelled() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.subscribeText("/topic/lobby") } throws CancellationException("cancel")

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToLobby()
        advanceUntilIdle()

        verify { Log.d("GameStomp", "Lobby subscription cancelled") }
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
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.startGame()
        gameStompClient.rollDice()
        gameStompClient.endTurn()
        gameStompClient.requestState()
        gameStompClient.joinGame("id", "name")
        gameStompClient.setGameId("new-id")

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
}
