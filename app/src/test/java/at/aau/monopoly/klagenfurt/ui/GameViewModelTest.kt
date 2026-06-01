package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
    fun `startGame should call gameService startGame`() {
        viewModel.startGame()
        assertTrue(fakeService.startGameCalled)
    }

    @Test
    fun `endTurn should call gameService endTurn`() {
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

    // --- FACTORY TEST ---

    @Test
    fun `Factory creates GameViewModel successfully`() {
        val factory = GameViewModel.Factory(fakeService)
        val createdViewModel = factory.create(GameViewModel::class.java)
        assertTrue(createdViewModel is GameViewModel)
    }

    // --- DICE CHEAT TESTS ---

    @Test
    fun `rollDice should call service`() {
        viewModel.rollDice()
        assertTrue(fakeService.rollDiceCalled)
    }

    @Test
    fun `cheat flag should be reset after one roll`() {
        viewModel.activateCheatForNextRoll()
        viewModel.rollDice()
        assertTrue(fakeService.rollDiceCalled)

        // Reset flag in fake and advance time
        fakeService.rollDiceCalled = false
        fakeTime += 1600L

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
    fun `dismissActionCard should clear current action card`() {
        val card = at.aau.monopoly.klagenfurt.model.card.ChanceCard(
            id = 1,
            description = "Collect money",
            action = at.aau.monopoly.klagenfurt.model.enums.CardAction.COLLECT_MONEY,
            amount = 100
        )

        viewModel.setCurrentActionCard(card)
        assertEquals(card, viewModel.currentActionCard.value)

        viewModel.dismissActionCard()

        assertNull(viewModel.currentActionCard.value)
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
    fun `buyProperty should call gameService with correct field id`() {
        val fakeService = FakeGameService()
        val viewModel = GameViewModel(fakeService)

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
    fun `dismissActionCard should clear action card state`() {
        val card = at.aau.monopoly.klagenfurt.model.card.ChanceCard(
            id = 99,
            description = "Test Card",
            action = at.aau.monopoly.klagenfurt.model.enums.CardAction.COLLECT_MONEY,
            amount = 200
        )

        viewModel.setCurrentActionCard(card)
        viewModel.dismissActionCard()

        assertNull(viewModel.currentActionCard.value)
    }

    @Test
    fun `buyHouse should set building action pending to true`() {
        viewModel.buyHouse(1)

        assertTrue(viewModel.buildingActionPending.value)
    }

    @Test
    fun `buyHotel should set building action pending to true`() {
        viewModel.buyHotel(1)

        assertTrue(viewModel.buildingActionPending.value)
    }

    @Test
    fun `sellHouse should set building action pending to true`() {
        viewModel.sellHouse(1)

        assertTrue(viewModel.buildingActionPending.value)
    }

    @Test
    fun `sellHotel should set building action pending to true`() {
        viewModel.sellHotel(1)

        assertTrue(viewModel.buildingActionPending.value)
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
    fun `BANKRUPTCY_DECLARED populates bankruptcyPlayerName correctly`() = runTest {
        val job = launch { viewModel.bankruptcyPlayerName.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [{"id":"p1","name":"BankruptPlayer"}],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "bankruptcyTotalAssets": 50,
            "bankruptcyTotalDebt": 100,
            "bankruptcyPropertiesCount": 1,
            "bankruptcyOwnedFieldIds": []
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
    fun `declareBankruptcy does nothing when paymentActionInFlight is true`() = runTest {
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

        fakeService.declareBankruptcyCalled = false
        viewModel.declareBankruptcy()
        assertTrue(fakeService.declareBankruptcyCalled)

        fakeService.declareBankruptcyCalled = false
        viewModel.declareBankruptcy()
        assertFalse(fakeService.declareBankruptcyCalled)

        job.cancel()
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
    fun `canRaiseFunds false when pendingPayment is null`() = runTest {
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

        assertFalse(viewModel.canRaiseFunds.value)

        job.cancel()
    }

    @Test
    fun `canRaiseFunds false when gameState is null initially`() = runTest {
        val job = launch { viewModel.canRaiseFunds.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.canRaiseFunds.value)

        job.cancel()
    }

    @Test
    fun `mortgageProperty blocked when propertyActionInFlight is true`() {
        fakeService.mortgagePropertyCalled = false
        viewModel.mortgageProperty(1)
        assertTrue(fakeService.mortgagePropertyCalled)

        fakeService.mortgagePropertyCalled = false
        viewModel.mortgageProperty(1)
        assertFalse(fakeService.mortgagePropertyCalled)
    }

    @Test
    fun `unmortgageProperty blocked when propertyActionInFlight is true`() {
        fakeService.unmortgagePropertyCalled = false
        viewModel.unmortgageProperty(1)
        assertTrue(fakeService.unmortgagePropertyCalled)

        fakeService.unmortgagePropertyCalled = false
        viewModel.unmortgageProperty(1)
        assertFalse(fakeService.unmortgagePropertyCalled)
    }

    @Test
    fun `sellHouse blocked when propertyActionInFlight is true`() {
        fakeService.sellHouseCalled = false
        viewModel.sellHouse(1)
        assertTrue(fakeService.sellHouseCalled)

        fakeService.sellHouseCalled = false
        viewModel.sellHouse(1)
        assertFalse(fakeService.sellHouseCalled)
    }

    @Test
    fun `sellHouse blocked when buildingActionPending is true`() {
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
    fun `sellHotel blocked when propertyActionInFlight is true`() {
        fakeService.sellHotelCalled = false
        viewModel.sellHotel(1)
        assertTrue(fakeService.sellHotelCalled)

        fakeService.sellHotelCalled = false
        viewModel.sellHotel(1)
        assertFalse(fakeService.sellHotelCalled)
    }

    @Test
    fun `sellHotel blocked when buildingActionPending is true`() {
        fakeService.buyHouseCalled = false
        viewModel.buyHouse(1)

        fakeService.sellHotelCalled = false
        viewModel.sellHotel(1)
        assertFalse(fakeService.sellHotelCalled)
    }

    @Test
    fun `buyHouse blocked when buildingActionPending is true`() {
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
}
