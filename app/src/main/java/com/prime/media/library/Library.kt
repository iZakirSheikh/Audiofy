package com.prime.media.library

import com.prime.media.core.db.Audio
import com.prime.media.core.db.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Library {

    companion object {
        const val route = "library"
    }

    /**
     * The recently played tracks.
     */
    val recent: Flow<List<Playlist.Member>>
    val carousel: StateFlow<Long?>
    val newlyAdded: Flow<List<Audio>>
}