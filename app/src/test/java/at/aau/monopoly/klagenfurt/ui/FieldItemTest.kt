package at.aau.monopoly.klagenfurt.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.TaxField
import at.aau.monopoly.klagenfurt.ui.board.FieldItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FieldItemTest {
    val sw = 800f
    val sh = 450f
    val myProperty= PropertyField(
        id = 1,
        name = "Heiligengeistplatz",
        type = FieldType.PROPERTY,
        color = PropertyColor.LIGHT_BLUE,
        price = 20,
        rent = listOf(1),
        houseCost = 30,
        hotelCost = 50,
        ownerId = "Dave",
        houses = 2,
        hasHotel = true,
        isMortgaged = true
    )
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**Property Fields different index**/
    @Test
    fun `verify FieldItem renders Bottom-Bar for Side 0`(){
        composeTestRule.setContent {
            FieldItem(index = 5, field = myProperty, sw = sw, sh = sh)
        }
        composeTestRule
            .onNodeWithTag("Bottom-Bar")
            .assertExists()
    }

    @Test
    fun `verify FieldItem renders Left-Bar for Side 1`(){
        composeTestRule.setContent {
            FieldItem(index = 15, field = myProperty, sw = sw, sh = sh)
        }
        composeTestRule
            .onNodeWithTag("Left-Bar")
            .assertExists()

    }

    @Test
    fun `verify FieldItem renders Top-Bar for Side 2`(){
        composeTestRule.setContent {
            FieldItem(index = 25, field = myProperty, sw = sw, sh = sh)
        }
        composeTestRule
            .onNodeWithTag("Top-Bar")
            .assertExists()
    }

    @Test
    fun `verify FieldItem renders Right-Bar for Side 3`(){
        val newProperty=myProperty.copy(color=PropertyColor.BROWN)
        composeTestRule.setContent {
            FieldItem(index = 35, field = newProperty, sw = sw, sh = sh)
        }
        composeTestRule
            .onNodeWithTag("Right-Bar")
            .assertExists()
    }
    @Test
    fun `verify FieldItem renders all colors`() {
        val colors = listOf(
            PropertyColor.BROWN, PropertyColor.LIGHT_BLUE, PropertyColor.PINK,
            PropertyColor.ORANGE, PropertyColor.RED, PropertyColor.YELLOW,
            PropertyColor.GREEN, PropertyColor.DARK_BLUE
        )

        composeTestRule.setContent {
            Column {
                colors.forEach { color ->
                    FieldItem(
                        index = 35,
                        field = myProperty.copy(color = color),
                        sw = sw,
                        sh = sh
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithTag("Right-Bar")
            .assertCountEquals(8)
    }




    @Test
    fun `verify FieldItem renders GO field text`() {


        val myGO = (GoField(name = "GO", id = 0, type = FieldType.GO))
        composeTestRule.setContent {
            FieldItem(0, myGO, sw, sh)

        }
        composeTestRule
            .onNodeWithText("GO")
            .assertExists()

    }
    @Test
    fun `verify FieldItem renders Chancefield text with index 5`(){
        val myChance = (ChanceField(
            id = 1,
            name = "Chance",
            type = FieldType.CHANCE
        ))


        composeTestRule.setContent {
            FieldItem(5,myChance,sw,sh)

        }
        composeTestRule
            .onNodeWithText("Chance")
            .assertExists()
    }

    @Test
    fun `verify FieldItem for TaxField does not render PropertyBar`() {
        val taxField = TaxField(id = 4, name = "Reichensteuer", type = FieldType.TAX, amount = 200)
        composeTestRule.setContent {
            FieldItem(index = 4, field = taxField, sw = sw, sh = sh)
        }
        // Verifies the "false" branch of the PropertyBar logic
        composeTestRule.onNodeWithTag("Bottom-Bar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("Top-Bar").assertDoesNotExist()
    }

    @Test
    fun `verify FieldItem with unknown name does not crash (null image branch)`() {
        val unknownField = GoField(id = 99, name = "Unknown Place", type = FieldType.GO)
        composeTestRule.setContent {
            FieldItem(index = 0, field = unknownField, sw = sw, sh = sh)
        }
        // Verifies the null-check in FieldImage
        composeTestRule.onNodeWithText("Unknown Place").assertExists()
    }
}
