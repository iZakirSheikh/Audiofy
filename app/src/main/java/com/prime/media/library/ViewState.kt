package com.prime.media.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import com.prime.media.core.Route
import com.prime.media.core.db.Audio
import com.prime.media.core.db.Playlist
import com.primex.core.Text
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Library {

    companion object : Route {
        override val title: Text get() = Text("Library")
        override val icon: ImageVector get() = Icons.Outlined.LibraryMusic
        override val route: String get() = "route_library"

        fun direction() = route
    }

    /**
     * The recently played tracks.
     */
    val recent: Flow<List<Playlist.Member>>
    val carousel: StateFlow<Long?>
    val newlyAdded: Flow<List<Audio>>

}