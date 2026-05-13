package at.aau.monopoly.klagenfurt.model.card

import at.aau.monopoly.klagenfurt.model.enums.CardAction
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Custom deserializer for Card that handles both polymorphic and concrete card types.
 * - If 'type' field exists: Uses it to determine the concrete class (ChanceCard vs CommunityChestCard)
 * - If 'type' field is missing: Defaults to ChanceCard
 */
class CardDeserializer : JsonDeserializer<Card>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Card {
        val codec = parser.codec
        val node: ObjectNode = codec.readTree(parser)

        val id = node.get("id")?.asInt() ?: 0
        val description = node.get("description")?.asText() ?: ""
        val actionStr = node.get("action")?.asText() ?: "COLLECT_MONEY"
        val action = try {
            CardAction.valueOf(actionStr)
        } catch (e: Exception) {
            CardAction.COLLECT_MONEY
        }
        val amount = node.get("amount")?.asInt() ?: 0
        val targetFieldId = node.get("targetFieldId")?.asInt()
        val moveSpaces = node.get("moveSpaces")?.asInt() ?: 0

        // Determine the type
        val typeStr = node.get("type")?.asText() ?: "CHANCE"

        return when (typeStr) {
            "COMMUNITY_CHEST" -> CommunityChestCard(
                id = id,
                description = description,
                action = action,
                amount = amount,
                targetFieldId = targetFieldId,
                moveSpaces = moveSpaces
            )
            else -> ChanceCard(
                id = id,
                description = description,
                action = action,
                amount = amount,
                targetFieldId = targetFieldId,
                moveSpaces = moveSpaces
            )
        }
    }
}
