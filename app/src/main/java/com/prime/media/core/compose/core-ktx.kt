package com.prime.media.core.compose

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.primex.core.Text
import com.primex.core.resolve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


private const val TAG = "ComposeUtil"

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
val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("no local nav host controller found")
}

/**
 * Returns the current route of the [NavHostController]
 */
val NavHostController.current
    @Composable inline get() = currentBackStackEntryAsState().value?.destination?.route


/**
 * The content padding for the screen under current [NavGraph]
 */
@Deprecated("Not required.")
val LocalWindowPadding = compositionLocalOf {
    PaddingValues(0.dp)
}

/**
 * A Type Alias for [resolve]
 */
inline fun Resources.stringResource(res: Text) = resolve(res)

/**
 * A Type Alias for [resolve]
 */
@JvmName("stringResource1")
inline fun Resources.stringResource(res: Text?) = resolve(res)

/**
 * @return [content] if [condition] is true else null
 */
@Deprecated("rename for better naming.")
fun composable(condition: Boolean, content: @Composable () -> Unit) = when (condition) {
    true -> content
    else -> null
}

/**
 * A Type Alias for [stringResource]
 */
@Deprecated("Use the Text.value instead.")
@Composable
fun stringResource(value: Text?) =
    if (value == null) null else com.primex.core.stringResource(value = value)

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

/**
 * @return The color that should be used over [overlay]
 */
val Colors.onOverlay
    @Composable inline get() = (MaterialTheme.colors.onBackground).copy(alpha = ContentAlpha.medium)

/**
 * The Light Shadow Color for [Neumorphic] surface.
 */
val Colors.lightShadowColor
    @Composable inline get() = if (isLight) Color.White else Color.White.copy(0.025f)

/**
 * The Dark Shadow Color for [Neumorphic] surface.
 */
val Colors.darkShadowColor
    @Composable inline get() = if (isLight) Color(0xFFAEAEC0).copy(0.7f) else Color.Black.copy(0.6f)