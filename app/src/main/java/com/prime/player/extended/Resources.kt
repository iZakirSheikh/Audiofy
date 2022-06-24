package com.prime.player.extended

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A generic class that holds a value or an exception
 */
class Resource<out R>(val data: R) {

    val state: StateFlow<State> = MutableStateFlow(State.Loading)
    var message: String? = null

    fun emit(new: State) {
        (state as MutableStateFlow).value = new
    }

    fun error(msg: String?){
        message = msg
        emit(State.Error)
    }

    fun empty(msg: String?){
        message = msg
        emit(State.Empty)
    }

    enum class State {
        Loading,
        Success,
        Error,
        Empty,
    }
}

fun <T> Resource<State<T>>.success(new: T){
    emit(Resource.State.Success)
    (data as MutableState).value = new
}
