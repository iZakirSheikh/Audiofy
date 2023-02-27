package com.prime.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.prime.player.common.*
import com.prime.player.console.Console
import com.prime.player.console.ConsoleViewModel
import com.prime.player.core.compose.Player
import com.prime.player.core.compose.PlayerState
import com.prime.player.core.compose.PlayerValue
import com.prime.player.core.compose.rememberPlayerState
import com.prime.player.directory.local.*
import com.prime.player.library.Library
import com.prime.player.library.LibraryViewModel
import com.prime.player.settings.Settings
import com.prime.player.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
private val EnterTransition =
    scaleIn(
        initialScale = 0.98f,
        animationSpec = tween(220, delayMillis = 90)
    ) + fadeIn(animationSpec = tween(700))

private val ExitTransition =
    fadeOut(tween(700))

private suspend inline fun PlayerState.toggle() = if (isExpanded) collapse() else expand()

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun NavGraph() {
    AnimatedNavHost(
        navController = LocalNavController.current,
        startDestination = Library.route,
        modifier = Modifier,
        enterTransition = { EnterTransition },
        exitTransition = { ExitTransition },
        builder = {

            composable(Library.route) {
                val viewModel = hiltViewModel<LibraryViewModel>()
                Library(viewModel = viewModel)
            }

            composable(Settings.route) {
                val viewModel = hiltViewModel<SettingsViewModel>()
                Settings(viewModel = viewModel)
            }

            composable(Albums.route) {
                val viewModel = hiltViewModel<AlbumsViewModel>()
                Albums(viewModel = viewModel)
            }

            composable(Artists.route) {
                val viewModel = hiltViewModel<ArtistsViewModel>()
                Artists(viewModel = viewModel)
            }

            composable(Audios.route) {
                val viewModel = hiltViewModel<AudiosViewModel>()
                Audios(viewModel = viewModel)
            }

            composable(Folders.route) {
                val viewModel = hiltViewModel<FoldersViewModel>()
                Folders(viewModel = viewModel)
            }

            composable(Genres.route) {
                val viewModel = hiltViewModel<GenresViewModel>()
                Genres(viewModel = viewModel)
            }

            composable(Playlists.route) {
                val viewModel = hiltViewModel<PlaylistsViewModel>()
                Playlists(viewModel = viewModel)
            }

            composable(Members.route) {
                val viewModel = hiltViewModel<MembersViewModel>()
                Members(viewModel = viewModel)
            }
        }
    )
}


@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun Home(show: Boolean) {
    // construct necessary variables.
    val controller = rememberAnimatedNavController()
    // required for toggling player.
    val scope = rememberCoroutineScope()
    //Handle messages etc.
    val state =
        rememberPlayerState(initial = PlayerValue.COLLAPSED)
    // collapse if expanded and
    // back button is clicked.
    BackHandler(state.isExpanded) { scope.launch { state.collapse() } }
    val peekHeight = if (show) Audiofy.MINI_PLAYER_HEIGHT else 0.dp
    val windowPadding by rememberUpdatedState(PaddingValues(bottom = peekHeight))
    CompositionLocalProvider(
        //TODO: maybe use the windowInsets somehow
        LocalWindowPadding provides windowPadding,
        LocalNavController provides controller,
    ) {
        Player(
            sheet = {
                //FixMe: May be use some kind of scope.
                val consoleViewModel = hiltViewModel<ConsoleViewModel>()
                Console(consoleViewModel, state.progress.value) { scope.launch { state.toggle() } }
            },
            state = state,
            sheetPeekHeight = peekHeight,
            toast = LocalContext.toastHostState,
            progress = LocalContext.inAppUpdateProgress.value,
            content = {
                Surface(modifier = Modifier.fillMaxSize(), color = Theme.colors.background) {
                    NavGraph()
                }
            },
            modifier = Modifier
                .background(Theme.colors.background)
                .navigationBarsPadding()
        )
    }
}
