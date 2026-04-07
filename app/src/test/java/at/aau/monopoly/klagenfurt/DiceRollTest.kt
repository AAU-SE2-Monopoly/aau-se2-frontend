package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.model.DiceRoll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiceRollTest {

    @Test
    fun `total should return sum of both dice`() {
        val roll = DiceRoll(3, 4)
        assertEquals(7, roll.total)
    }

    @Test
    fun `isDouble should return true when both dice are equal`() {
        val roll = DiceRoll(5, 5)
        assertTrue(roll.isDouble)
    }

    @Test
    fun `isDouble should return false when dice are different`() {
        val roll = DiceRoll(2, 3)
        assertFalse(roll.isDouble)
    }

    @Test
    fun `total should work for edge values`() {
        val roll = DiceRoll(1, 6)
        assertEquals(7, roll.total)
    }

    @Test
    fun `test copy method`() {
        val roll = DiceRoll(1, 2)
        val copy = roll.copy(die2 = 3)
        assertEquals(1, copy.die1)
        assertEquals(3, copy.die2)
    }

    @Test
    fun `toString should not be null`() {
        val roll = DiceRoll(1, 1)
        assertNotNull(roll.toString())
    }
}
