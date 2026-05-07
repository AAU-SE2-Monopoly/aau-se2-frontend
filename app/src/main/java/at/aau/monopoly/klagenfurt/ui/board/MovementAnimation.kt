package at.aau.monopoly.klagenfurt.ui.board

data class MovementAnimationState(
    val playerId: String,
    val startPosition: Int,
    val path: List<Int>,
    val currentStepIndex: Int,
    val isComplete: Boolean
)


fun computeMovementPath(fromPos: Int, total: Int, boardSize: Int = 40): List<Int> =
    (1..total).map { (fromPos + it) % boardSize }