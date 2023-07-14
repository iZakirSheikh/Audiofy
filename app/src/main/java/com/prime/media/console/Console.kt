@file:Suppress("InfiniteTransitionLabel", "InfinitePropertiesLabel")

package com.prime.media.console

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.marque
import com.prime.media.core.compose.rememberAnimatedVectorResource
import com.prime.media.core.compose.shape.CompactDisk
import com.prime.media.core.util.DateUtils
import com.prime.media.darkShadowColor
import com.prime.media.dialog.PlayingQueue
import com.prime.media.lightShadowColor
import com.prime.media.outline
import com.primex.core.lerp
import com.primex.core.rememberState
import com.primex.core.withSpanStyle
import com.primex.material2.BottomSheetDialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton2
import com.primex.material2.neumorphic.NeumorphicButton
import com.primex.material2.neumorphic.NeumorphicButtonDefaults
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private inline val MediaItem.title
    get() = mediaMetadata.title?.toString()
private inline val MediaItem.subtitle
    get() = mediaMetadata.subtitle?.toString()

/**
 * A simple extension fun to add to modifier.
 */
private inline fun Modifier.layoutID(id: ConstrainedLayoutReference) = layoutId(id.id)

private val Rounded = RoundedCornerShape(24)

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun PlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float,
    isPlaying: Boolean
) {
    val elevation = NeumorphicButtonDefaults.elevation(lerp(0.dp, 6.dp, progress))
    val border =
        if (progress != 0f && !Material.colors.isLight)
            BorderStroke(1.dp, Material.colors.outline.copy(0.06f))
        else
            null
    NeumorphicButton(
        onClick = onClick,
        modifier = modifier,
        shape = Rounded,
        elevation = elevation,
        border = border,
        colors = NeumorphicButtonDefaults.neumorphicButtonColors(
            lightShadowColor = Material.colors.lightShadowColor,
            darkShadowColor = Material.colors.darkShadowColor
        )
    ) {
        Icon(
            painter = rememberAnimatedVectorResource(
                id = R.drawable.avd_pause_to_play,
                atEnd = !isPlaying
            ),
            modifier = Modifier.scale(1.5f),
            contentDescription = null
        )
    }
}

private val ARTWORK_STROKE_DEFAULT_EXPANDED = 8.dp
private val ARTWORK_STROKE_DEFAULT_COLLAPSED = 3.dp

@Composable
private fun SpeedControllerLayout(
    value: Float,
    modifier: Modifier = Modifier,
    onRequestChange: (new: Float) -> Unit
) {
    Surface(modifier = modifier) {
        Column() {
            TopAppBar(
                title = { Label(text = "Playback Speed", style = Material.typography.body2) },
                backgroundColor = Material.colors.background,
            )

            Label(
                text = "${String.format("%.2f", value)}x",
                modifier = Modifier
                    .padding(top = ContentPadding.normal)
                    .align(Alignment.CenterHorizontally),
                style = Material.typography.h6
            )

            Slider(
                value = value,
                onValueChange = onRequestChange,
                valueRange = 0.25f..2f,
                steps = 6,
                modifier = Modifier.padding(
                    horizontal = ContentPadding.xLarge,
                )
            )
        }
    }
}

@Composable
private fun Artwork(
    data: Any?,
    modifier: Modifier = Modifier,
    progress: Float,
    isPlaying: Boolean = false
) {
    // Create an InfiniteTransition object
    val infiniteTransition = rememberInfiniteTransition()
    // Create an Animatable value for the rotation angle
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val borderWidth =
        lerp(ARTWORK_STROKE_DEFAULT_COLLAPSED, ARTWORK_STROKE_DEFAULT_EXPANDED, progress)
    val imageShape = CompactDisk
    val style = Material.typography.h3
    Image(
        data = data,
        modifier = Modifier
            .graphicsLayer {
                val scale = lerp(1f, 0.8f, progress)
                scaleX = scale
                scaleY = scale
                shadowElevation = ContentElevation.high.toPx()
                shape = imageShape
                clip = true
                rotationZ = if (isPlaying) angle else 0f
            }
            .background(Material.colors.surface)
            .border(borderWidth, Color.White, imageShape)
            .then(modifier),
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Console(
    state: Console,
    progress: Float,
    onRequestToggle: () -> Unit
) {
    val updated by rememberUpdatedState(newValue = progress)
    val expanded by remember {
        derivedStateOf { updated == 1f }
    }
    val color =
        lerp(Material.colors.surface, Material.colors.background, progress)
    Vertical(
        state = state,
        progress = updated,
        onRequestToggle = onRequestToggle,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = lerp(0.8f, 1f, (progress * 2.5f).coerceIn(0.0f..1.0f))
                scaleX = scale
                scaleY = scale
                this.shadowElevation = lerp(12.dp, 0.dp, progress).toPx()
                shape = RoundedCornerShape(lerp(100f, 0f, progress).roundToInt())
                clip = true
            }
            .drawWithCache {
                onDrawBehind {
                    drawRect(color, size = size)
                }
            }
            .clickable(
                onClick = onRequestToggle,
                indication = null,
                interactionSource = remember(::MutableInteractionSource),
                enabled = !expanded // only enabled when collapsed.
            )
    )
}

@OptIn(ExperimentalMotionApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun Vertical(
    modifier: Modifier = Modifier,
    state: Console,
    progress: Float,
    onRequestToggle: () -> Unit
) {
    MotionLayout(
        start = Console.Collapsed,
        end = Console.Expanded,
        progress = progress,
        modifier = modifier
    ) {
        var onColor = Material.colors.onSurface
        val insets = WindowInsets.statusBars
        // Signature
        Text(
            text = stringResource(id = R.string.app_name),
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
            fontSize = 70.sp,
            modifier = Modifier
                .windowInsetsPadding(insets)
                .layoutID(Console.SIGNATURE),
            color = onColor,
            maxLines = 1
        )
        // Close Button
        OutlinedButton2(
            onClick = onRequestToggle,
            modifier = Modifier
                .windowInsetsPadding(insets)
                .scale(0.8f)
                .layoutID(Console.CLOSE),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
            contentPadding = PaddingValues(vertical = 16.dp),
            shape = Rounded,
            content = {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Collpase")
            },
        )

        // artwork
        Artwork(
            data = state.artwork,
            modifier = Modifier.layoutID(Console.ARTWORK),
            progress = progress,
            isPlaying = state.playing
        )


        //slider
        val value = state.progress
        val time = state.position
        Label(
            text = DateUtils.formatAsDuration(time),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.layoutID(Console.PROGRESS_MILLS),
            fontSize = 34.sp
        )

        // Slider row
        Slider(
            value = value,
            onValueChange = { state.seekTo(it) },
            modifier = Modifier.layoutID(Console.SLIDER),
        )

        val favourite = state.favourite
        val facade = LocalSystemFacade.current
        IconButton(
            onClick = { state.toggleFav(); facade.launchReviewFlow() },
            painter = painterResource(id = if (favourite) R.drawable.ic_heart_filled else R.drawable.ic_heart),
            contentDescription = null,
            modifier = Modifier.layoutID(Console.HEART),
            tint = onColor
        )

        IconButton(
            onClick = { facade.launchEqualizer(state.audioSessionId) },
            imageVector = Icons.Outlined.Tune,
            contentDescription = null,
            modifier = Modifier.layoutID(Console.EQUALIZER),
            tint = onColor
        )

        //title
        val current = state.current
        Label(
            text = current?.subtitle ?: stringResource(id = R.string.unknown),
            style = Material.typography.caption2,
            modifier = Modifier
                .offset(y = 4.dp, x = 5.dp)
                .layoutID(Console.SUBTITLE),
            color = onColor
        )

        Label(
            text = current?.title ?: stringResource(id = R.string.unknown),
            fontSize = lerp(18.sp, 44.sp, progress),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .marque(Int.MAX_VALUE)
                .layoutID(Console.TITLE),
            color = onColor
        )

        // playButton
        val playing = state.playing
        PlayButton(
            onClick = { state.togglePlay(); facade.launchReviewFlow() },
            progress = progress,
            isPlaying = playing,
            modifier = Modifier
                .size(60.dp)
                .layoutID(Console.TOGGLE),
        )

        var enabled = if (current != null) !state.isFirst else false
        IconButton(
            onClick = { state.skipToPrev(); facade.launchReviewFlow() },
            painter = painterResource(id = R.drawable.ic_skip_to_prev),
            contentDescription = null,
            modifier = Modifier.layoutID(Console.SKIP_TO_PREVIOUS),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        enabled = if (current != null) !state.isLast else false
        IconButton(
            onClick = { state.skipToNext(); facade.launchReviewFlow() },
            painter = painterResource(id = R.drawable.ic_skip_to_next),
            contentDescription = null,
            modifier = Modifier.layoutID(Console.SKIP_TO_NEXT),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        enabled = playing
        IconButton(
            onClick = { state.replay() },
            imageVector = Icons.Outlined.Replay10,
            contentDescription = null,
            modifier = Modifier.layoutID(Console.SKIP_BACK_10),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        IconButton(
            onClick = { state.forward() },
            imageVector = Icons.Outlined.Forward30,
            contentDescription = null,
            modifier = Modifier.layoutID(Console.SKIP_FORWARD_30),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        var showPlayingQueue by rememberState(initial = false)
        PlayingQueue(
            state = state,
            expanded = showPlayingQueue,
            onDismissRequest = {
                showPlayingQueue = false
            }
        )

        IconButton(
            onClick = { showPlayingQueue = true },
            painter = rememberVectorPainter(image = Icons.Outlined.Queue),
            modifier = Modifier.layoutID(Console.QUEUE),
            tint = onColor
        )

        var showSpeedController by rememberState(initial = false)
        BottomSheetDialog(
            expanded = showSpeedController,
            onDismissRequest = { showSpeedController = false }) {
            var speed by rememberState(initial = state.playbackSpeed)
            SpeedControllerLayout(
                value = speed,
                onRequestChange = { speed = it; state.playbackSpeed = it })
        }

        IconButton(
            onClick = { showSpeedController = true },
            painter = rememberVectorPainter(image = Icons.Outlined.Speed),
            modifier = Modifier.layoutID(Console.SPEED),
            tint = onColor
        )

        IconButton(
            onClick = { /*TODO: Implement this.*/ state.setSleepAfter(1) },
            painter = rememberVectorPainter(image = Icons.Outlined.Timer),
            modifier = Modifier.layoutID(Console.SLEEP),
            tint = onColor
        )
        val shuffle = state.shuffle
        IconButton(
            onClick = { state.toggleShuffle(); facade.launchReviewFlow(); },
            painter = painterResource(id = R.drawable.ic_shuffle),
            modifier = Modifier.layoutID(Console.SHUFFLE),
            tint = onColor.copy(if (shuffle) ContentAlpha.high else ContentAlpha.medium)
        )

        val mode = state.repeatMode
        IconButton(
            onClick = { state.cycleRepeatMode();facade.launchReviewFlow(); },
            painter = painterResource(id = if (mode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat),
            modifier = Modifier.layoutID(Console.REPEAT),
            tint = onColor.copy(if (mode == Player.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
        )
    }
}