package at.aau.monopoly.klagenfurt
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PropertyFieldTest {

    @Test
    fun `verify PropertyField with set properties`() {
        val propertyField = PropertyField(
            id = 1,
            name = "Heiligengeistplatz",
            type = FieldType.PROPERTY,
            color = PropertyColor.LIGHT_BLUE,
            price = 100,
            rent = listOf(10, 30, 90, 160, 250, 350),
            houseCost = 50,
            hotelCost = 500,
            ownerId = "Davensive",
            houses = 3,
            hasHotel = true,
            isMortgaged = true
        )

        assertEquals(1, propertyField.id)
        assertEquals("Heiligengeistplatz", propertyField.name)
        assertEquals(FieldType.PROPERTY, propertyField.type)
        assertEquals(PropertyColor.LIGHT_BLUE, propertyField.color)
        assertEquals(100, propertyField.price)
        assertEquals(listOf(10, 30, 90, 160, 250, 350), propertyField.rent)
        assertEquals(50, propertyField.houseCost)
        assertEquals(500, propertyField.hotelCost)
        assertEquals("Davensive", propertyField.ownerId)
        assertEquals(3, propertyField.houses)
        assertTrue(propertyField.hasHotel)
        assertTrue(propertyField.isMortgaged)
    }

    @Test
    fun `verify PropertyField default properties`() {
        val propertyField = PropertyField(
            id = 2,
            name = "Parkspur",
            color = PropertyColor.BROWN,
            price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50,
            hotelCost = 50
        )

        assertEquals(FieldType.PROPERTY, propertyField.type)
        assertNull(propertyField.ownerId)
        assertEquals(0, propertyField.houses)
        assertFalse(propertyField.hasHotel)
        assertFalse(propertyField.isMortgaged)
    }


    @Test
    fun `test copy method`() {
        val propertyField = PropertyField(
            id = 1,
            name = "A",
            color = PropertyColor.BROWN,
            price = 60,
            rent = emptyList(),
            houseCost = 50,
            hotelCost = 50
        )
        val copy = propertyField.copy(name = "B", houses = 2)

        assertEquals(1, copy.id)
        assertEquals("B", copy.name)
        assertEquals(2, copy.houses)
        assertEquals(propertyField.price, copy.price)
    }

    @Test
    fun `test toString`() {
        val propertyField = PropertyField(
            id = 1,
            name = "A",
            color = PropertyColor.BROWN,
            price = 60,
            rent = emptyList(),
            houseCost = 50,
            hotelCost = 50
        )
        assertNotNull(propertyField.toString())
    }
}
