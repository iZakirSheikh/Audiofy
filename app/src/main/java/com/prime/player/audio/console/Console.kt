package com.prime.player.audio.console

import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import com.airbnb.lottie.LottieDrawable
import com.google.accompanist.insets.statusBarsPadding
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.HomeViewModel
import com.prime.player.audio.Track
import com.prime.player.extended.*
import com.prime.player.extended.managers.Banner
import com.prime.player.utils.toDuration

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Console(viewModel: HomeViewModel, toggle: () -> Unit) {
    with(viewModel) {
        val expanded by expanded
        when (expanded) {
            true -> AnimateVisibility(
                visible = true,
                initiallyVisible = false,
                enter = fadeIn(
                    animationSpec = tween(Anim.DURATION_LONG)
                ) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(220, delayMillis = 90)
                ),
            ) {
                Layout(toggle)
            }
            else -> AnimateVisibility(
                visible = true,
                initiallyVisible = false,
                enter = slideInVertically(initialOffsetY = { it })
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MiniPlayer(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                toggle()
                            }
                            .align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun HomeViewModel.Layout(toggle: () -> Unit) {
    Frame(
        contentColor = PlayerTheme.colors.onBackground,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        val dominant by animateColorAsState(
            targetValue = dominant.value.takeOrElse { PlayerTheme.colors.primary },
            animationSpec = tween(Anim.DURATION_MEDIUM)
        )
        //background
        Spacer(
            modifier = Modifier
                .verticalGradient(
                    listOf(
                        dominant,
                        PlayerTheme.colors.background
                    )
                )
                .fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                TitleBar(toggle)
            }

            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Artwork(
                    modifier = Modifier
                        .offset(y = Padding.MEDIUM)
                        .padding(horizontal = Padding.LARGE)
                        .fillMaxWidth()
                        .weight(1f)
                        .animate()
                )
            }

            val current by current
            val value = progress.value.toFloat() / (current?.duration ?: 1)


            Slider(
                value = value, onValueChange = { seekTo(it) },
                modifier = Modifier
                    //.offset(y = -Padding.LARGE)
                    .padding(horizontal = Padding.LARGE)
                    .fillMaxWidth(0.8f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White
                )
            )

            Label(
                text = current?.album?.title ?: stringResource(id = R.string.unknown),
                fontSize = 11.sp,
                modifier = Modifier
                    .offset(y = -Padding.MEDIUM)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(0.8f)
            )

            //title
            MarqueText(
                text = current?.title ?: stringResource(id = R.string.unknown),
                modifier = Modifier
                    .offset(y = (-12).dp)
                    .padding(horizontal = Padding.LARGE)
                    .fillMaxWidth(0.8f),
                textSize = 40.sp,
                typeface = Typeface.DEFAULT_BOLD,
            )

            Banner(
                modifier = Modifier
                    .padding(vertical = Padding.SMALL, horizontal = Padding.LARGE)
                    .animate(),
                placementID = stringResource(id = R.string.console_banner_id)
            )

            Controls(toggle = toggle)

            Label(
                text = "UP Next",
                style = PlayerTheme.typography.h6,
                modifier = Modifier
                    .padding(horizontal = Padding.LARGE)
                    .align(Alignment.Start),
                fontWeight = FontWeight.SemiBold,
            )
            val next by next

            Crossfade(targetState = next) { audio ->
                if (audio != null)
                    Track(
                        title = audio.title,
                        subtitle = audio.artist?.name ?: stringResource(id = R.string.unknown),
                        albumID = audio.album?.id ?: -1,
                        modifier = Modifier
                            .clickable { }
                            .padding(horizontal = Padding.MEDIUM)
                    )
            }
        }
    }
}

@Composable
fun HomeViewModel.TitleBar(toggle: () -> Unit) {
    ConstraintLayout(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = Padding.LARGE)
            .fillMaxWidth()
    ) {
        val (
            titleRef, label1, label2, label3, label4, label5, label6, icon1, icon2, closeRef
        ) = createRefs()

        createHorizontalChain(titleRef, closeRef, chainStyle = ChainStyle.SpreadInside)

        RainbowText(
            text = stringResource(id = R.string.app_name),
            modifier = Modifier.constrainAs(titleRef) {},
            textSize = 20.sp,
            typeface = Typeface.create("cursive", Typeface.BOLD),
            colors = listOf(
                Color.White,
                Color.White,
                Color.White.copy(0.6f),
                Color.White.copy(0.7f),
                Color.White.copy(0.6f),
                Color.White,
                Color.White,
            )
        )

        // close button
        IconButton(onClick = toggle, modifier = Modifier.constrainAs(closeRef) {}) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "collapse"
            )
        }

        createVerticalChain(
            titleRef,
            label1,
            label2,
            label3,
            label4,
            chainStyle = ChainStyle.Packed
        )

        val isPlaying by playing
        Crossfade(
            targetState = isPlaying,
            modifier = Modifier
                .constrainAs(icon1) {
                    start.linkTo(parent.start)
                    top.linkTo(label1.top)
                    bottom.linkTo(label2.bottom)
                }
                .animate()
        ) {
            if (it)
                Lottie(
                    res = R.raw.playback_indicator,
                    modifier = Modifier.requiredSize(24.dp),
                    repeatX = LottieDrawable.INFINITE,
                )
        }

        Label(
            text = "Playing From",
            fontSize = 9.sp,
            modifier = Modifier.constrainAs(label1) {
                start.linkTo(icon1.end, Padding.LARGE)
            }
        )

        val playlistName by playlistName
        Label2(
            text = playlistName,
            modifier = Modifier.fillMaxWidth(0.5f).constrainAs(label2) {
                start.linkTo(label1.start)
            },
            color = LocalContentColor.current.copy(ContentAlpha.medium)
        )


        Icon(
            painter = painterResource(id = R.drawable.ic_artist),
            contentDescription = null,
            modifier = Modifier.constrainAs(icon2) {
                start.linkTo(parent.start)
                top.linkTo(label3.top)
                bottom.linkTo(label4.bottom)
            }
        )

        Label(
            text = "Artist",
            fontSize = 9.sp,
            modifier = Modifier.padding(top = Padding.MEDIUM).constrainAs(label3) {
                start.linkTo(icon2.end, Padding.LARGE)
            }
        )

        val current by current
        Label2(
            text = current?.artist?.name ?: stringResource(id = R.string.unknown),
            modifier = Modifier.fillMaxWidth(0.7f).constrainAs(label4) {
                start.linkTo(label3.start)
            },
            color = LocalContentColor.current.copy(ContentAlpha.medium)
        )

        val sleepTimer by sleepAfter
        if (sleepTimer != -1L) {
            Label(
                text = "Sleep After",
                modifier = Modifier.constrainAs(label5) {
                    bottom.linkTo(
                        label6.top,
                    )
                    start.linkTo(label6.start)
                },
                fontSize = 9.sp,
            )

            Ticker(
                text = toDuration(sleepTimer),
                size = 28.sp,
                font = Typeface.DEFAULT_BOLD,
                modifier = Modifier.constrainAs(label6) {
                    top.linkTo(
                        titleRef.bottom,
                    )
                    end.linkTo(parent.end)
                },
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        }
    }
}