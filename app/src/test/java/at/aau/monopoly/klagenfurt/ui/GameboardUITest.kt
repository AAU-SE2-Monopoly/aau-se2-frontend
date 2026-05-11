package at.aau.monopoly.klagenfurt.ui
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.ui.board.calculateFieldBounds
import at.aau.monopoly.klagenfurt.ui.board.getFieldImageMapping
import at.aau.monopoly.klagenfurt.ui.util.getPlayerTokenResource
import at.aau.monopoly.klagenfurt.ui.util.toComposeColor
import at.aau.monopoly.klagenfurt.ui.zoom.ZoomState


import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.field.GoField
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameboardUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<GameboardUI>()

    @Test
    fun `verify GameboardScreen is called in onCreate`() {
        composeTestRule
            .onNodeWithContentDescription("Klagenfurt-Map")
            .assertExists()
    }


    @Test
    fun testToComposeColor() {
        assertEquals(Color(0xFF955436), PropertyColor.BROWN.toComposeColor())
        assertEquals(Color(0xFFAAE0FA), PropertyColor.LIGHT_BLUE.toComposeColor())
        assertEquals(Color(0xFFD93A96), PropertyColor.PINK.toComposeColor())
        assertEquals(Color(0xFFF7941D), PropertyColor.ORANGE.toComposeColor())
        assertEquals(Color(0xFFED1B24), PropertyColor.RED.toComposeColor())
        assertEquals(Color(0xFFFEF200), PropertyColor.YELLOW.toComposeColor())
        assertEquals(Color(0xFF1FB25A), PropertyColor.GREEN.toComposeColor())
        assertEquals(Color(0xFF0072BB), PropertyColor.DARK_BLUE.toComposeColor())
    }

    @Test
    fun testZoomStateLogic() {
        val state = ZoomState()
        val containerSize = Size(1000f, 1000f)

        // Test zoom in
        state.updateTransformation(Offset.Zero, 2f, containerSize)
        assertEquals(2f, state.scale)
        assertEquals(Offset.Zero, state.offset)

        // Test pan within bounds (maxX = 1000 * (2-1) / 2 = 500)
        state.updateTransformation(Offset(100f, 100f), 1f, containerSize)
        assertEquals(100f, state.offset.x)
        assertEquals(100f, state.offset.y)

        // Test pan out of bounds (should coerce)
        state.updateTransformation(Offset(1000f, 1000f), 1f, containerSize)
        assertEquals(500f, state.offset.x)
        assertEquals(500f, state.offset.y)

        // Test reset on zoom out to 1.0
        state.updateTransformation(Offset.Zero, 0.1f, containerSize)
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun testCalculateFieldBoundsCorners() {
        // Full HD scale (design size)
        val sw = 3840f
        val sh = 2160f

        // Corner 0 (Go)
        val b0 = calculateFieldBounds(0, sw, sh)
        assertTrue(b0.isCorner)
        assertEquals(0f, b0.rotation)
        assertEquals(2445f - 120f, b0.x, 0.1f)
        assertEquals(1720f - 120f, b0.y, 0.1f)

        // Corner 10 (Jail)
        val b10 = calculateFieldBounds(10, sw, sh)
        assertEquals(0f, b10.rotation)

        // Corner 20 (Free Parking)
        val b20 = calculateFieldBounds(20, sw, sh)
        assertEquals(0f, b20.rotation)

        // Corner 30 (Go To Jail)
        val b30 = calculateFieldBounds(30, sw, sh)
        assertEquals(0f, b30.rotation)
    }

    @Test
    fun testCalculateFieldBoundsSideCalculations() {
        val sw = 3840f
        val sh = 2160f

        // Side 0 (Bottom) - Field 1
        val b1 = calculateFieldBounds(1, sw, sh)
        assertFalse(b1.isCorner)
        assertEquals(0f, b1.rotation)

        // Side 1 (Left) - Field 11
        val b11 = calculateFieldBounds(11, sw, sh)
        assertEquals(90f, b11.rotation)

        // Side 2 (Top) - Field 21
        val b21 = calculateFieldBounds(21, sw, sh)
        assertEquals(180f, b21.rotation)

        // Side 3 (Right) - Field 31
        val b31 = calculateFieldBounds(31, sw, sh)
        assertEquals(270f, b31.rotation)
    }

    @Test
    fun testScalingIndependence() {
        // Test that bounds scale linearly with screen size
        val b1_large = calculateFieldBounds(1, 3840f, 2160f)
        val b1_small = calculateFieldBounds(1, 1920f, 1080f)

        assertEquals(b1_large.x / 2f, b1_small.x, 0.1f)
        assertEquals(b1_large.width / 2f, b1_small.width, 0.1f)
    }

    @Test
    fun testGetFieldImageMapping() {
        assertEquals(com.example.myapplication.R.drawable.corners_go_field, getFieldImageMapping("Go"))
        assertEquals(com.example.myapplication.R.drawable.herrengasse, getFieldImageMapping("Herrengasse"))
        assertEquals(com.example.myapplication.R.drawable.taxes, getFieldImageMapping("Reichensteuer"))
        assertNull(getFieldImageMapping("NonExistentField"))
        // Test trim
        assertEquals(com.example.myapplication.R.drawable.corners_go_field, getFieldImageMapping(" Go "))
    }

    @Test
    fun testGetPlayerTokenResource() {
        assertEquals(com.example.myapplication.R.drawable.lindwurm, getPlayerTokenResource("lindwurm"))
        assertEquals(com.example.myapplication.R.drawable.woertherseemandl, getPlayerTokenResource("woerthersee"))
        assertEquals(com.example.myapplication.R.drawable.gti, getPlayerTokenResource("gti"))
        assertEquals(com.example.myapplication.R.drawable.ironman, getPlayerTokenResource("ironman"))
        assertEquals(com.example.myapplication.R.drawable.josef, getPlayerTokenResource("josef"))
        assertEquals(com.example.myapplication.R.drawable.lindwurm, getPlayerTokenResource("unknown"))
    }

    @Test
    fun testCalculateFieldBoundsAllSides() {
        val sw = 3840f
        val sh = 2160f

        // Bottom side (0-9)
        val b5 = calculateFieldBounds(5, sw, sh)
        assertEquals(0f, b5.rotation)

        // Left side (10-19)
        val b15 = calculateFieldBounds(15, sw, sh)
        assertEquals(90f, b15.rotation)

        // Top side (20-29)
        val b25 = calculateFieldBounds(25, sw, sh)
        assertEquals(180f, b25.rotation)

        // Right side (30-39)
        val b35 = calculateFieldBounds(35, sw, sh)
        assertEquals(270f, b35.rotation)
    }

    @Test
    fun testOnKeyDown_interceptsVolumeUp() {
        // Hole die laufende Activity-Instanz aus der Compose-Rule
        val activity = composeTestRule.activity

        // 1. Teste die Volume Up Taste (sollte abgefangen werden -> return true)
        val volumeUpEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val resultUp = activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, volumeUpEvent)

        assertTrue("Volume Up sollte abgefangen werden (true)", resultUp)

        // 2. Teste eine andere Taste, z.B. Volume Down (sollte an super weitergereicht werden -> i.d.R. return false)
        val volumeDownEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)
        val resultDown = activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, volumeDownEvent)

        assertFalse("Andere Tasten sollten nicht abgefangen werden (false)", resultDown)
    }

    @Test
    fun testZoomStateDoesNotGoBelowMinimumScale() {
        val state = ZoomState(initialScale = 1f)
        val containerSize = Size(1000f, 1000f)

        state.updateTransformation(
            pan = Offset(200f, 200f),
            zoom = 0.2f,
            containerSize = containerSize
        )

        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun testZoomStateDoesNotGoAboveMaximumScale() {
        val state = ZoomState(initialScale = 4f)
        val containerSize = Size(1000f, 1000f)

        state.updateTransformation(
            pan = Offset.Zero,
            zoom = 10f,
            containerSize = containerSize
        )

        assertEquals(5f, state.scale)
    }

    @Test
    fun testZoomStateNegativePanIsClamped() {
        val state = ZoomState(initialScale = 2f)
        val containerSize = Size(1000f, 1000f)

        state.updateTransformation(
            pan = Offset(-1000f, -1000f),
            zoom = 1f,
            containerSize = containerSize
        )

        assertEquals(-500f, state.offset.x)
        assertEquals(-500f, state.offset.y)
    }

    @Test
    fun testZoomStateInitialValues() {
        val state = ZoomState(
            initialScale = 3f,
            initialOffset = Offset(50f, -40f)
        )

        assertEquals(3f, state.scale)
        assertEquals(50f, state.offset.x)
        assertEquals(-40f, state.offset.y)
    }

    @Test
    fun testPlayerTokenResourceIsCaseInsensitive() {
        assertEquals(
            com.example.myapplication.R.drawable.lindwurm,
            getPlayerTokenResource("LINDWURM")
        )

        assertEquals(
            com.example.myapplication.R.drawable.gti,
            getPlayerTokenResource("GTI")
        )
    }

    @Test
    fun testUnknownPlayerTokenFallsBackToLindwurmForBlankString() {
        assertEquals(
            com.example.myapplication.R.drawable.lindwurm,
            getPlayerTokenResource("")
        )

        assertEquals(
            com.example.myapplication.R.drawable.lindwurm,
            getPlayerTokenResource("   ")
        )
    }


    @Test
    fun testCalculateFieldBoundsTextSizeForCorner() {
        val bounds = calculateFieldBounds(0, 3840f, 2160f)

        assertTrue(bounds.isCorner)
        assertEquals(bounds.width, bounds.textWidth, 0.1f)
        assertEquals(bounds.height, bounds.textHeight, 0.1f)
    }

    @Test
    fun testCalculateFieldBoundsTextSizeForHorizontalField() {
        val bounds = calculateFieldBounds(1, 3840f, 2160f)

        assertFalse(bounds.isCorner)
        assertEquals(bounds.width, bounds.textWidth, 0.1f)
        assertEquals(bounds.height, bounds.textHeight, 0.1f)
    }

    @Test
    fun testCalculateFieldBoundsTextSizeForVerticalField() {
        val bounds = calculateFieldBounds(11, 3840f, 2160f)

        assertFalse(bounds.isCorner)
        assertEquals(bounds.height, bounds.textWidth, 0.1f)
        assertEquals(bounds.width, bounds.textHeight, 0.1f)
    }

    @Test
    fun testCalculateFieldBoundsForLastField() {
        val bounds = calculateFieldBounds(39, 3840f, 2160f)

        assertFalse(bounds.isCorner)
        assertEquals(270f, bounds.rotation)
        assertTrue(bounds.width > 0f)
        assertTrue(bounds.height > 0f)
    }

     @Test
     fun testCalculateFieldBoundsForEveryBoardIndexHasPositiveSize() {
         for (index in 0 until 40) {
             val bounds = calculateFieldBounds(index, 3840f, 2160f)

             assertTrue("width should be positive for index $index", bounds.width > 0f)
             assertTrue("height should be positive for index $index", bounds.height > 0f)
             assertTrue("textWidth should be positive for index $index", bounds.textWidth > 0f)
             assertTrue("textHeight should be positive for index $index", bounds.textHeight > 0f)
         }
     }




     @Test
     fun testGameboardUICreationWithGameId() {
         val activity = composeTestRule.activity
         assertNotNull("Activity should be created", activity)
     }



     @Test
     fun testZoomStatePanningPreservesScaleLevel() {
         val state = ZoomState()
         val containerSize = Size(1000f, 1000f)

         // Zoom to 2x
         state.updateTransformation(Offset.Zero, 2f, containerSize)
         val initialScale = state.scale

         // Pan without changing zoom
         state.updateTransformation(Offset(100f, 100f), 1f, containerSize)

         // Scale should remain unchanged
         assertEquals(initialScale, state.scale)
         assertEquals(100f, state.offset.x)
         assertEquals(100f, state.offset.y)
     }

     @Test
     fun testCalculateFieldBoundsBottomSide() {
         val bounds = calculateFieldBounds(3, 3840f, 2160f)
         assertEquals(0f, bounds.rotation)
         assertTrue("Should be on bottom side", bounds.x > 0f && bounds.x < 3840f)
     }

     @Test
     fun testCalculateFieldBoundsTopSide() {
         val bounds = calculateFieldBounds(23, 3840f, 2160f)
         assertEquals(180f, bounds.rotation)
     }

     @Test
     fun testCalculateFieldBoundsRightSide() {
         val bounds = calculateFieldBounds(33, 3840f, 2160f)
         assertEquals(270f, bounds.rotation)
     }

     @Test
     fun testCalculateFieldBoundsLeftSide() {
         val bounds = calculateFieldBounds(13, 3840f, 2160f)
         assertEquals(90f, bounds.rotation)
     }

     @Test
     fun testGetFieldImageMappingReturnsNullForNonExistent() {
         assertNull(getFieldImageMapping("ThisFieldDoesNotExist123"))
         assertNull(getFieldImageMapping(""))
     }

     @Test
     fun testGetPlayerTokenResourceHandlesWhitespace() {
         val resourceWithSpaces = getPlayerTokenResource("  lindwurm  ")
         val resourceWithoutSpaces = getPlayerTokenResource("lindwurm")
         assertEquals(resourceWithoutSpaces, resourceWithSpaces)
     }



     @Test
     fun testCalculateFieldBoundsScalingEffectsAllFields() {
         val sw1 = 1920f
         val sh1 = 1080f
         val sw2 = 3840f
         val sh2 = 2160f

         val bounds1 = calculateFieldBounds(1, sw1, sh1)
         val bounds2 = calculateFieldBounds(1, sw2, sh2)

         // Double resolution should result in double size
         assertEquals(bounds1.x * 2f, bounds2.x, 0.1f)
         assertEquals(bounds1.y * 2f, bounds2.y, 0.1f)
     }

     @Test
     fun testFieldImageMappingIsConsistent() {
         val mapping1 = getFieldImageMapping("Go")
         val mapping2 = getFieldImageMapping("Go")
         assertEquals(mapping1, mapping2)
     }

      @Test
      fun testPlayerTokenResourceIsConsistent() {
          val token1 = getPlayerTokenResource("lindwurm")
          val token2 = getPlayerTokenResource("lindwurm")
          assertEquals(token1, token2)
      }

      @Test
      fun testCalculateFieldBoundsFieldsAreInBoardBounds() {
          for (index in 0 until 40) {
              val bounds = calculateFieldBounds(index, 3840f, 2160f)

              assertTrue("Field $index x position should be within bounds", bounds.x >= 0f && bounds.x <= 3840f)
              assertTrue("Field $index y position should be within bounds", bounds.y >= 0f && bounds.y <= 2160f)
          }
      }



      @Test
      fun testPlayerTokenResourceHandlesAllTokenTypes() {
          val tokenTypes = listOf("lindwurm", "woerthersee", "gti", "ironman", "josef")
          for (type in tokenTypes) {
              val token = getPlayerTokenResource(type)
              assertTrue("Token resource should exist for $type", token > 0)
          }
      }



      @Test
      fun testCalculateFieldBoundsCornerFieldsHaveConsistentSize() {
          val corner0 = calculateFieldBounds(0, 3840f, 2160f)
          val corner10 = calculateFieldBounds(10, 3840f, 2160f)
          val corner20 = calculateFieldBounds(20, 3840f, 2160f)
          val corner30 = calculateFieldBounds(30, 3840f, 2160f)

          // All corners should be marked as corners
          assertTrue(corner0.isCorner)
          assertTrue(corner10.isCorner)
          assertTrue(corner20.isCorner)
          assertTrue(corner30.isCorner)

          // Corner rotations should be 0 (aligned with board)
          assertEquals(0f, corner0.rotation)
          assertEquals(0f, corner10.rotation)
          assertEquals(0f, corner20.rotation)
          assertEquals(0f, corner30.rotation)
      }



      @Test
      fun testGetFieldImageMappingAllFields() {
          // Test that we can map several important fields
          val fieldNames = listOf(
              "Go", "Herrengasse", "Heiligengeistplatz", "Neuer Platz",
              "Hauptbahnhof", "Chance", "Community Chest"
          )

          for (fieldName in fieldNames) {
              val hasMapping = getFieldImageMapping(fieldName) != null
              assertTrue("Field $fieldName should have a mapping", hasMapping)
          }
      }

      @Test
      fun testPlayerTokenResourceFallbackBehavior() {
          // Test that unknown tokens fall back to default
          val unknownToken = getPlayerTokenResource("unknown_token_xyz")
          val defaultToken = getPlayerTokenResource("lindwurm")
          assertEquals("Unknown tokens should fall back to default", unknownToken, defaultToken)
      }

      @Test
      fun testCalculateFieldBoundsResolutionIndependence() {
          // Test that calculations scale properly with different resolutions
          val smallResolution = calculateFieldBounds(5, 1920f, 1080f)
          val largeResolution = calculateFieldBounds(5, 3840f, 2160f)

          // Large resolution should be exactly 2x of small resolution
          assertEquals(smallResolution.x * 2f, largeResolution.x, 0.1f)
          assertEquals(smallResolution.y * 2f, largeResolution.y, 0.1f)
          assertEquals(smallResolution.width * 2f, largeResolution.width, 0.1f)
          assertEquals(smallResolution.height * 2f, largeResolution.height, 0.1f)
      }

      @Test
      fun testPropertyColorAllColorsMap() {
          val colors = listOf(
              PropertyColor.BROWN,
              PropertyColor.LIGHT_BLUE,
              PropertyColor.PINK,
              PropertyColor.ORANGE,
              PropertyColor.RED,
              PropertyColor.YELLOW,
              PropertyColor.GREEN,
              PropertyColor.DARK_BLUE
          )

          for (color in colors) {
              val composeColor = color.toComposeColor()
              assertNotNull("Color $color should map to a Compose color", composeColor)
          }
      }

      @Test
      fun testZoomStateResetOnZoomOut() {
          val state = ZoomState(initialScale = 2f, initialOffset = Offset(100f, 100f))
          val containerSize = Size(1000f, 1000f)

          // Should reset offset when zooming back to 1.0
          state.updateTransformation(Offset(50f, 50f), 0.5f, containerSize)
          assertEquals(1f, state.scale)
          assertEquals(Offset.Zero, state.offset)
      }

    @Test
    fun testGameboardUIActivityIsLandscapeLocked() {
        val activity = composeTestRule.activity

        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            activity.requestedOrientation
        )
    }

    @Test
    fun testGameboardUIShowsBackgroundAndPathImages() {
        composeTestRule
            .onNodeWithContentDescription("Klagenfurt-Map")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription("Path - Klagenfurt-Ring")
            .assertExists()
    }

}


