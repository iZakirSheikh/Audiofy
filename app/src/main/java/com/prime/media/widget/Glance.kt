@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.prime.media.widget

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.common.preference
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.console.Constraints
import com.prime.media.common.AppConfig
import com.prime.media.common.Registry
import com.prime.media.console.RouteConsole
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.LocalNavAnimatedVisibilityScope
import com.zs.core_ui.scale
import com.zs.core_ui.sharedBounds
import androidx.compose.foundation.combinedClickable as clickable

private const val TAG = "Glance"


/**
 * This object contains the defaults of Glance Widget.
 */
object Glance {

    val LAYOUT_MAX_WIDTH = 400.dp

    /**
     * The size of the pixel in collapsed mode.
     */
    val MIN_SIZE = 40.dp

    const val SHARED_ARTWORK_ID = Constraints.ID_ARTWORK
    const val SHARED_PLAYING_BARS_ID = "playing_bars"
    const val SHARED_BACKGROUND_ID = Constraints.ID_BACKGROUND
    const val SHARED_TITLE = Constraints.ID_TITLE
    const val SHARED_SUBTITLE = Constraints.ID_SUBTITLE
    const val SHARED_CONTROLS = Constraints.ID_CONTROLS
    const val SHARD_TIME_BAR = Constraints.ID_TIME_BAR

    val ELEVATION = 12.dp

    val SharedBoundsModifier = Modifier.sharedBounds(
        SHARED_BACKGROUND_ID,
        exit = fadeOut() + scaleOut(),
        enter = fadeIn() + scaleIn()
    )
}

/**
 * Displays the Glance widget with the given [id] and [state].
 *
 * Handles navigation via [onNavigate] and supports showcasing for purchase
 * if [showcase] is true.
 *
 * @param id Widget ID from [BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU].
 * @param state Current widget state ([NowPlaying]).
 * @param onDismissRequest Callback for dismissing the widget to [MiniLayout]
 * @param modifier [Modifier] for customization.
 * @param showcase If true, showcases the widget for purchase.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Glance(
    id: String,
    state: NowPlaying,
    noinline onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false,
) {
    when (id) {
        BuildConfig.IAP_PLATFORM_WIDGET_ELONGATE_BEAT -> ElongatedBeat(
            state,
            onDismissRequest,
            modifier,
            showcase
        )

        BuildConfig.IAP_PLATFORM_WIDGET_DISK_DYNAMO -> DiskDynamo(
            state,
            onDismissRequest,
            modifier,
            showcase
        )

        BuildConfig.IAP_PLATFORM_WIDGET_IPHONE -> Iphone(
            state,
            onDismissRequest,
            modifier,
            showcase
        )

        BuildConfig.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE ->
            RedVioletCake(state, onDismissRequest, modifier, showcase)

        BuildConfig.IAP_PLATFORM_WIDGET_SNOW_CONE -> SnowCone(
            state,
            onDismissRequest,
            modifier,
            showcase
        )

        BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU -> Tiramisu(
            state,
            onDismissRequest,
            modifier,
            showcase
        )

        BuildConfig.IAP_COLOR_CROFT_GOLDEN_DUST -> GoldenDust(
            state,
            onDismissRequest,
            modifier,
            showcase
        )

        BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES ->
            GradientGroves(state, onDismissRequest, modifier, showcase)

        BuildConfig.IAP_COLOR_CROFT_ROTATING_GRADEINT ->
            RotatingColorGradient(state, onDismissRequest, modifier, showcase)

        BuildConfig.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS ->
            WavyGradientDots(state, onDismissRequest, modifier, showcase)

        BuildConfig.IAP_COLOR_CROFT_MISTY_DREAM -> MistyDream(state, onDismissRequest, modifier, showcase)
        BuildConfig.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC -> SkewedDynamic(state, onDismissRequest, modifier, showcase)
    }
}


/**
 * Creates a Glance widget that displays either a mini player or a full screen layout,
 * based on user interaction.
 *
 * @param state The current playback state.
 * @param modifier Modifier for the layout.
 */
@Composable
fun Glance(
    state: NowPlaying,
    modifier: Modifier = Modifier
) {
    // State to track whether the widget is expanded or not.
    var expanded by remember { mutableStateOf(false) }
    // Get the navigation controller.
    val navController = LocalNavController.current
    // Handler to collapse the widget when back is pressed.
    val onDismissRequest = { expanded = false }
    // Handle back press when expanded.
    BackHandler(expanded, onDismissRequest)

    // Modifier for clickable behavior.
    val clickable = Modifier
        .clickable(
            // No ripple effect.
            interactionSource = null,
            // Scale animation on click.
            indication = scale(),
            // Toggle expanded state on click.
            onClick = {
                val isFab = !expanded
                when {
                    // If currently viewing a video, toggle playback (play/pause).
                    isFab && AppConfig.fabLongPressLaunchConsole -> expanded = true
                    // If in FAB mode and the "long-press FAB opens console" setting is enabled,
                    // expand the player to show the console.
                    isFab && !AppConfig.fabLongPressLaunchConsole ->
                        navController.navigate(/*Console.direction()*/ RouteConsole())
                    // If in FAB mode but the "long-press FAB opens console" setting is disabled,
                    // navigate directly to the console screen without expanding.
                    else -> expanded = false
                }
            },
            // Navigate to console on long click if not expanded, otherwise collapse.
            onLongClick = {
                val isFab = !expanded
                // Determine if the player is currently in FAB (mini) mode.
                // If not expanded, it's considered a FAB player.

                when {
                    isFab && AppConfig.fabLongPressLaunchConsole ->
                        // If in FAB mode AND the user preference "long-press FAB opens console" is enabled,
                        // navigate to the console screen.
                        navController.navigate(/*Console.direction()*/ RouteConsole())

                    isFab && !AppConfig.fabLongPressLaunchConsole ->
                        // If in FAB mode but the preference is disabled,
                        // expand the player to show the full-screen view instead.
                        expanded = true
                    // show config screen always
                    else -> /*showConfigScreen = true*/ "dfd"
                }
            }
        )

    // Actual content of the widget.
    // This content expands to mini-player on click
    // or expands to full-screen layout when user long presses
    AnimatedContent(
        targetState = expanded,
        label = "${TAG}_animated_content",
        modifier = modifier
            .padding(horizontal = ContentPadding.large)
            .widthIn(max = Glance.LAYOUT_MAX_WIDTH),
        content = { value ->
            // Provide the current scope for navigation animations.
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                // Get the user's preference for the Glance widget.
                val widget by preference(Registry.GLANCE)
                when (value) {
                    // Show mini player if not expanded.
                    false -> MiniLayout(state, modifier = clickable)
                    // Show full screen Glance layout if expanded.
                    true -> Glance(widget, state, onDismissRequest, modifier = clickable)
                }
            }
        }
    )
}