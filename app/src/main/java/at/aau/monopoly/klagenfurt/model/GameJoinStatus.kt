package at.aau.monopoly.klagenfurt.model

/**
 * Represents the join eligibility of a game from the lobby perspective.
 *
 * Kept as a standalone model (not coupled to any Activity/ViewModel) so it can be
 * reused across different join flows (standard join, reconnect, etc.)
 */
enum class GameJoinStatus {
    /** Game is open and accepting new players. */
    OPEN,
    /** Game has reached maximum player capacity. */
    FULL,
    /** Game has already started (reconnect possible). */
    IN_PROGRESS,
    /** Game has finished and can no longer be joined. */
    FINISHED;

    companion object {
        /**
         * Determines the join status for a game based on its current phase,
         * player count, and whether the current player is already in the game.
         *
         * @param phase          Current game phase (e.g. "WAITING", "ROLLING", "FINISHED")
         * @param playerCount    Current number of players in the game
         * @param maxPlayers     Maximum allowed players
         * @param playerIds      IDs of players currently in the game
         * @param currentPlayerId The local player's ID
         * @return The computed [GameJoinStatus]
         */
        fun compute(
            phase: String,
            playerCount: Int,
            maxPlayers: Int,
            playerIds: List<String> = emptyList(),
            currentPlayerId: String = ""
        ): GameJoinStatus = when {
            phase == "FINISHED"               -> FINISHED
            phase != "WAITING"                -> IN_PROGRESS
            currentPlayerId in playerIds      -> OPEN
            playerCount >= maxPlayers         -> FULL
            else                              -> OPEN
        }

        /** Maps an icon picker index to a server-recognized icon identifier. */
        fun iconIdForIndex(iconIndex: Int): String = when (iconIndex) {
            0    -> "lindwurm"
            1    -> "woerthersee"
            2    -> "gti"
            3    -> "ironman"
            4    -> "josef"
            else -> "lindwurm"
        }
    }
}
