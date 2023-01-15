@file:Suppress("NOTHING_TO_INLINE")

package com.prime.player.console

import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.*
import androidx.media3.common.Player
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.player.*
import com.prime.player.R
import com.prime.player.common.*
import com.prime.player.core.Util
import com.prime.player.core.db.Audio
import com.prime.player.core.formatAsDuration
import com.prime.player.core.share
import com.prime.player.tracks.TracksRoute
import com.primex.core.*
import com.primex.core.shadow.SpotLight
import com.primex.core.shadow.shadow
import com.primex.ui.*
import com.primex.ui.views.MarqueText
import cz.levinzonr.saferoute.core.navigateTo

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalAnimationGraphicsApi::class
)
@Composable
private fun ConsoleViewModel.MiniLayout(
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier
    ) {
        val (Artwork, Title, Subtitle, Heart, Play, ProgressBar) = createRefs()
        val color = Material.colors.surface

        createHorizontalChain(Artwork, Title, Heart, Play)
        val activity = LocalContext.activity
        //artwork
        val artwork by artwork
        Image(
            bitmap = artwork,
            modifier = Modifier
                .offset(x = -ContentPadding.medium)
                .requiredWidth(75.dp)
                .gradient(vertical = false, listOf(Color.Transparent, color))
                .fillMaxHeight()
                .constrainAs(Artwork) {},
        )

        //INFO create vertical chain of title ans subtitle
        constrain(
            ref = createVerticalChain(Title, Subtitle, chainStyle = ChainStyle.Packed(0.5f)),
            constrainBlock = {
                top.linkTo(Artwork.top)
                bottom.linkTo(Artwork.bottom)
            }
        )

        //title
        val current by current
        MarqueText(
            text = current?.name ?: "",
            modifier = Modifier.constrainAs(Title) {
                width = Dimension.fillToConstraints
            },
            typeface = Typeface.DEFAULT_BOLD,
            textSize = 12.sp
        )

        //subtitle
        AnimatedLabel(
            text = current?.album ?: stringResource(id = R.string.unknown),
            fontWeight = FontWeight.SemiBold,
            color = LocalContentColor.current.copy(0.8f),
            modifier = Modifier.constrainAs(Subtitle) {
                start.linkTo(Title.start)
                end.linkTo(Title.end)
                width = Dimension.fillToConstraints
            },
            style = Material.typography.caption2
        )

        val favourite by favourite
        IconButton(
            onClick = { toggleFav(); activity.launchReviewFlow() },
            painter = painterResource(id = if (favourite) R.drawable.ic_heart_filled else R.drawable.ic_heart),
            modifier = Modifier.constrainAs(Heart) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            },
            contentDescription = null
        )

        //play/pause
        val playing by playing
        IconButton(
            onClick = { togglePlay(); activity.launchReviewFlow() },
            contentDescription = null,
            painter = rememberAnimatedVectorResource(
                id = R.drawable.avd_pause_to_play,
                atEnd = !playing
            ),
            modifier = Modifier.constrainAs(Play) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
        )

        val showProgress by preference(key = Audiofy.SHOW_MINI_PROGRESS_BAR)
        if (showProgress) {
            val progress by progress
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .constrainAs(ProgressBar) {
                        bottom.linkTo(parent.bottom)
                    },
                color = Material.colors.primary,
                progress = progress
            )
        }
    }
}

@Composable
private inline fun NeuButton(
    painter: Painter,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconScale: Float = 1.5f,
    shape: RoundedCornerShape = CircleShape
) {
    NeumorphicButton(
        onClick = onClick,
        shape = shape,
        elevation = NeumorphicButtonDefaults.elevation(defaultElevation = 12.dp),
        border = if (Material.colors.isLight) null else BorderStroke(
            1.dp,
            Material.colors.outline.copy(0.06f)
        ),
        modifier = modifier,
        colors = NeumorphicButtonDefaults.neumorphicButtonColors(
            lightShadowColor = Material.colors.lightShadowColor,
            darkShadowColor = Material.colors.darkShadowColor
        ),

        content = {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.scale(iconScale)
            )
        }
    )
}

private val ArtworkValleyWidth = 20.dp
private val ArtworkBorderWidth = 8.dp
private val ArtworkShape = CircleShape

context(ConsoleViewModel) @Composable
private inline fun Artwork(
    modifier: Modifier = Modifier
) {
    val artwork by artwork
    val color = Material.colors.background

    Image(
        bitmap = artwork,
        contentScale = ContentScale.Crop,
        durationMillis = Anim.LongDurationMills,

        modifier = Modifier
            .shadow(
                shape = ArtworkShape,
                elevation = -12.dp,
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor,
                spotLight = SpotLight.BOTTOM_RIGHT,
            )
            .padding(ArtworkValleyWidth)
            .shadow(
                shape = ArtworkShape,
                elevation = 12.dp,
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor,
                spotLight = SpotLight.TOP_LEFT,
            )
            .border(BorderStroke(ArtworkBorderWidth, color), ArtworkShape)
            .gradient(colors = listOf(Color.Transparent, Color.Black.copy(0.5f)), vertical = false)
            .background(color)
            .then(modifier)
    )
}


@Composable
private inline fun MenuItem(
    vector: Painter,
    label: String,
    noinline onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        content = {
            Icon(painter = vector, contentDescription = null)
            Label(
                text = label,
                modifier = Modifier.padding(start = ContentPadding.medium)
            )
        }
    )
}


@Composable
private fun ConsoleViewModel.More(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    var showSleepMenu by rememberState(initial = false)
    SleepTimer(expanded = showSleepMenu) {
        showSleepMenu = false
    }
    val activity = LocalContext.activity

    var showPlaylistViewer by rememberState(initial = false)
    val playlists by playlists.collectAsState(initial = emptyList())
    val context = LocalContext.current
    Playlists(
        value = playlists,
        expanded = showPlaylistViewer,
        onPlaylistClick = {
            it?.let {
                addToPlaylist(it)
                Toast.makeText(context, "Adding tracks to Playlist ${it.name}.", Toast.LENGTH_SHORT)
                    .show()
            }
            showPlaylistViewer = false
        }
    )

    val current by current
    var showPropertiesDialog by rememberState(initial = false)
    current?.Properties(
        showPropertiesDialog,
        onDismissRequest = {
            showPropertiesDialog = false
        }
    )

    var showPlayingQueue by rememberState(initial = false)
    PlayingQueue(
        expanded = showPlayingQueue,
        onDismissRequest = {
            showPlayingQueue = false
        }
    )

    val navigator = LocalNavController.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        MenuItem(
            vector = rememberVectorPainter(image = Icons.Outlined.PlaylistAdd),
            label = "Add to playlist",
            onClick = {
                showPlaylistViewer = true; activity.launchReviewFlow(); onDismissRequest()
            }
        )

        MenuItem(
            vector = rememberVectorPainter(image = Icons.Outlined.Person),
            label = "Go to Artist",
            enabled = current?.artist?.isNotBlank() ?: false,
            onClick = {
                val encoded = Uri.encode(current?.artist ?: return@MenuItem)
                val direction = TracksRoute(Type.ARTISTS.name, encoded)
                navigator.navigateTo(direction)
                onDismissRequest()
            },
        )

        MenuItem(
            vector = rememberVectorPainter(image = Icons.Outlined.Info),
            label = "Info",
            onClick = { showPropertiesDialog = true; onDismissRequest() }
        )

        MenuItem(
            vector = rememberVectorPainter(image = Icons.Outlined.ModeNight),
            label = "Sleep timer",
            onClick = { showSleepMenu = true; onDismissRequest(); }
        )

        val context = LocalContext.current
        MenuItem(
            vector = rememberVectorPainter(image = Icons.Outlined.Share),
            label = "Share",
            onClick = { current?.let { context.share(it) }; onDismissRequest() }
        )

        Divider()

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),

            content = {

                val shuffle by shuffle
                IconButton(
                    onClick = { toggleShuffle(); activity.launchReviewFlow(); onDismissRequest() },
                    painter = painterResource(id = R.drawable.ic_shuffle),
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(if (shuffle) ContentAlpha.high else ContentAlpha.disabled)
                )

                val mode by repeatMode
                IconButton(
                    onClick = { cycleRepeatMode();activity.launchReviewFlow(); onDismissRequest(); },
                    painter = painterResource(id = if (mode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat),
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(
                        if (mode == Player.REPEAT_MODE_OFF) ContentAlpha.disabled
                        else ContentAlpha.high
                    )
                )

                IconButton(
                    onClick = {
                        showPlayingQueue = true; activity.launchReviewFlow(); onDismissRequest()
                    },
                    imageVector = Icons.Outlined.PlaylistPlay,
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun ConsoleViewModel.SleepTimer(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    val activity = LocalContext.activity
    DropdownMenu(
        title = "Sleep Timer",
        preserveIconSpace = true,
        expanded = expanded,
        items = listOf(
            null to "5 Minutes",
            null to "30 Minutes",
            null to "1 Hour",
            null to "3 Hours",
            Icons.Outlined.Close to "Clear"
        ),
        onDismissRequest = onDismissRequest,
    ) { index ->
        val minutes =
            when (index) {
                0 -> 5
                1 -> 30
                2 -> 60
                3 -> 180
                4 -> -1
                else -> error("No such value !!")
            }
        setSleepAfter(minutes)
        activity.launchReviewFlow()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Next(
    value: Audio?,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = value,
        modifier = modifier.padding(end = ContentPadding.large),
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() with
                    slideOutVertically { height -> -height } + fadeOut()
        },
        content = { new ->
            if (new != null)
                ListTile(
                    centreVertically = true,
                    text = {
                        Label(
                            text = new.name,
                            style = Material.typography.body2
                        )
                    },
                    secondaryText = {
                        Label(
                            text = new.album.uppercase(),
                            style = Material.typography.overline
                        )
                    },
                    leading = {
                        Surface(
                            elevation = ContentElevation.high,
                            border = BorderStroke(2.dp, Color.White),
                            shape = CircleShape,
                            content = {
                                Image(
                                    albumId = new.albumId,
                                    modifier = Modifier.requiredSize(56.dp),
                                    fadeMills = 0
                                )
                            }
                        )
                    }
                )
        }
    )
}

private val SignatureTextSize = 70.sp

@OptIn(ExperimentalAnimationApi::class, ExperimentalAnimationGraphicsApi::class)
@Composable
private fun ConsoleViewModel.Layout(
    toggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier = modifier
    ) {
        val (Signature, PlaylistLabel, ArtistLabel, Artwork, Slider, Album, Title, Play, UpNextLabel, UpNext) = createRefs()

        val primary = Material.colors.primary
        val activity = LocalContext.activity
        // Signature
        Text(
            text = stringResource(id = R.string.app_name),
            fontFamily = FontFamily.Cursive,
            color = primary,
            fontWeight = FontWeight.Bold,
            fontSize = SignatureTextSize,
            modifier = Modifier.constrainAs(Signature) {
                start.linkTo(parent.start, ContentPadding.normal)
                top.linkTo(parent.top)
            }
        )

        // Close Button
        NeuButton(
            painter = rememberVectorPainter(image = Icons.Default.Close),
            onClick = toggle,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .size(46.dp)
                .constrainAs(createRef()) {
                    end.linkTo(parent.end, ContentPadding.normal)
                    top.linkTo(Signature.top)
                    bottom.linkTo(Signature.bottom)
                }
        )

        // playlist line

        val playing by playing
        val composable by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(
                R.raw.playback_indicator
            )
        )

        val (PlayBars, Playlist) = createRefs()
        LottieAnimation(
            composition = composable,
            iterations = if (playing) Int.MAX_VALUE else 1,
            modifier = Modifier
                .size(24.dp)
                .constrainAs(PlayBars) {
                    start.linkTo(Signature.start, ContentPadding.medium)
                    top.linkTo(PlaylistLabel.top)
                    bottom.linkTo(Playlist.bottom)
                }
        )

        Label(
            text = "Playing From",
            style = Material.typography.caption2,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .constrainAs(PlaylistLabel) {
                    top.linkTo(Signature.bottom, ContentPadding.medium)
                    start.linkTo(PlayBars.end, ContentPadding.medium)
                },
            color = Material.colors.onSurface
        )

        val playlistName by playlistName
        Label(
            text = playlistName,
            color = LocalContentColor.current.copy(ContentAlpha.medium),
            style = Material.typography.caption,
            modifier = Modifier.constrainAs(Playlist) {
                start.linkTo(PlaylistLabel.start)
                top.linkTo(PlaylistLabel.bottom)
                width = Dimension.percent(0.4f)
            },
        )

        //artist
        val (ArtistIcon, Artist) = createRefs()
        Label(
            text = "Artist",
            style = Material.typography.caption2,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.constrainAs(ArtistLabel) {
                top.linkTo(Playlist.bottom, ContentPadding.normal)
                start.linkTo(ArtistIcon.end, ContentPadding.medium)
            },
            color = Material.colors.onSurface
        )

        val current by current
        AnimatedLabel(
            text = current?.artist ?: "",
            color = LocalContentColor.current.copy(ContentAlpha.medium),
            style = Material.typography.caption,
            modifier = Modifier.constrainAs(Artist) {
                start.linkTo(ArtistLabel.start)
                top.linkTo(ArtistLabel.bottom)
                width = Dimension.percent(0.4f)
            },
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_artist),
            contentDescription = null,
            modifier = Modifier.constrainAs(ArtistIcon) {
                start.linkTo(PlayBars.start)
                top.linkTo(ArtistLabel.top)
                bottom.linkTo(Artist.bottom)
            }
        )

        // sleep after
        val sleepAfterMills by sleepAfter
        val (SleepAfterLabel, SleepAfter) = createRefs()
        sleepAfterMills?.let {
            Label(
                text = "Sleep After",
                style = Material.typography.caption2,
                fontWeight = FontWeight.SemiBold,
                color = Material.colors.onSurface,

                modifier = Modifier
                    .constrainAs(SleepAfterLabel) {
                        top.linkTo(PlaylistLabel.top)
                        bottom.linkTo(SleepAfter.top)
                        start.linkTo(SleepAfter.start)
                    },
            )


            Ticker(
                text = Util.formatAsDuration(it),
                color = Material.colors.onSurface,
                font = Typeface.DEFAULT_BOLD,
                size = 32.sp,

                modifier = Modifier
                    .offset(x = -2.dp, y = -10.dp)
                    .constrainAs(SleepAfter) {
                        top.linkTo(SleepAfterLabel.bottom)
                        bottom.linkTo(Artist.bottom)
                        end.linkTo(parent.end, ContentPadding.normal)
                    },
            )
        }

        Artwork(
            modifier = Modifier
                .constrainAs(Artwork) {
                    top.linkTo(Artist.bottom, ContentPadding.normal)
                    bottom.linkTo(Slider.top, ContentPadding.medium)
                    start.linkTo(parent.start, ContentPadding.normal)
                    end.linkTo(parent.end, ContentPadding.normal)
                    height = Dimension.fillToConstraints
                    width = Dimension.ratio("1:1")
                }
        )

        //progress
        val (Progress, MaxTime) = createRefs()
        val position by progress
        val tickerPaddingEnd = ArtworkValleyWidth + ArtworkBorderWidth + 3.dp
        val onArtworkColor = Color.White
        Ticker(
            text = Util.formatAsDuration(position.toInt()),
            size = 48.sp,
            font = Typeface.DEFAULT_BOLD,
            color = onArtworkColor,

            modifier = Modifier.constrainAs(Progress) {
                end.linkTo(Artwork.end, tickerPaddingEnd)
                top.linkTo(Artwork.top)
                bottom.linkTo(Artwork.bottom)
            },
        )

        // track length
        AnimatedLabel(
            text = Util.formatAsDuration(current?.duration?.toLong() ?: 0),
            fontWeight = FontWeight.Bold,
            style = Material.typography.caption.copy(
                fontFamily = FontFamily.Default,
            ),
            color = onArtworkColor,

            modifier = Modifier
                .offset(y = 16.dp)
                .constrainAs(MaxTime) {
                    start.linkTo(Progress.start)
                    bottom.linkTo(Progress.top)
                },
        )

        // slider
        val (Heart, More) = createRefs()
        val favourite by favourite
        IconButton(
            onClick = { toggleFav(); activity.launchReviewFlow() },
            painter = painterResource(id = if (favourite) R.drawable.ic_heart_filled else R.drawable.ic_heart),
            contentDescription = null,
            modifier = Modifier.constrainAs(Heart) {
                top.linkTo(Slider.top)
                bottom.linkTo(Slider.bottom)
                start.linkTo(Title.start)
            }
        )

        val value by progress
        val duration = current?.duration?.toFloat() ?: 0f
        Slider(
            value = value, onValueChange = { seekTo(it) },
            //steps = (duration / 45_000).toInt(),
            modifier = Modifier.constrainAs(Slider) {
                bottom.linkTo(Album.top, ContentPadding.small)
                start.linkTo(Heart.end, ContentPadding.medium)
                end.linkTo(More.start, ContentPadding.medium)
                width = Dimension.fillToConstraints
            }
        )

        var expanded by rememberState(initial = false)
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.constrainAs(More) {
                top.linkTo(Slider.top)
                bottom.linkTo(Slider.bottom)
                end.linkTo(Title.end)
            },
            content = {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
                More(expanded = expanded) {
                    expanded = false
                }
            }
        )

        // Title group
        AnimatedLabel(
            text = current?.album ?: stringResource(id = R.string.unknown),
            style = Material.typography.caption2,
            modifier = Modifier
                .offset(y = 4.dp, x = 5.dp)
                .constrainAs(Album) {
                    start.linkTo(Title.start)
                    bottom.linkTo(Title.top)
                }
        )

        MarqueText(
            text = current?.name ?: stringResource(id = R.string.unknown),
            textSize = 40.sp,
            typeface = Typeface.DEFAULT_BOLD,

            modifier = Modifier.constrainAs(Title) {
                bottom.linkTo(Play.top, ContentPadding.medium)
                start.linkTo(parent.start, ContentPadding.large)
                end.linkTo(parent.end, ContentPadding.large)
                width = Dimension.fillToConstraints
            },
        )

        // play/toggle buttons
        val (SkipToNext, SkipToPrev) = createRefs()
        createHorizontalChain(SkipToPrev, Play, SkipToNext, chainStyle = ChainStyle.Packed)
        NeuButton(
            onClick = { togglePlay(); activity.launchReviewFlow() },

            painter = rememberAnimatedVectorResource(
                id = R.drawable.avd_pause_to_play,
                atEnd = !playing
            ),

            modifier = Modifier
                .padding(horizontal = ContentPadding.large)
                .size(70.dp)
                .constrainAs(Play) {
                    bottom.linkTo(UpNextLabel.top, ContentPadding.medium)
                }
        )

        NeuButton(
            onClick = { skipToPrev(); activity.launchReviewFlow() },
            shape = RoundedCornerShape(10.dp),
            painter = painterResource(id = R.drawable.ic_skip_to_prev),
            iconScale = 0.8f,
            modifier = Modifier.constrainAs(SkipToPrev) {
                top.linkTo(Play.top)
                bottom.linkTo(Play.bottom)
            },
        )

        NeuButton(
            onClick = { skipToNext(); activity.launchReviewFlow() },
            shape = RoundedCornerShape(10.dp),
            painter = painterResource(id = R.drawable.ic_skip_to_next),
            iconScale = 0.8f,
            modifier = Modifier.constrainAs(SkipToNext) {
                top.linkTo(Play.top)
                bottom.linkTo(Play.bottom)
            },
        )

        //upNext
        Header(
            text = "Up Next",
            modifier = Modifier.constrainAs(UpNextLabel) {
                start.linkTo(parent.start, ContentPadding.large)
                bottom.linkTo(UpNext.top)
                width = Dimension.percent(0.5f)
            },
            fontWeight = FontWeight.Bold
        )

        val next by next
        Next(
            value = next,
            modifier = Modifier.constrainAs(UpNext) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start, ContentPadding.normal)
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Console(
    viewModel: ConsoleViewModel,
    expanded: Boolean,
    toggle: () -> Unit
) {
    with(viewModel) {
        when (expanded) {
            false -> {
                AnimatedVisibility(
                    visible = true,
                    initiallyVisible = false,
                    modifier = Modifier.fillMaxSize(),
                    enter = scaleIn(),

                    content = {
                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize(),
                            content = {
                                Surface(
                                    modifier = Modifier
                                        .clickable(
                                            onClick = toggle,
                                            indication = null,
                                            interactionSource = remember(::MutableInteractionSource)
                                        )
                                        .fillMaxWidth(0.78f)
                                        .requiredHeight(Audiofy.MINI_PLAYER_HEIGHT - 10.dp),
                                    shape = CircleShape,
                                    elevation = ContentElevation.high,
                                    content = { MiniLayout() }
                                )
                            }
                        )

                    }
                )
            }
            else -> {

                AnimatedVisibility(
                    visible = true,
                    initiallyVisible = false,
                    modifier = Modifier.fillMaxSize(),

                    enter = fadeIn(tween(Anim.LongDurationMills)) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(220, delayMillis = 90)
                    ),
                    content = {

                        val controller = rememberSystemUiController()
                        val greyIcons = controller.statusBarDarkContentEnabled
                        val isLight = Material.colors.isLight
                        val channel = LocalContext.toastHostState
                        DisposableEffect(key1 = greyIcons && isLight) {
                            // set icon color for current screen
                            controller.setStatusBarColor(Color.Transparent, isLight)
                            viewModel.messenger = channel

                            onDispose {
                                // restore back the color of the old screen.
                                controller.setStatusBarColor(Color.Transparent, greyIcons)
                                viewModel.messenger = null
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxSize(),
                            color = Material.colors.background,
                            content = {
                                Layout(
                                    toggle = toggle,
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .fillMaxSize()
                                )
                            },
                        )
                    }

                )
            }
        }
    }
}