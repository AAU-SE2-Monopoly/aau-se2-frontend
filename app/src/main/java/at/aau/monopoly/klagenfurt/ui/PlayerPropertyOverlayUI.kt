package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue
import androidx.compose.foundation.layout.systemBarsPadding
//import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PlayerPropertyOverlay(
    player: at.aau.monopoly.klagenfurt.model.Player,
    allFields: List<at.aau.monopoly.klagenfurt.model.field.Field>,
    onDismiss: () -> Unit
) {
    val playerProperties = remember(player.ownedPropertyIds, allFields) {
        allFields
            .filterIsInstance<PropertyField>()
            .filter { field -> player.ownedPropertyIds.contains(field.id) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        // 1. Hintergrund-Ebene: Füllt alles aus, Klick schließt das Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .systemBarsPadding()
        ) {

            // 2. Vertikales Layout: Teilt den Bildschirm hart in "Oben" und "Unten" auf
            Column(modifier = Modifier.fillMaxSize()) {

                // --- KOPFZEILE (Nur für den Button) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onDismiss, // Reagiert jetzt garantiert
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // --- HAUPTBEREICH (Für die Karten, nimmt den restlichen Platz ein) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Füllt den restlichen Bildschirm nach unten aus
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // DIESER Bereich blockiert Klicks, ragt aber nicht mehr zum Button hoch!
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        Text(
                            text = "Besitz von ${player.name}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (playerProperties.isEmpty()) {
                            Text(
                                text = "Dieser Spieler besitzt noch keine kaufbaren Straßen.",
                                color = Color.LightGray,
                                fontSize = 18.sp
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(playerProperties) { property ->
                                    PropertyCardUI(property = property)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}