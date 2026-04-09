package at.aau.serg.websocketbrokerdemo.model.field

import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import org.json.JSONObject

data class GoField(
    override val id: Int = 0,
    override val name: String = "Go",
    override val type: FieldType = FieldType.GO,
    val salary: Int = 200
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): GoField {
            val id = json.getInt("id")
            val name = json.getString("name")
            val type = FieldType.valueOf(json.getString("type"))
            val salary = json.optInt("salary", 200)
            return GoField(id, name, type, salary)
        }
    }
}
