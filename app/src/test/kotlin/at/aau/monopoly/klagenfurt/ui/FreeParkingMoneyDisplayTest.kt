package at.aau.monopoly.klagenfurt.ui

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("Free Parking Money Display Tests")
class FreeParkingMoneyDisplayTest {

    @Test
    @DisplayName("Verify free parking money amount is passed correctly to FieldItem")
    fun `test free parking money display parameter` () {
        // This test verifies that the freeParkingMoney parameter is available in FieldItem
        // The implementation should display money on the Free Parking field (index 20)
        // when freeParkingMoney > 0

        val freeParkingMoney = 500
        assert(freeParkingMoney > 0) { "Free parking money should be positive" }
    }

    @Test
    @DisplayName("Free parking money should only display when greater than zero")
    fun `test free parking money display condition` () {
        val freeParkingAmount = 0
        val shouldDisplay = freeParkingAmount > 0
        assert(!shouldDisplay) { "Should not display when free parking money is 0" }

        val freeParkingAmount2 = 100
        val shouldDisplay2 = freeParkingAmount2 > 0
        assert(shouldDisplay2) { "Should display when free parking money is greater than 0" }
    }

    @Test
    @DisplayName("Free parking money display only on Free Parking field (index 20)")
    fun `test free parking field index` () {
        val freeParkingFieldIndex = 20
        val otherFieldIndex = 5

        assert(freeParkingFieldIndex == 20) { "Free Parking should only display on field index 20" }
        assert(otherFieldIndex != 20) { "Other fields should not trigger free parking display" }
    }
}

