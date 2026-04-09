package at.aau.serg.websocketbrokerdemo.model.field

import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import org.json.JSONObject

data class FreeParkingField(
    override val id: Int = 20,
    override val name: String = "Free Parking",
    override val type: FieldType = FieldType.FREE_PARKING
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): FreeParkingField {
            val id = json.optInt("id", 20)
            val name = json.optString("name", "Free Parking")
            val type = FieldType.valueOf(json.getString("type"))
            return FreeParkingField(id, name, type)
        }
    }
}
