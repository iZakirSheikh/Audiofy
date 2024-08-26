@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.prime.media.widget

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.MainActivity
import com.prime.media.Material
import com.prime.media.R
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
import com.primex.core.foreground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private const val TAG = "Pixel"

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

private val LAYOUT_MAX_WIDTH = 400.dp

object PixelDefaults {

    /**
     * The size of the pixel in collapsed mode.
     */
    val MIN_SIZE = 40.dp

    val ACTION_PLAY = "com.prime.media.action_play"
    val ACTION_NEXT_TRACK = "com.prime.media.action_next"
    val ACTION_PREV_TRACK = "com.prime.media.action_prev"
    val ACTION_LAUCH_CONSOLE = "com.prime.media.action_launch_console"

    const val SHARED_ARTWORK_ID = "artwork"
    const val SHARED_PLAYING_BARS_ID = "playing_bars"
    const val SHARED_BACKGROUND_ID = "background"

    val ELEVATION = 12.dp
}

/**
 * The Mini-Version of the Pixel.
 */
@Composable
private fun MiniLayout(
    imageUri: Uri?,
    playing: Boolean,
    modifier: Modifier = Modifier
) = Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
        // .clip(CircleShape)
        .sharedBounds(
            PixelDefaults.SHARED_BACKGROUND_ID,
            exit = fadeOut() + scaleOut(),
            enter = fadeIn() + scaleIn()
        )
        .shadow(PixelDefaults.ELEVATION, CircleShape)
        .background(Material.colors.surface)
        .requiredSize(PixelDefaults.MIN_SIZE),
    content = {
        // The artwork of the current media item
        Artwork(
            data = imageUri,
            modifier = Modifier
                .border(1.dp, Color.White.copy(0.12f), CircleShape)
                .aspectRatio(1.0f)
                .foreground(Color.Black.copy(0.24f))
                .sharedElement(PixelDefaults.SHARED_ARTWORK_ID)
                .clip(CircleShape),
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
                .sharedBounds(PixelDefaults.SHARED_PLAYING_BARS_ID)
                .requiredSize(24.dp),
            isPlaying = playing,
        )
    }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Pixel(
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    // If we are at console; return early; because we dnt want mini player here
    val route = navController.current
    if (route == Console.route)
        return
    // Get the 'remote' object from the MainActivity.
    // Collect the 'loaded' state from the 'remote' object and observe it as a State.
    // If the data isn't loaded, return a Spacer and exit.
    val remote = (LocalView.current.context as MainActivity).remote
    val isLoaded by remote.loaded.collectAsState(initial = false)
    if (!isLoaded) return
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
    val current = item ?: return
    // Setup the logic for transforming
    var expanded by remember { mutableStateOf(false) }
    // collapse if back pressed.
    BackHandler(expanded) { expanded = false }
    // Callback function that handles actions from the Pixel device.
    val onAction: (String) -> Unit = { action ->
        when (action) {
            PixelDefaults.ACTION_PLAY -> remote.togglePlay()
            PixelDefaults.ACTION_NEXT_TRACK -> remote.skipToNext()
            PixelDefaults.ACTION_PREV_TRACK -> remote.skipToPrev()
            PixelDefaults.ACTION_LAUCH_CONSOLE -> navController.navigate(Console.direction())
        }
    }

    // Setup the actual content
    val content: @Composable AnimatedContentScope.(Boolean) -> Unit = { value ->
        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides this,
            content = {
                val clickable = Modifier
                    .combinedClickable(
                        null, scale(),
                        onClick = { expanded = !expanded },
                        onLongClick = {
                            if (!expanded)
                                onAction(PixelDefaults.ACTION_LAUCH_CONSOLE)
                            else expanded = false
                        }
                    )
                when (value) {
                    // show pil if not expanded.
                    false -> MiniLayout(current.artworkUri, playing, modifier = clickable)
                    else -> Iphone(
                        duration = remote.duration,
                        item = current,
                        playing = playing,
                        progress = progress,
                        onSeek = { remote.progress = it },
                        onAction = onAction,
                        modifier = clickable
                    )
                }
            }
        )
    }

    AnimatedContent(
        expanded,
        content = content,
        label = "${TAG}_animated_content",
        modifier = modifier
            .padding(horizontal = ContentPadding.large)
            .widthIn(max = LAYOUT_MAX_WIDTH)
    )
}

