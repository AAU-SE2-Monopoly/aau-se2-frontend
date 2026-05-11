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
    private var currentTopic: String? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Subscribe to [baseTopic] with an optional [topicSuffix].
     *
     * Repeated calls with the same topic are ignored while an active job exists.
     * If the topic changes, the current subscription is cancelled and replaced.
     */
    fun subscribe(topicSuffix: String? = null) {
        val fullTopic = if (topicSuffix != null) "$baseTopic$topicSuffix" else baseTopic

        if (job?.isActive == true && currentTopic == fullTopic) {
            Log.w("SubscriptionChannel", "subscribe skipped – already subscribed to $fullTopic")
            return
        }

        cancel()
        _isReady.value = false
        currentTopic = fullTopic

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
                currentTopic = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _isReady.value = false
        currentTopic = null
    }

    private fun isCancellation(e: Throwable): Boolean {
        return e is kotlinx.coroutines.CancellationException ||
                (e is IllegalStateException && e.message?.contains("cancelled", ignoreCase = true) == true) ||
                (e.cause is kotlinx.coroutines.CancellationException)
    }
}