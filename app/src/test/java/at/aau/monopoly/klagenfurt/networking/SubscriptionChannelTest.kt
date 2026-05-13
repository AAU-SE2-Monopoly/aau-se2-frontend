package at.aau.monopoly.klagenfurt.networking

import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionChannelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    private lateinit var stompSession: StompSession
    private lateinit var events: MutableSharedFlow<String>
    private lateinit var channel: SubscriptionChannel
    private val baseTopic = "/topic/game/"

    @BeforeEach
    fun setup() {
        stompSession = mockk(relaxed = true)
        events = MutableSharedFlow()

        // Need to mock the extension function subscribeText
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun createChannel(
        sessionProvider: () -> StompSession? = { stompSession },
        onError: ((Throwable) -> Unit)? = null
    ): SubscriptionChannel {
        return SubscriptionChannel(
            scope = testScope,
            session = sessionProvider,
            baseTopic = baseTopic,
            events = events,
            onError = onError
        )
    }

    /** Returns a never-ending flow that keeps the subscription coroutine alive. */
    private fun neverEndingFlow(): MutableSharedFlow<String> = MutableSharedFlow()

    // =========================================================================
    // subscribe() sets isReady to true after successful subscription
    // =========================================================================

    @Test
    fun `subscribe without suffix sets isReady to true after successful subscription`() = runTest(testDispatcher) {
        val sessionFlow = neverEndingFlow()
        coEvery { stompSession.subscribeText(baseTopic) } returns sessionFlow

        channel = createChannel()
        assertFalse(channel.isReady.value)

        channel.subscribe()
        advanceUntilIdle()

        assertTrue(channel.isReady.value)
        coVerify(exactly = 1) { stompSession.subscribeText(baseTopic) }
    }

    @Test
    fun `subscribe with topicSuffix sets isReady to true after successful subscription`() = runTest(testDispatcher) {
        val suffix = "game-123"
        val fullTopic = "$baseTopic$suffix"
        val sessionFlow = neverEndingFlow()
        coEvery { stompSession.subscribeText(fullTopic) } returns sessionFlow

        channel = createChannel()
        assertFalse(channel.isReady.value)

        channel.subscribe(suffix)
        advanceUntilIdle()

        assertTrue(channel.isReady.value)
        coVerify(exactly = 1) { stompSession.subscribeText(fullTopic) }
    }

    // =========================================================================
    // subscribe() sets isReady to false in the finally block after cancellation
    // =========================================================================

    @Test
    fun `subscribe sets isReady to false in finally block after cancellation`() = runTest(testDispatcher) {
        val neverEndingFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(baseTopic) } returns neverEndingFlow

        channel = createChannel()
        channel.subscribe()
        advanceUntilIdle()

        // Verify we are ready
        assertTrue(channel.isReady.value)

        // Now cancel the channel's internal job — this triggers cancellation
        // of the coroutine, which enters the finally block and sets isReady = false
        channel.cancel()
        advanceUntilIdle()

        assertFalse(channel.isReady.value)
    }

    @Test
    fun `subscribe sets isReady to false when session returns null`() = runTest(testDispatcher) {
        channel = createChannel(sessionProvider = { null })

        channel.subscribe()
        advanceUntilIdle()

        // When session is null, subscribeText is never called and the coroutine
        // returns early without setting _isReady to true. The finally block runs
        // but _isReady is still false.
        assertFalse(channel.isReady.value)
    }

    // =========================================================================
    // subscribe() is a no-op if job is already active (guard check)
    // =========================================================================

    @Test
    fun `subscribe is a no-op if job is already active`() = runTest(testDispatcher) {
        val neverEndingFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(baseTopic) } returns neverEndingFlow

        channel = createChannel()
        channel.subscribe()
        advanceUntilIdle()

        // First call succeeded and isReady is true
        assertTrue(channel.isReady.value)

        // Second subscribe should be a no-op due to guard
        channel.subscribe()
        advanceUntilIdle()

        // Should still be ready (from first call), and subscribeText should NOT have been called again
        assertTrue(channel.isReady.value)
        coVerify(exactly = 1) { stompSession.subscribeText(baseTopic) }
    }

    // =========================================================================
    // cancel() sets isReady to false and nulls the job
    // =========================================================================

    @Test
    fun `cancel sets isReady to false and nulls the job`() = runTest(testDispatcher) {
        val neverEndingFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(baseTopic) } returns neverEndingFlow

        channel = createChannel()
        channel.subscribe()
        advanceUntilIdle()

        assertTrue(channel.isReady.value)

        channel.cancel()
        advanceUntilIdle()

        assertFalse(channel.isReady.value)

        // Calling cancel again should be safe (idempotent)
        channel.cancel()
        assertFalse(channel.isReady.value)
    }

    @Test
    fun `cancel on already cancelled channel is safe`() = runTest(testDispatcher) {
        channel = createChannel()
        // Channel was never subscribed, so cancel on null job
        channel.cancel()
        assertFalse(channel.isReady.value)

        // Should be safe to call again
        channel.cancel()
        assertFalse(channel.isReady.value)
    }

    // =========================================================================
    // onError callback is invoked on non-cancellation exceptions
    // =========================================================================

    @Test
    fun `onError is invoked on non-cancellation exceptions`() = runTest(testDispatcher) {
        val errorMessage = "Subscribe failed"
        coEvery { stompSession.subscribeText(baseTopic) } throws Exception(errorMessage)

        val capturedErrors = mutableListOf<Throwable>()
        channel = createChannel(onError = { capturedErrors.add(it) })

        channel.subscribe()
        advanceUntilIdle()

        assertEquals(1, capturedErrors.size)
        assertEquals(errorMessage, capturedErrors.first().message)
    }

    @Test
    fun `onError is invoked on IllegalStateException without cancellation message`() = runTest(testDispatcher) {
        coEvery { stompSession.subscribeText(baseTopic) } throws IllegalStateException("Some other illegal state")

        val capturedErrors = mutableListOf<Throwable>()
        channel = createChannel(onError = { capturedErrors.add(it) })

        channel.subscribe()
        advanceUntilIdle()

        assertEquals(1, capturedErrors.size)
    }

    @Test
    fun `onError is invoked on exception with non-cancellation cause`() = runTest(testDispatcher) {
        val cause = RuntimeException("Underlying cause")
        coEvery { stompSession.subscribeText(baseTopic) } throws Exception("Wrapper", cause)

        val capturedErrors = mutableListOf<Throwable>()
        channel = createChannel(onError = { capturedErrors.add(it) })

        channel.subscribe()
        advanceUntilIdle()

        assertEquals(1, capturedErrors.size)
    }

    // =========================================================================
    // onError is NOT invoked on CancellationException
    // =========================================================================

    @Test
    fun `onError is NOT invoked on CancellationException`() = runTest(testDispatcher) {
        coEvery { stompSession.subscribeText(baseTopic) } throws CancellationException("Intentional cancel")

        var onErrorCalled = false
        channel = createChannel(onError = { onErrorCalled = true })

        channel.subscribe()
        advanceUntilIdle()

        assertFalse(onErrorCalled)
    }

    @Test
    fun `onError is NOT invoked on IllegalStateException with cancelled message`() = runTest(testDispatcher) {
        coEvery { stompSession.subscribeText(baseTopic) } throws IllegalStateException("Job was CANCELLED by system")

        var onErrorCalled = false
        channel = createChannel(onError = { onErrorCalled = true })

        channel.subscribe()
        advanceUntilIdle()

        assertFalse(onErrorCalled)
    }

    @Test
    fun `onError is NOT invoked on exception with CancellationException cause`() = runTest(testDispatcher) {
        val cancelCause = CancellationException("Internal cancel")
        coEvery { stompSession.subscribeText(baseTopic) } throws Exception("Wrapper", cancelCause)

        var onErrorCalled = false
        channel = createChannel(onError = { onErrorCalled = true })

        channel.subscribe()
        advanceUntilIdle()

        assertFalse(onErrorCalled)
    }

    @Test
    fun `onError is NOT invoked on IllegalStateException with cancelled in any case`() = runTest(testDispatcher) {
        coEvery { stompSession.subscribeText(baseTopic) } throws IllegalStateException("Operation cancelled by user")

        var onErrorCalled = false
        channel = createChannel(onError = { onErrorCalled = true })

        channel.subscribe()
        advanceUntilIdle()

        assertFalse(onErrorCalled)
    }

    // =========================================================================
    // subscribe() with topicSuffix produces correct full topic string
    // =========================================================================

    @Test
    fun `subscribe with topicSuffix produces correct full topic string`() = runTest(testDispatcher) {
        coEvery { stompSession.subscribeText(any<String>()) } returns emptyFlow()

        channel = createChannel()

        channel.subscribe("abc-123")
        advanceUntilIdle()

        coVerify(exactly = 1) { stompSession.subscribeText("/topic/game/abc-123") }
    }

    @Test
    fun `subscribe without topicSuffix uses baseTopic as full topic`() = runTest(testDispatcher) {
        val customBase = "/topic/lobby"
        coEvery { stompSession.subscribeText(any<String>()) } returns emptyFlow()

        channel = SubscriptionChannel(
            scope = testScope,
            session = { stompSession },
            baseTopic = customBase,
            events = events
        )

        channel.subscribe()
        advanceUntilIdle()

        coVerify(exactly = 1) { stompSession.subscribeText("/topic/lobby") }
    }

    // =========================================================================
    // Additional behavioral tests
    // =========================================================================

    @Test
    fun `subscribe emits events collected from the session subscription`() = runTest(testDispatcher) {
        val sessionFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(baseTopic) } returns sessionFlow

        channel = createChannel()

        val receivedEvents = mutableListOf<String>()
        val collectJob = testScope.launch {
            events.collect { receivedEvents.add(it) }
        }

        channel.subscribe()
        advanceUntilIdle()

        assertTrue(channel.isReady.value)

        sessionFlow.emit("event-1")
        sessionFlow.emit("event-2")
        advanceUntilIdle()

        assertEquals(listOf("event-1", "event-2"), receivedEvents)
        collectJob.cancel()
    }

    @Test
    fun `multiple cancels and re-subscribes work correctly`() = runTest(testDispatcher) {
        val flow1 = MutableSharedFlow<String>()
        val flow2 = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText("$baseTopic/one") } returns flow1
        coEvery { stompSession.subscribeText("$baseTopic/two") } returns flow2

        channel = createChannel()

        // First subscribe
        channel.subscribe("/one")
        advanceUntilIdle()
        assertTrue(channel.isReady.value)
        coVerify(exactly = 1) { stompSession.subscribeText("$baseTopic/one") }

        // Cancel
        channel.cancel()
        advanceUntilIdle()
        assertFalse(channel.isReady.value)

        // Re-subscribe to different topic
        channel.subscribe("/two")
        advanceUntilIdle()
        assertTrue(channel.isReady.value)
        coVerify(exactly = 1) { stompSession.subscribeText("$baseTopic/two") }
    }

    @Test
    fun `subscribeText is not called when session is null`() = runTest(testDispatcher) {
        channel = createChannel(sessionProvider = { null })

        channel.subscribe("game-42")
        advanceUntilIdle()

        coVerify(exactly = 0) { stompSession.subscribeText(any<String>()) }
        assertFalse(channel.isReady.value)
    }

    @Test
    fun `finally block runs and sets isReady false when subscription flow throws`() = runTest(testDispatcher) {
        val throwingFlow = mockk<Flow<String>>()
        // When we collect, throw
        coEvery { throwingFlow.collect(any()) } throws Exception("Collection failed")

        coEvery { stompSession.subscribeText(baseTopic) } returns throwingFlow

        val capturedErrors = mutableListOf<Throwable>()
        channel = createChannel(onError = { capturedErrors.add(it) })

        channel.subscribe()
        advanceUntilIdle()

        // isReady should be false because finally set it to false
        assertFalse(channel.isReady.value)

        // onError should have been invoked
        assertEquals(1, capturedErrors.size)
        assertEquals("Collection failed", capturedErrors.first().message)
    }

    @Test
    fun `subscribe resubscribes when topic changes while active`() = runTest(testDispatcher) {
        val activeFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(any<String>()) } returns activeFlow

        channel = createChannel()

        channel.subscribe("topic-a")
        advanceUntilIdle()
        assertTrue(channel.isReady.value)

        // Switching topics should cancel and re-subscribe.
        channel.subscribe("topic-b")
        advanceUntilIdle()

        coVerify(exactly = 1) { stompSession.subscribeText("/topic/game/topic-a") }
        coVerify(exactly = 1) { stompSession.subscribeText("/topic/game/topic-b") }
        assertTrue(channel.isReady.value)
    }

    @Test
    fun `cancelling previous subscription does not clear new readiness`() = runTest(testDispatcher) {
        val slowCancelFlow = flow {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { delay(100) }
            }
        }
        val newFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText("${baseTopic}slow") } returns slowCancelFlow
        coEvery { stompSession.subscribeText("${baseTopic}new") } returns newFlow

        channel = createChannel()

        channel.subscribe("slow")
        runCurrent()
        assertTrue(channel.isReady.value)

        channel.subscribe("new")
        runCurrent()
        assertTrue(channel.isReady.value)

        advanceTimeBy(150)
        runCurrent()

        assertTrue(channel.isReady.value)
    }

    @Test
    fun `events emitted after cancel are not lost by the shared flow`() = runTest(testDispatcher) {
        val sessionFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(baseTopic) } returns sessionFlow

        channel = createChannel()

        val receivedEvents = mutableListOf<String>()
        val collectJob = testScope.launch {
            events.collect { receivedEvents.add(it) }
        }

        channel.subscribe()
        advanceUntilIdle()

        sessionFlow.emit("msg-1")
        advanceUntilIdle()
        assertTrue(receivedEvents.contains("msg-1"))

        channel.cancel()
        advanceUntilIdle()

        // After cancel, re-subscribe with a new flow
        val newFlow = MutableSharedFlow<String>()
        coEvery { stompSession.subscribeText(baseTopic) } returns newFlow
        channel.subscribe()
        advanceUntilIdle()

        newFlow.emit("msg-2")
        advanceUntilIdle()
        assertTrue(receivedEvents.contains("msg-2"))

        collectJob.cancel()
    }
}
