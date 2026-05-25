package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
                .width(280.dp)
                .heightIn(max = 420.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Buildings", fontWeight = FontWeight.Bold)

                    TextButton(onClick = onDismiss) {
                        Text("✕")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(properties) { property ->
                        Text("${property.name}: ${property.houses} houses")

                        if (!property.hasHotel && property.houses < 4) {
                            Button(onClick = { onBuyHouse(property.id) }) {
                                Text("Buy House")
                            }
                        }

                        if (property.houses > 0) {
                            Button(onClick = { onSellHouse(property.id) }) {
                                Text("Sell House")
                            }
                        }

                        if (property.houses == 4 && !property.hasHotel) {
                            Button(onClick = { onBuyHotel(property.id) }) {
                                Text("Buy Hotel")
                            }
                        }

                        if (property.hasHotel) {
                            Button(onClick = { onSellHotel(property.id) }) {
                                Text("Sell Hotel")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}