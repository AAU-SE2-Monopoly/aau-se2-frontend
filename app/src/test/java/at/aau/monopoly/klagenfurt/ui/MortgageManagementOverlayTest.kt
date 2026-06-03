package at.aau.monopoly.klagenfurt.ui

import org.junit.Assert.*
import org.junit.Test

class MortgageManagementOverlayTest {

    private fun prop(
        id: Int, name: String = "Prop$id", color: String? = "brown",
        isMortgaged: Boolean = false, houses: Int = 0, hasHotel: Boolean = false,
        canBuyHouse: Boolean = false, canBuyHotel: Boolean = false,
        canSellHouse: Boolean = false, canSellHotel: Boolean = false,
        canMortgage: Boolean = false
    ) = ManageableProperty(
        fieldId = id, name = name, color = color,
        price = 100, mortgageValue = 50, unmortgageCost = 55,
        houses = houses, hasHotel = hasHotel, isMortgaged = isMortgaged,
        houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25,
        canBuyHouse = canBuyHouse, canBuyHotel = canBuyHotel,
        canSellHouse = canSellHouse, canSellHotel = canSellHotel,
        canMortgage = canMortgage
    )

    @Test
    fun `findNextInGroup advances to next sibling when multiple in same color match`() {
        val list = listOf(
            prop(1, color = "brown", canBuyHouse = true),
            prop(2, color = "brown", canBuyHouse = true),
            prop(3, color = "brown", canBuyHouse = false)
        )
        val result = findNextInGroup(list, list[0]) { it.canBuyHouse }
        assertEquals(2, result.fieldId)
    }

    @Test
    fun `findNextInGroup stays on current when it is the only sibling match`() {
        val list = listOf(
            prop(1, color = "brown", canBuyHouse = true),
            prop(2, color = "brown", canBuyHouse = false),
            prop(3, color = "brown", canBuyHouse = false)
        )
        val result = findNextInGroup(list, list[0]) { it.canBuyHouse }
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `findNextInGroup wraps around to first sibling when current is last in group`() {
        val list = listOf(
            prop(1, color = "brown", canBuyHouse = true),
            prop(2, color = "brown", canBuyHouse = false),
            prop(3, color = "brown", canBuyHouse = true)
        )
        val result = findNextInGroup(list, list[2]) { it.canBuyHouse }
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `findNextInGroup does NOT cross color groups`() {
        val list = listOf(
            prop(1, color = "brown", canBuyHouse = true),
            prop(2, color = "brown", canBuyHouse = false),
            prop(3, color = "green", canBuyHouse = true)
        )
        val result = findNextInGroup(list, list[0]) { it.canBuyHouse }
        // Only brown siblings checked — none else match, stay on current
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `findNextInGroup stays on current when predicate matches nothing in group`() {
        val list = listOf(
            prop(1, color = "brown", canBuyHouse = false),
            prop(2, color = "brown", canBuyHouse = false),
            prop(3, color = "green", canBuyHouse = true)
        )
        val result = findNextInGroup(list, list[0]) { it.canBuyHouse }
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `findNextInGroup skips current when searching for next sibling`() {
        val list = listOf(
            prop(1, color = "pink", canBuyHouse = true),
            prop(2, color = "pink", canBuyHouse = true),
            prop(3, color = "pink", canBuyHouse = true)
        )
        val result = findNextInGroup(list, list[1]) { it.canBuyHouse }
        assertEquals(3, result.fieldId)
    }

    @Test
    fun `findNextInGroup handles single element in color group`() {
        val list = listOf(prop(1, color = "orange", canBuyHouse = true))
        val result = findNextInGroup(list, list[0]) { it.canBuyHouse }
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `findNextInGroup for railroad falls back to current`() {
        val list = listOf(
            prop(1, color = null, canMortgage = true),
            prop(2, color = null, canMortgage = false),
            prop(3, color = null, canMortgage = true)
        )
        // null color = railroad/utility; all null so they share the same "group"
        val result = findNextInGroup(list, list[0]) { it.canMortgage }
        assertEquals(3, result.fieldId)
    }

    @Test
    fun `findNextInGroup respects sellHouse staying within color group`() {
        val list = listOf(
            prop(1, color = "green", houses = 2, canSellHouse = true),
            prop(2, color = "green", houses = 2, canSellHouse = true),
            prop(3, color = "green", houses = 2, canSellHouse = true),
            prop(4, color = "blue", houses = 2, canSellHouse = true)
        )
        val result = findNextInGroup(list, list[0]) { it.canSellHouse }
        assertEquals(2, result.fieldId)
    }

    @Test
    fun `findNextInGroup for buyHotel cycles within same color`() {
        val list = listOf(
            prop(1, color = "yellow", houses = 4, canBuyHotel = true),
            prop(2, color = "yellow", houses = 4, canBuyHotel = true),
            prop(3, color = "yellow", houses = 4, canBuyHotel = true),
            prop(4, color = "red", houses = 4, canBuyHotel = true)
        )
        val result = findNextInGroup(list, list[2]) { it.canBuyHotel }
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `sortManageableProperties groups by property color`() {
        val list = listOf(
            prop(1, name = "C", color = "pink"),
            prop(2, name = "A", color = "brown"),
            prop(3, name = "B", color = "pink"),
            prop(4, name = "D", color = "brown")
        )
        val sorted = sortManageableProperties(list)
        assertEquals("brown", sorted[0].color)
        assertEquals("brown", sorted[1].color)
        assertEquals("pink", sorted[2].color)
        assertEquals("pink", sorted[3].color)
    }

    @Test
    fun `sortManageableProperties sorts alphabetically within same color`() {
        val list = listOf(
            prop(1, name = "B", color = "brown"),
            prop(2, name = "A", color = "brown")
        )
        val sorted = sortManageableProperties(list)
        assertEquals("A", sorted[0].name)
        assertEquals("B", sorted[1].name)
    }

    @Test
    fun `sortManageableProperties sorts railroads and utilities after colored properties`() {
        val list = listOf(
            prop(1, name = "Station", color = null),
            prop(2, name = "Street", color = "brown")
        )
        val sorted = sortManageableProperties(list)
        assertEquals("brown", sorted[0].color)
        assertEquals(null, sorted[1].color)
    }

    @Test
    fun `sortManageableProperties handles empty list`() {
        val sorted = sortManageableProperties(emptyList())
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `sortManageableProperties does NOT sort by buildings or mortgage status`() {
        val list = listOf(
            prop(3, name = "A", color = "green", houses = 3, hasHotel = true),
            prop(1, name = "A", color = "brown", houses = 0, isMortgaged = true),
            prop(2, name = "B", color = "brown", houses = 4),
            prop(4, name = "B", color = "green", houses = 0)
        )
        val sorted = sortManageableProperties(list)
        // Just by color + name, not building/mortgage state
        assertEquals(1, sorted[0].fieldId) // brown A (mortgaged)
        assertEquals(2, sorted[1].fieldId) // brown B (4 houses)
        assertEquals(3, sorted[2].fieldId) // green A (hotel)
        assertEquals(4, sorted[3].fieldId) // green B (vacant)
    }

    @Test
    fun `findNextInGroup with sortManageableProperties ordering finds next in correct order`() {
        val list = listOf(
            prop(3, name = "C", color = "green", canBuyHouse = true),
            prop(1, name = "A", color = "brown", canBuyHouse = true),
            prop(2, name = "B", color = "brown", canBuyHouse = true),
            prop(4, name = "D", color = "green", canBuyHouse = true)
        )
        val sorted = sortManageableProperties(list)
        val result = findNextInGroup(sorted, sorted[0]) { it.canBuyHouse }
        // Sorted: brown-A, brown-B, green-C, green-D
        // brown-A has sibling brown-B which also canBuyHouse → advances to brown-B
        assertEquals(2, result.fieldId)
    }
}
