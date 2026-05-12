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
    fun `isGameStarted should be false for WAITING phase`() = runTest {
        val job = launch { viewModel.isGameStarted.collect {} }

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [],
            "phase": "WAITING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertFalse(viewModel.isGameStarted.value)

        job.cancel()
    }

    @Test
    fun `isGameStarted should be true for ROLLING phase`() = runTest {
        val job = launch { viewModel.isGameStarted.collect {} }

        fakeService.emitTestEvent(
            """
        {
          "event": "STATE_UPDATED",
          "gameId": "g1",
          "gameState": {
            "gameId": "g1",
            "fields": [],
            "players": [],
            "phase": "ROLLING"
          }
        }
        """.trimIndent()
        )

        advanceUntilIdle()

        assertTrue(viewModel.isGameStarted.value)

        job.cancel()
    }

    @Test
    fun `isHost should be true when current player is first player`() = runTest {
        val job = launch { viewModel.isHost.collect {} }

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

        assertTrue(viewModel.isHost.value)

        job.cancel()
    }

    @Test
    fun `isHost should be false when current player is not first player`() = runTest {
        val job = launch { viewModel.isHost.collect {} }

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

        assertFalse(viewModel.isHost.value)

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
}
