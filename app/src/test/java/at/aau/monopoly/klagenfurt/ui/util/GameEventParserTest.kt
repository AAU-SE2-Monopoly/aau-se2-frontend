package at.aau.monopoly.klagenfurt.ui.util

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameEventParserTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `parseGameEvent returns GameEvent for valid JSON`() {
        val json = """{"gameId":"g1","event":"GAME_CREATED","message":"hello"}"""
        val result = parseGameEvent(json)
        assertNotNull(result)
        assertEquals("g1", result!!.gameId)
        assertEquals("GAME_CREATED", result.event)
        assertEquals("hello", result.message)
        assertNull(result.gameState)
    }

    @Test
    fun `parseGameEvent returns GameEvent with minimal fields`() {
        val json = """{"gameId":"","event":""}"""
        val result = parseGameEvent(json)
        assertNotNull(result)
        assertEquals("", result!!.gameId)
        assertEquals("", result.event)
    }

    @Test
    fun `parseGameEvent returns null for invalid JSON`() {
        val result = parseGameEvent("not valid json at all")
        assertNull(result)
    }

    @Test
    fun `parseGameEvent returns null for empty string`() {
        val result = parseGameEvent("")
        assertNull(result)
    }

    @Test
    fun `parseGameEvent handles unknown fields gracefully`() {
        val json = """{"gameId":"g1","event":"TEST","unknownField":"value","extraNumber":42}"""
        val result = parseGameEvent(json)
        assertNotNull(result)
        assertEquals("g1", result!!.gameId)
        assertEquals("TEST", result.event)
    }

    @Test
    fun `parseGameEvent parses message as null when absent`() {
        val json = """{"gameId":"g1","event":"DICE_ROLLED"}"""
        val result = parseGameEvent(json)
        assertNotNull(result)
        assertNull(result!!.message)
    }
}

