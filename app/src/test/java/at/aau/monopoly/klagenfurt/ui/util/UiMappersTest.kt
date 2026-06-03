package at.aau.monopoly.klagenfurt.ui.util

import androidx.compose.ui.graphics.Color
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
import com.example.myapplication.R
import org.junit.Assert.*
import org.junit.Test

class UiMappersTest {

    @Test
    fun `ownerIdFromField returns ownerId for PropertyField`() {
        val field = PropertyField(
            id = 1, name = "Mediterranean Ave", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50, ownerId = "p1"
        )
        assertEquals("p1", field.ownerIdFromField())
    }

    @Test
    fun `ownerIdFromField returns ownerId for RailroadField`() {
        val field = RailroadField(id = 5, name = "Reading Railroad", ownerId = "p2")
        assertEquals("p2", field.ownerIdFromField())
    }

    @Test
    fun `ownerIdFromField returns ownerId for UtilityField`() {
        val field = UtilityField(id = 12, name = "Electric Company", ownerId = "p3")
        assertEquals("p3", field.ownerIdFromField())
    }

    @Test
    fun `ownerIdFromField returns null for non-ownable field`() {
        val field = GoField()
        assertNull(field.ownerIdFromField())
    }

    @Test
    fun `toManageableProperty for PropertyField with no siblings`() {
        val field = PropertyField(
            id = 1, name = "Mediterranean Ave", color = PropertyColor.BROWN,
            price = 60, rent = listOf(2, 10, 30, 90, 160, 250),
            houseCost = 50, hotelCost = 50, ownerId = "p1", houses = 0
        )
        val result = field.toManageableProperty(emptyList())
        assertEquals(1, result.fieldId)
        assertEquals("Mediterranean Ave", result.name)
        assertEquals("BROWN", result.color)
        assertEquals(60, result.price)
        assertEquals(30, result.mortgageValue)
        assertEquals(33, result.unmortgageCost)
        assertEquals(0, result.houses)
        assertFalse(result.hasHotel)
        assertFalse(result.isMortgaged)
        assertEquals(50, result.houseCost)
        assertEquals(50, result.hotelCost)
        assertEquals(25, result.sellHouseValue)
        assertEquals(25, result.sellHotelValue)
        assertTrue(result.canBuyHouse)
        assertFalse(result.canBuyHotel)
        assertTrue(result.canMortgage)
        assertFalse(result.canSellHouse)
        assertFalse(result.canSellHotel)
    }

    @Test
    fun `toManageableProperty for RailroadField`() {
        val field = RailroadField(id = 5, name = "Reading Railroad", price = 200, ownerId = "p1")
        val result = field.toManageableProperty(emptyList())
        assertEquals(5, result.fieldId)
        assertEquals(100, result.mortgageValue)
        assertEquals(111, result.unmortgageCost)
        assertEquals(0, result.houseCost)
        assertEquals(0, result.hotelCost)
        assertNull(result.color)
        assertTrue(result.canMortgage)
        assertFalse(result.canBuyHouse)
        assertFalse(result.canBuyHotel)
    }

    @Test
    fun `toManageableProperty for UtilityField`() {
        val field = UtilityField(id = 12, name = "Electric Company", price = 150, ownerId = "p1")
        val result = field.toManageableProperty(emptyList())
        assertEquals(12, result.fieldId)
        assertEquals(75, result.mortgageValue)
        assertEquals(83, result.unmortgageCost)
        assertNull(result.color)
    }

    @Test
    fun `toManageableProperty for railroad when mortgaged`() {
        val field = RailroadField(id = 5, name = "Reading Railroad", price = 200, ownerId = "p1", isMortgaged = true)
        val result = field.toManageableProperty(emptyList())
        assertTrue(result.isMortgaged)
        assertFalse(result.canMortgage)
    }

    @Test
    fun `toManageableProperty for non-ownable field returns default`() {
        val field = GoField()
        val result = field.toManageableProperty(emptyList())
        assertEquals(0, result.price)
        assertNull(result.color)
        assertFalse(result.isMortgaged)
    }

    @Test
    fun `toComposeColor maps all colors`() {
        assertEquals(Color(0xFF955436), PropertyColor.BROWN.toComposeColor())
        assertEquals(Color(0xFFAAE0FA), PropertyColor.LIGHT_BLUE.toComposeColor())
        assertEquals(Color(0xFFD93A96), PropertyColor.PINK.toComposeColor())
        assertEquals(Color(0xFFF7941D), PropertyColor.ORANGE.toComposeColor())
        assertEquals(Color(0xFFED1B24), PropertyColor.RED.toComposeColor())
        assertEquals(Color(0xFFD4A017), PropertyColor.YELLOW.toComposeColor())
        assertEquals(Color(0xFF1FB25A), PropertyColor.GREEN.toComposeColor())
        assertEquals(Color(0xFF0072BB), PropertyColor.DARK_BLUE.toComposeColor())
    }

    @Test
    fun `toPropertyComposeColor maps string to color`() {
        assertEquals(Color(0xFF955436), "BROWN".toPropertyComposeColor())
        assertEquals(Color(0xFF0072BB), "dark_blue".toPropertyComposeColor())
        assertEquals(Color(0xFF424242), null.toPropertyComposeColor())
        assertEquals(Color(0xFF424242), "unknown".toPropertyComposeColor())
    }

    @Test
    fun `getPlayerTokenResource maps known icons`() {
        assertEquals(R.drawable.lindwurm, getPlayerTokenResource("lindwurm"))
        assertEquals(R.drawable.woertherseemandl, getPlayerTokenResource("woerthersee"))
        assertEquals(R.drawable.gti, getPlayerTokenResource("gti"))
        assertEquals(R.drawable.ironman, getPlayerTokenResource("ironman"))
        assertEquals(R.drawable.josef, getPlayerTokenResource("josef"))
    }

    @Test
    fun `getPlayerTokenResource returns default for unknown icon`() {
        assertEquals(R.drawable.lindwurm, getPlayerTokenResource("unknown"))
    }

    @Test
    fun `getPlayerTokenResource is case insensitive`() {
        assertEquals(R.drawable.lindwurm, getPlayerTokenResource("LINDWURM"))
        assertEquals(R.drawable.gti, getPlayerTokenResource("GTI"))
    }
}
