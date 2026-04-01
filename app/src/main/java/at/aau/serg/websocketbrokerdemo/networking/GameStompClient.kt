package at.aau.serg.websocketbrokerdemo.networking

import android.util.Log
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
import java.util.UUID

class GameStompClient(
    private val stompClient: StompClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val websocketUri: String = "ws://10.0.2.2:8080/ws"
) : GameService {

    private var session: StompSession? = null
    private var subscriptionJob: Job? = null
    private var isConnecting = false

    private val _events = MutableSharedFlow<String>(replay = 0)
    override val events: SharedFlow<String> = _events.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()

    private var currentGameId: String = ""
    private val currentPlayerId: String = UUID.randomUUID().toString()

    override fun connect() {
        if (session != null || isConnecting) return

        isConnecting = true
        scope.launch {
            try {
                session = stompClient.connect(websocketUri)
                _status.emit("Connected ✓")
            } catch (e: Exception) {
                Log.e("GameStomp", "connect error", e)
                _status.emit("Connection error: ${e.message}")
            } finally {
                isConnecting = false
            }
        }
    }

    override fun subscribeToGame(gameId: String) {
        currentGameId = gameId
        subscriptionJob?.cancel()
        subscriptionJob = scope.launch {
            try {
                session?.subscribeText("/topic/game/$gameId")?.collect { msg ->
                    _events.emit(msg)
                }
            } catch (e: Exception) {
                Log.e("GameStomp", "subscription error", e)
                _status.emit("Subscription error: ${e.message}")
            }
        }
    }

    override fun createGame(playerName: String) {
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
        sendRaw("/app/game/create", playerJson)
    }

    override fun joinGame(gameId: String, playerName: String) {
        currentGameId = gameId
        subscribeToGame(gameId)
        sendRaw("/app/game/join", buildAction(extra = mapOf("name" to playerName)))
    }

    override fun startGame() = sendRaw("/app/game/start", buildAction())
    override fun rollDice() = sendRaw("/app/game/action", buildAction("ROLL_DICE"))
    override fun endTurn() = sendRaw("/app/game/action", buildAction("END_TURN"))
    override fun requestState() = sendRaw("/app/game/state", buildAction())

    override fun setGameId(gameId: String) {
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
                session?.sendText(destination, json) ?: _status.emit("Not connected")
            } catch (e: Exception) {
                Log.e("GameStomp", "send error to $destination", e)
            }
        }
    }
}
