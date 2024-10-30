@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.prime.media.impl

import android.provider.MediaStore
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.prime.media.R
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.menu.Action
import com.prime.media.local.albums.AlbumsViewState
import com.zs.core.store.Album
import com.zs.core.store.MediaProvider
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

private const val TAG = "AlbumsViewModel"

private val Album.firstTitleChar
    inline get() = title.uppercase(Locale.ROOT)[0].toString()

private val ORDER_BY_TITLE = Action(R.string.title, id = MediaStore.Audio.Albums.ALBUM)
private val ORDER_BY_NONE = Action(R.string.none, id =  MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)
private val ORDER_BY_ARTIST = Action(R.string.artist, id = MediaStore.Audio.Albums.ARTIST)

private inline val TextFieldState.raw get() = text.trim().toString().ifEmpty { null }

class AlbumsViewModel(provider: MediaProvider) : KoinViewModel(), AlbumsViewState {

    override val orders: List<Action> =
        listOf(ORDER_BY_NONE, ORDER_BY_TITLE, ORDER_BY_ARTIST)
    override val query: TextFieldState = TextFieldState()
    override var order: Filter by mutableStateOf(true to ORDER_BY_TITLE)

    override fun filter(ascending: Boolean, order: Action) {
        if (order == this.order.second && this.order.first == ascending)
            return
        this.order = ascending to order
    }

    override val data: StateFlow<Mapped<Album>?> = combine(
        snapshotFlow(query::raw).onStart { emit(null) }.drop(1).debounce(300L),
        snapshotFlow(::order),
        transform = { query, filter -> Triple(query, filter.first, filter.second) }
    ).flatMapLatest { (query, filter, order) ->
        provider.observer(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI).map {
            val albums = provider.fetchAlbums(query, order.id, filter)
            when (order) {
                ORDER_BY_TITLE -> albums.groupBy { it.firstTitleChar }
                ORDER_BY_ARTIST -> albums.groupBy { it.artist }
                else -> albums.groupBy { "" }
            }
        }
    }// catch any errors.
        .catch {
            val report = report(it.message ?: getText(R.string.msg_unknown_error))
            if (report == Toast.ACTION_PERFORMED)
                Firebase.crashlytics.recordException(it)
        }
        // make sure the flow is released after sometime.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000L), null)
}