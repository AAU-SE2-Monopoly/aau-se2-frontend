package at.aau.monopoly.klagenfurt.model.field

import at.aau.monopoly.klagenfurt.model.enums.FieldType
import org.json.JSONObject
data class TaxField(
    override val id: Int,
    override val name: String,
    override val type: FieldType = FieldType.TAX,
    val amount: Int
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): TaxField {
            val id = json.getInt("id")
            val name = json.getString("name")
            val type = FieldType.valueOf(json.getString("type"))
            val amount = json.getInt("amount")
            return TaxField(id, name, type, amount)
        }
    }
}
