package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MortgageManagementOverlayComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun prop(
        id: Int = 1, name: String = "TestProp", color: String? = "brown",
        isMortgaged: Boolean = false, houses: Int = 0, hasHotel: Boolean = false,
        canBuyHouse: Boolean = false, canBuyHotel: Boolean = false,
        canSellHouse: Boolean = false, canSellHotel: Boolean = false,
        canMortgage: Boolean = true
    ) = ManageableProperty(
        fieldId = id, name = name, color = color,
        price = 100, mortgageValue = 50, unmortgageCost = 55,
        houses = houses, hasHotel = hasHotel, isMortgaged = isMortgaged,
        houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25,
        canBuyHouse = canBuyHouse, canBuyHotel = canBuyHotel,
        canSellHouse = canSellHouse, canSellHotel = canSellHotel,
        canMortgage = canMortgage
    )

    private fun renderContent(
        properties: List<ManageableProperty> = listOf(prop()),
        currentMoney: Int = 500,
        actionInFlight: Boolean = false,
        isPayingRent: Boolean = false,
        onBuyHouse: (Int) -> Unit = {},
        onBuyHotel: (Int) -> Unit = {},
        onMortgage: (Int) -> Unit = {},
        onUnmortgage: (Int) -> Unit = {},
        onSellHouse: (Int) -> Unit = {},
        onSellHotel: (Int) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MortgageManagementContent(
                properties = properties,
                currentMoney = currentMoney,
                actionInFlight = actionInFlight,
                isPayingRent = isPayingRent,
                onBuyHouse = onBuyHouse,
                onBuyHotel = onBuyHotel,
                onMortgage = onMortgage,
                onUnmortgage = onUnmortgage,
                onSellHouse = onSellHouse,
                onSellHotel = onSellHotel,
                onDismiss = onDismiss
            )
        }
    }

    @Test
    fun `shows Back button`() {
        renderContent()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun `Back button calls onDismiss`() {
        var dismissed = false
        renderContent(onDismiss = { dismissed = true })
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `Back icon has correct content description`() {
        renderContent()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `shows balance text`() {
        renderContent(currentMoney = 750)
        composeTestRule.onNodeWithText("Balance: €750").assertIsDisplayed()
    }

    @Test
    fun `shows empty message when no properties`() {
        renderContent(properties = emptyList())
        composeTestRule.onNodeWithText("You don't own any properties").assertIsDisplayed()
    }

    @Test
    fun `shows property card name`() {
        renderContent(properties = listOf(prop(name = "Main Street")))
        composeTestRule.onNodeWithText("Main Street").assertIsDisplayed()
    }

    @Test
    fun `shows property price`() {
        renderContent(properties = listOf(prop(name = "TestProp")))
        composeTestRule.onNodeWithText("€100", substring = true).assertExists()
    }

    @Test
    fun `clicking property card shows action panel`() {
        renderContent(properties = listOf(prop(name = "ClickMe", houses = 0, canMortgage = true)))
        composeTestRule.onNodeWithText("ClickMe").performClick()
        // After click, action panel should show Mortgage option
        composeTestRule.onAllNodesWithText("Mortgage")[1].assertIsDisplayed()
    }

    @Test
    fun `mortgage button click opens confirmation dialog`() {
        renderContent(properties = listOf(prop(name = "MortgageMe", houses = 0, canMortgage = true)))
        composeTestRule.onNodeWithText("MortgageMe").performClick()
        composeTestRule.onAllNodesWithText("Mortgage")[1].performClick()
        // Confirmation dialog should appear
        composeTestRule.onNodeWithText("Mortgage Property").assertIsDisplayed()
        composeTestRule.onNodeWithText("Do you really want to mortgage").assertIsDisplayed()
    }

    @Test
    fun `mortgage confirmation dialog shows property name`() {
        renderContent(properties = listOf(prop(name = "ConfirmProp", houses = 0, canMortgage = true)))
        composeTestRule.onNodeWithText("ConfirmProp").performClick()
        composeTestRule.onAllNodesWithText("Mortgage")[1].performClick()
        composeTestRule.onNodeWithText("ConfirmProp?").assertIsDisplayed()
    }

    @Test
    fun `mortgage confirmation dialog shows mortgage value`() {
        renderContent(
            properties = listOf(prop(name = "ValueProp", houses = 0, canMortgage = true)),
            currentMoney = 200
        )
        composeTestRule.onNodeWithText("ValueProp").performClick()
        composeTestRule.onAllNodesWithText("Mortgage")[1].performClick()
        composeTestRule.onNodeWithText("You will receive").assertIsDisplayed()
    }

    @Test
    fun `mortgage confirmation dialog shows warning text`() {
        renderContent(properties = listOf(prop(name = "WarnProp", houses = 0, canMortgage = true)))
        composeTestRule.onNodeWithText("WarnProp").performClick()
        composeTestRule.onAllNodesWithText("Mortgage")[1].performClick()
        composeTestRule.onNodeWithText("Rent cannot be collected while mortgaged.").assertIsDisplayed()
    }

    @Test
    fun `mortgage confirmation dialog Back button dismisses dialog`() {
        renderContent(properties = listOf(prop(name = "BackProp", houses = 0, canMortgage = true)))
        composeTestRule.onNodeWithText("BackProp").performClick()
        composeTestRule.onAllNodesWithText("Mortgage")[1].performClick()
        // Click Back in confirmation dialog
        composeTestRule.onAllNodesWithText("Back")[1].performClick()
        // Dialog should be dismissed
        composeTestRule.onNodeWithText("Mortgage Property").assertDoesNotExist()
    }

    @Test
    fun `mortgage confirmation dialog Mortgage button calls onMortgage`() {
        var mortgagedFieldId = -1
        renderContent(
            properties = listOf(prop(id = 42, name = "MortField", houses = 0, canMortgage = true)),
            onMortgage = { mortgagedFieldId = it }
        )
        composeTestRule.onNodeWithText("MortField").performClick()
        composeTestRule.onAllNodesWithText("Mortgage")[1].performClick()
        // Click "Mortgage" in the confirmation dialog (last one)
        composeTestRule.onAllNodesWithText("Mortgage").fetchSemanticsNodes().let { nodes ->
            // The confirm button text is just "Mortgage"
            composeTestRule.onAllNodesWithText("Mortgage")[nodes.lastIndex].performClick()
        }
        assertEquals(42, mortgagedFieldId)
    }

    @Test
    fun `unmortgage button shows for mortgaged property`() {
        renderContent(properties = listOf(prop(name = "MortProp", isMortgaged = true)))
        composeTestRule.onNodeWithText("MortProp").performClick()
        composeTestRule.onNodeWithText("Unmortgage").assertIsDisplayed()
    }

    @Test
    fun `unmortgage button disabled when insufficient funds`() {
        renderContent(
            properties = listOf(prop(name = "PoorProp", isMortgaged = true)),
            currentMoney = 10
        )
        composeTestRule.onNodeWithText("PoorProp").performClick()
        composeTestRule.onNodeWithText("Unmortgage").assertIsNotEnabled()
    }

    @Test
    fun `unmortgage button enabled when sufficient funds`() {
        renderContent(
            properties = listOf(prop(name = "RichProp", isMortgaged = true)),
            currentMoney = 500
        )
        composeTestRule.onNodeWithText("RichProp").performClick()
        composeTestRule.onNodeWithText("Unmortgage").assertIsEnabled()
    }

    @Test
    fun `unmortgage button disabled when isPayingRent`() {
        renderContent(
            properties = listOf(prop(name = "RentProp", isMortgaged = true)),
            currentMoney = 500,
            isPayingRent = true
        )
        composeTestRule.onNodeWithText("RentProp").performClick()
        composeTestRule.onNodeWithText("Unmortgage").assertIsNotEnabled()
    }

    @Test
    fun `shows Need more text when cannot afford unmortgage`() {
        renderContent(
            properties = listOf(prop(name = "NeedProp", isMortgaged = true)),
            currentMoney = 10
        )
        composeTestRule.onNodeWithText("NeedProp").performClick()
        composeTestRule.onNodeWithText("Need €45 more").assertIsDisplayed()
    }

    @Test
    fun `buy house button shows for eligible property`() {
        renderContent(properties = listOf(prop(name = "HouseProp", houses = 1, canBuyHouse = true)))
        composeTestRule.onNodeWithText("HouseProp").performClick()
        composeTestRule.onNodeWithText("Buy House").assertIsDisplayed()
    }

    @Test
    fun `buy house button disabled during isPayingRent`() {
        renderContent(
            properties = listOf(prop(name = "RentHouse", houses = 1, canBuyHouse = true)),
            currentMoney = 500,
            isPayingRent = true
        )
        composeTestRule.onNodeWithText("RentHouse").performClick()
        composeTestRule.onNodeWithText("Buy House").assertIsNotEnabled()
    }

    @Test
    fun `sell house button calls onSellHouse`() {
        var soldFieldId = -1
        renderContent(
            properties = listOf(prop(id = 7, name = "SellProp", houses = 2, canSellHouse = true)),
            onSellHouse = { soldFieldId = it }
        )
        composeTestRule.onNodeWithText("SellProp").performClick()
        composeTestRule.onNodeWithText("Sell House").performClick()
        assertEquals(7, soldFieldId)
    }

    @Test
    fun `sell hotel button shows for hotel property`() {
        renderContent(properties = listOf(prop(name = "HotelProp", hasHotel = true, canSellHotel = true)))
        composeTestRule.onNodeWithText("HotelProp").performClick()
        composeTestRule.onNodeWithText("Sell Hotel").assertIsDisplayed()
    }

    @Test
    fun `property card shows MORTGAGED stamp`() {
        renderContent(properties = listOf(prop(name = "StampProp", isMortgaged = true)))
        composeTestRule.onNodeWithText("MORTGAGED").assertIsDisplayed()
    }

    @Test
    fun `property card shows Hotel building status`() {
        renderContent(properties = listOf(prop(name = "HotelCard", hasHotel = true)))
        composeTestRule.onNodeWithText("🏨 Hotel", substring = true).assertIsDisplayed()
    }

    @Test
    fun `property card shows house count`() {
        renderContent(properties = listOf(prop(name = "HouseCard", houses = 3)))
        composeTestRule.onNodeWithText("🏠×3", substring = true).assertIsDisplayed()
    }

    @Test
    fun `property card shows No buildings for vacant`() {
        renderContent(properties = listOf(prop(name = "VacantCard", houses = 0, hasHotel = false)))
        composeTestRule.onNodeWithText("No buildings").assertIsDisplayed()
    }

    @Test
    fun `selected property shows SELECTED indicator`() {
        renderContent(properties = listOf(prop(name = "SelectProp")))
        composeTestRule.onNodeWithText("SelectProp").performClick()
        composeTestRule.onNodeWithText("▼ SELECTED ▼", substring = true).assertIsDisplayed()
    }

    @Test
    fun `unselected property shows Tap to manage`() {
        renderContent(properties = listOf(prop(name = "TapProp")))
        composeTestRule.onNodeWithText("Tap to manage").assertIsDisplayed()
    }

    @Test
    fun `action panel shows property name and Owned status`() {
        renderContent(properties = listOf(prop(name = "OwnedProp", houses = 0, canMortgage = true)))
        composeTestRule.onNodeWithText("OwnedProp").performClick()
        composeTestRule.onNodeWithText("Owned").assertIsDisplayed()
    }

    @Test
    fun `action panel shows MORTGAGED status for mortgaged property`() {
        renderContent(properties = listOf(prop(name = "MortStatus", isMortgaged = true)))
        composeTestRule.onNodeWithText("MortStatus").performClick()
        // Check both the stamp and the status text in the action panel
        composeTestRule.onAllNodesWithText("MORTGAGED")[0].assertIsDisplayed()
    }

    @Test
    fun `buy hotel button shows for 4 house property`() {
        renderContent(properties = listOf(prop(name = "Hotel4", houses = 4, canBuyHotel = true)))
        composeTestRule.onNodeWithText("Hotel4").performClick()
        composeTestRule.onNodeWithText("Buy Hotel").assertIsDisplayed()
    }

    @Test
    fun `buy hotel button disabled during isPayingRent`() {
        renderContent(
            properties = listOf(prop(name = "RentHotel", houses = 4, canBuyHotel = true)),
            currentMoney = 500,
            isPayingRent = true
        )
        composeTestRule.onNodeWithText("RentHotel").performClick()
        composeTestRule.onNodeWithText("Buy Hotel").assertIsNotEnabled()
    }

    @Test
    fun `action panel shows house count for property with houses`() {
        renderContent(properties = listOf(prop(name = "House2", houses = 2, canSellHouse = true)))
        composeTestRule.onNodeWithText("House2").performClick()
        composeTestRule.onNodeWithText("2 Houses").assertIsDisplayed()
    }

    @Test
    fun `action panel shows Hotel for property with hotel`() {
        renderContent(properties = listOf(prop(name = "HotelAct", hasHotel = true, canSellHotel = true)))
        composeTestRule.onNodeWithText("HotelAct").performClick()
        composeTestRule.onNodeWithText("Hotel").assertIsDisplayed()
    }
}


