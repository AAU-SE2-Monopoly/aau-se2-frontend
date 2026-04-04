package at.aau.serg.websocketbrokerdemo

import MyStompManager
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.sendText
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyStompManagerTest {

    private lateinit var mockStompClient: StompClient
    private lateinit var mockSession: StompSession
    private lateinit var stompManager: MyStompManager

    @BeforeEach
    fun setup() {
        // relaxed = true verhindert Abstürze bei nicht explizit gemockten Methoden
        mockStompClient = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)

        // Android Log statisch mocken, damit Log.e im JVM-Test nicht crasht
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Mocking JSONObject to avoid "Method put in org.json.JSONObject not mocked"
        mockkConstructor(JSONObject::class)
        val jsonMock = mockk<JSONObject>(relaxed = true)

        // When any JSONObject is constructed, make sure its methods behave
        // For 'put' we return the mock to support chaining
        every { anyConstructed<JSONObject>().put(any<String>(), any<Any>()) } returns jsonMock
        every { anyConstructed<JSONObject>().toString() } returns "{}"
        every {
            anyConstructed<JSONObject>().optString(
                any<String>(),
                any<String>()
            )
        } returns "Parsed JSON Message"

        // Also mock the controlled mock's behavior
        every { jsonMock.put(any<String>(), any<Any>()) } returns jsonMock
        every { jsonMock.toString() } returns "{}"
        every { jsonMock.optString(any<String>(), any<String>()) } returns "Parsed JSON Message"

        coEvery { mockStompClient.connect(any(), any()) } returns mockSession

        stompManager = MyStompManager(mockStompClient)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    // --- GRUNDLEGENDE TESTS (VERBINDUNG) ---

    @Test
    fun connect_successful_emitsConnected() = runTest {
        val deferred = async(UnconfinedTestDispatcher(testScheduler)) {
            stompManager.responses.first()
        }

        stompManager.connect()

        coVerify(timeout = 2000) { mockStompClient.connect("ws://10.0.2.2:8080/websocket-example-broker") }
        assertEquals("connected", deferred.await())
    }

    @Test
    fun connect_failure_emitsConnectionError() = runTest {
        coEvery { mockStompClient.connect(any(), any()) } throws Exception("Network Error")

        val deferred = async(UnconfinedTestDispatcher(testScheduler)) {
            stompManager.responses.first()
        }

        stompManager.connect()

        assertEquals("Connection error", deferred.await())
    }

    @Test
    fun connect_calledTwice_onlyConnectsOnce() = runTest {
        stompManager.connect()
        coVerify(timeout = 2000) { mockStompClient.connect(any(), any()) }

        // Zweiter Aufruf sollte durch "if (session != null) return" sofort abbrechen
        stompManager.connect()

        // Prüfen, dass der Client wirklich nur exakt 1x aufgerufen wurde
        coVerify(exactly = 1) { mockStompClient.connect(any(), any()) }
    }

    // --- EMPFANGS-TESTS (SUBSCRIPTIONS) ---

    @Test
    fun connect_receivesTopicMessage_emitsMessage() = runTest {
        // Wir fangen das echte "subscribe" ab und simulieren einen echten Krossbow-Frame
        coEvery { mockSession.subscribe(any()) } answers {
            val topicArg = firstArg<Any>().toString()
            val mockFrame = mockk<StompFrame.Message>(relaxed = true)

            if (topicArg.contains("hello-response")) {
                every { mockFrame.bodyAsText } returns "Hello from Server"
                flowOf(mockFrame)
            } else {
                emptyFlow()
            }
        }

        val deferred = async(UnconfinedTestDispatcher(testScheduler)) {
            // Wir filtern das initiale "connected" heraus und warten auf die Server-Nachricht
            stompManager.responses.filter { it != "connected" }.first()
        }

        stompManager.connect()

        assertEquals("Hello from Server", deferred.await())
    }

    @Test
    fun connect_receivesJsonMessage_emitsParsedText() = runTest {
        coEvery { mockSession.subscribe(any()) } answers {
            val topicArg = firstArg<Any>().toString()
            val mockFrame = mockk<StompFrame.Message>(relaxed = true)

            if (topicArg.contains("rcv-object")) {
                every { mockFrame.bodyAsText } returns """{"text": "Parsed JSON Message"}"""
                flowOf(mockFrame)
            } else {
                emptyFlow()
            }
        }

        val deferred = async(UnconfinedTestDispatcher(testScheduler)) {
            stompManager.responses.filter { it != "connected" }.first()
        }

        stompManager.connect()

        assertEquals("Parsed JSON Message", deferred.await())
    }

    // --- SENDE-TESTS (HELLO) ---

    @Test
    fun sendHello_whenNotConnected_emitsError() = runTest {
        val deferred = async(UnconfinedTestDispatcher(testScheduler)) {
            stompManager.responses.first()
        }

        stompManager.sendHello()

        assertEquals("Error: Not connected", deferred.await())
    }

    @Test
    fun sendHello_whenConnected_sendsText() = runTest {
        stompManager.connect()
        coVerify(timeout = 2000) { mockStompClient.connect(any(), any()) }

        stompManager.sendHello()

        coVerify(timeout = 2000) { mockSession.send(headers = any(), body = any()) }
    }

    @Test
    fun sendHello_throwsException_logsError() = runTest {
        stompManager.connect()
        coVerify(timeout = 2000) { mockStompClient.connect(any(), any()) }

        // Wir zwingen die Send-Methode zum Absturz
        coEvery {
            mockSession.send(
                headers = any(),
                body = any()
            )
        } throws RuntimeException("Network crash")

        stompManager.sendHello()

        // Kurze Pause, damit Dispatchers.IO den Fehler catchen kann
        delay(100)

        // Prüfen, ob der Fehler im Catch-Block korrekt geloggt wurde
        verify { Log.e("MyStompManager", "Send failed", any()) }
    }

    // --- SENDE-TESTS (JSON) ---

    @Test
    fun sendJson_whenNotConnected_emitsError() = runTest {
        val deferred = async(UnconfinedTestDispatcher(testScheduler)) {
            stompManager.responses.first()
        }

        stompManager.sendJson()

        assertEquals("Error: Not connected", deferred.await())
    }

    @Test
    fun sendJson_whenConnected_sendsJsonText() = runTest {
        stompManager.connect()
        coVerify(timeout = 2000) { mockStompClient.connect(any(), any()) }

        stompManager.sendJson()

        coVerify(timeout = 2000) { mockSession.send(headers = any(), body = any()) }
    }

    @Test
    fun sendJson_throwsException_logsError() = runTest {

        stompManager.connect()

        delay(100)

        coEvery {
            mockSession.send(any<StompSendHeaders>(), any<FrameBody>())
        } throws RuntimeException("JSON crash")

        stompManager.sendJson()

        advanceUntilIdle()

        // 5. Verification
        verify(exactly = 1) {
            Log.e(
                eq("MyStompManager"),
                eq("Send JSON failed"),
                match { it is RuntimeException && it.message == "JSON crash" }
            )
        }
    }
}