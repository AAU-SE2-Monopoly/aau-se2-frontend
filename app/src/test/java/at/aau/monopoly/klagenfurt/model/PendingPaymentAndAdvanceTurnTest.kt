package at.aau.monopoly.klagenfurt.model

import at.aau.monopoly.klagenfurt.model.enums.GamePhase
import at.aau.monopoly.klagenfurt.model.field.GoField
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PendingPaymentTest {

    @Test
    fun `PendingPayment default values`() {
        val payment = PendingPayment(amount = 100, source = PaymentSource.RENT)
        assertEquals(100, payment.amount)
        assertEquals(PaymentSource.RENT, payment.source)
        assertNull(payment.sourceFieldId)
        assertNull(payment.creditorPlayerId)
        assertFalse(payment.debtorCanPayAfterAssets)
    }

    @Test
    fun `PendingPayment with all fields set`() {
        val payment = PendingPayment(
            amount = 200,
            source = PaymentSource.CARD_PAY,
            sourceFieldId = 5,
            creditorPlayerId = "p2",
            debtorCanPayAfterAssets = true
        )
        assertEquals(200, payment.amount)
        assertEquals(PaymentSource.CARD_PAY, payment.source)
        assertEquals(5, payment.sourceFieldId)
        assertEquals("p2", payment.creditorPlayerId)
        assertTrue(payment.debtorCanPayAfterAssets)
    }

    @Test
    fun `PaymentSource has all expected values`() {
        val values = PaymentSource.entries
        assertEquals(5, values.size)
        assertNotNull(PaymentSource.valueOf("RENT"))
        assertNotNull(PaymentSource.valueOf("CARD_PAY"))
        assertNotNull(PaymentSource.valueOf("CARD_PAY_EACH"))
        assertNotNull(PaymentSource.valueOf("CARD_REPAIR"))
        assertNotNull(PaymentSource.valueOf("TAX"))

    }

    @Test
    fun `PendingPayment copy works`() {
        val original = PendingPayment(amount = 100, source = PaymentSource.RENT)
        val copy = original.copy(amount = 200, creditorPlayerId = "p3")
        assertEquals(200, copy.amount)
        assertEquals(PaymentSource.RENT, copy.source)
        assertEquals("p3", copy.creditorPlayerId)
    }

    @Test
    fun `PendingPayment equality`() {
        val a = PendingPayment(amount = 50, source = PaymentSource.CARD_PAY_EACH)
        val b = PendingPayment(amount = 50, source = PaymentSource.CARD_PAY_EACH)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `PendingPayment inequality`() {
        val a = PendingPayment(amount = 50, source = PaymentSource.RENT)
        val b = PendingPayment(amount = 100, source = PaymentSource.RENT)
        assertNotEquals(a, b)
    }
}

class GameStateAdvanceTurnTest {

    private fun makeState(vararg players: Player): GameState {
        return GameState(
            gameId = "g1",
            fields = listOf(GoField()),
            players = players.toMutableList()
        )
    }

    @Test
    fun `advanceTurn skips eliminated player`() {
        val state = makeState(
            Player(id = "p1", name = "A"),
            Player(id = "p2", name = "B", eliminated = true),
            Player(id = "p3", name = "C")
        )
        state.advanceTurn()
        // Should skip p2 (eliminated) and land on p3
        assertEquals(2, state.currentPlayerIndex)
        assertEquals("p3", state.currentPlayer?.id)
    }

    @Test
    fun `advanceTurn skips bankrupt player with no money and no properties`() {
        val p2 = Player(id = "p2", name = "B", money = 0)
        p2.ownedPropertyIds.clear()
        val state = makeState(
            Player(id = "p1", name = "A"),
            p2,
            Player(id = "p3", name = "C")
        )
        state.advanceTurn()
        assertEquals(2, state.currentPlayerIndex)
    }

    @Test
    fun `advanceTurn sets FINISHED when all players are bankrupt`() {
        val state = makeState(
            Player(id = "p1", name = "A", eliminated = true),
            Player(id = "p2", name = "B", eliminated = true)
        )
        state.advanceTurn()
        assertEquals(GamePhase.FINISHED, state.phase)
    }

    @Test
    fun `advanceTurn wraps around skipping bankrupt players`() {
        val state = makeState(
            Player(id = "p1", name = "A"),
            Player(id = "p2", name = "B", eliminated = true),
            Player(id = "p3", name = "C", eliminated = true)
        )
        state.currentPlayerIndex = 0
        state.advanceTurn()
        // p2 and p3 are bankrupt, wraps to p1
        assertEquals(0, state.currentPlayerIndex)
    }

    @Test
    fun `GameState bankruptcy fields have defaults`() {
        val state = GameState(gameId = "test", fields = emptyList())
        assertEquals(0, state.bankruptcyTotalAssets)
        assertEquals(0, state.bankruptcyTotalDebt)
        assertEquals(0, state.bankruptcyPropertiesCount)
        assertTrue(state.bankruptcyOwnedFieldIds.isEmpty())
        assertEquals("", state.bankruptcyPlayerId)
    }

    @Test
    fun `GameState with pending payment set`() {
        val payment = PendingPayment(
            amount = 150,
            source = PaymentSource.RENT,
            sourceFieldId = 3,
            creditorPlayerId = "p2"
        )
        val state = GameState(gameId = "g1", fields = emptyList(), pendingPayment = payment)
        assertNotNull(state.pendingPayment)
        assertEquals(150, state.pendingPayment!!.amount)
        assertEquals("p2", state.pendingPayment!!.creditorPlayerId)
    }
}

class PlayerEliminatedTest {

    @Test
    fun `isBankrupt returns true when eliminated is true regardless of money`() {
        val player = Player(id = "p1", name = "A", money = 1500, eliminated = true)
        player.ownedPropertyIds.add(1)
        assertTrue(player.isBankrupt())
    }

    @Test
    fun `isBankrupt returns false when not eliminated and has money`() {
        val player = Player(id = "p1", name = "A", money = 100, eliminated = false)
        assertFalse(player.isBankrupt())
    }

    @Test
    fun `eliminated defaults to false`() {
        val player = Player(id = "p1", name = "A")
        assertFalse(player.eliminated)
    }

    @Test
    fun `eliminated can be set to true`() {
        val player = Player(id = "p1", name = "A")
        player.eliminated = true
        assertTrue(player.eliminated)
        assertTrue(player.isBankrupt())
    }

    @Test
    fun `isBankrupt with eliminated false and zero money but has properties`() {
        val player = Player(id = "p1", name = "A", money = 0)
        player.ownedPropertyIds.add(5)
        assertFalse(player.isBankrupt())
    }

    @Test
    fun `iconId defaults to lindwurm`() {
        val player = Player(id = "p1", name = "A")
        assertEquals("lindwurm", player.iconId)
    }

    @Test
    fun `consecutiveDoublets defaults to zero`() {
        val player = Player(id = "p1", name = "A")
        assertEquals(0, player.consecutiveDoublets)
    }
}

