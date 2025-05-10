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

package com.prime.media.common.compose

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottiePainter
import com.prime.media.common.Route
import com.prime.media.common.SystemFacade
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.background
import com.zs.compose.foundation.composableIf
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Colors
import com.zs.compose.theme.LocalNavAnimatedVisibilityScope
import com.zs.compose.theme.Placeholder
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.appbar.FloatingLargeTopAppBar
import com.zs.compose.theme.appbar.LargeTopAppBar
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.appbar.TopAppBarScrollBehavior
import com.zs.compose.theme.appbar.TopAppBarStyle
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.billing.Purchase
import com.zs.preferences.Key
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
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
    duration: Int = -1,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    easing: Easing = FastOutSlowInEasing,
): Painter {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val duration2 = composition?.duration?.roundToInt() ?: AnimationConstants.DefaultDurationMillis
    val progress by animateFloatAsState(
        targetValue = if (atEnd) progressRange.start else progressRange.endInclusive,
        label = "Lottie $id",
        animationSpec = tween(if (duration == -1) duration2 else duration, easing = easing)
    )
    return rememberLottiePainter(
        composition = composition,
        progress = progress,
    )
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
    repeatMode: RepeatMode = RepeatMode.Restart,
    iterations: Int = Int.MAX_VALUE,
): Painter {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        reverseOnRepeat = repeatMode == RepeatMode.Reverse,
    )
    return rememberLottiePainter(
        composition = composition,
        progress = progress
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
fun Modifier.fadingEdge2(
    colors: List<Color>,
    length: Dp = 10.dp,
    vertical: Boolean = true,
) = drawWithContent {
    drawContent()
    drawRect(Brush.verticalGradient(colors, endY = length.toPx()))
    drawRect(Brush.verticalGradient(colors.reversed(), startY = (size.height - length.toPx())))
}

/** Creates and [remember] s the instance of [HazeState] */
@Composable
@NonRestartableComposable
fun rememberBackgroundProvider() = remember(::HazeState)

fun Modifier.observe(provider: HazeState) = hazeSource(state = provider)

// Reusable mask
private val PROGRESSIVE_MASK = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
    Brush.verticalGradient(
        listOf(
            Color.Black,
            Color.Black,
            Color.Transparent
        )
    )
else
    Brush.verticalGradient(
        0.5f to Color.Black,
        0.8f to Color.Black.copy(0.5f),
        1.0f to Color.Transparent,
    )

/**
 * Applies a hazy effect to the background based on the provided [HazeState].
 *
 * This function creates a blurred background with optional noise and tint effects. It provides customization options
 * for blur radius, noise factor, tint color, blend mode, and progressive blurring.
 *
 * @param provider The [HazeState] instance that manages the haze effect.
 * @param containerColor The background color of the container. Defaults to [Colors.background].
 * @param blurRadius The radius of the blur effect. Defaults to 38.dp for light backgrounds (luminance >= 0.5) and 60.dp for dark backgrounds.
 * @param noiseFactor The factor for the noise effect. Defaults to 0.4f for light backgrounds and 0.28f for dark backgrounds. Noise effect is disabled on Android versions below 12.
 * @param tint The color to tint the blurred background with. Defaults to a semi-transparent version of [containerColor].
 * @param blendMode The blend mode to use for the tint. Defaults to [BlendMode.SrcOver].
 * @param luminance controls the luminosity of the blured layer defaults to 0.07f. -1 disables it.
 * @param progressive A float value to control progressive blurring:
 *   - -1f: Progressive blurring is disabled.
 *   - 0f: Bottom to top gradient.
 *   - 1f: Top to bottom gradient.
 *   - Values between 0f and 1f: Intermediate gradient positions.
 *   Progressive blurring is only available on Android 12 and above.
 * @return A [Background] composable with the specified haze effect.
 */
@SuppressLint("ModifierFactoryExtensionFunction")
@OptIn(ExperimentalHazeApi::class)
fun Colors.background(
    provider: HazeState,
    containerColor: Color = background(0.4.dp),
    blurRadius: Dp = if (containerColor.luminance() >= 0.5f) 38.dp else 80.dp,
    noiseFactor: Float = if (containerColor.luminance() >= 0.5f) 0.5f else 0.25f,
    tint: Color = containerColor.copy(alpha = if (containerColor.luminance() >= 0.5) 0.63f else 0.65f),
    luminance: Float = 0.07f,
    blendMode: BlendMode = BlendMode.SrcOver,
    progressive: Float = -1f,
) = Background(
    Modifier.hazeEffect(state = provider) {
        this.blurEnabled = true
        this.blurRadius = blurRadius
        this.backgroundColor = containerColor
        // Disable noise factor on Android versions below 12.
        this.noiseFactor = noiseFactor
        this.tints = buildList {
            // apply luminosity just like in Microsoft Acrylic.
            if (luminance != -1f)
                this += HazeTint(Color.White.copy(0.07f), BlendMode.Luminosity)
            this += HazeTint(tint, blendMode = blendMode)
        }
        // Configure progressive blurring (if enabled).
        if (progressive != -1f) {
            this.progressive = HazeProgressive.verticalGradient(
                startIntensity = progressive,
                endIntensity = 0f,
                preferPerformance = true
            )
            // Adjust input scale for Android versions below 12 for better visuals.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                inputScale = HazeInputScale.Fixed(0.5f)
            mask = PROGRESSIVE_MASK
        }
    }
)

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
