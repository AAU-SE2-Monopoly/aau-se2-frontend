package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.FakeGameService
import at.aau.monopoly.klagenfurt.messaging.GameEvent
import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.PendingPayment
import at.aau.monopoly.klagenfurt.model.PaymentSource
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelPresentationBarrierTest {

    private lateinit var fakeService: FakeGameService
    private lateinit var viewModel: GameViewModel
    private val dispatcher = StandardTestDispatcher()
    private val objectMapper = JacksonProvider.objectMapper

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        fakeService = FakeGameService()
        fakeService.currentPlayerId = "p1"
        fakeService.currentGameId = "game-1"
        viewModel = GameViewModel(fakeService, currentTimeProvider = { 1000L })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fast rent response stays hidden until movement finishes`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        emit(diceRolled(state(position = 3, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 2))))
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.SHOWING_DICE_RESULT, viewModel.presentationPhase.value)
        assertEquals(0, viewModel.presentedBoardPlayers.value.first().position)
        assertFalse(viewModel.actionGates.value.canEndTurn)
        assertTrue(viewModel.presentedEventLog.value.none { it.eventType == "DICE_ROLLED" })

        emit(
            event(
                "RENT_DUE",
                state(
                    position = 3,
                    phase = GamePhase.PAYING_RENT,
                    diceRoll = DiceRoll(1, 2),
                    pendingPayment = PendingPayment(
                        amount = 100,
                        source = PaymentSource.RENT,
                        sourceFieldId = 3,
                        creditorPlayerId = "p2"
                    )
                )
            )
        )
        runCurrent()

        assertNull(viewModel.visiblePaymentState.value)
        assertFalse(viewModel.showPayRentOverlay.value)

        advanceTimeBy(4_000L)
        runCurrent()

        assertEquals(3, viewModel.presentedBoardPlayers.value.first().position)
        assertNotNull(viewModel.visiblePaymentState.value)
        assertTrue(viewModel.showPayRentOverlay.value)
        assertTrue(viewModel.actionGates.value.canPayRent)
    }

    @Test
    fun `railroad rent event with pending payment reveals after movement presentation`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        emit(diceRolled(state(position = 5, phase = GamePhase.BUYING, diceRoll = DiceRoll(2, 3))))
        runCurrent()

        emit(
            event(
                "RAILROAD_RENT_DUE",
                state(
                    position = 5,
                    phase = GamePhase.PAYING_RENT,
                    diceRoll = DiceRoll(2, 3),
                    pendingPayment = PendingPayment(
                        amount = 50,
                        source = PaymentSource.RENT,
                        sourceFieldId = 5,
                        creditorPlayerId = "p2"
                    )
                )
            )
        )
        runCurrent()

        assertNull(viewModel.visiblePaymentState.value)
        assertFalse(viewModel.showPayRentOverlay.value)

        advanceTimeBy(4_000L)
        runCurrent()

        assertEquals(5, viewModel.presentedBoardPlayers.value.first().position)
        assertEquals(50, viewModel.currentRentAmount.value)
        assertEquals(5, viewModel.currentRentFieldId.value)
        assertTrue(viewModel.showPayRentOverlay.value)
        assertTrue(viewModel.actionGates.value.canPayRent)
    }

    @Test
    fun `backend messages are displayed in presented event log`() = runTest(dispatcher) {
        emit(
            GameEvent(
                gameId = "game-1",
                event = "SOME_BACKEND_EVENT",
                message = "Backend says the auction started",
                gameState = state(position = 0, phase = GamePhase.BUYING)
            )
        )
        runCurrent()

        assertEquals("Backend says the auction started", viewModel.presentedEventLog.value.last().text)
    }

    @Test
    fun `dice backend message appears in log after dice result hold before landing reveal`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        emit(
            GameEvent(
                gameId = "game-1",
                event = "DICE_ROLLED",
                message = "Alice rolled a 3",
                gameState = state(position = 3, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 2))
            )
        )
        runCurrent()

        assertTrue(viewModel.presentedEventLog.value.none { it.eventType == "DICE_ROLLED" })

        advanceTimeBy(2_000L)
        runCurrent()

        val diceEntry = viewModel.presentedEventLog.value.last { it.eventType == "DICE_ROLLED" }
        assertEquals("Alice rolled a 3", diceEntry.text)
        assertNull(viewModel.visiblePaymentState.value)
    }

    @Test
    fun `action card drawn during active presentation is revealed at landing`() = runTest(dispatcher) {
        val card = ChanceCard(
            id = 7,
            description = "Advance carefully",
            action = CardAction.MOVE_FORWARD,
            moveSpaces = 2
        )

        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()
        emit(diceRolled(state(position = 2, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 1))))
        runCurrent()

        emit(event("ACTION_DRAWN", state(position = 2, phase = GamePhase.BUYING, actionCard = card)))
        runCurrent()

        assertNull(viewModel.visibleActionCard.value)

        advanceTimeBy(4_000L)
        runCurrent()

        assertEquals(card.description, viewModel.visibleActionCard.value?.description)
        assertTrue(viewModel.actionGates.value.canExecuteCard)
    }

    @Test
    fun `manual dice dismiss callback does not unlock actions`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        viewModel.rollDice()
        runCurrent()

        val sequenceId = viewModel.activeDicePresentation.value?.sequenceId
        assertNotNull(sequenceId)

        viewModel.onDiceDismissed(sequenceId!!)
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.ROLLING_DICE, viewModel.presentationPhase.value)
        assertFalse(viewModel.actionGates.value.canEndTurn)
        assertFalse(viewModel.actionGates.value.canRollDice)
    }

    @Test
    fun `stale dice callback after snapshot is ignored`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()
        viewModel.rollDice()
        runCurrent()

        val staleSequenceId = viewModel.activeDicePresentation.value?.sequenceId
        assertNotNull(staleSequenceId)

        emit(snapshot(state(position = 5, phase = GamePhase.BUYING)))
        runCurrent()
        val syncedPhase = viewModel.presentationPhase.value

        viewModel.onDiceDismissed(staleSequenceId!!)
        viewModel.onDiceResultDisplayed(staleSequenceId)
        runCurrent()

        assertEquals(syncedPhase, viewModel.presentationPhase.value)
        assertNull(viewModel.activeDicePresentation.value)
        assertEquals(5, viewModel.presentedBoardPlayers.value.first().position)
    }

    @Test
    fun `card go to jail uses direct jail movement`() = runTest(dispatcher) {
        val card = ChanceCard(
            id = 30,
            description = "Go directly to jail",
            action = CardAction.GO_TO_JAIL
        )
        val doubleRoll = DiceRoll(3, 3)
        viewModel.setCurrentActionCard(card)

        emit(
            snapshot(
                state(
                    position = 6,
                    phase = GamePhase.BUYING,
                    diceRoll = doubleRoll,
                    actionCard = card,
                    consecutiveDoublets = 1
                )
            )
        )
        runCurrent()
        emit(
            event(
                "ACTION_EXECUTED",
                state(
                    position = 10,
                    phase = GamePhase.TURN_END,
                    diceRoll = doubleRoll,
                    inJail = true,
                    consecutiveDoublets = 0
                )
            )
        )
        runCurrent()

        assertEquals(listOf(10), viewModel.movementAnimation.value?.path)

        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(10, viewModel.presentedBoardPlayers.value.first().position)
        assertFalse(viewModel.actionGates.value.canRollAgainAfterDouble)
        assertFalse(viewModel.actionGates.value.canUseJailAction)
        assertTrue(viewModel.actionGates.value.canEndTurn)
    }

    @Test
    fun `turn timeout hard sync clears active presentation`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()
        emit(diceRolled(state(position = 4, phase = GamePhase.BUYING, diceRoll = DiceRoll(2, 2))))
        runCurrent()

        assertNotNull(viewModel.activeDicePresentation.value)

        emit(event("TURN_TIMEOUT", state(position = 9, phase = GamePhase.ROLLING)))
        runCurrent()

        assertNull(viewModel.activeDicePresentation.value)
        assertEquals(9, viewModel.presentedBoardPlayers.value.first().position)
        assertNotEquals(GameViewModel.TurnPresentationPhase.MOVING_TOKEN, viewModel.presentationPhase.value)
    }

    @Test
    fun `hard sync flushes logs buffered during active presentation`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        emit(diceRolled(state(position = 3, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 2))))
        runCurrent()

        emit(
            event(
                "RENT_DUE",
                state(
                    position = 3,
                    phase = GamePhase.PAYING_RENT,
                    diceRoll = DiceRoll(1, 2),
                    pendingPayment = PendingPayment(
                        amount = 100,
                        source = PaymentSource.RENT,
                        sourceFieldId = 3,
                        creditorPlayerId = "p2"
                    )
                )
            )
        )
        runCurrent()

        assertTrue(viewModel.presentedEventLog.value.none { it.eventType == "DICE_ROLLED" })
        assertTrue(viewModel.presentedEventLog.value.none { it.eventType == "RENT_DUE" })

        emit(event("TURN_TIMEOUT", state(position = 9, phase = GamePhase.ROLLING)))
        runCurrent()

        val eventTypes = viewModel.presentedEventLog.value.map { it.eventType }
        assertTrue(eventTypes.contains("DICE_ROLLED"))
        assertTrue(eventTypes.contains("RENT_DUE"))
        assertTrue(eventTypes.contains("TURN_TIMEOUT"))
    }

    @Test
    fun `late local dice response after roll timeout hard syncs without dice presentation`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        viewModel.rollDice()
        runCurrent()

        assertNotNull(viewModel.activeDicePresentation.value)

        advanceTimeBy(5_000L)
        runCurrent()

        assertNull(viewModel.activeDicePresentation.value)
        assertEquals(
            GameViewModel.TurnPresentationPhase.WAITING_FOR_ROLL_INPUT,
            viewModel.presentationPhase.value
        )

        emit(diceRolled(state(position = 4, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 3))))
        runCurrent()

        assertNull(viewModel.activeDicePresentation.value)
        assertEquals(4, viewModel.presentedBoardPlayers.value.first().position)
        assertEquals(GameViewModel.TurnPresentationPhase.READY_FOR_ACTION, viewModel.presentationPhase.value)
        assertTrue(viewModel.presentedEventLog.value.any { it.eventType == "DICE_ROLLED" })
    }

    @Test
    fun `jail action events preserve presented token position`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.BUYING)))
        runCurrent()

        emit(event("JAIL_FINE_PAID", state(position = 10, phase = GamePhase.ROLLING)))
        runCurrent()

        assertEquals(0, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.position)
        assertTrue(viewModel.actionGates.value.canRollDice)
        assertTrue(viewModel.presentedEventLog.value.any { it.eventType == "JAIL_FINE_PAID" })

        emit(event("JAIL_CARD_USED", state(position = 10, phase = GamePhase.ROLLING)))
        runCurrent()

        assertEquals(0, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.position)
        assertTrue(viewModel.actionGates.value.canRollDice)
        assertTrue(viewModel.presentedEventLog.value.any { it.eventType == "JAIL_CARD_USED" })
    }

    @Test
    fun `double roll waits for rolling phase before allowing roll again`() = runTest(dispatcher) {
        emit(snapshot(state(position = 38, phase = GamePhase.ROLLING)))
        runCurrent()

        emit(diceRolled(state(position = 0, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 1))))
        runCurrent()

        assertFalse(viewModel.actionGates.value.canRollDice)
        assertFalse(viewModel.actionGates.value.canEndTurn)

        advanceTimeBy(4_000L)
        runCurrent()

        assertFalse(viewModel.actionGates.value.canRollDice)
        assertTrue(viewModel.actionGates.value.canRollAgainAfterDouble)
        assertFalse(viewModel.actionGates.value.canEndTurn)
        assertNull(viewModel.activeDicePresentation.value)

        viewModel.rollDice()
        assertEquals(0, fakeService.rollDiceCalls)
        assertNull(viewModel.activeDicePresentation.value)

        viewModel.rollAgainAfterDouble()
        viewModel.rollAgainAfterDouble()
        runCurrent()

        assertEquals(1, fakeService.endTurnCalls)
        assertFalse(viewModel.actionGates.value.canRollAgainAfterDouble)

        emit(event("TURN_ENDED", state(position = 0, phase = GamePhase.ROLLING, diceRoll = DiceRoll(1, 1))))
        runCurrent()

        assertEquals(1, fakeService.rollDiceCalls)
        assertEquals(GameViewModel.TurnPresentationPhase.ROLLING_DICE, viewModel.presentationPhase.value)
        assertNotNull(viewModel.activeDicePresentation.value)
        assertFalse(viewModel.actionGates.value.canRollDice)
        assertFalse(viewModel.actionGates.value.canRollAgainAfterDouble)
        assertFalse(viewModel.actionGates.value.canEndTurn)
    }

    @Test
    fun `cheat can be armed for repeated queued rolls after doubles`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 1))))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canRollAgainAfterDouble)
        assertTrue(viewModel.actionGates.value.canActivateCheat)

        viewModel.activateCheatForNextRoll()
        viewModel.rollAgainAfterDouble()
        runCurrent()

        emit(event("TURN_ENDED", state(position = 0, phase = GamePhase.ROLLING, diceRoll = DiceRoll(1, 1))))
        runCurrent()

        assertEquals(listOf(true), fakeService.rollDiceCheatFlags)

        emit(snapshot(state(position = 0, phase = GamePhase.BUYING, diceRoll = DiceRoll(2, 2))))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canRollAgainAfterDouble)
        assertTrue(viewModel.actionGates.value.canActivateCheat)

        viewModel.activateCheatForNextRoll()
        viewModel.rollAgainAfterDouble()
        runCurrent()

        emit(event("TURN_ENDED", state(position = 0, phase = GamePhase.ROLLING, diceRoll = DiceRoll(2, 2))))
        runCurrent()

        assertEquals(listOf(true, true), fakeService.rollDiceCheatFlags)
    }

    @Test
    fun `rejected roll attempt clears armed cheat before next valid roll`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.BUYING, diceRoll = DiceRoll(1, 1))))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canActivateCheat)
        viewModel.activateCheatForNextRoll()

        viewModel.rollDice()
        runCurrent()

        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        viewModel.rollDice()
        runCurrent()

        assertEquals(listOf(false), fakeService.rollDiceCheatFlags)
    }

    @Test
    fun `roll again after double is blocked while required card draw is pending or in flight`() = runTest(dispatcher) {
        emit(snapshot(state(position = 1, phase = GamePhase.BUYING, diceRoll = DiceRoll(2, 2))))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canDrawCard)
        assertFalse(viewModel.actionGates.value.canRollAgainAfterDouble)

        viewModel.drawCard("CHANCE")
        runCurrent()

        assertFalse(viewModel.actionGates.value.canDrawCard)
        assertFalse(viewModel.actionGates.value.canRollAgainAfterDouble)
    }

    @Test
    fun `duplicate cheater reports are ignored while request is in flight`() = runTest(dispatcher) {
        fakeService.currentPlayerId = "p2"
        emit(snapshot(state(position = 0, phase = GamePhase.BUYING, diceRoll = DiceRoll(3, 4), otherMoney = 600)))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canReportCheater)

        viewModel.reportCheater("p1")
        viewModel.reportCheater("p1")
        runCurrent()

        assertEquals(1, fakeService.reportCheaterCalls)
        assertFalse(viewModel.actionGates.value.canReportCheater)

        emit(event("CHEATER_REPORTED", state(position = 0, phase = GamePhase.BUYING, diceRoll = DiceRoll(3, 4), otherMoney = 600)))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canReportCheater)
    }

    @Test
    fun `cheater report updates balances during active presentation`() = runTest(dispatcher) {
        val roll = DiceRoll(1, 2)
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING, money = 1000, otherMoney = 1000)))
        runCurrent()

        emit(diceRolled(state(position = 3, phase = GamePhase.BUYING, diceRoll = roll, money = 1000, otherMoney = 1000)))
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.SHOWING_DICE_RESULT, viewModel.presentationPhase.value)
        assertEquals(0, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.position)

        emit(
            event(
                "CHEATER_REPORT_FAILED",
                state(position = 3, phase = GamePhase.BUYING, diceRoll = roll, money = 500, otherMoney = 1500)
            )
        )
        runCurrent()

        val reportedPlayers = viewModel.presentedBoardPlayers.value
        assertEquals(0, reportedPlayers.first { it.id == "p1" }.position)
        assertEquals(500, reportedPlayers.first { it.id == "p1" }.money)
        assertEquals(1500, reportedPlayers.first { it.id == "p2" }.money)
    }

    @Test
    fun `duplicate card draws are ignored while request is in flight`() = runTest(dispatcher) {
        emit(snapshot(state(position = 1, phase = GamePhase.BUYING)))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canDrawChance)

        viewModel.drawCard("CHANCE")
        viewModel.drawCard("CHANCE")
        runCurrent()

        assertEquals(1, fakeService.drawCardCalls)
        assertFalse(viewModel.actionGates.value.canDrawChance)
    }

    @Test
    fun `duplicate roll taps are ignored while request is in flight`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING)))
        runCurrent()

        viewModel.rollDice()
        viewModel.rollDice()
        runCurrent()

        assertEquals(1, fakeService.rollDiceCalls)
    }

    @Test
    fun `duplicate action executions are ignored while request is in flight`() = runTest(dispatcher) {
        val card = ChanceCard(
            id = 11,
            description = "Collect carefully",
            action = CardAction.COLLECT_MONEY,
            amount = 100
        )

        emit(snapshot(state(position = 1, phase = GamePhase.BUYING, actionCard = card)))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canExecuteCard)

        viewModel.executeAction()
        viewModel.executeAction()
        runCurrent()

        assertEquals(1, fakeService.executeActionCalls)
        assertFalse(viewModel.actionGates.value.canExecuteCard)
    }

    @Test
    fun `action execution lock clears after timeout`() = runTest(dispatcher) {
        val card = ChanceCard(
            id = 12,
            description = "Collect eventually",
            action = CardAction.COLLECT_MONEY,
            amount = 100
        )

        emit(snapshot(state(position = 1, phase = GamePhase.BUYING, actionCard = card)))
        runCurrent()

        viewModel.executeAction()
        runCurrent()

        assertFalse(viewModel.actionGates.value.canExecuteCard)

        advanceTimeBy(5_000L)
        runCurrent()

        assertTrue(viewModel.actionGates.value.canExecuteCard)
    }

    @Test
    fun `stale action executed does not use dismissed local action card fallback`() = runTest(dispatcher) {
        val staleCard = ChanceCard(
            id = 30,
            description = "Go directly to jail",
            action = CardAction.GO_TO_JAIL
        )

        emit(snapshot(state(position = 6, phase = GamePhase.BUYING)))
        runCurrent()

        viewModel.setCurrentActionCard(staleCard)
        viewModel.dismissActionCard()
        runCurrent()

        emit(event("ACTION_EXECUTED", state(position = 8, phase = GamePhase.BUYING)))
        runCurrent()

        assertEquals(listOf(7, 8), viewModel.movementAnimation.value?.path)
    }

    @Test
    fun `dismissing bankruptcy overlay keeps raw bankruptcy action blocker`() = runTest(dispatcher) {
        val bankruptState = GameState(
            gameId = "game-1",
            fields = boardFields(),
            players = mutableListOf(
                Player(id = "p1", name = "Alice", position = 0, money = 0),
                Player(id = "p2", name = "Bob", position = 0, money = 500)
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.TURN_END,
            bankruptcyPlayerId = "p1",
            bankruptcyTotalAssets = 0,
            bankruptcyTotalDebt = 100
        )

        emit(event("BANKRUPTCY_DECLARED", bankruptState))
        runCurrent()

        assertTrue(viewModel.showBankruptcyOverlay.value)
        assertFalse(viewModel.actionGates.value.canEndTurn)

        viewModel.dismissBankruptcyOverlay()
        runCurrent()

        assertFalse(viewModel.showBankruptcyOverlay.value)
        assertFalse(viewModel.actionGates.value.canEndTurn)

        emit(event("STATE_UPDATED", state(position = 0, phase = GamePhase.TURN_END)))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canEndTurn)
    }

    @Test
    fun `double roll advance timeout clears queued roll`() = runTest(dispatcher) {
        emit(snapshot(state(position = 0, phase = GamePhase.BUYING, diceRoll = DiceRoll(2, 2))))
        runCurrent()

        assertTrue(viewModel.actionGates.value.canRollAgainAfterDouble)

        viewModel.rollAgainAfterDouble()
        runCurrent()
        assertEquals(1, fakeService.endTurnCalls)

        advanceTimeBy(5_000L)
        runCurrent()

        emit(event("TURN_ENDED", state(position = 0, phase = GamePhase.ROLLING, diceRoll = DiceRoll(2, 2))))
        runCurrent()

        assertEquals(0, fakeService.rollDiceCalls)
        assertFalse(viewModel.actionGates.value.canRollAgainAfterDouble)
    }

    @Test
    fun `presentedBoardPlayers money remains old during movement and flips after landing reveal`() = runTest(dispatcher) {
        val oldMoney = 500
        val newMoney = 400
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING, money = oldMoney)))
        runCurrent()

        // Alice rolls a 3, landing on a field where she pays 100 tax.
        // We use DICE_ROLLED to trigger movement.
        emit(
            event(
                "DICE_ROLLED",
                state(
                    position = 3,
                    phase = GamePhase.BUYING,
                    money = newMoney,
                    diceRoll = DiceRoll(1, 2)
                )
            )
        )
        runCurrent()

        // During SHOWING_DICE_RESULT, position should be 0 and money should be 500
        assertEquals(0, viewModel.presentedBoardPlayers.value.first().position)
        assertEquals(oldMoney, viewModel.presentedBoardPlayers.value.first().money)

        advanceTimeBy(2_000L) // DICE_RESULT_PRESENTATION_MS
        runCurrent()

        // During MOVING_TOKEN, money should STILL be 500 even as token moves
        assertEquals(GameViewModel.TurnPresentationPhase.MOVING_TOKEN, viewModel.presentationPhase.value)
        assertEquals(oldMoney, viewModel.presentedBoardPlayers.value.first().money)

        advanceTimeBy(3 * 250L) // MOVEMENT_STEP_MS * 3 steps
        runCurrent()

        // Movement finished, now in REVEALING_LANDING_EFFECT
        assertEquals(GameViewModel.TurnPresentationPhase.REVEALING_LANDING_EFFECT, viewModel.presentationPhase.value)
        // Now money should have flipped to 400
        assertEquals(newMoney, viewModel.presentedBoardPlayers.value.first().money)
        assertEquals(3, viewModel.presentedBoardPlayers.value.first().position)
    }

    @Test
    fun `tax paid event during movement updates money at landing reveal`() = runTest(dispatcher) {
        val oldMoney = 500
        val taxedMoney = 300
        val roll = DiceRoll(1, 3)
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING, money = oldMoney)))
        runCurrent()

        emit(event("DICE_ROLLED", state(position = 4, phase = GamePhase.BUYING, money = oldMoney, diceRoll = roll)))
        runCurrent()

        emit(event("TAX_PAID", state(position = 4, phase = GamePhase.BUYING, money = taxedMoney, diceRoll = roll)))
        runCurrent()

        assertEquals(0, viewModel.presentedBoardPlayers.value.first().position)
        assertEquals(oldMoney, viewModel.presentedBoardPlayers.value.first().money)

        advanceTimeBy(2_000L)
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.MOVING_TOKEN, viewModel.presentationPhase.value)
        assertEquals(oldMoney, viewModel.presentedBoardPlayers.value.first().money)

        advanceTimeBy(4 * 250L)
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.REVEALING_LANDING_EFFECT, viewModel.presentationPhase.value)
        assertEquals(4, viewModel.presentedBoardPlayers.value.first().position)
        assertEquals(taxedMoney, viewModel.presentedBoardPlayers.value.first().money)
    }

    @Test
    fun `rent paid during landing reveal immediately updates presented player balances`() = runTest(dispatcher) {
        val oldMoney = 500
        val oldOwnerMoney = 700
        val paidMoney = 400
        val paidOwnerMoney = 800
        val roll = DiceRoll(1, 2)
        val pendingPayment = PendingPayment(
            amount = 100,
            source = PaymentSource.RENT,
            sourceFieldId = 3,
            creditorPlayerId = "p2",
            debtorPlayerId = "p1"
        )
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING, money = oldMoney, otherMoney = oldOwnerMoney)))
        runCurrent()

        emit(event("DICE_ROLLED", state(position = 3, phase = GamePhase.BUYING, money = oldMoney, otherMoney = oldOwnerMoney, diceRoll = roll)))
        runCurrent()

        emit(
            event(
                "RENT_DUE",
                state(
                    position = 3,
                    phase = GamePhase.PAYING_RENT,
                    money = oldMoney,
                    otherMoney = oldOwnerMoney,
                    diceRoll = roll,
                    pendingPayment = pendingPayment
                )
            )
        )
        runCurrent()

        advanceTimeBy(2_000L)
        runCurrent()
        advanceTimeBy(3 * 250L)
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.REVEALING_LANDING_EFFECT, viewModel.presentationPhase.value)
        assertEquals(oldMoney, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.money)
        assertEquals(oldOwnerMoney, viewModel.presentedBoardPlayers.value.first { it.id == "p2" }.money)

        emit(
            event(
                "RENT_PAID",
                state(
                    position = 3,
                    phase = GamePhase.TURN_END,
                    money = paidMoney,
                    otherMoney = paidOwnerMoney,
                    diceRoll = roll
                )
            )
        )
        runCurrent()

        assertEquals(3, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.position)
        assertEquals(paidMoney, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.money)
        assertEquals(paidOwnerMoney, viewModel.presentedBoardPlayers.value.first { it.id == "p2" }.money)
        assertFalse(viewModel.showPayRentOverlay.value)
    }

    @Test
    fun `rent paid buffered during movement clears rent due overlay at landing reveal`() = runTest(dispatcher) {
        val oldMoney = 500
        val oldOwnerMoney = 700
        val paidMoney = 400
        val paidOwnerMoney = 800
        val roll = DiceRoll(1, 2)
        val pendingPayment = PendingPayment(
            amount = 100,
            source = PaymentSource.RENT,
            sourceFieldId = 3,
            creditorPlayerId = "p2",
            debtorPlayerId = "p1"
        )
        emit(snapshot(state(position = 0, phase = GamePhase.ROLLING, money = oldMoney, otherMoney = oldOwnerMoney)))
        runCurrent()

        emit(event("DICE_ROLLED", state(position = 3, phase = GamePhase.BUYING, money = oldMoney, otherMoney = oldOwnerMoney, diceRoll = roll)))
        runCurrent()

        emit(
            event(
                "RENT_DUE",
                state(
                    position = 3,
                    phase = GamePhase.PAYING_RENT,
                    money = oldMoney,
                    otherMoney = oldOwnerMoney,
                    diceRoll = roll,
                    pendingPayment = pendingPayment
                )
            )
        )
        runCurrent()

        emit(
            event(
                "RENT_PAID",
                state(
                    position = 3,
                    phase = GamePhase.TURN_END,
                    money = paidMoney,
                    otherMoney = paidOwnerMoney,
                    diceRoll = roll
                )
            )
        )
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.SHOWING_DICE_RESULT, viewModel.presentationPhase.value)
        assertFalse(viewModel.showPayRentOverlay.value)

        advanceTimeBy(2_000L)
        runCurrent()
        advanceTimeBy(3 * 250L)
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.REVEALING_LANDING_EFFECT, viewModel.presentationPhase.value)
        assertFalse(viewModel.showPayRentOverlay.value)
        assertNull(viewModel.visiblePaymentState.value)
        assertEquals(paidMoney, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.money)
        assertEquals(paidOwnerMoney, viewModel.presentedBoardPlayers.value.first { it.id == "p2" }.money)
    }

    @Test
    fun `property bought immediately updates presented player balance`() = runTest(dispatcher) {
        emit(snapshot(state(position = 5, phase = GamePhase.BUYING, money = 500)))
        runCurrent()

        emit(event("PROPERTY_BOUGHT", state(position = 5, phase = GamePhase.BUYING, money = 440)))
        runCurrent()

        assertEquals(5, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.position)
        assertEquals(440, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.money)
    }

    @Test
    fun `legacy pending payment blocks local property spend action`() = runTest(dispatcher) {
        emit(
            snapshot(
                state(
                    position = 0,
                    phase = GamePhase.PAYING_RENT,
                    pendingPayment = PendingPayment(amount = 100, source = PaymentSource.TAX)
                )
            )
        )
        runCurrent()

        assertTrue(viewModel.actionGates.value.canManageProperties)

        viewModel.buyHouse(1)

        assertFalse(fakeService.buyHouseCalled)
    }

    @Test
    fun `MOVE_TO nearest railroad sentinel animates to resolved backend position`() = runTest(dispatcher) {
        val card = ChanceCard(
            id = 101,
            description = "Advance to nearest railroad",
            action = CardAction.MOVE_TO,
            targetFieldId = -1
        )
        emit(snapshot(state(position = 7, phase = GamePhase.BUYING, actionCard = card)))
        runCurrent()

        emit(event("ACTION_EXECUTED", state(position = 15, phase = GamePhase.BUYING)))
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.MOVING_TOKEN, viewModel.presentationPhase.value)
        assertEquals(15, viewModel.movementAnimation.value?.path?.last())
        assertEquals(7, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.position)

        advanceTimeBy(8 * 250L)
        runCurrent()

        assertEquals(15, viewModel.presentedBoardPlayers.value.first { it.id == "p1" }.position)
    }

    @Test
    fun `MOVE_TO 30 via card is animated before jailing`() = runTest(dispatcher) {
        val card = ChanceCard(
            id = 100,
            description = "Travel to field 30",
            action = CardAction.MOVE_TO,
            targetFieldId = 30
        )
        // Simulate backend reordering: ACTION_EXECUTED comes first
        emit(snapshot(state(position = 28, phase = GamePhase.BUYING, actionCard = card)))
        runCurrent()

        // ACTION_EXECUTED triggers movement animation to 30
        emit(event("ACTION_EXECUTED", state(position = 30, phase = GamePhase.BUYING)))
        runCurrent()

        assertEquals(GameViewModel.TurnPresentationPhase.MOVING_TOKEN, viewModel.presentationPhase.value)
        assertEquals(listOf(29, 30), viewModel.movementAnimation.value?.path)

        // While moving, PLAYER_JAILED arrives
        emit(event("PLAYER_JAILED", state(position = 10, phase = GamePhase.TURN_END, inJail = true)))
        runCurrent()

        // It MUST be buffered (stay in MOVING_TOKEN to 30)
        assertEquals(GameViewModel.TurnPresentationPhase.MOVING_TOKEN, viewModel.presentationPhase.value)
        assertEquals(30, viewModel.movementAnimation.value?.path?.last())

        advanceTimeBy(1000L) // Finish movement
        runCurrent()

        // After movement and reveal, it should finally flip to jail position 10
        assertEquals(10, viewModel.presentedBoardPlayers.value.first().position)
    }

    private fun state(
        position: Int,
        phase: GamePhase,
        diceRoll: DiceRoll? = null,
        pendingPayment: PendingPayment? = null,
        actionCard: ChanceCard? = null,
        inJail: Boolean = false,
        money: Int = 500,
        otherMoney: Int = 500,
        consecutiveDoublets: Int = if (diceRoll?.isDouble == true) 1 else 0
    ): GameState =
        GameState(
            gameId = "game-1",
            fields = boardFields(),
            players = mutableListOf(
                Player(
                    id = "p1",
                    name = "Alice",
                    position = position,
                    money = money,
                    inJail = inJail,
                    consecutiveDoublets = consecutiveDoublets
                ),
                Player(id = "p2", name = "Bob", position = 0, money = otherMoney)
            ),
            currentPlayerIndex = 0,
            phase = phase,
            lastDiceRoll = diceRoll,
            currentActionCard = actionCard,
            pendingPayment = pendingPayment
        )

    private fun boardFields(): List<Field> =
        listOf(GoField(id = 0)) + (1 until 40).map { id -> ChanceField(id = id, name = "Field $id") }

    private fun snapshot(state: GameState): GameEvent =
        event("STATE_SNAPSHOT", state)

    private fun diceRolled(state: GameState): GameEvent =
        event("DICE_ROLLED", state)

    private fun event(eventType: String, state: GameState): GameEvent =
        GameEvent(gameId = "game-1", event = eventType, gameState = state)

    private suspend fun emit(event: GameEvent) {
        fakeService.emitTestEvent(objectMapper.writeValueAsString(event))
    }
}
