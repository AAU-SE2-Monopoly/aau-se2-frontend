package at.aau.monopoly.klagenfurt.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the ManageableProperty data class.
 */
class ManageablePropertyTest {

    private fun defaultProp(
        fieldId: Int = 1,
        name: String = "Test",
        color: String? = "brown",
        price: Int = 100,
        mortgageValue: Int = 50,
        unmortgageCost: Int = 55,
        houses: Int = 0,
        hasHotel: Boolean = false,
        isMortgaged: Boolean = false,
        houseCost: Int = 50,
        hotelCost: Int = 50,
        sellHouseValue: Int = 25,
        sellHotelValue: Int = 25,
        canSellHouse: Boolean = false,
        canSellHotel: Boolean = false,
        canBuyHouse: Boolean = false,
        canBuyHotel: Boolean = false,
        canMortgage: Boolean = false
    ) = ManageableProperty(
        fieldId = fieldId, name = name, color = color,
        price = price, mortgageValue = mortgageValue, unmortgageCost = unmortgageCost,
        houses = houses, hasHotel = hasHotel, isMortgaged = isMortgaged,
        houseCost = houseCost, hotelCost = hotelCost,
        sellHouseValue = sellHouseValue, sellHotelValue = sellHotelValue,
        canSellHouse = canSellHouse, canSellHotel = canSellHotel,
        canBuyHouse = canBuyHouse, canBuyHotel = canBuyHotel,
        canMortgage = canMortgage
    )

    @Test
    fun `default boolean flags are false`() {
        val prop = defaultProp()
        assertFalse(prop.canSellHouse)
        assertFalse(prop.canSellHotel)
        assertFalse(prop.canBuyHouse)
        assertFalse(prop.canBuyHotel)
        assertFalse(prop.canMortgage)
        assertFalse(prop.hasHotel)
        assertFalse(prop.isMortgaged)
    }

    @Test
    fun `property with hotel`() {
        val prop = defaultProp(hasHotel = true)
        assertTrue(prop.hasHotel)
    }

    @Test
    fun `property with houses`() {
        val prop = defaultProp(houses = 3)
        assertEquals(3, prop.houses)
    }

    @Test
    fun `property with null color represents railroad or utility`() {
        val prop = defaultProp(color = null)
        assertNull(prop.color)
    }

    @Test
    fun `equality of ManageableProperty`() {
        val a = defaultProp(fieldId = 1, name = "A")
        val b = defaultProp(fieldId = 1, name = "A")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `inequality of ManageableProperty`() {
        val a = defaultProp(fieldId = 1)
        val b = defaultProp(fieldId = 2)
        assertNotEquals(a, b)
    }

    @Test
    fun `copy works`() {
        val original = defaultProp(fieldId = 1, name = "Original", houses = 2)
        val copy = original.copy(name = "Copy", houses = 3)
        assertEquals("Copy", copy.name)
        assertEquals(3, copy.houses)
        assertEquals(1, copy.fieldId)
    }

    @Test
    fun `mortgaged property has correct values`() {
        val prop = defaultProp(isMortgaged = true, mortgageValue = 50, unmortgageCost = 55)
        assertTrue(prop.isMortgaged)
        assertEquals(50, prop.mortgageValue)
        assertEquals(55, prop.unmortgageCost)
    }

    @Test
    fun `sell values are half of costs`() {
        val prop = defaultProp(houseCost = 100, hotelCost = 200, sellHouseValue = 50, sellHotelValue = 100)
        assertEquals(prop.houseCost / 2, prop.sellHouseValue)
        assertEquals(prop.hotelCost / 2, prop.sellHotelValue)
    }

    @Test
    fun `toString is not null`() {
        val prop = defaultProp()
        assertNotNull(prop.toString())
        assertTrue(prop.toString().contains("Test"))
    }
}

