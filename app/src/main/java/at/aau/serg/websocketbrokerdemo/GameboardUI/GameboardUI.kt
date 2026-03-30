package at.aau.serg.websocketbrokerdemo.GameboardUI


import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.serg.websocketdemoserver.model.field.Field
import com.example.myapplication.R

@Composable
fun LockScreenOrientation(orientation: Int){    // lock screen orientation in landscape
    val context= LocalContext.current           //LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
if(!LocalInspectionMode.current){
    DisposableEffect(orientation){
        val activity=context as? Activity
        activity?.requestedOrientation=orientation
        onDispose {  }
        }
    }
}
@Composable
fun ZoomableWrapper(modifier: Modifier = Modifier, content: @Composable () -> Unit){        //Container for zoom
    var scale by remember { mutableStateOf(1f) }            //sets Zoom
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier=modifier
            .clip(RectangleShape)
            .background(Color.Black)
            .pointerInput(Unit){
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale=scale*zoom.coerceIn(1f,5f)
                    val maxX=(size.width.toFloat() * (newScale-1)) / 2      //max drag calculation
                    val maxY=(size.height.toFloat() * (newScale-1)) / 2

                    scale=newScale
                    if(scale>1f){
                        val newOffset=offset+pan
                        offset= Offset(
                            x=newOffset.x.coerceIn(-maxX,maxX),     //if zoomed, drag is limited
                            y=newOffset.y.coerceIn(-maxY,maxY)
                        )
                    } else{
                        offset=Offset.Zero
                    }


                }

            }
    ){
        Box(
            modifier=Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ), contentAlignment = Alignment.Center
        ){
            content()
        }
    }

}
@Composable
fun GameboardContent(
    fields:List<Field>,
    modifier:Modifier=Modifier
){
    ZoomableWrapper(modifier=modifier.fillMaxSize()) {
        Box(modifier=Modifier.fillMaxSize()
            .aspectRatio(3840f/2160f),
            contentAlignment=Alignment.Center
        ){
            BoxWithConstraints(modifier=Modifier.fillMaxSize()) {
                val sw=this.maxWidth.value
                val sh=this.maxHeight.value

                Image(
                    painter = painterResource(id=R.drawable.inskapedownscalewebp),
                    contentDescription = "Klagenfurt-Map",
                    modifier=Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Image(
                    painter = painterResource(id=R.drawable.pathreworked),
                    contentDescription = "Path - Klagenfurt-Ring",
                    modifier=Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                fields.forEachIndexed{index,field ->
                    FieldItem(index,field,sw,sh)
                }
            }
        }
    }
}

@Composable
fun FieldItem(index:Int,field:Field,sw:Float,sh:Float){     //function to generate Fields
    val side = (index/10)%4
    val posInSide = index%10
    val isCorner = posInSide == 0


    //Center(1845/1120)
        val corners = listOf(
            Offset(2445f,1720f),
            Offset(1245f,1720f),
            Offset(1245f,520f),
            Offset(2445f,520f)

        )

        val start = corners[side]           //range between corners
        val end =corners[(side + 1) % 4]

        val designCornerSize = 240f

        fun scaleX(x: Float) = (x / 3840f) * sw
        fun scaleY(y: Float) = (y / 2160f) * sh

        if(isCorner){
            val textRotation = when(index){
                0 -> -45f
                10 -> 45f
                20 -> 135f
                30 -> 225f
                else -> 0f
            }
            Box(                                    //corner boxes
                modifier = Modifier
                    .offset(x = (scaleX(start.x) - scaleX(designCornerSize) / 2).dp, y = (scaleY(start.y) - scaleY(designCornerSize) / 2).dp)
                    .size(scaleX(designCornerSize).dp, scaleY(designCornerSize).dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f))
                    .background(Color.Red.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ){

                    Text(
                        text = field.name,
                        modifier = Modifier.rotate(textRotation),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 6.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            hyphens = Hyphens.Auto
                        ),
                        overflow = TextOverflow.Clip
                    )




            }






        } else{
            //TO-DO
            //Implement Logic for other fields
        }





}
