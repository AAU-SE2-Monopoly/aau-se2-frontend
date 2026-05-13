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
import kotlinx.coroutines.flow.mapNotNull
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

    private val _reconnectFailed = MutableStateFlow(false)
    override val reconnectFailed: StateFlow<Boolean> = _reconnectFailed.asStateFlow()

    private var _currentGameId: String = ""
    override val currentGameId: String get() = _currentGameId

    private var _currentPlayerName: String = ""
    override val currentPlayerName: String get() = _currentPlayerName

    override val currentPlayerId: String = UUID.randomUUID().toString()

    override fun connect() {
        _reconnectFailed.value = false
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
                _reconnectFailed.value = false
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
                    isConnecting = false  // clear BEFORE startReconnectLoop so its guard doesn't bail
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
        _reconnectFailed.value = false
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
                    _reconnectFailed.value = false
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
            _reconnectFailed.value = true
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
        _reconnectFailed.value = false
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

        // Launch the send asynchronously so we can start collecting events before the
        // server responds. By collecting synchronously on _events (instead of via
        // scope.launch) we eliminate the race condition where the GAME_CREATED event
        // could arrive before our collector is active.
        scope.launch {
            sendRawInternal("/app/game/create", playerJson)
        }

        // Collect from _events synchronously in the current coroutine.
        // This guarantees we are subscribed to _events before the server can respond.
        val createEvent = withTimeoutOrNull(10_000L) {
            _events
                .mapNotNull { json ->
                    try {
                        val event = objectMapper.readValue(json, GameEvent::class.java)
                        if (event.event == "GAME_CREATED") event else null
                    } catch (_: Exception) {
                        // Ignore parse errors
                        null
                    }
                }
                .first()
        }

        if (createEvent == null) {
            emitStatus("Create game failed: no response from server (timeout)")
            // Note: the game may still have been created on the server.
            // Server-side TTL on empty WAITING games is the proper mitigation.
            return null
        }

        val gameId = createEvent.gameId
        if (gameId.isNullOrBlank()) {
            emitStatus("Create game failed: no gameId in response")
            return null
        }


        val subscribed = subscribeToGameInternal(
            gameId = gameId,
            requestStateAfterSubscribe = true,
            isNewlyCreated = true
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

        // Launch the send asynchronously so we can start collecting events before the
        // server responds. By collecting synchronously on _events (instead of via
        // scope.launch) we eliminate the race condition where the PLAYER_JOINED event
        // could arrive before our collector is active.
        scope.launch {
            sendRawInternal(
                "/app/game/join",
                buildAction(extra = mapOf("name" to playerName, "iconId" to iconId))
            )
        }

        // Collect from _events synchronously in the current coroutine.
        // This guarantees we are subscribed to _events before the server can respond.
        val result = withTimeoutOrNull(15_000L) {
            _events
                .mapNotNull { json ->
                    try {
                        val event = objectMapper.readValue(json, GameEvent::class.java)

                        // Ignore events from other games
                        if (event.gameId != gameId) return@mapNotNull null

                        when (event.event) {
                            "PLAYER_JOINED" -> {
                                val isOurJoin = event.gameState?.players
                                    ?.any { it.id == currentPlayerId } == true
                                if (isOurJoin) event else null
                            }
                            "ERROR" -> {
                                event
                            }
                            else -> null
                        }
                    } catch (_: Exception) {
                        // Ignore parse errors
                        null
                    }
                }
                .first()
        }

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
    override fun rollDice(isCheating: Boolean) {
        // 1. Die Cheat-Info als Map vorbereiten
        val actionPayload = if (isCheating) {
            mapOf("cheat" to "true")
        } else {
            emptyMap()
        }

        // 2. Die Payload an buildAction übergeben (hier musst du buildAction evtl. anpassen!)
        val payload = buildAction("ROLL_DICE", actionPayload)

        Log.d("DiceDebug", "rollDice gameId=$_currentGameId playerId=$currentPlayerId isCheating=$isCheating payload=$payload")
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
        requestStateAfterSubscribe: Boolean,
        isNewlyCreated: Boolean = false
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

        var ready = withTimeoutOrNull(8_000L) {
            gameChannel.isReady.first { it }
        }
        if (ready == null) {
            Log.w("GameStomp", "Subscription timed out for $gameId (attempt 1/2) – retrying")
            gameChannel.cancel()
            gameChannel.subscribe(gameId)
            ready = withTimeoutOrNull(8_000L) {
                gameChannel.isReady.first { it }
            }
        }
        if (ready == null) {
            Log.e("GameStomp", "Subscription timed out for $gameId (attempt 2/2)")
            if (isNewlyCreated && gameId.isNotEmpty() && gameId == _currentGameId) {
                val capturedSession = session
                try {
                    if (capturedSession != null) {
                        capturedSession.sendText("/app/game/close", buildAction(gameId = gameId))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
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



    override fun executeAction(playerId: String) {
        Log.d("GameStomp", "Executing action for player: $playerId")
        val action = GameAction(
            gameId = _currentGameId,
            playerId = playerId,
            action = "EXECUTE_ACTION",
            payload = emptyMap()
        )
        val json = JacksonProvider.objectMapper.writeValueAsString(action)
        sendRaw("/app/game/action", json)
    }

    override fun drawCard(cardType: String) {
        Log.d("GameStomp", "Drawing card for player: $currentPlayerId with type: $cardType")
        val action = GameAction(
            gameId = _currentGameId,
            playerId = currentPlayerId,
            action = "DRAW_CARD",
            payload = mapOf("cardType" to cardType)
        )
        val json = JacksonProvider.objectMapper.writeValueAsString(action)
        sendRaw("/app/game/action", json)
    }
}
