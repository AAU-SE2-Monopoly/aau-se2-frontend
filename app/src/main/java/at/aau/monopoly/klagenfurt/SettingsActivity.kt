package at.aau.monopoly.klagenfurt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.networking.ServerConfig
import at.aau.monopoly.klagenfurt.ui.components.AnimatedScreenScaffold
import at.aau.monopoly.klagenfurt.ui.components.ScreenTitle
import at.aau.monopoly.klagenfurt.ui.theme.MyApplicationTheme
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlue
import at.aau.monopoly.klagenfurt.ui.theme.PrimaryBlueLight

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                SettingsScreen(onBackClicked = { finish() })
            }
        }
    }
}

@Composable
fun SettingsScreen(onBackClicked: () -> Unit) {
    // State für das Popup
    var showCheatDialog by remember { mutableStateOf(false) }

    AnimatedScreenScaffold(onBackClicked = onBackClicked) {
        ScreenTitle(title = "SETTINGS")

        Spacer(modifier = Modifier.height(24.dp))

        SettingsToggleRow(
            label = "Server: ${ServerConfig.displayLabel}",
            checked = ServerConfig.isGlobal,
            onCheckedChange = {
                ServerConfig.isGlobal = it
                ServiceLocator.resetGameService()
                // Disable debug mode when switching to global
                if (it) DebugSettings.isEnabled = false
            },
            testTag = "server_toggle_switch"
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsToggleRow(
            label = "Debug Mode",
            checked = DebugSettings.isEnabled,
            onCheckedChange = { DebugSettings.isEnabled = it },
            enabled = DebugSettings.canEnable
        )

        Text(
            text = "Debug mode is only available on local environment",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.3f),
            thickness = 1.dp,
            color = PrimaryBlueLight.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Cheat Tutorial Button
        Button(
            onClick = { showCheatDialog = true },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Show Cheating Tutorial",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }

    // Das Dialog-Popup
    if (showCheatDialog) {
        AlertDialog(
            onDismissRequest = { showCheatDialog = false },
            containerColor = Color(0xFF16213E), // Angepasst ans Dark-Theme
            title = {
                Text(
                    text = "Cheat Code",
                    color = PrimaryBlueLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Press the Volume Up button during your turn to automatically roll a double 6! But beware: if opponents catch and report you, you'll pay a 500€ fine!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showCheatDialog = false }
                ) {
                    Text(
                        text = "Got it",
                        color = PrimaryBlueLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}