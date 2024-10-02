package com.prime.media.old.library

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import com.prime.media.old.core.db.Audio
import com.primex.core.Text
import com.zs.core.db.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Library {

    companion object {
        val route: String get() = "route_library"
        fun direction() = route
    }

    /**
     * The recently played tracks.
     */
    val recent: StateFlow<List<Playlist.Track>?>
    val carousel: StateFlow<Long?>
    val newlyAdded: StateFlow<List<Audio>?>

    /**
     * Callback method invoked upon clicking a history item.
     *
     * This method manages diverse scenarios contingent on the selected history item:
     *
     * - Scenario 1: In the event the newly added file is already present in the playback queue,
     *   the queue will be directed to the chosen item's position, initiating playback from there.
     *
     * - Scenario 2: If the recently added item is absent from the queue, the ensuing sub-scenarios arise:
     *   - Sub-Scenario 2.1: A Snackbar is exhibited to the user, offering options to either append
     *     the item to the current queue at next or substitute the queue with this recently added playlist.
     *   - Sub-Scenario 2.2: Further actions are contingent upon the user's decision.
     *
     * @param uri The URI of the history item clicked by the user.
     */
    fun onClickRecentFile(uri: String)

    /**
     * Callback method triggered when a recently added  item is clicked.
     * This function handles the action where the recently added file is either included in the
     * playback queue or prompts the user to replace the existing queue with recently added items.
     *
     * @param id The unique identifier of the recently added history item.
     */
    fun onClickRecentAddedFile(id: Long)

    /**
     * Requests to play a video from a given URI using Media3 player.
     * @param uri The URI of the video to play, such as a content URI or a web URL.
     * @param context The context of the caller, such as an activity or a service.
     */
    fun onRequestPlayVideo(uri: Uri, context: Context)
}