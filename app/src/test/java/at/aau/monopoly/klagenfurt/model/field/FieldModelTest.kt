package at.aau.monopoly.klagenfurt.model.field

import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FieldModelTest {

    @Test
    fun `GoField has correct defaults`() {
        val go = GoField()
        assertEquals(0, go.id)
        assertEquals("Go", go.name)
        assertEquals(FieldType.GO, go.type)
        assertEquals(200, go.salary)
    }

    @Test
    fun `GoField custom salary`() {
        val go = GoField(salary = 400)
        assertEquals(400, go.salary)
    }

    @Test
    fun `JailField has correct defaults`() {
        val jail = JailField()
        assertEquals(10, jail.id)
        assertEquals("Jail / Just Visiting", jail.name)
        assertEquals(FieldType.JAIL, jail.type)
    }

    @Test
    fun `FreeParkingField has correct defaults`() {
        val fp = FreeParkingField()
        assertEquals(20, fp.id)
        assertEquals("Free Parking", fp.name)
        assertEquals(FieldType.FREE_PARKING, fp.type)
    }

    @Test
    fun `GoToJailField has correct defaults`() {
        val gtj = GoToJailField()
        assertEquals(30, gtj.id)
        assertEquals("Go To Jail", gtj.name)
        assertEquals(FieldType.GO_TO_JAIL, gtj.type)
    }

    @Test
    fun `ChanceField uses provided id`() {
        val chance = ChanceField(id = 7)
        assertEquals(7, chance.id)
        assertEquals("Chance", chance.name)
        assertEquals(FieldType.CHANCE, chance.type)
    }

    @Test
    fun `CommunityChestField uses provided id`() {
        val cc = CommunityChestField(id = 2)
        assertEquals(2, cc.id)
        assertEquals("Community Chest", cc.name)
        assertEquals(FieldType.COMMUNITY_CHEST, cc.type)
    }

    @Test
    fun `TaxField stores amount`() {
        val tax = TaxField(id = 4, name = "Income Tax", amount = 200)
        assertEquals(4, tax.id)
        assertEquals("Income Tax", tax.name)
        assertEquals(FieldType.TAX, tax.type)
        assertEquals(200, tax.amount)
    }

    @Test
    fun `RailroadField implements OwnableField`() {
        val rr = RailroadField(id = 5, name = "Hauptbahnhof")
        assertTrue(rr is OwnableField)
        assertEquals(200, rr.price)
        assertNull(rr.ownerId)
        assertFalse(rr.isMortgaged)

        rr.ownerId = "player1"
        rr.isMortgaged = true
        assertEquals("player1", rr.ownerId)
        assertTrue(rr.isMortgaged)
    }

    @Test
    fun `UtilityField implements OwnableField`() {
        val util = UtilityField(id = 12, name = "Kelag")
        assertTrue(util is OwnableField)
        assertEquals(150, util.price)
        assertNull(util.ownerId)
        assertFalse(util.isMortgaged)

        util.ownerId = "p2"
        assertEquals("p2", util.ownerId)
    }

    @Test
    fun `PropertyField implements OwnableField with color and rent`() {
        val prop = PropertyField(
            id = 1, name = "Herrengasse",
            color = PropertyColor.BROWN, price = 60,
            rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50
        )
        assertTrue(prop is OwnableField)
        assertEquals(PropertyColor.BROWN, prop.color)
        assertEquals(6, prop.rent.size)
        assertEquals(0, prop.houses)
        assertFalse(prop.hasHotel)

        prop.houses = 3
        prop.hasHotel = true
        prop.ownerId = "owner"
        assertEquals(3, prop.houses)
        assertTrue(prop.hasHotel)
        assertEquals("owner", prop.ownerId)
    }

    @Test
    fun `OwnableField check via is operator`() {
        val fields: List<Field> = listOf(
            GoField(),
            PropertyField(1, "A", color = PropertyColor.RED, price = 100, rent = listOf(5), houseCost = 50, hotelCost = 50),
            RailroadField(5, "B"),
            UtilityField(12, "C"),
            TaxField(4, "Tax", amount = 100),
            ChanceField(7),
            CommunityChestField(2),
            JailField(),
            FreeParkingField(),
            GoToJailField()
        )
        val ownable = fields.filterIsInstance<OwnableField>()
        assertEquals(3, ownable.size)
    }

    @Test
    fun `data class equality and copy`() {
        val rr1 = RailroadField(5, "HBF")
        val rr2 = RailroadField(5, "HBF")
        assertEquals(rr1, rr2)

        val rr3 = rr1.copy(name = "Other")
        assertNotEquals(rr1, rr3)
    }
}

