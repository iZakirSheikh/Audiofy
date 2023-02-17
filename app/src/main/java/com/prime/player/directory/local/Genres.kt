package com.prime.player.directory.local

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.player.R
import com.prime.player.core.Repository
import com.prime.player.core.compose.ToastHostState
import com.prime.player.core.db.Album
import com.prime.player.core.db.Audio
import com.prime.player.core.playback.Remote
import com.prime.player.directory.*
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"


typealias Genres = GenresViewModel.Companion

@HiltViewModel
class GenresViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: ToastHostState,
    private val remote: Remote,
) : DirectoryViewModel<Audio>(handle) {

    companion object {
        private const val HOST = "_local_genres"

        val route = compose(HOST)
        fun direction(
            query: String = NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = compose(HOST, NULL_STRING, query, order, ascending, viewType)
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
fun Genres(viewModel: GenresViewModel) {

}