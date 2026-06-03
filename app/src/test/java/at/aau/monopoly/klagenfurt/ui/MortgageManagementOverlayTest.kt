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

    // ═══════════════════════════════════════════════
    // Additional coverage tests for MortgageManagementOverlay.kt
    // ═══════════════════════════════════════════════

    @Test
    fun `sortManageableProperties handles all color types`() {
        val list = listOf(
            prop(1, name = "A", color = "light_blue"),
            prop(2, name = "B", color = "orange"),
            prop(3, name = "C", color = "red"),
            prop(4, name = "D", color = "yellow"),
            prop(5, name = "E", color = "dark_blue"),
            prop(6, name = "F", color = "brown"),
            prop(7, name = "G", color = "pink"),
            prop(8, name = "H", color = "green")
        )
        val sorted = sortManageableProperties(list)
        // Expected order: brown, light_blue, pink, orange, red, yellow, green, dark_blue
        assertEquals("brown", sorted[0].color)
        assertEquals("light_blue", sorted[1].color)
        assertEquals("pink", sorted[2].color)
        assertEquals("orange", sorted[3].color)
        assertEquals("red", sorted[4].color)
        assertEquals("yellow", sorted[5].color)
        assertEquals("green", sorted[6].color)
        assertEquals("dark_blue", sorted[7].color)
    }

    @Test
    fun `sortManageableProperties handles unknown color as last`() {
        val list = listOf(
            prop(1, name = "B", color = "unknown_color"),
            prop(2, name = "A", color = "brown"),
            prop(3, name = "A", color = "another_unknown")
        )
        val sorted = sortManageableProperties(list)
        assertEquals("brown", sorted[0].color)
        // Unknown colors come after all known colors, sorted by name
        assertEquals("another_unknown", sorted[1].color) // name "A"
        assertEquals("unknown_color", sorted[2].color) // name "B"
    }

    @Test
    fun `sortManageableProperties groups null color properties together at end`() {
        val list = listOf(
            prop(1, name = "Station A", color = null),
            prop(2, name = "Brown St", color = "brown"),
            prop(3, name = "Station B", color = null),
            prop(4, name = "Green Ave", color = "green")
        )
        val sorted = sortManageableProperties(list)
        assertEquals("brown", sorted[0].color)
        assertEquals("green", sorted[1].color)
        assertNull(sorted[2].color)
        assertNull(sorted[3].color)
    }

    @Test
    fun `sortManageableProperties null color sorted alphabetically by name`() {
        val list = listOf(
            prop(1, name = "Zeta Station", color = null),
            prop(2, name = "Alpha Station", color = null)
        )
        val sorted = sortManageableProperties(list)
        assertEquals("Alpha Station", sorted[0].name)
        assertEquals("Zeta Station", sorted[1].name)
    }

    @Test
    fun `findNextInGroup returns current when fieldId not found in list`() {
        val list = listOf(
            prop(1, color = "brown", canBuyHouse = true),
            prop(2, color = "brown", canBuyHouse = true)
        )
        val notInList = prop(99, color = "brown", canBuyHouse = true)
        val result = findNextInGroup(list, notInList) { it.canBuyHouse }
        assertEquals(99, result.fieldId)
    }

    @Test
    fun `ManageableProperty data class defaults are correct`() {
        val prop = ManageableProperty(
            fieldId = 1, name = "Test", color = "brown",
            price = 100, mortgageValue = 50, unmortgageCost = 55,
            houses = 0, hasHotel = false, isMortgaged = false,
            houseCost = 50, hotelCost = 50,
            sellHouseValue = 25, sellHotelValue = 25
        )
        assertFalse(prop.canSellHouse)
        assertFalse(prop.canSellHotel)
        assertFalse(prop.canBuyHouse)
        assertFalse(prop.canBuyHotel)
        assertFalse(prop.canMortgage)
    }

    @Test
    fun `ManageableProperty data class with all flags true`() {
        val prop = ManageableProperty(
            fieldId = 1, name = "Test", color = "brown",
            price = 100, mortgageValue = 50, unmortgageCost = 55,
            houses = 2, hasHotel = false, isMortgaged = false,
            houseCost = 50, hotelCost = 50,
            sellHouseValue = 25, sellHotelValue = 25,
            canSellHouse = true, canSellHotel = true,
            canBuyHouse = true, canBuyHotel = true,
            canMortgage = true
        )
        assertTrue(prop.canSellHouse)
        assertTrue(prop.canSellHotel)
        assertTrue(prop.canBuyHouse)
        assertTrue(prop.canBuyHotel)
        assertTrue(prop.canMortgage)
    }

    @Test
    fun `sortManageableProperties preserves single item list`() {
        val list = listOf(prop(1, name = "Only", color = "red"))
        val sorted = sortManageableProperties(list)
        assertEquals(1, sorted.size)
        assertEquals(1, sorted[0].fieldId)
    }

    @Test
    fun `sortManageableProperties stable sort within same color and name`() {
        val list = listOf(
            prop(1, name = "Same", color = "brown", houses = 0),
            prop(2, name = "Same", color = "brown", houses = 3)
        )
        val sorted = sortManageableProperties(list)
        // Both have same color and name, so original order should be preserved (stable sort)
        assertEquals(1, sorted[0].fieldId)
        assertEquals(2, sorted[1].fieldId)
    }

    @Test
    fun `findNextInGroup with mortgage predicate wraps correctly`() {
        val list = listOf(
            prop(1, color = "orange", canMortgage = false),
            prop(2, color = "orange", canMortgage = true),
            prop(3, color = "orange", canMortgage = false)
        )
        val result = findNextInGroup(list, list[0]) { it.canMortgage }
        assertEquals(2, result.fieldId)
    }

    @Test
    fun `findNextInGroup with sellHotel predicate`() {
        val list = listOf(
            prop(1, color = "red", hasHotel = true, canSellHotel = true),
            prop(2, color = "red", hasHotel = true, canSellHotel = false),
            prop(3, color = "red", hasHotel = true, canSellHotel = true)
        )
        val result = findNextInGroup(list, list[0]) { it.canSellHotel }
        assertEquals(3, result.fieldId)
    }

    @Test
    fun `sortManageableProperties mortgaged properties sort after unmortgaged within same color`() {
        // The colorKey function adds 200 for mortgaged, so mortgaged brown != unmortgaged brown
        val list = listOf(
            prop(1, name = "A", color = "brown", isMortgaged = true),
            prop(2, name = "B", color = "brown", isMortgaged = false)
        )
        val sorted = sortManageableProperties(list)
        // Both are brown, sorted by name; mortgage status doesn't affect sortManageableProperties
        // (sortManageableProperties uses PropertyColor ordinal, not colorKey)
        assertEquals("A", sorted[0].name)
        assertEquals("B", sorted[1].name)
    }

    @Test
    fun `findNextInGroup with empty siblings list returns current`() {
        val list = listOf(
            prop(1, color = "brown", canBuyHouse = true),
            prop(2, color = "green", canBuyHouse = true)
        )
        // Only one brown property
        val result = findNextInGroup(list, list[0]) { it.canBuyHouse }
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `findNextInGroup with two properties in same color both matching`() {
        val list = listOf(
            prop(1, color = "orange", canMortgage = true),
            prop(2, color = "orange", canMortgage = true)
        )
        val result = findNextInGroup(list, list[0]) { it.canMortgage }
        assertEquals(2, result.fieldId)
    }

    @Test
    fun `findNextInGroup wraps from second to first in two-item group`() {
        val list = listOf(
            prop(1, color = "orange", canMortgage = true),
            prop(2, color = "orange", canMortgage = true)
        )
        val result = findNextInGroup(list, list[1]) { it.canMortgage }
        assertEquals(1, result.fieldId)
    }

    @Test
    fun `ManageableProperty equality and copy`() {
        val prop1 = ManageableProperty(
            fieldId = 5, name = "Test", color = "red",
            price = 200, mortgageValue = 100, unmortgageCost = 110,
            houses = 3, hasHotel = false, isMortgaged = false,
            houseCost = 100, hotelCost = 100,
            sellHouseValue = 50, sellHotelValue = 50,
            canSellHouse = true, canSellHotel = false,
            canBuyHouse = true, canBuyHotel = false,
            canMortgage = false
        )
        val prop2 = prop1.copy(fieldId = 6)
        assertNotEquals(prop1, prop2)
        assertEquals(prop1.name, prop2.name)
        assertEquals(6, prop2.fieldId)
    }

    @Test
    fun `sortManageableProperties with large mixed list maintains color order`() {
        val list = listOf(
            prop(1, name = "D", color = "dark_blue"),
            prop(2, name = "B", color = "light_blue"),
            prop(3, name = "O", color = "orange"),
            prop(4, name = "P", color = "pink"),
            prop(5, name = "R", color = "red"),
            prop(6, name = "Y", color = "yellow"),
            prop(7, name = "G", color = "green"),
            prop(8, name = "B", color = "brown"),
            prop(9, name = "S", color = null) // railroad/utility
        )
        val sorted = sortManageableProperties(list)
        assertEquals("brown", sorted[0].color)
        assertEquals("light_blue", sorted[1].color)
        assertEquals("pink", sorted[2].color)
        assertEquals("orange", sorted[3].color)
        assertEquals("red", sorted[4].color)
        assertEquals("yellow", sorted[5].color)
        assertEquals("green", sorted[6].color)
        assertEquals("dark_blue", sorted[7].color)
        assertNull(sorted[8].color)
    }

    @Test
    fun `findNextInGroup predicate never matches returns current`() {
        val list = listOf(
            prop(1, color = "yellow", canBuyHouse = false),
            prop(2, color = "yellow", canBuyHouse = false),
            prop(3, color = "yellow", canBuyHouse = false)
        )
        val result = findNextInGroup(list, list[1]) { it.canBuyHouse }
        assertEquals(2, result.fieldId)
    }

    @Test
    fun `ManageableProperty toString contains all fields`() {
        val p = prop(42, name = "TestStreet", color = "green")
        val str = p.toString()
        assertTrue(str.contains("42"))
        assertTrue(str.contains("TestStreet"))
        assertTrue(str.contains("green"))
    }

    @Test
    fun `ManageableProperty hashCode differs for different fieldIds`() {
        val p1 = prop(1, name = "A", color = "brown")
        val p2 = prop(2, name = "A", color = "brown")
        assertNotEquals(p1.hashCode(), p2.hashCode())
    }
}
