package at.aau.monopoly.klagenfurt

import at.aau.monopoly.klagenfurt.model.card.Card
import at.aau.monopoly.klagenfurt.model.card.CardDeserializer
import at.aau.monopoly.klagenfurt.model.card.ChanceCard
import at.aau.monopoly.klagenfurt.model.card.CommunityChestCard
import at.aau.monopoly.klagenfurt.model.enums.CardAction
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CardDeserializerTest {

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
        val module = SimpleModule()
        module.addDeserializer(Card::class.java, CardDeserializer())
        objectMapper.registerModule(module)
    }

    @Test
    fun `deserialize chance card with all fields`() {
        val json = """
            {
              "type": "CHANCE",
              "id": 1,
              "description": "Advance to Go",
              "action": "MOVE_TO",
              "amount": 200,
              "targetFieldId": 0,
              "moveSpaces": 3
            }
        """.trimIndent()

        val card = objectMapper.readValue(json, Card::class.java)

        assertTrue(card is ChanceCard)
        assertEquals(1, card.id)
        assertEquals("Advance to Go", card.description)
        assertEquals(CardAction.MOVE_TO, card.action)
        assertEquals(200, card.amount)
        assertEquals(0, card.targetFieldId)
        assertEquals(3, card.moveSpaces)
    }

    @Test
    fun `deserialize community chest card with all fields`() {
        val json = """
            {
              "type": "COMMUNITY_CHEST",
              "id": 2,
              "description": "Bank error",
              "action": "COLLECT_MONEY",
              "amount": 200,
              "targetFieldId": 5,
              "moveSpaces": 1
            }
        """.trimIndent()

        val card = objectMapper.readValue(json, Card::class.java)

        assertTrue(card is CommunityChestCard)
        assertEquals(2, card.id)
        assertEquals("Bank error", card.description)
        assertEquals(CardAction.COLLECT_MONEY, card.action)
        assertEquals(200, card.amount)
        assertEquals(5, card.targetFieldId)
        assertEquals(1, card.moveSpaces)
    }

    @Test
    fun `deserialize defaults to chance card when type is missing`() {
        val json = """
            {
              "id": 3,
              "description": "Default chance",
              "action": "PAY_MONEY",
              "amount": 50
            }
        """.trimIndent()

        val card = objectMapper.readValue(json, Card::class.java)

        assertTrue(card is ChanceCard)
        assertEquals(3, card.id)
        assertEquals("Default chance", card.description)
        assertEquals(CardAction.PAY_MONEY, card.action)
        assertEquals(50, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `deserialize uses default values when fields are missing`() {
        val json = "{}"

        val card = objectMapper.readValue(json, Card::class.java)

        assertTrue(card is ChanceCard)
        assertEquals(0, card.id)
        assertEquals("", card.description)
        assertEquals(CardAction.COLLECT_MONEY, card.action)
        assertEquals(0, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }

    @Test
    fun `deserialize invalid action defaults to collect money`() {
        val json = """
            {
              "type": "CHANCE",
              "id": 4,
              "description": "Invalid action card",
              "action": "INVALID_ACTION",
              "amount": 123
            }
        """.trimIndent()

        val card = objectMapper.readValue(json, Card::class.java)

        assertTrue(card is ChanceCard)
        assertEquals(CardAction.COLLECT_MONEY, card.action)
        assertEquals(123, card.amount)
    }

    @Test
    fun `deserialize unknown type defaults to chance card`() {
        val json = """
            {
              "type": "UNKNOWN",
              "id": 5,
              "description": "Unknown type",
              "action": "MOVE_FORWARD",
              "moveSpaces": 4
            }
        """.trimIndent()

        val card = objectMapper.readValue(json, Card::class.java)

        assertTrue(card is ChanceCard)
        assertEquals(5, card.id)
        assertEquals(CardAction.MOVE_FORWARD, card.action)
        assertEquals(4, card.moveSpaces)
    }

    @Test
    fun `deserialize community chest with null optional values`() {
        val json = """
            {
              "type": "COMMUNITY_CHEST",
              "id": 6,
              "description": "No optional values",
              "action": "GO_TO_JAIL"
            }
        """.trimIndent()

        val card = objectMapper.readValue(json, Card::class.java)

        assertTrue(card is CommunityChestCard)
        assertEquals(6, card.id)
        assertEquals(CardAction.GO_TO_JAIL, card.action)
        assertEquals(0, card.amount)
        assertNull(card.targetFieldId)
        assertEquals(0, card.moveSpaces)
    }
}