@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.core.compose

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.*
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.media.R
import com.primex.material2.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
inline fun Image(
    data: Any?,
    modifier: Modifier = Modifier,
    fallback: Painter = painterResource(id = R.drawable.default_art),
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    fadeMills: Int = AnimationConstants.DefaultDurationMillis,
) {
    val context = LocalContext.current
    val request = remember(data) {
        ImageRequest.Builder(context).data(data).crossfade(fadeMills).build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        error = fallback,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

//This file holds the simple extension, utility methods of compose.
/**
 * Composes placeholder with lottie icon.
 */
@Composable
inline fun Placeholder(
    title: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: String? = null,
    noinline action: @Composable (() -> Unit)? = null
) {
    Placeholder(
        modifier = modifier,
        vertical = vertical,
        message = { if (message != null) Text(text = message) },
        title = { Label(text = title.ifEmpty { " " }, maxLines = 2) },

        icon = {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(
                    iconResId
                )
            )
            LottieAnimation(
                composition = composition, iterations = Int.MAX_VALUE
            )
        },
        action = action,
    )
}

@ExperimentalAnimationApi
@Composable
@Deprecated("Doesn't required.", level = DeprecationLevel.HIDDEN)
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    initiallyVisible: Boolean,
    content: @Composable () -> Unit
) = AnimatedVisibility(visibleState = remember { MutableTransitionState(initiallyVisible) }.apply {
    targetState = visible
}, modifier = modifier, enter = enter, exit = exit
) {
    content()
}

@ExperimentalAnimationGraphicsApi
@Composable
inline fun rememberAnimatedVectorResource(@DrawableRes id: Int, atEnd: Boolean) =
    androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = id), atEnd = atEnd
    )


context (ViewModel) @Suppress("NOTHING_TO_INLINE")
@Deprecated("find new solution.")
inline fun <T> Flow<T>.asComposeState(initial: T): State<T> {
    val state = mutableStateOf(initial)
    onEach { state.value = it }.launchIn(viewModelScope)
    return state
}

/**
 * A variant of caption.
 */
private val caption2 =
    TextStyle(fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.4.sp)

/**
 * A variant of [caption] with a smaller font size and tighter letter spacing.
 * Use this style for captions that require less emphasis or in situations where space is constrained.
 *
 * @see caption
 */
val Typography.caption2 get() = com.prime.media.core.compose.caption2

/**
 * The alpha value for the container colors.
 *
 * This constant value represents the alpha (transparency) of the container colors in the current
 * [MaterialTheme]. The value is a Float between 0.0 and 1.0, where 0.0 is completely transparent
 * and 1.0 is completely opaque. This value can be used to adjust the transparency of container
 * backgrounds and other elements in your app that use the container color.
 */
val MaterialTheme.CONTAINER_COLOR_ALPHA get() = 0.15f

/**
 * A variant of [MaterialTheme.shapes.small] with a corner radius of 8dp.
 */
private val small2 = RoundedCornerShape(8.dp)

/**
 * A variant of [MaterialTheme.shapes.small] with a radius of 8dp.
 */
val Shapes.small2 get() = com.prime.media.core.compose.small2

/**
 * This Composable function provides a primary container color with reduced emphasis as compared to
 * the primary color.
 * It is used for styling elements that require a less prominent color.
 *
 * The color returned by this function is derived from the primary color of the current
 * MaterialTheme with an alpha value equal to [MaterialTheme.CONTAINER_COLOR_ALPHA].
 *
 * @return a [Color] object representing the primary container color.
 */
val Colors.primaryContainer
    @Composable inline get() = MaterialTheme.colors.primary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * Returns a color that is suitable for content (icons, text, etc.) that sits on top of the primary container color.
 * This color is simply the primary color of the current theme.
 *
 * @return [Color] object that represents the on-primary container color
 */
val Colors.onPrimaryContainer
    @Composable inline get() = MaterialTheme.colors.primary

/**
 * Secondary container is applied to elements needing less emphasis than secondary
 */
val Colors.secondaryContainer
    @Composable inline get() = MaterialTheme.colors.secondary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * On-secondary container is applied to content (icons, text, etc.) that sits on top of secondary
 * container
 */
val Colors.onSecondaryContainer @Composable inline get() = MaterialTheme.colors.secondary

/**
 * Error container is applied to elements associated with an error state
 */
val Colors.errorContainer
    @Composable inline get() = MaterialTheme.colors.error.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * On-error container is applied to content (icons, text, etc.) that sits on top of error container
 */
val Colors.onErrorContainer @Composable inline get() = MaterialTheme.colors.error

/**
 * The overlay color used for backgrounds and shadows.
 * The color is black with alpha 0.04 on light themes and white with alpha 0.04 on dark themes.
 */
val Colors.overlay
    @Composable inline get() = (if (isLight) Color.Black else Color.White).copy(0.04f)

/**
 * The outline color used in the light/dark theme.
 *
 * The color is semi-transparent white/black, depending on the current theme, with an alpha of 0.12.
 */
inline val Colors.outline
    get() = (if (isLight) Color.Black else Color.White).copy(0.12f)
val Colors.onOverlay
    @Composable inline get() = (MaterialTheme.colors.onBackground).copy(alpha = ContentAlpha.medium)
val Colors.lightShadowColor
    @Composable inline get() = if (isLight) Color.White else Color.White.copy(0.025f)
val Colors.darkShadowColor
    @Composable inline get() = if (isLight) Color(0xFFAEAEC0).copy(0.7f) else Color.Black.copy(0.6f)