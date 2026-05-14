package at.aau.monopoly.klagenfurt


import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.networking.GameService
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** This Class is only used for testing **/
class FakeGameService : GameService {
    private val _events = MutableSharedFlow<String>(replay = 1)
    override val events: SharedFlow<String> = _events.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()

    private val _lobbyEvents = MutableSharedFlow<String>(replay = 1)
    override val lobbyEvents: SharedFlow<String> = _lobbyEvents.asSharedFlow()

    private val _subscriptionReady = MutableStateFlow(false)
    override val subscriptionReady: StateFlow<Boolean> = _subscriptionReady.asStateFlow()

    private val _lobbySubscriptionReady = MutableStateFlow(false)
    override val lobbySubscriptionReady: StateFlow<Boolean> = _lobbySubscriptionReady.asStateFlow()

    private val _connectionState = MutableStateFlow(false)
    override val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _reconnectFailed = MutableStateFlow(false)
    override val reconnectFailed: StateFlow<Boolean> = _reconnectFailed.asStateFlow()

    override var currentPlayerId: String = "test-player-id"
    override var currentPlayerName: String = "test-player-name"
    override var currentGameId: String = "test-game-id"

    var connectCalled = false
    var connectCalls = 0
    var ignoreFirstConnectCall = false
    var lastSubscribedGameId: String? = null
    var lastCreatedPlayerName: String? = null
    var lastJoinedGameId: String? = null
    var lastJoinedPlayerName: String? = null

    var lastBoughtFieldId: Int? = null
    var lastCreatedIconId: String? = null
    var lastJoinedIconId: String? = null

    // Call counters
    var joinGameCalls = 0
    var createGameCalls = 0
    var startGameCalled = false
    var rollDiceCalled = false
    var endTurnCalled = false
    var requestStateCalled = false
    var subscribeToLobbyCalled = false
    var requestGameListCalled = false
    var lastClosedGameId: String? = null
    var closeGameCalls = 0

    /** Set to false to simulate a rejected join during tests */
    var joinGameSuccess: Boolean = true
    var joinGameDelayMs: Long = 0


    override fun connect() {
        connectCalled = true
        if (ignoreFirstConnectCall) {
            ignoreFirstConnectCall = false
            return
        }
        connectCalls++
    }

    override fun disconnect() {
        connectCalled = false
    }

    override fun subscribeToGame(gameId: String) {
        lastSubscribedGameId = gameId
        _subscriptionReady.value = true
    }

    override suspend fun createGame(playerName: String, iconId: String): String? {
        createGameCalls++
        lastCreatedPlayerName = playerName
        lastCreatedIconId = iconId
        return "test-created-game-id"
    }

    override suspend fun joinGame(gameId: String, playerName: String, iconId: String): Result<GameEvent> {
        if (joinGameDelayMs > 0) {
            kotlinx.coroutines.delay(joinGameDelayMs)
        }
        joinGameCalls++
        lastJoinedGameId = gameId
        lastJoinedPlayerName = playerName
        lastJoinedIconId = iconId
        return if (joinGameSuccess) {
            Result.success(
                GameEvent(
                    gameId = gameId,
                    event = "PLAYER_JOINED",
                    message = "$playerName joined the game"
                )
            )
        } else {
            Result.failure(Exception("Join rejected by server"))
        }
    }

    override fun startGame() {
        startGameCalled = true
    }

    override fun rollDice(isCheating: Boolean) {
        rollDiceCalled = true
    }

    override fun endTurn() {
        endTurnCalled = true
    }

    override fun requestState() {
        requestStateCalled = true
    }

    override fun setGameId(gameId: String) {
        lastSubscribedGameId = gameId
        currentGameId = gameId
        _subscriptionReady.value = true
    }

    override fun executeAction(playerId: String) {
        TODO("Not yet implemented")
    }

    override fun drawCard(cardType: String) {
        TODO("Not yet implemented")
    }

    var buyPropertyCalled = false

    override fun buyProperty(fieldId: Int) {
        buyPropertyCalled = true
        lastBoughtFieldId = fieldId
    }

    override fun subscribeToLobby() {
        subscribeToLobbyCalled = true
    }

    override fun requestGameList() {
        requestGameListCalled = true
    }

    override fun closeGame(gameId: String) {
        closeGameCalls++
        lastClosedGameId = gameId
    }

    suspend fun emitTestEvent(jsonMessage: String) {
        _events.emit(jsonMessage)
    }

    suspend fun emitTestStatus(statusMessage: String) {
        _status.emit(statusMessage)
    }

    suspend fun emitTestLobbyEvent(jsonMessage: String) {
        _lobbyEvents.emit(jsonMessage)
    }

    fun setConnectionState(connected: Boolean) {
        _connectionState.value = connected
    }

    fun setLobbySubscriptionReady(ready: Boolean) {
        _lobbySubscriptionReady.value = ready
    }

    fun setReconnectFailed(failed: Boolean) {
        _reconnectFailed.value = failed
    }

    override fun payJailFine(){}

    override fun useJailCard(){}

    fun emitGameState(gameState: GameState) {
        val event = GameEvent(
            gameId = gameState.gameId,
            event = "STATE_SNAPSHOT",
            gameState = gameState
        )

        val json = JacksonProvider.objectMapper.writeValueAsString(event)

        _events.tryEmit(json)
    }

}
