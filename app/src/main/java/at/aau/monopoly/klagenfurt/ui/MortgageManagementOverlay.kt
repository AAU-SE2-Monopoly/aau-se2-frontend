package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Represents a property that can be managed (mortgaged/unmortgaged/have houses sold).
 */
data class ManageableProperty(
    val fieldId: Int,
    val name: String,
    val color: String?,
    val price: Int,
    val mortgageValue: Int,
    val unmortgageCost: Int,
    val houses: Int,
    val hasHotel: Boolean,
    val isMortgaged: Boolean,
    val houseCost: Int,
    val hotelCost: Int,
    val sellHouseValue: Int,
    val sellHotelValue: Int
)

/**
 * Displays a management dialog for the player's properties.
 * Allows mortgage, unmortgage, and sell house/hotel operations.
 */
@Composable
fun MortgageManagementOverlay(
    isVisible: Boolean,
    properties: List<ManageableProperty>,
    currentMoney: Int,
    onMortgage: (Int) -> Unit,
    onUnmortgage: (Int) -> Unit,
    onSellHouse: (Int) -> Unit,
    onSellHotel: (Int) -> Unit,
    onDismiss: () -> Unit
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
        MortgageManagementContent(
            properties = properties,
            currentMoney = currentMoney,
            onMortgage = onMortgage,
            onUnmortgage = onUnmortgage,
            onSellHouse = onSellHouse,
            onSellHotel = onSellHotel,
            onDismiss = onDismiss
        )
    }
}

/**
 * The actual content of the mortgage management UI, separated from the Dialog
 */
@Composable
fun MortgageManagementContent(
    properties: List<ManageableProperty>,
    currentMoney: Int,
    onMortgage: (Int) -> Unit,
    onUnmortgage: (Int) -> Unit,
    onSellHouse: (Int) -> Unit,
    onSellHotel: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenHeight = maxHeight
        val buttonHeight = (screenHeight * 0.065f).coerceIn(40.dp, 56.dp)

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.95f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1565C0), RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Property Management",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Current Balance: €$currentMoney",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF69F0AE)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (properties.isEmpty()) {
                    // No properties message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You don't own any properties",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Properties list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(properties, key = { it.fieldId }) { property ->
                            PropertyManagementCard(
                                property = property,
                                currentMoney = currentMoney,
                                onMortgage = onMortgage,
                                onUnmortgage = onUnmortgage,
                                onSellHouse = onSellHouse,
                                onSellHotel = onSellHotel
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF424242)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Close",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Property Management Content", widthDp = 640, heightDp = 480)
@Composable
fun MortgageManagementOverlayPreview() {
    val sampleProperties = listOf(
        ManageableProperty(
            fieldId = 1, name = "HeiligengeistPlatz", color = "BROWN",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 2, hasHotel = false, isMortgaged = false,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 3, name = "Alter Platz", color = "BROWN",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 0, hasHotel = false, isMortgaged = true,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 5, name = "Klagenfurt Hauptbahnhof", color = "RAILROAD",
            price = 200, mortgageValue = 100, unmortgageCost = 110,
            houses = 0, hasHotel = false, isMortgaged = false,
            houseCost = 0, hotelCost = 0, sellHouseValue = 0, sellHotelValue = 0
        ),
        ManageableProperty(
            fieldId = 3, name = "Alter Platz", color = "BROWN",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 0, hasHotel = false, isMortgaged = true,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 3, name = "Alter Platz", color = "BROWN",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 0, hasHotel = false, isMortgaged = true,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 3, name = "Alter Platz", color = "BROWN",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 0, hasHotel = false, isMortgaged = true,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 3, name = "Alter Platz", color = "BROWN",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 0, hasHotel = false, isMortgaged = true,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        )
    )

    Surface(color = Color.Gray) {
        MortgageManagementContent(
            properties = sampleProperties,
            currentMoney = 1500,
            onMortgage = {},
            onUnmortgage = {},
            onSellHouse = {},
            onSellHotel = {},
            onDismiss = {}
        )
    }
}


@Composable
private fun PropertyManagementCard(
    property: ManageableProperty,
    currentMoney: Int,
    onMortgage: (Int) -> Unit,
    onUnmortgage: (Int) -> Unit,
    onSellHouse: (Int) -> Unit,
    onSellHotel: (Int) -> Unit
) {
    val propertyColor = getPropertyColor(property.color)
    val canUnmortgage = currentMoney >= property.unmortgageCost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    property.isMortgaged -> Color(0xFF2D2D2D)
                    property.hasHotel || property.houses > 0 -> Color(0xFF1E3A1E)
                    else -> Color(0xFF2A2A2A)
                },
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                propertyColor.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Property header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = property.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (property.isMortgaged) Color.White.copy(alpha = 0.5f) else Color.White
                    )
                    if (property.isMortgaged) {
                        Text(
                            text = "MORTGAGED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252),
                            letterSpacing = 1.sp
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "€${property.price}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Building status
            if (property.houses > 0 || property.hasHotel) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buildings: ",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (property.hasHotel) {
                        Text(
                            text = "Hotel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    } else {
                        // Safe repeat to avoid potential infinite loops in renderer if houses is huge
                        Text(
                            text = "House ".repeat(property.houses.coerceIn(0, 4)).trim(),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(sell for €${if (property.hasHotel) property.sellHotelValue else property.sellHouseValue})",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons based on property state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mortgage button (if not already mortgaged and no buildings)
                if (!property.isMortgaged && property.houses == 0 && !property.hasHotel) {
                    ActionButton(
                        text = "Mortgage\n€${property.mortgageValue}",
                        backgroundColor = Color(0xFFFF8F00),
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onMortgage(property.fieldId) }
                    )
                }

                // Unmortgage button (if mortgaged)
                if (property.isMortgaged) {
                    ActionButton(
                        text = "Unmortgage\n€${property.unmortgageCost}",
                        backgroundColor = Color(0xFF2E7D32),
                        enabled = canUnmortgage,
                        modifier = Modifier.weight(1f),
                        onClick = { onUnmortgage(property.fieldId) }
                    )
                }

                // Sell house button (if has houses and not mortgaged)
                if (property.houses > 0 && !property.isMortgaged) {
                    ActionButton(
                        text = "Sell House\n+€${property.sellHouseValue}",
                        backgroundColor = Color(0xFF6A1B9A),
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onSellHouse(property.fieldId) }
                    )
                }

                // Sell hotel button (if has hotel and not mortgaged)
                if (property.hasHotel && !property.isMortgaged) {
                    ActionButton(
                        text = "Sell Hotel\n+€${property.sellHotelValue}",
                        backgroundColor = Color(0xFF6A1B9A),
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onSellHotel(property.fieldId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    backgroundColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

private fun getPropertyColor(color: String?): Color {
    return when (color?.lowercase()) {
        "brown" -> Color(0xFF795548)
        "light_blue" -> Color(0xFF4FC3F7)
        "pink" -> Color(0xFFE91E63)
        "orange" -> Color(0xFFFF9800)
        "red" -> Color(0xFFF44336)
        "yellow" -> Color(0xFFFFEB3B)
        "green" -> Color(0xFF4CAF50)
        "blue" -> Color(0xFF2196F3)
        "railroad" -> Color(0xFF424242)
        "utility" -> Color(0xFF9E9E9E)
        else -> Color(0xFF616161)
    }
}
