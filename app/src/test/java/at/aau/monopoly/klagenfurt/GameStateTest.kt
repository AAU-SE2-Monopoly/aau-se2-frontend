package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.model.DiceRoll
import at.aau.monopoly.klagenfurt.model.GameState
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import kotlin.collections.emptyList

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
    /** GameState Companion fromJson Tests**/


    @Test
    fun `test GameState fromJson with valid JSON`() {
        val json = JSONObject()
            .put("gameId", "1")
            .put("phase", "ROLLING")
            .put("freeParkingMoney", 1000)
            .put("currentPlayerIndex", 1)
            .put("lastDiceRoll", JSONObject().put("die1", 3).put("die2", 4))
            .put("players", JSONArray().apply {
                put(JSONObject().put("id", "p1").put("name", "Alice").put("money", 1500).put("position", 0).put("properties", JSONArray()))
                put(JSONObject().put("id", "p2").put("name", "Bob").put("money", 1500).put("position", 0).put("properties", JSONArray()))
            })
            .put("fields", JSONArray())
        val state = GameState.fromJson(json)
        assertEquals("1", state.gameId)
        assertEquals(GamePhase.ROLLING, state.phase)
        assertEquals(1000, state.freeParkingMoney)
        assertEquals(1, state.currentPlayerIndex)
        assertNotNull(state.lastDiceRoll)
        assertEquals(2, state.players.size)
        assertNotNull(state.fields)
    }
    @Test
    fun `test GameState fromJson with missing required fields`() {
        val json = JSONObject()
            .put("phase", "ROLLING")
         assertThrows< JSONException> {
            GameState.fromJson(json)
        }
    }

}



