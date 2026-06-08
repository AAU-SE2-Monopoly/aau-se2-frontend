package at.aau.monopoly.klagenfurt.networking

import at.aau.monopoly.klagenfurt.messaging.GameEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertNotNull
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

        var lastPayRentFieldId: Int? = null
        var lastPayRentDiceTotal: Int? = null
        var payRentCalled = false
        var mortgagePropertyCalled = false
        var lastMortgageFieldId: Int? = null
        var unmortgagePropertyCalled = false
        var lastUnmortgageFieldId: Int? = null
        var declareBankruptcyCalled = false
        var debugForwardCalled = false
        var debugSetupCalled = false

        override fun connect() {}
        override fun disconnect() {}
        override fun subscribeToGame(gameId: String) {}
        override fun subscribeToLobby() {}
        override fun requestGameList() {}
        override fun closeGame(gameId: String) {}

        override fun payJailFine() {}
        override fun useJailCard() {}
        override fun reportCheater(reportedPlayerId: String) {}

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
        override fun buyProperty(fieldId: Int) {}

        override fun buyHouse(fieldId: Int) {}
        override fun sellHouse(fieldId: Int) {}
        override fun buyHotel(fieldId: Int) {}
        override fun sellHotel(fieldId: Int) {}
        override fun payRent(fieldId: Int?, diceTotal: Int) {
            payRentCalled = true
            lastPayRentFieldId = fieldId
            lastPayRentDiceTotal = diceTotal
        }
        override fun mortgageProperty(fieldId: Int) {
            mortgagePropertyCalled = true
            lastMortgageFieldId = fieldId
        }
        override fun unmortgageProperty(fieldId: Int) {
            unmortgagePropertyCalled = true
            lastUnmortgageFieldId = fieldId
        }
        override fun declareBankruptcy() { declareBankruptcyCalled = true }
        override fun debugForwardGame() { debugForwardCalled = true }
        override fun debugSetupBankruptcy() { debugSetupCalled = true }
        override fun payTax(fieldId: Int) {
            // no-op
        }
    }

    @Test
    fun `default logEvents returns events`() {
        val service = TestGameService()
        assertSame(service.events, service.logEvents)
    }

    @Test
    fun `payRent is callable through interface`() {
        val service: GameService = TestGameService()
        service.payRent(5, 8)
    }

    @Test
    fun `payRent passes fieldId and diceTotal to implementation`() {
        val service = TestGameService()
        service.payRent(5, 8)
        junit.framework.Assert.assertTrue(service.payRentCalled)
        junit.framework.Assert.assertEquals(5, service.lastPayRentFieldId)
        junit.framework.Assert.assertEquals(8, service.lastPayRentDiceTotal)
    }

    @Test
    fun `payRent handles null fieldId`() {
        val service = TestGameService()
        service.payRent(null, 0)
        junit.framework.Assert.assertTrue(service.payRentCalled)
        junit.framework.Assert.assertNull(service.lastPayRentFieldId)
    }

    @Test
    fun `mortgageProperty is callable through interface`() {
        val service: GameService = TestGameService()
        service.mortgageProperty(3)
        (service as TestGameService).let {
            junit.framework.Assert.assertTrue(it.mortgagePropertyCalled)
            junit.framework.Assert.assertEquals(3, it.lastMortgageFieldId)
        }
    }

    @Test
    fun `unmortgageProperty is callable through interface`() {
        val service = TestGameService()
        service.unmortgageProperty(7)
        junit.framework.Assert.assertTrue(service.unmortgagePropertyCalled)
        junit.framework.Assert.assertEquals(7, service.lastUnmortgageFieldId)
    }

    @Test
    fun `declareBankruptcy is callable through interface`() {
        val service = TestGameService()
        service.declareBankruptcy()
        junit.framework.Assert.assertTrue(service.declareBankruptcyCalled)
    }

    @Test
    fun `debugForwardGame is callable through interface`() {
        val service = TestGameService()
        service.debugForwardGame()
        junit.framework.Assert.assertTrue(service.debugForwardCalled)
    }

    @Test
    fun `debugSetupBankruptcy is callable through interface`() {
        val service = TestGameService()
        service.debugSetupBankruptcy()
        junit.framework.Assert.assertTrue(service.debugSetupCalled)
    }

    @Test
    fun `interface exposes all payment flow fields`() {
        val service: GameService = TestGameService()
        assertNotNull(service.events)
        assertNotNull(service.status)
        assertNotNull(service.lobbyEvents)
        assertNotNull(service.subscriptionReady)
        assertNotNull(service.lobbySubscriptionReady)
        assertNotNull(service.connectionState)
        assertNotNull(service.reconnectFailed)
    }
}