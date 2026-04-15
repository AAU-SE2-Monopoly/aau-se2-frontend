package at.aau.monopoly.klagenfurt.networking

import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.field.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Provides a pre-configured Jackson [ObjectMapper] that handles the polymorphic
 * [Field] hierarchy using the `type` discriminator property.
 */
object JacksonProvider {

    val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        registerModule(
            SimpleModule().addDeserializer(Field::class.java, FieldDeserializer())
        )
    }

    /**
     * Custom deserializer that inspects the JSON `type` field and delegates
     * to the correct concrete [Field] subclass.
     */
    private class FieldDeserializer : JsonDeserializer<Field>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Field {
            val node: JsonNode = p.codec.readTree(p)
            val typeString = node.get("type")?.asText()
                ?: throw IllegalArgumentException("Field JSON missing 'type' property")
            val fieldType = FieldType.valueOf(typeString)
            val targetClass: Class<out Field> = when (fieldType) {
                FieldType.GO -> GoField::class.java
                FieldType.PROPERTY -> PropertyField::class.java
                FieldType.COMMUNITY_CHEST -> CommunityChestField::class.java
                FieldType.TAX -> TaxField::class.java
                FieldType.RAILROAD -> RailroadField::class.java
                FieldType.CHANCE -> ChanceField::class.java
                FieldType.JAIL -> JailField::class.java
                FieldType.UTILITY -> UtilityField::class.java
                FieldType.FREE_PARKING -> FreeParkingField::class.java
                FieldType.GO_TO_JAIL -> GoToJailField::class.java
            }
            // Use a fresh codec-based read to avoid infinite recursion
            val treeParser = node.traverse(p.codec)
            treeParser.nextToken()
            return ctxt.readValue(treeParser, targetClass)
        }
    }
}
