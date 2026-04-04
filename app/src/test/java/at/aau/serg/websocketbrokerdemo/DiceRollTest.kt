package at.aau.serg.websocketbrokerdemo.at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketdemoserver.model.DiceRoll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

}