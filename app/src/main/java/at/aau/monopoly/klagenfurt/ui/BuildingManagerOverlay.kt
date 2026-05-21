package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.PropertyField

@Composable
fun BuildingManagerOverlay(
    properties: List<PropertyField>,
    fields: List<Field>,
    onBuyHouse: (Int) -> Unit,
    onBuyHotel: (Int) -> Unit,
    onSellHouse: (Int) -> Unit,
    onSellHotel: (Int) -> Unit,
    onDismiss: () -> Unit,
    canEndTurn: Boolean,
    isBuyingPhase: Boolean
) {
    Card(
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Manage Buildings")

            properties.forEach { property ->
                val colorSet = fields
                    .filterIsInstance<PropertyField>()
                    .filter { it.color == property.color }

                val minHouses = colorSet.minOf { it.houses }
                val maxHouses = colorSet.maxOf { it.houses }

                val canBuyHouse =
                    isBuyingPhase &&
                            !property.hasHotel &&
                            property.houses < 4 &&
                            property.houses == minHouses

                val canBuyHotel =
                    isBuyingPhase &&
                            !property.hasHotel &&
                            property.houses == 4

                val canSellHouse =
                    canEndTurn &&
                            !property.hasHotel &&
                            property.houses > 0 &&
                            property.houses == maxHouses

                val canSellHotel =
                    canEndTurn && property.hasHotel

                Column {
                    Text("${property.name}: ${property.houses} houses${if (property.hasHotel) ", hotel" else ""}")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canBuyHouse) {
                            Button(onClick = { onBuyHouse(property.id) }) {
                                Text("Buy House")
                            }
                        }

                        if (canBuyHotel) {
                            Button(onClick = { onBuyHotel(property.id) }) {
                                Text("Buy Hotel")
                            }
                        }

                        if (canSellHouse) {
                            Button(onClick = { onSellHouse(property.id) }) {
                                Text("Sell House")
                            }
                        }

                        if (canSellHotel) {
                            Button(onClick = { onSellHotel(property.id) }) {
                                Text("Sell Hotel")
                            }
                        }
                    }
                }
            }

            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    }
}