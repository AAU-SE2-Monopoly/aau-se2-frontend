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
import org.json.JSONArray
import org.json.JSONObject
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


        mockkConstructor(JSONObject::class)

        val jsonMock = mockk<JSONObject>(relaxed = true)

        every { anyConstructed<JSONObject>().put(any<String>(), any<Any>()) } returns jsonMock
        every { anyConstructed<JSONObject>().toString() } returns "{}"

        every { jsonMock.put(any<String>(), any<Any>()) } returns jsonMock
        every { jsonMock.toString() } returns "{}"

        mockkConstructor(JSONArray::class)
        val arrayMock = mockk<JSONArray>(relaxed = true)
        every { anyConstructed<JSONArray>().put(any<Any>()) } returns arrayMock
        every { arrayMock.put(any<Any>()) } returns arrayMock

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
    fun connect_success() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession

        gameStompClient.connect()
        advanceUntilIdle()

        coVerify { stompClient.connect(any<String>()) }
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

        coEvery { stompSession.subscribeText(any<String>()) } throws CancellationException("Cancelled by test")

        gameStompClient.connect()
        advanceUntilIdle()

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
        // subscribeText is a suspend extension function
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

        assertEquals("Test Message", receivedEvents.first())
        collectJob.cancel()
    }

    @Test
    fun subscribeToGame_already_subscribed() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        // Return a flow that doesn't complete so the job stays active
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
        coEvery { stompSession.subscribeText(any<String>()) } throws Exception("Subscribe Error")

        val statuses = mutableListOf<String>()
        val job = launch { gameStompClient.status.collect { statuses.add(it) } }

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.subscribeToGame("test-id")
        advanceUntilIdle()

        verify { Log.e("GameStomp", "subscription error", any()) }
        assertTrue(statuses.contains("Subscription error: Subscribe Error"))
        job.cancel()
    }

    @Test
    fun createGame_calls_subscribe_and_send() = runTest(testDispatcher) {
        coEvery { stompClient.connect(any<String>()) } returns stompSession
        coEvery { stompSession.subscribeText(any<String>()) } returns flowOf()
        coEvery { stompSession.sendText(any<String>(), any<String>()) } returns mockk()

        gameStompClient.connect()
        advanceUntilIdle()

        gameStompClient.createGame("Player1")
        advanceUntilIdle()

        coVerify { stompSession.sendText(any<String>(), any<String>()) }
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
        //  e is IllegalStateException && e.message "cancelled" (True)
        coEvery { stompClient.connect(any<String>()) } throws IllegalStateException("Job was CANCELLED by system")

        gameStompClient.connect()
        advanceUntilIdle()


        verify { Log.d("GameStomp", "Connection attempt cancelled") }
    }

    @Test
    fun connect_cancellation_via_cause() = runTest(testDispatcher) {
        //: e.cause is CancellationException (True)
        val cause = CancellationException("Internal coroutine cancellation")
        coEvery { stompClient.connect(any<String>()) } throws Exception("Wrapper Exception", cause)

        gameStompClient.connect()
        advanceUntilIdle()

        verify { Log.d("GameStomp", "Connection attempt cancelled") }
    }

    @Test
    fun connect_error_illegal_state_other_message() = runTest(testDispatcher) {
        //  e is IllegalStateException && message not "cancelled" (False)
        coEvery { stompClient.connect(any<String>()) } throws IllegalStateException("Invalid state detected")

        gameStompClient.connect()
        advanceUntilIdle()


        verify { Log.e("GameStomp", "connect error", any()) }
    }

    @Test
    fun connect_error_illegal_state_null_message() = runTest(testDispatcher) {
        //e is IllegalStateException && message is NULL
        val exceptionWithNullMessage = IllegalStateException(null as String?)
        coEvery { stompClient.connect(any<String>()) } throws exceptionWithNullMessage

        gameStompClient.connect()
        advanceUntilIdle()

        verify { Log.e("GameStomp", "connect error", any()) }
    }
}