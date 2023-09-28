package com.prime.media.console

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.Anim
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.MediumDurationMills
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LottieAnimButton
import com.prime.media.core.compose.shape.CompactDisk
import com.prime.media.core.playback.Remote
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.subtitle
import com.prime.media.core.playback.title
import com.prime.media.darkShadowColor
import com.prime.media.lightShadowColor
import com.prime.media.settings.Settings
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.neumorphic.Neumorphic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "MiniPlayer"

// TODO: handle the case when the media-item does not have any progress.
/**
 * return - Returns a progress between 0.o and 1.0f
 */
private val Remote.progress get() = position / duration.toFloat()

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
 * A constant that represents the stiffness of the drag effect responsible for updating the position
 * of the currently playing media item.
 *
 * The 'DRAG_STIFF_CONST' is used as a coefficient to control the responsiveness and speed of the
 * drag interaction that allows users to adjust the position of the media item being played.
 * Modifying this constant can fine-tune the drag behavior as needed.
 */
private const val DRAG_STIFF_CONST = 3

/**
 * The maximum width of the [Horizontal] [MiniPlayer].
 */
private val HORIZONTAL_MAX_WIDTH = 360.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
private inline fun Horizontal(
    value: MediaItem,
    progress: Float,
    isPlaying: Boolean,
    noinline onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    Neumorphic(
        lightShadowColor = Material.colors.darkShadowColor,
        darkShadowColor = Material.colors.lightShadowColor,
        elevation = ContentElevation.low,
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
        onClick = { navController.navigate(Console.direction()) },
        // Add necessay params to it.
        modifier = modifier
            .navigationBarsPadding()
            .sizeIn(maxHeight = Settings.MINI_PLAYER_HEIGHT, maxWidth = HORIZONTAL_MAX_WIDTH)
            .padding(top = 8.dp, start = 10.dp, end = 10.dp)
            .scale(0.85f),

        // content
        content = {
            ListTile(
                headline = { Label(text = value.title.toString(), fontWeight = FontWeight.Bold) },
                overline = { Label(text = value.subtitle.toString() ?: "") },
                color = Color.Transparent,
                leading = {
                    Image(
                        data = value.artworkUri,
                        modifier = Modifier
                            .aspectRatio(1.0f)
                            .clip(CompactDisk),
                    )
                },

                trailing = {
                    LottieAnimButton(
                        id = R.raw.lt_play_pause,
                        atEnd = !isPlaying,
                        scale = 1.5f,
                        progressRange = 0.0f..0.29f,
                        duration = Anim.MediumDurationMills,
                        easing = LinearEasing,
                        onClick = onTogglePlay
                    )
                },
                footer = {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .height(3.dp)
                            .offset(y = 7.dp)
                            .padding(horizontal = ContentPadding.normal)
                            .fillMaxWidth()
                    )
                }
            )
        }
    )
}

@Composable
fun MiniPlayer(
    remote: Remote,
    modifier: Modifier = Modifier
) {
    // Initialize '_item' using 'mutableStateOf' with the value from 'remote.current'.
    // It's important to note that 'remote.current' should not return null at this point,
    // as the MiniPlayer is initialized only when 'item' is not null.
    var item by remember { mutableStateOf(remote.current) }

    // If 'item' is null, return a Spacer with the given modifier and exit
    if (item == null)
        return Spacer(modifier = modifier)
    var progress by remember { mutableFloatStateOf(remote.progress) }
    var isPlaying by rememberSaveable { mutableStateOf(remote.playWhenReady) }

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
            isPlaying = remote.playWhenReady
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

    val density = LocalDensity.current
    Horizontal(
        value = item ?: return Unit,
        progress = progress,
        isPlaying = isPlaying,
        onTogglePlay = remote::togglePlay,
        modifier = modifier
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState() { delta ->
                    // Adjust the drag sensitivity by multiplying or dividing 'delta' as needed.
                    // In this code, we divide 'delta' by 'DRAG_STIFF_CONST' for sensitivity control.
                    // Convert 'delta' to the amount the user dragged in dp (density-independent pixels).
                    val dp = with(density) { (delta / DRAG_STIFF_CONST).toDp().value }

                    // Calculate the time adjustment based on the user's drag in milliseconds.
                    val milliseconds = (dp * 1000).roundToInt()

                    // Calculate the new position in milliseconds by adding the time adjustment to the current position.
                    val newPositionMillis = milliseconds + remote.position

                    // update the progress; so as to reflect the change in real_time.
                    progress = newPositionMillis / remote.duration.toFloat()

                    // Seek to the new position in the media playback.
                    remote.seekTo(newPositionMillis)

                    // Log the updated position for debugging purposes.
                    Log.d(TAG, "MiniPlayer: ${remote.position}")
                }
            )
    )
}
