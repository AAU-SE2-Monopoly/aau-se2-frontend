package at.aau.monopoly.klagenfurt


import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.FreeParkingField
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.GoToJailField
import at.aau.monopoly.klagenfurt.model.field.JailField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.TaxField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import at.aau.monopoly.klagenfurt.networking.JacksonProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FieldTest {

    private val objectMapper = JacksonProvider.objectMapper

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
    /**  Jackson deserialization tests   **/
    @Test
    fun `test Jackson deserialize GO field`() {
        val json = """{"id":1,"name":"Test Field","type":"GO"}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(1, field.id)
        assertEquals("Test Field", field.name)
        assertEquals(FieldType.GO, field.type)
    }
    @Test
    fun `test Jackson deserialize GO field with missing name`() {
        val json = """{"id":1,"type":"GO"}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(1, field.id)
        assertEquals(FieldType.GO, field.type)
    }
    @Test
    fun `test Jackson deserialize GoToJail field`() {
        val json = """{"id":30,"type":"GO_TO_JAIL"}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(30, field.id)
    }
    @Test
    fun `test Jackson deserialize Chance field`() {
        val json = """{"id":7,"type":"CHANCE"}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(7, field.id)
    }
    @Test
    fun `test Jackson deserialize CommunityChest field`() {
        val json = """{"id":2,"type":"COMMUNITY_CHEST"}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(2, field.id)
    }
    @Test
    fun `test Jackson deserialize FreeParking field`() {
        val json = """{"id":20,"type":"FREE_PARKING"}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(20, field.id)
    }
    @Test
    fun `test Jackson deserialize Jail field`() {
        val json = """{"id":10,"type":"JAIL"}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(10, field.id)
    }
    @Test
    fun `test Jackson deserialize Property field`() {
        val json = """
            {
                "id":1,"name":"Test Property","type":"PROPERTY",
                "color":"BROWN","price":60,
                "rent":[2,10,30,90,160,250],
                "houseCost":50,"hotelCost":50,
                "ownerId":"player1","houses":0,
                "hasHotel":false,"isMortgaged":false
            }
        """.trimIndent()
        val field = objectMapper.readValue(json, Field::class.java)
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
    fun `test Jackson deserialize Railroad field`() {
        val json = """
            {"id":5,"name":"Test Railroad","type":"RAILROAD",
             "price":200,"ownerId":"player2","isMortgaged":true}
        """.trimIndent()
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(5, field.id)
        assertEquals("Test Railroad", field.name)
        assertEquals(FieldType.RAILROAD, field.type)
        assertEquals(200, (field as RailroadField).price)
        assertEquals("player2", field.ownerId)
        assertTrue(field.isMortgaged)
    }
    @Test
    fun `test Jackson deserialize Tax field`() {
        val json = """{"id":4,"name":"Test Tax","type":"TAX","amount":100}"""
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(4, field.id)
        assertEquals("Test Tax", field.name)
        assertEquals(FieldType.TAX, field.type)
        assertEquals(100, (field as TaxField).amount)
    }
    @Test
    fun `test Jackson deserialize Utility field`() {
        val json = """
            {"id":12,"name":"Test Utility","type":"UTILITY",
             "price":150,"ownerId":"player3","isMortgaged":false}
        """.trimIndent()
        val field = objectMapper.readValue(json, Field::class.java)
        assertEquals(12, field.id)
        assertEquals("Test Utility", field.name)
        assertEquals(FieldType.UTILITY, field.type)
        assertEquals(150, (field as UtilityField).price)
        assertEquals("player3", field.ownerId)
        assertFalse(field.isMortgaged)
    }
    @Test
    fun `test Jackson deserialize with invalid type`() {
        val json = """{"id":1,"type":"INVALID_TYPE"}"""
        assertThrows(IllegalArgumentException::class.java) {
            objectMapper.readValue(json, Field::class.java)
        }
    }

}

