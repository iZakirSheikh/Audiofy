package com.prime.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.player.settings.FontFamily
import com.prime.player.settings.GlobalKeys
import com.primex.core.hsl
import com.primex.preferences.LocalPreferenceStore
import com.primex.ui.*
import kotlinx.coroutines.flow.map
import androidx.compose.ui.text.font.FontFamily as AndroidFontFamily

private const val TAG = "Theme"

typealias Material = MaterialTheme

/**
 * An Extra font family.
 */
private val ProvidedFontFamily =
    AndroidFontFamily(
        //light
        Font(R.font.lato_light, FontWeight.Light),
        //normal
        Font(R.font.lato_regular, FontWeight.Normal),
        //bold
        Font(R.font.lato_bold, FontWeight.Bold),
    )

/**
 * Constructs the typography with the [fontFamily] provided with support for capitalizing.
 */
private fun Typography(fontFamily: AndroidFontFamily): Typography {
    return Typography(
        defaultFontFamily = fontFamily,
        button = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 1.25.sp,
            // a workaround for capitalizing
            fontFeatureSettings = "c2sc, smcp"
        ),
        overline = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            // a workaround for capitalizing
            fontFeatureSettings = "c2sc, smcp"
        )
    )
}

/**
 * A variant of caption.
 */
private val caption2 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
    letterSpacing = 0.4.sp
)

/**
 * A variant of caption
 */
val Typography.caption2 get() = com.prime.player.caption2

/**
 * The alpha of the container colors.
 */
val MaterialTheme.CONTAINER_COLOR_ALPHA get() = 0.15f

/**
 * checks If [GlobalKeys.FORCE_COLORIZE]
 */
val MaterialTheme.forceColorize
    @Composable inline get() = LocalPreferenceStore.current.run {
        get(GlobalKeys.FORCE_COLORIZE).observeAsState().value
    }

private val small2 = RoundedCornerShape(8.dp)

/**
 * A variant of MaterialTheme shape with coroner's 8 dp
 */
val Shapes.small2 get() = com.prime.player.small2

/**
 * returns [primary] if [requires] is met else [elze].
 * @param requires The condition for primary to return. default value is [requiresAccent]
 * @param elze The color to return if [requires] is  not met The default value is [surface]
 */
@Composable
fun Colors.primary(requires: Boolean = MaterialTheme.forceColorize, elze: Color = colors.surface) =
    if (requires) MaterialTheme.colors.primary else elze

/**
 * returns [onPrimary] if [requires] is met else [otherwise].
 * @param requires The condition for onPrimary to return. default value is [requiresAccent]
 * @param otherwise The color to return if [requires] is  not met The default value is [onSurface]
 */
@Composable
fun Colors.onPrimary(
    requires: Boolean = MaterialTheme.forceColorize,
    elze: Color = colors.onSurface
) =
    if (requires) MaterialTheme.colors.onPrimary else elze

/**
 * @see primary()
 */
@Composable
fun Colors.secondary(
    requires: Boolean = MaterialTheme.forceColorize,
    elze: Color = colors.surface
) =
    if (requires) colors.secondary else elze

/**
 * @see onPrimary()
 */
@Composable
fun Colors.onSecondary(
    requires: Boolean = MaterialTheme.forceColorize,
    elze: Color = colors.onSurface
) =
    if (requires) MaterialTheme.colors.onSecondary else elze

val Colors.surfaceVariant
    @Composable inline get() = colors.surface.hsl(lightness = if (isLight) 0.94f else 0.06f)

/**
 * Primary container is applied to elements needing less emphasis than primary
 */
val Colors.primaryContainer
    @Composable inline get() = colors.primary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * On-primary container is applied to content (icons, text, etc.) that sits on top of primary container
 */
val Colors.onPrimaryContainer @Composable inline get() = colors.primary

val Colors.secondaryContainer
    @Composable inline get() = colors.secondary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

val Colors.onSecondaryContainer @Composable inline get() = colors.secondary

val Colors.errorContainer
    @Composable inline get() = colors.error.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

val Colors.onErrorContainer @Composable inline get() = colors.error

/**
 * Observes the coloring [GlobalKeys.COLOR_STATUS_BAR] of status Bar.
 */
val MaterialTheme.colorStatusBar
    @Composable inline get() = LocalPreferenceStore.current.run {
        get(GlobalKeys.COLOR_STATUS_BAR).observeAsState().value
    }

inline val Colors.overlay
    @Composable get() = (if (isLight) Color.Black else Color.White).copy(0.04f)

val Colors.onOverlay
    @Composable inline get() =
        (colors.onBackground).copy(alpha = ContentAlpha.medium)

val Colors.lightShadowColor
    @Composable inline get() =
        if (isLight) Color.White else Color.White.copy(0.025f)

val Colors.darkShadowColor
    @Composable inline get() =
        if (isLight) Color(0xFFAEAEC0).copy(0.7f) else Color.Black.copy(0.6f)


private val defaultPrimaryColor = Color(0xFF5600E8)
private val defaultSecondaryColor = Color.Amber

private val defaultThemeShapes =
    Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(0.dp)
    )

@Composable
fun Material(isDark: Boolean, content: @Composable () -> Unit) {

    val preferences = LocalPreferenceStore.current

    val background by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0E0E0F) else Color(0xFFF5F5FA),
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )

    val surface by animateColorAsState(
        targetValue = if (isDark) Color.TrafficBlack else Color.White,
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )

    val primary = defaultPrimaryColor
    val secondary = defaultSecondaryColor

    val colors = Colors(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        primaryVariant = primary.blend(Color.Black, 0.2f),
        secondaryVariant = secondary.blend(Color.Black, 0.2f),
        onPrimary = Color.SignalWhite,
        onSurface = if (isDark) Color.SignalWhite else Color.UmbraGrey,
        onBackground = if (isDark) Color.SignalWhite else Color.Black,
        error = Color.OrientRed,
        onSecondary = Color.SignalWhite,
        onError = Color.SignalWhite,
        isLight = !isDark
    )

    val fontFamily by with(preferences) {
        preferences[GlobalKeys.FONT_FAMILY].map { font ->
            when (font) {
                FontFamily.SYSTEM_DEFAULT -> AndroidFontFamily.Default
                FontFamily.PROVIDED -> ProvidedFontFamily
                FontFamily.SAN_SERIF -> AndroidFontFamily.SansSerif
                FontFamily.SARIF -> AndroidFontFamily.Serif
                FontFamily.CURSIVE -> AndroidFontFamily.Cursive
            }
        }.observeAsState()
    }

    MaterialTheme(
        colors = colors,
        typography = Typography(fontFamily),
        shapes = defaultThemeShapes,
        content = content
    )
}
