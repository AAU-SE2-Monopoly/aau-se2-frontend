package at.aau.monopoly.klagenfurt.model.field

import at.aau.monopoly.klagenfurt.model.enums.FieldType
import org.json.JSONObject

abstract class Field(
    open val id: Int,
    open val name: String,
    open val type: FieldType
){
    companion object {
        fun fromJson(json: JSONObject): Field {
            val type = FieldType.valueOf(json.getString("type"))
            return when (type) {
                FieldType.GO -> GoField.fromJson(json)
                FieldType.PROPERTY -> PropertyField.fromJson(json)
                FieldType.COMMUNITY_CHEST -> CommunityChestField.fromJson(json)
                FieldType.TAX -> TaxField.fromJson(json)
                FieldType.RAILROAD -> RailroadField.fromJson(json)
                FieldType.CHANCE -> ChanceField.fromJson(json)
                FieldType.JAIL -> JailField.fromJson(json)
                FieldType.UTILITY -> UtilityField.fromJson(json)
                FieldType.FREE_PARKING -> FreeParkingField.fromJson(json)
                FieldType.GO_TO_JAIL -> GoToJailField.fromJson(json)
            }
        }
    }
}
