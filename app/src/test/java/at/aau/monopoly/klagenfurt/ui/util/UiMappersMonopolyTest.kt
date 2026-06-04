package at.aau.monopoly.klagenfurt.ui.util

import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for toManageableProperty monopoly/building logic in UiMappers.
 */
class UiMappersMonopolyTest {

    private fun brownProperty(
        id: Int, name: String = "Brown$id", ownerId: String = "p1",
        houses: Int = 0, hasHotel: Boolean = false, isMortgaged: Boolean = false
    ) = PropertyField(
        id = id, name = name, color = PropertyColor.BROWN,
        price = 60, rent = listOf(2, 10, 30, 90, 160, 250),
        houseCost = 50, hotelCost = 50, ownerId = ownerId,
        houses = houses, hasHotel = hasHotel, isMortgaged = isMortgaged
    )

    @Test
    fun `canBuyHouse true when player owns full monopoly and even building`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0),
            brownProperty(2, ownerId = "p1", houses = 0)
        )
        val result = fields[0].toManageableProperty(fields)
        assertTrue(result.canBuyHouse)
    }

    @Test
    fun `canBuyHouse false when monopoly not complete`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0),
            brownProperty(2, ownerId = "p2", houses = 0) // different owner
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canBuyHouse)
    }

    @Test
    fun `canBuyHouse false when sibling has fewer houses (even building rule)`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 1),
            brownProperty(2, ownerId = "p1", houses = 0)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canBuyHouse)
    }

    @Test
    fun `canBuyHouse true when sibling has same houses`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 1),
            brownProperty(2, ownerId = "p1", houses = 1)
        )
        val result = fields[0].toManageableProperty(fields)
        assertTrue(result.canBuyHouse)
    }

    @Test
    fun `canBuyHouse false when property is mortgaged`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0, isMortgaged = true),
            brownProperty(2, ownerId = "p1", houses = 0)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canBuyHouse)
    }

    @Test
    fun `canBuyHouse false when sibling is mortgaged`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0),
            brownProperty(2, ownerId = "p1", houses = 0, isMortgaged = true)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canBuyHouse)
    }

    @Test
    fun `canBuyHotel true when all siblings have 4 houses`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 4),
            brownProperty(2, ownerId = "p1", houses = 4)
        )
        val result = fields[0].toManageableProperty(fields)
        assertTrue(result.canBuyHotel)
    }

    @Test
    fun `canBuyHotel false when sibling has fewer than 4 houses`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 4),
            brownProperty(2, ownerId = "p1", houses = 3)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canBuyHotel)
    }

    @Test
    fun `canBuyHotel true when sibling already has hotel`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 4),
            brownProperty(2, ownerId = "p1", houses = 0, hasHotel = true)
        )
        val result = fields[0].toManageableProperty(fields)
        assertTrue(result.canBuyHotel)
    }

    @Test
    fun `canSellHouse true when even building allows`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 2),
            brownProperty(2, ownerId = "p1", houses = 2)
        )
        val result = fields[0].toManageableProperty(fields)
        assertTrue(result.canSellHouse)
    }

    @Test
    fun `canSellHouse false when selling would violate even building (sibling has fewer)`() {
        // field1 has 2 houses, field2 has 0.  Selling from field1 → newCount=1
        // siblings need houses in (1..2), but field2 has 0 → canSellHouse = false
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 2),
            brownProperty(2, ownerId = "p1", houses = 0)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canSellHouse)
    }

    @Test
    fun `canSellHotel true when siblings have 4 houses or hotel`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0, hasHotel = true),
            brownProperty(2, ownerId = "p1", houses = 4)
        )
        val result = fields[0].toManageableProperty(fields)
        assertTrue(result.canSellHotel)
    }

    @Test
    fun `canSellHotel false when sibling has fewer than 4 houses and no hotel`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0, hasHotel = true),
            brownProperty(2, ownerId = "p1", houses = 3)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canSellHotel)
    }

    @Test
    fun `canMortgage true when no buildings on property or siblings`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0),
            brownProperty(2, ownerId = "p1", houses = 0)
        )
        val result = fields[0].toManageableProperty(fields)
        assertTrue(result.canMortgage)
    }

    @Test
    fun `canMortgage false when sibling has buildings`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 0),
            brownProperty(2, ownerId = "p1", houses = 1)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canMortgage)
    }

    @Test
    fun `canMortgage false when property has buildings`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", houses = 1),
            brownProperty(2, ownerId = "p1", houses = 1)
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canMortgage)
    }

    @Test
    fun `canMortgage false when property already mortgaged`() {
        val fields = listOf(
            brownProperty(1, ownerId = "p1", isMortgaged = true),
            brownProperty(2, ownerId = "p1")
        )
        val result = fields[0].toManageableProperty(fields)
        assertFalse(result.canMortgage)
    }

    @Test
    fun `toManageableProperty no-arg overload uses empty sibling list`() {
        val field = brownProperty(1, ownerId = "p1")
        val result = field.toManageableProperty()
        assertEquals(1, result.fieldId)
        // Without siblings, monopoly check finds no others with same color -> still canBuyHouse since all checks pass trivially
        assertTrue(result.canBuyHouse)
    }

    @Test
    fun `UtilityField toManageableProperty has canMortgage true when not mortgaged`() {
        val field = UtilityField(id = 12, name = "Water Works", price = 150, ownerId = "p1", isMortgaged = false)
        val result = field.toManageableProperty(emptyList())
        assertTrue(result.canMortgage)
        assertFalse(result.canBuyHouse)
        assertFalse(result.canBuyHotel)
    }

    @Test
    fun `UtilityField toManageableProperty has canMortgage false when mortgaged`() {
        val field = UtilityField(id = 12, name = "Water Works", price = 150, ownerId = "p1", isMortgaged = true)
        val result = field.toManageableProperty(emptyList())
        assertFalse(result.canMortgage)
        assertTrue(result.isMortgaged)
    }

    @Test
    fun `RailroadField toManageableProperty canMortgage true`() {
        val field = RailroadField(id = 5, name = "Station", price = 200, ownerId = "p1", isMortgaged = false)
        val result = field.toManageableProperty(emptyList())
        assertTrue(result.canMortgage)
    }

    @Test
    fun `RailroadField toManageableProperty canMortgage false when mortgaged`() {
        val field = RailroadField(id = 5, name = "Station", price = 200, ownerId = "p1", isMortgaged = true)
        val result = field.toManageableProperty(emptyList())
        assertFalse(result.canMortgage)
    }

    @Test
    fun `toPropertyComposeColor maps all PropertyColor values`() {
        val fallback = androidx.compose.ui.graphics.Color(0xFF424242)
        for (color in PropertyColor.entries) {
            val result = color.name.toPropertyComposeColor()
            assertNotEquals(fallback, result)
        }
    }

    @Test
    fun `toPropertyComposeColor case insensitive`() {
        assertEquals("brown".toPropertyComposeColor(), "BROWN".toPropertyComposeColor())
        assertEquals("LIGHT_BLUE".toPropertyComposeColor(), "light_blue".toPropertyComposeColor())
    }
}



