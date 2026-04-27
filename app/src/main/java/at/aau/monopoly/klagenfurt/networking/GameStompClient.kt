package at.aau.monopoly.klagenfurt.networking

import android.util.Log
import at.aau.monopoly.klagenfurt.messaging.GameAction
import at.aau.monopoly.klagenfurt.model.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import java.util.UUID

class GameStompClient(
    private val stompClient: StompClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val websocketUri: String = "ws://10.0.2.2:8080/ws"
    //ip for working with WSL
    //private val websocketUri: String = "ws://localhost:8080/ws"
) : GameService {

    private var session: StompSession? = null
    private var subscriptionJob: Job? = null
    private var personalSubscriptionJob: Job? = null
    private var lobbySubscriptionJob: Job? = null
    private var connectJob: Job? = null
    private var isConnecting = false


    private val _events = MutableSharedFlow<String>(replay = 1)
    override val events: SharedFlow<String> = _events.asSharedFlow()


    private val _logEvents = MutableSharedFlow<String>(replay = 80)
    override val logEvents: SharedFlow<String> = _logEvents.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()

    private val _lobbyEvents = MutableSharedFlow<String>(replay = 1)
    override val lobbyEvents: SharedFlow<String> = _lobbyEvents.asSharedFlow()

    private var _currentGameId: String = ""
    override val currentGameId: String get() = _currentGameId

    private var _currentPlayerName: String = ""
    override val currentPlayerName: String get() = _currentPlayerName

    override val currentPlayerId: String = UUID.randomUUID().toString()

    override fun connect() {
        if (session != null) {
            Log.d("GameStomp", "Already connected (session=$session)")
            tryEmitStatus("Already connected")
            return
        }

        if (isConnecting) {
            Log.d("GameStomp", "Connection already in progress...")
            tryEmitStatus("Connection already in progress")
            return
        }

        isConnecting = true
        connectJob = scope.launch {
            try {
                Log.d("GameStomp", "Connecting to $websocketUri...")
                session = stompClient.connect(websocketUri)

                subscribeToPersonalTopic()
                
                emitStatus("Connected ✓")
                Log.d("GameStomp", "Connected successfully")

            } catch (e: Throwable) {
                if (isCancellation(e)) {
                    Log.d("GameStomp", "Connection attempt cancelled")
                } else {
                    Log.e("GameStomp", "connect error", e)
                    session = null
                    emitStatus("Connection error: ${e.message}")
                }
            } finally {
                isConnecting = false
            }
        }
    }

    private fun subscribeToPersonalTopic() {
        personalSubscriptionJob?.cancel()
        personalSubscriptionJob = scope.launch {
            try {
                val currentSession = session ?: return@launch
                Log.d("GameStomp", "Subscribing to personal topic: /topic/game/$currentPlayerId")
                currentSession.subscribeText("/topic/game/$currentPlayerId").collect { msg ->
                    _events.emit(msg)
                    _logEvents.emit(msg)
                }
            } catch (e: Throwable) {
                if (!isCancellation(e)) {
                    Log.e("GameStomp", "personal subscription error", e)
                }
                personalSubscriptionJob = null
            }
        }
    }

    override fun disconnect() {
        connectJob?.cancel()
        subscriptionJob?.cancel()
        personalSubscriptionJob?.cancel()
        val currentSession = session
        session = null
        scope.launch {
            try {
                currentSession?.disconnect()
                emitStatus("Disconnected")
            } catch (e: Throwable) {
                if (!isCancellation(e)) {
                    Log.e("GameStomp", "disconnect error", e)
                }
            }
        }
    }

    override fun subscribeToGame(gameId: String) {
        scope.launch {

            val success = subscribeToGameInternal(gameId = gameId, requestStateAfterSubscribe = true)
            if (!success) {
                emitStatus("Subscription failed for $gameId")
            }
        }
    }


    override fun subscribeToLobby() {
        if (lobbySubscriptionJob?.isActive == true) {
            Log.d("GameStomp", "Already subscribed to lobby")
            return
        }
        lobbySubscriptionJob = scope.launch {
            try {
                val currentSession = session
                if (currentSession == null) {
                    Log.w("GameStomp", "Cannot subscribe to lobby: not connected")
                    emitStatus("Lobby sub failed: Not connected")
                    return@launch
                }
                Log.d("GameStomp", "Subscribing to /topic/lobby")
                currentSession.subscribeText("/topic/lobby").collect { msg ->
                    _lobbyEvents.emit(msg)
                }
            } catch (e: Throwable) {
                if (isCancellation(e)) {
                    Log.d("GameStomp", "Lobby subscription cancelled")
                } else {
                    Log.e("GameStomp", "lobby subscription error", e)
                    emitStatus("Lobby subscription error: ${e.message}")
                }
                lobbySubscriptionJob = null
            }
        }
    }

    override fun requestGameList() {
        sendRaw("/app/game/list", buildAction())
    }

    override fun closeGame(gameId: String) {
        val savedGameId = _currentGameId
        _currentGameId = gameId
        sendRaw("/app/game/close", buildAction())
        _currentGameId = savedGameId
    }


    private val objectMapper = JacksonProvider.objectMapper

    override fun createGame(playerName: String, iconId: String) {
        _currentPlayerName = playerName
        val player = Player(
            id = currentPlayerId,
            name = playerName,
            iconId = iconId
        )
        val playerJson = objectMapper.writeValueAsString(player)

        Log.d("GameStomp", "Sending create command for player: $playerName with icon: $iconId")
        sendRaw("/app/game/create", playerJson)
    }

    override fun joinGame(gameId: String, playerName: String, iconId: String) {
        _currentPlayerName = playerName
        scope.launch {

            val subscribed = subscribeToGameInternal(gameId = gameId, requestStateAfterSubscribe = true)
            if (!subscribed) {
                emitStatus("Join failed: Could not subscribe to game topic")
                return@launch
            }
            yield()
            Log.d("GameStomp", "Sending join command for game: $gameId with icon: $iconId")
            sendRawInternal(
                "/app/game/join",
                buildAction(extra = mapOf("name" to playerName, "iconId" to iconId))
            )
        }
    }

    override fun startGame() = sendRaw("/app/game/start", buildAction())
    override fun rollDice() = sendRaw("/app/game/action", buildAction("ROLL_DICE"))
    override fun endTurn() = sendRaw("/app/game/action", buildAction("END_TURN"))
    override fun requestState() = sendRaw("/app/game/state", buildAction())

    override fun setGameId(gameId: String) {
        subscribeToGame(gameId)
    }

    private fun buildAction(action: String = "", extra: Map<String, String> = emptyMap()): String {
        val gameAction = GameAction(
            gameId = _currentGameId,
            playerId = currentPlayerId,
            action = action,
            payload = extra
        )
        return objectMapper.writeValueAsString(gameAction)
    }

    private suspend fun subscribeToGameInternal(
        gameId: String,
        requestStateAfterSubscribe: Boolean
    ): Boolean {
        if (subscriptionJob?.isActive == true && gameId == _currentGameId) {
            Log.d("GameStomp", "Already subscribed to $gameId")
            if (requestStateAfterSubscribe) {
                sendRawInternal("/app/game/state", buildAction())
            }
            return true
        }

        _currentGameId = gameId
        subscriptionJob?.cancel()

        return try {
            val currentSession = session
            if (currentSession == null) {
                Log.w("GameStomp", "Cannot subscribe: not connected")
                return false
            }

            Log.d("GameStomp", "Subscribing to /topic/game/$gameId")
            val subscription = currentSession.subscribeText("/topic/game/$gameId")


            subscriptionJob = scope.launch {
                try {
                    subscription.collect { msg ->
                        _events.emit(msg)
                        _logEvents.emit(msg)
                    }
                } catch (e: Throwable) {
                    if (!isCancellation(e)) Log.e("GameStomp", "collect error", e)
                }
            }

            if (requestStateAfterSubscribe) {
                sendRawInternal("/app/game/state", buildAction())
            }

            true
        } catch (e: Throwable) {
            subscriptionJob = null
            if (isCancellation(e)) {
                Log.d("GameStomp", "Subscription job cancelled for $gameId")
            } else {
                Log.e("GameStomp", "subscription error", e)
                emitStatus("Subscription error: ${e.message}")
            }
            false
        }
    }

    private suspend fun sendRawInternal(destination: String, json: String) {
        try {
            val currentSession = session
            if (currentSession != null) {
                currentSession.sendText(destination, json)
            } else {
                emitStatus("Not connected")
            }
        } catch (e: Throwable) {
            if (!isCancellation(e)) {
                Log.e("GameStomp", "send error to $destination", e)
                emitStatus("Send error: ${e.message}")
            }
        }
    }

    private fun sendRaw(destination: String, json: String) {
        scope.launch {
            sendRawInternal(destination, json)
        }
    }

    private suspend fun emitStatus(message: String) {
        Log.d("GameStomp", "Status update: $message")
        _status.emit(message)
    }

    private fun tryEmitStatus(message: String) {
        Log.d("GameStomp", "Status update: $message")
        _status.tryEmit(message)
    }

    private fun isCancellation(e: Throwable): Boolean {
        return e is CancellationException || 
               (e is IllegalStateException && e.message?.contains("cancelled", ignoreCase = true) == true) ||
               (e.cause is CancellationException)
    }
}
