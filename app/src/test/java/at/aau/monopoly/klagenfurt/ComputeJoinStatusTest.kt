package at.aau.monopoly.klagenfurt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the companion object function [JoinActivity.computeJoinStatus],
 * covering all branches of the `when` expression.
 */
class ComputeJoinStatusTest {

    // --- FINISHED phase ------------------------------------------------------

    @Test
    fun `FINISHED returns FINISHED regardless of playerCount or playerIds`() {
        // Even with capacity and the player already in the list → FINISHED wins
        assertEquals(
            GameJoinStatus.FINISHED,
            JoinActivity.computeJoinStatus(
                phase = "FINISHED",
                playerCount = 1,
                maxPlayers = 4,
                playerIds = listOf("alice"),
                currentPlayerId = "alice"
            )
        )

        // Edge: empty player list, zero count
        assertEquals(
            GameJoinStatus.FINISHED,
            JoinActivity.computeJoinStatus(
                phase = "FINISHED",
                playerCount = 0,
                maxPlayers = 4,
                playerIds = emptyList(),
                currentPlayerId = "bob"
            )
        )
    }

    // --- IN_PROGRESS (non-WAITING, non-FINISHED) -----------------------------

    @Test
    fun `ROLLING returns IN_PROGRESS`() {
        assertEquals(
            GameJoinStatus.IN_PROGRESS,
            JoinActivity.computeJoinStatus(
                phase = "ROLLING",
                playerCount = 2,
                maxPlayers = 4,
                playerIds = listOf("alice", "bob"),
                currentPlayerId = "alice"
            )
        )
    }

    @Test
    fun `BUYING returns IN_PROGRESS`() {
        assertEquals(
            GameJoinStatus.IN_PROGRESS,
            JoinActivity.computeJoinStatus(
                phase = "BUYING",
                playerCount = 3,
                maxPlayers = 4,
                playerIds = listOf("alice"),
                currentPlayerId = "charlie"
            )
        )
    }

    @Test
    fun `AUCTIONING returns IN_PROGRESS`() {
        assertEquals(
            GameJoinStatus.IN_PROGRESS,
            JoinActivity.computeJoinStatus(
                phase = "AUCTIONING",
                playerCount = 4,
                maxPlayers = 4,
                playerIds = listOf("alice", "bob"),
                currentPlayerId = "alice"
            )
        )
    }

    @Test
    fun `TURN_END returns IN_PROGRESS`() {
        assertEquals(
            GameJoinStatus.IN_PROGRESS,
            JoinActivity.computeJoinStatus(
                phase = "TURN_END",
                playerCount = 2,
                maxPlayers = 2,
                playerIds = emptyList(),
                currentPlayerId = ""
            )
        )
    }

    @Test
    fun `arbitrary non-WAITING non-FINISHED phase returns IN_PROGRESS`() {
        assertEquals(
            GameJoinStatus.IN_PROGRESS,
            JoinActivity.computeJoinStatus(
                phase = "SOME_FUTURE_PHASE",
                playerCount = 1,
                maxPlayers = 4,
                playerIds = listOf("alice"),
                currentPlayerId = "alice"
            )
        )
    }

    // --- WAITING + currentPlayerId in playerIds → OPEN (returning player) -----

    @Test
    fun `WAITING with currentPlayerId in playerIds returns OPEN (returning player)`() {
        assertEquals(
            GameJoinStatus.OPEN,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 3,
                maxPlayers = 4,
                playerIds = listOf("alice", "bob", "charlie"),
                currentPlayerId = "bob"
            )
        )
    }

    // --- WAITING + full + currentPlayerId NOT in playerIds → FULL -------------

    @Test
    fun `WAITING full with currentPlayerId not in playerIds returns FULL`() {
        assertEquals(
            GameJoinStatus.FULL,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 4,
                maxPlayers = 4,
                playerIds = listOf("alice", "bob", "charlie", "dave"),
                currentPlayerId = "eve"
            )
        )
    }

    @Test
    fun `WAITING playerCount exceeds maxPlayers returns FULL`() {
        // Defensive case – playerCount > maxPlayers should still resolve to FULL
        assertEquals(
            GameJoinStatus.FULL,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 5,
                maxPlayers = 4,
                playerIds = listOf("alice", "bob", "charlie", "dave", "eve"),
                currentPlayerId = "frank"
            )
        )
    }

    // --- WAITING + not full + currentPlayerId NOT in playerIds → OPEN ---------

    @Test
    fun `WAITING not full with currentPlayerId not in playerIds returns OPEN`() {
        assertEquals(
            GameJoinStatus.OPEN,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 2,
                maxPlayers = 4,
                playerIds = listOf("alice", "bob"),
                currentPlayerId = "charlie"
            )
        )
    }

    @Test
    fun `WAITING not full with empty playerIds returns OPEN`() {
        assertEquals(
            GameJoinStatus.OPEN,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 0,
                maxPlayers = 4,
                playerIds = emptyList(),
                currentPlayerId = "newbie"
            )
        )
    }

    // --- WAITING + full but currentPlayerId IS in playerIds → OPEN ------------

    @Test
    fun `WAITING full but currentPlayerId in playerIds returns OPEN (not FULL)`() {
        // When the game is full but the requesting player is already part of the game,
        // they should be able to rejoin → OPEN, not FULL.
        assertEquals(
            GameJoinStatus.OPEN,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 4,
                maxPlayers = 4,
                playerIds = listOf("alice", "bob", "charlie", "dave"),
                currentPlayerId = "alice"
            )
        )
    }

    // --- Default parameter behaviour -----------------------------------------

    @Test
    fun `default empty playerIds and blank currentPlayerId with WAITING returns OPEN`() {
        // Tests the scenario where neither playerIds nor currentPlayerId are provided
        assertEquals(
            GameJoinStatus.OPEN,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 0,
                maxPlayers = 4
            )
        )
    }

    @Test
    fun `default empty playerIds with WAITING and full count returns FULL`() {
        assertEquals(
            GameJoinStatus.FULL,
            JoinActivity.computeJoinStatus(
                phase = "WAITING",
                playerCount = 4,
                maxPlayers = 4
            )
        )
    }
}