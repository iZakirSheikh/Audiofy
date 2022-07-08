package com.prime.player.common.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.player.LocalSystemUiController


/**
 * A Utility extension function for managing status bar UI.
 *
 * @param color: The background color of the statusBar. if [Color.Unspecified] the status bar will
 * be painted by primaryVariant.
 * @param darkIcons: same as name suggests works in collaboration with color. if it is unspecified; uses
 * light icons as we will use primaryVariant as background.
 */
fun Modifier.statusBarsPadding2(
    color: Color = Color.Unspecified,
    darkIcons: Boolean = false,
) =
    composed {

        val controller = LocalSystemUiController.current
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


// Nav Host Controller
val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("no local nav host controller found")
}

val NavHostController.current
    @Composable
    get() = currentBackStackEntryAsState().value?.destination?.route