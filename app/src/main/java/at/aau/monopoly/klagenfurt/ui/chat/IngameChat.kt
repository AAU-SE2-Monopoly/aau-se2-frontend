package at.aau.monopoly.klagenfurt.ui.chat

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import at.aau.monopoly.klagenfurt.ServiceLocator

@Composable
fun ChatOverlay(
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.ChatViewModelFactory(ServiceLocator.provideChatService())
    )
) {

}
