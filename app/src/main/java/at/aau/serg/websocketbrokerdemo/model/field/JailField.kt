package at.aau.serg.websocketbrokerdemo.model.field

import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import org.json.JSONObject

data class JailField(
    override val id: Int = 10,
    override val name: String = "Jail / Just Visiting",
    override val type: FieldType = FieldType.JAIL
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): JailField {
            val id = json.optInt("id", 10)
            val name = json.optString("name", "Jail / Just Visiting")
            val type = FieldType.valueOf(json.getString("type"))
            return JailField(id, name, type)
        }
    }
}
