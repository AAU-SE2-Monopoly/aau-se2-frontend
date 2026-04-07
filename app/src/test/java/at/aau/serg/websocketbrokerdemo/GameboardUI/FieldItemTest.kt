package at.aau.serg.websocketbrokerdemo.at.aau.serg.websocketbrokerdemo.GameboardUI

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.serg.websocketbrokerdemo.GameboardUI.FieldItem
import at.aau.serg.websocketdemoserver.model.field.Field
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FieldItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**FieldItemTest**/
    @Test
    fun `verify FieldItem renders normal field text`(){
        val mockField = mockk<Field>(relaxed = true)
        every { mockField.name } returns "Los"
        composeTestRule.setContent {
            FieldItem(0,mockField,3840f,2160f)

        }
        composeTestRule
            .onNodeWithText("Los")
            .assertExists()
            .assertIsDisplayed()
    }
}
