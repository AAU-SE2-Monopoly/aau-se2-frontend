package at.aau.serg.websocketbrokerdemo.networking.Testing

import at.aau.serg.websocketbrokerdemo.networking.GameService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** This Class is only used for testing **/
class FakeGameService : GameService {
    private val _events = MutableSharedFlow<String>(replay = 1)
    override val events: SharedFlow<String> = _events.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()

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

    override fun connect() {
        connectCalled = true
    }

    override fun subscribeToGame(gameId: String) {
        lastSubscribedGameId = gameId
    }

    override fun createGame(playerName: String) {
        createGameCalls++
        lastCreatedPlayerName = playerName
    }

    override fun joinGame(gameId: String, playerName: String) {
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
    }

    suspend fun emitTestEvent(jsonMessage: String) {
        _events.emit(jsonMessage)
    }

    suspend fun emitTestStatus(statusMessage: String) {
        _status.emit(statusMessage)
    }
}
