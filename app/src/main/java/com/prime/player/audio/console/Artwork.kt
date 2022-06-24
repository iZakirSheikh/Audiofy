package com.prime.player.audio.console

import android.graphics.Typeface
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.HomeViewModel
import com.prime.player.audio.Track
import com.prime.player.core.playback.PlaybackService
import com.prime.player.extended.*
import com.prime.player.extended.managers.LocalAdvertiser
import com.prime.player.extended.visualizers.CircleLineVisualizer
import com.prime.player.preferences.Preferences
import com.prime.player.preferences.showVisualizer
import com.prime.player.utils.toDuration

@Composable
fun HomeViewModel.Artwork(modifier: Modifier = Modifier) {
    ConstraintLayout(
        modifier = modifier.fillMaxWidth()
    ) {
        val (visualizerRef, artRef, shuffleRef, repeatRef, queueRef, timer1Ref, timer2Ref) = createRefs()
        val current by current
        val currArt by artwork

        val circular = CircleShape
        val showVisualizer by with(Preferences.get(LocalContext.current)) { showVisualizer().collectAsState() }

        //Audio visualizer
        if (showVisualizer) {
            val audioSessionId by audioSessionID
            if (audioSessionId != -1)
                CircleLineVisualizer(
                    density = 0.65f,
                    audioSessionID = audioSessionId,
                    modifier = Modifier
                        .scale(1.2f)
                        .constrainAs(visualizerRef) {
                            linkTo(
                                start = parent.start,
                                top = parent.top,
                                bottom = parent.bottom,
                                end = parent.end
                            )
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        },
                    color = Color.White
                )
        }

        Frame(
            shape = circular,
            modifier = Modifier
                .fillMaxWidth(if (showVisualizer) 0.63f else 0.72f)
                .aspectRatio(1.0f)
                .constrainAs(artRef) {
                    linkTo(
                        parent.start,
                        parent.top,
                        parent.end,
                        parent.bottom
                    )
                },
            elevation = Elevation.EXTRA_HIGH,
            border = BorderStroke(7.dp, Color.White)
        ) {
            KenBurns(modifier = Modifier.verticalGradient()) {
                setImageBitmap(currArt)
            }
        }


        // track length
        Label(
            text = toDuration(current?.duration ?: 0),
            style = PlayerTheme.typography.h6.copy(
                fontFamily = FontFamily.SansSerif
            ),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .constrainAs(timer1Ref) {
                    start.linkTo(timer2Ref.start, Padding.MEDIUM)
                    bottom.linkTo(timer2Ref.top)
                }
                .offset(
                    y = Padding.MEDIUM
                ),
        )


        //progress
        val position by progress
        Ticker(
            text = toDuration(position),
            size = 48.sp,
            modifier = Modifier.constrainAs(timer2Ref) {
                end.linkTo(artRef.end, 16.dp)
                top.linkTo(artRef.top)
                bottom.linkTo(artRef.bottom)
            },
            font = Typeface.DEFAULT_BOLD,
        )

        val advertiser = LocalAdvertiser.current
        val messenger = LocalMessenger.current
        val shuffle by shuffle

        //shuffle button
        IconButton(
            onClick = {
                toggleShuffle(messenger)
                advertiser.show(false)
            },
            modifier = Modifier
                .offset(y = (-25).dp)
                .constrainAs(shuffleRef) {
                    start.linkTo(artRef.start, Padding.EXTRA_LARGE)
                    bottom.linkTo(artRef.bottom)
                }
        ) {
            val tint by animateColorAsState(
                targetValue = if (shuffle) Color.White else Color.White.copy(
                    ContentAlpha.disabled
                )
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_shuffle),
                contentDescription = "",
                tint = tint
            )
        }

        val repeat by repeatMode
        // repeatRef
        IconButton(
            onClick = {
                cycleRepeatMode(messenger)
                advertiser.show(false)
            },
            modifier = Modifier
                .constrainAs(repeatRef) {
                    start.linkTo(artRef.start)
                    end.linkTo(artRef.end)
                    bottom.linkTo(artRef.bottom, Padding.SMALL)
                }
        ) {
            val tint by animateColorAsState(
                targetValue = if (repeat == PlaybackService.REPEAT_MODE_NONE) Color.White.copy(
                    ContentAlpha.disabled
                ) else Color.White
            )
            Icon(
                painter = painterResource(
                    when (repeat) {
                        PlaybackService.REPEAT_MODE_NONE, PlaybackService.REPEAT_MODE_ALL -> R.drawable.ic_repeat
                        else -> R.drawable.ic_repeat_one
                    }
                ),
                contentDescription = "",
                tint = tint
            )
        }


        val playingQueue = memorize {
            PlayingQueue {
                hide()
                advertiser.show(false)
            }
        }

        //queueRef
        IconButton(
            onClick = { playingQueue.show() },
            modifier = Modifier
                .offset(y = (-25).dp)
                .constrainAs(queueRef) {
                    end.linkTo(artRef.end, Padding.EXTRA_LARGE)
                    bottom.linkTo(artRef.bottom)
                }
        ) {
            Icon(
                imageVector = Icons.Outlined.PlaylistPlay,
                contentDescription = "",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HomeViewModel.PlayingQueue(onDismissRequest: () -> Unit) {
    val list = getPlayingQueue()
    val current by current

    val state = rememberLazyListState()
    val playing by playing

    PrimeDialog(
        title = "Playing Queue",
        onDismissRequest = onDismissRequest,
        vectorIcon = Icons.Outlined.PlaylistPlay,
        button2 = stringResource(id = R.string.dismiss) to onDismissRequest,
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .heightIn(max = with(LocalDensity.current) { displayHeight * 0.7f }),
            contentPadding = PaddingValues(vertical = Padding.MEDIUM)
        ) {
            items(list) { audio ->
                Crossfade(
                    targetState = audio == current,
                    modifier = Modifier
                        .wrapContentSize()
                        .animateContentSize(animationSpec = tween(Anim.DURATION_LONG))
                ) { show ->
                    if (show)
                        Column {
                            val color = PlayerTheme.colors.primary
                            Label(
                                text = "Now Playing",
                                modifier = Modifier.padding(
                                    top = Padding.LARGE,
                                    start = Padding.LARGE,
                                    bottom = Padding.MEDIUM
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                            Divider(
                                modifier = Modifier.padding(horizontal = Padding.LARGE),
                                color = color.copy(0.12f)
                            )
                        }
                }

                val advertiser = LocalAdvertiser.current
                Track(
                    title = audio.title,
                    subtitle = audio.artist?.name ?: stringResource(id = R.string.unknown),
                    albumID = audio.album?.id ?: -1,
                    overline = audio.album?.title ?: stringResource(id = R.string.unknown),
                    playing = current == audio && playing,
                    modifier = Modifier.clickable(enabled = audio != current) {
                        playTrackAt(list.indexOf(audio))
                        advertiser.show(false)
                    }
                )
            }
        }
    }


    LaunchedEffect(key1 = Unit) {
        current?.let {
            state.scrollToItem(list.indexOf(it))
        }
    }
}