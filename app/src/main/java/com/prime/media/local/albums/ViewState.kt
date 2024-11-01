package com.prime.media.local.albums

import androidx.compose.foundation.text.input.TextFieldState
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.Route
import com.prime.media.common.menu.Action
import com.zs.core.store.Album
import kotlinx.coroutines.flow.StateFlow

object RouteAlbums : Route


interface AlbumsViewState {
    val orders: List<Action>
    val data: StateFlow<Mapped<Album>?>

    // filter.
    val query: TextFieldState
    val order: Filter

    // actions

    fun filter(ascending: Boolean = this.order.first, order: Action = this.order.second)
}