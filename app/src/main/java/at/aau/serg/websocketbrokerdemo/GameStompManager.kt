package at.aau.serg.websocketbrokerdemo


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
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import org.json.JSONObject



object GameStompManager  {
    private const val WEBSOCKET_URI = "ws://10.0.2.2:8080/ws"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var session: StompSession? = null
    private var subscriptionJob: Job? = null
    private val _events = MutableSharedFlow<String>(replay = 0)
    val events: SharedFlow<String> = _events.asSharedFlow()
    private var isConnecting=false
    private val _status = MutableSharedFlow<String>(replay = 1)
    val status: SharedFlow<String> = _status.asSharedFlow()
    var currentGameId: String = ""
    var currentPlayerId: String = java.util.UUID.randomUUID().toString()

    fun connect() {

        if (session != null) return

        if (isConnecting) return

        isConnecting = true
        scope.launch {
            try {
                session = StompClientProvider.client.connect(WEBSOCKET_URI)
                _status.emit("Connected ✓")
            } catch (e: Exception) {
                Log.e("GameStomp", "connect error", e)
                _status.emit("Connection error: ${e.message}")
            }
            finally {
                isConnecting = false // Reset the flag
            }
         }
            }



    fun subscribeToGame(gameId: String) {
        currentGameId=gameId
        subscriptionJob?.cancel()
        subscriptionJob = scope.launch {
            try {
                session?.subscribeText("/topic/game/$gameId")?.collect { msg->
                    _events.emit(msg)
                }

            } catch (e: Exception) {
                Log.e("GameStomp", "subscription error", e)
               _status.emit("Subscription error: ${e.message}")
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
        sendRaw("/app/game/create", playerJson)


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
                val currentsession = session
                if (currentsession != null) {
                    currentsession.sendText(destination, json)
                } else {
                    _status.emit("Not connected")
                }
            }catch (e: Exception) {
                Log.e("GameStomp", "send error to $destination", e)
            }
        }
    }


}

