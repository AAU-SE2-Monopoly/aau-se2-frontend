


package at.aau.monopoly.klagenfurt.model.field
import org.json.JSONObject
import at.aau.monopoly.klagenfurt.model.enums.FieldType

data class CommunityChestField(
    override val id: Int,
    override val name: String = "Community Chest",
    override val type: FieldType = FieldType.COMMUNITY_CHEST
) : Field(id, name, type) {

    companion object {
        fun fromJson(json: JSONObject): CommunityChestField {
            val id = json.getInt("id")
            val name = json.optString("name", "Community Chest")
            val type = FieldType.valueOf(json.getString("type"))
            return CommunityChestField(id, name, type)
        }
    }
}
