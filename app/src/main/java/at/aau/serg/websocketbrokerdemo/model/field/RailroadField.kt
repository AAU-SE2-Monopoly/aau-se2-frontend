package at.aau.serg.websocketbrokerdemo.model.field

import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import org.json.JSONObject

data class RailroadField(
    override val id: Int,
    override val name: String,
    override val type: FieldType = FieldType.RAILROAD,
    val price: Int = 200,
    var ownerId: String? = null,
    var isMortgaged: Boolean = false
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): RailroadField {
            val id = json.getInt("id")
            val name = json.getString("name")
            val type = FieldType.valueOf(json.getString("type"))
            val price = json.optInt("price", 200)
            val ownerId = json.optString("ownerId", null).takeIf { it.isNotEmpty() }
            val isMortgaged = json.optBoolean("isMortgaged", false)
            return RailroadField(id, name, type, price, ownerId, isMortgaged)
        }
    }
}
