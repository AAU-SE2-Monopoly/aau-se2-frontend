package at.aau.monopoly.klagenfurt.networking

import at.aau.monopoly.klagenfurt.messaging.GameEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertSame
import org.junit.Test

class GameServiceTest {

    private class TestGameService : GameService {
        override val events: SharedFlow<String> = MutableSharedFlow()
        override val status: SharedFlow<String> = MutableSharedFlow()
        override val lobbyEvents: SharedFlow<String> = MutableSharedFlow()

        override val currentPlayerId: String = "p1"
        override val currentPlayerName: String = "Alice"
        override val currentGameId: String = "g1"

        override val subscriptionReady: StateFlow<Boolean> = MutableStateFlow(true)
        override val lobbySubscriptionReady: StateFlow<Boolean> = MutableStateFlow(true)
        override val connectionState: StateFlow<Boolean> = MutableStateFlow(true)
        override val reconnectFailed: StateFlow<Boolean> = MutableStateFlow(false)

        override fun connect() {}
        override fun disconnect() {}
        override fun subscribeToGame(gameId: String) {}
        override fun subscribeToLobby() {}
        override fun requestGameList() {}
        override fun closeGame(gameId: String) {}

        override fun payJailFine() {
        }

        override fun useJailCard() { }

        override suspend fun createGame(
            playerName: String,
            iconId: String
        ): String? = "g1"

        override suspend fun joinGame(
            gameId: String,
            playerName: String,
            iconId: String
        ): Result<GameEvent> {
            return Result.success(
                GameEvent(
                    gameId = gameId,
                    event = "PLAYER_JOINED"
                )
            )
        }

        override fun startGame() {}
        override fun rollDice(isCheating: Boolean) {}
        override fun endTurn() {}
        override fun requestState() {}
        override fun setGameId(gameId: String) {}
        override fun executeAction(playerId: String) {}
        override fun drawCard(cardType: String) {}
        override fun buyProperty(fieldId: Int) {
            // no-op for interface test
        }
    }

    @Test
    fun `default logEvents returns events`() {
        val service = TestGameService()

        assertSame(service.events, service.logEvents)
    }


}