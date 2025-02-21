package com.prime.media.local

import androidx.compose.foundation.text.input.TextFieldState
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.menu.Action
import kotlinx.coroutines.flow.StateFlow

/**
* Represents the common interface among directories
* @property data
*/
interface DirectoryViewState<T> {



    companion object

    val title: CharSequence

    val orders: List<Action>
    val data: StateFlow<Mapped<T>?>

    // filter.
    val query: TextFieldState
    val order: Filter

    // actions
    fun filter(ascending: Boolean = this.order.first, order: Action = this.order.second)
}