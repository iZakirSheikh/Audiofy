@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.prime.media.impl

import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.prime.media.library.LibraryViewState
import com.prime.media.common.AppConfig
import com.primex.core.Rose
import com.zs.core.db.Playlist
import com.zs.core.db.Playlists
import com.zs.core.playback.PlaybackController
import com.zs.core.playback.toMediaFile
import com.zs.core.store.MediaProvider
import com.zs.core.util.debounceAfterFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    val provider: MediaProvider,
    val playlists: Playlists,
    val remote: PlaybackController
) : LibraryViewState, KoinViewModel() {

    private val CAROUSAL_DELAY_MILLS = 10_000L // 10 seconds
    private val SHOW_CASE_MAX_ITEMS = 20

    override val recent: StateFlow<List<Playlist.Track>?> =
        playlists.observer("_recent")
            .map { it.distinctBy { it.uri } }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    override val carousel =
    // Observe changes to the media store URI. When a change occurs (e.g., new audio added),
        // the flow is re-evaluated.
        provider
            .observer(MediaProvider.EXTERNAL_CONTENT_URI)
            .debounceAfterFirst(500L)
            // Use flatMapLatest to cancel the previous flow and start a new one
            // whenever the external content URI changes.
            .flatMapLatest { _ ->
                val items = provider.fetchAudioFiles(
                    limit = SHOW_CASE_MAX_ITEMS,
                    order = MediaProvider.COLUMN_DATE_MODIFIED,
                    ascending = false
                )
                // If no items are found, maybe return a default value
                // Emit a default if no items
                if (items.isEmpty()) return@flatMapLatest flow { emit(-1L) }
                flow {
                    var current = 0
                    while (true) {
                        if (current >= items.size) current = 0 // Loop back to the beginning
                        emit(items[current].albumId) // Emit the ID of the current item
                        current++
                        // Wait for the delay before emitting the next item
                        delay(CAROUSAL_DELAY_MILLS)
                    }
                }
            }
            .catch { Log.d("TAG", "sds: $it") }
            .stateIn(viewModelScope, SharingStarted.Lazily, -1L)


    override val newlyAdded = provider
        .observer(MediaProvider.EXTERNAL_CONTENT_URI)
        .debounceAfterFirst(500L)
        .map {
            provider.fetchAudioFiles(
                order = MediaProvider.COLUMN_DATE_MODIFIED,
                ascending = false,
                offset = 0,
                limit = SHOW_CASE_MAX_ITEMS
            ).filter { it.duration > AppConfig.minTrackLengthSecs * 1000 }
        }
        .catch {
            Log.d("TAG", "ssd: $it")
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    override fun onClickRecentFile(uri: String) {
        // This function handles the click event for a recent file.
        // It determines if the clicked file is already in the current playlist.
        //
        // Case 1: The media item is already in the playlist.
        //   - In this case, the function simply seeks to the item's position in the playlist
        //     and starts playback.
        //
        // Case 2: The media item is not in the playlist.
        //   - The function first tries to find the item in the 'recent' list.
        //   - If found, the current playlist is replaced with a new playlist containing only this item,
        //     and playback starts.
        //   - If the item is not found in the 'recent' list (which is unexpected),
        //     an error message is displayed.
        tryLaunch {
            val index = remote.indexOf(Uri.parse(uri))
            val isAlreadyInPlaylist =
                index != PlaybackController.INDEX_UNSET // Check if the item is in the current playlist.

            if (isAlreadyInPlaylist) {
                // If already in playlist, seek to it and play.
                remote.seekTo(index)
                remote.play(true)
                return@tryLaunch
            }

            // If not in the playlist, try to find it in the recent list.
            val recentItems = recent.firstOrNull() // Get the current list of recent items.
            val item = recentItems?.find { it.uri == uri } // Find the specific item by its URI.

            if (item == null) {
                // This case should ideally not happen if the UI is displaying valid recent items.
                showToast(
                    "Oops! Unknown error",
                    icon = Icons.Outlined.Error,
                    accent = Color.Rose
                )
                return@tryLaunch
            }
            // If found in recent, create a new playlist with this item and play it.
            remote.setMediaFiles(listOf(item.toMediaFile())) // Replace current playlist.
            remote.play(true)
        }
    }

    override fun onClickRecentAddedFile(id: Long) {
        // This function handles the click event for a newly added file.
        // When a newly added file is clicked, this function ensures that only this specific file is played.
        tryLaunch {
            // Retrieve the list of newly added files.
            val newlyAddedItems = newlyAdded.firstOrNull()
            // Find the specific item by its ID.
            val item = newlyAddedItems?.find { it.id == id }
            if (item == null) {
                // This case should ideally not happen if the UI is displaying valid newly added items.
                showToast(
                    "Oops! Unknown error",
                    icon = Icons.Outlined.Error,
                    accent = Color.Rose
                )
                return@tryLaunch
            }
            // Create a new playlist with this item and start playback.
            remote.setMediaFiles(listOf(item.toMediaFile()))
            remote.play(true)
        }
    }

    override fun onRequestRemoveRecentItem(uri: String) {
        tryLaunch {
            // Retrieve the "Recently Played" playlist entry
            val playlist = playlists[PlaybackController.PLAYLIST_RECENT]
                ?: error("Recent playlist not found. Cannot proceed with removal.")

            // Attempt to remove the item from the playlist
            val removedCount = playlists.remove(playlist.id, uri)

            // Show feedback based on result
            if (removedCount == 1)
                showPlatformToast("Item removed from history.")
            else
                error("Failed to remove item from recent playlist. URI may be invalid or missing.")
        }
    }
}