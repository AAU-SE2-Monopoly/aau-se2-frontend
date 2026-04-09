package at.aau.serg.websocketbrokerdemo.at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketbrokerdemo.model.DiceRoll
import at.aau.serg.websocketbrokerdemo.model.GameState
import at.aau.serg.websocketbrokerdemo.model.Player
import at.aau.serg.websocketbrokerdemo.model.enums.GamePhase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class GameStateTest {
    private lateinit var player1: Player
    private lateinit var player2: Player

    @BeforeEach
    fun setup() {
        player1 = Player(id = "p1", name = "Alice")
        player2 = Player(id = "p2", name = "Bob")
    }

    @Test
    fun `test currentPlayer returns correct player or null`() {
        val state = GameState(gameId = "game123", fields = emptyList())

        // noplayers
        assertNull(state.currentPlayer)

        // with players
        state.players.addAll(listOf(player1, player2))
        assertEquals(player1, state.currentPlayer)

        // move index
        state.currentPlayerIndex = 1
        assertEquals(player2, state.currentPlayer)
    }

    @Test
    fun `test advanceTurn updates index and phase`() {
        val state = GameState(
            gameId = "game123",
            fields = emptyList(),
            players = mutableListOf(player1, player2),
            phase = GamePhase.WAITING
        )

        //0 to 1
        state.advanceTurn()
        assertEquals(1, state.currentPlayerIndex)
        assertEquals(player2, state.currentPlayer)
        assertEquals(GamePhase.ROLLING, state.phase)

        // wrap around 1 to 0
        state.advanceTurn()
        assertEquals(0, state.currentPlayerIndex)
        assertEquals(player1, state.currentPlayer)
    }

    @Test
    fun `test isGameOver logic`() {
        val state = GameState(gameId = "game123", fields = emptyList())

        // noplayers
        assertTrue(state.isGameOver())
            //both player have money and properties
        state.players.addAll(listOf(player1, player2))
        assertFalse(state.isGameOver())

        player1.money = -1
        assertTrue(state.isGameOver())
    }

    @Test
    fun `test data class coverage for GameState`() {
        val state1 = GameState(gameId = "1", fields = emptyList())
        val state2 = state1.copy(gameId = "2")

        assertNotEquals(state1, state2)
        assertNotNull(state1.toString())
        assertEquals(state1.hashCode(), state1.hashCode())

        state1.freeParkingMoney = 500
        assertEquals(500, state1.freeParkingMoney)

        state1.lastDiceRoll = DiceRoll(3, 4)
        assertNotNull(state1.lastDiceRoll)
    }
}
