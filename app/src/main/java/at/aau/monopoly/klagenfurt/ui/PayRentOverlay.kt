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
 * Shows the rent amount and provides options to pay or declare bankruptcy(if cannot pay).
 */
@Composable
fun PayRentOverlay(
    isVisible: Boolean,
    rentAmount: Int,
    ownerName: String?,
    fieldName: String,
    currentMoney: Int,
    canPay: Boolean,
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
                            .background(Color(0xFFC62828), RoundedCornerShape(12.dp))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💸 RENT DUE",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Field name
                    Text(
                        text = fieldName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Owner info
                    Text(
                        text = "Owner: ${ownerName ?: "Unknown"}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Amount box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D2D), RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Amount to Pay",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "€$rentAmount",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Balance:",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "€$currentMoney",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canPay) Color(0xFF69F0AE) else Color(0xFFFF5252)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action buttons
                    Button(
                        onClick = onPay,
                        enabled = canPay,
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
                            text = if (canPay) "Pay Rent" else "Insufficient Funds",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onManageProperties,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Manage Properties",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                        if(!canPay) {
                            Button(
                                onClick = onDeclareBankruptcy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(buttonHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB71C1C)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {

                                Text(
                                    text = "Declare Bankruptcy",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                            }
                        }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Note
                    Text(
                        text = if (canPay) "You must pay rent to continue" else "You must pay rent or declare bankruptcy to continue",
                        fontSize = 12.sp,
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
        onPay = {},
        onManageProperties = {},
        onDeclareBankruptcy = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true, name = "Pay Rent - Insufficient Funds")
@Composable
fun PayRentOverlayPreview_CannotPay() {
    PayRentOverlay(
        isVisible = true,
        rentAmount = 2000,
        ownerName = "Another Random Dude",
        fieldName = "Heiligengeistplatz",
        currentMoney = 500,
        canPay = false,
        onPay = {},
        onManageProperties = {},
        onDeclareBankruptcy = {},
        onDismiss = {}
    )
}
