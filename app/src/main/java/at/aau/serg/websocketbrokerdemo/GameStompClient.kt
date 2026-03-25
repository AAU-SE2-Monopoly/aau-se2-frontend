package at.aau.serg.websocketbrokerdemo

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import org.json.JSONObject

private const val WEBSOCKET_URI = "ws://10.0.2.2:8080/ws"

class GameStompClient(private val callbacks: GameCallbacks) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var session: StompSession? = null
    private var subscriptionJob: Job? = null

    var currentGameId: String = ""
    var currentPlayerId: String = java.util.UUID.randomUUID().toString()

    fun connect() {
        val client = StompClient(OkHttpWebSocketClient())
        scope.launch {
            try {
                session = client.connect(WEBSOCKET_URI)
                postToMain { callbacks.onStatus("Connected ✓") }
            } catch (e: Exception) {
                Log.e("GameStomp", "connect error", e)
                postToMain { callbacks.onStatus("Connection failed: ${e.message}") }
            }
        }
    }

    fun subscribeToGame(gameId: String) {
        subscriptionJob?.cancel()
        subscriptionJob = scope.launch {
            try {
                val flow: Flow<String> = session!!.subscribeText("/topic/game/$gameId")
                flow.collect { raw ->
                    postToMain { callbacks.onGameEvent(raw) }
                }
            } catch (e: Exception) {
                Log.e("GameStomp", "subscription error", e)
                postToMain { callbacks.onStatus("Subscription error: ${e.message}") }
            }
        }
    }

    /** Create a new game. Subscribes to a player-specific temp topic first so we can
     *  receive the GAME_CREATED event (which contains the real gameId) before we know it. */
    fun createGame(playerName: String) {
        // Subscribe to /topic/game/{playerId} BEFORE sending – the backend will echo
        // the GAME_CREATED event there so we can extract and auto-fill the real gameId.
        subscribeToGame(currentPlayerId)

        val playerJson = JSONObject()
            .put("id", currentPlayerId)
            .put("name", playerName)
            .put("position", 0)
            .put("money", 1500)
            .put("inJail", false)
            .put("jailTurns", 0)
            .put("getOutOfJailCards", 0)
            .put("ownedPropertyIds", org.json.JSONArray())
            .toString()
        scope.launch {
            try {
                session?.sendText("/app/game/create", playerJson)
                    ?: postToMain { callbacks.onStatus("Not connected") }
            } catch (e: Exception) {
                Log.e("GameStomp", "createGame error", e)
            }
        }
    }

    fun joinGame(gameId: String, playerName: String) {
        currentGameId = gameId
        subscribeToGame(gameId)
        sendRaw("/app/game/join", buildAction(action = "", extra = mapOf("name" to playerName)))
    }

    fun startGame() {
        sendRaw("/app/game/start", buildAction())
    }

    fun rollDice() = sendRaw("/app/game/action", buildAction("ROLL_DICE"))

    fun endTurn() = sendRaw("/app/game/action", buildAction("END_TURN"))

    fun requestState() = sendRaw("/app/game/state", buildAction())

    fun setGameId(gameId: String) {
        currentGameId = gameId
        subscribeToGame(gameId)
    }

    private fun buildAction(action: String = "", extra: Map<String, String> = emptyMap()): String {
        val payload = JSONObject()
        extra.forEach { (k, v) -> payload.put(k, v) }
        return JSONObject()
            .put("gameId", currentGameId)
            .put("playerId", currentPlayerId)
            .put("action", action)
            .put("payload", payload)
            .toString()
    }

    private fun sendRaw(destination: String, json: String) {
        scope.launch {
            try {
                session?.sendText(destination, json)
                    ?: postToMain { callbacks.onStatus("Not connected") }
            } catch (e: Exception) {
                Log.e("GameStomp", "send error to $destination", e)
            }
        }
    }

    private fun postToMain(block: () -> Unit) = Handler(Looper.getMainLooper()).post(block)
}

