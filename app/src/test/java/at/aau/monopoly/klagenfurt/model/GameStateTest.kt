package at.aau.monopoly.klagenfurt.model

import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.GoField
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameStateTest {

    private fun makeState(playerCount: Int = 2): GameState {
        val players = (1..playerCount).map { Player(id = "p$it", name = "Player$it") }.toMutableList()
        return GameState(gameId = "g1", fields = listOf(GoField()), players = players)
    }

    @Test
    fun `currentPlayer returns first player initially`() {
        val state = makeState()
        assertEquals("p1", state.currentPlayer?.id)
    }

    @Test
    fun `currentPlayer returns null for empty players`() {
        val state = GameState(gameId = "g1", fields = emptyList())
        assertNull(state.currentPlayer)
    }

    @Test
    fun `advanceTurn moves to next player and resets phase`() {
        val state = makeState(3)
        state.phase = GamePhase.BUYING

        state.advanceTurn()

        assertEquals(1, state.currentPlayerIndex)
        assertEquals("p2", state.currentPlayer?.id)
        assertEquals(GamePhase.ROLLING, state.phase)
    }

    @Test
    fun `advanceTurn wraps around`() {
        val state = makeState(2)
        state.advanceTurn() // -> p2
        state.advanceTurn() // -> p1
        assertEquals(0, state.currentPlayerIndex)
    }

    @Test
    fun `advanceTurn with empty players does not crash`() {
        val state = GameState(gameId = "g1", fields = emptyList())
        state.advanceTurn()
        assertEquals(0, state.currentPlayerIndex)
    }

    @Test
    fun `isGameOver returns true when one or fewer non-bankrupt`() {
        val state = makeState(3)
        assertFalse(state.isGameOver())

        state.players[0].money = 0
        state.players[0].ownedPropertyIds.clear()
        state.players[1].money = 0
        state.players[1].ownedPropertyIds.clear()
        assertTrue(state.isGameOver())
    }

    @Test
    fun `isGameOver with all bankrupt`() {
        val state = makeState(2)
        state.players.forEach {
            it.money = 0
            it.ownedPropertyIds.clear()
        }
        assertTrue(state.isGameOver())
    }

    @Test
    fun `pendingPayment is null by default`() {
        val state = makeState()
        assertNull(state.pendingPayment)
    }

    @Test
    fun `pendingTradeOffer is null by default`() {
        val state = makeState()
        assertNull(state.pendingTradeOffer)
    }

    @Test
    fun `GameState can carry pendingTradeOffer with accepted players`() {
        val offer = TradeOffer(
            id = "trade-1",
            fromPlayerId = "p1",
            toPlayerId = "p2",
            offerMoney = 100,
            requestMoney = 20,
            offerPropertyIds = listOf(1, 3),
            requestPropertyIds = listOf(6),
            offerJailCards = 1,
            requestJailCards = 0,
            acceptedByPlayerIds = listOf("p1")
        )
        val state = makeState().copy(pendingTradeOffer = offer)

        assertEquals(offer, state.pendingTradeOffer)
        assertEquals(listOf("p1"), state.pendingTradeOffer?.acceptedByPlayerIds)
    }

    @Test
    fun `freeParkingMoney defaults to zero`() {
        val state = makeState()
        assertEquals(0, state.freeParkingMoney)
    }
}

