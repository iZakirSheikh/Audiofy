/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.audiofy.common.compose

import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottiePainter
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.SystemFacade
import com.zs.compose.foundation.composableIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Colors
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalNavAnimatedVisibilityScope
import com.zs.compose.theme.Placeholder
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.billing.Purchase
import com.zs.preferences.Key
import kotlin.math.roundToInt

private const val TAG = "Delegates"

/**
 * A composable function that delegates to [LottieAnimation] painter and behaves like [AndroidVectorDrawable].
 * This overload animates between the start and end frames of the animation based on the value of `atEnd`.
 *
 * @param id The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param atEnd A boolean parameter that determines whether to display the end-frame or the start
 *              frame of the animation. The change in value causes animation.
 * @param duration The duration of the animation in milliseconds. The default value is -1, which
 *                 means the animation will use the duration specified in the
 *                 [LottieCompositionSpec] object. If a positive value is given, it will override
 *                 the duration from the [LottieCompositionSpec] object.
 * @param progressRange A range of float values that specifies the start and end frames of the
 *                      animation. The default range is 0f..1f, which means the animation will
 *                      start from the first frame and end at the last frame. Some [Lottie]
 *                      animation files may have different start/end frames, and this parameter
 *                      can be used to adjust them accordingly.
 * @param easing The easing function to use for the animation.
 */
@Composable
fun lottieAnimationPainter(
    @RawRes id: Int,
    atEnd: Boolean,
    dynamicProperties: LottieDynamicProperties?= null,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    animationSpec: AnimationSpec<Float>? = null,
): Painter {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val duration2 = composition?.duration?.roundToInt() ?: AnimationConstants.DefaultDurationMillis
    val progress by animateFloatAsState(
        targetValue = if (!atEnd) progressRange.start else progressRange.endInclusive,
        label = "Lottie $id",
        animationSpec = animationSpec ?: tween(duration2)
    )
    return rememberLottiePainter(
        composition = composition,
        dynamicProperties = dynamicProperties,
        progress = progress,
    )
}

/**
 * A composable function that displays a Lottie animation as an icon.
 * This function uses [lottieAnimationPainter] to render the animation and behaves like an [Icon].
 * The animation plays based on the `atEnd` parameter, animating between the start and end frames.
 *
 * @param id The raw resource ID of the Lottie animation file.
 * @param contentDescription Text used by accessibility services to describe what this icon represents.
 * @param modifier The [Modifier] to be applied to this icon.
 * @param atEnd A boolean indicating whether the animation should be at its end state.
 *              When this value changes, the animation transitions between the start and end of the `progressRange`.
 * @param scale A factor to scale the size of the icon. Defaults to 1f (no scaling).
 * @param progressRange A [ClosedFloatingPointRange] specifying the start and end progress values for the animation.
 *                      Defaults to 0f..1f, representing the full animation.
 * @param duration The duration of the animation transition in milliseconds.
 *                 Defaults to -1, which means the duration will be derived from the Lottie composition.
 * @param easing The [Easing] function to be used for the animation transition.
 *               Defaults to [FastOutSlowInEasing].
 */
@Composable
inline fun LottieAnimatedIcon(
    @RawRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    scale: Float = 1f,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    tint: Color = Color.Unspecified,
    animationSpec: AnimationSpec<Float>? = null,
) {
    Icon(
        painter = lottieAnimationPainter(
            id = id,
            atEnd = atEnd,
            progressRange = progressRange,
           animationSpec = animationSpec
        ),
        tint = tint,
        contentDescription = contentDescription,
        modifier = modifier
            .size(24.dp)
            .scale(scale),
    )
}

@Composable
inline fun LottieAnimatedButton(
    @RawRes id: Int,
    noinline onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    scale: Float = 1f,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    tint: Color = Color.Unspecified,
    animationSpec: AnimationSpec<Float>? = null,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = lottieAnimationPainter(
                id = id,
                atEnd = atEnd,
                progressRange = progressRange,
                animationSpec = animationSpec
            ),
            tint = tint,
            contentDescription = contentDescription,
            modifier = modifier
                .size(24.dp)
                .scale(scale),
        )
    }
}

/**
 * A composable function that delegates to [LottieAnimation] painter and behaves like [AndroidVectorDrawable].
 * This overload animates the Lottie composition according to the given parameters.
 *
 * @param id The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param speed The speed at which the animation plays.
 * @param repeatMode The repeat mode for the animation.
 * @param iterations The number of iterations to play the animation.
 * @see lottieAnimationPainter
 */
@Composable
fun lottieAnimationPainter(
    @RawRes id: Int,
    speed: Float = 1f,
    isPlaying: Boolean = true,
    dynamicProperties: LottieDynamicProperties?= null,
    repeatMode: RepeatMode = RepeatMode.Restart,
    iterations: Int = Int.MAX_VALUE,
): Painter {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        isPlaying = isPlaying,
        reverseOnRepeat = repeatMode == RepeatMode.Reverse,
    )
    return rememberLottiePainter(
        composition = composition,
        progress = progress,

        dynamicProperties = dynamicProperties
    )
}

/** Composes placeholder with lottie icon. */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Placeholder(
    title: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: CharSequence? = null,
    noinline action: @Composable (() -> Unit)? = null,
) {
    Placeholder(
        modifier = modifier, vertical = vertical,
        message = composableIf(message != null) {
            Text(
                text = message!!,
                color = AppTheme.colors.onBackground,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        },
        title = {
            Label(
                text = title.ifEmpty { " " },
                maxLines = 2,
                color = AppTheme.colors.onBackground
            )
        },
        icon = {
            Image(
                painter = lottieAnimationPainter(id = iconResId),
                contentDescription = null
            )
        },
        action = action,
    )
}


private val MASK_TOP_EDGE = listOf(Color.Black, Color.Transparent)
private val MASK_BOTTOM_EDGE = listOf(Color.Transparent, Color.Black)

/**
 * Applies a fading edge effect to content.
 *
 * Creates a gradient that fades the content to transparency at the edges.
 *
 * @param colors Gradient colors, e.g., `listOf(backgroundColor, Color.Transparent)`. Horizontal if `vertical` is `false`.
 * @param length Fade length from the edge.
 * @param vertical `true` for top/bottom fade, `false` for left/right. Defaults to `true`.
 * @return A [Modifier] with the fading edge effect.
 */
// TODO - Add logic to make fading edge apply/exclude content padding in real one.
fun Modifier.fadingEdge2(
    length: Dp = 10.dp,
    vertical: Boolean = true,
) = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    drawRect(
        Brush.verticalGradient(
            MASK_TOP_EDGE, endY = length.toPx(), startY = 0f
        ), blendMode = BlendMode.DstOut, size = size.copy(height = length.toPx())
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = MASK_BOTTOM_EDGE,
            startY = size.height - length.toPx(),
            endY = size.height
        ),
        //  topLeft = Offset(0f, size.height - length.toPx()),
        // size = size.copy(height = length.toPx()),
        blendMode = BlendMode.DstOut
    )
}


/**
 * Adds a composable route to the [NavGraphBuilder] for the given [Route].
 *
 * @param route The [Route] object representing the navigation destination.
 * @param content The composable content to display for this route.
 */
fun NavGraphBuilder.composable(
    route: Route,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route.route,
    content = { id ->
        CompositionLocalProvider(value = LocalNavAnimatedVisibilityScope provides this) {
            content(id)
        }
    }
)

/**
 * Used to provide access to the [NavHostController] through composition without needing to pass it down the tree.
 *
 * To use this composition local, you can call [LocalNavController.current] to get the [NavHostController].
 * If no [NavHostController] has been set, an error will be thrown.
 *
 * Example usage:
 *
 * ```
 * val navController = LocalNavController.current
 * navController.navigate("destination")
 * ```
 */
val LocalNavController =
    staticCompositionLocalOf<NavHostController> {
        error("no local nav host controller found")
    }

/**
 * A [staticCompositionLocalOf] variable that provides access to the [SystemFacade] interface.
 *
 * The [SystemFacade] interface defines common methods that can be implemented by an activity that
 * uses a single view with child views.
 * This local composition allows child views to access the implementation of the [SystemFacade]
 * interface provided by their parent activity.
 *
 * If the [SystemFacade] interface is not defined, an error message will be thrown.
 */
val LocalSystemFacade =
    staticCompositionLocalOf<SystemFacade> {
        error("Provider not defined.")
    }

/**
 * A composable function that uses the [LocalSystemFacade] to fetch [Preference] as state.
 * @param key A key to identify the preference value.
 * @return A [State] object that represents the current value of the preference identified by the provided key.
 * The value can be null if no preference value has been set for the given key.
 */
@Composable
inline fun <S, O> preference(key: Key.Key1<S, O>): androidx.compose.runtime.State<O?> {
    val provider = LocalSystemFacade.current
    return provider.observeAsState(key = key)
}

/**
 * @see [preference]
 */
@Composable
inline fun <S, O> preference(key: Key.Key2<S, O>): androidx.compose.runtime.State<O> {
    val provider = LocalSystemFacade.current
    return provider.observeAsState(key = key)
}

/**
 * A composable function that retrieves the purchase state of a product using the [LocalSystemFacade].
 *
 * This function leverages the `LocalSystemFacade` to access the purchase information for a given product ID.
 * In preview mode, it returns a `null` purchase state as the activity context is unavailable.
 *
 * @param id The ID of the product to check the purchase state for.
 * @return A [State] object representing the current purchase state of the product.
 * The state value can be `null` if there is no purchase associated with the given product ID or if the function
 * is called in preview mode.
 */
@Composable
@NonRestartableComposable
@Stable
fun purchase(id: String): State<Purchase?> =
    LocalSystemFacade.current.observePurchaseAsState(id)

/**
 * A composable function that provides an [AnimatedContentScope] to its content.
 *
 * This function is a wrapper around [AnimatedContent] that also provides the
 * [AnimatedContentScope] through a [CompositionLocalProvider] using [LocalNavAnimatedVisibilityScope].
 * This allows child composables to access the animation scope, for example, to coordinate
 * animations.
 *
 * @param S The type of the [target] state.
 * @param target The target state for the [AnimatedContent]. When this state changes,
 *               [AnimatedContent] will animate between the old and new content.
 * @param modifier Optional [Modifier] to be applied to the [AnimatedContent].
 * @param content A lambda that receives the [AnimatedContentScope] and the current [targetState].
 *                This is where you define the content that will be animated.
 */
@Composable
inline fun <S> ProvideAnimationScope(
    target: S,
    modifier: Modifier = Modifier,
    crossinline content: @Composable() AnimatedContentScope.(targetState: S) -> Unit
) {
    AnimatedContent(target, modifier) { value ->
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            content(value)
        }
    }
}

val Colors.lightShadowColor
    inline get() = if (isLight) Color.White else Color.White.copy(
        0.025f
    )
val Colors.darkShadowColor
    inline get() = if (isLight) Color(0xFFAEAEC0).copy(0.7f) else Color.Black.copy(
        0.6f
    )