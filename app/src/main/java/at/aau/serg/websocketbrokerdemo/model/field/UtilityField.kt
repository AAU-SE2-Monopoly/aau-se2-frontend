package at.aau.serg.websocketbrokerdemo.model.field

import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import org.json.JSONObject

data class UtilityField(
    override val id: Int,
    override val name: String,
    override val type: FieldType = FieldType.UTILITY,
    val price: Int = 150,
    var ownerId: String? = null,
    var isMortgaged: Boolean = false
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): UtilityField {
            val id = json.getInt("id")
            val name = json.getString("name")
            val type = FieldType.valueOf(json.getString("type"))
            val price = json.optInt("price", 150)
            val ownerId = json.optString("ownerId", null).takeIf { it.isNotEmpty() }
            val isMortgaged = json.optBoolean("isMortgaged", false)
            return UtilityField(id, name, type, price, ownerId, isMortgaged)
        }
    }
}
