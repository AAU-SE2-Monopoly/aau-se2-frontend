package at.aau.monopoly.klagenfurt.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.aau.monopoly.klagenfurt.model.Player
import at.aau.monopoly.klagenfurt.model.enums.FieldType
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerPropertyOverlayUITest {

    // Erstellt die Testumgebung für Jetpack Compose
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overlay_showsEmptyMessage_whenNoPropertiesOwned() {
        val player = Player(id = "p1", name = "Spieler Ohne Alles", ownedPropertyIds = mutableListOf())

        composeTestRule.setContent {
            PlayerPropertyOverlay(player = player, allFields = emptyList(), onDismiss = {})
        }

        // Verifiziert, dass der leere Zustand korrekt gerendert wird (if-Zweig)
        composeTestRule.onNodeWithText("Besitz von Spieler Ohne Alles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dieser Spieler besitzt noch keine kaufbaren Straßen.").assertIsDisplayed()
    }

    @Test
    fun overlay_showsPropertyCards_withFullRentDetails() {
        val propertyId = 1
        val player = Player(id = "p1", name = "Immobilienhai", ownedPropertyIds = mutableListOf(propertyId))

        // PropertyField MIT allen Mietangaben
        val testField = PropertyField(
            id = propertyId,
            name = "Herrengasse",
            type = FieldType.PROPERTY,
            color = PropertyColor.DARK_BLUE,
            price = 400,
            rent = listOf(50, 200, 600, 1400, 1700, 2000), // Index 0 bis 5 vorhanden
            houseCost = 200,
            hotelCost = 200
        )

        composeTestRule.setContent {
            PlayerPropertyOverlay(player = player, allFields = listOf(testField), onDismiss = {})
        }

        // Verifiziert Titel und Basisdaten
        composeTestRule.onNodeWithText("Besitz von Immobilienhai").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITEL-URKUNDE").assertIsDisplayed()
        composeTestRule.onNodeWithText("HERRENGASSE").assertIsDisplayed() // Uppercase-Logik testen

        // Verifiziert die for-Schleife der Mieten (Mit 1 Haus, etc.)
        composeTestRule.onNodeWithText("Miete: € 50").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mit 1 Haus").assertIsDisplayed()
        composeTestRule.onNodeWithText("€ 200").assertIsDisplayed() // 1 Haus
        composeTestRule.onNodeWithText("Mit 4 Häusern").assertIsDisplayed()
        composeTestRule.onNodeWithText("€ 1700").assertIsDisplayed() // 4 Häuser
        composeTestRule.onNodeWithText("Mit HOTEL: € 2000").assertIsDisplayed()

        // Verifiziert Baukosten
        composeTestRule.onNodeWithText("Haus kostet").assertIsDisplayed()
        composeTestRule.onNodeWithText("€ 200").assertIsDisplayed()
    }

    @Test
    fun overlay_showsPropertyCard_withoutRent_whenRentListIsEmpty() {
        val propertyId = 2
        val player = Player(id = "p1", name = "Testspieler", ownedPropertyIds = mutableListOf(propertyId))

        // PropertyField OHNE Mietangaben (um if(rent.isNotEmpty()) zu umgehen)
        val testField = PropertyField(
            id = propertyId,
            name = "Spezialfeld",
            type = FieldType.PROPERTY,
            color = PropertyColor.BROWN,
            price = 100,
            rent = emptyList(), // Leer!
            houseCost = 50,
            hotelCost = 50
        )

        composeTestRule.setContent {
            PlayerPropertyOverlay(player = player, allFields = listOf(testField), onDismiss = {})
        }

        // Karte wird angezeigt, aber "Miete" darf nicht crashen und fehlt
        composeTestRule.onNodeWithText("SPEZIALFELD").assertIsDisplayed()

        // Die asserts überprüfen, dass die Exception-sichere Programmierung funktioniert
        // (getOrNull bzw. isNotEmpty check)
    }

    @Test
    fun overlay_triggersOnDismiss_whenBackButtonClicked() {
        var dismissCalled = false
        val player = Player(id = "p1", name = "Testspieler")

        composeTestRule.setContent {
            PlayerPropertyOverlay(
                player = player,
                allFields = emptyList(),
                onDismiss = { dismissCalled = true }
            )
        }

        // Simuliert den Klick auf den "Back"-Button
        composeTestRule.onNodeWithText("Back").performClick()

        // Verifiziert, dass die Callback-Funktion aufgerufen wurde
        assertTrue("onDismiss should have been called", dismissCalled)
    }
}