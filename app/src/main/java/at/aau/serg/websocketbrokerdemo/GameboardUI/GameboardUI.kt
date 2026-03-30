package at.aau.serg.websocketbrokerdemo.GameboardUI


import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

