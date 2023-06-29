package com.prime.media.core.compose.channel

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prime.media.core.compose.composable
import com.prime.media.core.compose.channel.Channel.Data
import com.primex.core.get
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.TextButton
import kotlinx.coroutines.delay


// TODO: magic numbers adjustment
private fun Channel.Duration.toMillis(
    hasAction: Boolean, accessibilityManager: AccessibilityManager?
): Long {
    val original = when (this) {
        Channel.Duration.Short -> 4000L
        Channel.Duration.Long -> 10000L
        Channel.Duration.Indefinite -> Long.MAX_VALUE
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
fun Message(
    data: Data,
    modifier: Modifier = Modifier,
    //actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = Channel.backgroundColor,
    contentColor: Color = contentColorFor(backgroundColor = backgroundColor),
    actionColor: Color = data.accent.takeOrElse { Channel.primaryActionColor },
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
                    text = data.message.get,
                    color = LocalContentColor.current.copy(ContentAlpha.medium),
                    style = MaterialTheme.typography.caption,
                    maxLines = 2,
                )
            },

            overlineText = composable(data.title != null) {
                Label(
                    text = data.title!!.get,
                    color = LocalContentColor.current.copy(ContentAlpha.high),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold
                )
            },

            trailing = {
                if (data.label != null)
                    TextButton(
                        label = data.label!!.get,
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
 * Host for [Channel]s to be used in [Scaffold] to properly show, hide and dismiss items based
 * on material specification and the [state].
 *
 * This component with default parameters comes build-in with [Scaffold], if you need to show a
 * default [Channel], use use [ScaffoldState.snackbarHostState] and
 * [SnackbarHostState.showSnackbar].
 *
 * @sample androidx.compose.material.samples.ScaffoldWithSimpleSnackbar
 *
 * If you want to customize appearance of the [Channel], you can pass your own version as a child
 * of the [Channel] to the [Scaffold]:
 *
 * @sample androidx.compose.material.samples.ScaffoldWithCustomSnackbar
 *
 * @param state state of this component to read and show [Channel]s accordingly
 * @param modifier optional modifier for this component
 * @param toast the instance of the [Channel] to be shown at the appropriate time with
 * appearance based on the [Data] provided as a param
 */
@Composable
fun Channel(
    state: Channel,
    modifier: Modifier = Modifier,
    snackbar: @Composable (Data) -> Unit = { Message(it) }
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