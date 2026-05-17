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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.Player
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
import at.aau.monopoly.klagenfurt.model.field.UtilityField
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
    fun `player token sizes shrink only when multiple players share a field`() {
        assertEquals(8.dp, nonCornerPlayerTokenSize(1))
        assertEquals(7.dp, nonCornerPlayerTokenSize(2))
        assertEquals(6.25.dp, nonCornerPlayerTokenSize(3))
        assertEquals(5.5.dp, nonCornerPlayerTokenSize(4))
        assertEquals(5.dp, nonCornerPlayerTokenSize(5))
        assertEquals(5.dp, nonCornerPlayerTokenSize(6))

        assertEquals(11.dp, cornerPlayerTokenSize(1))
        assertEquals(10.dp, cornerPlayerTokenSize(2))
        assertEquals(9.dp, cornerPlayerTokenSize(3))
        assertEquals(8.dp, cornerPlayerTokenSize(4))
        assertEquals(7.dp, cornerPlayerTokenSize(5))
        assertEquals(7.dp, cornerPlayerTokenSize(6))
    }

    @Test
    fun `non corner token container renders up to five players before overflow`() {
        val players = (1..6).map {
            Player(id = "p$it", name = "Player $it", iconId = "lindwurm", position = 1)
        }

        composeTestRule.setContent {
            Box {
                FieldItem(
                    index = 1,
                    field = PropertyField(
                        id = 1,
                        name = "Benediktiner Platz",
                        color = PropertyColor.LIGHT_BLUE,
                        price = 60,
                        rent = listOf(2, 4, 8, 16, 32, 64),
                        houseCost = 50,
                        hotelCost = 50
                    ),
                    sw = 3840f,
                    sh = 2160f,
                    playersOnField = players
                )
            }
        }

        composeTestRule.onAllNodesWithTag("MiniPlayerToken").assertCountEquals(5)
        composeTestRule.onNodeWithText("+1").assertExists()
    }

    @Test
    fun `vertical non corner token container renders five players without overflow indicator`() {
        val players = (1..5).map {
            Player(id = "p$it", name = "Player $it", iconId = "lindwurm", position = 11)
        }

        composeTestRule.setContent {
            Box {
                FieldItem(
                    index = 11,
                    field = PropertyField(
                        id = 11,
                        name = "Strandbad",
                        color = PropertyColor.GREEN,
                        price = 220,
                        rent = listOf(18, 90, 250, 700, 875, 1050),
                        houseCost = 150,
                        hotelCost = 150
                    ),
                    sw = 3840f,
                    sh = 2160f,
                    playersOnField = players
                )
            }
        }

        composeTestRule.onAllNodesWithTag("MiniPlayerToken").assertCountEquals(5)
        composeTestRule.onAllNodesWithText("+1").assertCountEquals(0)
    }

    @Test
    fun `top and right token containers render players on matching board sides`() {
        val topPlayers = listOf(
            Player(id = "top-1", name = "Top Player 1", iconId = "lindwurm", position = 21),
            Player(id = "top-2", name = "Top Player 2", iconId = "gti", position = 21)
        )
        val rightPlayers = listOf(
            Player(id = "right-1", name = "Right Player 1", iconId = "ironman", position = 31),
            Player(id = "right-2", name = "Right Player 2", iconId = "josef", position = 31)
        )

        composeTestRule.setContent {
            Column {
                FieldItem(
                    index = 21,
                    field = PropertyField(
                        id = 21,
                        name = "City Arkaden",
                        color = PropertyColor.RED,
                        price = 260,
                        rent = listOf(22, 110, 330, 800, 975, 1150),
                        houseCost = 150,
                        hotelCost = 150
                    ),
                    sw = 3840f,
                    sh = 2160f,
                    playersOnField = topPlayers,
                    animatingPlayerId = "not-on-field",
                    animatingStep = 21,
                    animationComplete = false
                )
                FieldItem(
                    index = 31,
                    field = PropertyField(
                        id = 31,
                        name = "Loretto",
                        color = PropertyColor.DARK_BLUE,
                        price = 350,
                        rent = listOf(35, 175, 500, 1100, 1300, 1500),
                        houseCost = 200,
                        hotelCost = 200
                    ),
                    sw = 3840f,
                    sh = 2160f,
                    playersOnField = rightPlayers,
                    animatingPlayerId = null,
                    animatingStep = null,
                    animationComplete = true
                )
            }
        }

        composeTestRule.onAllNodesWithTag("PlayerContainer-Top").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("PlayerContainer-Right").assertCountEquals(1)
        composeTestRule.onNodeWithContentDescription("Top Player 1").assertExists()
        composeTestRule.onNodeWithContentDescription("Right Player 2").assertExists()
    }

    @Test
    fun `corner token container renders up to six players before overflow`() {
        val players = (1..7).map {
            Player(id = "corner-$it", name = "Corner Player $it", iconId = "lindwurm", position = 20)
        }

        composeTestRule.setContent {
            Box {
                FieldItem(
                    index = 20,
                    field = FreeParkingField(),
                    sw = 3840f,
                    sh = 2160f,
                    playersOnField = players,
                    animatingPlayerId = "not-on-field",
                    animatingStep = 20,
                    animationComplete = false
                )
            }
        }

        composeTestRule.onAllNodesWithTag("MiniPlayerToken").assertCountEquals(6)
        composeTestRule.onNodeWithText("+1").assertExists()
    }

    @Test
    fun `highlighted non corner and corner player containers render while animation clock is paused`() {
        val nonCornerPlayer = Player(id = "moving-1", name = "Moving One", iconId = "lindwurm", position = 1)
        val cornerPlayer = Player(id = "moving-2", name = "Moving Two", iconId = "gti", position = 0)

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.setContent {
                Column {
                    FieldItem(
                        index = 1,
                        field = PropertyField(
                            id = 1,
                            name = "Benediktiner Platz",
                            color = PropertyColor.LIGHT_BLUE,
                            price = 60,
                            rent = listOf(2, 4, 8, 16, 32, 64),
                            houseCost = 50,
                            hotelCost = 50
                        ),
                        sw = 3840f,
                        sh = 2160f,
                        playersOnField = listOf(nonCornerPlayer),
                        animatingPlayerId = nonCornerPlayer.id,
                        animatingStep = 1,
                        animationComplete = false
                    )
                    FieldItem(
                        index = 0,
                        field = GoField(),
                        sw = 3840f,
                        sh = 2160f,
                        playersOnField = listOf(cornerPlayer),
                        animatingPlayerId = cornerPlayer.id,
                        animatingStep = 0,
                        animationComplete = false
                    )
                }
            }

            composeTestRule.onNodeWithContentDescription("Moving One").assertExists()
            composeTestRule.onNodeWithContentDescription("Moving Two").assertExists()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `legacy PlayerToken composable positions token from player position`() {
        composeTestRule.setContent {
            Box {
                PlayerToken(
                    player = Player(id = "p1", name = "Legacy Alice", iconId = "lindwurm", position = 5),
                    playerIndex = 4,
                    sw = 3840f,
                    sh = 2160f
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Legacy Alice").assertExists()
    }

    @Test
    fun `legacy PlayerToken composable supports very small board dimensions`() {
        composeTestRule.setContent {
            Box {
                PlayerToken(
                    player = Player(id = "p1", name = "Tiny Alice", iconId = "lindwurm", position = 15),
                    playerIndex = 1,
                    sw = 300f,
                    sh = 160f
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Tiny Alice").assertExists()
    }

    @Test
    fun `field item renders gracefully when no field image mapping exists`() {
        val customField = object : at.aau.monopoly.klagenfurt.model.field.Field(
            id = 99,
            name = "Custom Without Image",
            type = FieldType.PROPERTY
        ) {}

        composeTestRule.setContent {
            Box {
                FieldItem(
                    index = 9,
                    field = customField,
                    sw = 3840f,
                    sh = 2160f
                )
            }
        }

        composeTestRule.onNodeWithText("Custom Without Image").assertExists()
    }

    @Test
    fun `FieldTitle no ops for non corner bounds`() {
        val bounds = FieldBounds(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 40f,
            rotation = 0f,
            isCorner = false,
            textWidth = 100f,
            textHeight = 40f
        )

        composeTestRule.setContent {
            Box {
                FieldTitle(
                    index = 1,
                    side = 0,
                    field = PropertyField(
                        id = 1,
                        name = "Benediktiner Platz",
                        color = PropertyColor.LIGHT_BLUE,
                        price = 60,
                        rent = listOf(2, 4, 8, 16, 32, 64),
                        houseCost = 50,
                        hotelCost = 50
                    ),
                    bounds = bounds
                )
            }
        }

        composeTestRule.onAllNodesWithText("Benediktiner\nPlatz").assertCountEquals(0)
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
                FieldItem(
                    index = 12,
                    field = UtilityField(
                        id = 12,
                        name = "Stadtwerke Klagenfurt",
                        ownerId = "owner-3",
                        isMortgaged = true
                    ),
                    sw = 800f,
                    sh = 450f
                )
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
        composeTestRule.onNodeWithText("Stadtwerke\nKlagenfurt").assertExists()
        composeTestRule.onAllNodesWithTag("OwnerIndicator").assertCountEquals(3)
        composeTestRule.onAllNodesWithText("MORTGAGED").assertCountEquals(3)
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

    @Test
    fun `owner indicator renders on all four sides`() {
        // Renders owned properties on bottom (side 0), left (side 1), top (side 2), right (side 3)
        // to exercise the OwnerIndicator offset branches
        composeTestRule.setContent {
            Column {
                // side 0 (bottom): index 1-9
                FieldItem(
                    index = 3,
                    field = PropertyField(
                        id = 3, name = "Side0 Prop", color = PropertyColor.BROWN,
                        price = 60, rent = listOf(2, 4, 8, 16, 32, 64),
                        houseCost = 50, hotelCost = 50, ownerId = "o1"
                    ),
                    sw = 3840f, sh = 2160f
                )
                // side 1 (left): index 11-19
                FieldItem(
                    index = 13,
                    field = PropertyField(
                        id = 13, name = "Side1 Prop", color = PropertyColor.ORANGE,
                        price = 180, rent = listOf(14, 70, 200, 550, 750, 950),
                        houseCost = 100, hotelCost = 100, ownerId = "o2"
                    ),
                    sw = 3840f, sh = 2160f
                )
                // side 2 (top): index 21-29
                FieldItem(
                    index = 23,
                    field = PropertyField(
                        id = 23, name = "Side2 Prop", color = PropertyColor.RED,
                        price = 260, rent = listOf(22, 110, 330, 800, 975, 1150),
                        houseCost = 150, hotelCost = 150, ownerId = "o3"
                    ),
                    sw = 3840f, sh = 2160f
                )
                // side 3 (right): index 31-39
                FieldItem(
                    index = 33,
                    field = PropertyField(
                        id = 33, name = "Side3 Prop", color = PropertyColor.DARK_BLUE,
                        price = 350, rent = listOf(35, 175, 500, 1100, 1300, 1500),
                        houseCost = 200, hotelCost = 200, ownerId = "o4"
                    ),
                    sw = 3840f, sh = 2160f
                )
            }
        }

        composeTestRule.onAllNodesWithTag("OwnerIndicator").assertCountEquals(4)
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
