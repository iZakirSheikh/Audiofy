package com.prime.media

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.prime.media.console.Console
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalWindowPadding
import com.prime.media.core.compose.scaffold.Scaffold2
import com.prime.media.core.compose.scaffold.ScaffoldState2
import com.prime.media.core.compose.scaffold.SheetState
import com.prime.media.core.compose.scaffold.rememberScaffoldState2
import com.prime.media.directory.playlists.Members
import com.prime.media.directory.playlists.Playlists
import com.prime.media.directory.store.Albums
import com.prime.media.directory.store.Artists
import com.prime.media.directory.store.Audios
import com.prime.media.directory.store.Folders
import com.prime.media.directory.store.Genres
import com.prime.media.impl.ConsoleViewModel
import com.prime.media.impl.LibraryViewModel
import com.prime.media.impl.SettingsViewModel
import com.prime.media.impl.playlists.MembersViewModel
import com.prime.media.impl.playlists.PlaylistsViewModel
import com.prime.media.impl.store.AlbumsViewModel
import com.prime.media.impl.store.ArtistsViewModel
import com.prime.media.impl.store.AudiosViewModel
import com.prime.media.impl.store.FoldersViewModel
import com.prime.media.impl.store.GenresViewModel
import com.prime.media.library.Library
import com.prime.media.settings.Settings
import kotlinx.coroutines.launch

private val EnterTransition =
    scaleIn(
        initialScale = 0.98f,
        animationSpec = tween(220, delayMillis = 90)
    ) + fadeIn(animationSpec = tween(700))

private val ExitTransition =
    fadeOut(tween(700))

private suspend inline fun ScaffoldState2.toggle() = if (isExpanded) collapse() else expand()

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

            composable(com.prime.media.settings.Settings.route) {
                val viewModel = hiltViewModel<SettingsViewModel>()
                Settings(viewModel = viewModel)
            }

            composable(Albums.route) {
                val viewModel = hiltViewModel<AlbumsViewModel>()
                Albums(viewModel = viewModel)
            }

            composable(Artists.route) {
                val viewModel = hiltViewModel<ArtistsViewModel>()
                Artists(state = viewModel)
            }

            composable(Audios.route) {
                val viewModel = hiltViewModel<AudiosViewModel>()
                Audios(state = viewModel)
            }

            composable(Folders.route) {
                val viewModel = hiltViewModel<FoldersViewModel>()
                Folders(state = viewModel)
            }

            composable(Genres.route) {
                val viewModel = hiltViewModel<GenresViewModel>()
                Genres(state = viewModel)
            }

            composable(Playlists.route) {
                val viewModel = hiltViewModel<PlaylistsViewModel>()
                Playlists(state = viewModel)
            }

            composable(Members.route) {
                val viewModel = hiltViewModel<MembersViewModel>()
                Members(state = viewModel)
            }
        }
    )
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Home(show: Boolean) {
    // construct necessary variables.
    val controller = rememberAnimatedNavController()
    // required for toggling player.
    val scope = rememberCoroutineScope()
    //Handle messages etc.
    val state =
        rememberScaffoldState2(initial = SheetState.COLLAPSED)
    // collapse if expanded and
    // back button is clicked.
    BackHandler(state.isExpanded) { scope.launch { state.collapse() } }
    val peekHeight = if (show) Audiofy.MINI_PLAYER_HEIGHT else 0.dp
    val windowPadding by rememberUpdatedState(PaddingValues(bottom = peekHeight))
    val provider = LocalsProvider.current
    CompositionLocalProvider(
        //TODO: maybe use the windowInsets somehow
        LocalWindowPadding provides windowPadding,
        LocalNavController provides controller,
    ) {
        Scaffold2(
            sheet = {
                //FixMe: May be use some kind of scope.
                val consoleViewModel = hiltViewModel<ConsoleViewModel>()
                Console(consoleViewModel, state.progress.value) { scope.launch { state.toggle() } }
            },
            state = state,
            sheetPeekHeight = peekHeight,
            channel = provider.toastHostState,
            progress = provider.inAppUpdateProgress.value,
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
