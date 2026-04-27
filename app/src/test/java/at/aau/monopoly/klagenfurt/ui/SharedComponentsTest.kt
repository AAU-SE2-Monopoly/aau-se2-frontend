package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.ui.components.BackButton
import at.aau.monopoly.klagenfurt.ui.components.DarkGradientBackground
import at.aau.monopoly.klagenfurt.ui.components.GradientDirection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SharedComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun backButton_displaysAndClicks() {
        var clicked = false
        composeTestRule.setContent {
            BackButton(onClick = { clicked = true })
        }
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()
        assert(clicked) { "Expected onClick to be called" }
    }

    @Test
    fun darkGradientBackground_verticalRenders() {
        composeTestRule.setContent {
            DarkGradientBackground(gradientDirection = GradientDirection.VERTICAL) {}
        }
        // No crash = success for background rendering
    }

    @Test
    fun darkGradientBackground_horizontalRenders() {
        composeTestRule.setContent {
            DarkGradientBackground(gradientDirection = GradientDirection.HORIZONTAL) {}
        }
    }
}

