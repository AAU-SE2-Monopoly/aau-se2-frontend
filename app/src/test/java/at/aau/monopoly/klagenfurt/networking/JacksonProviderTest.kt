package at.aau.monopoly.klagenfurt.networking

import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.*
import org.junit.Assert.*
import org.junit.Test

class JacksonProviderTest {

    private val mapper = JacksonProvider.objectMapper

    @Test
    fun `deserialize GoField from JSON`() {
        val json = """{"id":0,"name":"Go","type":"GO","salary":200}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is GoField)
        assertEquals(0, field.id)
        assertEquals("Go", field.name)
        assertEquals(200, (field as GoField).salary)
    }

    @Test
    fun `deserialize PropertyField from JSON`() {
        val json = """{"id":1,"name":"Herrengasse","type":"PROPERTY","color":"BROWN","price":60,"rent":[2,10,30,90,160,250],"houseCost":50,"hotelCost":50}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is PropertyField)
        val pf = field as PropertyField
        assertEquals(PropertyColor.BROWN, pf.color)
        assertEquals(60, pf.price)
        assertEquals(listOf(2, 10, 30, 90, 160, 250), pf.rent)
    }

    @Test
    fun `deserialize TaxField from JSON`() {
        val json = """{"id":4,"name":"Tax","type":"TAX","amount":200}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is TaxField)
        assertEquals(200, (field as TaxField).amount)
    }

    @Test
    fun `deserialize RailroadField from JSON`() {
        val json = """{"id":5,"name":"Railroad","type":"RAILROAD","price":200}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is RailroadField)
        assertEquals(200, (field as RailroadField).price)
    }

    @Test
    fun `deserialize UtilityField from JSON`() {
        val json = """{"id":12,"name":"Utility","type":"UTILITY","price":150}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is UtilityField)
        assertEquals(150, (field as UtilityField).price)
    }

    @Test
    fun `deserialize ChanceField from JSON`() {
        val json = """{"id":7,"name":"Chance","type":"CHANCE"}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is ChanceField)
    }

    @Test
    fun `deserialize CommunityChestField from JSON`() {
        val json = """{"id":2,"name":"Community Chest","type":"COMMUNITY_CHEST"}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is CommunityChestField)
    }

    @Test
    fun `deserialize JailField from JSON`() {
        val json = """{"id":10,"name":"Jail","type":"JAIL"}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is JailField)
    }

    @Test
    fun `deserialize FreeParkingField from JSON`() {
        val json = """{"id":20,"name":"Free Parking","type":"FREE_PARKING"}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is FreeParkingField)
    }

    @Test
    fun `deserialize GoToJailField from JSON`() {
        val json = """{"id":30,"name":"Go To Jail","type":"GO_TO_JAIL"}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is GoToJailField)
    }

    @Test
    fun `unknown properties are ignored`() {
        val json = """{"id":0,"name":"Go","type":"GO","salary":200,"unknownProp":"value"}"""
        val field = mapper.readValue(json, Field::class.java)
        assertTrue(field is GoField)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing type property throws exception`() {
        val json = """{"id":0,"name":"Go"}"""
        mapper.readValue(json, Field::class.java)
    }

    @Test
    fun `serialize and deserialize GoField roundtrip`() {
        val original = GoField(id = 0, name = "Go", salary = 200)
        val json = mapper.writeValueAsString(original)
        val deserialized = mapper.readValue(json, Field::class.java)
        assertTrue(deserialized is GoField)
        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
    }
}

