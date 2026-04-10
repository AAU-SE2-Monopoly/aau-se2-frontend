package at.aau.serg.websocketbrokerdemo.networking

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    //private val websocketUri: String = "ws://10.0.2.2:8080/ws"
    //IP for WSL use
    private val websocketUri: String = "ws://localhost:8080/ws"
) : GameService {

    private var session: StompSession? = null
    private var subscriptionJob: Job? = null
    private var connectJob: Job? = null
    private var isConnecting = false

    private val _events = MutableSharedFlow<String>(replay = 1)
    override val events: SharedFlow<String> = _events.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()

    private var currentGameId: String = ""
    private val currentPlayerId: String = UUID.randomUUID().toString()

    override fun connect() {
        if (session != null) {
            Log.d("GameStomp", "Already connected (session=$session)")
            _status.tryEmit("Already connected")
            return
        }

        if (isConnecting) {
            Log.d("GameStomp", "Connection already in progress...")
            _status.tryEmit("Connection already in progress")
            return
        }

        isConnecting = true
        connectJob = scope.launch {
            try {
                Log.d("GameStomp", "Connecting to $websocketUri...")
                session = stompClient.connect(websocketUri)
                _status.emit("Connected ✓")
                Log.d("GameStomp", "Connected successfully")
            } catch (e: Throwable) {
                if (isCancellation(e)) {
                    Log.d("GameStomp", "Connection attempt cancelled")
                } else {
                    Log.e("GameStomp", "connect error", e)
                    session = null
                    _status.emit("Connection error: ${e.message}")
                }
            } finally {
                isConnecting = false
            }
        }
    }

    override fun disconnect() {
        connectJob?.cancel()
        subscriptionJob?.cancel()
        val currentSession = session
        session = null
        scope.launch {
            try {
                currentSession?.disconnect()
                _status.emit("Disconnected")
            } catch (e: Throwable) {
                if (!isCancellation(e)) {
                    Log.e("GameStomp", "disconnect error", e)
                }
            }
        }
    }

    override fun subscribeToGame(gameId: String) {
        if (subscriptionJob?.isActive == true && gameId == currentGameId) {
            Log.d("GameStomp", "Already subscribed to $gameId")
            return
        }
        
        currentGameId = gameId
        subscriptionJob?.cancel()
        subscriptionJob = scope.launch {
            try {
                val currentSession = session
                if (currentSession == null) {
                    Log.w("GameStomp", "Cannot subscribe: not connected")
                    return@launch
                }
                Log.d("GameStomp", "Subscribing to /topic/game/$gameId")
                currentSession.subscribeText("/topic/game/$gameId").collect { msg ->
                    _events.emit(msg)
                }
            } catch (e: Throwable) {
                if (isCancellation(e)) {
                    Log.d("GameStomp", "Subscription job cancelled for $gameId")
                } else {
                    Log.e("GameStomp", "subscription error", e)
                    _status.emit("Subscription error: ${e.message}")
                }
            }
        }
    }

    override fun createGame(playerName: String) {
        scope.launch {
            val currentSession = session
            if (currentSession == null) {
                Log.w("GameStomp", "Cannot create game: not connected")
                _status.emit("Not connected")
                return@launch
            }
            try {

                currentGameId = currentPlayerId
                subscriptionJob?.cancel()

                Log.d("GameStomp", "Establishing subscription for createGame...")
        subscribeToGame(currentPlayerId)
                val flow = currentSession.subscribeText("/topic/game/$currentPlayerId")


                subscriptionJob = launch {
                    flow.collect { msg ->
                        _events.emit(msg)
                    }
                }

                Log.d("GameStomp", "Subscription established! Now sending create command...")

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

                currentSession.sendText("/app/game/create", playerJson)

            } catch (e: Throwable) {
                Log.e("GameStomp", "createGame error", e)
            }
        }
    }

    override fun joinGame(gameId: String, playerName: String) {

        subscribeToGame(gameId)
        sendRaw("/app/game/join", buildAction(extra = mapOf("name" to playerName)))
    }

    override fun startGame() = sendRaw("/app/game/start", buildAction())
    override fun rollDice() = sendRaw("/app/game/action", buildAction("ROLL_DICE"))
    override fun endTurn() = sendRaw("/app/game/action", buildAction("END_TURN"))
    override fun requestState() = sendRaw("/app/game/state", buildAction())

    override fun setGameId(gameId: String) {

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
                val currentSession = session
                if (currentSession != null) {
                    currentSession.sendText(destination, json)
                } else {
                    _status.emit("Not connected")
                }
            } catch (e: Throwable) {
                if (!isCancellation(e)) {
                    Log.e("GameStomp", "send error to $destination", e)
                    _status.emit("Send error: ${e.message}")
                }
            }
        }
    }

    private fun isCancellation(e: Throwable): Boolean {
        return e is CancellationException || 
               (e is IllegalStateException && e.message?.contains("cancelled", ignoreCase = true) == true) ||
               (e.cause is CancellationException)
    }
}
