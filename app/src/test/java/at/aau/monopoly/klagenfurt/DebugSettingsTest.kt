package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.networking.ServerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DebugSettingsTest {

    @BeforeEach
    fun setup() {
        DebugSettings.isEnabled = false
        ServerConfig.isGlobal = false
    }

    @AfterEach
    fun teardown() {
        DebugSettings.isEnabled = false
        ServerConfig.isGlobal = false
    }

    @Test
    fun `isEnabled is false by default`() {
        assertFalse(DebugSettings.isEnabled)
    }

    @Test
    fun `isEnabled can be set to true`() {
        DebugSettings.isEnabled = true
        assertTrue(DebugSettings.isEnabled)
    }

    @Test
    fun `isEnabled can be toggled back to false`() {
        DebugSettings.isEnabled = true
        DebugSettings.isEnabled = false
        assertFalse(DebugSettings.isEnabled)
    }

    @Test
    fun `canEnable returns true when server is local`() {
        ServerConfig.isGlobal = false
        assertTrue(DebugSettings.canEnable)
    }

    @Test
    fun `canEnable returns false when server is global`() {
        ServerConfig.isGlobal = true
        assertFalse(DebugSettings.canEnable)
    }

    @Test
    fun `canEnable reflects server config changes`() {
        ServerConfig.isGlobal = false
        assertTrue(DebugSettings.canEnable)

        ServerConfig.isGlobal = true
        assertFalse(DebugSettings.canEnable)

        ServerConfig.isGlobal = false
        assertTrue(DebugSettings.canEnable)
    }
}

