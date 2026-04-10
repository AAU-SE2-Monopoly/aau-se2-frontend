package at.aau.monopoly.klagenfurt.model
import org.json.JSONArray
import org.json.JSONObject
data class Player(
    val id: String,
    var name: String,
    var position: Int = 0,
    var money: Int = 1500,
    var inJail: Boolean = false,
    var jailTurns: Int = 0,
    var getOutOfJailCards: Int = 0,
    val ownedPropertyIds: MutableList<Int> = mutableListOf()
) {
    /** Returns true if the player is bankrupt (no money and no properties). */
    fun isBankrupt(): Boolean = money <= 0 && ownedPropertyIds.isEmpty()

    companion object {
        fun fromJson(json: JSONObject): Player {
            val id = json.getString("id")
            val name = json.getString("name")
            val position = json.optInt("position", 0)
            val money = json.optInt("money", 1500)
            val inJail = json.optBoolean("inJail", false)
            val jailTurns = json.optInt("jailTurns", 0)
            val getOutOfJailCards = json.optInt("getOutOfJailCards", 0)
            val ownedPropertyIds = mutableListOf<Int>()
            val propertiesArray = json.optJSONArray("ownedPropertyIds") ?: JSONArray()
            for (i in 0 until propertiesArray.length()) {
                ownedPropertyIds.add(propertiesArray.getInt(i))
            }

            return Player(
                id,
                name,
                position,
                money,
                inJail,
                jailTurns,
                getOutOfJailCards,
                ownedPropertyIds
            )
        }
    }
}
