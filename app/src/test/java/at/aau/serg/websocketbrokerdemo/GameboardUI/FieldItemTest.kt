package at.aau.serg.websocketbrokerdemo.GameboardUI

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.serg.websocketdemoserver.model.enums.FieldType
import at.aau.serg.websocketdemoserver.model.enums.PropertyColor
import at.aau.serg.websocketdemoserver.model.field.GoField
import at.aau.serg.websocketdemoserver.model.field.PropertyField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FieldItemTest {
    val sw = 800f
    val sh = 450f
    val myProperty= PropertyField(
        id=1,
        name = "Heiligengeistplatz",
        type= FieldType.PROPERTY,
        color= PropertyColor.LIGHT_BLUE,
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
    val composeTestRule = createComposeRule()

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
        composeTestRule.setContent {
            FieldItem(index = 35, field = myProperty, sw = sw, sh = sh)
        }
        composeTestRule
            .onNodeWithTag("Right-Bar")
            .assertExists()
    }





    @Test
    fun `verify FieldItem renders GO field text`() {


        val myGO = (GoField(name = "GO", id = 0, type = FieldType.GO))
        composeTestRule.setContent {
            FieldItem(1, myGO, sw, sh)

        }
        composeTestRule
            .onNodeWithText("GO")
            .assertExists()

    }
    @Test
    fun `verify FieldItem renders  Chancefield text with index 1`(){
        val myChance = (GoField(name = "Chance", id = 1, type = FieldType.CHANCE))


    composeTestRule.setContent {
        FieldItem(2,myChance,sw,sh)

    }
    composeTestRule
        .onNodeWithText("Chance")
        .assertExists()
    }
    }

