package at.aau.monopoly.klagenfurt.networking

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ServerConfigTest {

    @After
    fun tearDown() {
        ServerConfig.isGlobal = false
    }

    @Test
    fun `default is local`() {
        assertFalse(ServerConfig.isGlobal)
    }

    @Test
    fun `displayLabel is Local by default`() {
        assertEquals("Local", ServerConfig.displayLabel)
    }

    @Test
    fun `displayLabel is Global when isGlobal is true`() {
        ServerConfig.isGlobal = true
        assertEquals("Global", ServerConfig.displayLabel)
    }

    @Test
    fun `websocketUri returns local URI by default`() {
        assertTrue(ServerConfig.websocketUri.contains("10.0.2.2"))
    }

    @Test
    fun `websocketUri returns global URI when isGlobal`() {
        ServerConfig.isGlobal = true
        assertTrue(ServerConfig.websocketUri.contains("se2-demo.aau.at"))
    }

    @Test
    fun `toggling isGlobal changes websocketUri`() {
        ServerConfig.isGlobal = false
        val localWs = ServerConfig.websocketUri

        ServerConfig.isGlobal = true
        assertNotEquals(localWs, ServerConfig.websocketUri)
    }
}


