package at.aau.monopoly.klagenfurt

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.ui.BuildingManagerOverlay
import org.junit.Assert
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
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = { dismissed = true },
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Manage Buildings").assertIsDisplayed()
        composeTestRule.onNodeWithText("✕").assertIsDisplayed()

        composeTestRule.onNodeWithText("✕").performClick()

        Assert.assertEquals(true, dismissed)
    }

    @Test
    fun propertyWithoutBuildingsShowsBuyHouseOnly() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(1, "Alter Platz", houses = 0)),
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Alter Platz: 0 houses").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buy House").assertIsDisplayed()
    }

    @Test
    fun propertyWithHouseShowsSellHouse() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(1, "Alter Platz", houses = 1)),
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Alter Platz: 1 houses").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sell House").assertIsDisplayed()
    }

    @Test
    fun propertyWithFourHousesShowsBuyHotel() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(1, "Alter Platz", houses = 4)),
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Buy Hotel").assertIsDisplayed()
    }

    @Test
    fun propertyWithHotelShowsSellHotel() {
        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(1, "Alter Platz", hasHotel = true)),
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Sell Hotel").assertIsDisplayed()
    }

    @Test
    fun buyHouseClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(5, "Alter Platz")),
                fields = emptyList(),
                onBuyHouse = { clickedId = it },
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Buy House").performClick()

        Assert.assertEquals(5, clickedId)
    }

    @Test
    fun sellHouseClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(6, "Alter Platz", houses = 1)),
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = { clickedId = it },
                onSellHotel = {},
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Sell House").performClick()

        Assert.assertEquals(6, clickedId)
    }

    @Test
    fun buyHotelClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(7, "Alter Platz", houses = 4)),
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = { clickedId = it },
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Buy Hotel").performClick()

        Assert.assertEquals(7, clickedId)
    }

    @Test
    fun sellHotelClickCallsCallback() {
        var clickedId = -1

        composeTestRule.setContent {
            BuildingManagerOverlay(
                properties = listOf(property(8, "Alter Platz", hasHotel = true)),
                fields = emptyList(),
                onBuyHouse = {},
                onBuyHotel = {},
                onSellHouse = {},
                onSellHotel = { clickedId = it },
                onDismiss = {},
                canEndTurn = true,
                isBuyingPhase = true
            )
        }

        composeTestRule.onNodeWithText("Sell Hotel").performClick()

        Assert.assertEquals(8, clickedId)
    }
}