package com.prime.player.audio.library


import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.AlbumArt
import com.prime.player.core.models.Audio
import com.prime.player.extended.*
import com.prime.player.preferences.Preferences
import com.prime.player.preferences.requiresAccentThoroughly

private const val TAG = "RecentRow"


@Composable
fun LibraryViewModel.RecentRow(
    modifier: Modifier = Modifier
) {
    val state by recent.state.collectAsState()
    Crossfade(
        targetState = state,
        animationSpec = tween(1000),
        modifier = modifier
    ) { value ->
        when (value) {
            Resource.State.Loading -> PlaceHolder(
                lottieResource = R.raw.empty,
                message = "Loading...",
                size = 99.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Resource.State.Success -> recent.LazyList()
            Resource.State.Error -> Log.i(TAG, "RecentRow: Not implemented yet.")
            Resource.State.Empty -> PlaceHolder(
                lottieResource = R.raw.empty,
                message = recent.message ?: "",
                size = 99.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlaybackHistory.LazyList(
    modifier: Modifier = Modifier,
) {
    val audios by data
    audios?.let { value ->
        val observer = remember {
            { view: RecyclerView ->
                object : RecyclerView.AdapterDataObserver() {
                    override fun onChanged() {
                        if ((view.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0)
                            view.scrollToPosition(0)
                    }

                    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                        onChanged()
                    }

                    override fun onItemRangeChanged(
                        positionStart: Int,
                        itemCount: Int,
                        payload: Any?
                    ) {
                        onChanged()
                    }

                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        onChanged()
                    }

                    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                        onChanged()
                    }

                    override fun onItemRangeMoved(
                        fromPosition: Int,
                        toPosition: Int,
                        itemCount: Int
                    ) {
                        onChanged()
                    }
                }
            }
        }

        val callback = remember {
            object : DiffUtil.ItemCallback<Audio>() {
                override fun areItemsTheSame(oldItem: Audio, newItem: Audio): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: Audio, newItem: Audio): Boolean {
                    return oldItem == newItem
                }
            }
        }

        Recycler(
            list = value,
            callback = remember { callback },
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = Padding.LARGE),
            orientation = RecyclerView.HORIZONTAL,
            observer = observer
        ) { audio ->
            RecentItem(
                audio = audio,
                modifier = Modifier
                    .clickable { }
                    .padding(2.dp)
            )
        }
    }
}

/**
 * Representing recent Item
 */

@Composable
private fun RecentItem(
    modifier: Modifier = Modifier,
    audio: Audio
) {
    val circle = CircleShape
    val elevationPx = with(LocalDensity.current) { 8.dp.toPx() }
    Column(
        modifier = modifier.width(70.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val forceAccent by with(Preferences.get(LocalContext.current)) { requiresAccentThoroughly().collectAsState() }
        val color = if (forceAccent) PlayerTheme.colors.primary else PlayerTheme.colors.onSurface

        AlbumArt(
            contentDescription = "Artwork",
            albumId = audio.album?.id ?: -1,
            modifier = Modifier
                //.padding(8.dp)
                .graphicsLayer {
                    this.shadowElevation = elevationPx
                    shape = circle
                }
                .border(width = 2.dp, color = color, shape = circle)
                .padding(Padding.SMALL)
                .clip(circle)
                .verticalGradient()
                .requiredSize(50.dp)
        )

        Caption(
            text = audio.title,
            modifier = Modifier
                .padding(vertical = Padding.SMALL)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}