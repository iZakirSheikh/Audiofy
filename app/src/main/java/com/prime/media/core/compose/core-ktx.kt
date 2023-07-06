package com.prime.media.core.compose

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.prime.media.Audiofy
import com.primex.core.Text
import com.primex.core.resolve


private const val TAG = "ComposeUtil"


/**
 * Returns a Resources instance for the application's package.
 */
val ProvidableCompositionLocal<Context>.resources: Resources
    @ReadOnlyComposable @Composable inline get() = current.resources

/**
 * Used to access the [NavHostController] without passing it down the tree.
 */
val LocalNavController =
    staticCompositionLocalOf<NavHostController> {
        error("no local nav host controller found")
    }

/**
 * Returns the current route of the [NavHostController]
 */
val NavHostController.current
    @Composable inline get() = currentBackStackEntryAsState().value?.destination?.route


inline fun Resources.stringResource(res: Text) = resolve(res)

@JvmName("stringResource1")
inline fun Resources.stringResource(res: Text?) = resolve(res)


/**
 * @return [content] if [condition] is true else null
 */
@Deprecated("rename for better naming.")
fun composable(condition: Boolean, content: @Composable () -> Unit) =
    when (condition) {
        true -> content
        else -> null
    }

@Composable
fun stringResource(value: Text?)  = if(value == null) null else com.primex.core.stringResource(value = value)

@ExperimentalAnimationGraphicsApi
@Composable
inline fun rememberAnimatedVectorResource(@DrawableRes id: Int, atEnd: Boolean) =
    androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = id), atEnd = atEnd
    )

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
 * Checks whether the preference with key [Audiofy.FORCE_COLORIZE] has been set, indicating
 * whether the app should force colorization of views.
 *
 * This property uses the `preference` composable to retrieve the preference value.
 *
 * @return `true` if the preference has been set, `false` otherwise.
 */
val MaterialTheme.forceColorize
    @Composable inline get() = preference(key = Audiofy.FORCE_COLORIZE)

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