package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.aau.monopoly.klagenfurt.model.field.PropertyField

@Composable
fun BuildingManagerOverlay(
    properties: List<PropertyField>,
    onBuyHouse: (Int) -> Unit,
    onBuyHotel: (Int) -> Unit,
    onSellHouse: (Int) -> Unit,
    onSellHotel: (Int) -> Unit,
    onDismiss: () -> Unit,
    isBuildingActionPending: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(min = 360.dp, max = 520.dp)
                .heightIn(max = 520.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverlayHeader(onDismiss)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(properties) { property ->
                        PropertyCard(
                            property = property,
                            onBuyHouse = onBuyHouse,
                            onBuyHotel = onBuyHotel,
                            onSellHouse = onSellHouse,
                            onSellHotel = onSellHotel,
                            isBuildingActionPending = isBuildingActionPending
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayHeader(
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🏗️ Manage Buildings",
            fontWeight = FontWeight.Bold
        )

        TextButton(onClick = onDismiss) {
            Text("✕")
        }
    }
}

@Composable
private fun PropertyCard(
    property: PropertyField,
    onBuyHouse: (Int) -> Unit,
    onBuyHotel: (Int) -> Unit,
    onSellHouse: (Int) -> Unit,
    onSellHotel: (Int) -> Unit,
    isBuildingActionPending: Boolean

) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PropertyHeader(property)
            BuildingButtons(
                property = property,
                onBuyHouse = onBuyHouse,
                onBuyHotel = onBuyHotel,
                onSellHouse = onSellHouse,
                onSellHotel = onSellHotel,
                isBuildingActionPending = isBuildingActionPending
            )
        }
    }
}

@Composable
private fun PropertyHeader(
    property: PropertyField
) {
    val buildingStatus = when {
        property.hasHotel -> "🏨 Hotel"
        property.houses > 0 -> "🏠".repeat(property.houses)
        else -> "No buildings"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = property.name,
            fontWeight = FontWeight.Bold
        )

        Text(text = buildingStatus)
    }

    Text(
        text = "House: ${property.houseCost}M · Hotel: ${property.hotelCost}M"
    )
}

@Composable
private fun BuildingButtons(
    property: PropertyField,
    onBuyHouse: (Int) -> Unit,
    onBuyHotel: (Int) -> Unit,
    onSellHouse: (Int) -> Unit,
    onSellHotel: (Int) -> Unit,
    isBuildingActionPending: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!property.hasHotel && property.houses < 4) {
            Button(onClick = { onBuyHouse(property.id) },
                enabled = !isBuildingActionPending) {
                Text("Buy 🏠")
            }
        }

        if (property.houses > 0) {
            OutlinedButton(onClick = { onSellHouse(property.id) },
                enabled = !isBuildingActionPending) {
                Text("Sell 🏠")
            }
        }

        if (property.houses == 4 && !property.hasHotel) {
            Button(onClick = { onBuyHotel(property.id) },
                enabled = !isBuildingActionPending) {
                Text("Buy 🏨")
            }
        }

        if (property.hasHotel) {
            OutlinedButton(
                onClick = { onSellHotel(property.id) },
                enabled = !isBuildingActionPending
            ) {
                Text("Sell 🏨")
            }
        }
    }
}