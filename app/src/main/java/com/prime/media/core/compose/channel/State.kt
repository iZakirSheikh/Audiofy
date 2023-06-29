package com.prime.media.core.compose.channel

import androidx.annotation.StringRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.ResourcesCompat
import com.prime.media.core.compose.channel.Channel.Data
import com.prime.media.core.compose.channel.Channel.Duration
import com.prime.media.core.compose.channel.Channel.Result
import com.primex.core.Text
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * State of the [Channel], controls the queue and the current [Channel] being shown inside
 * the [Channel].
 *
 * This state usually lives as a part of a [ScaffoldState] and provided to the [Channel]
 * automatically, but can be decoupled from it and live separately when desired.
 *
 * This class also serves as the default for [Channel]
 */
@Stable
class Channel {
    /**
     * Possible durations of the [Channel] in [Channel]
     */
    enum class Duration {
        /**
         * Show the Toast for a short period of time
         */
        Short,

        /**
         * Show the Toast for a long period of time
         */
        Long,

        /**
         * Show the Toast indefinitely until explicitly dismissed or action is clicked
         */
        Indefinite
    }

    /**
     * Interface to represent one particular [Channel] as a piece of the [Channel] State.
     *
     * @property message text to be shown in the [Channel]
     * @property label optional action label to show as button in the Toast
     * @property duration duration of the toast
     * @property accent The accent color of this [Channel]. Default [Color.Unspecified]
     * @property leading optional leading icon for [Channel]. Default null. The leading must be a vector icon or resource drawbale.
     */
    interface Data {

        val accent: Color
        val leading: Any?

        val title: Text?

        val message: Text
        val label: Text?
        val duration: Duration

        /**
         * Function to be called when Toast action has been performed to notify the listeners
         */
        fun action()

        /**
         * Function to be called when Toast is dismissed either by timeout or by the user
         */
        fun dismiss()
    }

    /**
     * Possible results of the [SnackbarHostState.showSnackbar] call
     */
    enum class Result {
        /**
         * [Channel] that is shown has been dismissed either by timeout of by user
         */
        Dismissed,

        /**
         * Action on the [Channel] has been clicked before the time out passed
         */
        ActionPerformed,
    }


    /**
     * Only one [Channel] can be shown at a time.
     * Since a suspending Mutex is a fair queue, this manages our message queue
     * and we don't have to maintain one.
     */
    private val mutex = Mutex()

    /**
     * The current [Channel.Data] being shown by the [Channel], of `null` if none.
     */
    var current by mutableStateOf<Data?>(null)
        private set

    /**
     * Shows or queues to be shown a [Channel] at the bottom of the [Scaffold] at
     * which this state is attached and suspends until toast is disappeared.
     *
     * [Channel] state guarantees to show at most one toast at a time. If this function is
     * called while another toast is already visible, it will be suspended until this toast
     * is shown and subsequently addressed. If the caller is cancelled, the toast will be
     * removed from display and/or the queue to be displayed.
     *
     * All of this allows for granular control over the toast queue from within:
     *
     * @sample androidx.compose.material.samples.ScaffoldWithCoroutinesSnackbar
     *
     * To change the toast appearance, change it in 'ToastHost' on the [Scaffold].
     *
     * @param message text to be shown in the Toast
     * @param label optional action label to show as button in the Toast
     * @param duration duration to control how long toast will be shown in [Channel], either
     * [Duration.Short], [Duration.Long] or [Duration.Indefinite].
     * @param accent: option accent color, default upto implementation.
     * @param leading Optional leading icon to be displayed. Must be either [ImageVector] or [Drawble] resource.
     * @return [SnackbarResult.ActionPerformed] if option action has been clicked or
     * [SnackbarResult.Dismissed] if snackbar has been dismissed via timeout or by the user
     */
    suspend fun show(
        message: Text,
        title: Text? = null,
        label: Text? = null,
        leading: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
    ): Result = mutex.withLock {
        try {
            return suspendCancellableCoroutine { continuation ->
                current = Message(message, title, accent, leading, label, duration, continuation)
            }
        } finally {
            current = null
        }
    }


    /**
     * @see [Channel.show]
     */
    suspend fun show(
        message: CharSequence,
        title: CharSequence? = null,
        label: CharSequence? = null,
        leading: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
    ) = show(
        Text(message),
        title?.let { Text(it) },
        label = label?.let { Text(it) },
        leading = leading,
        accent = accent,
        duration = duration
    )

    /**
     * Show a Toast Message with string resource.
     * ** Note: The html version isn't supported.
     */
    suspend fun show(
        @StringRes message: Int,
        @StringRes title: Int = ResourcesCompat.ID_NULL,
        @StringRes label: Int = ResourcesCompat.ID_NULL,
        leading: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
    ) = show(
        title = if (title == ResourcesCompat.ID_NULL) null else Text(title),
        message = Text(message),
        label = if (label == ResourcesCompat.ID_NULL) null else Text(label),
        leading = leading,
        accent = accent,
        duration = duration
    )

    companion object {

        /**
         * Default alpha of the overlay applied to the [backgroundColor]
         */
        private const val SnackbarOverlayAlpha = 0.8f

        /**
         * Default background color of the [Snackbar]
         */
        val backgroundColor: Color
            @Composable get() = MaterialTheme.colors.surface

        /**
         * Provides a best-effort 'primary' color to be used as the primary color inside a [Snackbar].
         * Given that [Snackbar]s have an 'inverted' theme, i.e. in a light theme they appear dark, and
         * in a dark theme they appear light, just using [Colors.primary] will not work, and has
         * incorrect contrast.
         *
         * If your light theme has a corresponding dark theme, you should instead directly use
         * [Colors.primary] from the dark theme when in a light theme, and use
         * [Colors.primaryVariant] from the dark theme when in a dark theme.
         *
         * When in a light theme, this function applies a color overlay to [Colors.primary] from
         * [MaterialTheme.colors] to attempt to reduce the contrast, and when in a dark theme this
         * function uses [Colors.primaryVariant].
         */
        val primaryActionColor: Color
            @Composable get() = MaterialTheme.colors.primary
    }

}

@Stable
private class Message(
    override val message: Text,
    override val title: Text? = null,
    override val accent: Color = Color.Unspecified,
    override val leading: Any? = null,
    override val label: Text? = null,
    override val duration: Duration = Duration.Indefinite,
    private val continuation: CancellableContinuation<Result>,
) : Data {

    override fun action() {
        if (continuation.isActive) continuation.resume(Result.ActionPerformed)
    }

    override fun dismiss() {
        if (continuation.isActive) continuation.resume(Result.Dismissed)
    }
}
