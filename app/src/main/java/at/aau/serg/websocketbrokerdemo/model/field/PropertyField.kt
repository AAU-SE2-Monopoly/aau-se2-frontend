package at.aau.serg.websocketbrokerdemo.model.field

import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import at.aau.serg.websocketbrokerdemo.model.enums.PropertyColor
import org.json.JSONArray
import org.json.JSONObject

data class PropertyField(
    override val id: Int,
    override val name: String,
    override val type: FieldType = FieldType.PROPERTY,
    val color: PropertyColor,
    val price: Int,
    /** rent[0] = no houses, rent[1] = 1 house, …, rent[5] = hotel */
    val rent: List<Int>,
    val houseCost: Int,
    val hotelCost: Int,
    var ownerId: String? = null,
    var houses: Int = 0,
    var hasHotel: Boolean = false,
    var isMortgaged: Boolean = false
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): PropertyField {
            val id = json.getInt("id")
            val name = json.getString("name")
            val type = FieldType.valueOf(json.getString("type"))
            val color = PropertyColor.valueOf(json.getString("color"))
            val price = json.getInt("price")
            val rentArray = json.getJSONArray("rent")
            val rent = mutableListOf<Int>()
            for (i in 0 until rentArray.length()) {
                rent.add(rentArray.getInt(i))
            }
            val houseCost = json.getInt("houseCost")
            val hotelCost = json.getInt("hotelCost")
            val ownerId = json.optString("ownerId", null).takeIf { it.isNotEmpty() }
            val houses = json.optInt("houses", 0)
            val hasHotel = json.optBoolean("hasHotel", false)
            val isMortgaged = json.optBoolean("isMortgaged", false)
            return PropertyField(id, name, type, color, price, rent, houseCost, hotelCost, ownerId, houses, hasHotel, isMortgaged)
        }
    }
}
