package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Displays a dialog when the player declares bankruptcy.
 * Shows a summary of assets and the resolution outcome.
 */
@Composable
fun BankruptcyResolutionOverlay(
    isVisible: Boolean,
    playerName: String,
    totalAssets: Int,
    totalDebt: Int,
    propertiesOwned: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    if (!isVisible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BankruptcyResolutionContent(
            playerName = playerName,
            totalAssets = totalAssets,
            totalDebt = totalDebt,
            propertiesOwned = propertiesOwned,
            onConfirm = onConfirm
        )
    }
}

/**
 * Content of the bankruptcy resolution overlay, separated for easier previewing.
 */
@Composable
fun BankruptcyResolutionContent(
    playerName: String,
    totalAssets: Int,
    totalDebt: Int,
    propertiesOwned: Int,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val buttonHeight = (screenWidth * 0.12f).coerceIn(44.dp, 56.dp)

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.92f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4A0000), RoundedCornerShape(12.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BANKRUPTCY",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Player name
                Text(
                    text = playerName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "has declared bankruptcy",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Summary box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D2D2D), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        SummaryRow(label = "Total Assets", value = "€$totalAssets")
                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryRow(label = "Total Debt", value = "€$totalDebt", isNegative = true)
                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryRow(label = "Properties Owned", value = "$propertiesOwned")
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SummaryRow(
                            label = "Net Worth",
                            value = "€${totalAssets - totalDebt}",
                            isNegative = totalAssets < totalDebt
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Warning message
                Text(
                    text = "All properties will be transferred to creditors",
                    fontSize = 13.sp,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Confirm button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Accept",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Bankruptcy Resolution", widthDp = 640, heightDp = 480)
@Composable
fun BankruptcyResolutionOverlayPreview() {
    Surface(color = Color.Gray) {
        BankruptcyResolutionContent(
            playerName = "Poor Player",
            totalAssets = 150,
            totalDebt = 400,
            propertiesOwned = 2,
            onConfirm = {}
        )
    }
}


@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isNegative) Color(0xFFFF5252) else Color(0xFF69F0AE)
        )
    }
}
