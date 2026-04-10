package at.aau.monopoly.klagenfurt

import MyStompManager
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyStompManagerTest {

    private lateinit var mockStompClient: StompClient
    private lateinit var mockSession: StompSession
    private lateinit var stompManager: MyStompManager
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        mockStompClient = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Krossbow Extensions mocken
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        // WICHTIG: Rückgabewert für sendText definieren (vermeidet lautloses Abbrechen)
        coEvery { mockSession.sendText(any(), any()) } returns mockk()

        mockkConstructor(JSONObject::class)
        every { anyConstructed<JSONObject>().put(any<String>(), any<Any>()) } answers { it.invocation.self as JSONObject }
        every { anyConstructed<JSONObject>().toString() } returns "{}"
        every { anyConstructed<JSONObject>().optString(any<String>(), any<String>()) } returns "Parsed JSON Message"

        coEvery { mockStompClient.connect(any(), any()) } returns mockSession

        stompManager = MyStompManager(mockStompClient, TestScope(testDispatcher))
    }

    @AfterEach
    fun teardown() {
        // Explizites Aufräumen verhindert "Datei gesperrt" Fehler
        unmockkStatic(Log::class)
        unmockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        unmockkAll()
        clearAllMocks()
    }

    // --- VERBINDUNG ---

    @Test
    fun connect_successful_emitsConnected() = runTest(testDispatcher) {
        val responses = mutableListOf<String>()
        val job = launch { stompManager.responses.collect { responses.add(it) } }

        stompManager.connect()
        advanceUntilIdle()

        coVerify { mockStompClient.connect(any()) }
        assertTrue(responses.contains("connected"))
        job.cancel()
    }

    @Test
    fun connect_failure_emitsConnectionError() = runTest(testDispatcher) {
        coEvery { mockStompClient.connect(any()) } throws Exception("Network Error")

        val responses = mutableListOf<String>()
        val job = launch { stompManager.responses.collect { responses.add(it) } }

        stompManager.connect()
        advanceUntilIdle()

        assertTrue(responses.contains("Connection error"))
        job.cancel()
    }

    // --- EMPFANG (SUBSCRIPTIONS) ---
    @Test
    fun connect_calledTwice_onlyConnectsOnce() = runTest(testDispatcher) {
        stompManager.connect()
        advanceUntilIdle()

        stompManager.connect()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockStompClient.connect(any()) }
    }
    @Test
    fun connect_receivesTopicMessage_emitsMessage() = runTest(testDispatcher) {
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")

        val expectedMessage = "Hello from Server"

        // Wir mocken spezifisch für das Hello-Topic
        coEvery {
            mockSession.subscribeText("/topic/hello-response")
        } returns flowOf(expectedMessage)

        // Wir mocken auch das andere Topic (mit leerem Flow),
        // damit MockK nicht meckert oder hängen bleibt
        coEvery {
            mockSession.subscribeText("/topic/rcv-object")
        } returns emptyFlow()

        val received = mutableListOf<String>()
        val collectJob = launch {
            stompManager.responses.collect { received.add(it) }
        }

        stompManager.connect()
        advanceUntilIdle()

        // Jetzt prüfen wir nur, ob unsere Nachricht in der Liste gelandet ist
        assertTrue(received.contains(expectedMessage), "Nachricht nicht gefunden. Empfangen: $received")

        collectJob.cancel()
    }

    // --- SENDEN (HELLO) ---

    @Test
    fun connect_receivesJsonMessage_emitsParsedText() = runTest(testDispatcher) {
        coEvery { mockSession.subscribeText("/topic/hello-response") } returns emptyFlow()
        coEvery { mockSession.subscribeText("/topic/rcv-object") } returns flowOf("""{"text": "Parsed JSON Message"}""")

        val responses = mutableListOf<String>()
        val job = launch { stompManager.responses.collect { responses.add(it) } }

        stompManager.connect()
        advanceUntilIdle()

        assertTrue(responses.contains("Parsed JSON Message"))

        job.cancel()
    }
    @Test
    fun sendHello_whenNotConnected_emitsError() = runTest(testDispatcher) {
        val responses = mutableListOf<String>()
        val job = launch { stompManager.responses.collect { responses.add(it) } }

        stompManager.sendHello()
        advanceUntilIdle()

        assertTrue(responses.contains("Error: Not connected"))

        job.cancel()
    }
    @Test
    fun sendHello_whenConnected_sendsText() = runTest(testDispatcher) {
        stompManager.connect()
        advanceUntilIdle()

        stompManager.sendHello()
        advanceUntilIdle()

        coVerify { mockSession.sendText(eq("/app/hello"), any()) }
    }

    @Test
    fun sendHello_throwsException_logsError() = runTest(testDispatcher) {
        stompManager.connect()
        advanceUntilIdle()

        coEvery { mockSession.sendText(any(), any()) } throws RuntimeException("Network crash")

        stompManager.sendHello()
        advanceUntilIdle()

        verify { Log.e("MyStompManager", "Send failed", any()) }
    }

    // --- SENDEN (JSON) ---
    @Test
    fun sendJson_whenNotConnected_emitsError() = runTest(testDispatcher) {
        val responses = mutableListOf<String>()
        val job = launch { stompManager.responses.collect { responses.add(it) } }

        stompManager.sendJson()
        advanceUntilIdle()

        assertTrue(responses.contains("Error: Not connected"))

        job.cancel()
    }
    @Test
    fun sendJson_whenConnected_sendsJsonText() = runTest(testDispatcher) {
        stompManager.connect()
        advanceUntilIdle()

        stompManager.sendJson()
        advanceUntilIdle()

        coVerify { mockSession.sendText(eq("/app/object"), any()) }
    }

    @Test
    fun sendJson_throwsException_logsError() = runTest(testDispatcher) {
        stompManager.connect()
        advanceUntilIdle()

        coEvery { mockSession.sendText(eq("/app/object"), any()) } throws RuntimeException("JSON crash")

        stompManager.sendJson()
        advanceUntilIdle()

        verify { Log.e("MyStompManager", "Send JSON failed", any()) }
    }
}