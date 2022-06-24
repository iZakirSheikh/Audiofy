package com.prime.player.audio.console

import android.graphics.Typeface
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.AUDIO_BOTTOM_SHEET_PEEK_HEIGHT
import com.prime.player.audio.AlbumArt
import com.prime.player.audio.HomeViewModel
import com.prime.player.extended.*
import com.prime.player.extended.managers.LocalAdvertiser
import com.prime.player.preferences.Preferences
import com.prime.player.preferences.requiresProgressBarInMiniPlayer

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun HomeViewModel.MiniPlayer(modifier: Modifier = Modifier) {
    val shape =
        PlayerTheme.shapes.small.copy(CornerSize((AUDIO_BOTTOM_SHEET_PEEK_HEIGHT - 10.dp) / 2))
    val current by current
    val artwork by artwork

    val colorSurface = PlayerTheme.colors.surface
    val advertiser = LocalAdvertiser.current

    Frame(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .requiredHeight(AUDIO_BOTTOM_SHEET_PEEK_HEIGHT - 10.dp),
        shape = shape,
        elevation = Elevation.HIGH,
        color = colorSurface
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (artworkRef, titleRef, subtitleRef, heartRef, playRef, progressRef) = createRefs()
            createHorizontalChain(artworkRef, titleRef, heartRef, playRef)

            //artwork
            AlbumArt(
                contentDescription = "artwork",
                bitmap = artwork,
                modifier = Modifier
                    .horizontalGradient(
                        listOf(
                            Color.Transparent,
                            colorSurface,
                        )
                    )
                    .horizontalGradient()
                    .requiredWidth(75.dp)
                    .fillMaxHeight()
                    .constrainAs(artworkRef) {}
            )

            //INFO create vertical chain of title ans subtitle
            createVerticalChain(titleRef, subtitleRef, chainStyle = ChainStyle.Packed)

            //title
            MarqueText(
                text = current?.title ?: "",
                modifier = Modifier.constrainAs(titleRef) {
                    width = Dimension.fillToConstraints
                    start.linkTo(artworkRef.end, Padding.LARGE)
                    end.linkTo(heartRef.start, Padding.MEDIUM)
                    top.linkTo(parent.top)
                },
                typeface = Typeface.DEFAULT_BOLD,
                textSize = 14.sp
            )

            //subtitle
            Caption(
                text = current?.album?.title ?: stringResource(id = R.string.unknown),
                fontWeight = FontWeight.SemiBold,
                color = LocalContentColor.current.copy(0.8f),
                modifier = Modifier.constrainAs(subtitleRef) {
                    start.linkTo(titleRef.start)
                    end.linkTo(titleRef.end)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                }
            )

            val favourite by favourite
            val messenger = LocalMessenger.current

            //favourite
            IconButton(
                onClick = {
                    toggleFav(messenger)
                    advertiser.show(false)
                },
                modifier = Modifier.constrainAs(heartRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            ) {
                Icon(
                    painter = painterResource(
                        id = if (favourite) R.drawable.ic_heart_filled else R.drawable.ic_heart
                    ),
                    contentDescription = null,
                    tint = PlayerTheme.colors.primary
                )
            }

            //play/pause
            val playRes = AnimatedImageVector.animatedVectorResource(id = R.drawable.avd_pause_to_play)
            val playing by playing

            IconButton(
                onClick = { togglePlay(); advertiser.show(false) },
                modifier = Modifier.constrainAs(playRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
            ) {
                Icon(
                    painter = rememberAnimatedVectorPainter(
                        animatedImageVector = playRes,
                        atEnd = !playing
                    ),
                    contentDescription = null, // decorative element
                    modifier = Modifier.requiredSize(34.dp)
                )
            }

            val showProgress by
            with(Preferences.get(LocalContext.current)) { requiresProgressBarInMiniPlayer().collectAsState() }
            if (showProgress) {
                val progress by progress
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .constrainAs(progressRef) {
                            bottom.linkTo(parent.bottom)
                        },
                    color = PlayerTheme.colors.secondary,
                    progress = progress.toFloat() / (current?.duration ?: 1)
                )
            }
        }
    }
}