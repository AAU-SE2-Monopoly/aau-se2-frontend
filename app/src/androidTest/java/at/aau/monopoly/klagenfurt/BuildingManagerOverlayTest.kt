package at.aau.monopoly.klagenfurt

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.ui.BuildingManagerOverlay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BuildingManagerOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun property(
        id: Int,
        name: String,
        houses: Int = 0,
        hasHotel: Boolean = false
    ) = PropertyField(
        id = id,
        name = name,
        color = PropertyColor.BROWN,
        price = 60,
        rent = listOf(2, 10, 30, 90, 160, 250),
        houseCost = 50,
        hotelCost = 50,
        ownerId = "player1",
        houses = houses,
        hasHotel = hasHotel
    )

    @Test
    fun overlayDisplaysTitleAndCloseButton() {
        var dismissed = false

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(1, "Alter Platz")),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = { dismissed = true },
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("🏗️ Manage Buildings")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("✕")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("✕")
            .performClick()

        assertEquals(true, dismissed)
    }

    @Test
    fun propertyWithoutBuildingsShowsBuyHouseOnly() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(1, "Alter Platz")),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Alter Platz")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Buy 🏠")
            .assertIsDisplayed()
    }

    @Test
    fun propertyWithHouseShowsSellHouse() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(
                    property(
                        id = 1,
                        name = "Alter Platz",
                        houses = 1
                    )
                ),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Sell 🏠")
            .assertIsDisplayed()
    }

    @Test
    fun propertyWithFourHousesShowsBuyHotel() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(
                    property(
                        id = 1,
                        name = "Alter Platz",
                        houses = 4
                    )
                ),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Buy 🏨")
            .assertIsDisplayed()
    }

    @Test
    fun propertyWithHotelShowsSellHotel() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(
                    property(
                        id = 1,
                        name = "Alter Platz",
                        hasHotel = true
                    )
                ),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Sell 🏨")
            .assertIsDisplayed()
    }

    @Test
    fun buyHouseClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(5, "Alter Platz")),
                onBuyHouse = { clickedId = it },
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Buy 🏠")
            .performClick()

        assertEquals(5, clickedId)
    }

    @Test
    fun sellHouseClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(
                    property(
                        id = 6,
                        name = "Alter Platz",
                        houses = 1
                    )
                ),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = { clickedId = it },
                onSellHotel = {},
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Sell 🏠")
            .performClick()

        assertEquals(6, clickedId)
    }

    @Test
    fun buyHotelClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(
                    property(
                        id = 7,
                        name = "Alter Platz",
                        houses = 4
                    )
                ),
                onBuyHouse = {},
                onBuyHotel = { clickedId = it },
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Buy 🏨")
            .performClick()

        assertEquals(7, clickedId)
    }

    @Test
    fun sellHotelClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(
                    property(
                        id = 8,
                        name = "Alter Platz",
                        hasHotel = true
                    )
                ),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = { clickedId = it },
                onDismiss = {},
                isBuildingActionPending = false
            )
        }

        composeTestRule
            .onNodeWithText("Sell 🏨")
            .performClick()

        assertEquals(8, clickedId)
    }
}