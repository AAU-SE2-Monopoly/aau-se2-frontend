package at.aau.monopoly.klagenfurt.ui

import org.junit.Test
import org.junit.Assert.*

class CardScaleTest {

    @Test
    fun `dp scales linearly with ratio`() {
        val scale = CardScale(0.5f)
        assertEquals(5f, scale.dp(10f).value, 0.01f)
    }

    @Test
    fun `dp at ratio 1 returns same value`() {
        val scale = CardScale(1f)
        assertEquals(14f, scale.dp(14f).value, 0.01f)
    }

    @Test
    fun `dp at ratio 2 doubles value`() {
        val scale = CardScale(2f)
        assertEquals(20f, scale.dp(10f).value, 0.01f)
    }

    @Test
    fun `sp uses sqrt scaling`() {
        val scale = CardScale(1f)
        // sqrt(1) = 1, so sp(10) = 10
        assertEquals(10f, scale.sp(10f).value, 0.01f)
    }

    @Test
    fun `sp at ratio 4 uses sqrt 2`() {
        val scale = CardScale(4f)
        // sqrt(4) = 2, so sp(10) = 20
        assertEquals(20f, scale.sp(10f).value, 0.01f)
    }

    @Test
    fun `sp coerces minimum ratio to 0_65`() {
        val scale = CardScale(0.1f)
        // sqrt(0.1) ≈ 0.316, coerced to 0.65
        assertEquals(6.5f, scale.sp(10f).value, 0.01f)
    }

    @Test
    fun `sp at ratio 0_5 uses sqrt`() {
        val scale = CardScale(0.5f)
        // sqrt(0.5) ≈ 0.707
        val expected = 10f * kotlin.math.sqrt(0.5f)
        assertEquals(expected, scale.sp(10f).value, 0.01f)
    }

    @Test
    fun `dp with zero value returns zero`() {
        val scale = CardScale(2f)
        assertEquals(0f, scale.dp(0f).value, 0.01f)
    }

    @Test
    fun `sp with zero value returns zero`() {
        val scale = CardScale(2f)
        assertEquals(0f, scale.sp(0f).value, 0.01f)
    }
}

