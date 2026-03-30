package at.aau.serg.websocketbrokerdemo.GameboardUI


import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

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
