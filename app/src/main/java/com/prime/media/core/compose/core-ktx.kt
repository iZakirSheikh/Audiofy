package com.prime.media.core.compose

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.primex.core.Text
import com.primex.core.resolve

private const val TAG = "ComposeUtil"

/**
 * Returns a Resources instance for the application's package.
 */
val ProvidableCompositionLocal<Context>.resources: Resources
    @ReadOnlyComposable @Composable inline get() = current.resources

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
 * [CompositionLocal] containing the [WindowSizeClass].
 *
 * This [CompositionLocal] is used to access the current [WindowSizeClass] within a composition.
 * If no [WindowSizeClass] is found in the composition hierarchy, a error will be thorn.
 *
 * Usage:
 * ```
 * val windowSizeClass = LocalWindowSizeClass.current
 * // Use the windowSizeClass value within the composition
 * ```
 * @optIn ExperimentalMaterial3WindowSizeClassApi
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("no local WindowSizeClass defined.")
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
@Deprecated("use the one from library toolkit.")
fun composable(condition: Boolean, content: @Composable () -> Unit) =
    when (condition) {
        true -> content
        else -> null
    }

@Composable
fun stringResource(value: Text?) =
    if (value == null) null else com.primex.core.stringResource(value = value)

/**
 * @see androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
 */
@ExperimentalAnimationGraphicsApi
@Composable
inline fun rememberAnimatedVectorResource(@DrawableRes id: Int, atEnd: Boolean) =
    androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = id), atEnd = atEnd
    )