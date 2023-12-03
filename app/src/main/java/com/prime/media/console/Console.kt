@file:Suppress(
    "InfiniteTransitionLabel",
    "InfinitePropertiesLabel",
    "AnimatedContentLabel",
    "CrossfadeLabel"
)

package com.prime.media.console

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.text.format.DateUtils.formatElapsedTime
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.Anim
import com.prime.media.core.ContentElevation
import com.prime.media.core.MediumDurationMills
import com.prime.media.core.compose.AnimatedIconButton
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LottieAnimButton
import com.prime.media.core.compose.LottieAnimation
import com.prime.media.core.compose.PlayerView
import com.prime.media.core.compose.marque
import com.prime.media.core.compose.menu.DropdownMenu2
import com.prime.media.core.compose.menu.DropdownMenuItem2
import com.prime.media.core.compose.preference
import com.prime.media.core.compose.shape.CompactDisk
import com.prime.media.core.playback.subtitle
import com.prime.media.core.playback.title
import com.prime.media.darkShadowColor
import com.prime.media.dialog.PlaybackSpeedDialog
import com.prime.media.dialog.PlayingQueue
import com.prime.media.dialog.Timer
import com.prime.media.effects.AudioFx
import com.prime.media.lightShadowColor
import com.prime.media.outline
import com.prime.media.settings.Settings
import com.primex.core.activity
import com.primex.core.rememberState
import com.primex.core.rotateTransform
import com.primex.core.withSpanStyle
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton2
import com.primex.material2.neumorphic.NeumorphicButton
import com.primex.material2.neumorphic.NeumorphicButtonDefaults
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import java.util.concurrent.TimeUnit

private const val TAG = "Console"

// Extensions | Helpers
private inline val Console.title get() = current?.title?.toString()
private inline val Console.subtitle get() = current?.subtitle?.toString()

/**
 * Utility Fun that toggles [Console.resizeMode]
 */
private fun Console.toggleResizeMode() {
    resizeMode =
        if (resizeMode == Console.RESIZE_MODE_FILL) Console.RESIZE_MORE_FIT else Console.RESIZE_MODE_FILL
}

/**
 * Shows or hides the system bars, such as the status bar and the navigation bar.
 * @param value A boolean value that indicates whether to show or hide the system bars.
 * If true, the system bars are shown. If false, the system bars are hidden.
 */
private fun WindowInsetsControllerCompat.immersiveMode(value: Boolean) =
    if (value) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())

/**
 * Toggles the screen orientation of the activity between portrait and sensor-based landscape modes.
 *
 * If the current screen orientation is unspecified, the function sets it to
 * [ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE]. If it is already in sensor-based landscape mode,
 * the function sets it back to [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED].
 *
 * @return The new screen orientation value after the toggle.
 */
private fun Activity.toggleRotation(): Int {
    // Determine the new screen orientation based on the current state.
    val rotation =
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    // Set the requested screen orientation to the calculated value.
    requestedOrientation = rotation

    // Return the new screen orientation value.
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
    value: ImageBitmap?,
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

    Crossfade(
        targetState = value,
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
            .then(modifier)
    ) { res ->
        if (res == null)
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.default_art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
        else
            androidx.compose.foundation.Image(
                bitmap = res,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
    }
}

@Composable
fun Artwork(
    value: ImageBitmap?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(15),
    elevation: Dp = ContentElevation.medium
) {
    Crossfade(
        targetState = value,
        modifier = Modifier
            .scale(0.8f)
            .shadow(elevation, shape,)
            .background(Material.colors.surface, shape)
            .then(modifier)
    ) { res ->
        if (res == null)
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.default_art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
        else
            androidx.compose.foundation.Image(
                bitmap = res,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
    }
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
private inline fun More(
    state: Console,
    modifier: Modifier = Modifier
) {
    // Represents the state of all menus in this class. 0 means main menu, 1 means audio menu and 2 means
    var expanded by remember { mutableIntStateOf(-1) }
    IconButton(onClick = { expanded = 0 }, modifier = modifier) {
        // The icon of this item.
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = null,
            tint = LocalContentColor.current
        )

        // MainMenu
        DropdownMenu2(
            expanded = expanded == 0,
            onDismissRequest = { expanded = -1 }
        ) {
            val onColor = LocalContentColor.current
            // First Row
            Row {
                // CycleRepeatMode | Option 6
                val facade = LocalSystemFacade.current
                val mode = state.repeatMode
                AnimatedIconButton(
                    id = R.drawable.avd_repeat_more_one_all,
                    onClick = {
                        state.cycleRepeatMode();facade.launchReviewFlow(); expanded = -1
                    },
                    atEnd = mode == Player.REPEAT_MODE_ALL,
                    modifier = Modifier.layoutId(R.id.np_option_6),
                    tint = onColor.copy(if (mode == Player.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
                )

                if (!state.isVideo) return@Row

                val controller = LocalNavController.current
                // FixMe: State is not required here. implement to get value without state.
                val useBuiltIn by preference(key = Settings.USE_IN_BUILT_AUDIO_FX)
                IconButton(
                    onClick = {
                        if (useBuiltIn)
                            controller.navigate(AudioFx.route)
                        else
                            facade.launchEqualizer(state.audioSessionId)
                        expanded = -1
                    },
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.layoutId(R.id.np_option_1),
                    tint = onColor
                )

                val favourite = state.favourite
                LottieAnimButton(
                    id = R.raw.lt_twitter_heart_filled_unfilled,
                    onClick = { state.toggleFav(); facade.launchReviewFlow(); expanded = -1 },
                    modifier = Modifier.layoutId(R.id.np_option_0),
                    scale = 3.5f,
                    progressRange = 0.13f..0.95f,
                    duration = 800,
                    atEnd = !favourite
                )

                // Lock
                IconButton(
                    imageVector = Icons.Outlined.Lock,
                    onClick = { state.visibility = Visibility.Locked() }
                )
            }

            //
            Divider()

            // Audio
            DropdownMenuItem2(
                title = buildAnnotatedString {
                    append("Audio")
                    withSpanStyle(
                        color = onColor.copy(ContentAlpha.disabled),
                        fontSize = 11.sp
                    ) {
                        append("\n${state.currAudioTrack?.name ?: "Auto"}")
                    }
                },
                onClick = { expanded = 1 },
                leading = rememberVectorPainter(image = Icons.Outlined.Speaker),
                enabled = state.isVideo
            )

            // Subtitle
            DropdownMenuItem2(
                title = buildAnnotatedString {
                    append("Subtitle")
                    withSpanStyle(
                        color = onColor.copy(ContentAlpha.disabled),
                        fontSize = 11.sp
                    ) {
                        append("\n${state.currSubtitleTrack?.name ?: "Off"}")
                    }
                },
                onClick = { expanded = 2 },
                leading = rememberVectorPainter(image = Icons.Outlined.ClosedCaption),
                enabled = state.isVideo
            )
        }

        // Audio Menu
        DropdownMenu2(
            expanded = expanded == 1,
            onDismissRequest = { expanded = 0 }
        ) {
            DropdownMenuItem2(
                title = "Auto",
                onClick = { state.currAudioTrack = null; expanded = 0 })
            state.audios.forEach { track ->
                DropdownMenuItem2(
                    title = track.name,
                    onClick = { state.currAudioTrack = track; expanded = 0 })
            }
        }

        // Subttile Menu
        DropdownMenu2(
            expanded = expanded == 2,
            onDismissRequest = { expanded = 0 }
        ) {
            DropdownMenuItem2(
                title = "Off",
                onClick = { state.currSubtitleTrack = null; expanded = 0 })
            state.subtiles.forEach { track ->
                DropdownMenuItem2(
                    title = track.name,
                    onClick = { state.currSubtitleTrack = track; expanded = 0 })
            }
        }
    }
}

/**
 * An Icon Button That houses the logic for displaying/hiding the [PlayingQueue]
 */
@Composable
private inline fun Queue(
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
private inline fun PlayingSpeed(
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
private inline fun SleepAfter(
    state: Console,
    modifier: Modifier = Modifier
) {
    var showSleepAfter by remember { mutableStateOf(false) }
    Timer(
        expanded = showSleepAfter,
        onValueChange = {
            if (it != -2L)
                state.sleepAfterMills = it
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
private fun Controls(
    state: Console,
    modifier: Modifier = Modifier,
    accent: Color = Material.colors.primary,
) {
    val isVideo = state.isVideo
    val portrait =
        LocalConfiguration.current.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    // Determine the right type of Constraints.
    val constraints = when {
        portrait && isVideo -> Console.VideoPortraitConstraintSet
        portrait && !isVideo -> Console.AudioPortraitConstraintSet
        !portrait && isVideo -> Console.VideoLandscapeConstraintSet
        else -> Console.AudioLandscapeConstraintSet
    }
    // Place the items.
    ConstraintLayout(
        modifier = modifier,
        constraintSet = constraints
    ) {

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
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                accent
            ),
            contentPadding = PaddingValues(vertical = 16.dp),
            shape = RoundedCornerShape_24,
            content = { Icon(imageVector = Icons.Outlined.Close, contentDescription = "Collpase") },
        )

        // Artwork
        Artwork(
            value = state.artwork,
            modifier = Modifier.layoutId(R.id.np_artwork),
            //isRotating = state.playing
        )

        // Timer
        Label(
            text = state.position(LocalContentColor.current.copy(ContentAlpha.disabled)),
            modifier = Modifier.layoutId(R.id.np_timer),
            style = Material.typography.caption2,
            fontWeight = FontWeight.Bold
        )

        // Subtitle
        Label(
            text = state.subtitle ?: stringResource(id = R.string.unknown),
            style = Material.typography.caption2,
            modifier = Modifier.layoutId(R.id.np_subtitle),
            color = onColor
        )

        // Title
        Label(
            text = state.title ?: stringResource(id = R.string.unknown),
            fontSize = if (isVideo) 16.sp else 44.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .marque(Int.MAX_VALUE)
                .layoutId(R.id.np_title),
            color = onColor
        )


        // Skip to Prev
        var enabled = !state.isFirst
        val facade = LocalSystemFacade.current
        IconButton(
            onClick = { state.skipToPrev(); facade.launchReviewFlow() },
            painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowLeft),
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_skip_to_prev),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // Skip to Next
        enabled = !state.isLast
        IconButton(
            onClick = { state.skipToNext(); facade.launchReviewFlow() },
            painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowRight),
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_skip_to_next),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // SeekBack_10
        enabled = state.progress != -1f
        IconButton(
            onClick = { state.seek(mills = -TimeUnit.SECONDS.toMillis(10)) },
            imageVector = Icons.Outlined.Replay10,
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_seek_back_10),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // SeekForward_30
        IconButton(
            onClick = { state.seek(mills = TimeUnit.SECONDS.toMillis(30)) },
            imageVector = Icons.Outlined.Forward30,
            contentDescription = null,
            modifier = Modifier.layoutId(R.id.np_seek_forward_30),
            enabled = enabled,
            tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
        )

        // Option Row
        // Playing Queue / Option 2 | Option Row
        Queue(
            state = state,
            modifier = Modifier.layoutId(R.id.np_option_2)
        )

        // Playing Speed | Option 3
        PlayingSpeed(
            state = state,
            Modifier.layoutId(R.id.np_option_3)
        )

        // SleepAfter | Option 4
        SleepAfter(
            state = state,
            Modifier.layoutId(R.id.np_option_4)
        )

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

        // More
        More(state = state, modifier = Modifier.layoutId(R.id.np_option_6))

        // Audio
        if (!isVideo) {
            val favourite = state.favourite
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
                value = if (state.isSeekable) state.progress else 1f,
                onValueChange = { state.progress = it },
                modifier = Modifier.layoutId(R.id.np_slider),
                waveLength = 75.dp,
                waveHeight = 60.dp,
                shouldFlatten = true,
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
            // return from here
            return@ConstraintLayout
        }

        // else video
        val playing = state.playing
        IconButton(
            painter = painterResource(id = if (playing) R.drawable.media3_notification_pause else R.drawable.media3_notification_play),
            modifier = Modifier
                .size(60.dp)
                .layoutId(R.id.np_play_toggle)
                .scale(2f),
            onClick = { state.togglePlay(); facade.launchReviewFlow() }
        )

        Slider(
            value = if (state.isSeekable) state.progress else 1f,
            onValueChange = { state.progress = it },
            modifier = Modifier.layoutId(R.id.np_slider),
            colors = SliderDefaults.colors(activeTrackColor = accent, thumbColor = accent)
        )

        val activity = LocalContext.activity
        IconButton(
            imageVector = Icons.Outlined.ScreenRotation,
            onClick = { activity.toggleRotation() },
            modifier = Modifier.layoutId(R.id.np_option_1),
        )

        val resizeMode = state.resizeMode
        IconButton(
            imageVector = if (resizeMode == Console.RESIZE_MODE_FILL) Icons.Outlined.Fullscreen else Icons.Outlined.FitScreen,
            onClick = state::toggleResizeMode,
            modifier = Modifier.layoutId(R.id.np_option_0),
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun Console(state: Console) {
    Box(modifier = Modifier.fillMaxSize()) {
        val showVideoPlayer = state.isVideo
        val showController = state.visibility.isVisible
        // Show Background or VideoPlayer
        when (showVideoPlayer) {
            // Background
            false -> Spacer(
                Modifier
                    .background(Material.colors.background)
                    .fillMaxSize()
            )

            // Show Video Player.
            else -> PlayerView(
                player = state.player,
                resizeMode = state.resizeMode,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput("") {
                        detectTapGestures {
                            val visibility = state.visibility
                            if (visibility is Visibility.Visible) return@detectTapGestures
                            state.visibility = visibility.toggle()
                        }
                    }
            )
        }
        // Overlay with controller
        // The Controller
        val contentColor = if (showVideoPlayer) Color.White else Material.colors.onSurface
        CompositionLocalProvider(value = LocalContentColor provides contentColor) {
            // Always how controller if not video
            AnimatedVisibility(
                visible = showController || !showVideoPlayer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Controls(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    accent = if (showVideoPlayer) Color.White else Material.colors.primary,
                )
            }
        }
        // only execute from here onwards if is video player
        if (!showVideoPlayer)
            return@Box
        val activity = LocalContext.activity
        val controller =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        //Show Lock in case in progress of getting closed
        if (state.visibility is Visibility.Locked && (state.visibility as Visibility.Locked).mills != 0L)
            IconButton(
                imageVector = Icons.Outlined.LockOpen,
                onClick = { state.visibility = Visibility.Limited() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(40.dp),
                tint = Color.White
            )

        // Set immersive mode based on the visibility state.
        controller.immersiveMode(showController)

        // Call to pause the screen when the user intends to leave the screen, and the current content is a video.
        val owner = LocalLifecycleOwner.current

        // Determine the default appearance of light system bars based on user preferences and material theme.
        val defaultLightSystemBars =
            preference(key = Settings.COLOR_STATUS_BAR).value && !Material.colors.isLight

        // Use DisposableEffect to observe the lifecycle events of the current owner (typically the current composable).
        DisposableEffect(key1 = owner) {

            // Define a LifecycleEventObserver to pause playback when the screen is paused.
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) {
                    // Pause the playback when the screen is paused.
                    state.isPlaying = false
                }
            }

            // Add the observer to the owner's lifecycle.
            owner.lifecycle.addObserver(observer)

            // Set the appearance of system bars to ensure visibility in a video playback scenario.
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false

            // Define cleanup logic when the effect is disposed of.
            onDispose {
                // Remove the observer from the owner's lifecycle.
                owner.lifecycle.removeObserver(observer)

                // Restore the default color appearance of system bars based on the theme.
                controller.isAppearanceLightStatusBars = defaultLightSystemBars
                controller.isAppearanceLightNavigationBars = defaultLightSystemBars

                // Reset the screen orientation to allow the system to manage it automatically.
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}