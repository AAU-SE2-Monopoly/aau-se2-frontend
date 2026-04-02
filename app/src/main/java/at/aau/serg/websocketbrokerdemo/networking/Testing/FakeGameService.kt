package at.aau.serg.websocketbrokerdemo.networking.Testing

import at.aau.serg.websocketbrokerdemo.networking.GameService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
/**This Class is only used for testing**/
class FakeGameService: GameService {
    private val _events = MutableSharedFlow<String>(replay = 1)
    override val events: SharedFlow<String> = _events.asSharedFlow()

    private val _status = MutableSharedFlow<String>(replay = 1)
    override val status: SharedFlow<String> = _status.asSharedFlow()
    // Fake implementation of GameService

    var connectCalled = false
    var lastSubscribedGameId: String? = null
    var lastCreatedPlayerName: String? = null
    var lastJoinedGameId: String? = null
    var lastJoinedPlayerName: String? = null
    var startGameCalled = false
    var rollDiceCalled = false
    var endTurnCalled = false
    var requestStateCalled = false
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun subscribeToGame(gameId: String) {
        TODO("Not yet implemented")
    }

    override fun createGame(playerName: String) {
        TODO("Not yet implemented")
    }

    override fun joinGame(gameId: String, playerName: String) {
        TODO("Not yet implemented")
    }

    override fun startGame() {
        TODO("Not yet implemented")
    }

    override fun rollDice() {
        TODO("Not yet implemented")
    }

    override fun endTurn() {
        TODO("Not yet implemented")
    }

    override fun requestState() {
        TODO("Not yet implemented")
    }

    override fun setGameId(gameId: String) {
        TODO("Not yet implemented")
    }
}