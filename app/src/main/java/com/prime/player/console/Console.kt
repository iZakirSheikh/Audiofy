package com.prime.player.console

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.player.*
import com.prime.player.R
import com.prime.player.common.*
import com.prime.player.core.Util
import com.prime.player.core.formatAsDuration
import com.primex.core.gradient
import com.primex.core.lerp
import com.primex.core.rememberState
import com.primex.core.shadow.SpotLight
import com.primex.core.shadow.shadow
import com.primex.ui.*
import com.primex.ui.dialog.BottomSheetDialog
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "Console"

//Constraint reference of components.
private val Signature = ConstrainedLayoutReference("_signature")
private val Close = ConstrainedLayoutReference("_close")
private val Heart = ConstrainedLayoutReference("_heart")


private val Artwork = ConstrainedLayoutReference("_artwork")
private val ProgressMills = ConstrainedLayoutReference("_progress_mills")

private val Subtitle = ConstrainedLayoutReference("_subtitle")
private val Title = ConstrainedLayoutReference("_title")

private val ProgressBar = ConstrainedLayoutReference("_progress_bar")
private val TuneUp = ConstrainedLayoutReference("_tune_up")


private val SkipToPrevious = ConstrainedLayoutReference("_previous")
private val SkipBack10 = ConstrainedLayoutReference("_skip_back_10")
private val Toggle = ConstrainedLayoutReference("_toggle")
private val SkipForward30 = ConstrainedLayoutReference("_skip_forward_30")
private val SkipToNext = ConstrainedLayoutReference("_next")

private val BottomRowLabel = ConstrainedLayoutReference("_bottom_row_label")
private val Shuffle = ConstrainedLayoutReference("_shuffle")
private val Repeat = ConstrainedLayoutReference("_repeat")
private val Queue = ConstrainedLayoutReference("_queue")
private val Speed = ConstrainedLayoutReference("_speed")
private val Sleep = ConstrainedLayoutReference("_sleep")

val edgeWidth = 10.dp
fun ContentDrawScope.drawFadedEdge(leftEdge: Boolean) {
    val edgeWidthPx = edgeWidth.toPx()
    drawRect(
        topLeft = Offset(if (leftEdge) 0f else size.width - edgeWidthPx, 0f),
        size = Size(edgeWidthPx, size.height),
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startX = if (leftEdge) 0f else size.width,
            endX = if (leftEdge) edgeWidthPx else size.width - edgeWidthPx
        ),
        blendMode = BlendMode.DstIn
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.marque(iterations: Int) =
    Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawFadedEdge(leftEdge = true)
            drawFadedEdge(leftEdge = false)
        }
        .basicMarquee(
            // Animate forever.
            iterations = iterations,
        )
        .then(this)

/**
 * A simple extension fun to add to modifier.
 */
private inline fun Modifier.layoutID(id: ConstrainedLayoutReference) =
    layoutId(id.id)

private inline fun ConstraintSetScope.hide(vararg ref: ConstrainedLayoutReference) {
    ref.forEach {
        constrain(it) {
            //start.linkTo(parent.start)
            end.linkTo(parent.start)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            visibility = Visibility.Gone
        }
    }
}

private inline val MediaItem.title
    get() =
        mediaMetadata.title?.toString()
private inline val MediaItem.subtitle
    get() =
        mediaMetadata.subtitle?.toString()

@Composable
@NonRestartableComposable
private fun NeumorphicIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(30),
    enabled: Boolean = true,
    border: BorderStroke? = if (Material.colors.isLight) null else BorderStroke(
        1.dp,
        Material.colors.outline.copy(0.06f)
    ),
    elevation: ButtonElevation = NeumorphicButtonDefaults.elevation(6.dp),
    iconScale: Float = 1.5f,
    painter: Painter
) {
    NeumorphicButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        enabled = enabled,
        elevation = elevation,
        border = border,
        colors = NeumorphicButtonDefaults.neumorphicButtonColors(
            lightShadowColor = Material.colors.lightShadowColor,
            darkShadowColor = Material.colors.darkShadowColor,
        )
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.scale(iconScale)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NeumorphicVertButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alpha: Float = LocalContentAlpha.current,
    onClick: () -> Unit,
    icon: Painter,
    label: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val elevation = NeumorphicButtonDefaults.elevation(5.dp)
        val source = remember(::MutableInteractionSource)
        val depth by elevation.elevation(enabled = enabled, interactionSource = source)
        Neumorphic(
            onClick = onClick,
            modifier = Modifier,
            lightShadowColor = Material.colors.lightShadowColor,
            darkShadowColor = Material.colors.darkShadowColor,
            elevation = depth,
            interactionSource = source,
            border = if (Material.colors.isLight) null else BorderStroke(
                1.dp,
                Material.colors.outline.copy(0.06f)
            ),
            shape = RoundedCornerShape(30)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = LocalContentColor.current.copy(alpha)
            )
        }


        Label(
            text = label,
            style = Material.typography.caption2,
            modifier = Modifier.padding(top = 6.dp),
            color = LocalContentColor.current.copy(alpha)
        )
    }
}

private val ARTWORK_STROKE_DEFAULT_EXPANDED = 20.dp
private val ARTWORK_STROKE_DEFAULT_COLLAPSED = 3.dp

@Composable
@NonRestartableComposable
private fun Artwork(
    data: Any?,
    modifier: Modifier = Modifier,
    stroke: Dp = ARTWORK_STROKE_DEFAULT_EXPANDED,
) {
    val color = Material.colors.background
    Image(
        data = data,
        contentScale = ContentScale.Crop,
        fadeMills = Anim.LongDurationMills,

        // now apply the modifier.
        modifier = Modifier
            .shadow(
                shape = CircleShape,
                elevation = -12.dp,
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor,
                spotLight = SpotLight.BOTTOM_RIGHT,
            )
            .padding(stroke)
            .shadow(
                shape = CircleShape,
                elevation = 12.dp,
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor,
                spotLight = SpotLight.TOP_LEFT,
            )
            .border(BorderStroke(stroke / 2, color), CircleShape)
            .gradient(colors = listOf(Color.Transparent, Color.Black.copy(0.5f)), vertical = false)
            .background(color)
            .then(modifier)
    )
}


@Deprecated("Move this launching to Playback; because of easily availability of custom audioSessionId.")
private fun Activity.launchEqualizer(id: Int) {
    if (id == AudioEffect.ERROR_BAD_VALUE) {
        Toast.makeText(this, "No Session Id", Toast.LENGTH_LONG).show();
        return
    }
    val res = kotlin.runCatching {
        startActivityForResult(
            Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, "your app package name");
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id);
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
            },
            0
        )
    }

    if (res.exceptionOrNull() is ActivityNotFoundException)
        Toast.makeText(this, "There is no equalizer", Toast.LENGTH_SHORT).show();
}

@Composable
fun Console(
    viewModel: ConsoleViewModel,
    progress: Float,
    onRequestToggle: () -> Unit
) {
    val controller = rememberSystemUiController()
    val expanded = progress == 1f
    val wasDark = remember {
        controller.statusBarDarkContentEnabled
    }
    val isLight = Material.colors.isLight
    val channel = LocalContext.toastHostState

    // move messenger to view-model using hilt.
    DisposableEffect(key1 = isLight, key2 = expanded) {
        // set icon color for current screen
        controller.setStatusBarColor(Color.Transparent, if (expanded) isLight else wasDark)
        viewModel.messenger = channel
        onDispose {
            // restore back the color of the old screen.
            controller.setStatusBarColor(Color.Transparent, wasDark)
            viewModel.messenger = null
        }
    }

    // actual content
    CompositionLocalProvider(LocalContentColor provides Material.colors.onSurface) {
        //Maybe Use Modifier.composed {}
        Vertical(
            progress = progress,
            resolver = viewModel,
            onRequestToggle = onRequestToggle,
            modifier = Modifier
                .fillMaxSize()
                // animate 2.5x scale between collapsed and expanded.
                .scale(lerp(0.8f, 1f, (progress * 2.5f).coerceIn(0.0f..1.0f)))
                // animate shadow including its shape.
                .shadow(
                    shape = RoundedCornerShape(lerp(100f, 0f, progress).roundToInt()),
                    lightShadowColor = Material.colors.darkShadowColor,
                    darkShadowColor = Material.colors.darkShadowColor,
                    elevation = lerp(8.dp, 0.dp, progress),
                    spotLight = SpotLight.TOP_LEFT,
                    border = BorderStroke(lerp(2.dp, 0.dp, progress), Material.colors.surface)
                )
                .background(Material.colors.background)
                // .background(color = lerp(Material.colors.surface, Material.colors.background, 0.0f, 1.0f,  progress))
                // don't forget to disable it when expanded.
                .clickable(
                    onClick = onRequestToggle,
                    indication = null,
                    interactionSource = remember(::MutableInteractionSource),
                    // only enabled when collapsed.
                    enabled = !expanded
                )
        )
    }
}

private val collapsed =
    ConstraintSet {
        hide(Signature, Close)
        hide(ProgressBar, TuneUp, ProgressMills)
        hide(SkipForward30, SkipToNext, SkipBack10, SkipToPrevious)
        hide(BottomRowLabel)
        hide(Queue, Speed, Sleep, Shuffle, Repeat)

        constrain(Artwork) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start, ContentPadding.medium)
            height = Dimension.value(56.dp)
            width = Dimension.ratio("1:1")
        }

        createVerticalChain(Title, Subtitle, chainStyle = ChainStyle.Packed)
        constrain(Title) {
            start.linkTo(Artwork.end, ContentPadding.medium)
            end.linkTo(Heart.start, ContentPadding.medium)
            width = Dimension.fillToConstraints
        }


        constrain(Subtitle) {
            start.linkTo(Title.start)
            end.linkTo(Title.end)
            width = Dimension.fillToConstraints
            visibility = Visibility.Visible
        }

        constrain(Heart) {
            start.linkTo(Title.end)
            top.linkTo(Artwork.top)
            bottom.linkTo(Artwork.bottom)
        }

        // toggles
        constrain(Toggle) {
            start.linkTo(Heart.end)
            end.linkTo(parent.end)
            top.linkTo(Artwork.top)
            bottom.linkTo(Artwork.bottom)
        }
    }

val expanded =
    ConstraintSet {
        // signature
        constrain(Signature) {
            start.linkTo(parent.start, ContentPadding.normal)
            top.linkTo(parent.top)
        }

        constrain(Close) {
            end.linkTo(parent.end, ContentPadding.normal)
            top.linkTo(Signature.top)
            bottom.linkTo(Signature.bottom)
        }

        // artwork
        constrain(Artwork) {
            top.linkTo(Signature.bottom, ContentPadding.normal)
            bottom.linkTo(Subtitle.top, ContentPadding.normal)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            height = Dimension.fillToConstraints
            width = Dimension.ratio("1:1")
        }

        constrain(ProgressMills) {
            end.linkTo(Artwork.end, ContentPadding.large)
            top.linkTo(Artwork.top)
            bottom.linkTo(Artwork.bottom)
            visibility = Visibility.Visible
        }

        //title
        constrain(Title) {
            bottom.linkTo(ProgressBar.top, ContentPadding.normal)
            start.linkTo(parent.start, ContentPadding.large)
            end.linkTo(parent.end, ContentPadding.large)
            width = Dimension.fillToConstraints
        }

        constrain(Subtitle) {
            start.linkTo(Title.start)
            bottom.linkTo(Title.top)
        }

        //progressbar
        constrain(ProgressBar) {
            bottom.linkTo(Toggle.top, ContentPadding.normal)
            start.linkTo(Heart.end, ContentPadding.medium)
            end.linkTo(TuneUp.start, ContentPadding.medium)
            width = Dimension.fillToConstraints
        }

        constrain(Heart) {
            top.linkTo(ProgressBar.top)
            bottom.linkTo(ProgressBar.bottom)
            start.linkTo(Title.start)
        }

        constrain(TuneUp) {
            top.linkTo(ProgressBar.top)
            bottom.linkTo(ProgressBar.bottom)
            end.linkTo(Title.end)
        }

        // play controls row
        constrain(Toggle) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(Queue.top, ContentPadding.large)
        }

        constrain(SkipToPrevious) {
            end.linkTo(Toggle.start, ContentPadding.normal)
            top.linkTo(Toggle.top)
            bottom.linkTo(Toggle.bottom)
        }

        constrain(SkipBack10) {
            end.linkTo(SkipToPrevious.start, ContentPadding.medium)
            top.linkTo(Toggle.top)
            bottom.linkTo(Toggle.bottom)
        }

        constrain(SkipToNext) {
            start.linkTo(Toggle.end, ContentPadding.normal)
            top.linkTo(Toggle.top)
            bottom.linkTo(Toggle.bottom)
        }

        constrain(SkipForward30) {
            start.linkTo(SkipToNext.end, ContentPadding.medium)
            top.linkTo(Toggle.top)
            bottom.linkTo(Toggle.bottom)
        }

        val ref =
            createHorizontalChain(
                Queue,
                Speed,
                Sleep,
                Shuffle,
                Repeat,
                chainStyle = ChainStyle.SpreadInside
            )
        constrain(ref) {
            start.linkTo(parent.start, ContentPadding.large)
            end.linkTo(parent.end, ContentPadding.large)
        }

        constrain(Queue) {
            bottom.linkTo(parent.bottom, ContentPadding.large)
        }

        constrain(Speed) {
            bottom.linkTo(Queue.bottom)
        }

        constrain(Sleep) {
            bottom.linkTo(Queue.bottom)
        }

        constrain(Shuffle) {
            bottom.linkTo(Queue.bottom)
        }

        constrain(Repeat) {
            bottom.linkTo(Queue.bottom)
        }
    }

@OptIn(
    ExperimentalMotionApi::class, ExperimentalAnimationGraphicsApi::class,
    ExperimentalAnimationApi::class, ExperimentalComposeApi::class, ExperimentalComposeUiApi::class
)
@Composable
fun Vertical(
    modifier: Modifier = Modifier,
    resolver: ConsoleViewModel,
    progress: Float,
    onRequestToggle: () -> Unit
) {
    MotionLayout(
        start = collapsed,
        end = expanded,
        progress = progress,
        modifier = modifier
    ) {

        val primary = Material.colors.onSurface
        val activity = LocalContext.activity
        val insets = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // Signature
        Text(
            text = stringResource(id = R.string.app_name),
            fontFamily = FontFamily.Cursive,
            color = primary,
            fontWeight = FontWeight.Bold,
            fontSize = 70.sp,
            modifier = Modifier
                .padding(top = insets)
                .layoutID(Signature)
        )

        // Close Button
        NeumorphicIconButton(
            painter = rememberVectorPainter(image = Icons.Default.Close),
            onClick = onRequestToggle,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .size(46.dp)
                .layoutID(Close)
        )


        // artwork
        val artwork by resolver.artwork
        Artwork(
            data = artwork,
            modifier = Modifier.layoutID(Artwork),
            // maybe make this a lambda call
            stroke = lerp(
                ARTWORK_STROKE_DEFAULT_COLLAPSED,
                ARTWORK_STROKE_DEFAULT_EXPANDED,
                progress
            )
        )

        //slider
        val value by resolver.progress
        val time = (value * resolver.duration).roundToLong()
        Header(
            text = Util.formatAsDuration(time),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.layoutID(ProgressMills),
            style = Material.typography.h3
        )

        // slider row
        Slider(
            value = value,
            onValueChange = { resolver.seekTo(it) },
            modifier = Modifier.layoutID(ProgressBar),
        )

        val favourite by resolver.favourite
        IconButton(
            onClick = { resolver.toggleFav(); activity.launchReviewFlow() },
            painter = painterResource(id = if (favourite) R.drawable.ic_heart_filled else R.drawable.ic_heart),
            contentDescription = null,
            modifier = Modifier.layoutID(Heart)
        )

        IconButton(
            onClick = { activity.launchEqualizer(resolver.audioSessionId) },
            imageVector = Icons.Outlined.Tune,
            contentDescription = null,
            modifier = Modifier.layoutID(TuneUp)
        )


        //title
        val current by resolver.current
        AnimatedLabel(
            text = current?.subtitle ?: stringResource(id = R.string.unknown),
            style = Material.typography.caption2,
            modifier = Modifier
                .offset(y = 4.dp, x = 5.dp)
                .layoutID(Subtitle)
        )

        Label(
            text = current?.title ?: stringResource(id = R.string.unknown),
            fontSize = lerp(18.sp, 40.sp, progress),
            fontWeight = FontWeight.Bold,
            color = Material.colors.onSurface,
            modifier = Modifier
                .marque(Int.MAX_VALUE)
                .layoutID(Title),
        )

        // controls
        val playing by resolver.playing
        NeumorphicIconButton(
            onClick = { resolver.togglePlay(); activity.launchReviewFlow() },

            painter = rememberAnimatedVectorResource(
                id = R.drawable.avd_pause_to_play,
                atEnd = !playing
            ),
            shape = RoundedCornerShape(30),
            modifier = Modifier
                .size(60.dp)
                .layoutID(Toggle),
            elevation = NeumorphicButtonDefaults.elevation(lerp(0.dp, 6.dp, progress)),
            border = if (progress != 0f && !Material.colors.isLight) BorderStroke(
                1.dp,
                Material.colors.outline.copy(0.06f)
            ) else null
        )


        IconButton(
            onClick = { resolver.skipToPrev(); activity.launchReviewFlow() },
            //   shape = RoundedCornerShape(10.dp),
            painter = painterResource(id = R.drawable.ic_skip_to_prev),
            // iconScale = 0.8f,
            contentDescription = null,
            modifier = Modifier.layoutID(SkipToPrevious),
            enabled = if (current != null) resolver.hasPreviousTrack else false
        )


        IconButton(
            onClick = { resolver.skipToNext(); activity.launchReviewFlow() },
            //shape = RoundedCornerShape(10.dp),
            painter = painterResource(id = R.drawable.ic_skip_to_next),
            contentDescription = null,
            //iconScale = 0.8f,
            modifier = Modifier.layoutID(SkipToNext),
            enabled = if (current != null) resolver.hasNextTrack else false,
        )

        IconButton(
            onClick = { resolver.replay10() },
            imageVector = Icons.Outlined.Replay10,
            contentDescription = null,
            modifier = Modifier.layoutID(SkipBack10)
        )

        IconButton(
            onClick = { resolver.forward30() },
            imageVector = Icons.Outlined.Forward30,
            contentDescription = null,
            modifier = Modifier.layoutID(SkipForward30)
        )

        var showPlayingQueue by rememberState(initial = false)
        resolver.PlayingQueue(
            expanded = showPlayingQueue,
            onDismissRequest = {
                showPlayingQueue = false
            }
        )

        NeumorphicVertButton(
            onClick = { showPlayingQueue = true },
            icon = rememberVectorPainter(image = Icons.Outlined.Queue),
            label = "Queue",
            modifier = Modifier.layoutID(Queue)
        )

        var showSpeedController by rememberState(initial = false)
        BottomSheetDialog(
            expanded = showSpeedController,
            onDismissRequest = { showSpeedController = false }) {
            var speed by rememberState(initial = resolver.playbackSpeed)
            SpeedControllerLayout(
                value = speed,
                onRequestChange = { speed = it; resolver.setPlaybackSpeed(it) })
        }

        NeumorphicVertButton(
            onClick = { showSpeedController = true },
            icon = rememberVectorPainter(image = Icons.Outlined.Speed),
            label = "Speed",
            modifier = Modifier.layoutID(Speed),
            alpha = ContentAlpha.high
        )

        NeumorphicVertButton(
            onClick = { /*TODO: Implement this.*/ resolver.setSleepAfter(1) },
            icon = rememberVectorPainter(image = Icons.Outlined.Timer),
            label = "Sleep",
            modifier = Modifier.layoutID(Sleep),
            alpha = ContentAlpha.high
        )

        val shuffle by resolver.shuffle
        NeumorphicVertButton(
            onClick = { resolver.toggleShuffle(); activity.launchReviewFlow(); },
            icon = painterResource(id = R.drawable.ic_shuffle),
            label = "Shuffle",
            modifier = Modifier.layoutID(Shuffle),
            alpha = if (shuffle) ContentAlpha.high else ContentAlpha.disabled
        )

        val mode by resolver.repeatMode
        NeumorphicVertButton(
            onClick = { resolver.cycleRepeatMode();activity.launchReviewFlow(); },
            icon = painterResource(id = if (mode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat),
            label = "Repeat",
            modifier = Modifier.layoutID(Repeat),
            alpha = if (mode == Player.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high
        )
    }
}

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
                    horizontal = ContentPadding.large,
                )
            )
        }
    }
}