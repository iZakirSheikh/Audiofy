@file:Suppress("InfiniteTransitionLabel", "InfinitePropertiesLabel", "AnimatedContentLabel")

package com.prime.media.console

import android.text.format.DateUtils.formatElapsedTime
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.Anim
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
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
import com.prime.media.core.util.DateUtils
import com.prime.media.darkShadowColor
import com.prime.media.dialog.PlaybackSpeedDialog
import com.prime.media.dialog.PlayingQueue
import com.prime.media.dialog.Timer
import com.prime.media.effects.AudioFx
import com.prime.media.lightShadowColor
import com.prime.media.outline
import com.prime.media.settings.Settings
import com.primex.core.lerp
import com.primex.core.rememberState
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton2
import com.primex.material2.neumorphic.NeumorphicButton
import com.primex.material2.neumorphic.NeumorphicButtonDefaults
import ir.mahozad.multiplatform.WavySlider

private const val TAG = "Console"

// Constraint reference of components.
// Signature Row
private val SIGNATURE = ConstrainedLayoutReference("_signature")
private val CLOSE = ConstrainedLayoutReference("_close")

// Artwork Row
private val ARTWORK = ConstrainedLayoutReference("_artwork")
private val PROGRESS_MILLS = ConstrainedLayoutReference("_progress_mills")
private val DURATION = ConstrainedLayoutReference("_id_duration")

// Title
private val SUBTITLE = ConstrainedLayoutReference("_subtitle")
private val TITLE = ConstrainedLayoutReference("_title")

// Slider
private val HEART = ConstrainedLayoutReference("_heart")
private val SLIDER = ConstrainedLayoutReference("_slider")
private val EQUALIZER = ConstrainedLayoutReference("_equalizer")

// Controls
private val SKIP_TO_PREVIOUS = ConstrainedLayoutReference("_previous")
private val SKIP_BACK_10 = ConstrainedLayoutReference("_skip_back_10")
private val TOGGLE = ConstrainedLayoutReference("_toggle")
private val SKIP_FORWARD_30 = ConstrainedLayoutReference("_skip_forward_30")
private val SKIP_TO_NEXT = ConstrainedLayoutReference("_next")

// Buttons
private val OPTION_1 = ConstrainedLayoutReference("_shuffle")
private val OPTION_2 = ConstrainedLayoutReference("_repeat")
private val OPTION_3 = ConstrainedLayoutReference("_queue")
private val OPTION_4 = ConstrainedLayoutReference("_speed")
private val OPTION_5 = ConstrainedLayoutReference("_sleep")

private inline val MediaItem.fTitle
    get() = mediaMetadata.title?.toString()
private inline val MediaItem.fSubtitle
    get() = mediaMetadata.subtitle?.toString()

/**
 * A simple extension fun to add to modifier.
 */
private inline fun Modifier.constraintAs(id: ConstrainedLayoutReference) = layoutId(id.id)

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
        LottieAnimation(
            id = R.raw.lt_play_pause,
            atEnd = !isPlaying,
            scale = 1.5f,
            progressRange = 0.0f..0.29f,
            duration = Anim.MediumDurationMills,
            easing = LinearEasing
        )
    }
}

private val ARTWORK_STROKE_DEFAULT_EXPANDED = 8.dp
private val ARTWORK_STROKE_DEFAULT_COLLAPSED = 3.dp

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

private val Expanded = ConstraintSet {
    // signature
    constrain(SIGNATURE) {
        start.linkTo(parent.start, ContentPadding.normal)
        top.linkTo(parent.top, ContentPadding.medium)
    }

    constrain(CLOSE) {
        end.linkTo(parent.end, ContentPadding.normal)
        top.linkTo(SIGNATURE.top)
        bottom.linkTo(SIGNATURE.bottom)
    }

    // artwork
    constrain(ARTWORK) {
        linkTo(parent.start, SIGNATURE.bottom, parent.end, SUBTITLE.top)
        height = Dimension.fillToConstraints
        width = Dimension.ratio("1:1")
    }

    constrain(PROGRESS_MILLS) {
        end.linkTo(ARTWORK.end, 50.dp)
        top.linkTo(ARTWORK.top)
        bottom.linkTo(ARTWORK.bottom)
    }

    //Title
    constrain(SUBTITLE) {
        start.linkTo(TITLE.start)
        bottom.linkTo(TITLE.top)
    }

    constrain(TITLE) {
        bottom.linkTo(SLIDER.top, ContentPadding.normal)
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
        width = Dimension.fillToConstraints
    }

    //progressbar
    constrain(SLIDER) {
        bottom.linkTo(TOGGLE.top, ContentPadding.normal)
        start.linkTo(HEART.end, ContentPadding.medium)
        end.linkTo(EQUALIZER.start, ContentPadding.medium)
        width = Dimension.fillToConstraints
    }

    constrain(HEART) {
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
        start.linkTo(TITLE.start)
    }

    constrain(EQUALIZER) {
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
        end.linkTo(TITLE.end)
    }

    // play controls row
    constrain(TOGGLE) {
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        bottom.linkTo(OPTION_3.top, ContentPadding.xLarge)
    }

    constrain(SKIP_TO_PREVIOUS) {
        end.linkTo(TOGGLE.start, ContentPadding.normal)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    constrain(SKIP_BACK_10) {
        end.linkTo(SKIP_TO_PREVIOUS.start, ContentPadding.medium)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    constrain(SKIP_TO_NEXT) {
        start.linkTo(TOGGLE.end, ContentPadding.normal)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    constrain(SKIP_FORWARD_30) {
        start.linkTo(SKIP_TO_NEXT.end, ContentPadding.medium)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    val ref =
        createHorizontalChain(OPTION_3, OPTION_4, OPTION_5, OPTION_1, OPTION_2, chainStyle = ChainStyle.Packed)
    constrain(ref) {
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
    }

    constrain(OPTION_3) {
        bottom.linkTo(parent.bottom, ContentPadding.xLarge)
    }

    constrain(OPTION_4) {
        bottom.linkTo(OPTION_3.bottom)
    }

    constrain(OPTION_5) {
        bottom.linkTo(OPTION_3.bottom)
    }

    constrain(OPTION_1) {
        bottom.linkTo(OPTION_3.bottom)
    }

    constrain(OPTION_2) {
        bottom.linkTo(OPTION_3.bottom)
    }
}

@Composable
private inline fun Controls(
    state: Console,
    noinline onRequestToggle: () -> Unit
) {
    var onColor = Material.colors.onSurface
    val insets = WindowInsets.statusBars
    // Signature
    Text(
        text = stringResource(id = R.string.app_name),
        fontFamily = Settings.DancingScriptFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 70.sp,
        modifier = Modifier
            .windowInsetsPadding(insets)
            .constraintAs(SIGNATURE),
        color = onColor,
        maxLines = 1
    )
    // Close Button
    OutlinedButton2(
        onClick = onRequestToggle,
        modifier = Modifier
            .windowInsetsPadding(insets)
            .scale(0.8f)
            .constraintAs(CLOSE),
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
        modifier = Modifier.constraintAs(ARTWORK),
        progress = 1.0f,
        isPlaying = state.playing
    )


    //slider
    val value = state.progress
    val time = state.position
    Label(
        text = DateUtils.formatAsDuration(time),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.constraintAs(PROGRESS_MILLS),
        fontSize = 34.sp
    )

    // Slider
    // The Wavy has minSDK of 24; currently don't know if it might trigger some error below API 24.
    // So be carefully until I found some new solution.
    WavySlider(
        value = value,
        onValueChange = { state.seekTo(it) },
        modifier = Modifier.constraintAs(SLIDER),
        waveLength = 75.dp,
        waveHeight = 60.dp
    )

    val favourite = state.favourite
    val facade = LocalSystemFacade.current
    LottieAnimButton(
        id = R.raw.lt_twitter_heart_filled_unfilled,
        onClick = { state.toggleFav(); facade.launchReviewFlow() },
        modifier = Modifier.constraintAs(HEART),
        scale = 3.5f,
        progressRange = 0.13f..0.95f,
        duration = 800,
        atEnd = !favourite
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
        modifier = Modifier.constraintAs(EQUALIZER),
        tint = onColor
    )

    //title
    val current = state.current
    Label(
        text = current?.fSubtitle ?: stringResource(id = R.string.unknown),
        style = Material.typography.caption2,
        modifier = Modifier
            .offset(y = 4.dp, x = 5.dp)
            .constraintAs(SUBTITLE),
        color = onColor
    )

    Label(
        text = current?.fTitle ?: stringResource(id = R.string.unknown),
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .marque(Int.MAX_VALUE)
            .constraintAs(TITLE),
        color = onColor
    )

    // playButton
    val playing = state.playing
    PlayButton(
        onClick = { state.togglePlay(); facade.launchReviewFlow() },
        progress = 1.0f,
        isPlaying = playing,
        modifier = Modifier
            .size(60.dp)
            .constraintAs(TOGGLE),
    )

    var enabled = if (current != null) !state.isFirst else false
    IconButton(
        onClick = { state.skipToPrev(); facade.launchReviewFlow() },
        painter = painterResource(id = R.drawable.ic_skip_to_prev),
        contentDescription = null,
        modifier = Modifier.constraintAs(SKIP_TO_PREVIOUS),
        enabled = enabled,
        tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
    )

    enabled = if (current != null) !state.isLast else false
    IconButton(
        onClick = { state.skipToNext(); facade.launchReviewFlow() },
        painter = painterResource(id = R.drawable.ic_skip_to_next),
        contentDescription = null,
        modifier = Modifier.constraintAs(SKIP_TO_NEXT),
        enabled = enabled,
        tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
    )

    enabled = playing
    IconButton(
        onClick = { state.replay() },
        imageVector = Icons.Outlined.Replay10,
        contentDescription = null,
        modifier = Modifier.constraintAs(SKIP_BACK_10),
        enabled = enabled,
        tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
    )

    IconButton(
        onClick = { state.forward() },
        imageVector = Icons.Outlined.Forward30,
        contentDescription = null,
        modifier = Modifier.constraintAs(SKIP_FORWARD_30),
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
        modifier = Modifier.constraintAs(OPTION_3),
        tint = onColor
    )

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
        modifier = Modifier.constraintAs(OPTION_4),
        tint = onColor
    )

    var showSleepAfter by remember { mutableStateOf(false) }
    Timer(
        expanded = showSleepAfter,
        onValueChange = {
            if (it != -2L)
                state.setSleepAfter(it)
            showSleepAfter = false
        }
    )

    androidx.compose.material.IconButton(
        onClick = { showSleepAfter = true },
        modifier = Modifier.constraintAs(OPTION_5),
        content = {
            val mills = state.sleepAfterMills
            if (mills == -1L)
                return@IconButton Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = onColor
                )
            Label(
                text = formatElapsedTime(mills / 1000L),
                style = Material.typography.caption2,
                fontWeight = FontWeight.Bold,
                color = Material.colors.secondary
            )
        }
    )

    val shuffle = state.shuffle
    LottieAnimButton(
        id = R.raw.lt_shuffle_on_off,
        onClick = { state.toggleShuffle(); facade.launchReviewFlow(); },
        modifier = Modifier.constraintAs(OPTION_1),
        atEnd = !shuffle,
        progressRange = 0f..0.8f,
        scale = 1.5f
    )

    val mode = state.repeatMode
    AnimatedIconButton(
        id = R.drawable.avd_repeat_more_one_all,
        onClick = { state.cycleRepeatMode();facade.launchReviewFlow(); },
        atEnd = mode == Player.REPEAT_MODE_ALL,
        modifier = Modifier.constraintAs(OPTION_2),
        tint = onColor.copy(if (mode == Player.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
    )
}

@Composable
fun Console(state: Console) {
    ConstraintLayout(constraintSet = Expanded) {
        Spacer(
            modifier = Modifier
                .background(Material.colors.background)
                .fillMaxSize()
        )
        val navController = LocalNavController.current
        Controls(state = state, navController::navigateUp)
    }
}