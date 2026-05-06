package at.aau.monopoly.klagenfurt.ui.board
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.monopoly.klagenfurt.model.field.ChanceField
import at.aau.monopoly.klagenfurt.model.field.CommunityChestField
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.OwnableField
import at.aau.monopoly.klagenfurt.model.field.PropertyField
import at.aau.monopoly.klagenfurt.model.field.RailroadField
import at.aau.monopoly.klagenfurt.ui.util.toComposeColor
import com.example.myapplication.R
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class FieldBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val isCorner: Boolean,
    val textWidth: Float,
    val textHeight: Float
)

fun calculateFieldBounds(index: Int, sw: Float, sh: Float): FieldBounds {
    val side = (index / 10) % 4
    val posInSide = index % 10
    val isCorner = posInSide == 0

    val corners = listOf(
        Offset(2445f, 1720f),
        Offset(1245f, 1720f),
        Offset(1245f, 520f),
        Offset(2445f, 520f)
    )

    val start = corners[side]
    val end = corners[(side + 1) % 4]
    val designCornerSize = 240f

    fun scaleX(x: Float) = ((x / 3840f) * sw).roundToInt().toFloat()
    fun scaleY(y: Float) = ((y / 2160f) * sh).roundToInt().toFloat()

    if (isCorner) {
        val textRotation = 0f
        val innerScale = 1.0f
        val tW = scaleX(designCornerSize) * innerScale
        val tH = scaleY(designCornerSize) * innerScale

        return FieldBounds(
            x = scaleX(start.x) - scaleX(designCornerSize) / 2,
            y = scaleY(start.y) - scaleY(designCornerSize) / 2,
            width = scaleX(designCornerSize),
            height = scaleY(designCornerSize),
            rotation = textRotation,
            isCorner = true,
            textWidth = tW,
            textHeight = tH
        )
    } else {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val innerDist = dist - designCornerSize
        val fieldStep = innerDist / 9f
        val dirX = dx / dist
        val dirY = dy / dist

        val centerX = start.x + dirX * (designCornerSize / 2f + posInSide * fieldStep - fieldStep / 2f)
        val centerY = start.y + dirY * (designCornerSize / 2f + posInSide * fieldStep - fieldStep / 2f)

        val isHorizontal = side == 0 || side == 2
        val dw = if (isHorizontal) fieldStep else 180f
        val dh = if (isHorizontal) 180f else fieldStep

        val boxW = scaleX(dw)
        val boxH = scaleY(dh)
        val textRotation = side * 90f

        return FieldBounds(
            x = scaleX(centerX) - boxW / 2,
            y = scaleY(centerY) - boxH / 2,
            width = boxW,
            height = boxH,
            rotation = textRotation,
            isCorner = false,
            textWidth = if (isHorizontal) boxW else boxH,
            textHeight = if (isHorizontal) boxH else boxW
        )
    }
}

/** Tinted backgrounds for the four corner fields to visually differentiate them. */
private val cornerColors = mapOf(
    0  to Color(0xFFE8F5E9),
    10 to Color(0xFFFFF3E0),
    20 to Color(0xFFF3E5F5),
    30 to Color(0xFFFFEBEE),
)
@Composable
fun FieldItem(index: Int, field: Field, sw: Float, sh: Float) {
    val bounds = remember(index, sw, sh) { calculateFieldBounds(index, sw, sh) }
    val side = (index / 10) % 4
    val imageMap = remember(field.name) { getFieldImageMapping(field.name) }
    val containerMod = remember(bounds, index) { fieldItemContainerMod(bounds, index) }

    val imageTint = remember(field) {
        when (field) {
            is PropertyField -> field.color.toComposeColor()
            is ChanceField -> Color(0xFFFFB900)       // yellow-amber
            is CommunityChestField -> Color(0xFF1E88E5) // blue
            is RailroadField -> Color(0xFF5D4037)     // dark brown
            else -> null
        }
    }

    Box(
        modifier = containerMod,
        contentAlignment = Alignment.Center
    ) {
        FieldImage(
            imageRes = imageMap,
            fieldName = field.name,
            bounds = bounds,
            tint = imageTint
        )
        if (!bounds.isCorner && field is PropertyField) {
            PropertyColorBar(
                side = side,
                sw = sw,
                sh = sh,
                color = field.color.toComposeColor()
            )
        }

        // Owner indicator dot on owned fields
        if (!bounds.isCorner && field is OwnableField && field.ownerId != null) {
            val dotColor = when (field) {
                is PropertyField -> field.color.toComposeColor()
                else -> Color.DarkGray
            }
            OwnerIndicator(
                side = side,
                fieldColor = dotColor
            )
        }

        // Mortgaged overlay watermark
        if (!bounds.isCorner && field is OwnableField && field.isMortgaged) {
            MortgagedOverlay(bounds = bounds)
        }

        FieldTitle(
            text = field.name,
            bounds = bounds
        )
    }
}

private val fieldImageMappingsLower = mapOf(
    "go" to R.drawable.corners_go_field,
    "herrengasse" to R.drawable.herrengasse,
    "community chest" to R.drawable.chest,
    "reichensteuer" to R.drawable.taxes,
    "hauptbahnhof" to R.drawable.hauptbahnhof,
    "neuer platz" to R.drawable.neuer_platz,
    "chance" to R.drawable.chance,
    "alter platz" to R.drawable.alter_platz,
    "benediktiner platz" to R.drawable.benedektiner,
    "jail / just visiting" to R.drawable.jail,
    "cine city" to R.drawable.cinema,
    "kelag klagenfurt" to R.drawable.kelag,
    "mcdonalds" to R.drawable.mcdonalds,
    "ruthar" to R.drawable.ruthar,
    "ostbahnhof" to R.drawable.ostbahnhof,
    "wohnzimmer" to R.drawable.wonhzimmer,
    "hafenstadt" to R.drawable.hafenstadt,
    "lendcafe" to R.drawable.lendcafe,
    "free parking" to R.drawable.free_parking,
    "city arkaden" to R.drawable.city_arkaden,
    "le burger" to R.drawable.le_burger,
    "mc mullens" to R.drawable.mc_mullens,
    "westbahnhof" to R.drawable.westbahnhof,
    "mensa" to R.drawable.mensa,
    "universität klagenfurt" to R.drawable.uni,
    "stadtwerke klagenfurt" to R.drawable.stadtwerke,
    "lakeside" to R.drawable.lakeside,
    "go to jail" to R.drawable.gotojail,
    "strandbad" to R.drawable.strandbad,
    "loretto" to R.drawable.loretto,
    "villa lido" to R.drawable.villa_lido,
    "lendbahnhof" to R.drawable.lendbahnhof,
    "botanischer garten" to R.drawable.botanischer_garten,
    "kreuzbergl" to R.drawable.kreuzbegl,
    "heiligengeistplatz" to R.drawable.heiligengeistplatz
)

/** Normalized lookup: trims and lowercases the field name before matching. */
fun getFieldImageMapping(fieldName: String): Int? {
    return fieldImageMappingsLower[fieldName.trim().lowercase()]
}

private fun fieldItemContainerMod(bounds: FieldBounds, index: Int): Modifier {
    val borderWidth = if (bounds.isCorner) 0.75.dp else 0.5.dp
    val borderAlpha = if (bounds.isCorner) 0.5f else 0.4f
    val backgroundColor = if (bounds.isCorner) {
        cornerColors[index] ?: Color.White
    } else {
        Color.White
    }
    val elevation = if (bounds.isCorner) 4.dp else 2.dp

    return Modifier
        .offset(x = bounds.x.dp, y = bounds.y.dp)
        .size(bounds.width.dp, bounds.height.dp)
        .shadow(elevation, RectangleShape, clip = false)
        .clip(RectangleShape)
        .border(borderWidth, Color.Black.copy(alpha = borderAlpha))
        .background(backgroundColor)
}

@Composable
private fun FieldImage(
    imageRes: Int?,
    fieldName: String,
    bounds: FieldBounds,
    tint: Color? = null
) {
    if (imageRes == null) return

    val imagePadding = if (bounds.isCorner) 0.dp else 1.dp
    val imageShape = RoundedCornerShape(2.dp)
    val borderWidth = if (bounds.isCorner) 1.dp else 0.5.dp

    val imgHeight = if (bounds.isCorner) bounds.textHeight else bounds.textHeight * 0.75f
    val imgOffsetY = 0.dp

    Box(
        modifier = Modifier
            .requiredSize(width = bounds.textWidth.dp, height = imgHeight.dp)
            .offset(y = imgOffsetY)
            .rotate(bounds.rotation)
            .clip(imageShape)
            .background(Color.Transparent)
            .border(borderWidth, Color.White, imageShape)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = fieldName,
            modifier = Modifier
                .fillMaxSize()
                .padding(imagePadding),
            contentScale = ContentScale.Fit,
            colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.SrcAtop) }
        )
    }
}

/**
 * A small colored dot in the outer corner of a field indicating it's owned.
 * Color matches the property color for PropertyFields, dark-gray for railroads/utilities.
 */
@Composable
private fun BoxScope.OwnerIndicator(
    side: Int,
    fieldColor: Color
) {
    val dotSize = 4.dp
    val alignment = when (side) {
        0 -> Alignment.TopStart
        1 -> Alignment.BottomStart
        2 -> Alignment.BottomEnd
        3 -> Alignment.TopEnd
        else -> Alignment.TopStart
    }

    Box(
        modifier = Modifier
            .size(dotSize)
            .align(alignment)
            .offset(x = 2.dp, y = 2.dp)
            .clip(CircleShape)
            .background(fieldColor)
            .border(0.5.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
    )
}

/**
 * A diagonal "MORTGAGED" watermark overlay for owned fields that are mortgaged.
 */
@Composable
private fun BoxScope.MortgagedOverlay(bounds: FieldBounds) {
    Text(
        text = "MORTGAGED",
        color = Color.Red.copy(alpha = 0.5f),
        fontSize = 2.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .align(Alignment.Center)
            .rotate(bounds.rotation)
    )
}

@Composable
fun BoxScope.PropertyColorBar(
    side: Int,
    sw: Float,
    sh: Float,
    color: Color
) {
    val barSize = 35f
    val barModifier = when (side) {
        0 -> Modifier
            .fillMaxWidth()
            .height(((barSize / 2160f) * sh).dp)
            .align(Alignment.TopCenter)
            .testTag("Bottom-Bar")

        1 -> Modifier
            .fillMaxHeight()
            .width(((barSize / 3840f) * sw).dp)
            .align(Alignment.CenterEnd)
            .testTag("Left-Bar")

        2 -> Modifier
            .fillMaxWidth()
            .height(((barSize / 2160f) * sh).dp)
            .align(Alignment.BottomCenter)
            .testTag("Top-Bar")

        3 -> Modifier
            .fillMaxHeight()
            .width(((barSize / 3840f) * sw).dp)
            .align(Alignment.CenterStart)
            .testTag("Right-Bar")

        else -> Modifier
    }

    Box(
        modifier = barModifier.background(color)
    )
}

@Composable
fun FieldTitle(
    text: String,
    bounds: FieldBounds
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .rotate(bounds.rotation)
            .padding(horizontal = 2.dp, vertical = if (bounds.isCorner) 2.dp else 1.dp),
        contentAlignment = if (bounds.isCorner) Alignment.Center else Alignment.BottomCenter
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = if (bounds.isCorner) 5.sp else 3.5.sp,
            lineHeight = if (bounds.isCorner) 6.sp else 4.2.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            style = TextStyle(hyphens = Hyphens.Auto)
        )
    }
}
