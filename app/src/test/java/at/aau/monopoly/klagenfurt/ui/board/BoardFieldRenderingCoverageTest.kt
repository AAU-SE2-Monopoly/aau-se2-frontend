package at.aau.monopoly.klagenfurt.ui.board

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.FreeParkingField
import at.aau.monopoly.klagenfurt.model.field.GoField
import at.aau.monopoly.klagenfurt.model.field.GoToJailField
import at.aau.monopoly.klagenfurt.model.field.JailField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.TaxField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardFieldRenderingCoverageTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `getDisplayFieldName formats all special board names`() {
        val expectedNames = mapOf(
            "Community Chest" to "Community\nChest",
            "Benediktiner Platz" to "Benediktiner\nPlatz",
            "Alter Platz" to "Alter\nPlatz",
            "Neuer Platz" to "Neuer\nPlatz",
            "Hauptbahnhof" to "Haupt-\nbahnhof",
            "Reichensteuer" to "Reichen-\nsteuer",
            "Heiligengeistplatz" to "Heiligen-\ngeistplatz",
            "Universität Klagenfurt" to "Universität\nKlagenfurt",
            "Stadtwerke Klagenfurt" to "Stadtwerke\nKlagenfurt",
            "Botanischer Garten" to "Botanischer\nGarten",
            "Go To Jail" to "Go To\nJail",
            "Jail / Just Visiting" to "Jail / Just\nVisiting",
            "Free Parking" to "Free\nParking",
            "Cine City" to "Cine\nCity",
            "City Arkaden" to "City\nArkaden",
            "Kelag Klagenfurt" to "Kelag\nKlagenfurt",
            "Le Burger" to "Le\nBurger",
            "Lendcafe" to "Lend\nCafe",
            "Mc Mullens" to "Mc\nMullens",
            "Westbahnhof" to "West-\nbahnhof",
            "Lendbahnhof" to "Lend-\nbahnhof"
        )

        expectedNames.forEach { (source, expected) ->
            assertEquals(expected, getDisplayFieldName(source))
        }
        assertEquals("Custom Field", getDisplayFieldName("Custom Field"))
    }

    @Test
    fun `getFieldImageMapping trims lowercases and supports aliases`() {
        assertEquals(com.example.myapplication.R.drawable.corners_go_field, getFieldImageMapping(" Go "))
        assertEquals(com.example.myapplication.R.drawable.chest, getFieldImageMapping("community chest"))
        assertEquals(com.example.myapplication.R.drawable.mc_mullens, getFieldImageMapping("Mc Mullens"))
        assertEquals(com.example.myapplication.R.drawable.mc_mullens, getFieldImageMapping("mcmullens"))
        assertEquals(com.example.myapplication.R.drawable.gotojail, getFieldImageMapping("GO TO JAIL"))
        assertNull(getFieldImageMapping("Unknown"))
    }

    @Test
    fun `calculateFieldBounds exposes correct text dimensions for vertical and horizontal fields`() {
        val horizontal = calculateFieldBounds(index = 1, sw = 3840f, sh = 2160f)
        assertEquals(horizontal.width, horizontal.textWidth, 0.01f)
        assertEquals(horizontal.height, horizontal.textHeight, 0.01f)

        val vertical = calculateFieldBounds(index = 11, sw = 3840f, sh = 2160f)
        assertEquals(vertical.height, vertical.textWidth, 0.01f)
        assertEquals(vertical.width, vertical.textHeight, 0.01f)
    }

    @Test
    fun `private helper branches return expected values`() {
        assertEquals(Alignment.TopStart, invokePrivate("innerCornerAlignment", 0))
        assertEquals(Alignment.TopEnd, invokePrivate("innerCornerAlignment", 10))
        assertEquals(Alignment.BottomEnd, invokePrivate("innerCornerAlignment", 20))
        assertEquals(Alignment.BottomStart, invokePrivate("innerCornerAlignment", 30))
        assertEquals(Alignment.Center, invokePrivate("innerCornerAlignment", 99))

        assertEquals(Alignment.BottomCenter, invokePrivate("cornerTextAlignment", 0))
        assertEquals(Alignment.BottomCenter, invokePrivate("cornerTextAlignment", 10))
        assertEquals(Alignment.TopCenter, invokePrivate("cornerTextAlignment", 20))
        assertEquals(Alignment.TopCenter, invokePrivate("cornerTextAlignment", 30))
        assertEquals(Alignment.BottomCenter, invokePrivate("cornerTextAlignment", 99))

        assertEquals(Alignment.TopStart, invokePrivate("ownerIndicatorAlignment", 0))
        assertEquals(Alignment.BottomStart, invokePrivate("ownerIndicatorAlignment", 1))
        assertEquals(Alignment.BottomEnd, invokePrivate("ownerIndicatorAlignment", 2))
        assertEquals(Alignment.TopEnd, invokePrivate("ownerIndicatorAlignment", 3))
        assertEquals(Alignment.TopStart, invokePrivate("ownerIndicatorAlignment", 99))

        assertNotNull(invokePrivate("outerEdgeTextOffset", 0) as Modifier)
        assertNotNull(invokePrivate("outerEdgeTextOffset", 1) as Modifier)
        assertNotNull(invokePrivate("outerEdgeTextOffset", 2) as Modifier)
        assertNotNull(invokePrivate("outerEdgeTextOffset", 3) as Modifier)
        assertEquals(Modifier, invokePrivate("outerEdgeTextOffset", 99))

        val bounds = FieldBounds(
            x = 1f,
            y = 2f,
            width = 30f,
            height = 10f,
            rotation = 0f,
            isCorner = true,
            textWidth = 30f,
            textHeight = 10f
        )
        assertEquals(30f, invokePrivate("normalContentWidth", bounds) as Float, 0.01f)
        assertEquals(10f, invokePrivate("normalContentHeight", bounds) as Float, 0.01f)
        assertNotNull(invokePrivate("fieldItemContainerMod", bounds, 99) as Modifier)
        assertNotNull(invokePrivate("fieldItemContainerMod", bounds.copy(isCorner = false), 5) as Modifier)
    }

    @Test
    fun `field item renders corner titles standard titles action titles and ownable overlays`() {
        val propertyField = PropertyField(
            id = 1,
            name = "Heiligengeistplatz",
            type = FieldType.PROPERTY,
            color = PropertyColor.LIGHT_BLUE,
            price = 100,
            rent = listOf(2, 4, 8, 16, 32, 64),
            houseCost = 50,
            hotelCost = 50,
            ownerId = "owner-1",
            isMortgaged = true
        )
        val railroadField = RailroadField(
            id = 5,
            name = "Hauptbahnhof",
            ownerId = "owner-2",
            isMortgaged = true
        )

        composeTestRule.setContent {
            Column {
                FieldItem(index = 0, field = GoField(name = "Go"), sw = 800f, sh = 450f)
                FieldItem(index = 10, field = JailField(), sw = 800f, sh = 450f)
                FieldItem(index = 20, field = FreeParkingField(), sw = 800f, sh = 450f)
                FieldItem(index = 30, field = GoToJailField(), sw = 800f, sh = 450f)
                FieldItem(index = 1, field = CommunityChestField(id = 2), sw = 800f, sh = 450f)
                FieldItem(index = 4, field = TaxField(id = 4, name = "Reichensteuer", amount = 200), sw = 800f, sh = 450f)
                FieldItem(index = 5, field = railroadField, sw = 800f, sh = 450f)
                FieldItem(index = 6, field = ChanceField(id = 6), sw = 800f, sh = 450f)
                FieldItem(index = 15, field = propertyField, sw = 800f, sh = 450f)
            }
        }

        composeTestRule.onNodeWithContentDescription("Go").assertExists()
        composeTestRule.onNodeWithContentDescription("Jail / Just Visiting").assertExists()
        composeTestRule.onNodeWithContentDescription("Free Parking").assertExists()
        composeTestRule.onNodeWithContentDescription("Go To Jail").assertExists()
        composeTestRule.onNodeWithContentDescription("Community Chest").assertExists()
        composeTestRule.onNodeWithContentDescription("Hauptbahnhof").assertExists()
        composeTestRule.onNodeWithText("Community\nChest").assertExists()
        composeTestRule.onNodeWithText("Reichen-\nsteuer").assertExists()
        composeTestRule.onNodeWithText("Haupt-\nbahnhof").assertExists()
        composeTestRule.onNodeWithText("Heiligen-\ngeistplatz").assertExists()
        composeTestRule.onAllNodesWithTag("OwnerIndicator").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("MORTGAGED").assertCountEquals(2)
    }

    @Test
    fun `property color bar handles all sides including fallback`() {
        composeTestRule.setContent {
            Column {
                Box { PropertyColorBar(side = 0, sw = 800f, sh = 450f, color = Color.Red) }
                Box { PropertyColorBar(side = 1, sw = 800f, sh = 450f, color = Color.Red) }
                Box { PropertyColorBar(side = 2, sw = 800f, sh = 450f, color = Color.Red) }
                Box { PropertyColorBar(side = 3, sw = 800f, sh = 450f, color = Color.Red) }
                Box { PropertyColorBar(side = 99, sw = 800f, sh = 450f, color = Color.Red) }
            }
        }

        composeTestRule.onAllNodesWithTag("Bottom-Bar").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("Left-Bar").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("Top-Bar").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("Right-Bar").assertCountEquals(1)
    }

    private fun invokePrivate(name: String, vararg args: Any): Any? {
        val owner = Class.forName("at.aau.monopoly.klagenfurt.ui.board.BoardFieldRenderingKt")
        val parameterTypes = args.map {
            when (it) {
                is Int -> Int::class.javaPrimitiveType
                is Float -> Float::class.javaPrimitiveType
                else -> it::class.java
            }
        }.toTypedArray()
        val method = owner.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        return method.invoke(null, *args)
    }
}
