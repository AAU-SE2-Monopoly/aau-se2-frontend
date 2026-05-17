package at.aau.monopoly.klagenfurt.networking

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class SessionPreferencesTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockContext: Context
    private val storedValues = mutableMapOf<String, String?>()

    private fun getPrefsField(): Field {
        val field = SessionPreferences::class.java.getDeclaredField("prefs")
        field.isAccessible = true
        return field
    }

    @BeforeEach
    fun setup() {
        // Reset singleton state
        getPrefsField().set(SessionPreferences, null)

        storedValues.clear()
        mockEditor = mockk(relaxed = true)
        every { mockEditor.putString(any(), any()) } answers {
            storedValues[firstArg()] = secondArg()
            mockEditor
        }
        every { mockEditor.remove(any()) } answers {
            storedValues.remove(firstArg<String>())
            mockEditor
        }

        mockPrefs = mockk(relaxed = true)
        every { mockPrefs.edit() } returns mockEditor
        every { mockPrefs.getString(any(), any()) } answers {
            storedValues[firstArg()] ?: secondArg()
        }

        mockContext = mockk(relaxed = true)
        every { mockContext.applicationContext } returns mockContext
        every {
            mockContext.getSharedPreferences("monopoly_session", Context.MODE_PRIVATE)
        } returns mockPrefs
    }

    @AfterEach
    fun tearDown() {
        // Reset singleton for other tests
        getPrefsField().set(SessionPreferences, null)
    }

    @Test
    fun `init sets prefs from context`() {
        SessionPreferences.init(mockContext)
        verify { mockContext.getSharedPreferences("monopoly_session", Context.MODE_PRIVATE) }
    }

    @Test
    fun `init does not re-initialize if already initialized`() {
        SessionPreferences.init(mockContext)
        SessionPreferences.init(mockContext)
        // Only one call expected since prefs is already set
        verify(exactly = 1) { mockContext.getSharedPreferences(any(), any()) }
    }

    @Test
    fun `getOrCreatePlayerId returns random UUID when prefs not initialized`() {
        val id = SessionPreferences.getOrCreatePlayerId()
        assertNotNull(id)
        assertTrue(id.isNotBlank())
    }

    @Test
    fun `getOrCreatePlayerId returns existing ID when persisted`() {
        storedValues["player_id"] = "existing-id-123"
        SessionPreferences.init(mockContext)

        val id = SessionPreferences.getOrCreatePlayerId()
        assertEquals("existing-id-123", id)
    }

    @Test
    fun `getOrCreatePlayerId generates and persists new ID when none exists`() {
        SessionPreferences.init(mockContext)

        val id = SessionPreferences.getOrCreatePlayerId()
        assertNotNull(id)
        assertTrue(id.isNotBlank())
        // Verify it was persisted
        assertEquals(id, storedValues["player_id"])
        verify { mockEditor.putString("player_id", id) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getOrCreatePlayerId returns same ID on subsequent calls`() {
        SessionPreferences.init(mockContext)

        val id1 = SessionPreferences.getOrCreatePlayerId()
        // Now it's stored
        val id2 = SessionPreferences.getOrCreatePlayerId()
        assertEquals(id1, id2)
    }

    @Test
    fun `playerName getter returns empty when prefs not initialized`() {
        assertEquals("", SessionPreferences.playerName)
    }

    @Test
    fun `playerName getter returns persisted value`() {
        storedValues["player_name"] = "Alice"
        SessionPreferences.init(mockContext)
        assertEquals("Alice", SessionPreferences.playerName)
    }

    @Test
    fun `playerName setter persists value`() {
        SessionPreferences.init(mockContext)
        SessionPreferences.playerName = "Bob"
        assertEquals("Bob", storedValues["player_name"])
        verify { mockEditor.putString("player_name", "Bob") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `gameId getter returns empty when prefs not initialized`() {
        assertEquals("", SessionPreferences.gameId)
    }

    @Test
    fun `gameId getter returns persisted value`() {
        storedValues["game_id"] = "game-42"
        SessionPreferences.init(mockContext)
        assertEquals("game-42", SessionPreferences.gameId)
    }

    @Test
    fun `gameId setter persists value`() {
        SessionPreferences.init(mockContext)
        SessionPreferences.gameId = "game-99"
        assertEquals("game-99", storedValues["game_id"])
        verify { mockEditor.putString("game_id", "game-99") }
    }

    @Test
    fun `iconId getter returns default when prefs not initialized`() {
        assertEquals("lindwurm", SessionPreferences.iconId)
    }

    @Test
    fun `iconId getter returns persisted value`() {
        storedValues["icon_id"] = "dragon"
        SessionPreferences.init(mockContext)
        assertEquals("dragon", SessionPreferences.iconId)
    }

    @Test
    fun `iconId setter persists value`() {
        SessionPreferences.init(mockContext)
        SessionPreferences.iconId = "knight"
        assertEquals("knight", storedValues["icon_id"])
        verify { mockEditor.putString("icon_id", "knight") }
    }

    @Test
    fun `clearGameSession removes gameId playerName and iconId`() {
        storedValues["game_id"] = "game-1"
        storedValues["player_name"] = "Alice"
        storedValues["icon_id"] = "dragon"
        storedValues["player_id"] = "persistent-id"

        SessionPreferences.init(mockContext)
        SessionPreferences.clearGameSession()

        verify { mockEditor.remove("game_id") }
        verify { mockEditor.remove("player_name") }
        verify { mockEditor.remove("icon_id") }
        verify { mockEditor.apply() }
        // player_id should NOT be removed
        assertTrue("persistent-id" == storedValues["player_id"])
    }

    @Test
    fun `clearGameSession does not crash when prefs not initialized`() {
        // Should not throw
        SessionPreferences.clearGameSession()
    }

    @Test
    fun `playerName setter does nothing when prefs not initialized`() {
        SessionPreferences.playerName = "test"
        // No crash, no verify needed
    }

    @Test
    fun `gameId setter does nothing when prefs not initialized`() {
        SessionPreferences.gameId = "test"
    }

    @Test
    fun `iconId setter does nothing when prefs not initialized`() {
        SessionPreferences.iconId = "test"
    }

    @Test
    fun `getOrCreatePlayerId generates different IDs for blank stored value`() {
        // Blank string stored should trigger new ID generation
        storedValues["player_id"] = ""
        SessionPreferences.init(mockContext)

        val id = SessionPreferences.getOrCreatePlayerId()
        assertTrue(id.isNotBlank())
        assertNotEquals("", id)
    }
}

