package at.aau.serg.websocketbrokerdemo

import at.aau.serg.websocketdemoserver.model.enums.FieldType
import at.aau.serg.websocketdemoserver.model.field.*
import nl.jqno.equalsverifier.EqualsVerifier
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
}
