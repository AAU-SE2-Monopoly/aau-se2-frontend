package at.aau.monopoly.klagenfurt.model
import org.json.JSONObject
/** Represents the result of a single dice roll (two six-sided dice). */
data class DiceRoll(
    val die1: Int,
    val die2: Int
) {
    val total: Int get() = die1 + die2
    val isDouble: Boolean get() = die1 == die2

    companion object {
        fun fromJson(json: JSONObject): DiceRoll {
            val die1 = json.getInt("die1")
            val die2 = json.getInt("die2")
            return DiceRoll(die1, die2)
        }
    }
}
