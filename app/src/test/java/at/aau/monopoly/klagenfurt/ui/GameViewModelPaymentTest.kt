package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.PendingPayment
import at.aau.monopoly.klagenfurt.model.PaymentSource
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.GoField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelPaymentTest {

    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: GameViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeGameService()
        fakeService.currentPlayerId = "p1"
        fakeService.currentGameId = "game-1"
        viewModel = GameViewModel(fakeService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `testPayRentOverlayState`() = runTest {
        val job = launch { viewModel.showPayRentOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
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
        assertEquals(100, viewModel.currentRentAmount.value)
        assertEquals(5, viewModel.currentRentFieldId.value)
        assertEquals("p2", viewModel.currentRentOwnerId.value)

        job.cancel()
    }

    @Test
    fun `testPayTaxOverlayState`() = runTest {
        val job = launch { viewModel.showPayRentOverlay.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "TAX_DUE",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":200,"source":"TAX","sourceFieldId":2,"creditorPlayerId":null},
            "lastDiceRoll": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showPayRentOverlay.value)
        assertEquals(200, viewModel.currentRentAmount.value)
        assertEquals(2, viewModel.currentRentFieldId.value)

        job.cancel()
    }

    @Test
    fun `testMortgageOverlayManagement`() {
        viewModel.showMortgageManagementOverlay()
        assertTrue(viewModel.showMortgageOverlay.value)

        viewModel.dismissMortgageOverlay()
        assertFalse(viewModel.showMortgageOverlay.value)
    }

    @Test
    fun `testBankruptcyOverlayManagement`() {
        viewModel.showBankruptcyOverlay()
        assertTrue(viewModel.showBankruptcyOverlay.value)

        viewModel.dismissBankruptcyOverlay()
        assertFalse(viewModel.showBankruptcyOverlay.value)
    }

    @Test
    fun `testAcceptBankruptcyResolution`() = runTest {
        val job = launch { viewModel.bankruptcyPlayerName.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "BANKRUPTCY_DECLARED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [
              {"id":"p1","name":"Alice","money":0},
              {"id":"p2","name":"Bob","money":0}
            ],
            "currentPlayerIndex": 0,
            "phase": "TURN_END",
            "bankruptcyPlayerId": "p2",
            "bankruptcyPlayerName": "Bob",
            "bankruptcyTotalAssets": 50,
            "bankruptcyTotalDebt": 500,
            "bankruptcyPropertiesCount": 0,
            "bankruptcyOwnedFieldIds": []
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertTrue(viewModel.showBankruptcyOverlay.value)
        assertEquals("Bob", viewModel.bankruptcyPlayerName.value)

        viewModel.acceptBankruptcyResolution()
        assertFalse(viewModel.showBankruptcyOverlay.value)
        assertEquals("", viewModel.bankruptcyPlayerName.value)

        job.cancel()
    }

    @Test
    fun `testCanPayRentWhenPlayerHasSufficientFunds`() = runTest {
        val job = launch { viewModel.canPayRent.collect {} }

        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":500}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":100,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.showPayRentOverlay(100, "p2", 5)
        assertTrue(viewModel.canPayRent.value)

        job.cancel()
    }

    @Test
    fun `testCannotPayRentWhenPlayerInsufficientFunds`() = runTest {
        val job = launch { viewModel.canPayRent.collect {} }

        fakeService.currentPlayerId = "p1"
        fakeService.emitTestEvent("""
        {
          "event": "RENT_DUE",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":100}],
            "currentPlayerIndex": 0,
            "phase": "PAYING_RENT",
            "pendingPayment": {"amount":500,"source":"RENT","sourceFieldId":5,"creditorPlayerId":"p2"},
            "lastDiceRoll": null
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.showPayRentOverlay(500, "p2", 5)
        assertFalse(viewModel.canPayRent.value)

        job.cancel()
    }

    @Test
    fun `testSelectedPlayerForTradeManagement`() {
        val testPlayer = Player(id = "p2", name = "Bob")

        assertFalse(viewModel.selectedPlayerForTrade.value != null)

        viewModel.showTradeOverlay(testPlayer)
        assertEquals(testPlayer, viewModel.selectedPlayerForTrade.value)

        viewModel.hideTradeOverlay()
        assertFalse(viewModel.selectedPlayerForTrade.value != null)
    }

    @Test
    fun `testActionCardDrawing`() = runTest {
        val job = launch { viewModel.currentActionCard.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "ACTION_DRAWN",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice"}],
            "currentPlayerIndex": 0,
            "phase": "BUYING",
            "currentActionCard": {
              "type": "COMMUNITY_CHEST",
              "id": 5,
              "description": "Pay Hospital Fees",
              "action": "PAY_MONEY",
              "amount": 100
              }
            }
          }
        """.trimIndent())
        advanceUntilIdle()

        assertEquals("Pay Hospital Fees", viewModel.visibleActionCard.value?.description)

        viewModel.executeAction()
        assertTrue(viewModel.isExecutingAction.value)

        job.cancel()
    }

    @Test
    fun `testMovementAnimation`() = runTest {
        val job = launch { viewModel.movementAnimation.collect {} }

        val initialState = fakeService.createGameState("game-1", position = 0)
        fakeService.emitTestEvent("""
        {
          "event": "DICE_ROLLED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [
              {"id":0,"name":"Go","type":"GO","position":0,"positionOnBoard":0},
              {"id":1,"name":"F1","type":"PROPERTY","position":1,"positionOnBoard":1},
              {"id":2,"name":"F2","type":"PROPERTY","position":2,"positionOnBoard":2},
              {"id":3,"name":"F3","type":"PROPERTY","position":3,"positionOnBoard":3}
            ],
            "players": [{"id":"p1","name":"Alice","position":3}],
            "currentPlayerIndex": 0,
            "phase": "BUYING",
            "lastDiceRoll": {"die1":2,"die2":1}
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        viewModel.movementAnimation.value?.let { animation ->
            assertEquals("p1", animation.playerId)
        }

        job.cancel()
    }

    @Test
    fun `testJailLogicIntegration`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "PLAYER_JAILED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","position":10,"inJail":true,"jailTurns":1,"getOutOfJailCards":0}],
            "currentPlayerIndex": 0,
            "phase": "ROLLING"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        val currentPlayer = viewModel.gameState.value?.currentPlayer
        assertTrue(currentPlayer?.inJail == true)
        assertEquals(1, currentPlayer?.jailTurns)

        job.cancel()
    }

    @Test
    fun `testGameOverDetection`() = runTest {
        val job = launch { viewModel.gameState.collect {} }

        fakeService.emitTestEvent("""
        {
          "event": "STATE_UPDATED",
          "gameId": "game-1",
          "gameState": {
            "gameId": "game-1",
            "fields": [],
            "players": [{"id":"p1","name":"Alice","money":1500,"ownedPropertyIds":[]}],
            "currentPlayerIndex": 0,
            "phase": "FINISHED"
          }
        }
        """.trimIndent())
        advanceUntilIdle()

        assertEquals(GamePhase.FINISHED, viewModel.gameState.value?.phase)
        assertTrue(viewModel.gameState.value?.isGameOver()!!)

        job.cancel()
    }

}
