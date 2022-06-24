package com.prime.player.audio.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.prime.player.PlayerTheme
import com.prime.player.audio.AlbumArt
import com.prime.player.audio.AudioNavigationActions
import com.prime.player.audio.GroupOf
import com.prime.player.audio.resolveAccentColor
import com.prime.player.extended.*
import com.prime.player.extended.managers.LocalAdvertiser
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.absoluteValue


private const val TAG = "CollectionRow"

@OptIn(ExperimentalPagerApi::class)
@Composable
fun LibraryViewModel.CollectionRow(modifier: Modifier = Modifier, state: PagerState) {
    val actions = LocalNavActionProvider.current as AudioNavigationActions
    val advertiser = LocalAdvertiser.current

    HorizontalPager(state = state, modifier = modifier.fillMaxWidth(), count = 6) { number ->
        val page = when (number) {
            0 -> artists
            1 -> albums
            2 -> audios
            3 -> genres
            4 -> playlists
            else -> folders
        }

        val itemModifier = Modifier
            .let {
                if (currentPage == number)
                    it.clickable {
                        navigateTo(number, actions)
                        advertiser.show(false)
                    }
                else
                    it
            }
            .graphicsLayer {
                // Calculate the absolute offset for the current page from the
                // scroll position. We use the absolute value which allows us to mirror
                // any effects for both directions
                val pageOffset = calculateCurrentOffsetForPage(number).absoluteValue

                // scaleX = 46f
                // We animate the scaleX + scaleY, between 85% and 100%
                lerp(
                    startValue = 0.75f,
                    endValue = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                ).also { scale ->
                    scaleX = scale
                    scaleY = scale
                }
            }
            .fillMaxHeight(0.9f)
            .aspectRatio(0.75f)

        CollectionItem(
            state = page,
            modifier = itemModifier
        )
    }
}

@Composable
private fun CollectionItem(
    modifier: Modifier = Modifier,
    state: StateFlow<Page>,
) {
    val page by state.collectAsState()

    Frame(
        modifier = modifier,
        shape = PlayerTheme.shapes.small,
        elevation = Elevation.HIGH
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AlbumArt(
                contentDescription = null,
                albumId = page.forth,
                modifier = Modifier
                    .weight(1f)
                    .verticalGradient()
            )

            ListItem(
                modifier = Modifier
                    .padding(horizontal = Padding.MEDIUM, vertical = Padding.SMALL)
                    .fillMaxWidth(),
                icon = {
                    page.third?.let {
                        Icon(imageVector = it, contentDescription = null)
                    }
                },
                secondaryText = {
                    Caption(text = page.second)
                },
                text = { Label(text = page.first, fontWeight = FontWeight.SemiBold) }
            )
        }
    }

}


private fun navigateTo(number: Int, actions: AudioNavigationActions) {
    when (number) {
        2 -> actions.toGroupViewer(GroupOf.AUDIOS)
        0 -> actions.toGroups(GroupOf.ARTISTS)
        1 -> actions.toGroups(GroupOf.ALBUMS)
        3 -> actions.toGroups(GroupOf.GENRES)
        4 -> actions.toGroups(GroupOf.PLAYLISTS)
        else -> actions.toGroups(GroupOf.FOLDERS)
    }

}