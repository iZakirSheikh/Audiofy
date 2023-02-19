package com.prime.player.directory.local

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import com.prime.player.core.Repository
import com.prime.player.core.compose.ToastHostState
import com.prime.player.core.db.Audio
import com.prime.player.core.db.Playlist
import com.prime.player.core.db.Playlist.Member
import com.prime.player.core.playback.Remote
import com.prime.player.directory.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"

typealias Members = MembersViewModel.Companion

@HiltViewModel
class MembersViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: ToastHostState,
    private val remote: Remote,
) : DirectoryViewModel<Member>(handle) {

    companion object {
        private const val HOST = "_local_playlist_members"

        val route = compose(HOST)
        fun direction(
            key: String,
            query: String = NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = compose(HOST, Uri.encode(key), query, order, ascending, viewType)
    }

    override fun toggleViewType() {
        TODO("Not yet implemented")
    }

    override val actions: List<Action>
        get() = TODO("Not yet implemented")
    override val orders: List<GroupBy>
        get() = TODO("Not yet implemented")
    override val mActions: List<Action?>
        get() = TODO("Not yet implemented")
    override val data: Flow<Any>
        get() = TODO("Not yet implemented")
}


@Composable
fun Members(viewModel: MembersViewModel){

}

