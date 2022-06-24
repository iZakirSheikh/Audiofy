package com.prime.player.audio.console

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.*
import com.prime.player.extended.*
import com.prime.player.extended.managers.LocalAdvertiser
import com.prime.player.utils.share

private const val TAG = "Controls"

@ExperimentalMaterialApi
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun HomeViewModel.Controls(modifier: Modifier = Modifier, toggle: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val favourite by favourite
        val messenger = LocalMessenger.current
        val advertiser = LocalAdvertiser.current

        //favourite
        IconButton(onClick = {
            toggleFav(messenger)
            advertiser.show(false)
        }) {
            Icon(
                painter = painterResource(
                    id = if (favourite) R.drawable.ic_heart_filled else R.drawable.ic_heart
                ),
                contentDescription = null,
                tint = PlayerTheme.colors.primary
            )
        }

        //skipToNext
        IconButton(onClick = { skipToPrev(); advertiser.show(false) }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_to_prev),
                contentDescription = ""
            )
        }

        val shape = CircleShape
        val res = AnimatedImageVector.animatedVectorResource(id = R.drawable.avd_pause_to_play)
        val playing by playing

        Frame(
            color = Color.Transparent,
            shape = shape,
            onClick = { togglePlay(); advertiser.show(false) },
            border = BorderStroke(2.dp, LocalContentColor.current),
            modifier = Modifier.requiredSize(64.dp)
        ) {
            Icon(
                painter = rememberAnimatedVectorPainter(res, !playing),
                contentDescription = null, // decorative element
                modifier = Modifier.requiredSize(40.dp)
            )
        }

        //skipToNext
        IconButton(onClick = { skipToNext(); advertiser.show(false) }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_to_next),
                contentDescription = ""
            )
        }

        val expanded = remember {
            mutableStateOf(false)
        }

        IconButton(onClick = { expanded.value = true }) {
            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "")
            More(expanded, toggle = toggle)
        }
    }
}

@Composable
private fun HomeViewModel.More(state: MutableState<Boolean>, toggle: () -> Unit) {
    var state by state
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val actions = LocalNavActionProvider.current as AudioNavigationActions
    val advertiser = LocalAdvertiser.current

    val showTrackInfo = memorize {
        current.value?.let {
            TrackInfo(it) {
                hide()
            }
        }
    }

    //custom timer menu
    val showCustomTimer = memorize {
        TextInputDialog(
            title = "Custom Timer",
            label = "Enter time in minutes.",
            defaultValue = "0",
            vectorIcon = Icons.Default.Timer3,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
        ) { text ->
            val minutes = text?.toIntOrNull() ?: 0
            if (minutes > 0)
                setSleepAfter(minutes, messenger)
            advertiser.show(false)
            hide()
        }
    }

    val showSleepTimerMenu = memorize {
        DropdownMenu(
            title = "Sleep Timer",
            preserveIconSpace = true,
            expanded = isVisible(),
            items = listOf(
                null to "5 Minutes",
                null to "15 Minutes",
                null to "30 Minutes",
                null to "1 Hour",
                Icons.Outlined.Watch to "Add Manually",
                Icons.Outlined.Close to "Clear"
            ),
            onDismissRequest = { hide() },
        ) { index ->
            when (index) {
                4 -> showCustomTimer.show()
                else -> {
                    val minutes = when (index) {
                        0 -> 5
                        1 -> 15
                        2 -> 30
                        3 -> 60
                        5 -> -1
                        else -> error("No such value !!")
                    }
                    setSleepAfter(minutes, messenger)
                    advertiser.show(false)
                }
            }
        }
    }

    val showPlaylistViewer = memorize {
        current.value?.let {
            AddToPlaylist(audios = listOf(it.id)) {
                hide()
            }
        }
    }


    DropdownMenu(
        expanded = state,
        items = listOf(
            Icons.Outlined.PlaylistAdd to "Add to playlist",
            Icons.Outlined.Person to "Go to Artist",
            Icons.Outlined.Info to "Info",
            Icons.Outlined.ModeNight to "Sleep timer",
            Icons.Outlined.Share to "Share"
        ),
        isEnabled = { index ->
            when (index) {
                1 -> current.value?.artist != null
                else -> true
            }
        },
        onDismissRequest = { state = false },
        onItemClick = { index ->
            when (index) {
                0 -> showPlaylistViewer.show()
                1 -> current.value?.let {
                    toggle()
                    it.artist?.let {
                        actions.toGroupViewer(GroupOf.ARTISTS, "${it.id}")
                    }
                }
                2 -> showTrackInfo.show()
                3 -> showSleepTimerMenu.show()
                4 -> current.value?.let {
                    context.share(it)
                    advertiser.show(false)
                }
            }
            state = false
        }
    )
}