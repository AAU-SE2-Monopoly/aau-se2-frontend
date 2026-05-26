package at.aau.monopoly.klagenfurt.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import at.aau.monopoly.klagenfurt.model.enums.PropertyColor
import at.aau.monopoly.klagenfurt.ui.theme.MyApplicationTheme
import at.aau.monopoly.klagenfurt.ui.util.toPropertyComposeColor

private val ManagedCardBackground = Color(0xFFFFF8E1)
private val MortgagedManagedCardBackground = Color(0xFFE0E0E0)
private val SelectedManagedCardBorder = Color(0xFF69F0AE)
private val MortgagedManagedCardRed = Color(0xFFD32F2F)

/**
 * Sorts [ManageableProperty] instances using the same multi-tier ordering:
 * non-mortgaged (buildings first) → color-grouped → mortgaged.
 */
fun sortManageableProperties(properties: List<ManageableProperty>): List<ManageableProperty> {
    return properties.sortedWith(
        compareBy<ManageableProperty> { prop ->
            when {
                !prop.isMortgaged && (prop.houses > 0 || prop.hasHotel) -> 0
                !prop.isMortgaged -> 1
                prop.isMortgaged -> 3
                else -> 4
            }
        }
            .thenByDescending { prop ->
                if (!prop.isMortgaged && (prop.houses > 0 || prop.hasHotel))
                    if (prop.hasHotel) 5 else prop.houses
                else 0
            }
            .thenBy { prop ->
                when (prop.color?.lowercase()) {
                    "brown" -> PropertyColor.BROWN.ordinal
                    "light_blue" -> PropertyColor.LIGHT_BLUE.ordinal
                    "pink" -> PropertyColor.PINK.ordinal
                    "orange" -> PropertyColor.ORANGE.ordinal
                    "red" -> PropertyColor.RED.ordinal
                    "yellow" -> PropertyColor.YELLOW.ordinal
                    "green" -> PropertyColor.GREEN.ordinal
                    "dark_blue" -> PropertyColor.DARK_BLUE.ordinal
                    else -> 100 // railroads, utilities
                }
            }
            .thenBy { it.name }
    )
}

// public data class (kept for compatibility with UiMappers / GameViewModel)

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



/** Displays a management dialog for the player's properties. */
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
    var selectedProperty by remember { mutableStateOf<ManageableProperty?>(null) }

    // Sort: buildings first -> color-grouped -> mortgaged
    val sortedProperties = remember(properties) { sortManageableProperties(properties) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val cardWidth = (screenWidth * 0.30f).coerceIn(100.dp, 190.dp)
        val cardHeight = cardWidth * 1.6f
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

                Spacer(modifier = Modifier.height(10.dp))

                // Balance display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Balance: €$currentMoney",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF69F0AE)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (sortedProperties.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You don't own any properties",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    PropertyCardRow(
                        properties = sortedProperties,
                        selectedProperty = selectedProperty,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        onCardClick = { selected ->
                            selectedProperty = if (selectedProperty?.fieldId == selected.fieldId) null else selected
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(
                        visible = selectedProperty != null,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        selectedProperty?.let { prop ->
                            PropertyActionPanel(
                                property = prop,
                                currentMoney = currentMoney,
                                onMortgage = { onMortgage(prop.fieldId); selectedProperty = null },
                                onUnmortgage = { onUnmortgage(prop.fieldId); selectedProperty = null },
                                onSellHouse = { onSellHouse(prop.fieldId); selectedProperty = null },
                                onSellHotel = { onSellHotel(prop.fieldId); selectedProperty = null }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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

// card row

@Composable
private fun PropertyCardRow(
    properties: List<ManageableProperty>,
    selectedProperty: ManageableProperty?,
    cardWidth: Dp,
    cardHeight: Dp,
    onCardClick: (ManageableProperty) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = remember(properties) { groupByColor(properties) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        var first = true
        grouped.forEach { group ->
            if (!first) {
                item(key = "spacer_${group.key}") {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            first = false

            items(group.properties, key = { "prop_${it.fieldId}" }) { prop ->
                val isSelected = selectedProperty?.fieldId == prop.fieldId
                ManagedPropertyCard(
                    property = prop,
                    isSelected = isSelected,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    onClick = { onCardClick(prop) }
                )
                if (prop != group.properties.last()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

// card in the row
@Composable
private fun ManagedPropertyCard(
    property: ManageableProperty,
    isSelected: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    onClick: () -> Unit
) {
    val isMortgaged = property.isMortgaged
    val propColor = property.color.toPropertyComposeColor()
    val cardBackground = if (isMortgaged) MortgagedManagedCardBackground else ManagedCardBackground
    val primaryTextColor = if (isMortgaged) Color.Black.copy(alpha = 0.55f) else Color.Black
    val secondaryTextColor = if (isMortgaged) Color.DarkGray.copy(alpha = 0.55f) else Color.DarkGray
    val borderColor = when {
        isSelected -> SelectedManagedCardBorder
        isMortgaged -> MortgagedManagedCardRed
        else -> propColor
    }
    val borderWidth = if (isSelected) 2.5.dp else 1.dp
    val buildingStatus = when {
        property.hasHotel -> "🏨 Hotel"
        property.houses > 0 -> "🏠×${property.houses}"
        else -> "No buildings"
    }

    CardShell(
        borderColor = borderColor,
        borderWidth = borderWidth,
        backgroundColor = cardBackground,
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clickable(onClick = onClick)
    ) { s ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.dp(19f))
                        .background(propColor.copy(alpha = if (isMortgaged) 0.35f else 1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (property.color == null) {
                        Text(
                            text = "OWNABLE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = s.sp(8f),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Text(
                    text = property.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = s.sp(12.5f),
                    color = primaryTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = s.dp(4f), vertical = s.dp(2f))
                )

                ThinDivider(s, color = Color.LightGray.copy(alpha = if (isMortgaged) 0.6f else 1f))

                RentRow("Price", "€${property.price}", s, labelColor = secondaryTextColor, valueColor = primaryTextColor)
                RentRow("Mortgage", "€${property.mortgageValue}", s, labelColor = secondaryTextColor, valueColor = primaryTextColor)

                ThinDivider(s, color = Color.LightGray.copy(alpha = if (isMortgaged) 0.6f else 1f))

                Text(
                    text = buildingStatus,
                    fontSize = s.sp(8f),
                    fontWeight = if (property.houses > 0 || property.hasHotel) FontWeight.Bold else FontWeight.Normal,
                    color = secondaryTextColor
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (isSelected) "▼ SELECTED ▼" else "Tap to manage",
                    fontSize = s.sp(if (isSelected) 8f else 7f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) SelectedManagedCardBorder else secondaryTextColor.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = s.dp(3f))
                )
            }

            if (isMortgaged) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .rotate(-14f)
                            .background(
                                MortgagedManagedCardRed.copy(alpha = 0.85f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = s.dp(14f), vertical = s.dp(2.5f))
                    ) {
                        Text(
                            text = "MORTGAGED",
                            fontSize = s.sp(9f),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}

// below cards, appears when a card is tapped

@Composable
private fun PropertyActionPanel(
    property: ManageableProperty,
    currentMoney: Int,
    onMortgage: () -> Unit,
    onUnmortgage: () -> Unit,
    onSellHouse: () -> Unit,
    onSellHotel: () -> Unit
) {
    val canUnmortgage = currentMoney >= property.unmortgageCost

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = property.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (property.isMortgaged) "MORTGAGED" else "Owned",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (property.isMortgaged) Color(0xFFFF5252) else Color(0xFF69F0AE)
                )
            }

            if (property.houses > 0 || property.hasHotel) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (property.hasHotel) {
                        Text(
                            "🏨 Hotel",
                            fontSize = 13.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "🏠 ${property.houses} House${if (property.houses > 1) "s" else ""}",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!property.isMortgaged && property.houses == 0 && !property.hasHotel) {
                    ActionChip(
                        text = "Mortgage",
                        sub = "+€${property.mortgageValue}",
                        backgroundColor = Color(0xFFC62828),
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onMortgage
                    )
                }

                if (property.isMortgaged) {
                    ActionChip(
                        text = "Unmortgage",
                        sub = "-€${property.unmortgageCost}",
                        backgroundColor = Color(0xFF2E7D32),
                        enabled = canUnmortgage,
                        modifier = Modifier.weight(1f),
                        onClick = onUnmortgage
                    )
                }

                if (property.houses > 0 && !property.isMortgaged) {
                    ActionChip(
                        text = "Sell House",
                        sub = "+€${property.sellHouseValue}",
                        backgroundColor = Color(0xFFC77700),
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onSellHouse
                    )
                }

                if (property.hasHotel && !property.isMortgaged) {
                    ActionChip(
                        text = "Sell Hotel",
                        sub = "+€${property.sellHotelValue}",
                        backgroundColor = Color(0xFFC77700),
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onSellHotel
                    )
                }
            }

            if (property.isMortgaged && !canUnmortgage) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Need €${property.unmortgageCost - currentMoney} more",
                    fontSize = 11.sp,
                    color = Color(0xFFFF5252).copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@Composable
private fun ActionChip(
    text: String,
    sub: String,
    backgroundColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = sub,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}


private data class ColorGroup(val key: Int, val properties: List<ManageableProperty>)

private fun groupByColor(properties: List<ManageableProperty>): List<ColorGroup> {
    val result = mutableListOf<ColorGroup>()
    var currentKey = Int.MIN_VALUE
    var currentGroup = mutableListOf<ManageableProperty>()

    for (prop in properties) {
        val key = colorKey(prop)
        if (key != currentKey && currentGroup.isNotEmpty()) {
            result.add(ColorGroup(currentKey, currentGroup.toList()))
            currentGroup = mutableListOf()
        }
        currentKey = key
        currentGroup.add(prop)
    }
    if (currentGroup.isNotEmpty()) {
        result.add(ColorGroup(currentKey, currentGroup.toList()))
    }
    return result
}

private fun colorKey(prop: ManageableProperty): Int {
    val base = when (prop.color?.lowercase()) {
        "brown" -> PropertyColor.BROWN.ordinal
        "light_blue" -> PropertyColor.LIGHT_BLUE.ordinal
        "pink" -> PropertyColor.PINK.ordinal
        "orange" -> PropertyColor.ORANGE.ordinal
        "red" -> PropertyColor.RED.ordinal
        "yellow" -> PropertyColor.YELLOW.ordinal
        "green" -> PropertyColor.GREEN.ordinal
        "dark_blue" -> PropertyColor.DARK_BLUE.ordinal
        else -> 100
    }
    return if (prop.isMortgaged) base + 200 else base
}


@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun MortgageManagementPreview() {
    val sampleProperties = listOf(
        ManageableProperty(
            fieldId = 1, name = "Heiligengeistplatz", color = "brown",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 0, hasHotel = true, isMortgaged = false,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 2, name = "Heiligengeistplatz", color = "brown",
            price = 60, mortgageValue = 30, unmortgageCost = 33,
            houses = 0, hasHotel = false, isMortgaged = true,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 3, name = "Heiligengeistplatz", color = null,
            price = 200, mortgageValue = 100, unmortgageCost = 110,
            houses = 0, hasHotel = false, isMortgaged = false,
            houseCost = 0, hotelCost = 0, sellHouseValue = 0, sellHotelValue = 0
        ),
        ManageableProperty(
            fieldId = 4, name = "Heiligengeistplatz", color = "light_blue",
            price = 100, mortgageValue = 50, unmortgageCost = 55,
            houses = 4, hasHotel = false, isMortgaged = false,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 5, name = "Heiligengeistplatz", color = "light_blue",
            price = 120, mortgageValue = 60, unmortgageCost = 66,
            houses = 0, hasHotel = true, isMortgaged = false,
            houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
        ),
        ManageableProperty(
            fieldId = 6, name = "Heiligengeistplatz", color = "pink",
            price = 140, mortgageValue = 70, unmortgageCost = 77,
            houses = 0, hasHotel = false, isMortgaged = true,
            houseCost = 100, hotelCost = 100, sellHouseValue = 50, sellHotelValue = 50
        )
    )

    MyApplicationTheme {
        Surface(color = Color.DarkGray) {
            MortgageManagementContent(
                properties = sampleProperties,
                currentMoney = 1200,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManagedPropertyCardPreview() {
    val prop = ManageableProperty(
        fieldId = 1, name = "Heiligengeistplatz", color = "brown",
        price = 60, mortgageValue = 30, unmortgageCost = 33,
        houses = 2, hasHotel = false, isMortgaged = false,
        houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
    )
    MyApplicationTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ManagedPropertyCard(
                property = prop,
                isSelected = true,
                cardWidth = 120.dp,
                cardHeight = 192.dp,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MortgagedManagedPropertyCardPreview() {
    val prop = ManageableProperty(
        fieldId = 2, name = "Heiligengeistplatz", color = "brown",
        price = 60, mortgageValue = 30, unmortgageCost = 33,
        houses = 0, hasHotel = false, isMortgaged = true,
        houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
    )
    MyApplicationTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ManagedPropertyCard(
                property = prop,
                isSelected = false,
                cardWidth = 120.dp,
                cardHeight = 192.dp,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun PropertyActionPanelPreview() {
    val prop = ManageableProperty(
        fieldId = 1, name = "Heiligengeistplatz", color = "brown",
        price = 60, mortgageValue = 30, unmortgageCost = 33,
        houses = 2, hasHotel = false, isMortgaged = false,
        houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
    )
    MyApplicationTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PropertyActionPanel(
                property = prop,
                currentMoney = 1200,
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun MortgagedPropertyActionPanelPreview() {
    val prop = ManageableProperty(
        fieldId = 1, name = "Heiligengeistplatz", color = "brown",
        price = 60, mortgageValue = 30, unmortgageCost = 33,
        houses = 0, hasHotel = false, isMortgaged = true,
        houseCost = 50, hotelCost = 50, sellHouseValue = 25, sellHotelValue = 25
    )
    MyApplicationTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PropertyActionPanel(
                property = prop,
                currentMoney = 20, // Not enough to unmortgage
                onMortgage = {},
                onUnmortgage = {},
                onSellHouse = {},
                onSellHotel = {}
            )
        }
    }
}
