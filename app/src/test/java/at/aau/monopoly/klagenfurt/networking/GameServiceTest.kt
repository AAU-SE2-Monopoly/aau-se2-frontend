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
        var debugLandOnTaxCalled = false
        var debugExecutePayMoneyCalled = false
        var debugExecutePayPerBuildingCalled = false
        var debugExecutePayEachCalled = false
        var debugExecuteCollectFromEachCalled = false
        var proposeTradeCalled = false
        var lastTradeTargetId: String? = null
        var lastTradeOfferMoney: Int? = null
        var lastTradeRequestMoney: Int? = null
        var lastTradeOfferPropertyIds: List<Int>? = null
        var lastTradeRequestPropertyIds: List<Int>? = null
        var lastTradeOfferJailCards: Int? = null
        var lastTradeRequestJailCards: Int? = null
        var acceptTradeCalled = false
        var lastAcceptedTradeId: String? = null
        var rejectTradeCalled = false
        var lastRejectedTradeId: String? = null

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
        override fun proposeTrade(
            toPlayerId: String,
            offerMoney: Int,
            requestMoney: Int,
            offerPropertyIds: List<Int>,
            requestPropertyIds: List<Int>,
            offerJailCards: Int,
            requestJailCards: Int
        ) {
            proposeTradeCalled = true
            lastTradeTargetId = toPlayerId
            lastTradeOfferMoney = offerMoney
            lastTradeRequestMoney = requestMoney
            lastTradeOfferPropertyIds = offerPropertyIds
            lastTradeRequestPropertyIds = requestPropertyIds
            lastTradeOfferJailCards = offerJailCards
            lastTradeRequestJailCards = requestJailCards
        }
        override fun acceptTrade(tradeId: String) {
            acceptTradeCalled = true
            lastAcceptedTradeId = tradeId
        }
        override fun rejectTrade(tradeId: String) {
            rejectTradeCalled = true
            lastRejectedTradeId = tradeId
        }
        override fun debugForwardGame() { debugForwardCalled = true }
        override fun debugSetupBankruptcy() { debugSetupCalled = true }
        override fun debugLandOnTaxField() { debugLandOnTaxCalled = true }
        override fun debugExecutePayMoney() { debugExecutePayMoneyCalled = true }
        override fun debugExecutePayPerBuilding() { debugExecutePayPerBuildingCalled = true }
        override fun debugExecutePayEach() { debugExecutePayEachCalled = true }
        override fun debugExecuteCollectFromEach() { debugExecuteCollectFromEachCalled = true }
        override fun debugForceDoublet() {}
        override fun debugForceJail() {}
        override fun debugForceBackwards() {}
        override fun debugForceGameOver() {}
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
    fun `debugLandOnTaxField is callable through interface`() {
        val service = TestGameService()
        service.debugLandOnTaxField()
        junit.framework.Assert.assertTrue(service.debugLandOnTaxCalled)
    }

    @Test
    fun `debugExecutePayMoney is callable through interface`() {
        val service = TestGameService()
        service.debugExecutePayMoney()
        junit.framework.Assert.assertTrue(service.debugExecutePayMoneyCalled)
    }

    @Test
    fun `debugExecutePayPerBuilding is callable through interface`() {
        val service = TestGameService()
        service.debugExecutePayPerBuilding()
        junit.framework.Assert.assertTrue(service.debugExecutePayPerBuildingCalled)
    }

    @Test
    fun `debugExecutePayEach is callable through interface`() {
        val service = TestGameService()
        service.debugExecutePayEach()
        junit.framework.Assert.assertTrue(service.debugExecutePayEachCalled)
    }

    @Test
    fun `debugExecuteCollectFromEach is callable through interface`() {
        val service = TestGameService()
        service.debugExecuteCollectFromEach()
        junit.framework.Assert.assertTrue(service.debugExecuteCollectFromEachCalled)
    }

    @Test
    fun `proposeTrade passes the full live trade offer to implementation`() {
        val service = TestGameService()
        service.proposeTrade(
            toPlayerId = "p2",
            offerMoney = 100,
            requestMoney = 20,
            offerPropertyIds = listOf(1, 3),
            requestPropertyIds = listOf(6),
            offerJailCards = 1,
            requestJailCards = 0
        )

        junit.framework.Assert.assertTrue(service.proposeTradeCalled)
        junit.framework.Assert.assertEquals("p2", service.lastTradeTargetId)
        junit.framework.Assert.assertEquals(100, service.lastTradeOfferMoney)
        junit.framework.Assert.assertEquals(20, service.lastTradeRequestMoney)
        junit.framework.Assert.assertEquals(listOf(1, 3), service.lastTradeOfferPropertyIds)
        junit.framework.Assert.assertEquals(listOf(6), service.lastTradeRequestPropertyIds)
        junit.framework.Assert.assertEquals(1, service.lastTradeOfferJailCards)
        junit.framework.Assert.assertEquals(0, service.lastTradeRequestJailCards)
    }

    @Test
    fun `acceptTrade and rejectTrade pass trade id to implementation`() {
        val service = TestGameService()

        service.acceptTrade("trade-1")
        service.rejectTrade("trade-2")

        junit.framework.Assert.assertTrue(service.acceptTradeCalled)
        junit.framework.Assert.assertEquals("trade-1", service.lastAcceptedTradeId)
        junit.framework.Assert.assertTrue(service.rejectTradeCalled)
        junit.framework.Assert.assertEquals("trade-2", service.lastRejectedTradeId)
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
