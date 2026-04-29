package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.model.field.UtilityField
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
    val playerProperties = remember(player.id, allFields) {
        allFields.filter { field ->
            when (field) {
                is PropertyField -> field.ownerId == player.id || player.ownedPropertyIds.contains(field.id)
                is RailroadField -> field.ownerId == player.id || player.ownedPropertyIds.contains(field.id)
                is UtilityField -> field.ownerId == player.id || player.ownedPropertyIds.contains(field.id)
                else -> false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        // 1. Background layer: fills everything, click dismisses the overlay
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

            // 2. Vertical layout: splits screen into top and bottom
            Column(modifier = Modifier.fillMaxSize()) {

                // --- HEADER (Back button) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
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

                // --- MAIN AREA (Cards, fills remaining space) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Fills the rest of the screen
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // This area blocks clicks from passing through
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        Text(
                            text = "${player.name}'s Properties",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (playerProperties.isEmpty()) {
                            Text(
                                text = "This player does not own any properties yet.",
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
                                    FieldCardUI(field = property)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}