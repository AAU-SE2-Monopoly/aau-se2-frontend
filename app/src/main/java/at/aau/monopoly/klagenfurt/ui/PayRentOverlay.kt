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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
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
            val screenHeight = maxHeight
            val buttonHeight = (screenHeight * 0.08f).coerceIn(32.dp, 44.dp)

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RENT DUE",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Field name
                    Text(
                        text = fieldName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    // Owner info
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Owner: ${ownerName ?: "Unknown"}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Amount box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D2D), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Amount to Pay",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "€$rentAmount",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Balance:",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "€$currentMoney",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canPay) Color(0xFF69F0AE) else Color(0xFFFF5252)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
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
                                    text = "Declare Bankruptcy",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                            }
                        }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Note
                    val noteText = if (canPay) "You must pay rent to continue"
                        else "You must pay rent or declare bankruptcy to continue"
                    Text(
                        text = noteText,
                        fontSize = 10.sp,
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

@Preview(showBackground = true, name = "Pay Rent - Can Afford")
@Composable
fun PayRentOverlayPreview_CanPay() {
    PayRentOverlay(
        isVisible = true,
        rentAmount = 200,
        ownerName = "Just a Random Dude",
        fieldName = "Heiligengeistplatz",
        currentMoney = 1500,
        canPay = true,
        canRaiseFunds = true,
        paymentInFlight = false,
        propertyInFlight = false,
        onPay = {},
        onManageProperties = {},
        onDeclareBankruptcy = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true, name = "Pay Rent - Insufficient Cash but can raise")
@Composable
fun PayRentOverlayPreview_CannotPay() {
    PayRentOverlay(
        isVisible = true,
        rentAmount = 2000,
        ownerName = "Another Random Dude",
        fieldName = "Heiligengeistplatz",
        currentMoney = 500,
        canPay = false,
        canRaiseFunds = true,
        paymentInFlight = false,
        propertyInFlight = false,
        onPay = {},
        onManageProperties = {},
        onDeclareBankruptcy = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true, name = "Pay Rent - Bankrupt")
@Composable
fun PayRentOverlayPreview_Bankrupt() {
    PayRentOverlay(
        isVisible = true,
        rentAmount = 5000,
        ownerName = "Another Random Dude",
        fieldName = "Heiligengeistplatz",
        currentMoney = 100,
        canPay = false,
        canRaiseFunds = false,
        paymentInFlight = false,
        propertyInFlight = false,
        onPay = {},
        onManageProperties = {},
        onDeclareBankruptcy = {},
        onDismiss = {}
    )
}
