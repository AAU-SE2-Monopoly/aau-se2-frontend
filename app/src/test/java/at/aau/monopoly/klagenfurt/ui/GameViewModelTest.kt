package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.TradeOffer
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.GoField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: GameViewModel
    private val testDispatcher = StandardTestDispatcher()
    private var fakeTime = 1000L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeGameService()
        fakeService.currentPlayerId = "p1"
        fakeService.currentGameId = ""
        fakeTime = 2000L // Increased to avoid rollDice debounce (1500ms)

        viewModel = GameViewModel(fakeService, currentTimeProvider = { fakeTime })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- OVERLAY TESTS ---
    @Test
    fun `initial selectedPlayerForOverlay state should be null`() {
        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `showPlayerOverlay should update state with correct player`() {
        val testPlayer = Player(id = "p1", name = "Spieler 1")
        viewModel.showPlayerOverlay(testPlayer)
        assertEquals(testPlayer, viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `hidePlayerOverlay should reset state to null`() {
        val testPlayer = Player(id = "p1", name = "Spieler 1")
        viewModel.showPlayerOverlay(testPlayer)
        viewModel.hidePlayerOverlay()
        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `showTradeOverlay should hide player overlay and select trade partner`() {
        val overlayPlayer = Player(id = "p1", name = "Alice")
        val tradePartner = Player(id = "p2", name = "Bob")

        viewModel.showPlayerOverlay(overlayPlayer)
        viewModel.showTradeOverlay(tradePartner)

        assertNull(viewModel.selectedPlayerForOverlay.value)
        assertEquals(tradePartner, viewModel.selectedPlayerForTrade.value)
    }

    @Test
    fun `showTradeOverlay should ignore bankrupt trade partner`() {
        val tradePartner = Player(id = "p2", name = "Bob", money = 0, eliminated = true)

        viewModel.showTradeOverlay(tradePartner)

        assertNull(viewModel.selectedPlayerForTrade.value)
    }

    @Test
    fun `showTradeOverlay should ignore bankrupt current player`() = runTest {
        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":0,"eliminated":true},
              {"id":"p2","name":"Bob","money":300}
            ],
            "currentPlayerIndex": 0
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.showTradeOverlay(Player(id = "p2", name = "Bob", money = 300))

        assertNull(viewModel.selectedPlayerForTrade.value)
    }

    @Test
    fun `hideTradeOverlay should reset selected trade partner`() {
        val tradePartner = Player(id = "p2", name = "Bob")

        viewModel.showTradeOverlay(tradePartner)
        viewModel.hideTradeOverlay()

        assertNull(viewModel.selectedPlayerForTrade.value)
    }

    @Test
    fun `trade overlay uses its own timeout`() = runTest(testDispatcher) {
        val tradePartner = Player(id = "p2", name = "Bob")

        viewModel.showTradeOverlay(tradePartner)
        advanceTimeBy(5_000)

        assertEquals(tradePartner, viewModel.selectedPlayerForTrade.value)

        advanceTimeBy(55_000)
        runCurrent()

        assertNull(viewModel.selectedPlayerForTrade.value)
    }

    @Test
    fun `trade overlay timeout ignores stale timer after reopen`() = runTest(testDispatcher) {
        val firstPartner = Player(id = "p2", name = "Bob")
        val secondPartner = Player(id = "p3", name = "Carla")

        viewModel.showTradeOverlay(firstPartner)
        viewModel.hideTradeOverlay()
        advanceTimeBy(30_000)
        viewModel.showTradeOverlay(secondPartner)

        advanceTimeBy(30_000)
        runCurrent()

        assertEquals(secondPartner, viewModel.selectedPlayerForTrade.value)

        advanceTimeBy(30_000)
        runCurrent()

        assertNull(viewModel.selectedPlayerForTrade.value)
    }

    @Test
    fun `trade overlay timeout keeps overlay open while pending trade offer exists`() = runTest(testDispatcher) {
        val tradePartner = Player(id = "p2", name = "Bob")
        fakeService.currentPlayerId = "p1"
        fakeService.emitGameState(
            GameState(
                gameId = "game-1",
                players = mutableListOf(
                    Player(id = "p1", name = "Alice", money = 1500),
                    tradePartner
                ),
                currentPlayerIndex = 0,
                phase = GamePhase.BUYING,
                fields = listOf(GoField(0)),
                pendingTradeOffer = TradeOffer(
                    id = "trade-1",
                    fromPlayerId = "p1",
                    toPlayerId = "p2"
                )
            )
        )
        runCurrent()

        viewModel.showTradeOverlay(tradePartner)
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(tradePartner, viewModel.selectedPlayerForTrade.value)
    }

    @Test
    fun `connect should call gameService connect`() {
        viewModel.connect()
        assertTrue(fakeService.connectCalled)
    }

    @Test
    fun `createGame should call gameService createGame`() = runTest(testDispatcher) {
        val playerName = "Lukas"
        viewModel.createGame(playerName)
        advanceUntilIdle()
        assertEquals(1, fakeService.createGameCalls)
    }

    @Test
    fun `joinGame should call gameService joinGame`() = runTest(testDispatcher) {
        val gameId = "game123"
        val playerName = "Lukas"
        viewModel.joinGame(gameId, playerName)
        advanceUntilIdle()
        assertEquals(1, fakeService.joinGameCalls)
        assertEquals(gameId, fakeService.lastJoinedGameId)
    }

    @Test
    fun `failed join clears stale attempted game state after rollback`() = runTest(testDispatcher) {
        fakeService.currentGameId = "failed-game"
        fakeService.emitTestEvent(
            """
            {
              "event": "STATE_SNAPSHOT",
              "gameId": "failed-game",
              "gameState": {
                "gameId": "failed-game",
                "fields": [],
                "players": [{"id":"p1","name":"Alice"}],
                "currentPlayerIndex": 0,
                "phase": "BUYING"
              }
            }
            """.trimIndent()
        )
        advanceUntilIdle()

        assertEquals("failed-game", viewModel.gameState.value?.gameId)

        fakeService.joinGameSuccess = false
        fakeService.rollbackGameIdOnJoinFailure = "previous-game"

        viewModel.joinGame("failed-game", "Alice")
        advanceUntilIdle()

        assertEquals("previous-game", fakeService.currentGameId)
        assertNull(viewModel.gameState.value)
        assertTrue(fakeService.requestStateCalled)
    }

    @Test
    fun `startGame should call gameService startGame`() {
        viewModel.startGame()
        assertTrue(fakeService.startGameCalled)
    }

    @Test
    fun `endTurn should call gameService endTurn`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.endTurn()

        assertTrue(fakeService.endTurnCalled)
    }

    @Test
    fun `requestState should call gameService requestState`() {
        viewModel.requestState()
        assertTrue(fakeService.requestStateCalled)
    }

    @Test
    fun `setGameId should call gameService setGameId`() {
        viewModel.setGameId("game123")
        assertEquals("game123", fakeService.currentGameId)
    }

    @Test
    fun `proposeTrade should delegate complete offer to game service`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.proposeTrade(
            toPlayerId = "p2",
            offerMoney = 120,
            requestMoney = 30,
            offerPropertyIds = listOf(1, 3),
            requestPropertyIds = listOf(6),
            offerJailCards = 1,
            requestJailCards = 0
        )

        assertEquals("p2", fakeService.lastTradeTargetId)
        assertEquals(120, fakeService.lastTradeOfferMoney)
        assertEquals(30, fakeService.lastTradeRequestMoney)
        assertEquals(listOf(1, 3), fakeService.lastTradeOfferPropertyIds)
        assertEquals(listOf(6), fakeService.lastTradeRequestPropertyIds)
        assertEquals(1, fakeService.lastTradeOfferJailCards)
        assertEquals(0, fakeService.lastTradeRequestJailCards)
    }

    @Test
    fun `proposeTrade should delegate while current player has pending payment`() = runTest(testDispatcher) {
        seedGameState(
            phase = "PAYING_RENT",
            extraState = """,
                "pendingPayment": {
                  "amount": 500,
                  "source": "RENT",
                  "sourceFieldId": 1,
                  "creditorPlayerId": "p2",
                  "debtorPlayerId": "p1"
                }
            """.trimIndent()
        )

        assertTrue(viewModel.actionGates.value.canTrade)

        viewModel.proposeTrade(
            toPlayerId = "p2",
            offerMoney = 0,
            requestMoney = 500,
            offerPropertyIds = emptyList(),
            requestPropertyIds = emptyList(),
            offerJailCards = 0,
            requestJailCards = 0
        )

        assertEquals("p2", fakeService.lastTradeTargetId)
        assertEquals(0, fakeService.lastTradeOfferMoney)
        assertEquals(500, fakeService.lastTradeRequestMoney)
    }

    @Test
    fun `proposeTrade should not delegate when current player is bankrupt`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":0,"eliminated":true},
              {"id":"p2","name":"Bob","money":300}
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING",
            "pendingTradeOffer": {"id":"trade-1","fromPlayerId":"p1","toPlayerId":"p2"}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.proposeTrade(
            toPlayerId = "p2",
            offerMoney = 10,
            requestMoney = 0,
            offerPropertyIds = emptyList(),
            requestPropertyIds = emptyList(),
            offerJailCards = 0,
            requestJailCards = 0
        )

        assertNull(fakeService.lastTradeTargetId)
    }

    @Test
    fun `proposeTrade should not delegate when trade partner is bankrupt`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":500},
              {"id":"p2","name":"Bob","money":0,"eliminated":true}
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.proposeTrade(
            toPlayerId = "p2",
            offerMoney = 10,
            requestMoney = 0,
            offerPropertyIds = emptyList(),
            requestPropertyIds = emptyList(),
            offerJailCards = 0,
            requestJailCards = 0
        )

        assertNull(fakeService.lastTradeTargetId)
    }

    @Test
    fun `proposeTrade should delegate when state contains live players`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":500},
              {"id":"p2","name":"Bob","money":300}
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.proposeTrade(
            toPlayerId = "p2",
            offerMoney = 10,
            requestMoney = 5,
            offerPropertyIds = emptyList(),
            requestPropertyIds = emptyList(),
            offerJailCards = 0,
            requestJailCards = 0
        )

        assertEquals("p2", fakeService.lastTradeTargetId)
        assertEquals(10, fakeService.lastTradeOfferMoney)
        assertEquals(5, fakeService.lastTradeRequestMoney)
    }

    @Test
    fun `acceptTrade and rejectTrade should delegate trade id to game service`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.acceptTrade("trade-1")

        assertTrue(fakeService.acceptTradeCalled)
        assertEquals("trade-1", fakeService.lastAcceptedTradeId)

        fakeService.emitTestEvent("""
        {
          "event": "TRADE_COMPLETED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice"},{"id":"p2","name":"Bob"}],
            "currentPlayerIndex": 0,
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.rejectTrade("trade-2")

        assertTrue(fakeService.rejectTradeCalled)
        assertEquals("trade-2", fakeService.lastRejectedTradeId)
    }

    @Test
    fun `trade actions use separate timeout before allowing another trade action`() = runTest(testDispatcher) {
        val gatesJob = launch { viewModel.actionGates.collect {} }
        seedGameState(phase = "BUYING")

        viewModel.acceptTrade("trade-1")
        viewModel.rejectTrade("trade-2")
        runCurrent()

        assertTrue(fakeService.acceptTradeCalled)
        assertFalse(fakeService.rejectTradeCalled)
        assertTrue(viewModel.tradeActionInFlight.value)
        assertFalse(viewModel.actionGates.value.canTrade)

        advanceTimeBy(5_000)

        assertTrue(viewModel.tradeActionInFlight.value)
        assertFalse(fakeService.rejectTradeCalled)

        advanceTimeBy(15_000)
        runCurrent()

        assertFalse(viewModel.tradeActionInFlight.value)

        viewModel.rejectTrade("trade-2")

        assertTrue(fakeService.rejectTradeCalled)
        assertEquals("trade-2", fakeService.lastRejectedTradeId)
        gatesJob.cancel()
    }

    @Test
    fun `TRADE_COMPLETED should clear selected trade partner`() = runTest {
        val job = launch { viewModel.selectedPlayerForTrade.collect {} }
        viewModel.showTradeOverlay(Player(id = "p2", name = "Bob"))

        fakeService.emitTestEvent("""
        {
          "event": "TRADE_COMPLETED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice"},{"id":"p2","name":"Bob"}],
            "currentPlayerIndex": 0
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertNull(viewModel.selectedPlayerForTrade.value)
        assertTrue(fakeService.requestStateCalled)
        job.cancel()
    }

    @Test
    fun `TRADE_REJECTED should clear selected trade partner`() = runTest {
        val job = launch { viewModel.selectedPlayerForTrade.collect {} }
        viewModel.showTradeOverlay(Player(id = "p2", name = "Bob"))

        fakeService.emitTestEvent("""
        {
          "event": "TRADE_REJECTED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice"},{"id":"p2","name":"Bob"}],
            "currentPlayerIndex": 0
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertNull(viewModel.selectedPlayerForTrade.value)
        assertTrue(fakeService.requestStateCalled)
        job.cancel()
    }

    @Test
    fun `TRADE_REJECTED should finish trade action without waiting for timeout`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")
        viewModel.acceptTrade("trade-1")
        runCurrent()

        assertTrue(viewModel.tradeActionInFlight.value)

        fakeService.emitTestEvent("""
        {
          "event": "TRADE_REJECTED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice"},{"id":"p2","name":"Bob"}],
            "currentPlayerIndex": 0,
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.tradeActionInFlight.value)
    }

    @Test
    fun `unrelated game event should keep selected trade partner`() = runTest(testDispatcher) {
        val job = launch { viewModel.selectedPlayerForTrade.collect {} }
        val tradePartner = Player(id = "p2", name = "Bob")
        viewModel.showTradeOverlay(tradePartner)

        fakeService.emitTestEvent("""
        {
          "event": "TRADE_UPDATED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice"},{"id":"p2","name":"Bob"}],
            "currentPlayerIndex": 0
          }
        }
        """.trimIndent())
        runCurrent()

        assertEquals(tradePartner, viewModel.selectedPlayerForTrade.value)
        job.cancel()
    }

    // --- FACTORY TEST ---

    @Test
    fun `Factory creates GameViewModel successfully`() {
        val factory = GameViewModel.Factory(fakeService)
        val createdViewModel = factory.create(GameViewModel::class.java)
        assertTrue(createdViewModel is GameViewModel)
    }

    // --- DICE CHEAT TESTS ---

    @Test
    fun `rollDice should call service`() = runTest(testDispatcher) {
        seedGameState(phase = "ROLLING")

        viewModel.rollDice()

        assertTrue(fakeService.rollDiceCalled)
    }


    // --- PROPERTY GETTER TESTS ---

    @Test
    fun `currentPlayerId getter should return value from service`() {
        fakeService.currentPlayerId = "Alice"
        assertEquals("Alice", viewModel.currentPlayerId)
    }

    @Test
    fun `events and status flows should delegate to gameService`() {
        assertEquals(fakeService.events, viewModel.events)
        assertEquals(fakeService.status, viewModel.status)
    }

    // --- SYNC STATE TESTS ---

    @Test
    fun `syncGameboardEntryState should do nothing if currentGameId is blank`() {
        fakeService.currentGameId = ""
        fakeService.requestStateCalled = false
        viewModel.syncGameboardEntryState()
        assertFalse(fakeService.requestStateCalled)
    }

    @Test
    fun `syncGameboardEntryState should call requestState if currentGameId is set`() {
        fakeService.currentGameId = "active-game-id"
        fakeService.requestStateCalled = false
        viewModel.syncGameboardEntryState()
        assertTrue(fakeService.requestStateCalled)
    }

    // --- FLOW & JSON PARSING TESTS (Für 100% Coverage) ---

    @Test
    fun `malformed JSON in events should be caught and ignored`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        fakeService.emitTestEvent("""{ invalid json """)
        advanceUntilIdle()
        assertNull(viewModel.gameState.value)

        job.cancel()
    }

    @Test
    fun `init block should auto-capture gameId on GAME_CREATED event`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        fakeService.currentGameId = ""
        fakeService.emitTestEvent("""{"event":"GAME_CREATED","gameId":"new-game-id"}""")
        advanceUntilIdle()

        assertEquals("new-game-id", fakeService.currentGameId)

        job.cancel()
    }

    @Test
    fun `gameState ignores events from a different gameId`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        fakeService.currentGameId = "my-game-id"
        // Event gehört zu anderem Spiel, sollte ignoriert werden
        fakeService.emitTestEvent("""{"event":"STATE_UPDATED","gameId":"other-game-id","gameState":{"phase":"ROLLING"}}""")
        advanceUntilIdle()

        assertNull(viewModel.gameState.value)

        job.cancel()
    }

    @Test
    fun `eventLog resets when new game is created`() = runTest {
        val job = launch { viewModel.eventLog.collect {} }

        fakeService.emitTestEvent("""{"event":"PLAYER_JOINED","gameId":"g1","message":"A joined"}""")
        advanceUntilIdle()
        assertEquals(1, viewModel.eventLog.value.size)

        fakeService.emitTestEvent("""{"event":"GAME_CREATED","gameId":"g2","message":"New game"}""")
        advanceUntilIdle()

        assertEquals(1, viewModel.eventLog.value.size)
        assertEquals("New game", viewModel.eventLog.value.last().text)

        job.cancel()
    }

    @Test
    fun `eventLog keeps only last 80 entries`() = runTest {
        val job = launch { viewModel.eventLog.collect {} }
        advanceUntilIdle()

        for (i in 1..85) {
            fakeService.emitTestEvent("""{"event":"TURN_ENDED","gameId":"g1","message":"Entry $i"}""")
            // IMPORTANT: Allow the collector to process each event in the fold
            // especially since we are using StandardTestDispatcher and stateIn
            advanceUntilIdle()
        }

        assertEquals(80, viewModel.eventLog.value.size)
        assertEquals("Entry 6", viewModel.eventLog.value.first().text)
        assertEquals("Entry 85", viewModel.eventLog.value.last().text)

        job.cancel()
    }

    @Test
    fun `isGameReady state transitions correctly`() = runTest {
        val job = launch { viewModel.isGameReady.collect {} }
        assertFalse(viewModel.isGameReady.value)

        // Must provide required fields (gameId, fields) for successful parsing
        fakeService.emitTestEvent("""{"event":"STATE_UPDATED","gameId":"g1","gameState":{"gameId":"g1","fields":[],"phase":"ROLLING"}}""")
        advanceUntilIdle()
        assertTrue(viewModel.isGameReady.value)

        job.cancel()
    }

    @Test
    fun `fields returns empty when no game state exists`() = runTest {
        val job = launch { viewModel.fields.collect {} }
        assertTrue(viewModel.fields.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `eventLog processes log events and formats human readable text`() = runTest {
        val job = launch { viewModel.eventLog.collect {} }

        fakeService.emitTestEvent("""{"event":"PLAYER_JOINED","gameId":"g1"}""")
        advanceUntilIdle()
        assertEquals("A new player joined", viewModel.eventLog.value.last().text)
        assertFalse(viewModel.eventLog.value.last().isTechnical)

        fakeService.emitTestEvent("""{"event":"CUSTOM_EVENT","gameId":"g1"}""")
        advanceUntilIdle()
        assertEquals("Custom event", viewModel.eventLog.value.last().text)

        fakeService.emitTestEvent("""{"event":"STATE_SNAPSHOT","gameId":"g1"}""")
        advanceUntilIdle()
        assertTrue(viewModel.eventLog.value.last().isTechnical)
        assertEquals("State snapshot synced", viewModel.eventLog.value.last().text)

        fakeService.emitTestEvent("""{"event":"SOME_EVENT","gameId":"g1","message":"Direct message"}""")
        advanceUntilIdle()
        assertEquals("Direct message", viewModel.eventLog.value.last().text)

        job.cancel()
    }


    @Test
    fun `dismissActionCard should clear visible action card`() {
        val card = at.aau.monopoly.klagenfurt.model.card.ChanceCard(
            id = 1,
            description = "Collect money",
            action = at.aau.monopoly.klagenfurt.model.enums.CardAction.COLLECT_MONEY,
            amount = 100
        )

        viewModel.setCurrentActionCard(card)
        assertEquals(card, viewModel.currentActionCard.value)

        viewModel.dismissActionCard()

        assertNull(viewModel.visibleActionCard.value)
        assertEquals(card, viewModel.currentActionCard.value)
    }

    @Test
    fun `setCurrentActionCard should update current action card`() {
        val card = at.aau.monopoly.klagenfurt.model.card.CommunityChestCard(
            id = 2,
            description = "Pay money",
            action = at.aau.monopoly.klagenfurt.model.enums.CardAction.PAY_MONEY,
            amount = 50
        )

        viewModel.setCurrentActionCard(card)

        assertEquals(card, viewModel.currentActionCard.value)
    }

    @Test
    fun `ACTION_DRAWN event should update currentActionCard`() = runTest {
        val job = launch { viewModel.currentActionCard.collect {} }

        fakeService.emitTestEvent(
            """
        {
          "event": "ACTION_DRAWN",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [],
            "phase": "BUYING",
            "currentActionCard": {
              "type": "CHANCE",
              "id": 1,
              "description": "Collect 100",
              "action": "COLLECT_MONEY",
              "amount": 100
            }
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertEquals("Collect 100", viewModel.currentActionCard.value?.description)

        job.cancel()
    }

    @Test
    fun `community chest draw gate uses field id when fields are sparse`() = runTest(testDispatcher) {
        val job = launch { viewModel.actionGates.collect {} }
        fakeService.currentPlayerId = "p1"
        fakeService.currentGameId = "game-1"
        fakeService.emitGameState(
            GameState(
                gameId = "game-1",
                players = mutableListOf(Player(id = "p1", name = "Alice", position = 2, money = 1500)),
                currentPlayerIndex = 0,
                phase = GamePhase.BUYING,
                fields = listOf(
                    GoField(id = 0),
                    CommunityChestField(id = 2)
                )
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.actionGates.value.canDrawCommunityChest)
        assertFalse(viewModel.actionGates.value.canDrawChance)

        job.cancel()
    }

    @Test
    fun `canStartGame should be false for host with only 1 player in WAITING phase`() = runTest {
        val job = launch { viewModel.canStartGame.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" }
            ],
            "phase": "WAITING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertFalse(viewModel.canStartGame.value)

        job.cancel()
    }

    @Test
    fun `canStartGame should be true for host with 2 players in WAITING phase`() = runTest {
        val job = launch { viewModel.canStartGame.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" },
              { "id": "p2", "name": "Bob" }
            ],
            "phase": "WAITING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertTrue(viewModel.canStartGame.value)

        job.cancel()
    }

    @Test
    fun `canStartGame should be false for non-host even with 2 players in WAITING phase`() = runTest {
        val job = launch { viewModel.canStartGame.collect {} }

        fakeService.currentPlayerId = "p2"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" },
              { "id": "p2", "name": "Bob" }
            ],
            "phase": "WAITING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertFalse(viewModel.canStartGame.value)

        job.cancel()
    }

    @Test
    fun `canStartGame should be false for host with 2 players in ROLLING phase`() = runTest {
        val job = launch { viewModel.canStartGame.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" },
              { "id": "p2", "name": "Bob" }
            ],
            "phase": "ROLLING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertFalse(viewModel.canStartGame.value)

        job.cancel()
    }

    @Test
    fun `canStartGame should be true for host with 3 players in WAITING phase`() = runTest {
        val job = launch { viewModel.canStartGame.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" },
              { "id": "p2", "name": "Bob" },
              { "id": "p3", "name": "Charlie" }
            ],
            "phase": "WAITING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertTrue(viewModel.canStartGame.value)

        job.cancel()
    }

    @Test
    fun `isRollingPhaseForCurrentPlayer should be true for current player in ROLLING phase`() = runTest {
        val job = launch { viewModel.isRollingPhaseForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" }
            ],
            "currentPlayerIndex": 0,
            "phase": "ROLLING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertTrue(viewModel.isRollingPhaseForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `isRollingPhaseForCurrentPlayer should be false for BUYING phase`() = runTest {
        val job = launch { viewModel.isRollingPhaseForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" }
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertFalse(viewModel.isRollingPhaseForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `isBuyingPhaseForCurrentPlayer should be true for current player in BUYING phase`() = runTest {
        val job = launch { viewModel.isBuyingPhaseForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" }
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertTrue(viewModel.isBuyingPhaseForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `isBuyingPhaseForCurrentPlayer should be false for non current player in BUYING phase`() = runTest {
        val job = launch { viewModel.isBuyingPhaseForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p2"

        fakeService.emitTestEvent(
            """
            {
              "event": "RENT_DUE",
              "gameId": "g1",
              "gameState": {
                "gameId": "g1",
                "fields": [],
                "players": [{"id":"p1","name":"Alice","money":500,"ownedPropertyIds":[]}],
                "currentPlayerIndex": 0,
                "phase": "PAYING_RENT",
                "pendingPayment": {
                  "amount": 100,
                  "source": "RENT",
                  "sourceFieldId": 5,
                  "creditorPlayerId": "p2",
                  "goesToFreeParking": false
                },
                "lastDiceRoll": {"die1":3,"die2":4}
              }
            }
            """.trimIndent()
        )

        advanceUntilIdle()

        assertFalse(viewModel.isBuyingPhaseForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `isBuyingPhaseForCurrentPlayer should be false for current player in ROLLING phase`() = runTest {
        val job = launch { viewModel.isBuyingPhaseForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" }
            ],
            "currentPlayerIndex": 0,
            "phase": "ROLLING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertFalse(viewModel.isBuyingPhaseForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `showDiceOverlayForCurrentPlayer should be true during rolling phase`() = runTest {
        val job = launch { viewModel.showDiceOverlayForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" }
            ],
            "currentPlayerIndex": 0,
            "phase": "ROLLING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertTrue(viewModel.showDiceOverlayForCurrentPlayer.value)

        job.cancel()
    }


    @Test
    fun `diceResultForCurrentPlayer should return dice roll in buying phase`() = runTest {
        val job = launch { viewModel.diceResultForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              { "id": "p1", "name": "Alice" }
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING",
            "lastDiceRoll": {
              "die1": 5,
              "die2": 6
            }
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertEquals(11, viewModel.diceResultForCurrentPlayer.value?.total)

        job.cancel()
    }

    @Test
    fun `buyProperty should call gameService with correct field id`() = runTest(testDispatcher) {
        seedGameState(
            phase = "BUYING",
            fieldsJson = """[
              {"id":5,"name":"Buyable","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":null,"houses":0,"hasHotel":false,"isMortgaged":false}
            ]"""
        )

        viewModel.buyProperty(5)

        assertTrue(fakeService.buyPropertyCalled)
        assertEquals(5, fakeService.lastBoughtFieldId)
    }
    @Test
    fun `RENT_PAID event should clear all payment state flows`() = runTest {
        val job = launch {
            viewModel.showPayRentOverlay.collect {}
            viewModel.currentRentAmount.collect {}
            viewModel.currentRentOwnerId.collect {}
            viewModel.currentRentFieldId.collect {}
        }

        // First set non-default values via RENT_DUE
        fakeService.emitTestEvent(
            """
{
  "event": "RENT_DUE",
  "gameId": "g1",
  "gameState": {
    "gameId": "g1",
    "fields": [],
    "players": [{"id":"p1","name":"Alice","money":500,"ownedPropertyIds":[]}],
    "currentPlayerIndex": 0,
    "phase": "PAYING_RENT",
    "pendingPayment": {
      "amount": 100,
      "source": "RENT",
      "sourceFieldId": 5,
      "creditorPlayerId": "p2",
      "goesToFreeParking": false
    },
    "lastDiceRoll": {"die1":3,"die2":4}
  }
}
        """.trimIndent()
        )
        advanceUntilIdle()

        assertTrue(viewModel.showPayRentOverlay.value)
        assertEquals(100, viewModel.currentRentAmount.value)
        assertEquals("p2", viewModel.currentRentOwnerId.value)
        assertEquals(5, viewModel.currentRentFieldId.value)

        // Now send RENT_PAID
        fakeService.emitTestEvent(
            """
        {
          "event": "RENT_PAID",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":400,"ownedPropertyIds":[]}],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "pendingPayment": null,
            "lastDiceRoll": null
          }
        }
        """.trimIndent()
        )
        advanceUntilIdle()

        assertFalse(viewModel.showPayRentOverlay.value)
        assertEquals(0, viewModel.currentRentAmount.value)
        assertEquals(null, viewModel.currentRentOwnerId.value)
        assertEquals(null, viewModel.currentRentFieldId.value)

        job.cancel()
    }

    @Test
    fun `setSelectedPlayerForOverlay should update selected player`() {
        val player = Player(id = "p99", name = "Overlay Test")

        viewModel.showPlayerOverlay(player)

        assertEquals(player, viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `hidePlayerOverlay should clear selected player`() {
        val player = Player(id = "p99", name = "Overlay Test")

        viewModel.showPlayerOverlay(player)
        viewModel.hidePlayerOverlay()

        assertNull(viewModel.selectedPlayerForOverlay.value)
    }

    @Test
    fun `setCurrentActionCard should update action card state`() {
        val card = at.aau.monopoly.klagenfurt.model.card.ChanceCard(
            id = 99,
            description = "Test Card",
            action = at.aau.monopoly.klagenfurt.model.enums.CardAction.COLLECT_MONEY,
            amount = 200
        )

        viewModel.setCurrentActionCard(card)

        assertEquals(card, viewModel.currentActionCard.value)
    }

    @Test
    fun `dismissActionCard should clear visible action card state`() {
        val card = at.aau.monopoly.klagenfurt.model.card.ChanceCard(
            id = 99,
            description = "Test Card",
            action = at.aau.monopoly.klagenfurt.model.enums.CardAction.COLLECT_MONEY,
            amount = 200
        )

        viewModel.setCurrentActionCard(card)
        viewModel.dismissActionCard()

        assertNull(viewModel.visibleActionCard.value)
        assertEquals(card, viewModel.currentActionCard.value)
    }

    @Test
    fun `buyHouse should set building action pending to true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.buyHouse(1)

        assertTrue(viewModel.buildingActionPending.value)
    }

    @Test
    fun `buyHotel should set building action pending to true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.buyHotel(1)

        assertTrue(viewModel.buildingActionPending.value)
    }

    @Test
    fun `sellHouse should set building action pending to true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.sellHouse(1)

        assertTrue(viewModel.buildingActionPending.value)
    }

    @Test
    fun `sellHotel should set building action pending to true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.sellHotel(1)

        assertTrue(viewModel.buildingActionPending.value)
    }

    @Test
    fun `HOUSE_SOLD clears property and building action locks`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        viewModel.sellHouse(1)

        assertTrue(viewModel.propertyActionInFlight.value)
        assertTrue(viewModel.buildingActionPending.value)

        fakeService.emitTestEvent("""
        {
          "event": "HOUSE_SOLD",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [],
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.propertyActionInFlight.value)
        assertFalse(viewModel.buildingActionPending.value)
    }

    @Test
    fun `PAYMENT_FAILED shows error message`() = runTest {
        val captured = mutableListOf<String?>()
        val job = launch { viewModel.errorMessage.collect { captured.add(it) } }
        advanceUntilIdle()

        fakeService.emitTestEvent("""
        {
          "event": "PAYMENT_FAILED",
          "gameId": "g1",
          "message": "Insufficient funds. Need 100 but have 50."
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(captured.any { it?.contains("Insufficient") == true })

        job.cancel()
    }

    @Test
    fun `BANKRUPTCY_DECLARED sets showBankruptcyOverlay to true`() = runTest {
        val job = launch { viewModel.showBankruptcyOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":0}],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "pendingPayment": null,
            "bankruptcyTotalAssets": 50,
            "bankruptcyTotalDebt": 100,
            "bankruptcyPropertiesCount": 1,
            "bankruptcyOwnedFieldIds": [1],
            "lastDiceRoll": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showBankruptcyOverlay.value)

        job.cancel()
    }

    @Test
    fun `BANKRUPTCY_DECLARED populates bankruptcyPlayerName from bankruptcyPlayerId`() = runTest {
        val job = launch { viewModel.bankruptcyPlayerName.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"BankruptPlayer"}, {"id":"p2","name":"NextPlayer"}],
            "currentPlayerIndex": 1,
            "phase": "ROLLING",
            "bankruptcyTotalAssets": 50,
            "bankruptcyTotalDebt": 100,
            "bankruptcyPropertiesCount": 1,
            "bankruptcyOwnedFieldIds": [],
            "bankruptcyPlayerId": "p1"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertEquals("BankruptPlayer", viewModel.bankruptcyPlayerName.value)

        job.cancel()
    }

    @Test
    fun `BANKRUPTCY_DECLARED populates bankruptcyTotalAssets correctly`() = runTest {
        val job = launch { viewModel.bankruptcyTotalAssets.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice"}],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "bankruptcyTotalAssets": 500,
            "bankruptcyTotalDebt": 1200,
            "bankruptcyPropertiesCount": 3,
            "bankruptcyOwnedFieldIds": []
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertEquals(500, viewModel.bankruptcyTotalAssets.value)

        job.cancel()
    }

    @Test
    fun `PROPERTY_MORTGAGED clears propertyActionInFlight`() = runTest {
        val job = launch { viewModel.propertyActionInFlight.collect {} }
        seedGameState(phase = "BUYING")

        viewModel.mortgageProperty(1)
        assertTrue(viewModel.propertyActionInFlight.value)

        fakeService.emitTestEvent("""
        {
          "event": "PROPERTY_MORTGAGED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [],
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.propertyActionInFlight.value)

        job.cancel()
    }

    @Test
    fun `PROPERTY_UNMORTGAGED clears propertyActionInFlight`() = runTest {
        val job = launch { viewModel.propertyActionInFlight.collect {} }
        seedGameState(phase = "BUYING")

        viewModel.unmortgageProperty(1)
        assertTrue(viewModel.propertyActionInFlight.value)

        fakeService.emitTestEvent("""
        {
          "event": "PROPERTY_UNMORTGAGED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [],
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.propertyActionInFlight.value)

        job.cancel()
    }

    @Test
    fun `hasPendingPayment true when pendingPayment amount is positive`() = runTest {
        val job = launch { viewModel.hasPendingPayment.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2"
            },
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.hasPendingPayment.value)

        job.cancel()
    }

    @Test
    fun `hasPendingPayment false when pendingPayment is null`() = runTest {
        val job = launch { viewModel.hasPendingPayment.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_PAID",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":400}],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "pendingPayment": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.hasPendingPayment.value)

        job.cancel()
    }

    @Test
    fun `canEndTurnForCurrentPlayer false when hasPendingPayment is true`() = runTest {
        val job = launch { viewModel.canEndTurnForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.dismissPayRentOverlay()

        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "BUYING",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.hasPendingPayment.value)
        assertFalse(viewModel.showPayRentOverlay.value)
        assertFalse(viewModel.canEndTurnForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `canEndTurnForCurrentPlayer false when showPayRentOverlay is true`() = runTest {
        val job = launch { viewModel.canEndTurnForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.canEndTurnForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `canEndTurnForCurrentPlayer false when showBankruptcyOverlay is true`() = runTest {
        val job = launch { viewModel.canEndTurnForCurrentPlayer.collect {} }

        fakeService.currentPlayerId = "p1"

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":0}],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "bankruptcyTotalAssets": 0,
            "bankruptcyTotalDebt": 100,
            "bankruptcyPropertiesCount": 0,
            "bankruptcyOwnedFieldIds": []
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.canEndTurnForCurrentPlayer.value)

        job.cancel()
    }

    @Test
    fun `payRent does nothing when paymentActionInFlight is true`() = runTest {
        val job = launch { viewModel.currentRentAmount.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        fakeService.payRentCalled = false
        viewModel.payRent()
        assertTrue(fakeService.payRentCalled)

        fakeService.payRentCalled = false
        viewModel.payRent()
        assertFalse(fakeService.payRentCalled)

        job.cancel()
    }

    @Test
    fun `declareBankruptcy shows confirmation instead of sending to backend`() = runTest {
        val job = launch { viewModel.currentRentAmount.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        fakeService.declareBankruptcyCalled = false
        viewModel.declareBankruptcy()
        assertFalse(fakeService.declareBankruptcyCalled)
        assertTrue(viewModel.showBankruptcyConfirmation.value)

        job.cancel()
    }

    @Test
    fun `declareBankruptcy uses pending payment even when action gate has not opened`() = runTest {
        val job = launch { viewModel.showBankruptcyConfirmation.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.dismissPayRentOverlay()

        assertFalse(viewModel.showPayRentOverlay.value)

        viewModel.declareBankruptcy()

        assertTrue(viewModel.showBankruptcyConfirmation.value)

        job.cancel()
    }

    @Test
    fun `declareBankruptcy is blocked when player can pay with cash`() = runTest {
        val job = launch { viewModel.showBankruptcyConfirmation.collect {} }
        val canRaiseFundsJob = launch { viewModel.canRaiseFunds.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":1450}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            },
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.actionGates.value.canPayRent)
        assertTrue(viewModel.canRaiseFunds.value)
        assertFalse(viewModel.actionGates.value.canDeclareBankruptcy)

        fakeService.declareBankruptcyCalled = false
        viewModel.declareBankruptcy()
        assertFalse(fakeService.declareBankruptcyCalled)
        assertFalse(viewModel.showBankruptcyConfirmation.value)

        job.cancel()
        canRaiseFundsJob.cancel()
    }

    @Test
    fun `confirmDeclareBankruptcy sends action to backend`() = runTest {
        val job = launch { viewModel.showBankruptcyConfirmation.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        fakeService.declareBankruptcyCalled = false
        viewModel.declareBankruptcy()
        advanceUntilIdle()
        viewModel.confirmDeclareBankruptcy()
        assertTrue(fakeService.declareBankruptcyCalled)
        assertFalse(viewModel.showBankruptcyConfirmation.value)

        job.cancel()
    }

    @Test
    fun `confirmDeclareBankruptcy sends action immediately after opening confirmation`() = runTest {
        val job = launch { viewModel.showBankruptcyConfirmation.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        fakeService.declareBankruptcyCalled = false
        viewModel.declareBankruptcy()
        viewModel.confirmDeclareBankruptcy()

        assertTrue(fakeService.declareBankruptcyCalled)
        assertFalse(viewModel.showBankruptcyConfirmation.value)

        job.cancel()
    }

    @Test
    fun `bankruptcy confirmation dismiss after confirm does not reopen pay rent overlay`() = runTest {
        val overlayJob = launch { viewModel.showPayRentOverlay.collect {} }
        val confirmationJob = launch { viewModel.showBankruptcyConfirmation.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.declareBankruptcy()
        viewModel.confirmDeclareBankruptcy()
        viewModel.cancelDeclareBankruptcy()

        assertTrue(fakeService.declareBankruptcyCalled)
        assertFalse(viewModel.showBankruptcyConfirmation.value)
        assertFalse(viewModel.showPayRentOverlay.value)

        overlayJob.cancel()
        confirmationJob.cancel()
    }

    @Test
    fun `confirmed bankruptcy keeps pay rent closed and advances to next player`() = runTest {
        val overlayJob = launch { viewModel.showPayRentOverlay.collect {} }
        val confirmationJob = launch { viewModel.showBankruptcyConfirmation.collect {} }
        val bankruptcyJob = launch { viewModel.showBankruptcyOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":50},
              {"id":"p2","name":"Bob","money":500},
              {"id":"p3","name":"Charlie","money":500}
            ],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showPayRentOverlay.value)

        viewModel.declareBankruptcy()
        viewModel.confirmDeclareBankruptcy()

        assertTrue(fakeService.declareBankruptcyCalled)
        assertFalse(viewModel.showPayRentOverlay.value)
        assertFalse(viewModel.showBankruptcyConfirmation.value)

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":0,"eliminated":true},
              {"id":"p2","name":"Bob","money":500},
              {"id":"p3","name":"Charlie","money":500}
            ],
            "currentPlayerIndex": 1,
            "phase": "ROLLING",
            "pendingPayment": null,
            "bankruptcyPlayerId": "p1",
            "bankruptcyTotalAssets": 50,
            "bankruptcyTotalDebt": 100,
            "bankruptcyPropertiesCount": 0,
            "bankruptcyOwnedFieldIds": []
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.showPayRentOverlay.value)
        assertNull(viewModel.visiblePaymentState.value)
        assertFalse(viewModel.hasPendingPayment.value)
        assertFalse(viewModel.showBankruptcyConfirmation.value)
        assertTrue(viewModel.showBankruptcyOverlay.value)
        assertEquals("p2", viewModel.gameState.value?.currentPlayer?.id)
        assertEquals(GamePhase.ROLLING, viewModel.gameState.value?.phase)

        viewModel.dismissBankruptcyOverlay()
        advanceUntilIdle()

        assertFalse(viewModel.showPayRentOverlay.value)
        assertEquals("p2", viewModel.gameState.value?.currentPlayer?.id)

        overlayJob.cancel()
        confirmationJob.cancel()
        bankruptcyJob.cancel()
    }

    @Test
    fun `confirmed tax bankruptcy keeps payment closed and advances to next player`() = runTest {
        val overlayJob = launch { viewModel.showPayRentOverlay.collect {} }
        val confirmationJob = launch { viewModel.showBankruptcyConfirmation.collect {} }
        val bankruptcyJob = launch { viewModel.showBankruptcyOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "TAX_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":50},
              {"id":"p2","name":"Bob","money":500},
              {"id":"p3","name":"Charlie","money":500}
            ],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "TAX",
              "sourceFieldId": 4,
              "creditorPlayerId": null,
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showPayRentOverlay.value)
        assertFalse(viewModel.canRaiseFunds.value)
        assertTrue(viewModel.actionGates.value.canDeclareBankruptcy)

        viewModel.declareBankruptcy()
        viewModel.confirmDeclareBankruptcy()

        assertTrue(fakeService.declareBankruptcyCalled)
        assertFalse(viewModel.showPayRentOverlay.value)
        assertFalse(viewModel.showBankruptcyConfirmation.value)

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":0,"eliminated":true},
              {"id":"p2","name":"Bob","money":500},
              {"id":"p3","name":"Charlie","money":500}
            ],
            "currentPlayerIndex": 1,
            "phase": "ROLLING",
            "pendingPayment": null,
            "bankruptcyPlayerId": "p1",
            "bankruptcyTotalAssets": 50,
            "bankruptcyTotalDebt": 100,
            "bankruptcyPropertiesCount": 0,
            "bankruptcyOwnedFieldIds": []
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.showPayRentOverlay.value)
        assertNull(viewModel.visiblePaymentState.value)
        assertFalse(viewModel.hasPendingPayment.value)
        assertTrue(viewModel.showBankruptcyOverlay.value)
        assertEquals("p2", viewModel.gameState.value?.currentPlayer?.id)
        assertEquals(GamePhase.ROLLING, viewModel.gameState.value?.phase)

        overlayJob.cancel()
        confirmationJob.cancel()
        bankruptcyJob.cancel()
    }

    @Test
    fun `stale snapshot after bankruptcy confirmation does not reopen pay rent overlay`() = runTest {
        val overlayJob = launch { viewModel.showPayRentOverlay.collect {} }
        val confirmationJob = launch { viewModel.showBankruptcyConfirmation.collect {} }

        val rentDueState = """
        {
          "gameId": "g1",
          "fields": [],
          "players": [
            {"id":"p1","name":"Alice","money":50},
            {"id":"p2","name":"Bob","money":500},
            {"id":"p3","name":"Charlie","money":500}
          ],
          "currentPlayerIndex": 0,
          "phase": "PAYING_RENT",
          "pendingPayment": {
            "amount": 100,
            "source": "RENT",
            "sourceFieldId": 5,
            "creditorPlayerId": "p2",
            "debtorPlayerId": "p1",
            "debtorCanPayAfterAssets": false
          }
        }
        """.trimIndent()

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": $rentDueState
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showPayRentOverlay.value)

        viewModel.declareBankruptcy()
        viewModel.confirmDeclareBankruptcy()

        assertFalse(viewModel.showPayRentOverlay.value)
        assertFalse(viewModel.showBankruptcyConfirmation.value)

        fakeService.emitTestEvent("""
        {
          "event": "STATE_SNAPSHOT",
          "gameId": "g1",
          "gameState": $rentDueState
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.showPayRentOverlay.value)
        assertNull(viewModel.visiblePaymentState.value)
        assertTrue(viewModel.hasPendingPayment.value)

        overlayJob.cancel()
        confirmationJob.cancel()
    }

    @Test
    fun `bankruptcy confirmation error restores payment overlay`() = runTest {
        val overlayJob = launch { viewModel.showPayRentOverlay.collect {} }
        val confirmationJob = launch { viewModel.showBankruptcyConfirmation.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.declareBankruptcy()
        advanceUntilIdle()
        viewModel.confirmDeclareBankruptcy()

        assertFalse(viewModel.showPayRentOverlay.value)
        assertFalse(viewModel.showBankruptcyConfirmation.value)

        fakeService.emitTestEvent("""
        {
          "event": "ERROR",
          "gameId": "g1",
          "message": "Mortgage all properties before declaring bankruptcy.",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 100,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            }
          }
        }
        """.trimIndent())
        runCurrent()

        assertTrue(viewModel.showPayRentOverlay.value)
        assertEquals("Mortgage all properties before declaring bankruptcy.", viewModel.errorMessage.value)

        overlayJob.cancel()
        confirmationJob.cancel()
    }

    @Test
    fun `declareBankruptcy does nothing when confirmation already open`() = runTest {
        val job = launch { viewModel.showBankruptcyConfirmation.collect {} }
        val job2 = launch { viewModel.showPayRentOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.declareBankruptcy()
        assertTrue(viewModel.showBankruptcyConfirmation.value)
        viewModel.declareBankruptcy()
        assertTrue(viewModel.showBankruptcyConfirmation.value)

        job.cancel()
        job2.cancel()
    }

    @Test
    fun `cancelDeclareBankruptcy reopens pay rent overlay`() = runTest {
        val job = launch { viewModel.showBankruptcyConfirmation.collect {} }
        val job2 = launch { viewModel.showPayRentOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.declareBankruptcy()
        assertFalse(viewModel.showPayRentOverlay.value)
        assertTrue(viewModel.showBankruptcyConfirmation.value)

        viewModel.cancelDeclareBankruptcy()
        assertFalse(viewModel.showBankruptcyConfirmation.value)
        assertTrue(viewModel.showPayRentOverlay.value)

        job.cancel()
        job2.cancel()
    }

    @Test
    fun `dismissing PayRentOverlay does not clear pending rent reopen capability`() = runTest {
        val job = launch { viewModel.showPayRentOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showPayRentOverlay.value)

        viewModel.dismissPayRentOverlay()
        assertFalse(viewModel.showPayRentOverlay.value)

        viewModel.showPayRentOverlay(100, "p2", 5)
        assertTrue(viewModel.showPayRentOverlay.value)

        job.cancel()
    }

    @Test
    fun `after bankruptcy overlay dismissed player can still view board`() = runTest {
        val job = launch { viewModel.showBankruptcyOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":0,"eliminated":true}],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "bankruptcyTotalAssets": 0,
            "bankruptcyTotalDebt": 100,
            "bankruptcyPropertiesCount": 0,
            "bankruptcyOwnedFieldIds": []
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showBankruptcyOverlay.value)

        viewModel.dismissBankruptcyOverlay()
        assertFalse(viewModel.showBankruptcyOverlay.value)
        assertNotNull(viewModel.gameState.value)

        job.cancel()
    }

    @Test
    fun `canRaiseFunds true when debtorCanPayAfterAssets is true`() = runTest {
        val job = launch { viewModel.canRaiseFunds.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 200,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": true
            },
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.canRaiseFunds.value)

        job.cancel()
    }

    @Test
    fun `canRaiseFunds false when debtorCanPayAfterAssets is false`() = runTest {
        val job = launch { viewModel.canRaiseFunds.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 500,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorCanPayAfterAssets": false
            },
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.canRaiseFunds.value)

        job.cancel()
    }

    @Test
    fun `canRaiseFunds false when total assets insufficient even with remaining assets`() = runTest {
        val fundsJob = launch { viewModel.canRaiseFunds.collect {} }
        val gatesJob = launch { viewModel.actionGates.collect {} }
        val confirmationJob = launch { viewModel.showBankruptcyConfirmation.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [
              {"id":5,"name":"Low Value","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":0,"hasHotel":false,"isMortgaged":false}
            ],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 500,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": false
            },
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertFalse(viewModel.canRaiseFunds.value)
        assertTrue(viewModel.actionGates.value.canDeclareBankruptcy)

        viewModel.declareBankruptcy()

        assertTrue(viewModel.showBankruptcyConfirmation.value)

        fundsJob.cancel()
        gatesJob.cancel()
        confirmationJob.cancel()
    }

    @Test
    fun `canRaiseFunds true when pendingPayment is null`() = runTest {
        val job = launch { viewModel.canRaiseFunds.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [],
            "phase": "ROLLING",
            "pendingPayment": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.canRaiseFunds.value)

        job.cancel()
    }

    @Test
    fun `canRaiseFunds true when gameState is null initially`() = runTest {
        val job = launch { viewModel.canRaiseFunds.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.canRaiseFunds.value)

        job.cancel()
    }

    @Test
    fun `mortgageProperty works while rent payment is pending`() = runTest {
        val job = launch { viewModel.actionGates.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":50}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 200,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": true
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertNotNull(viewModel.visiblePaymentState.value)
        assertTrue(viewModel.actionGates.value.canManageProperties)

        viewModel.mortgageProperty(1)

        assertTrue(fakeService.mortgagePropertyCalled)
        assertEquals(1, fakeService.lastMortgageFieldId)

        job.cancel()
    }

    @Test
    fun `property spending actions are blocked while payment is pending`() = runTest {
        val job = launch { viewModel.actionGates.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":300}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {
              "amount": 200,
              "source": "RENT",
              "sourceFieldId": 5,
              "creditorPlayerId": "p2",
              "debtorPlayerId": "p1",
              "debtorCanPayAfterAssets": true
            }
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.actionGates.value.canManageProperties)

        viewModel.unmortgageProperty(1)
        viewModel.buyHouse(1)
        viewModel.buyHotel(1)

        assertFalse(fakeService.unmortgagePropertyCalled)
        assertFalse(fakeService.buyHouseCalled)
        assertFalse(fakeService.buyHotelCalled)

        job.cancel()
    }

    @Test
    fun `mortgageProperty blocked when propertyActionInFlight is true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        fakeService.mortgagePropertyCalled = false
        viewModel.mortgageProperty(1)
        assertTrue(fakeService.mortgagePropertyCalled)

        fakeService.mortgagePropertyCalled = false
        viewModel.mortgageProperty(1)
        assertFalse(fakeService.mortgagePropertyCalled)
    }

    @Test
    fun `unmortgageProperty blocked when propertyActionInFlight is true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        fakeService.unmortgagePropertyCalled = false
        viewModel.unmortgageProperty(1)
        assertTrue(fakeService.unmortgagePropertyCalled)

        fakeService.unmortgagePropertyCalled = false
        viewModel.unmortgageProperty(1)
        assertFalse(fakeService.unmortgagePropertyCalled)
    }

    @Test
    fun `sellHouse blocked when propertyActionInFlight is true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        fakeService.sellHouseCalled = false
        viewModel.sellHouse(1)
        assertTrue(fakeService.sellHouseCalled)

        fakeService.sellHouseCalled = false
        viewModel.sellHouse(1)
        assertFalse(fakeService.sellHouseCalled)
    }

    @Test
    fun `sellHouse blocked when buildingActionPending is true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        fakeService.sellHouseCalled = false
        fakeService.buyHouseCalled = false
        fakeService.buyHouseCalled = false
        viewModel.buyHouse(1)
        assertTrue(viewModel.buildingActionPending.value)

        fakeService.sellHouseCalled = false
        viewModel.sellHouse(1)
        assertFalse(fakeService.sellHouseCalled)
    }

    @Test
    fun `sellHotel blocked when propertyActionInFlight is true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        fakeService.sellHotelCalled = false
        viewModel.sellHotel(1)
        assertTrue(fakeService.sellHotelCalled)

        fakeService.sellHotelCalled = false
        viewModel.sellHotel(1)
        assertFalse(fakeService.sellHotelCalled)
    }

    @Test
    fun `sellHotel blocked when buildingActionPending is true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        fakeService.buyHouseCalled = false
        viewModel.buyHouse(1)

        fakeService.sellHotelCalled = false
        viewModel.sellHotel(1)
        assertFalse(fakeService.sellHotelCalled)
    }

    @Test
    fun `buyHouse blocked when buildingActionPending is true`() = runTest(testDispatcher) {
        seedGameState(phase = "BUYING")

        fakeService.buyHouseCalled = false
        viewModel.buyHouse(1)
        assertTrue(fakeService.buyHouseCalled)

        fakeService.buyHouseCalled = false
        viewModel.buyHouse(1)
        assertFalse(fakeService.buyHouseCalled)
    }

    @Test
    fun `manageableProperties canSellHouse reacts to sibling house changes`() = runTest {
        val job = launch { viewModel.manageableProperties.collect {} }
        fakeService.currentPlayerId = "p1"

        // Two owned BROWN properties: Field1 has 2 houses, Field2 has 0 houses.
        // Selling from Field1 → newHouseCount = 1, sibling Field2 has 0 < 1 → canSellHouse = false
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "phase": "BUYING",
            "currentPlayerIndex": 0,
            "players": [{"id":"p1","name":"Alice","money":1500}],
            "fields": [
              {"id":1,"name":"F1","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":2,"hasHotel":false,"isMortgaged":false},
              {"id":2,"name":"F2","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":0,"hasHotel":false,"isMortgaged":false}
            ],
            "pendingPayment": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        val prop1 = viewModel.manageableProperties.value.find { it.fieldId == 1 }
        assertNotNull(prop1)
        assertEquals(2, prop1!!.houses)
        assertFalse("Sell House should be blocked: newHouseCount=1, sibling has 0 < 1", prop1.canSellHouse)

        // Field2 gains a house → both now have 2 and 1.
        // Selling from Field1 → newHouseCount=1, sibling Field2 has 1 >= 1 → canSellHouse = true
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "phase": "BUYING",
            "currentPlayerIndex": 0,
            "players": [{"id":"p1","name":"Alice","money":1500}],
            "fields": [
              {"id":1,"name":"F1","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":2,"hasHotel":false,"isMortgaged":false},
              {"id":2,"name":"F2","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":1,"hasHotel":false,"isMortgaged":false}
            ],
            "pendingPayment": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        val prop1Updated = viewModel.manageableProperties.value.find { it.fieldId == 1 }
        assertNotNull(prop1Updated)
        assertTrue("Sell House should now be allowed: newHouseCount=1, sibling has 1 >= 1", prop1Updated!!.canSellHouse)

        job.cancel()
    }

    @Test
    fun `manageableProperties canSellHouse blocked when sibling has more houses`() = runTest {
        val job = launch { viewModel.manageableProperties.collect {} }
        fakeService.currentPlayerId = "p1"

        // Field1 has 1 house, Field2 (sibling) has 3 houses.
        // Selling from Field1 → newHouseCount = 0, but sibling 3 > 1 → blocked
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "phase": "BUYING",
            "currentPlayerIndex": 0,
            "players": [{"id":"p1","name":"Alice","money":1500}],
            "fields": [
              {"id":1,"name":"F1","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":1,"hasHotel":false,"isMortgaged":false},
              {"id":2,"name":"F2","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":3,"hasHotel":false,"isMortgaged":false}
            ],
            "pendingPayment": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        val prop1 = viewModel.manageableProperties.value.find { it.fieldId == 1 }
        assertNotNull(prop1)
        // OLD rule would allow (3 in 0..1 = false)... wait, 3 in 0..1 IS false. So old rule also blocks?
        // Actually old rule was it.houses >= newHouseCount: 3 >= 0 = true → allowed ✗
        // New rule: 3 in 0..1 = false → blocked ✓
        assertFalse("Sell House from lower-count property blocked: sibling has 3 > 1", prop1!!.canSellHouse)
        assertEquals("Sibling has 3 houses, canSellHouse must be false", false, prop1.canSellHouse)

        job.cancel()
    }

    @Test
    fun `manageableProperties canSellHotel reacts to sibling house changes`() = runTest {
        val job = launch { viewModel.manageableProperties.collect {} }
        fakeService.currentPlayerId = "p1"

        // Two owned BROWN properties: Field1 has hotel, Field2 has 3 houses.
        // Selling hotel → need all siblings >= 4, Field2 has 3 < 4 → canSellHotel = false
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "phase": "BUYING",
            "currentPlayerIndex": 0,
            "players": [{"id":"p1","name":"Alice","money":1500}],
            "fields": [
              {"id":1,"name":"F1","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":0,"hasHotel":true,"isMortgaged":false},
              {"id":2,"name":"F2","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":3,"hasHotel":false,"isMortgaged":false}
            ],
            "pendingPayment": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        val prop1 = viewModel.manageableProperties.value.find { it.fieldId == 1 }
        assertNotNull(prop1)
        assertTrue(prop1!!.hasHotel)
        assertFalse("Sell Hotel should be blocked: sibling has 3 < 4", prop1.canSellHotel)

        // Field2 gains another house → now has 4 houses.
        // Selling hotel → need all siblings >= 4, Field2 has 4 >= 4 → canSellHotel = true
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "phase": "BUYING",
            "currentPlayerIndex": 0,
            "players": [{"id":"p1","name":"Alice","money":1500}],
            "fields": [
              {"id":1,"name":"F1","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":0,"hasHotel":true,"isMortgaged":false},
              {"id":2,"name":"F2","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50,"ownerId":"p1","houses":4,"hasHotel":false,"isMortgaged":false}
            ],
            "pendingPayment": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        val prop1Updated = viewModel.manageableProperties.value.find { it.fieldId == 1 }
        assertNotNull(prop1Updated)
        assertTrue("Sell Hotel should now be allowed: sibling has 4 >= 4", prop1Updated!!.canSellHotel)

        job.cancel()
    }


    @Test
    fun `reportCheater should delegate to gameService`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p2"
        fakeService.currentGameId = "g1"
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":1500},
              {"id":"p2","name":"Bob","money":600}
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING",
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()
        val suspectId = "p1"

        viewModel.reportCheater(suspectId)

        assertTrue(fakeService.reportCheaterCalled)
        assertEquals(suspectId, fakeService.lastReportedPlayerId)
    }

    @Test
    fun `reportCheater does NOT delegate when current player has not rolled`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p2"
        fakeService.currentGameId = "g1"
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":1500},
              {"id":"p2","name":"Bob","money":600}
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.reportCheater("p1")

        assertFalse(fakeService.reportCheaterCalled)
        assertEquals("", fakeService.lastReportedPlayerId)
    }

    @Test
    fun `reportCheater does NOT delegate when local player reports non current player`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p2"
        fakeService.currentGameId = "g1"
        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":1500},
              {"id":"p2","name":"Bob","money":600},
              {"id":"p3","name":"Carla","money":1500}
            ],
            "currentPlayerIndex": 0,
            "phase": "BUYING",
            "lastDiceRoll": {"die1":3,"die2":4}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.reportCheater("p3")

        assertFalse(fakeService.reportCheaterCalled)
        assertEquals("", fakeService.lastReportedPlayerId)
    }

    @Test
    fun `CHEATER_REPORTED event emits message to dramaEvent flow`() = runTest {
        val dramaMessages = mutableListOf<String>()
        val job = launch { viewModel.dramaEvent.collect { dramaMessages.add(it) } }

        val testMessage = "Alice successfully reported Bob for cheating!"

        fakeService.emitTestEvent("""
        {
          "event": "CHEATER_REPORTED",
          "gameId": "g1",
          "message": "$testMessage"
        }
        """.trimIndent())

        advanceUntilIdle()

        assertTrue("Expected dramaEvent to emit the message", dramaMessages.contains(testMessage))

        job.cancel()
    }

    @Test
    fun `reportCheater does NOT delegate when current player is bankrupt`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
            {
              "event": "STATE_UPDATED",
              "gameId": "game-1",
              "gameState": {
                "gameId": "game-1",
                "fields": [],
                "players": [
                  {"id":"p1","name":"Alice","money":501,"eliminated":true},
                  {"id":"p2","name":"Bob","money":1500}
                ],
                "currentPlayerIndex": 0,
                "phase": "BUYING"
              }
            }
            """.trimIndent())
        advanceUntilIdle()

        viewModel.reportCheater("p2")

        assertFalse(fakeService.reportCheaterCalled)
        assertEquals("", fakeService.lastReportedPlayerId)
    }

    @Test
    fun `reportCheater does NOT delegate when reported player is bankrupt`() = runTest(testDispatcher) {
        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
            {
              "event": "STATE_UPDATED",
              "gameId": "game-1",
              "gameState": {
                "gameId": "game-1",
                "fields": [],
                "players": [
                  {"id":"p1","name":"Alice","money":501},
                  {"id":"p2","name":"Bob","money":0,"eliminated":true}
                ],
                "currentPlayerIndex": 0,
                "phase": "BUYING"
              }
            }
            """.trimIndent())
        advanceUntilIdle()

        viewModel.reportCheater("p2")

        assertFalse(fakeService.reportCheaterCalled)
        assertEquals("", fakeService.lastReportedPlayerId)
    }

    @Test
    fun `CHEATER_REPORT_FAILED event emits message to dramaEvent flow`() = runTest {
        val dramaMessages = mutableListOf<String>()
        val job = launch { viewModel.dramaEvent.collect { dramaMessages.add(it) } }

        val testMessage = "Alice falsely accused Bob of cheating!"

        fakeService.emitTestEvent("""
        {
          "event": "CHEATER_REPORT_FAILED",
          "gameId": "g1",
          "message": "$testMessage"
        }
        """.trimIndent())

        advanceUntilIdle()

        assertTrue("Expected dramaEvent to emit the message", dramaMessages.contains(testMessage))

        job.cancel()
    }

    @Test
    fun `humanReadableEvent fallback produces correct text for events without message field`() = runTest {
        val job = launch { viewModel.eventLog.collect {} }

        val testCases = listOf(
            "TAX_DUE" to "Tax is due!",
            "TAX_PAID" to "Tax paid",
            "FREE_PARKING_COLLECTED" to "Free Parking jackpot collected!",
            "PROPERTY_BOUGHT" to "Property bought",
            "PROPERTY_MORTGAGED" to "Property mortgaged",
            "PROPERTY_UNMORTGAGED" to "Property unmortgaged",
            "HOUSE_BOUGHT" to "House bought",
            "HOTEL_BOUGHT" to "Hotel bought",
            "HOUSE_SOLD" to "House sold",
            "HOTEL_SOLD" to "Hotel sold",
            "GAME_CLOSED" to "Game closed by host",
            "TURN_TIMEOUT" to "Turn timed out",
        )

        for ((eventType, expectedText) in testCases) {
            fakeService.emitTestEvent("""{"event":"$eventType","gameId":"g1"}""")
            advanceUntilIdle()
            assertEquals(expectedText, viewModel.eventLog.value.last().text)
        }

        job.cancel()
    }

    @Test
    fun `payTax calls gameService payTax with current field id`() = runTest {
        seedGameState(
            eventType = "TAX_DUE",
            phase = "PAYING_RENT",
            extraState = """,
            "pendingPayment": {
              "amount": 200,
              "source": "TAX",
              "sourceFieldId": 4,
              "creditorPlayerId": null
            }
            """
        )

        viewModel.payTax()

        assertEquals(4, fakeService.lastPaidTaxFieldId)
    }

    @Test
    fun `rollDice should set rollRequestInFlight and block consecutive roll calls`() = runTest(testDispatcher) {
        seedGameState(phase = "ROLLING")

        // First roll must succeed
        fakeService.rollDiceCalled = false
        viewModel.rollDice()
        assertTrue("First roll should be passed to the service", fakeService.rollDiceCalled)

        // Call second roll immediately (while rollRequestInFlight is active)
        fakeService.rollDiceCalled = false
        viewModel.rollDice()
        assertFalse("Second roll must be blocked because a request is already in flight", fakeService.rollDiceCalled)
    }


    @Test
    fun `DICE_ROLLED event should clear rollRequestInFlight lock`() = runTest(testDispatcher) {
        val job = launch { viewModel.gameState.collect {} }
        seedGameState(phase = "ROLLING")

        viewModel.rollDice() // Activate lock
        fakeService.rollDiceCalled = false

        // Simulate successful response from server
        fakeService.emitTestEvent(
            """
            {
              "event": "DICE_ROLLED",
              "gameId": "g1",
              "gameState": {
                "gameId": "g1",
                "fields": [],
                "players": [{ "id": "p1", "name": "Alice" }],
                "phase": "BUYING"
              }
            }
            """.trimIndent()
        )
        advanceUntilIdle()

      // Emit a ROLLING-phase event to release the lock
      fakeService.emitTestEvent(
          """
          {
            "event": "STATE_UPDATED",
            "gameId": "g1",
            "gameState": {
              "gameId": "g1",
              "fields": [],
              "players": [{ "id": "p1", "name": "Alice" }],
              "phase": "ROLLING"
            }
          }
          """.trimIndent()
      )
      advanceUntilIdle()

      // Lock must be released
      viewModel.rollDice()
      assertTrue("DICE_ROLLED event must release the lock", fakeService.rollDiceCalled)

        job.cancel()
    }

    @Test
    fun `TURN_ENDED event should clear rollRequestInFlight lock and reset cheat state`() = runTest(testDispatcher) {
        val job = launch { viewModel.gameState.collect {} }
        seedGameState(phase = "ROLLING")

        viewModel.rollDice() // Activate lock
        fakeService.rollDiceCalled = false

        // Simulate turn end from server
        fakeService.emitTestEvent("""
        {
          "event": "TURN_ENDED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [{"id":0,"name":"Go","type":"GO"}],
            "players": [{"id":"p1","name":"Alice"}],
            "currentPlayerIndex": 0,
            "phase": "ROLLING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        // Lock must be released
        viewModel.rollDice()
        assertTrue("TURN_ENDED event must release the lock", fakeService.rollDiceCalled)

        job.cancel()
    }

    @Test
    fun `ERROR event should clear rollRequestInFlight lock`() = runTest(testDispatcher) {
        val job = launch { viewModel.errorMessage.collect {} }
        seedGameState(phase = "ROLLING")

        viewModel.rollDice() // Activate lock
        fakeService.rollDiceCalled = false

        // Simulate network/server error
        fakeService.emitTestEvent("""{"event":"ERROR","gameId":"g1","message":"Internal Server Error"}""")
        advanceUntilIdle()

        // Lock must be released
        viewModel.rollDice()
        assertTrue("ERROR event must release the lock", fakeService.rollDiceCalled)

        job.cancel()
    }



    @Test
    fun `activateCheatForNextRoll should be ignored if not in rolling phase`() = runTest(testDispatcher) {
        val job = launch { viewModel.isRollingPhaseForCurrentPlayer.collect {} }

        // Set to BUYING phase (isRollingPhaseForCurrentPlayer becomes false)
        fakeService.emitTestEvent("""{"event":"STATE_UPDATED","gameId":"g1","gameState":{"gameId":"g1","fields":[],"players":[{"id":"p1","name":"Alice"}],"currentPlayerIndex":0,"phase":"BUYING"}}""")
        advanceUntilIdle()
        assertFalse(viewModel.isRollingPhaseForCurrentPlayer.value)

        // Cheat attempt in wrong phase
        viewModel.activateCheatForNextRoll()

        job.cancel()
    }

    private suspend fun TestScope.seedGameState(
        eventType: String = "STATE_UPDATED",
        phase: String = "BUYING",
        money: Int = 1500,
        position: Int = 0,
        fieldsJson: String = """[{"id":0,"name":"Go","type":"GO"}]""",
        extraState: String = ""
    ) {
        fakeService.currentPlayerId = "p1"
        fakeService.currentGameId = "g1"
        fakeService.emitTestEvent(
            """
            {
              "event": "$eventType",
              "gameId": "g1",
              "gameState": {
                "gameId": "g1",
                "fields": $fieldsJson,
                "players": [
                  {
                    "id": "p1",
                    "name": "Alice",
                    "money": $money,
                    "position": $position,
                    "ownedPropertyIds": []
                  },
                  {
                    "id": "p2",
                    "name": "Bob",
                    "money": 1500,
                    "position": 0,
                    "ownedPropertyIds": []
                  }
                ],
                "currentPlayerIndex": 0,
                "phase": "$phase"
                $extraState
              }
            }
            """.trimIndent()
        )
        advanceUntilIdle()
    }

}
