package com.prime.media.core.compose

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.primex.core.Text

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
 * Returns the current route of the [NavHostController]
 */
val NavHostController.current
    @Composable inline get() = currentBackStackEntryAsState().value?.destination?.route

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
private fun stringResource(value: Text?) =
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

val edgeWidth = 10.dp
private fun ContentDrawScope.drawFadedEdge(leftEdge: Boolean) {
    val edgeWidthPx = edgeWidth.toPx()
    drawRect(
        topLeft = Offset(if (leftEdge) 0f else size.width - edgeWidthPx, 0f),
        size = Size(edgeWidthPx, size.height),
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startX = if (leftEdge) 0f else size.width,
            endX = if (leftEdge) edgeWidthPx else size.width - edgeWidthPx
        ),
        blendMode = BlendMode.DstIn
    )
}

/**
 * An extension of marque that draw marqueue with faded edge.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.marque(iterations: Int) =
    Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawFadedEdge(leftEdge = true)
            drawFadedEdge(leftEdge = false)
        }
        .basicMarquee(
            // Animate forever.
            iterations = iterations,
        )
        .then(this)

private val NoneWindowInsets = WindowInsets(0)
/**
 * Represents empty window insets with all values set to 0.
 */
val WindowInsets.Companion.None get() =  NoneWindowInsets



/**
 * Conditionally applies another [Modifier] if the given [condition] is true.
 *
 * @param condition The condition to evaluate.
 * @param other The [Modifier] to apply if the condition is true.
 * @return This [Modifier] if the condition is false, otherwise this [Modifier] combined with [other].
 */
fun Modifier.thenIf(condition: Boolean, other: Modifier): Modifier {
    return if (condition) this then other else this
}