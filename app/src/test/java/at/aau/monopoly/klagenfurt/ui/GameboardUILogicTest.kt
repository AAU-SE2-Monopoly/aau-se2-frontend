package at.aau.monopoly.klagenfurt.ui

import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.GoField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GameboardUI logic and helper functions.
 * Some Compose-specific renders are tested in integration tests (GameboardUITest.kt).
 * These focus on the business logic and state derivations.
 */
class GameboardUILogicTest {
    @Test
    fun `testGameStateFieldCalculations`() {
        val fields = listOf(
            GoField(0, "Go"),
            GoField(1, "Field 1"),
            GoField(2, "Field 2"),
            GoField(3, "Field 3")
        )

        val players = mutableListOf(
            Player(id = "p1", name = "Alice", position = 0),
            Player(id = "p2", name = "Bob", position = 1),
            Player(id = "p3", name = "Charlie", position = 2)
        )

        val gameState = GameState(
            gameId = "test-game",
            fields = fields,
            players = players,
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING
        )

        assertNotNull(gameState)
        assertEquals("test-game", gameState.gameId)
        assertEquals(4, gameState.fields.size)
        assertEquals(3, gameState.players.size)
        assertEquals(0, gameState.currentPlayerIndex)
        assertEquals(GamePhase.ROLLING, gameState.phase)
    }

    @Test
    fun `testGameStatePlayerProgression`() {
        val players = (1..3).map {
            Player(id = "p$it", name = "Player$it", position = it - 1)
        }.toMutableList()

        val gameState = GameState(
            gameId = "g1",
            fields = listOf(GoField(0, "Go")),
            players = players,
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING
        )

        assertEquals("p1", gameState.currentPlayer?.id)

        gameState.currentPlayerIndex = 1
        assertEquals("p2", gameState.currentPlayer?.id)

        gameState.currentPlayerIndex = 2
        assertEquals("p3", gameState.currentPlayer?.id)
    }

    @Test
    fun `testEmptyGameState`() {
        val gameState = GameState(
            gameId = "empty",
            fields = emptyList(),
            players = mutableListOf(),
            currentPlayerIndex = 0,
            phase = GamePhase.WAITING
        )

        assertEquals(0, gameState.players.size)
        assertEquals(0, gameState.fields.size)
        assertEquals(null, gameState.currentPlayer)
        assertTrue(gameState.isGameOver())
    }

    @Test
    fun `testPlayerEliminationState`() {
        val players = mutableListOf(
            Player(id = "p1", name = "Alice", money = 1500),
            Player(id = "p2", name = "Bob", money = 0),
            Player(id = "p3", name = "Charlie", money = 500)
        )

        val gameState = GameState(
            gameId = "elim-game",
            fields = emptyList(),
            players = players,
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING
        )

        // Bob is bankrupt (money = 0)
        assertTrue(gameState.players[1].isBankrupt())
        assertFalse(gameState.players[0].isBankrupt())
        assertFalse(gameState.players[2].isBankrupt())
    }

    @Test
    fun `testMultiplePlayerGameState`() {
        val gameState = GameState(
            gameId = "multi",
            fields = listOf(GoField(0, "Go"), GoField(1, "Field1")),
            players = mutableListOf(
                Player(id = "p1", name = "Player1", position = 0, money = 1500),
                Player(id = "p2", name = "Player2", position = 1, money = 1500),
                Player(id = "p3", name = "Player3", position = 0, money = 1500),
                Player(id = "p4", name = "Player4", position = 1, money = 1500)
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.BUYING
        )

        assertEquals(4, gameState.players.size)
        assertEquals(GamePhase.BUYING, gameState.phase)

        // Group by position
        val groupedByPosition = gameState.players.groupBy { it.position }
        assertEquals(2, groupedByPosition[0]?.size)
        assertEquals(2, groupedByPosition[1]?.size)
    }

    @Test
    fun `testGameStateWithNullValues`() {
        val gameState = GameState(
            gameId = "test",
            fields = emptyList(),
            players = mutableListOf(),
            currentActionCard = null,
            pendingPayment = null
        )

        assertEquals(null, gameState.currentActionCard)
        assertEquals(null, gameState.pendingPayment)
        assertEquals(null, gameState.lastDiceRoll)
    }

    @Test
    fun `testGameStateCloning`() {
        val originalState = GameState(
            gameId = "original",
            fields = listOf(GoField(0, "Go")),
            players = mutableListOf(
                Player(id = "p1", name = "Alice", money = 1500)
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.ROLLING,
            freeParkingMoney = 100
        )

        // Simulate state mutation (as would happen during gameplay)
        originalState.players[0].money = 1300
        originalState.freeParkingMoney = 150

        assertEquals(1300, originalState.players[0].money)
        assertEquals(150, originalState.freeParkingMoney)
    }

    @Test
    fun `testGamePhaseTransitions`() {
        val gameState = GameState(
            gameId = "phases",
            fields = emptyList(),
            players = mutableListOf(
                Player(id = "p1", name = "Player1"),
                Player(id = "p2", name = "Player2")
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.WAITING
        )

        // Simulate phase transitions
        gameState.phase = GamePhase.ROLLING
        assertEquals(GamePhase.ROLLING, gameState.phase)

        gameState.phase = GamePhase.BUYING
        assertEquals(GamePhase.BUYING, gameState.phase)

        gameState.phase = GamePhase.TURN_END
        assertEquals(GamePhase.TURN_END, gameState.phase)
    }

    @Test
    fun `testPlayerPositionTracking`() {
        val player = Player(
            id = "p1",
            name = "Alice",
            position = 0,
            money = 1500
        )

        // Simulate movement around the board
        val updatedPlayer = player.copy(position = 5)
        assertEquals(5, updatedPlayer.position)
        // Original should be unchanged
        assertEquals(0, player.position)
    }

    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }

    private fun assertFalse(value: Boolean) {
        org.junit.Assert.assertFalse(value)
    }
}
