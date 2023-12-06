@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.core.compose

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.AsyncUpdates
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.media.R
import com.prime.media.core.LongDurationMills
import com.primex.material2.Label
import com.primex.material2.Placeholder
import kotlin.math.roundToLong

@Composable
inline fun Artwork(
    data: Any?,
    modifier: Modifier = Modifier,
    fallback: Painter? = painterResource(id = R.drawable.default_art),
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
@Deprecated("The reason for deprication of this is that it doesnt morph for all window sizes.")
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

/**
 * A composable function that delegates to [LottieAnimation] and behaves like [AndroidVectorDrawable].
 *
 * @param atEnd: A boolean parameter that determines whether to display the end-frame or the start
 *              frame of the animation. The change in value causes animation.
 * @param id: The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param scale: A float parameter that adjusts the size of the animation. The default size is
 *               24.dp, and the scale can be used to increase or decrease it.
 * @param progressRange: A range of float values that specifies the start and end frames of the
 *                       animation. The default range is 0f..1f, which means the animation will
 *                       start from the first frame and end at the last frame. Some [Lottie]
 *                       animation files may have different start/end frames, and this parameter
 *                       can be used to adjust them accordingly.
 * @param duration: The duration of the animation in milliseconds. The default value is -1, which
 *                  means the animation will use the duration specified in the
 *                  [LottieCompositionSpec] object. If a positive value is given, it will override
 *                  the duration from the [LottieCompositionSpec] object.
 */
@Composable
inline fun LottieAnimation(
    @RawRes id: Int,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    scale: Float = 1f,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    duration: Int = -1,
    easing: Easing = FastOutSlowInEasing
) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val duration2 = composition?.duration?.roundToLong() ?: AnimationConstants.LongDurationMills
    val progress by animateFloatAsState(
        targetValue = if (atEnd) progressRange.start else progressRange.endInclusive,
        label = "Lottie $id",
        animationSpec = tween(if (duration == -1) duration2.toInt() else duration, easing = easing)
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .size(24.dp)
            .scale(scale)
            .then(modifier),
    )
}


/**
 * A Delegate to [LottieAnimation] that takes [RawRes] id as parameter.
 * @param scale A float parameter that adjusts the size of the animation. The default size is
 *              24.dp, and the scale can be used to increase or decrease it.
 * @see LottieAnimation
 */
@Composable
inline fun LottieAnimation(
    @RawRes id: Int,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    isPlaying: Boolean = true,
    restartOnPlay: Boolean = true,
    clipSpec: LottieClipSpec? = null,
    speed: Float = 1f,
    iterations: Int = 1,
    outlineMasksAndMattes: Boolean = false,
    applyOpacityToLayers: Boolean = false,
    enableMergePaths: Boolean = false,
    renderMode: RenderMode = RenderMode.AUTOMATIC,
    reverseOnRepeat: Boolean = false,
    maintainOriginalImageBounds: Boolean = false,
    dynamicProperties: LottieDynamicProperties? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    clipToCompositionBounds: Boolean = true,
    fontMap: Map<String, Typeface>? = null,
    asyncUpdates: AsyncUpdates = AsyncUpdates.AUTOMATIC
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(id)
    )
    LottieAnimation(
        composition,
        Modifier
            .size(24.dp)
            .scale(scale)
            .then(modifier),
        isPlaying,
        restartOnPlay,
        clipSpec,
        speed,
        iterations,
        outlineMasksAndMattes,
        applyOpacityToLayers,
        enableMergePaths,
        renderMode,
        reverseOnRepeat,
        maintainOriginalImageBounds,
        dynamicProperties,
        alignment,
        contentScale,
        clipToCompositionBounds,
        fontMap,
        asyncUpdates
    )
}

/**
 * A composable function that creates a [LottieAnimation] [IconButton] with the given resource
 * identifier of the [LottieCompositionSpec.RawRes] type. The [LottieAnimation] renders an Adobe
 * After Effects animation exported as JSON on the screen, and the [IconButton] provides a clickable
 * area around it.
 *
 * @param id: The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param onClick: A lambda function that is invoked when the user clicks on the button.
 * @see LottieAnimation for more details about how to render a [Lottie] animation.
 * @see IconButton for more details about how to create a button with an icon.
 */
@Composable
inline fun LottieAnimButton(
    @RawRes id: Int,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    scale: Float = 1f,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    duration: Int = -1,
    enabled: Boolean = true,
    easing: Easing = FastOutSlowInEasing,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    IconButton(
        onClick = onClick,
        modifier,
        enabled,
        interactionSource,
        content = {
            LottieAnimation(
                id = id,
                atEnd = atEnd,
                scale = scale,
                easing = easing,
                progressRange = progressRange,
                duration = duration
            )
        }
    )
}


/**
 * A composable function that creates a [rememberAnimatedVectorResource] [IconButton] with the given
 * resource identifier and the [IconButton] provides a clickable  area around it.
 *
 * @param id: The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param onClick: A lambda function that is invoked when the user clicks on the button.
 * @see rememberAnimatedVectorResource for more details about how to render a [Lottie] animation.
 * @param atEnd: A boolean parameter that determines whether to display the end-frame or the start
 *              frame of the animation. The change in value causes animation.
 * @param id: The resource identifier of the [AnimatedVectorResource] type.
 * @param scale: A float parameter that adjusts the size of the animation. The default size is
 *               size of the icon drawable and the scale can be used to increase or decrease it.
 * @see IconButton for more details about how to create a button with an icon.
 * @see rememberAnimatedVectorResource
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
inline fun AnimatedIconButton(
    @DrawableRes id: Int,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    atEnd: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tint: Color = Color.Unspecified
) {
    IconButton(onClick = onClick, modifier = modifier, enabled, interactionSource, ) {
        Icon(
            painter = rememberAnimatedVectorResource(id = id, atEnd = atEnd),
            modifier = Modifier.scale(scale),
            contentDescription = null,
            tint = tint
        )
    }
}