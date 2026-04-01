
import android.util.Log
import at.aau.serg.websocketbrokerdemo.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.json.JSONObject

private const val WEBSOCKET_URI = "ws://10.0.2.2:8080/websocket-example-broker"

class MyStompManager(stompClient: StompClient) {


    private var topicJob: Job? = null
    private var jsonJob: Job? = null

    private val _responses = MutableSharedFlow<String>(replay = 0)
    val responses: SharedFlow<String> = _responses.asSharedFlow()
    private var session: StompSession? = null
    private var isConnecting = false
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    fun connect() {

        if (session != null) return

        if (isConnecting) return

        isConnecting = true
        val client = ServiceLocator.provideStompClient()
        // other config can be passed in here
        scope.launch {
            try {
                session = client.connect(WEBSOCKET_URI)

                topicJob = launch {
                    session?.subscribeText("/topic/hello-response")?.collect { msg ->
                        _responses.emit(msg)
                    }
                }
                jsonJob = launch {
                    session?.subscribeText("/topic/rcv-object")?.collect  { msg ->
                        val text = JSONObject(msg).optString("text", "Parse error")
                        _responses.emit(text)
                    }

                }
                _responses.emit("connected")
            } catch(e: Exception) {
                Log.e("MyStompManager","Connection failed",e)
                _responses.emit("Connection error")
            }
            finally {
                isConnecting=false  //reset the flag
            }
        }
    }
    // connect to JSON topic




    fun sendHello() {
        scope.launch {
            try {
                val currentSession = session
                if (currentSession != null) {
                    currentSession.sendText("/app/hello", "message from client")
                } else {
                    _responses.emit("Error: Not connected")
                }
            } catch (e: Exception) {
                Log.e("MyStompManager", "Send failed", e)
            }
        }
    }

    fun sendJson() {
        val json = JSONObject()
            .put("from", "client")
            .put("text", "from client")
            .toString()

        scope.launch {
            try {
                val currentSession = session
                if (currentSession != null) {
                    currentSession.sendText("/app/object", json)
                } else {
                    _responses.emit("Error: Not connected")
                }
            } catch (e: Exception) {
                Log.e("MyStompManager", "Send JSON failed", e)
            }
        }
    }
}