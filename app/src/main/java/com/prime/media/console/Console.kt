@file:Suppress(
    "InfiniteTransitionLabel", "InfinitePropertiesLabel", "AnimatedContentLabel", "CrossfadeLabel"
)

package com.prime.media.console

import android.app.Activity
import android.content.pm.ActivityInfo
import android.text.format.DateUtils.formatElapsedTime
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.Anim
import com.prime.media.core.ContentElevation
import com.prime.media.core.MediumDurationMills
import com.prime.media.core.compose.AnimatedIconButton
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LottieAnimButton
import com.prime.media.core.compose.LottieAnimation
import com.prime.media.core.compose.marque
import com.prime.media.core.compose.preference
import com.prime.media.core.compose.shape.CompactDisk
import com.prime.media.darkShadowColor
import com.prime.media.dialog.PlaybackSpeedDialog
import com.prime.media.dialog.PlayingQueue
import com.prime.media.dialog.Timer
import com.prime.media.effects.AudioFx
import com.prime.media.lightShadowColor
import com.prime.media.outline
import com.prime.media.settings.Settings
import com.primex.core.rememberState
import com.primex.core.rotateTransform
import com.primex.core.withSpanStyle
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton2
import com.primex.material2.neumorphic.NeumorphicButton
import com.primex.material2.neumorphic.NeumorphicButtonDefaults
import ir.mahozad.multiplatform.wavyslider.material.WavySlider

private const val TAG = "Console"

private inline val MediaItem.fTitle get() = mediaMetadata.title?.toString()
private inline val MediaItem.fSubtitle get() = mediaMetadata.subtitle?.toString()

/**
 * Shows or hides the system bars, such as the status bar and the navigation bar.
 * @param value A boolean value that indicates whether to show or hide the system bars.
 * If true, the system bars are shown. If false, the system bars are hidden.
 */
private fun WindowInsetsControllerCompat.immersiveMode(value: Boolean) =
    if (value) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())

/**
 * Toggles the screen orientation of the activity between portrait and landscape.
 * @return the new screen orientation value.
 */
private fun Activity.toggleRotation(): Int {
    val rotation =
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    requestedOrientation = rotation
    return rotation
}

/**
 * A short-hand for setting scale in android compose [GraphicsLayerScope].
 * @throws UnsupportedOperationException for get.
 */
private var GraphicsLayerScope.scale: Float
    set(value) {
        scaleX = value; scaleY = value
    }
    get() = error("The getter of scale is not supported this")

private val ROTATING_DISK_BORDER_STROKE = 8.dp

/**
 * A composable function that displays an artwork of a rotating disk. The disk is an image that can
 * be loaded from any data source, such as a bitmap, vector, or network resource.
 */
@Composable
private fun RotatingDisk(
    data: Any?,
    modifier: Modifier = Modifier,
    isRotating: Boolean = false
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
    val imageShape = CompactDisk
    val style = Material.typography.h3
    Image(
        data = data,
        modifier = Modifier
            .graphicsLayer {
                scale = 0.8f
                shadowElevation = ContentElevation.high.toPx()
                shape = imageShape
                clip = true

                // Rotate the image if isRotating is true or angle is not zero;
                // this prevents the image from jumping abruptly from a non-zero angle to zero;
                // set rotationZ to angle or zero depending on the condition
                rotationZ = if (isRotating || rotationZ != 0f) angle else 0f
            }
            .background(Material.colors.surface)
            .border(ROTATING_DISK_BORDER_STROKE, Color.White, imageShape)
            .then(modifier),
    )
}

private val RoundedCornerShape_24 = RoundedCornerShape(24)

@Composable
private fun NeumorphicPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    NeumorphicButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape_24,
        colors = NeumorphicButtonDefaults.neumorphicButtonColors(
            lightShadowColor = Material.colors.lightShadowColor,
            darkShadowColor = Material.colors.darkShadowColor
        ),
        border = if (!Material.colors.isLight) BorderStroke(
            1.dp,
            Material.colors.outline.copy(0.06f)
        ) else null,
        content = {
            LottieAnimation(
                id = R.raw.lt_play_pause,
                atEnd = !isPlaying,
                scale = 1.5f,
                progressRange = 0.0f..0.29f,
                duration = Anim.MediumDurationMills,
                easing = LinearEasing
            )
        }
    )
}

/**
 * A [DropdownMenu] that displays a list of options supported by the current media_item. The menu
 * is triggered by a vertical more icon button. The expansion and collapse of the menu
 * are handled internally by this widget, so the user does not need to implement any logic for that.
 */
@Composable
private inline fun MoreIconButton(
    state: Console,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = modifier) {
        // The icon of this item.
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = null,
            tint = LocalContentColor.current
        )
        // tint
    }
}

/**
 * An Icon Button That houses the logic for displaying/hiding the [PlayingQueue]
 */
@Composable
private inline fun QueueIconButton(
    state: Console,
    modifier: Modifier = Modifier
) {
    var showPlayingQueue by rememberState(initial = false)
    PlayingQueue(
        state = state,
        expanded = showPlayingQueue,
        onDismissRequest = {
            showPlayingQueue = false
        }
    )

    // Option 1
    IconButton(
        onClick = { showPlayingQueue = true },
        painter = rememberVectorPainter(image = Icons.Outlined.Queue),
        modifier = modifier,
        tint = LocalContentColor.current
    )
}

/**
 * An IconButton That displays the queue.
 */
@Composable
private inline fun SpeedIconButton(
    state: Console,
    modifier: Modifier = Modifier
) {
    var showSpeedController by rememberState(initial = false)
    PlaybackSpeedDialog(
        expanded = showSpeedController,
        value = state.playbackSpeed,
        onRequestChange = {
            if (it != -2f)
                state.playbackSpeed = it
            showSpeedController = false
        }
    )
    IconButton(
        onClick = { showSpeedController = true },
        painter = rememberVectorPainter(image = Icons.Outlined.Speed),
        modifier = modifier,
        tint = LocalContentColor.current
    )
}

@Composable
private inline fun SleepAfterIconButton(
    state: Console,
    modifier: Modifier = Modifier
) {
    var showSleepAfter by remember { mutableStateOf(false) }
    Timer(
        expanded = showSleepAfter,
        onValueChange = {
            if (it != -2L)
                state.setSleepAfter(it)
            showSleepAfter = false
        }
    )
    val mills = state.sleepAfterMills
    IconButton(
        onClick = { showSleepAfter = true },
        modifier = modifier,
        content = {
            Crossfade(targetState = mills != -1L) { show ->
                when (show) {
                    true -> Label(
                        text = formatElapsedTime(mills / 1000L),
                        style = Material.typography.caption2,
                        fontWeight = FontWeight.Bold,
                        color = Material.colors.secondary
                    )

                    else -> Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            }
        },
    )
}

@Composable
private inline fun Position(
    mills: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val color = LocalContentColor.current
    Label(
        text = buildAnnotatedString {
            append(formatElapsedTime(mills / 1000))
            append(" - ")
            withSpanStyle(color = color.copy(ContentAlpha.disabled)) {
                append(formatElapsedTime(duration / 1000))
            }
        },
        modifier = modifier,
        style = Material.typography.caption2,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun Controls(
    state: Console,
    modifier: Modifier = Modifier,
    accent: Color = Material.colors.primary,
) {
    val portrait =
        LocalConfiguration.current.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    val constraintSet =
        if (portrait) Console.AudioPortraitConstraintSet else Console.AudioLandscapeConstraintSet
    ConstraintLayout(modifier = modifier, constraintSet = constraintSet) {
        val onColor = LocalContentColor.current
        // Signature
        Text(
            text = stringResource(id = R.string.app_name),
            fontFamily = Settings.DancingScriptFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 70.sp,
            modifier = Modifier
                .layoutId(R.id.np_signature)
                .then(
                    if (portrait)
                        Modifier
                    else Modifier.rotateTransform(false)
                ),
            color = onColor,
            maxLines = 1
        )

        // Close Button
        val navController = LocalNavController.current
        OutlinedButton2(
            onClick = navController::navigateUp,
            modifier = Modifier
                .scale(0.8f)
                .layoutId(R.id.np_close),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
            contentPadding = PaddingValues(vertical = 16.dp),
            shape = RoundedCornerShape_24,
            content = { Icon(imageVector = Icons.Outlined.Close, contentDescription = "Collpase") },
        )

        // Artwork
        RotatingDisk(
            data = state.artwork,
            modifier = Modifier.layoutId(R.id.np_artwork),
            isRotating = state.playing
        )

        // Position
        Position(
            mills = state.position,
            duration = state.duration,
            modifier = Modifier.layoutId(R.id.np_timer)
        )

        //Subtitle
        val current = state.current
        Label(
            text = current?.fSubtitle ?: stringResource(id = R.string.unknown),
            style = Material.typography.caption2,
            modifier = Modifier.layoutId(R.id.np_subtitle),
            color = onColor
        )

        // Title
        Label(
            text = current?.fTitle ?: stringResource(id = R.string.unknown),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .marque(Int.MAX_VALUE)
                .layoutId(R.id.np_title),
            color = onColor
        )

        val favourite = state.favourite
        val facade = LocalSystemFacade.current
        LottieAnimButton(
            id = R.raw.lt_twitter_heart_filled_unfilled,
            onClick = { state.toggleFav(); facade.launchReviewFlow() },
            modifier = Modifier.layoutId(R.id.np_option_0),
            scale = 3.5f,
            progressRange = 0.13f..0.95f,
            duration = 800,
            atEnd = !favourite
        )

        // Slider
        // The Wavy has minSDK of 24; currently don't know if it might trigger some error below API 24.
        // So be carefully until I found some new solution.
        WavySlider(
            value = state.progress,
            onValueChange = { state.seekTo(it) },
            modifier = Modifier.layoutId(R.id.np_slider),
            waveLength = 75.dp,
            waveHeight = 60.dp,
            shouldFlatten = true
        )

        val controller = LocalNavController.current
        // FixMe: State is not required here. implement to get value without state.
        val useBuiltIn by preference(key = Settings.USE_IN_BUILT_AUDIO_FX)
        IconButton(
            onClick = {
                if (useBuiltIn)
                    controller.navigate(AudioFx.route)
                else
                    facade.launchEqualizer(state.audioSessionId)
            },
            imageVector = Icons.Outlined.Tune,
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_option_1),
            tint = onColor
        )

        // PlayButton
        val playing = state.playing
        NeumorphicPlayButton(
            onClick = { state.togglePlay(); facade.launchReviewFlow() },
            isPlaying = playing,
            modifier = Modifier
                .size(60.dp)
                .layoutId(R.id.np_play_toggle),
        )

        // Skip to Prev
        var enabled = if (current != null) !state.isFirst else false
        IconButton(
            onClick = { state.skipToPrev(); facade.launchReviewFlow() },
            painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowLeft),
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_skip_to_prev),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // Skip to Next
        enabled = if (current != null) !state.isLast else false
        IconButton(
            onClick = { state.skipToNext(); facade.launchReviewFlow() },
            painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowRight),
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_skip_to_next),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // SeekBack_10
        enabled = playing
        IconButton(
            onClick = { state.replay() },
            imageVector = Icons.Outlined.Replay10,
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_seek_back_10),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // SeekForward_30
        IconButton(
            onClick = { state.forward() },
            imageVector = Icons.Outlined.Forward30,
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_seek_forward_30),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // Option Row
        // Playing Queue / Option 2 | Option Row
        QueueIconButton(state = state, modifier = Modifier.layoutId(R.id.np_option_2))

        // Playing Speed | Option 3
        SpeedIconButton(state = state, Modifier.layoutId(R.id.np_option_3))

        // SleepAfter | Option 4
        SleepAfterIconButton(state = state, Modifier.layoutId(R.id.np_option_4))

        // Shuffle | Option 5
        val shuffle = state.shuffle
        LottieAnimButton(
            id = R.raw.lt_shuffle_on_off,
            onClick = { state.toggleShuffle(); facade.launchReviewFlow(); },
            modifier = Modifier.layoutId(R.id.np_option_5),
            atEnd = !shuffle,
            progressRange = 0f..0.8f,
            scale = 1.5f
        )

        // CycleRepeatMode | Option 6
        val mode = state.repeatMode
        AnimatedIconButton(
            id = R.drawable.avd_repeat_more_one_all,
            onClick = { state.cycleRepeatMode();facade.launchReviewFlow(); },
            atEnd = mode == Player.REPEAT_MODE_ALL,
            modifier = Modifier.layoutId(R.id.np_option_6),
            tint = onColor.copy(if (mode == Player.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
        )
    }
}

@Composable
fun Console(state: Console) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Spacer(
            Modifier
                .background(Material.colors.background)
                .fillMaxSize()
        )

        // The Controller
        val contentColor = Material.colors.onSurface
        CompositionLocalProvider(value = LocalContentColor provides contentColor) {
            Controls(
                state = state, modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            )
        }
    }
}