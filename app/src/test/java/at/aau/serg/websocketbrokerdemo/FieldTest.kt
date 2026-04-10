package at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketbrokerdemo.model.field.ChanceField
import at.aau.serg.websocketbrokerdemo.model.field.CommunityChestField
import at.aau.serg.websocketbrokerdemo.model.field.FreeParkingField
import at.aau.serg.websocketbrokerdemo.model.field.GoField
import at.aau.serg.websocketbrokerdemo.model.field.GoToJailField
import at.aau.serg.websocketbrokerdemo.model.field.JailField
import at.aau.serg.websocketbrokerdemo.model.field.TaxField
import at.aau.serg.websocketbrokerdemo.model.field.UtilityField
import at.aau.serg.websocketbrokerdemo.model.enums.FieldType
import at.aau.serg.websocketbrokerdemo.model.enums.PropertyColor
import at.aau.serg.websocketbrokerdemo.model.field.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FieldTest {

    @Test
    fun `verify GoField`() {
        val field = GoField()
        assertEquals(0, field.id)
        assertEquals("Go", field.name)
        assertEquals(FieldType.GO, field.type)
        assertEquals(200, field.salary)
        
        val copy = field.copy(salary = 400)
        assertEquals(400, copy.salary)
        assertNotNull(field.toString())
    }

    @Test
    fun `verify TaxField`() {
        val field = TaxField(id = 4, name = "Income Tax", amount = 200)
        assertEquals(FieldType.TAX, field.type)
        assertEquals(200, field.amount)
        
        val copy = field.copy(amount = 100)
        assertEquals(100, copy.amount)
        assertNotNull(field.toString())

    }

    @Test
    fun `verify RailroadField`() {
        val field = RailroadField(id = 5, name = "Reading Railroad")
        assertEquals(FieldType.RAILROAD, field.type)
        assertEquals(200, field.price)
        assertNull(field.ownerId)
        assertFalse(field.isMortgaged)

        field.ownerId = "player1"
        field.isMortgaged = true
        assertEquals("player1", field.ownerId)
        assertTrue(field.isMortgaged)

        val copy = field.copy(price = 300)
        assertEquals(300, copy.price)
        assertNotNull(field.toString())
    }

    @Test
    fun `verify UtilityField`() {
        val field = UtilityField(id = 12, name = "Electric Company")
        assertEquals(FieldType.UTILITY, field.type)
        assertEquals(150, field.price)
        
        field.ownerId = "player2"
        assertEquals("player2", field.ownerId)

        val copy = field.copy(ownerId = "player3")
        assertEquals("player3", copy.ownerId)
        assertNotNull(field.toString())
    }

    @Test
    fun `verify JailField`() {
        val field = JailField()
        assertEquals(10, field.id)
        assertEquals(FieldType.JAIL, field.type)
        assertNotNull(field.toString())
    }

    @Test
    fun `verify ChanceField`() {
        val field = ChanceField(id = 7, name = "Chance")
        assertEquals(FieldType.CHANCE, field.type)
        assertNotNull(field.toString())
    }

    @Test
    fun `verify CommunityChestField`() {
        val field = CommunityChestField(id = 2, name = "Community Chest")
        assertEquals(FieldType.COMMUNITY_CHEST, field.type)
        assertNotNull(field.toString())
    }

    @Test
    fun `verify FreeParkingField`() {
        val field = FreeParkingField()
        assertEquals(20, field.id)
        assertEquals(FieldType.FREE_PARKING, field.type)
        assertNotNull(field.toString())
    }

    @Test
    fun `verify GoToJailField`() {
        val field = GoToJailField()
        assertEquals(30, field.id)
        assertEquals(FieldType.GO_TO_JAIL, field.type)
        assertNotNull(field.toString())
    }




    /**  fromJsonTests   **/
    @Test
    fun `test fromJson in Field with valid JSON`() {
        val json= JSONObject()
            .put("id", 1)
            .put("name", "Test Field")
            .put("type", "GO")

        val field = Field.fromJson(json)
        assertEquals(1, field.id)
        assertEquals("Test Field", field.name)
        assertEquals(FieldType.GO, field.type)

    }
    @Test
    fun `test fromJson with missing required fields and GOType`() {
        val json = JSONObject()
            .put("id", 1)
            .put("type", "GO")
        val field = Field.fromJson(json)
        assertEquals(1, field.id)
        assertEquals("Go", field.name)
        assertEquals(FieldType.GO, field.type)

    }
    @Test
    fun `test fromJson with GoToJailType`() {
        val json = JSONObject()
            .put("id", 30)
            .put("type", "GO_TO_JAIL")
        val field = Field.fromJson(json)
        assertEquals(30, field.id)
    }
    @Test
    fun `test fromJson with ChanceType`() {
        val json = JSONObject()
            .put("id", 7)
            .put("type", "CHANCE")
        val field = Field.fromJson(json)
        assertEquals(7, field.id)
    }
    @Test
    fun `test fromJson with CommunityChestType`() {
        val json = JSONObject()
            .put("id", 2)
            .put("type", "COMMUNITY_CHEST")
        val field = Field.fromJson(json)
        assertEquals(2, field.id)
    }
    @Test
        fun `test fromJson with FreeParkingType`() {
            val json = JSONObject()
                .put("id", 20)
                .put("type", "FREE_PARKING")
            val field = Field.fromJson(json)
            assertEquals(20, field.id)
        }
    @Test
    fun `test fromJson with JailType`() {
        val json = JSONObject()
            .put("id", 10)
            .put("type", "JAIL")
        val field = Field.fromJson(json)
        assertEquals(10, field.id)

    }
    @Test
    fun `test fromJson with PropertyType`() {
        val json = JSONObject()
            .put("id", 1)
            .put("name", "Test Property")
            .put("type", "PROPERTY")
            .put("color", "BROWN")
            .put("price", 60)
            .put("rent", JSONArray().put(2).put(10).put(30).put(90).put(160).put(250))
            .put("houseCost", 50)
            .put("hotelCost", 50)
            .put("ownerId", "player1")
            .put("houses", 0)
            .put("hasHotel", false)
            .put("isMortgaged", false)

        val field = Field.fromJson(json)
        assertEquals(1, field.id)
        assertEquals("Test Property", field.name)
        assertEquals(FieldType.PROPERTY, field.type)
        assertEquals(PropertyColor.BROWN, (field as PropertyField).color)
        assertEquals(60, field.price)
        assertEquals(listOf(2, 10, 30, 90, 160, 250), field.rent)
        assertEquals(50, field.houseCost)
        assertEquals(50, field.hotelCost)
        assertEquals("player1", field.ownerId)
        assertEquals(0, field.houses)
        assertFalse(field.hasHotel)
        assertFalse(field.isMortgaged)
    }
    @Test
    fun `test fromJson with RailroadType`() {

        val json = JSONObject()
            .put("id", 5)
            .put("name", "Test Railroad")
            .put("type", "RAILROAD")
            .put("price", 200)
            .put("ownerId", "player2")
            .put("isMortgaged", true)
        val field = Field.fromJson(json)
        assertEquals(5, field.id)
        assertEquals("Test Railroad", field.name)
        assertEquals(FieldType.RAILROAD, field.type)
        assertEquals(200, (field as RailroadField).price)
        assertEquals("player2", field.ownerId)
        assertTrue(field.isMortgaged)
    }
    @Test
    fun `test fromJson with TaxType`() {
        val json = JSONObject()
            .put("id", 4)
            .put("name", "Test Tax")
            .put("type", "TAX")
            .put("amount", 100)
        val field = Field.fromJson(json)
        assertEquals(4, field.id)
        assertEquals("Test Tax", field.name)
        assertEquals(FieldType.TAX, field.type)
        assertEquals(100, (field as TaxField).amount)
    }
    @Test
    fun `test fromJson with UtilityType`() {
        val json = JSONObject()
            .put("id", 12)
            .put("name", "Test Utility")
            .put("type", "UTILITY")
            .put("price", 150)
            .put("ownerId", "player3")
            .put("isMortgaged", false)
        val field = Field.fromJson(json)
        assertEquals(12, field.id)
        assertEquals("Test Utility", field.name)
        assertEquals(FieldType.UTILITY, field.type)
        assertEquals(150, (field as UtilityField).price)
        assertEquals("player3", field.ownerId)
        assertFalse(field.isMortgaged)
    }
    @Test
    fun `test fromJson with invalid type`() {
        val json = JSONObject()
            .put("id", 1)
            .put("type", "INVALID_TYPE")
        assertThrows(IllegalArgumentException::class.java) {
            Field.fromJson(json)
        }
    }

}
