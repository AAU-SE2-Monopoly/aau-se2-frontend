package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.networking.GameService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelJailLogicTest {

    private lateinit var gameService: GameService
    private lateinit var viewModel: GameViewModel


    private lateinit var eventsFlow: MutableSharedFlow<String>
    private lateinit var logEventsFlow: MutableSharedFlow<String>

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())


        eventsFlow = MutableSharedFlow(replay = 10)
        logEventsFlow = MutableSharedFlow(replay = 10)

        gameService = mockk(relaxed = true) {
            every { events } returns eventsFlow
            every { logEvents } returns logEventsFlow
            every { currentGameId } returns "test-game-id"
            every { currentPlayerId } returns "player-1"
        }

        // Action-Mocks vorbereiten
        every { gameService.payJailFine() } just Runs
        every { gameService.useJailCard() } just Runs
        every { gameService.rollDice(any()) } just Runs


        viewModel = GameViewModel(gameService, currentTimeProvider = { 1000L })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `payJailFine delegates directly to gameService`() = runTest {
        viewModel.payJailFine()
        verify(exactly = 1) { gameService.payJailFine() }
    }

    @Test
    fun `useJailCard delegates directly to gameService`() = runTest {
        viewModel.useJailCard()
        verify(exactly = 1) { gameService.useJailCard() }
    }

    @Test
    fun `Jail events are mapped to correct human readable log entries`() = runTest {

        val job = backgroundScope.launch { viewModel.eventLog.collect {} }

        // JSON-Payload für JAIL_FINE_PAID simulieren
        val finePaidEventJson = """
            {
                "event": "JAIL_FINE_PAID",
                "gameId": "test-game-id"
            }
        """.trimIndent()

        // JSON-Payload für JAIL_CARD_USED simulieren
        val cardUsedEventJson = """
            {
                "event": "JAIL_CARD_USED",
                "gameId": "test-game-id"
            }
        """.trimIndent()

        // JSON-Payload für PLAYER_JAILED simulieren
        val playerJailedEventJson = """
            {
                "event": "PLAYER_JAILED",
                "gameId": "test-game-id"
            }
        """.trimIndent()

        // Events auf den Log-Flow pushen
        logEventsFlow.emit(finePaidEventJson)
        logEventsFlow.emit(cardUsedEventJson)
        logEventsFlow.emit(playerJailedEventJson)

        advanceUntilIdle() // Warten bis die StateFlows aktualisiert sind

        val logs = viewModel.eventLog.value

        assertTrue("Log should contain 'Bail paid: 50M'", logs.any { it.text == "Bail paid: 50M" })
        assertTrue("Log should contain 'Used 'Get out of jail free' card'", logs.any { it.text == "Used 'Get out of jail free' card" })
        assertTrue("Log should contain 'Player went to jail!'", logs.any { it.text == "Player went to jail!" })

        job.cancel()
    }

    @Test
    fun `GameState parses inJail status and properties correctly`() = runTest {
        val job = backgroundScope.launch { viewModel.gameState.collect {} }

        val gameStateEventJson = """
            {
                "event": "STATE_UPDATED",
                "gameId": "test-game-id",
                "gameState": {
                    "gameId": "test-game-id",
                    "fields": [],
                    "players": [
                        {
                            "id": "player-1",
                            "name": "Joachim",
                            "position": 10,
                            "money": 1000,
                            "inJail": true,
                            "jailTurns": 2,
                            "getOutOfJailCards": 1,
                            "consecutiveDoublets": 0,
                            "ownedPropertyIds": []
                        }
                    ],
                    "currentPlayerIndex": 0,
                    "phase": "ROLLING"
                }
            }
        """.trimIndent()

        eventsFlow.emit(gameStateEventJson)
        advanceUntilIdle()

        val state = viewModel.gameState.value
        val currentPlayer = state?.players?.find { it.id == "player-1" }


        assertTrue("Game state should not be null", state != null)
        assertTrue("Player should be in jail", currentPlayer?.inJail == true)
        assertEquals("Player should be at position 10", 10, currentPlayer?.position)
        assertEquals("Player should have 2 jail turns", 2, currentPlayer?.jailTurns)
        assertEquals("Player should have 1 get out of jail card", 1, currentPlayer?.getOutOfJailCards)

        job.cancel()
    }
}