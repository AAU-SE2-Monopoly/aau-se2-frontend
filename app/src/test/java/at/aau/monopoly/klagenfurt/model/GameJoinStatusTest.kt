package at.aau.monopoly.klagenfurt.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameJoinStatusTest {

    @Test
    fun `compute returns FINISHED for finished phase`() {
        assertEquals(
            GameJoinStatus.FINISHED,
            GameJoinStatus.compute(phase = "FINISHED", playerCount = 2, maxPlayers = 6)
        )
    }

    @Test
    fun `compute returns IN_PROGRESS for non-waiting phase`() {
        assertEquals(
            GameJoinStatus.IN_PROGRESS,
            GameJoinStatus.compute(phase = "ROLLING", playerCount = 2, maxPlayers = 6)
        )
        assertEquals(
            GameJoinStatus.IN_PROGRESS,
            GameJoinStatus.compute(phase = "BUYING", playerCount = 2, maxPlayers = 6)
        )
    }

    @Test
    fun `compute returns OPEN when player already in game even if full`() {
        assertEquals(
            GameJoinStatus.OPEN,
            GameJoinStatus.compute(
                phase = "WAITING", playerCount = 6, maxPlayers = 6,
                playerIds = listOf("p1", "p2"), currentPlayerId = "p1"
            )
        )
    }

    @Test
    fun `compute returns FULL when at capacity`() {
        assertEquals(
            GameJoinStatus.FULL,
            GameJoinStatus.compute(
                phase = "WAITING", playerCount = 6, maxPlayers = 6,
                playerIds = listOf("p1"), currentPlayerId = "p99"
            )
        )
    }

    @Test
    fun `compute returns OPEN for available game`() {
        assertEquals(
            GameJoinStatus.OPEN,
            GameJoinStatus.compute(phase = "WAITING", playerCount = 2, maxPlayers = 6)
        )
    }

    @Test
    fun `iconIdForIndex maps correctly`() {
        assertEquals("lindwurm", GameJoinStatus.iconIdForIndex(0))
        assertEquals("woerthersee", GameJoinStatus.iconIdForIndex(1))
        assertEquals("gti", GameJoinStatus.iconIdForIndex(2))
        assertEquals("ironman", GameJoinStatus.iconIdForIndex(3))
        assertEquals("josef", GameJoinStatus.iconIdForIndex(4))
        assertEquals("lindwurm", GameJoinStatus.iconIdForIndex(99))
        assertEquals("lindwurm", GameJoinStatus.iconIdForIndex(-1))
    }
}

