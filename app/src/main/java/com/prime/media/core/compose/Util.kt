package com.prime.media.core.compose

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.primex.core.Text
import com.primex.core.resolve


private const val TAG = "ComposeUtil"

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


/**
 * A Utility extension function for managing status bar UI.
 *
 * @param color: The background color of the statusBar. if [Color.Unspecified] the status bar will
 * be painted by primaryVariant.
 * @param darkIcons: same as name suggests works in collaboration with color. if it is unspecified; uses
 * light icons as we will use primaryVariant as background.
 */
@Deprecated("Make color of statusbar change at single place.")
fun Modifier.statusBarsPadding2(
    color: Color = Color.Unspecified,
    darkIcons: Boolean = color.luminance() > 0.5,
) = composed {
    val controller = rememberSystemUiController()

    // invoke but control only icons not color.
    SideEffect {
        controller.setStatusBarColor(
            //INFO we are not going to change the background of the statusBar here.
            // Reasons are.
            //  * It adds a delay and the change becomes ugly.
            //  * animation to color can't be added.
            Color.Transparent,

            // dark icons only when requested by user and color is unSpecified.
            // because we are going to paint status bar with primaryVariant if unspecified.
            darkIcons && !color.isUnspecified
        )
    }

    val paint = color.takeOrElse { MaterialTheme.colors.primaryVariant }
    // add padding

    val height = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toFloat()
    }

    // add background
    Modifier
        .drawWithContent {
            drawContent()
            drawRect(paint, size = size.copy(height = height))
        }
        .then(this@composed)
        .statusBarsPadding()
}

/**
 * The content padding for the screen under current [NavGraph]
 */
@Deprecated("Not required. Pass directly.")
val LocalWindowPadding =
    compositionLocalOf {
        PaddingValues(0.dp)
    }


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