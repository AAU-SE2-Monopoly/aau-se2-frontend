package at.aau.serg.websocketbrokerdemo.model.field

import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import org.json.JSONObject

data class GoToJailField(
    override val id: Int = 30,
    override val name: String = "Go To Jail",
    override val type: FieldType = FieldType.GO_TO_JAIL
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): GoToJailField {
            val id = json.optInt("id", 30)
            val name = json.optString("name", "Go To Jail")
            val type = FieldType.valueOf(json.getString("type"))
            return GoToJailField(id, name, type)
        }
    }
}
