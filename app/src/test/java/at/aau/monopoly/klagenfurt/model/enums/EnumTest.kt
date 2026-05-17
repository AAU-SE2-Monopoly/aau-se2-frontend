package at.aau.monopoly.klagenfurt.model.enums

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnumTest {

    @Test
    fun `CardAction has all expected values`() {
        val values = CardAction.entries
        assertEquals(8, values.size)
        assertNotNull(CardAction.valueOf("COLLECT_MONEY"))
        assertNotNull(CardAction.valueOf("PAY_MONEY"))
        assertNotNull(CardAction.valueOf("MOVE_TO"))
        assertNotNull(CardAction.valueOf("MOVE_FORWARD"))
        assertNotNull(CardAction.valueOf("GO_TO_JAIL"))
        assertNotNull(CardAction.valueOf("GET_OUT_OF_JAIL"))
        assertNotNull(CardAction.valueOf("PAY_EACH_PLAYER"))
        assertNotNull(CardAction.valueOf("COLLECT_FROM_EACH"))
    }

    @Test
    fun `FieldType has all expected values`() {
        val values = FieldType.entries
        assertEquals(10, values.size)
        assertNotNull(FieldType.valueOf("GO"))
        assertNotNull(FieldType.valueOf("PROPERTY"))
        assertNotNull(FieldType.valueOf("COMMUNITY_CHEST"))
        assertNotNull(FieldType.valueOf("TAX"))
        assertNotNull(FieldType.valueOf("RAILROAD"))
        assertNotNull(FieldType.valueOf("CHANCE"))
        assertNotNull(FieldType.valueOf("JAIL"))
        assertNotNull(FieldType.valueOf("UTILITY"))
        assertNotNull(FieldType.valueOf("FREE_PARKING"))
        assertNotNull(FieldType.valueOf("GO_TO_JAIL"))
    }

    @Test
    fun `GamePhase has all expected values`() {
        val values = GamePhase.entries
        assertEquals(6, values.size)
        assertNotNull(GamePhase.valueOf("WAITING"))
        assertNotNull(GamePhase.valueOf("ROLLING"))
        assertNotNull(GamePhase.valueOf("BUYING"))
        assertNotNull(GamePhase.valueOf("AUCTIONING"))
        assertNotNull(GamePhase.valueOf("TURN_END"))
        assertNotNull(GamePhase.valueOf("FINISHED"))
    }

    @Test
    fun `PropertyColor has all expected values`() {
        val values = PropertyColor.entries
        assertEquals(8, values.size)
        assertNotNull(PropertyColor.valueOf("BROWN"))
        assertNotNull(PropertyColor.valueOf("LIGHT_BLUE"))
        assertNotNull(PropertyColor.valueOf("PINK"))
        assertNotNull(PropertyColor.valueOf("ORANGE"))
        assertNotNull(PropertyColor.valueOf("RED"))
        assertNotNull(PropertyColor.valueOf("YELLOW"))
        assertNotNull(PropertyColor.valueOf("GREEN"))
        assertNotNull(PropertyColor.valueOf("DARK_BLUE"))
    }

    @Test
    fun `invalid enum valueOf throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            CardAction.valueOf("NONEXISTENT")
        }
    }
}





