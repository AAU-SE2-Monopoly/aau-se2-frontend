package at.aau.monopoly.klagenfurt

import MyStomp
import android.os.Looper
import io.mockk.Awaits
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MyStompTest {

    private lateinit var callbacks: Callbacks
    private lateinit var myStomp: MyStomp
    private lateinit var mockSession: StompSession

    @Before
    fun setup() {
        callbacks = mockk(relaxed = true)
        myStomp = MyStomp(callbacks)
        mockSession = mockk(relaxed = true)

        // 1. StompClient Instanziierung abfangen
        mockkConstructor(StompClient::class)
        coEvery { anyConstructed<StompClient>().connect(any<String>()) } returns mockSession

        // 2. DER MAGISCHE FIX: Krossbow Extension Functions statisch mocken!
        // Dadurch fängt MockK 'sendText' und 'subscribeText' direkt ab,
        // ohne dass Krossbow im Hintergrund störende UUIDs oder Objekte generiert.
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")

        // Standard-Verhalten für die gemockten Extensions definieren
        coEvery { mockSession.sendText(any(), any()) } just Awaits
        coEvery { mockSession.subscribeText(any()) } returns flowOf()
    }

    @After
    fun teardown() {
        unmockkAll() // WICHTIG: Löscht auch die statischen Mocks
        clearAllMocks()
    }

    private fun flushMainThread() {
        ShadowLooper.shadowMainLooper().idle()
    }

    @Test
    fun `connect should initialize session, subscribe to topics and notify success`() {
        coEvery { mockSession.subscribeText("/topic/hello-response") } returns flowOf()
        coEvery { mockSession.subscribeText("/topic/rcv-object") } returns flowOf()

        myStomp.connect()

        coVerify(timeout = 2000) { anyConstructed<StompClient>().connect("ws://10.0.2.2:8080/websocket-example-broker") }
        coVerify(timeout = 2000) { mockSession.subscribeText("/topic/hello-response") }
        coVerify(timeout = 2000) { mockSession.subscribeText("/topic/rcv-object") }

        flushMainThread()
        verify { callbacks.onResponse("connected") }
    }

    @Test
    fun `connect failure should notify error`() {
        coEvery { anyConstructed<StompClient>().connect(any<String>()) } throws RuntimeException("Connection Timeout")

        myStomp.connect()

        coVerify(timeout = 2000) { anyConstructed<StompClient>().connect(any<String>()) }
        flushMainThread()
        verify { callbacks.onResponse("Connection error") }
    }

    @Test
    fun `collecting from hello-response topic should trigger callback`() {
        val testMessage = "Hello from server"
        coEvery { mockSession.subscribeText("/topic/hello-response") } returns flowOf(testMessage)
        coEvery { mockSession.subscribeText("/topic/rcv-object") } returns flowOf()

        myStomp.connect()

        coVerify(timeout = 2000) { mockSession.subscribeText("/topic/hello-response") }
        Thread.sleep(100)
        flushMainThread()

        verify { callbacks.onResponse(testMessage) }
    }

    @Test
    fun `collecting from rcv-object topic should parse JSON and trigger callback`() {
        val validJson = "{\"from\":\"server\", \"text\":\"Parsed JSON message\"}"
        coEvery { mockSession.subscribeText("/topic/hello-response") } returns flowOf()
        coEvery { mockSession.subscribeText("/topic/rcv-object") } returns flowOf(validJson)

        myStomp.connect()

        coVerify(timeout = 2000) { mockSession.subscribeText("/topic/rcv-object") }
        Thread.sleep(100)
        flushMainThread()

        verify { callbacks.onResponse("Parsed JSON message") }
    }

    @Test
    fun `sendHello should send message if connected`() {
        myStomp.connect()
        coVerify(timeout = 2000) { anyConstructed<StompClient>().connect(any<String>()) }

        myStomp.sendHello()

        // Dank mockkStatic funktioniert das hier jetzt fehlerfrei mit Strings!
        coVerify(timeout = 2000) { mockSession.sendText("/app/hello", "message from client") }
    }

    @Test
    fun `sendHello should notify error if not connected`() {
        myStomp.sendHello()

        Thread.sleep(100)
        flushMainThread()

        verify { callbacks.onResponse("Error: Not connected") }
    }

    @Test
    fun `sendJson should format correct JSON and send if connected`() {
        myStomp.connect()
        coVerify(timeout = 2000) { anyConstructed<StompClient>().connect(any<String>()) }

        myStomp.sendJson()

        // Dank mockkStatic können wir einfach den String-Slot verwenden
        val jsonSlot = slot<String>()
        coVerify(timeout = 2000) { mockSession.sendText("/app/object", capture(jsonSlot)) }

        val capturedJson = JSONObject(jsonSlot.captured)
        assertEquals("client", capturedJson.getString("from"))
        assertEquals("from client", capturedJson.getString("text"))
    }

    @Test
    fun `sendJson should notify error if not connected`() {
        myStomp.sendJson()

        Thread.sleep(100)
        flushMainThread()

        verify { callbacks.onResponse("Error: Not connected") }
    }
}