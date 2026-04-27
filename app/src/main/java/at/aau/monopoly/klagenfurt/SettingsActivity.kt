package at.aau.monopoly.klagenfurt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.ui.components.BackButton
import at.aau.monopoly.klagenfurt.ui.components.DarkGradientBackground
import at.aau.monopoly.klagenfurt.networking.ServerConfig
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
    val accentBlue = PrimaryBlueLight

    val soundEnabled = remember { mutableStateOf(true) }
    val musicEnabled = remember { mutableStateOf(true) }

    DarkGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SETTINGS",
                color = accentBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                thickness = 1.dp,
                color = accentBlue.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Server toggle (Local / Global)
            SettingsToggleRow(
                label = "Server: ${ServerConfig.displayLabel}",
                checked = ServerConfig.isGlobal,
                onCheckedChange = {
                    ServerConfig.isGlobal = it
                    ServiceLocator.resetGameService()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sound toggle
            SettingsToggleRow(
                label = "Sounds",
                checked = soundEnabled.value,
                onCheckedChange = { soundEnabled.value = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Music toggle
            SettingsToggleRow(
                label = "Music",
                checked = musicEnabled.value,
                onCheckedChange = { musicEnabled.value = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                thickness = 1.dp,
                color = accentBlue.copy(alpha = 0.3f)
            )
        }

        // Fixed back button – top-left
        BackButton(
            onClick = onBackClicked,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

