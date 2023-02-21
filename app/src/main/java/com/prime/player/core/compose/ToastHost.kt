package com.prime.player.core.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prime.player.common.composable
import com.prime.player.core.compose.ToastHostState.Data
import com.prime.player.core.compose.ToastHostState.Duration
import com.primex.core.Text
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.prime.player.core.compose.ToastHostState.Result
import com.primex.core.obtain
import com.primex.ui.IconButton
import com.primex.ui.Label
import com.primex.ui.ListTile
import com.primex.ui.TextButton
import kotlinx.coroutines.delay
import kotlin.coroutines.resume

/**
 * State of the [ToastHost], controls the queue and the current [ToastHostState] being shown inside
 * the [ToastHost].
 *
 * This state usually lives as a part of a [ScaffoldState] and provided to the [ToastHost]
 * automatically, but can be decoupled from it and live separately when desired.
 *
 * This class also serves as the default for [ToastHostState]
 */
@Stable
class ToastHostState {
    /**
     * Possible durations of the [ToastHostState] in [ToastHost]
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
     * Interface to represent one particular [ToastHostState] as a piece of the [ToastHostState] State.
     *
     * @property message text to be shown in the [ToastHostState]
     * @property label optional action label to show as button in the Toast
     * @property duration duration of the toast
     * @property accent The accent color of this [ToastHostState]. Default [Color.Unspecified]
     * @property leading optional leading icon for [ToastHostState]. Default null. The leading must be a vector icon or resource drawbale.
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
         * [ToastHostState] that is shown has been dismissed either by timeout of by user
         */
        Dismissed,

        /**
         * Action on the [ToastHostState] has been clicked before the time out passed
         */
        ActionPerformed,
    }


    /**
     * Only one [ToastHostState] can be shown at a time.
     * Since a suspending Mutex is a fair queue, this manages our message queue
     * and we don't have to maintain one.
     */
    private val mutex = Mutex()

    /**
     * The current [ToastHostState.Data] being shown by the [ToastHostState], of `null` if none.
     */
    var current by mutableStateOf<Data?>(null)
        private set

    /**
     * Shows or queues to be shown a [ToastHostState] at the bottom of the [Scaffold] at
     * which this state is attached and suspends until toast is disappeared.
     *
     * [ToastHostState] state guarantees to show at most one toast at a time. If this function is
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
     * @param duration duration to control how long toast will be shown in [ToastHost], either
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
                current =
                    ToastDataImpl(message, title, accent, leading, label, duration, continuation)
            }
        } finally {
            current = null
        }
    }

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
private class ToastDataImpl(
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

/**
 * @see [ToastHostState.show]
 */
suspend fun ToastHostState.show(
    message: String,
    title: String? = null,
    label: String? = null,
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
 * @see [ToastHostState.show]
 */
suspend fun ToastHostState.show(
    message: AnnotatedString,
    title: AnnotatedString? = null,
    label: AnnotatedString? = null,
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

// TODO: magic numbers adjustment
private fun Duration.toMillis(
    hasAction: Boolean, accessibilityManager: AccessibilityManager?
): Long {
    val original = when (this) {
        Duration.Short -> 4000L
        Duration.Long -> 10000L
        Duration.Indefinite -> Long.MAX_VALUE
    }
    if (accessibilityManager == null) {
        return original
    }
    return accessibilityManager.calculateRecommendedTimeoutMillis(
        original, containsIcons = true, containsText = true, containsControls = hasAction
    )
}

// TODO: to be replaced with the public customizable implementation
// it's basically tweaked nullable version of Crossfade
@Composable
private fun FadeInFadeOutWithScale(
    current: Data?, modifier: Modifier = Modifier, content: @Composable (Data) -> Unit
) {
    val state = remember { FadeInFadeOutState<Data?>() }
    if (current != state.current) {
        state.current = current
        val keys = state.items.map { it.key }.toMutableList()
        if (!keys.contains(current)) {
            keys.add(current)
        }
        state.items.clear()
        keys.filterNotNull().mapTo(state.items) { key ->
            FadeInFadeOutAnimationItem(key) { children ->
                val isVisible = key == current
                val duration = if (isVisible) ToastFadeInMillis else ToastFadeOutMillis
                val delay = ToastFadeOutMillis + ToastInBetweenDelayMillis
                val animationDelay = if (isVisible && keys.filterNotNull().size != 1) delay else 0
                val opacity = animatedOpacity(animation = tween(
                    easing = LinearEasing,
                    delayMillis = animationDelay,
                    durationMillis = duration
                ), visible = isVisible, onAnimationFinish = {
                    if (key != state.current) {
                        // leave only the current in the list
                        state.items.removeAll { it.key == key }
                        state.scope?.invalidate()
                    }
                })
                val scale = animatedScale(
                    animation = tween(
                        easing = FastOutSlowInEasing,
                        delayMillis = animationDelay,
                        durationMillis = duration
                    ), visible = isVisible
                )
                Box(
                    Modifier
                        .graphicsLayer(
                            scaleX = scale.value, scaleY = scale.value, alpha = opacity.value
                        )
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            dismiss { key.dismiss(); true }
                        }) {
                    children()
                }
            }
        }
    }
    Box(modifier) {
        state.scope = currentRecomposeScope
        state.items.forEach { (item, opacity) ->
            key(item) {
                opacity {
                    content(item!!)
                }
            }
        }
    }
}

private class FadeInFadeOutState<T> {
    // we use Any here as something which will not be equals to the real initial value
    var current: Any? = Any()
    var items = mutableListOf<FadeInFadeOutAnimationItem<T>>()
    var scope: RecomposeScope? = null
}

private data class FadeInFadeOutAnimationItem<T>(
    val key: T, val transition: FadeInFadeOutTransition
)

private typealias FadeInFadeOutTransition = @Composable (content: @Composable () -> Unit) -> Unit

@Composable
private fun animatedOpacity(
    animation: AnimationSpec<Float>, visible: Boolean, onAnimationFinish: () -> Unit = {}
): State<Float> {
    val alpha = remember { Animatable(if (!visible) 1f else 0f) }
    LaunchedEffect(visible) {
        alpha.animateTo(
            if (visible) 1f else 0f, animationSpec = animation
        )
        onAnimationFinish()
    }
    return alpha.asState()
}

@Composable
private fun animatedScale(animation: AnimationSpec<Float>, visible: Boolean): State<Float> {
    val scale = remember { Animatable(if (!visible) 1f else 0.8f) }
    LaunchedEffect(visible) {
        scale.animateTo(
            if (visible) 1f else 0.8f, animationSpec = animation
        )
    }
    return scale.asState()
}

private const val ToastFadeInMillis = 150
private const val ToastFadeOutMillis = 75
private const val ToastInBetweenDelayMillis = 0

private inline fun Indicatior(color: Color) = Modifier.drawBehind {
    drawRect(color = color, size = size.copy(width = 4.dp.toPx()))
}


@Composable
fun Toast(
    data: Data,
    modifier: Modifier = Modifier,
    //actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = ToastHostState.backgroundColor,
    contentColor: Color = contentColorFor(backgroundColor = backgroundColor),
    actionColor: Color = data.accent.takeOrElse { ToastHostState.primaryActionColor },
    elevation: Dp = 6.dp,
) {
    Surface(
        // fill whole width and add some padding.
        modifier = modifier
            .padding(12.dp)
            .sizeIn(minHeight = 56.dp),
        shape = shape,
        elevation = elevation,
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        ListTile(
            // draw the indicator.
            modifier = Indicatior(actionColor), centreVertically = true,

            leading = composable(data.leading != null) {
                // TODO: It might casue the problems.
                val icon = data.leading
                Icon(
                    painter = when (icon) {
                        is ImageVector -> rememberVectorPainter(image = icon)
                        is Int -> painterResource(id = icon)
                        else -> error("$icon is neither resource nor ImageVector.")
                    }, contentDescription = null, tint = actionColor
                )
            },

            // the title
            text = {
                Label(
                    text = data.message.obtain,
                    color = LocalContentColor.current.copy(ContentAlpha.medium),
                    style = MaterialTheme.typography.caption,
                    maxLines = 2,
                )
            },

            overlineText = composable(data.title != null) {
                Label(
                    text = data.title!!.obtain,
                    color = LocalContentColor.current.copy(ContentAlpha.high),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold
                )
            },

            trailing = {
                if (data.label != null)
                    TextButton(
                        label = data.label!!.obtain,
                        onClick = { data.action() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = actionColor
                        )
                    )
                else
                    IconButton(
                        onClick = { data.dismiss() },
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null
                    )
            }
        )
    }
}


/**
 * Host for [ToastHostState]s to be used in [Scaffold] to properly show, hide and dismiss items based
 * on material specification and the [state].
 *
 * This component with default parameters comes build-in with [Scaffold], if you need to show a
 * default [ToastHostState], use use [ScaffoldState.snackbarHostState] and
 * [SnackbarHostState.showSnackbar].
 *
 * @sample androidx.compose.material.samples.ScaffoldWithSimpleSnackbar
 *
 * If you want to customize appearance of the [ToastHostState], you can pass your own version as a child
 * of the [ToastHost] to the [Scaffold]:
 *
 * @sample androidx.compose.material.samples.ScaffoldWithCustomSnackbar
 *
 * @param state state of this component to read and show [ToastHostState]s accordingly
 * @param modifier optional modifier for this component
 * @param toast the instance of the [ToastHostState] to be shown at the appropriate time with
 * appearance based on the [Data] provided as a param
 */
@Composable
fun ToastHost(
    state: ToastHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (Data) -> Unit = { Toast(it) }
) {
    val currentSnackbarData = state.current
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            val duration = currentSnackbarData.duration.toMillis(
                currentSnackbarData.label != null, accessibilityManager
            )
            delay(duration)
            currentSnackbarData.dismiss()
        }
    }
    FadeInFadeOutWithScale(
        current = state.current, modifier = modifier, content = snackbar
    )
}