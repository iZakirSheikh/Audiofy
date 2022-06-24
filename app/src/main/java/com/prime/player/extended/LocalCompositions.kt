package com.prime.player.extended

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.channels.Channel


typealias Message = Pair<List<String>, (() -> Unit)?>
typealias Messenger = Channel<Message>

/**
 * *Local Messenger*
 *
 *  Construct a [LocalMessenger] to send messages across.
 *
 *  ***This makes it possible to handle messages at a single point***
 *
 *  The Local messenger is a composition of [Channel] API which takes a pair as argument.
 *
 *  The pair consists of list of size 2 and action. list[0] = {action label, can be empty},
 *  list[1] = message must not be empty.
 *  action can be null
 */
val LocalMessenger = staticCompositionLocalOf<Messenger> {
    error("no local messenger provided!!")
}


val LocalSystemUiController = staticCompositionLocalOf<SystemUiController> {
    error("No system UI Controller defined!!.")
}


suspend fun Messenger.send(
    label: String,
    message: String,
    action: (() -> Unit)?
) {
    send(
        listOf(
            label,
            message
        ) to action
    )
}

suspend fun Messenger.send(
    message: String,
) {
    send(
        listOf(
            "",
            message
        ) to null
    )
}

val LocalNavActionProvider = staticCompositionLocalOf<INavActions> {
    error("no local nav controller found")
}

@Composable
fun ProvideNavActions(
    actions: INavActions,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNavActionProvider provides actions) {
        content()
    }
}


interface INavActions {
    fun getNavController(): NavHostController

    fun navigateUp()
}