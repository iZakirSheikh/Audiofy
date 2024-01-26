package com.prime.media.console

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.MainActivity
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.Anim
import com.prime.media.core.MediumDurationMills
import com.prime.media.core.compose.Artwork
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LottieAnimButton
import com.prime.media.core.compose.menu.Popup2
import com.prime.media.core.playback.Remote
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.subtitle
import com.prime.media.core.playback.title
import com.prime.media.outline
import com.primex.core.SignalWhite
import com.primex.material2.Label
import com.primex.material2.ListTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "MiniPlayer"

// TODO: Handle the case when the media item does not have any progress.

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

// The default shape of the MiniPlayer.
private val MiniPlayerShape = RoundedCornerShape(20)
private val DefaultArtworkShape = MiniPlayerShape

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
private val HORIZONTAL_MAX_WIDTH = 340.dp

@Composable
private fun Layout(
    value: MediaItem,
    progress: Float,
    isPlaying: Boolean,
    remote: Remote,
    modifier: Modifier = Modifier
) {
    ListTile(
        headline = { Label(text = value.title.toString(), fontWeight = FontWeight.Bold) },
        // The artist name as Subtitle/Overline
        overline = {
            Label(
                text = value.subtitle.toString() ?: "",
                style = Material.typography.caption2,
                color = LocalContentColor.current.copy(ContentAlpha.disabled)
            )
        },
        // Background Color
        color = Color.Transparent,
        // Trailing as Artwork
        trailing = {
            Artwork(
                data = value.artworkUri,
                modifier = Modifier
                    .size(84.dp)
                    .clip(DefaultArtworkShape),
            )
        },
        // Row of common Buttons
        subtitle = {
            Row {
                val color = LocalContentColor.current.copy(ContentAlpha.medium)
                // SeekBackward
                com.primex.material2.IconButton(
                    onClick = { remote.skipToPrev() },
                    imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                    contentDescription = null,
                    tint = color
                )

                // Play Toggle
                LottieAnimButton(
                    id = R.raw.lt_play_pause,
                    atEnd = !isPlaying,
                    scale = 1.5f,
                    progressRange = 0.0f..0.29f,
                    duration = Anim.MediumDurationMills,
                    easing = LinearEasing,
                    onClick = { if (isPlaying) remote.pause() else remote.play(true) }
                )

                // SeekNext
                com.primex.material2.IconButton(
                    onClick = { remote.skipToNext() },
                    imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                    contentDescription = null,
                    tint = color
                )
            }
        },
        // Footer as ProgressBar.
        footer = {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .height(3.dp)
                    .offset(y = 11.dp)
                    .fillMaxWidth()
            )
        },
        modifier = modifier.widthIn(max = HORIZONTAL_MAX_WIDTH)
    )
}

/**
 * Represents a popup that displays a media notification, which includes a button with playing bars and
 * facilitates the expansion and collapse of the notification.
 *
 * @param expanded Boolean indicating whether the media notification is expanded or collapsed.
 * @param onRequestToggle Callback to be invoked when the user requests to toggle the expansion state.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopupMedia(
    expanded: Boolean,
    onRequestToggle: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero
) {
    val remote = (LocalView.current.context as MainActivity).remote
    val isLoaded by remote.loaded.collectAsState(initial = false)

    // if their is noting return.
    if (!isLoaded)
        return Spacer(modifier = modifier)
    // If 'item' is null, return a Spacer with the given modifier and exit
    var item by remember { mutableStateOf(remote.current) }
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

    val shape = CircleShape
    val navController = LocalNavController.current

    val content = @Composable {
        // show playing bars.
        com.prime.media.core.compose.LottieAnimation(
            id = R.raw.playback_indicator,
            iterations = Int.MAX_VALUE,
            modifier = Modifier.requiredSize(24.dp),
            isPlaying = isPlaying
        )

        Popup2(
            expanded = expanded,
            onDismissRequest = onRequestToggle,
            backgroundColor = Color(0xFF0E0E0F),
            contentColor = Color.SignalWhite,
            border = BorderStroke(1.dp, Material.colors.outline),
            shape = MiniPlayerShape,
            offset = offset,
            content = {
                val density = LocalDensity.current
                Layout(
                    value = item ?: return@Popup2,
                    progress = progress,
                    isPlaying = isPlaying,
                    remote = remote,
                    modifier = Modifier
                        .clickable(remember { MutableInteractionSource() }, null, onClick = { navController.navigate(Console.direction())})
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
                        }
                    )
                )
            }
        )
    }

    // Button Surface
    Surface(
        color = Color.Black,
        shape = CircleShape,
        border = BorderStroke(1.dp, Material.colors.outline),
        content = content,
        modifier = modifier
            .clip(shape)
            .combinedClickable(
                onClick = onRequestToggle,
                onLongClick = {
                    // navigate to Console
                    navController.navigate(Console.direction())
                }
            )
            .size(40.dp)
            .scale(0.9f),
    )
}