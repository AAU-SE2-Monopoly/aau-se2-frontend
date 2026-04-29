package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerPropertyOverlayUITest {

    // Creates the test environment for Jetpack Compose
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overlay_showsEmptyMessage_whenNoPropertiesOwned() {
        val player = Player(id = "p1", name = "Player Without Properties", ownedPropertyIds = mutableListOf())

        composeTestRule.setContent {
            PlayerPropertyOverlay(player = player, allFields = emptyList(), onDismiss = {})
        }

        composeTestRule.onNodeWithText("Player Without Properties's Properties").assertIsDisplayed()
        composeTestRule.onNodeWithText("This player does not own any properties yet.").assertIsDisplayed()
    }

    @Test
    fun overlay_showsPropertyCards_withFullRentDetails() {
        val propertyId = 1
        val player = Player(id = "p1", name = "Property Tycoon", ownedPropertyIds = mutableListOf(propertyId))

        val testField = PropertyField(
            id = propertyId,
            name = "Herrengasse",
            type = FieldType.PROPERTY,
            color = PropertyColor.DARK_BLUE,
            price = 400,
            rent = listOf(50, 200, 600, 1400, 1700, 2000),
            houseCost = 200,
            hotelCost = 200
        )

        composeTestRule.setContent {
            PlayerPropertyOverlay(player = player, allFields = listOf(testField), onDismiss = {})
        }

        composeTestRule.onNodeWithText("Property Tycoon's Properties").assertIsDisplayed()
    }

    @Test
    fun overlay_showsPropertyCard_withoutRent_whenRentListIsEmpty() {
        val propertyId = 2
        val player = Player(id = "p1", name = "Test Player", ownedPropertyIds = mutableListOf(propertyId))

        val testField = PropertyField(
            id = propertyId,
            name = "Spezialfeld",
            type = FieldType.PROPERTY,
            color = PropertyColor.BROWN,
            price = 100,
            rent = emptyList(),
            houseCost = 50,
            hotelCost = 50
        )

        composeTestRule.setContent {
            PlayerPropertyOverlay(player = player, allFields = listOf(testField), onDismiss = {})
        }

        // Card is displayed without crashing
        composeTestRule.onNodeWithText("Test Player's Properties").assertIsDisplayed()
    }

    @Test
    fun overlay_triggersOnDismiss_whenBackButtonClicked() {
        var dismissCalled = false
        val player = Player(id = "p1", name = "Test Player")

        composeTestRule.setContent {
            PlayerPropertyOverlay(
                player = player,
                allFields = emptyList(),
                onDismiss = { dismissCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue("onDismiss should have been called", dismissCalled)
    }
}