@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.prime.media.widget

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.MainActivity
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.backgroundColorAtElevation
import com.prime.media.console.Console
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.Artwork
import com.prime.media.core.compose.LocalAnimatedVisibilityScope
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.current
import com.prime.media.core.compose.scale
import com.prime.media.core.compose.sharedBounds
import com.prime.media.core.compose.sharedElement
import com.prime.media.core.playback.Remote
import com.prime.media.core.playback.artworkUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private const val TAG = "Pixel"

/**
 * Represents the default settings action of pixel widget.
 */
object Pixel {
    val ACTION_PLAY = "com.prime.media.action_play"
    val ACTION_NEXT_TRACK = "com.prime.media.action_next"
    val ACTION_PREV_TRACK = "com.prime.media.action_prev"
    val ACTION_LAUCH_CONSOLE = "com.prime.media.action_launch_console"

    const val SHARED_ARTWORK_ID = "artwork"
    const val SHARED_PLAYING_BARS_ID = "playing_bars"
}

/**
 * Gets or sets the progress of the remote media item.
 *
 * @return Returns a progress value between 0.0 and 1.0; if not available, returns -1.0.
 */
private var Remote.progress
    get() = if (this.position == C.TIME_UNSET || this.duration == C.TIME_UNSET) -1f
    else position / duration.toFloat()
    set(value) {
        if (!isCurrentMediaItemSeekable) return
        val millis = (value * duration).roundToLong()
        seekTo(millis)
    }

/**
 * An array containing the only events that need to be monitored from a remote player.
 *
 * These events are essential for tracking changes in media item transitions and play state.
 * You can use this array to filter and subscribe to specific events when interacting with a remote player.
 *
 * @see Player.EVENT_MEDIA_ITEM_TRANSITION
 * @see Player.EVENT_IS_PLAYING_CHANGED
 */
private val IN_FOCUS_EVENTS = intArrayOf(
    Player.EVENT_MEDIA_ITEM_TRANSITION,
    Player.EVENT_IS_PLAYING_CHANGED
)

/**
 * The standard size of a pixel
 */
private val PixelSize = DpSize(width = 100.dp, height = 34.dp)
private val PixelContentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)

@Composable
private fun Layout(
    imageUri: Uri?,
    playing: Boolean,
    modifier: Modifier = Modifier
) = Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .requiredSize(PixelSize)
        .padding(PixelContentPadding),
    content = {
        // The artwork of the current media item
        Artwork(
            data = imageUri,
            modifier = Modifier
                .border(1.dp, Color.White.copy(0.12f), CircleShape)
                .aspectRatio(1.0f)
                .sharedElement(Pixel.SHARED_ARTWORK_ID)
                .clip(CircleShape)
            ,
        )

        // Divider
        Spacer(
            Modifier
                .padding(vertical = ContentPadding.small)
                .fillMaxHeight()
                .requiredWidth(1.dp)
                .background(Color.White.copy(0.2f))
        )

        val accent = Material.colors.primary
        val properties = rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(
                property = LottieProperty.COLOR,
                accent.toArgb(),
                "**"
            )
        )
        // show playing bars.
        com.prime.media.core.compose.LottieAnimation(
            id = R.raw.playback_indicator,
            iterations = Int.MAX_VALUE,
            dynamicProperties = properties,
            modifier = Modifier
                .sharedBounds(Pixel.SHARED_PLAYING_BARS_ID)
                .requiredSize(24.dp),
            isPlaying = playing,
        )
    },
)

private val RECOMMENDED_ELEVATION = 12.dp
private val LAYOUT_MAX_WIDTH = 400.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Pixel(
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    // If we are at console; return early; because we dnt want mini player here
    val route = navController.current
    if (route == Console.route)
        return Spacer(Modifier)
    // Get the 'remote' object from the MainActivity.
    // Collect the 'loaded' state from the 'remote' object and observe it as a State.
    // If the data isn't loaded, return a Spacer and exit.
    val remote = (LocalView.current.context as MainActivity).remote
    val isLoaded by remote.loaded.collectAsState(initial = false)
    if (!isLoaded) return Spacer(modifier = modifier)
    // Construct the state of this composable
    var item by remember { mutableStateOf(remote.current) }
    var progress by remember { mutableFloatStateOf(remote.progress) }
    var playing by rememberSaveable { mutableStateOf(remote.playWhenReady) }
    // Use LaunchedEffect to observe playback events and update the fields as required
    LaunchedEffect(key1 = Unit) {
        var job: Job? = null
        // Collect events from 'remote.events' and perform actions based on the event
        remote.events.collect { event ->
            // early return if the even is irrelevant.
            if (event != null && !event.containsAny(*IN_FOCUS_EVENTS))
                return@collect

            // Update the variables
            item = remote.current
            playing = remote.playWhenReady
            // If playback is not ready, cancel the existing job (if any)
            job?.cancel()
            if (!remote.playWhenReady) return@collect
            // Start a new coroutine to continuously update 'progress' with 'remote.progress'
            job = launch(Dispatchers.Main) {
                while (true) {
                    progress = remote.position / remote.duration.toFloat()
                    delay(1000)
                }
            }
        }
    }
    // return from here if current is empty
    val current = item ?: return Spacer(Modifier)
    // Setup the logic for transforming
    var expanded by remember { mutableStateOf(false) }
    val radius by animateIntAsState(if (expanded) 14 else 100, label = "${TAG}_radius")
    val shape = RoundedCornerShape(radius)
    // collapse if back pressed.
    BackHandler(expanded) { expanded = false }
    // Callback function that handles actions from the Pixel device.
    val onAction: (String) -> Unit = { action ->
        when (action) {
            Pixel.ACTION_PLAY -> remote.togglePlay()
            Pixel.ACTION_NEXT_TRACK -> remote.skipToNext()
            Pixel.ACTION_PREV_TRACK -> remote.skipToPrev()
            Pixel.ACTION_LAUCH_CONSOLE -> navController.navigate(Console.direction())
        }
    }
    // Setup the actual content
    val content: @Composable AnimatedContentScope.(Boolean) -> Unit = { value ->
        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides this,
            LocalContentColor provides Color.White,
            content = {
                when (value) {
                    // show pil if not expanded.
                    false -> Layout(current.artworkUri, playing)
                    else -> Widget(
                        duration = remote.duration,
                        item = current,
                        playing = playing,
                        progress = progress,
                        onSeek = { remote.progress = it },
                        onAction = onAction,
                    )
                }
            }
        )
    }
    val colors = Material.colors
    val bgColor = if (colors.isLight) Color.Black else colors.backgroundColorAtElevation(1.dp)

    AnimatedContent(
        expanded,
        content = content,
        label = "${TAG}_animated_content",
        // This modifier serves as a container for the content.
        modifier = Modifier
            .padding(horizontal = ContentPadding.large)
            .widthIn(max = LAYOUT_MAX_WIDTH)
            .combinedClickable(
                null, scale(),
                onClick = { expanded = !expanded },
                onLongClick = {
                    if (!expanded)
                        onAction(Pixel.ACTION_LAUCH_CONSOLE)
                    else expanded = false
                }
            )
            .shadow(RECOMMENDED_ELEVATION, shape)
            .border(1.dp, Color.White.copy(0.12f), shape)
            .background(bgColor, shape = shape),
    )
}