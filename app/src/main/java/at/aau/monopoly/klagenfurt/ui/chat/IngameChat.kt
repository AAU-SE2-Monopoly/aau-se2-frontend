package at.aau.monopoly.klagenfurt.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import at.aau.monopoly.klagenfurt.ServiceLocator
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.frame.FrameBody

@Composable
fun ChatOverlay(
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.ChatViewModelFactory(ServiceLocator.provideChatService(),ServiceLocator.provideGameService())
    )
) {
    Box(modifier= Modifier.background(Color.Blue).border(2.dp,Color.Black).width(100.dp))
    {

    }

}
fun observeViewModel(viewModel: ChatViewModel){
    val scope=viewModel.viewModelScope
    scope.launch {
        viewModel.messageFlow.collect{

        }
    }
}
