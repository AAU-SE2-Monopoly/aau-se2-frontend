package at.aau.monopoly.klagenfurt


import at.aau.monopoly.klagenfurt.networking.GameService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** This Class is only used for testing **/
class FakeGameService : GameService {
    private val _events = MutableSharedFlow<String>(replay = 1)
    override val events: SharedFlow<String> = _events.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()

    private val _lobbyEvents = MutableSharedFlow<String>(replay = 1)
    override val lobbyEvents: SharedFlow<String> = _lobbyEvents.asSharedFlow()

    override val currentPlayerId: String = "test-player-id"
    override var currentPlayerName: String = "test-player-name"
    override var currentGameId: String = "test-game-id"

    var connectCalled = false
    var lastSubscribedGameId: String? = null
    var lastCreatedPlayerName: String? = null
    var lastJoinedGameId: String? = null
    var lastJoinedPlayerName: String? = null

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

    override fun connect() {
        connectCalled = true
    }

    override fun disconnect() {
        connectCalled = false
    }

    override fun subscribeToGame(gameId: String) {
        lastSubscribedGameId = gameId
    }

    override fun createGame(playerName: String, iconId: String) {
        createGameCalls++
        lastCreatedPlayerName = playerName
    }

    override fun joinGame(gameId: String, playerName: String, iconId: String) {
        joinGameCalls++
        lastJoinedGameId = gameId
        lastJoinedPlayerName = playerName
    }

    override fun startGame() {
        startGameCalled = true
    }

    override fun rollDice() {
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
}
