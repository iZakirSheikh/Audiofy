package com.prime.player


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.player.extended.*
import com.prime.player.preferences.*
import kotlinx.coroutines.flow.map

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

@Composable
fun PlayerTheme(darkTheme: Boolean, content: @Composable() () -> Unit) {

    val context = LocalContext.current
    val preferences = Preferences.get(context = context)

    val primary by with(preferences) { getPrimaryColor().collectAsState() }
    val secondary by with(preferences) { getSecondaryColor().collectAsState() }

    val fontFamily by with(preferences) {
        getDefaultFont().map { font ->
            when (font) {
                Font.SYSTEM_DEFAULT -> FontFamily.Default
                Font.PROVIDED -> FontFamily(
                    androidx.compose.ui.text.font.Font(R.font.lato_bold, FontWeight.Bold),
                    androidx.compose.ui.text.font.Font(
                        R.font.lato_regular,
                        FontWeight.Normal
                    ),
                    androidx.compose.ui.text.font.Font(R.font.lato_light, FontWeight.Light),
                )
                Font.SAN_SERIF -> FontFamily.SansSerif
                Font.SARIF -> FontFamily.Serif
                Font.CURSIVE -> FontFamily.Cursive
            }
        }.collectAsState()
    }

    val background by animateColorAsState(
        targetValue = if (darkTheme) Color.Black else Color.SignalWhite,
        animationSpec = tween(Anim.DURATION_LONG)
    )

    val surface by animateColorAsState(
        targetValue = if (darkTheme) Color.TrafficBlack else  Color.White,
        animationSpec = tween(Anim.DURATION_LONG)
    )

    val colors = Colors(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        primaryVariant = primary.blend(Color.Black, 0.2f),
        secondaryVariant = secondary.blend(Color.Black, 0.2f),
        onPrimary = Color.SignalWhite,
        onSurface = if (darkTheme) Color.SignalWhite else  Color.UmbraGrey,
        onBackground = if (darkTheme) Color.SignalWhite else  Color.UmbraGrey,
        error = Color.OrientRed,
        onSecondary = Color.SignalWhite,
        onError = Color.SignalWhite,
        isLight = !darkTheme
    )

    MaterialTheme(
        colors = colors,
        typography = Typography(
            defaultFontFamily = fontFamily
        ),
        shapes = Shapes,
        content = content
    )
}

/**
 * Alternate to [MaterialTheme] allowing us to add our own theme systems (e.g. [LocalFixedElevation]) or to
 * extend [MaterialTheme]'s types e.g. return our own [Colors] extension
 */
object PlayerTheme {

    /**
     * Proxy to [MaterialTheme]
     */
    val colors: Colors
        @Composable
        get() = MaterialTheme.colors

    /**
     * Proxy to [MaterialTheme]
     */
    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    /**
     * Proxy to [MaterialTheme]
     */
    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes

    /**
     * Retrieves the current [Images] at the call site's position in the hierarchy.
     */
    /*val images: Images
        @Composable
        get() = LocalImages.current*/
}


