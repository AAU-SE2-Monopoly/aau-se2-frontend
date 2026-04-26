package at.aau.monopoly.klagenfurt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.field.PropertyField

@Composable
fun PropertyCardUI(property: PropertyField) {
    // Feste Breite, damit ca. 4 Karten nebeneinander auf den Bildschirm (Landscape) passen
    Column(
        modifier = Modifier
            .width(220.dp)
            .height(320.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Farbbalken oben
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(1.dp, Color.Black)
                .background(property.color.toComposeColor()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TITEL-URKUNDE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = property.name.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mieten (rent[0] = normal, rent[1..4] = 1-4 Häuser, rent[5] = Hotel)
        if (property.rent.isNotEmpty()) {
            Text(text = "Miete: € ${property.rent.getOrNull(0) ?: 0}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val houseLabels = listOf("Mit 1 Haus", "Mit 2 Häusern", "Mit 3 Häusern", "Mit 4 Häusern")
            for (i in 1..4) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = houseLabels[i-1], fontSize = 12.sp)
                    Text(text = "€ ${property.rent.getOrNull(i) ?: 0}", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Mit HOTEL: € ${property.rent.getOrNull(5) ?: 0}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Kosten
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Haus kostet", fontSize = 10.sp)
            Text(text = "€ ${property.houseCost}", fontSize = 10.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Hotel kostet", fontSize = 10.sp)
            Text(text = "€ ${property.hotelCost}", fontSize = 10.sp)
        }
    }
}