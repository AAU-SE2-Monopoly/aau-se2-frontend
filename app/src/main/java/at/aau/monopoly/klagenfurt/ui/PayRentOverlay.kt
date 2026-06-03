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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Displays a payment dialog when the player lands on a property owned by another player.
 * Shows the amount due and provides options to pay or declare bankruptcy (if cannot pay).
 */
@Composable
fun PayRentOverlay(
    isVisible: Boolean,
    rentAmount: Int,
    ownerName: String?,
    fieldName: String,
    currentMoney: Int,
    canPay: Boolean,
    canRaiseFunds: Boolean,
    paymentInFlight: Boolean,
    propertyInFlight: Boolean,
    onPay: () -> Unit,
    onManageProperties: () -> Unit,
    onDeclareBankruptcy: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val content = @Composable {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
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
            val largeSpacerHeight = (screenHeight * 0.02f).coerceIn(8.dp, 20.dp)

            val amountTextSize = (screenHeight.value * 0.03f).coerceIn(18f, 24f).sp
            val buttonTextSize = (screenHeight.value * 0.017f).coerceIn(12f, 14f).sp
            val headerTextSize = (screenHeight.value * 0.02f).coerceIn(14f, 18f).sp
            val fieldNameSize = (screenHeight.value * 0.018f).coerceIn(12f, 16f).sp
            val ownerInfoSize = (screenHeight.value * 0.014f).coerceIn(10f, 12f).sp
            val balanceTextSize = (screenHeight.value * 0.017f).coerceIn(12f, 14f).sp
            val noteTextSize = (screenHeight.value * 0.012f).coerceIn(8f, 10f).sp

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
                            .background(
                                Color(0xFFC62828),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = verticalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RENT DUE",
                            color = Color.White,
                            fontSize = headerTextSize,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(mediumSpacerHeight))

                    // Field name
                    Text(
                        text = fieldName,
                        fontSize = fieldNameSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    // Owner info
                    Spacer(modifier = Modifier.height(smallSpacerHeight))
                    Text(
                        text = "Owner: ${ownerName ?: "Unknown"}",
                        fontSize = ownerInfoSize,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(mediumSpacerHeight))

                    // Amount box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D2D), RoundedCornerShape(12.dp))
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Amount to Pay",
                                fontSize = ownerInfoSize,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "€$rentAmount",
                                fontSize = amountTextSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(mediumSpacerHeight))

                    // Player balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Balance:",
                            fontSize = ownerInfoSize,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "€$currentMoney",
                            fontSize = balanceTextSize,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canPay) Color(0xFF69F0AE) else Color(0xFFFF5252)
                        )
                    }

                    Spacer(modifier = Modifier.height(largeSpacerHeight))

                    // Action buttons
                    Button(
                        onClick = onPay,
                        enabled = canPay && !paymentInFlight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when {
                                paymentInFlight -> "Processing..."
                                canPay -> "Pay Rent"
                                else -> "Insufficient Funds"
                            },
                            fontSize = buttonTextSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(smallSpacerHeight))

                    Button(
                        onClick = onManageProperties,
                        enabled = !propertyInFlight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Manage Properties",
                            fontSize = buttonTextSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(smallSpacerHeight))
                    if(!canRaiseFunds) {
                        Button(
                            onClick = onDeclareBankruptcy,
                            enabled = !paymentInFlight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB71C1C),
                                disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (paymentInFlight) "Processing..." else "Declare Bankruptcy",
                                fontSize = buttonTextSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(smallSpacerHeight))

                    // Note
                    val noteText = when {
                        canPay -> "Click Pay Rent to pay the rent"
                        canRaiseFunds -> "Manage properties to raise cash"
                        else -> "Insufficient total assets — declare bankruptcy"
                    }
                    Text(
                        text = noteText,
                        fontSize = noteTextSize,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (LocalInspectionMode.current) {
        content()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            content()
        }
    }
}
