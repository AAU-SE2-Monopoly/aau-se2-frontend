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
class PayRentOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderOverlay(
        isVisible: Boolean = true,
        rentAmount: Int = 200,
        ownerName: String? = "Bob",
        fieldName: String = "Herrengasse",
        currentMoney: Int = 500,
        canPay: Boolean = true,
        canRaiseFunds: Boolean = true,
        paymentInFlight: Boolean = false,
        propertyInFlight: Boolean = false,
        onPay: () -> Unit = {},
        onManageProperties: () -> Unit = {},
        onDeclareBankruptcy: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            PayRentOverlay(
                isVisible = isVisible,
                rentAmount = rentAmount,
                ownerName = ownerName,
                fieldName = fieldName,
                currentMoney = currentMoney,
                canPay = canPay,
                canRaiseFunds = canRaiseFunds,
                paymentInFlight = paymentInFlight,
                propertyInFlight = propertyInFlight,
                onPay = onPay,
                onManageProperties = onManageProperties,
                onDeclareBankruptcy = onDeclareBankruptcy,
                onDismiss = onDismiss
            )
        }
    }

    @Test
    fun `overlay not visible when isVisible false`() {
        renderOverlay(isVisible = false)
        composeTestRule.onNodeWithText("RENT DUE").assertDoesNotExist()
    }

    @Test
    fun `overlay shows RENT DUE header`() {
        renderOverlay()
        composeTestRule.onNodeWithText("RENT DUE").assertIsDisplayed()
    }

    @Test
    fun `overlay shows field name`() {
        renderOverlay(fieldName = "Hauptbahnhof")
        composeTestRule.onNodeWithText("Hauptbahnhof").assertIsDisplayed()
    }

    @Test
    fun `overlay shows owner name`() {
        renderOverlay(ownerName = "Alice")
        composeTestRule.onNodeWithText("Owner: Alice").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Unknown when ownerName is null`() {
        renderOverlay(ownerName = null)
        composeTestRule.onNodeWithText("Owner: Unknown").assertIsDisplayed()
    }

    @Test
    fun `overlay shows rent amount`() {
        renderOverlay(rentAmount = 350)
        composeTestRule.onNodeWithText("€350").assertIsDisplayed()
    }

    @Test
    fun `overlay shows current money`() {
        renderOverlay(currentMoney = 1200)
        composeTestRule.onNodeWithText("€1200").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Pay Rent button when canPay`() {
        renderOverlay(canPay = true, paymentInFlight = false)
        composeTestRule.onNodeWithText("Pay Rent").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Insufficient when cannot pay`() {
        renderOverlay(canPay = false, paymentInFlight = false)
        composeTestRule.onNodeWithText("Insufficient").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Processing when paymentInFlight`() {
        renderOverlay(paymentInFlight = true)
        composeTestRule.onNodeWithText("Processing...").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Manage button`() {
        renderOverlay()
        composeTestRule.onNodeWithText("Manage").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Bankrupt button when canRaiseFunds is false`() {
        renderOverlay(canPay = false, canRaiseFunds = false)
        composeTestRule.onNodeWithText("Bankrupt").assertIsDisplayed()
    }

    @Test
    fun `overlay hides Bankrupt button when canRaiseFunds is true`() {
        renderOverlay(canPay = false, canRaiseFunds = true)
        composeTestRule.onNodeWithText("Bankrupt").assertDoesNotExist()
    }

    @Test
    fun `overlay shows Processing on Bankrupt button when paymentInFlight and cannot raise funds`() {
        renderOverlay(canPay = false, canRaiseFunds = false, paymentInFlight = true)
        composeTestRule.onAllNodesWithText("Processing...")[0].assertIsDisplayed()
    }

    @Test
    fun `Pay Rent button calls onPay`() {
        var paid = false
        renderOverlay(canPay = true, onPay = { paid = true })
        composeTestRule.onNodeWithText("Pay Rent").performClick()
        assertTrue(paid)
    }

    @Test
    fun `Manage button calls onManageProperties`() {
        var managed = false
        renderOverlay(onManageProperties = { managed = true })
        composeTestRule.onNodeWithText("Manage").performClick()
        assertTrue(managed)
    }

    @Test
    fun `Bankrupt button calls onDeclareBankruptcy`() {
        var bankrupt = false
        renderOverlay(canPay = false, canRaiseFunds = false, onDeclareBankruptcy = { bankrupt = true })
        composeTestRule.onNodeWithText("Bankrupt").performClick()
        assertTrue(bankrupt)
    }

    @Test
    fun `Back button exists and calls onDismiss`() {
        var dismissed = false
        renderOverlay(onDismiss = { dismissed = true })
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `Back icon has correct content description`() {
        renderOverlay()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Amount to Pay label`() {
        renderOverlay()
        composeTestRule.onNodeWithText("Amount to Pay").assertIsDisplayed()
    }

    @Test
    fun `overlay shows Your Balance label`() {
        renderOverlay()
        composeTestRule.onNodeWithText("Your Balance:").assertIsDisplayed()
    }

    @Test
    fun `note text shows pay rent message when canPay`() {
        renderOverlay(canPay = true)
        composeTestRule.onNodeWithText("Click Pay Rent to pay the rent").assertIsDisplayed()
    }

    @Test
    fun `note text shows manage message when canRaiseFunds but cannot pay`() {
        renderOverlay(canPay = false, canRaiseFunds = true)
        composeTestRule.onNodeWithText("Manage properties to raise cash").assertIsDisplayed()
    }

    @Test
    fun `note text shows bankruptcy message when cannot raise funds`() {
        renderOverlay(canPay = false, canRaiseFunds = false)
        composeTestRule.onNodeWithText("Insufficient total assets — declare bankruptcy", substring = true).assertIsDisplayed()
    }

    @Test
    fun `Pay Rent button disabled when paymentInFlight`() {
        renderOverlay(canPay = true, paymentInFlight = true)
        composeTestRule.onNodeWithText("Processing...").assertIsNotEnabled()
    }

    @Test
    fun `Manage button disabled when propertyInFlight`() {
        renderOverlay(propertyInFlight = true)
        composeTestRule.onNodeWithText("Manage").assertIsNotEnabled()
    }

    @Test
    fun `Manage button enabled when propertyInFlight is false`() {
        renderOverlay(propertyInFlight = false)
        composeTestRule.onNodeWithText("Manage").assertIsEnabled()
    }
}

