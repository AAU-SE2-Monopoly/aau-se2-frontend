package at.aau.monopoly.klagenfurt.ui

internal fun moveToTargetLabel(targetFieldId: Int?, includeFieldPrefix: Boolean): String =
    when (targetFieldId) {
        -1 -> "Nearest railroad"
        -2 -> "Nearest utility"
        null -> if (includeFieldPrefix) "Field #?" else "#?"
        else -> if (includeFieldPrefix) "Field #$targetFieldId" else "#$targetFieldId"
    }
