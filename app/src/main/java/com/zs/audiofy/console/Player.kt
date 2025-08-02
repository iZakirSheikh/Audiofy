@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.console

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.thenIf
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderColors
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.Surface
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote

private const val TAG = "Console-Player"

private val NonePlaying = NowPlaying(null, null)

@Composable
private inline fun Artwork(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: Dp = 1.dp,
    shadow: Dp = 0.dp,
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .visualEffect(ImageBrush.NoiseBrush, 0.5f, true)
            .thenIf(border > 0.dp) { border(1.dp, Color.White, shape) }
            .shadow(shadow, shape, clip = shape != RectangleShape)
            .background(AppTheme.colors.background(1.dp)),
    )
}

@Composable
private fun PlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    style: Int = 0,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(60.dp),
        shape = AppTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            AppTheme.colors.onBackground.copy(if (!AppTheme.colors.isLight) ContentAlpha.indication else 1f)
        ),
        contentColor = LocalContentColor.current,
        content = {
            Icon(
                painter = lottieAnimationPainter(
                    id = R.raw.lt_play_pause,
                    atEnd = isPlaying,
                    progressRange = 0.0f..0.29f,
                    animationSpec = tween(easing = LinearEasing)
                ),
                modifier = Modifier.lottie(1.5f),
                contentDescription = null
            )
        }
    )
}

@Composable
fun TimeBar(
    progress: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
) {
    // FIXME: This is a temporary workaround.
    //  Problem:
    //  The Slider composable uses BoxWithConstraints internally. When used within a ConstraintLayout
    //  with width Dimension.fillToConstraints, it behaves unexpectedly. This workaround addresses the issue.
    //  Remove this workaround once the underlying issue is resolved.
    var width by remember { mutableIntStateOf(0) }
    Row(modifier.onSizeChanged() {
        width = it.width
    }) {
        Slider(
            progress,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.width(with(LocalDensity.current) { width.toDp() }),
            enabled = enabled,
            colors = colors
        )
    }
}

@Composable
fun Player(
    viewState: ConsoleViewState,
    constraints: Constraints,
    onRequest: (request: Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        constraintSet = constraints.value,
        modifier = modifier,
        animateChangesSpec = null,
    ) {
        // Background
        Spacer(
            modifier = Modifier
                .background(AppTheme.colors.background(1.dp))
                .layoutId(Constraints.ID_BACKGROUND)
        )

        val contentColor = AppTheme.colors.onBackground
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            content = {

                // Collapse
                IconButton(
                    icon = Icons.Outlined.ExpandMore,
                    onClick = { },
                    modifier = Modifier
                        .background(AppTheme.colors.background(3.dp), shape = CircleShape)
                        .layoutId(Constraints.ID_CLOSE_BTN),
                    tint = AppTheme.colors.accent,
                    contentDescription = null,
                )

                // Artwork
                val current by viewState.state.collectAsState(null)
                val state = current ?: NonePlaying
                Artwork(
                    model = state.artwork,
                    modifier = Modifier
                        .layoutId(Constraints.ID_ARTWORK)
                        .sharedElement(RouteConsole.SHARED_ELEMENT_ARTWORK),
                    border = 3.dp,
                    shape = AppTheme.shapes.xLarge,
                    shadow = 4.dp
                )

                // Position
                val chronometer = state.chronometer
                val elapsed = chronometer.elapsed
                val fPos =
                    if (elapsed == Long.MIN_VALUE) "N/A" else DateUtils.formatElapsedTime(elapsed / 1000)
                val duration = state.duration
                val fDuration =
                    if (duration == Remote.TIME_UNSET) "N/A" else DateUtils.formatElapsedTime(duration / 1000)
                Label(
                    "$fPos / $fDuration (${stringResource(R.string.postfix_x_f, state.speed)})",
                    style = AppTheme.typography.label3,
                    color = contentColor.copy(ContentAlpha.medium),
                    modifier = Modifier.layoutId(Constraints.ID_POSITION),
                )

                // Subtitle
                Label(
                    text = state.subtitle ?: stringResource(id = R.string.unknown),
                    style = AppTheme.typography.label3,
                    modifier = Modifier
                        .sharedElement(RouteConsole.SHARED_ELEMENT_SUBTITLE)
                        .layoutId(Constraints.ID_SUBTITLE),
                    color = contentColor.copy(ContentAlpha.medium)
                )
                // Title
                Label(
                    text = state.title ?: stringResource(id = R.string.unknown),
                    fontSize = constraints.titleTextSize,// Maybe Animate
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .sharedElement(RouteConsole.SHARED_ELEMENT_TITLE)
                        .marque(Int.MAX_VALUE)
                        .layoutId(Constraints.ID_TITLE),
                    color = contentColor
                )

                // Slider
                TimeBar(
                    progress =  chronometer.progress(state.duration),
                    onValueChange = {
                        val mills = (it * state.duration).toLong()
                        Log.d(TAG, "Player: $mills")
                        chronometer.raw = mills
                    },
                    onValueChangeFinished = {
                        val progress = chronometer.elapsed / state.duration.toFloat()
                        viewState.seekTo(progress)
                    },
                    modifier = Modifier.layoutId(Constraints.ID_SEEK_BAR),
                    enabled = state.duration > 0,
                    colors = SliderDefaults.colors(
                        disabledThumbColor = AppTheme.colors.accent,
                        disabledActiveTrackColor = AppTheme.colors.accent
                    )
                )

                // Shuffle
                LottieAnimatedButton(
                    id = R.raw.lt_shuffle_on_off,
                    onClick = { viewState.shuffle(!state.shuffle) },
                    atEnd = state.shuffle,
                    progressRange = 0f..0.8f,
                    scale = 1.5f,
                    contentDescription = null,
                    tint = if (state.shuffle) AppTheme.colors.accent else contentColor.copy(ContentAlpha.disabled),
                    modifier = Modifier.layoutId(Constraints.ID_SHUFFLE)
                )

                // Skip to next
                IconButton(
                    onClick = viewState::skipToNext,
                    icon = Icons.Outlined.KeyboardDoubleArrowRight,
                    contentDescription = null,
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_SKIP_TO_NEXT)
                    /* tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)*/
                )

                // Skip to Prev
                IconButton(
                    onClick = viewState::skipToPrev,
                    icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                    contentDescription = null,
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_SKIP_PREVIOUS)
                    /* tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)*/
                )

                // Repreat Mode
                IconButton(
                    onClick = viewState::cycleRepeatMode,
                    content = {
                        val mode = state.repeatMode
                        Icon(
                            painter = com.zs.audiofy.common.compose.rememberAnimatedVectorPainter(
                                R.drawable.avd_repeat_more_one_all,
                                mode == Remote.REPEAT_MODE_ALL
                            ),
                            contentDescription = null,
                            tint = contentColor.copy(if (mode == Remote.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
                        )
                    },
                    modifier = Modifier.layoutId(Constraints.ID_REPEAT_MODE)
                )

                PlayButton(
                    onClick = viewState::togglePlay,
                    isPlaying = state.playing,
                    modifier = Modifier.layoutId(Constraints.ID_PLAY_PAUSE)
                )

                // Resize Mode
                IconButton(
                    icon = Icons.Outlined.Fullscreen,
                    contentDescription = null,
                    onClick = {},
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_RESIZE_MODE)
                )

                // rotation
                IconButton(
                    icon = Icons.Outlined.ScreenRotation,
                    contentDescription = null,
                    onClick = {},
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_ROTATION_LOCK)
                )

                // options
                IconButton(
                    icon = Icons.Outlined.Queue,
                    contentDescription = null,
                    onClick = {},
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_QUEUE)
                )

                IconButton(
                    icon = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    onClick = {},
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_LIKED)
                )

                IconButton(
                    icon = Icons.Outlined.Speed,
                    contentDescription = null,
                    onClick = {},
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_SPEED)
                )

                IconButton(
                    icon = Icons.Outlined.Timer,
                    contentDescription = null,
                    onClick = {},
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_SLEEP_TIMER)
                )

                IconButton(
                    icon = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    onClick = {},
                    enabled = true,
                    modifier = Modifier.layoutId(Constraints.ID_MORE)
                )
            }
        )
    }
}
