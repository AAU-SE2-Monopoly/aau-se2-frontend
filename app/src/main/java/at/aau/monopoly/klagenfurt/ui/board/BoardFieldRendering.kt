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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import at.aau.monopoly.klagenfurt.model.field.Field
import at.aau.monopoly.klagenfurt.model.field.PropertyField
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

@Composable
fun FieldItem(index: Int, field: Field, sw: Float, sh: Float) {
    val bounds = remember(index, sw, sh) { calculateFieldBounds(index, sw, sh) }
    val side = (index / 10) % 4
    val imageMap = remember(field.name) { getFieldImageMapping(field.name) }

    val containerMod = remember(bounds) { fieldItemContainerMod(bounds) }

    Box(
        modifier = containerMod,
        contentAlignment = Alignment.Center
    ) {
        FieldImage(
            imageRes = imageMap,
            fieldName = field.name,
            bounds = bounds
        )
        if (!bounds.isCorner && field is PropertyField) {
            PropertyColorBar(
                side = side,
                sw = sw,
                sh = sh,
                color = field.color.toComposeColor()
            )
        }

        FieldTitle(
            text = field.name,
            bounds = bounds
        )
    }
}

private val fieldImageMappings = mapOf(
    "Go" to R.drawable.corners_go_field,
    "Herrengasse" to R.drawable.herrengasse,
    "Community Chest" to R.drawable.chest,
    "Reichensteuer" to R.drawable.taxes,
    "Hauptbahnhof" to R.drawable.hauptbahnhof,
    "Neuer Platz" to R.drawable.neuer_platz,
    "Chance" to R.drawable.chance,
    "Alter Platz" to R.drawable.alter_platz,
    "Benediktiner Platz" to R.drawable.benedektiner,
    "Jail / Just Visiting" to R.drawable.jail,
    "Cine City" to R.drawable.cinema,
    "Kelag Klagenfurt" to R.drawable.kelag,
    "McDonalds" to R.drawable.mcdonalds,
    "Ruthar" to R.drawable.ruthar,
    "Ostbahnhof" to R.drawable.ostbahnhof,
    "Wohnzimmer" to R.drawable.wonhzimmer,
    "Hafenstadt" to R.drawable.hafenstadt,
    "Lendcafe" to R.drawable.lendcafe,
    "Free Parking" to R.drawable.free_parking,
    "City Arkaden" to R.drawable.city_arkaden,
    "Le Burger" to R.drawable.le_burger,
    "McMullens" to R.drawable.mc_mullens,
    "Westbahnhof" to R.drawable.westbahnhof,
    "Mensa" to R.drawable.mensa,
    "Universität Klagenfurt" to R.drawable.uni,
    "Stadtwerke Klagenfurt" to R.drawable.stadtwerke,
    "Lakeside" to R.drawable.lakeside,
    "Go To Jail" to R.drawable.gotojail,
    "Strandbad" to R.drawable.strandbad,
    "Loretto" to R.drawable.loretto,
    "Villa Lido" to R.drawable.villa_lido,
    "Lendbahnhof" to R.drawable.lendbahnhof,
    "Botanischer Garten" to R.drawable.botanischer_garten,
    "Kreuzbergl" to R.drawable.kreuzbegl,
    "Heiligengeistplatz" to R.drawable.heiligengeistplatz
)

fun getFieldImageMapping(fieldName: String): Int? {
    return fieldImageMappings[fieldName.trim()]
}

private fun fieldItemContainerMod(bounds: FieldBounds): Modifier {
    val borderWidth = if (bounds.isCorner) 0.75.dp else 0.5.dp
    val borderAlpha = if (bounds.isCorner) 0.5f else 0.4f
    val backgroundColor = Color.White

    return Modifier
        .offset(x = bounds.x.dp, y = bounds.y.dp)
        .size(bounds.width.dp, bounds.height.dp)
        .clip(RectangleShape)
        .border(borderWidth, Color.Black.copy(alpha = borderAlpha))
        .background(backgroundColor)
}

@Composable
private fun FieldImage(
    imageRes: Int?,
    fieldName: String,
    bounds: FieldBounds
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
            .background(Color.White)
            .border(borderWidth, Color.White, imageShape)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = fieldName,
            modifier = Modifier
                .fillMaxSize()
                .padding(imagePadding),
            contentScale = ContentScale.Fit
        )
    }
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
