@file:Suppress("NOTHING_TO_INLINE")

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prime.player.audio.console.ConsoleViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Console(
    viewModel: ConsoleViewModel,
    expanded: Boolean,
    toggle: () -> Unit
) {
    Box(Modifier.fillMaxSize()){

    }
}