package at.aau.monopoly.klagenfurt.networking

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText

class SubscriptionChannel(
    private val scope: CoroutineScope,
    private val session: () -> StompSession?,
    private val baseTopic: String,
    private val events: MutableSharedFlow<String>,
    private val onError: ((Throwable) -> Unit)? = null
) {
    private var job: Job? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Subscribe to [baseTopic] with an optional [topicSuffix].
     *
     * **Important:** If you are switching to a different topic, you **must** call
     * [cancel] first, because the guard below only checks whether a job is running,
     * not whether it's subscribed to the correct topic. See [GameStompClient.subscribeToGameInternal].
     */
    fun subscribe(topicSuffix: String? = null) {
        val fullTopic = if (topicSuffix != null) "$baseTopic$topicSuffix" else baseTopic

        // Caller is responsible for calling cancel() before subscribe() when switching topics
        if (job?.isActive == true) {
            Log.w("SubscriptionChannel", "subscribe skipped – active job already running for topic. Call cancel() first.")
            return
        }

        cancel()
        _isReady.value = false

        job = scope.launch {
            try {
                Log.d("SubscriptionChannel", "Subscribing to $fullTopic")
                val sub = session()?.subscribeText(fullTopic) ?: return@launch
                _isReady.value = true
                sub.collect { events.emit(it) }
            } catch (e: Throwable) {
                if (isCancellation(e)) {
                    Log.d("SubscriptionChannel", "Subscription to $fullTopic cancelled")
                } else {
                    Log.e("SubscriptionChannel", "Error in subscription to $fullTopic", e)
                    onError?.invoke(e)
                }
            } finally {
                _isReady.value = false
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _isReady.value = false
    }

    private fun isCancellation(e: Throwable): Boolean {
        return e is kotlinx.coroutines.CancellationException ||
                (e is IllegalStateException && e.message?.contains("cancelled", ignoreCase = true) == true) ||
                (e.cause is kotlinx.coroutines.CancellationException)
    }
}