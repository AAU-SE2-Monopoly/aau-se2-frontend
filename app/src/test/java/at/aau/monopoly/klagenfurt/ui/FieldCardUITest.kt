package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FieldCardUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleProperty = PropertyField(
        id = 1, name = "Herrengasse", color = PropertyColor.BROWN,
        price = 60, rent = listOf(2, 10, 30, 90, 160, 250),
        houseCost = 50, hotelCost = 50
    )

    @Test
    fun `PropertyField card shows name`() {
        composeTestRule.setContent { FieldCardUI(field = sampleProperty) }
        composeTestRule.onNodeWithText("Herrengasse").assertExists()
    }

    @Test
    fun `PropertyField card shows rent values`() {
        composeTestRule.setContent { FieldCardUI(field = sampleProperty) }
        // Rent label exists (first rent row)
        composeTestRule.onNodeWithText("Rent").assertExists()
        // Multiple rent values exist in the card
        composeTestRule.onAllNodesWithText("$2", substring = true).assertCountEquals(2) // "$2" and "$250"
    }

    @Test
    fun `PropertyField card shows house and hotel costs`() {
        composeTestRule.setContent { FieldCardUI(field = sampleProperty) }
        composeTestRule.onAllNodesWithText("$50")[0].assertExists()
    }

    @Test
    fun `PropertyField with houses shows house count`() {
        val withHouses = sampleProperty.copy(houses = 3)
        composeTestRule.setContent { FieldCardUI(field = withHouses) }
        composeTestRule.onNodeWithText("🏠×3", substring = true).assertExists()
    }

    @Test
    fun `PropertyField with hotel shows hotel icon`() {
        val withHotel = sampleProperty.copy(hasHotel = true)
        composeTestRule.setContent { FieldCardUI(field = withHotel) }
        composeTestRule.onNodeWithText("🏨", substring = true).assertExists()
    }

    @Test
    fun `PropertyField mortgaged shows MORTGAGED`() {
        val mortgaged = sampleProperty.copy(isMortgaged = true)
        composeTestRule.setContent { FieldCardUI(field = mortgaged) }
        composeTestRule.onNodeWithText("MORTGAGED").assertExists()
    }

    @Test
    fun `RailroadField card shows name and RAILROAD header`() {
        val railroad = RailroadField(id = 5, name = "Hauptbahnhof")
        composeTestRule.setContent { FieldCardUI(field = railroad) }
        composeTestRule.onNodeWithText("Hauptbahnhof").assertExists()
        composeTestRule.onNodeWithText("RAILROAD").assertExists()
    }

    @Test
    fun `RailroadField card shows price`() {
        val railroad = RailroadField(id = 5, name = "Hauptbahnhof", price = 200)
        composeTestRule.setContent { FieldCardUI(field = railroad) }
        composeTestRule.onAllNodesWithText("$200")[0].assertExists()
    }

    @Test
    fun `RailroadField mortgaged shows MORTGAGED`() {
        val railroad = RailroadField(id = 5, name = "Hauptbahnhof", isMortgaged = true)
        composeTestRule.setContent { FieldCardUI(field = railroad) }
        composeTestRule.onNodeWithText("MORTGAGED").assertExists()
    }

    @Test
    fun `UtilityField card shows name and UTILITY header`() {
        val utility = UtilityField(id = 12, name = "Kelag Klagenfurt")
        composeTestRule.setContent { FieldCardUI(field = utility) }
        composeTestRule.onNodeWithText("Kelag Klagenfurt").assertExists()
        composeTestRule.onNodeWithText("UTILITY").assertExists()
    }

    @Test
    fun `UtilityField card shows multiplier info`() {
        val utility = UtilityField(id = 12, name = "Kelag Klagenfurt")
        composeTestRule.setContent { FieldCardUI(field = utility) }
        composeTestRule.onNodeWithText("1 owned: 4× dice").assertExists()
        composeTestRule.onNodeWithText("2 owned: 10× dice").assertExists()
    }

    @Test
    fun `UtilityField mortgaged shows MORTGAGED`() {
        val utility = UtilityField(id = 12, name = "Kelag Klagenfurt", isMortgaged = true)
        composeTestRule.setContent { FieldCardUI(field = utility) }
        composeTestRule.onNodeWithText("MORTGAGED").assertExists()
    }

    @Test
    fun `TaxField card shows name and amount`() {
        val tax = TaxField(id = 4, name = "Reichensteuer", amount = 200)
        composeTestRule.setContent { FieldCardUI(field = tax) }
        composeTestRule.onNodeWithText("Reichensteuer").assertExists()
        composeTestRule.onNodeWithText("Pay $200").assertExists()
        composeTestRule.onNodeWithText("TAX").assertExists()
    }

    @Test
    fun `GoField renders as generic card with GO header`() {
        val go = GoField(id = 0, name = "Go")
        composeTestRule.setContent { FieldCardUI(field = go) }
        composeTestRule.onNodeWithText("Go").assertExists()
        composeTestRule.onNodeWithText("GO").assertExists()
    }

    @Test
    fun `JailField renders with JAIL header`() {
        val jail = JailField(id = 10, name = "Jail / Just Visiting")
        composeTestRule.setContent { FieldCardUI(field = jail) }
        composeTestRule.onNodeWithText("Jail / Just Visiting").assertExists()
        composeTestRule.onNodeWithText("JAIL").assertExists()
    }

    @Test
    fun `ChanceField renders with CHANCE header`() {
        val chance = ChanceField(id = 7, name = "Chance")
        composeTestRule.setContent { FieldCardUI(field = chance) }
        composeTestRule.onNodeWithText("Chance").assertExists()
        composeTestRule.onNodeWithText("CHANCE").assertExists()
    }

    @Test
    fun `FreeParkingField renders with FREE PARKING header`() {
        val fp = FreeParkingField(id = 20, name = "Free Parking")
        composeTestRule.setContent { FieldCardUI(field = fp) }
        composeTestRule.onNodeWithText("Free Parking").assertExists()
        composeTestRule.onNodeWithText("FREE PARKING").assertExists()
    }

    @Test
    fun `FieldCardUI renders with custom cardWidth and cardHeight`() {
        composeTestRule.setContent {
            FieldCardUI(
                field = sampleProperty,
                cardWidth = 200.dp,
                cardHeight = 320.dp
            )
        }
        composeTestRule.onNodeWithText("Herrengasse").assertExists()
    }

    @Test
    fun `FieldCardUI renders with small custom dimensions`() {
        composeTestRule.setContent {
            FieldCardUI(
                field = sampleProperty,
                cardWidth = 80.dp,
                cardHeight = 128.dp
            )
        }
        composeTestRule.onNodeWithText("Herrengasse").assertExists()
    }

    @Test
    fun `GoToJailField renders with GO TO JAIL header`() {
        val gtj = GoToJailField(id = 30, name = "Go To Jail")
        composeTestRule.setContent { FieldCardUI(field = gtj) }
        composeTestRule.onNodeWithText("Go To Jail").assertExists()
        composeTestRule.onNodeWithText("GO TO JAIL").assertExists()
    }

    @Test
    fun `CommunityChestField renders with COMMUNITY CHEST header`() {
        val cc = CommunityChestField(id = 2, name = "Community Chest")
        composeTestRule.setContent { FieldCardUI(field = cc) }
        composeTestRule.onNodeWithText("Community Chest").assertExists()
        composeTestRule.onNodeWithText("COMMUNITY CHEST").assertExists()
    }
}


