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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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
    isOwnBankruptcy: Boolean = false,
    isConfirmation: Boolean = false,
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
            isOwnBankruptcy = isOwnBankruptcy,
            isConfirmation = isConfirmation,
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
    isOwnBankruptcy: Boolean = false,
    isConfirmation: Boolean = false,
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
        val screenHeight = maxHeight
        val buttonHeight = (screenHeight * 0.08f).coerceIn(32.dp, 44.dp)
        val horizontalPadding = (screenWidth * 0.05f).coerceIn(8.dp, 24.dp)
        val verticalPadding = (screenHeight * 0.02f).coerceIn(4.dp, 16.dp)
        val contentPadding = (screenWidth * 0.06f).coerceIn(12.dp, 24.dp)
        val smallSpacerHeight = (screenHeight * 0.008f).coerceIn(2.dp, 8.dp)
        val mediumSpacerHeight = (screenHeight * 0.01f).coerceIn(6.dp, 10.dp)
        val summarySpacerHeight = (screenHeight * 0.012f).coerceIn(6.dp, 12.dp)

        // Scaling text sizes (derived from Pixel 5/6 proportions)
        val headerTextSize = (screenHeight.value * 0.02f).coerceIn(14f, 18f).sp
        val playerNameSize = (screenHeight.value * 0.02f).coerceIn(14f, 18f).sp
        val normalTextSize = (screenHeight.value * 0.015f).coerceIn(10f, 13f).sp
        val summaryLabelSize = (screenHeight.value * 0.015f).coerceIn(11f, 12f).sp
        val summaryValueSize = (screenHeight.value * 0.016f).coerceIn(12f, 14f).sp
        val warningTextSize = (screenHeight.value * 0.013f).coerceIn(9f, 11f).sp
        val buttonTextSize = (screenHeight.value * 0.017f).coerceIn(12f, 14f).sp

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontalPadding),
            color = Color.Black.copy(alpha = 0.92f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4A0000), RoundedCornerShape(12.dp))
                        .padding(vertical = verticalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BANKRUPTCY",
                        color = Color.White,
                        fontSize = headerTextSize,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(mediumSpacerHeight))

                // Player name
                Text(
                    text = playerName,
                    fontSize = playerNameSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(smallSpacerHeight))

                Text(
                    text = if (isConfirmation) "Are you sure you want to declare bankruptcy?"
                    else if (isOwnBankruptcy) "You are bankrupt"
                    else "has gone bankrupt",
                    fontSize = normalTextSize,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(mediumSpacerHeight))

                // Summary box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D2D2D), RoundedCornerShape(12.dp))
                        .padding(contentPadding)
                ) {
                    Column {
                        SummaryRow(label = "Total Assets", value = "€$totalAssets", labelSize = summaryLabelSize, valueSize = summaryValueSize)
                        Spacer(modifier = Modifier.height(summarySpacerHeight))
                        SummaryRow(label = "Total Debt", value = "€$totalDebt", isNegative = true, labelSize = summaryLabelSize, valueSize = summaryValueSize)
                        Spacer(modifier = Modifier.height(summarySpacerHeight))
                        SummaryRow(label = "Properties Owned", value = "$propertiesOwned", labelSize = summaryLabelSize, valueSize = summaryValueSize)
                        Spacer(modifier = Modifier.height(summarySpacerHeight * 1.33f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(summarySpacerHeight * 1.33f))
                        SummaryRow(
                            label = "Net Worth",
                            value = "€${totalAssets - totalDebt}",
                            isNegative = totalAssets < totalDebt,
                            labelSize = summaryLabelSize,
                            valueSize = summaryValueSize
                        )
                    }
                }

                Spacer(modifier = Modifier.height(mediumSpacerHeight))

                Text(
                    text = "All properties will be transferred to creditors",
                    fontSize = warningTextSize,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(mediumSpacerHeight))

                Text(
                    text = "This action cannot be undone. All your properties will be transferred to your creditors.",
                    fontSize = warningTextSize,
                    color = Color(0xFFFF8A80).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(mediumSpacerHeight))

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
                        text = if (isConfirmation) "Confirm" else if (isOwnBankruptcy) "Continue" else "Close",
                        fontSize = buttonTextSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isNegative: Boolean = false,
    labelSize: TextUnit = 12.sp,
    valueSize: TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = labelSize,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = valueSize,
            fontWeight = FontWeight.SemiBold,
            color = if (isNegative) Color(0xFFFF5252) else Color(0xFF69F0AE)
        )
    }
}
