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

fun computeBackwardMovementPath(fromPos: Int, total: Int, boardSize: Int = 40): List<Int> =
    (1..total).map { (fromPos - it).floorMod(boardSize) }

fun computeDirectMovementPath(fromPos: Int, toPos: Int, boardSize: Int = 40): List<Int> {
    val normalizedFrom = fromPos.floorMod(boardSize)
    val normalizedTo = toPos.floorMod(boardSize)
    if (normalizedFrom == normalizedTo) return emptyList()

    val forwardSteps = (normalizedTo - normalizedFrom).floorMod(boardSize)
    return computeMovementPath(normalizedFrom, forwardSteps, boardSize)
}

private fun Int.floorMod(modulus: Int): Int =
    ((this % modulus) + modulus) % modulus
