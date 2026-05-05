package at.aau.monopoly.klagenfurt.networking

import android.util.Log
import at.aau.monopoly.klagenfurt.messaging.GameAction
import at.aau.monopoly.klagenfurt.model.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.channels.Channel
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import java.util.UUID

class GameStompClient(
    private val stompClient: StompClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val websocketUri: String = ServerConfig.websocketUri
) : GameService {

    private var session: StompSession? = null

    private val _events = MutableSharedFlow<String>()
    override val events: SharedFlow<String> = _events.asSharedFlow()

    private val _logEvents = MutableSharedFlow<String>(replay = 80)
    override val logEvents: SharedFlow<String> = _logEvents.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()

    private val _lobbyEvents = MutableSharedFlow<String>(replay = 1)
    override val lobbyEvents: SharedFlow<String> = _lobbyEvents.asSharedFlow()

    private val lobbyChannel = SubscriptionChannel(
        scope, { session }, "/topic/lobby", _lobbyEvents,
        onError = { startReconnectLoop() }
    )
    private val gameChannel = SubscriptionChannel(
        scope, { session }, "/topic/game/", _events,
        onError = { startReconnectLoop() }
    )
    private var personalSubscriptionJob: Job? = null
    private var connectJob: Job? = null
    private var isConnecting = false
    private var reconnectJob: Job? = null
    private var isReconnecting = false
    private var reconnectAttempts = 0

    init {
        // Forward all game events to the log events shared flow (used by UI event log)
        scope.launch {
            _events.collect { _logEvents.emit(it) }
        }
    }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
    }

    override val subscriptionReady: StateFlow<Boolean> = gameChannel.isReady
    override val lobbySubscriptionReady: StateFlow<Boolean> = lobbyChannel.isReady

    private val _connectionState = MutableStateFlow(false)
    override val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private var _currentGameId: String = ""
    override val currentGameId: String get() = _currentGameId

    private var _currentPlayerName: String = ""
    override val currentPlayerName: String get() = _currentPlayerName

    override val currentPlayerId: String = UUID.randomUUID().toString()

    override fun connect() {
        reconnectJob?.cancel()
        isReconnecting = false
        reconnectAttempts = 0

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
                _connectionState.value = true
                emitStatus("Connected ✓")
                Log.d("GameStomp", "Connected successfully")
            } catch (e: Throwable) {
                if (isCancellation(e)) {
                    Log.d("GameStomp", "Connection attempt cancelled")
                } else {
                    Log.e("GameStomp", "connect error", e)
                    session = null
                    _connectionState.value = false
                    emitStatus("Connection error: ${e.message}")
                    startReconnectLoop()
                }
            } finally {
                isConnecting = false
            }
        }
    }

    private fun startReconnectLoop() {
        if (isReconnecting || isConnecting) return
        isReconnecting = true
        session = null  // discard the stale/dead session
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            while (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                val delayMs = INITIAL_RECONNECT_DELAY_MS * (1L shl reconnectAttempts)
                Log.d("GameStomp", "Reconnecting in ${delayMs}ms (attempt ${reconnectAttempts + 1}/$MAX_RECONNECT_ATTEMPTS)...")
                emitStatus("Reconnecting in ${delayMs / 1000}s...")
                delay(delayMs)
                reconnectAttempts++
                try {
                    _connectionState.value = false
                    session = stompClient.connect(websocketUri)
                    _connectionState.value = true
                    reconnectAttempts = 0
                    isReconnecting = false
                    emitStatus("Reconnected ✓")
                    Log.d("GameStomp", "Reconnected successfully")

                    subscribeToPersonalTopic()
                    if (_currentGameId.isNotBlank()) {
                        gameChannel.cancel()
                        gameChannel.subscribe(_currentGameId)
                    } else {
                        lobbyChannel.subscribe()
                    }
                    return@launch
                } catch (e: CancellationException) {
                    isReconnecting = false
                    throw e
                } catch (e: Throwable) {
                    Log.e("GameStomp", "Reconnect attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS failed: ${e.message}")
                    emitStatus("Reconnect failed ($reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
                    _connectionState.value = false
                }
            }
            isReconnecting = false
            Log.e("GameStomp", "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached – giving up")
            emitStatus("Connection lost – please restart")
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
                    personalSubscriptionJob = null
                    startReconnectLoop()
                }
            }
        }
    }

    override fun disconnect() {
        reconnectJob?.cancel()
        isReconnecting = false
        reconnectAttempts = 0
        _connectionState.value = false
        gameChannel.cancel()
        lobbyChannel.cancel()
        connectJob?.cancel()
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
        lobbyChannel.subscribe()
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

    override suspend fun createGame(playerName: String, iconId: String): String? {
        _currentPlayerName = playerName
        val player = Player(
            id = currentPlayerId,
            name = playerName,
            iconId = iconId
        )
        val playerJson = objectMapper.writeValueAsString(player)

        Log.d("GameStomp", "Sending create command for player: $playerName with icon: $iconId")

        // Start the collector BEFORE sending the request to avoid race conditions
        val resultChannel = Channel<GameEvent>(Channel.CONFLATED)
        val collectingJob = scope.launch {
            _events.collect { json ->
                try {
                    val event = objectMapper.readValue(json, GameEvent::class.java)
                    if (event.event == "GAME_CREATED") {
                        resultChannel.trySend(event)
                    }
                } catch (_: Exception) {
                    // Ignore parse errors
                }
            }
        }

        sendRaw("/app/game/create", playerJson)

        val createEvent = withTimeoutOrNull(10_000L) {
            resultChannel.receive()
        }
        collectingJob.cancel()
        resultChannel.close()

        val gameId = createEvent?.gameId
        if (gameId.isNullOrBlank()) {
            emitStatus("Create game failed: no gameId in response")
            return null
        }


        val subscribed = subscribeToGameInternal(
            gameId = gameId,
            requestStateAfterSubscribe = true
        )
        if (!subscribed) {
            emitStatus("Create game failed: could not subscribe to game topic")
            return null
        }

        return gameId
    }

    override suspend fun joinGame(gameId: String, playerName: String, iconId: String): Result<GameEvent> {
        _currentPlayerName = playerName


        val subscribed = subscribeToGameInternal(
            gameId = gameId,
            requestStateAfterSubscribe = true
        )
        if (!subscribed) {
            emitStatus("Join failed: Could not subscribe to game topic")
            return Result.failure(Exception("Join failed: Could not subscribe to game topic"))
        }

        Log.d("GameStomp", "Sending join command for game: $gameId with icon: $iconId")

        // Start the collector BEFORE sending the request to avoid race conditions
        val resultChannel = Channel<GameEvent>(Channel.CONFLATED)
        val collectingJob = scope.launch {
            _events.collect { json ->
                try {
                    val event = objectMapper.readValue(json, GameEvent::class.java)

                    // Ignore events from other games
                    if (event.gameId != gameId) return@collect

                    when (event.event) {
                        "PLAYER_JOINED" -> {
                            val isOurJoin = event.gameState?.players
                                ?.any { it.id == currentPlayerId } == true
                            if (isOurJoin) {
                                resultChannel.trySend(event)
                            }
                        }
                        "ERROR" -> {
                            resultChannel.trySend(event)
                        }
                    }
                } catch (_: Exception) {
                    // Ignore parse errors
                }
            }
        }

        sendRawInternal(
            "/app/game/join",
            buildAction(extra = mapOf("name" to playerName, "iconId" to iconId))
        )

        val result = withTimeoutOrNull(10_000L) {
            resultChannel.receive()
        }
        collectingJob.cancel()
        resultChannel.close()

        if (result == null) {
            val msg = "Join timeout: no server response for game $gameId"
            Log.w("GameStomp", msg)
            emitStatus(msg)
            return Result.failure(Exception(msg))
        }

        return when (result.event) {
            "PLAYER_JOINED" -> {
                Log.d("GameStomp", "Join confirmed for game $gameId")
                Result.success(result)
            }
            "ERROR" -> {
                val errMsg = result.message ?: "Join rejected by server"
                Log.w("GameStomp", "Join rejected for game $gameId: $errMsg")
                emitStatus(errMsg)
                Result.failure(Exception(errMsg))
            }
            else -> {
                // Should not happen since waitForEventOnGameTopic only returns on these
                Result.failure(Exception("Unexpected event: ${result.event}"))
            }
        }
    }

    override fun startGame() = sendRaw("/app/game/start", buildAction())
    override fun rollDice() {
        val payload = buildAction("ROLL_DICE")
        Log.d("DiceDebug", "rollDice gameId=$_currentGameId playerId=$currentPlayerId payload=$payload")
        sendRaw("/app/game/action", payload)
    }
    override fun endTurn() = sendRaw("/app/game/action", buildAction("END_TURN"))
    override fun requestState() = sendRaw("/app/game/state", buildAction())

    override fun setGameId(gameId: String) {
        subscribeToGame(gameId)
    }

    private fun buildAction(
        action: String = "",
        extra: Map<String, String> = emptyMap(),
        gameId: String = _currentGameId
    ): String {
        val gameAction = GameAction(
            gameId = gameId,
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
        if (gameChannel.isReady.value && gameId == _currentGameId) {
            Log.d("GameStomp", "Already subscribed to $gameId")
            emitStatus("SUBSCRIBED:$gameId")
            if (requestStateAfterSubscribe) {
                sendRawInternal("/app/game/state", buildAction())
            }
            return true
        }

        _currentGameId = gameId
        gameChannel.cancel()
        gameChannel.subscribe(gameId)

        val ready = withTimeoutOrNull(10_000L) {
            gameChannel.isReady.first { it }
        }
        if (ready == null) {
            Log.e("GameStomp", "Subscription timed out for $gameId")
            // Close orphan game if this subscription failure is for a newly created game
            if (gameId.isNotEmpty() && gameId == _currentGameId) {
                scope.launch {
                    sendRawInternal("/app/game/close", buildAction(gameId = gameId))
                }
            }
            startReconnectLoop()
            return false
        }

        emitStatus("SUBSCRIBED:$gameId")

        if (requestStateAfterSubscribe) {
            sendRawInternal("/app/game/state", buildAction())
        }

        return true
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
