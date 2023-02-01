package com.prime.player

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.player.core.FontFamily
import com.primex.ui.*
import androidx.compose.ui.text.font.FontFamily as AndroidFontFamily

private const val TAG = "Theme"

/**
 * A simple typealias of [MaterialTheme]
 */
typealias Theme = MaterialTheme

/**
 * An Extra font family.
 */
@Deprecated("Maybe use google font library.")
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
private fun Typography(fontFamily: FontFamily): Typography {
    return Typography(
        defaultFontFamily = when (fontFamily) {
            FontFamily.SYSTEM_DEFAULT -> AndroidFontFamily.Default
            FontFamily.PROVIDED -> ProvidedFontFamily
            FontFamily.SAN_SERIF -> AndroidFontFamily.SansSerif
            FontFamily.SARIF -> AndroidFontFamily.Serif
            FontFamily.CURSIVE -> AndroidFontFamily.Cursive
        }, button = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 1.25.sp,
            // a workaround for capitalizing
            fontFeatureSettings = "c2sc, smcp"
        ), overline = TextStyle(
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
private val caption2 =
    TextStyle(fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.4.sp)

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
    @Composable inline get() = preference(key = Audiofy.FORCE_COLORIZE)

/**
 * A vaiant of [MaterialTheme.shapes.small]
 */
private val small2 = RoundedCornerShape(8.dp)

/**
 * A variant of MaterialTheme shape with coroner's 8 dp
 */
val Shapes.small2 get() = com.prime.player.small2

/**
 * Primary container is applied to elements needing less emphasis than primary
 */
val Colors.primaryContainer
    @Composable inline get() = colors.primary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * On-primary container is applied to content (icons, text, etc.) that sits on top of primary container
 */
val Colors.onPrimaryContainer
    @Composable inline get() = colors.primary

val Colors.secondaryContainer
    @Composable inline get() = colors.secondary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

val Colors.onSecondaryContainer @Composable inline get() = colors.secondary

val Colors.errorContainer
    @Composable inline get() = colors.error.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

val Colors.onErrorContainer @Composable inline get() = colors.error

inline val Colors.overlay
    @Composable get() = (if (isLight) Color.Black else Color.White).copy(0.04f)

inline val Colors.outline
    get() = (if (isLight) Color.Black else Color.White).copy(0.12f)

val Colors.onOverlay
    @Composable inline get() = (colors.onBackground).copy(alpha = ContentAlpha.medium)

val Colors.lightShadowColor
    @Composable inline get() = if (isLight) Color.White else Color.White.copy(0.025f)

val Colors.darkShadowColor
    @Composable inline get() = if (isLight) Color(0xFFAEAEC0).copy(0.7f) else Color.Black.copy(0.6f)

private val defaultPrimaryColor = Color.SkyBlue
private val defaultSecondaryColor = Color.Rose


private val defaultThemeShapes =
    Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(0.dp)
    )

@Composable
fun Theme(isDark: Boolean, content: @Composable () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0E0E0F) else Color(0xFFF5F5FA),
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )

    // TODO: update status_bar here.

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

    // TODO: update status_bar here.
    val colorize by preference(key = Audiofy.COLOR_STATUS_BAR)
    val uiController = rememberSystemUiController()
    val isStatusBarHidden by preference(key = Audiofy.HIDE_STATUS_BAR)
    Log.d(TAG, "Theme: $colorize $isStatusBarHidden")
    SideEffect {
        uiController.setSystemBarsColor(
            if (colorize) colors.primaryVariant else Color.Transparent,
            darkIcons = !colorize && !isDark,
        )
        uiController.isStatusBarVisible = !isStatusBarHidden
    }

    val fontFamily by preference(key = Audiofy.FONT_FAMILY)

    MaterialTheme(
        colors = colors,
        typography = Typography(fontFamily),
        shapes = defaultThemeShapes,
        content = content
    )
}
