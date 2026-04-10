



package at.aau.monopoly.klagenfurt.model.field

import at.aau.monopoly.klagenfurt.model.enums.FieldType
import org.json.JSONObject
data class ChanceField(
    override val id: Int,
    override val name: String = "Chance",
    override val type: FieldType = FieldType.CHANCE
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): ChanceField {
            val id = json.getInt("id")
            val name = json.optString("name", "Chance")
            val type = FieldType.valueOf(json.getString("type"))
            return ChanceField(id, name, type)
        }
    }
}
