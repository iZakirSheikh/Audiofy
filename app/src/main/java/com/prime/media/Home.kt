package com.prime.media

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.media.console.Console
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowPadding
import com.prime.media.core.compose.preference
import com.prime.media.core.compose.scaffold.Scaffold2
import com.prime.media.core.compose.scaffold.ScaffoldState2
import com.prime.media.core.compose.scaffold.SheetState
import com.prime.media.core.compose.scaffold.rememberScaffoldState2
import com.prime.media.playlists.Members
import com.prime.media.playlists.Playlists
import com.prime.media.store.Albums
import com.prime.media.store.Artists
import com.prime.media.store.Audios
import com.prime.media.store.Folders
import com.prime.media.store.Genres
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
import com.primex.core.MetroGreen
import com.primex.core.OrientRed
import com.primex.core.Rose
import com.primex.core.SignalWhite
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.blend
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
    val peekHeight = if (show) Settings.MINI_PLAYER_HEIGHT else 0.dp
    val windowPadding by rememberUpdatedState(PaddingValues(bottom = peekHeight))
    val provider = LocalSystemFacade.current
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
            channel = provider.channel,
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
/**
 * A simple/shortcut typealias of [MaterialTheme]
 */
typealias Theme = MaterialTheme

private const val TAG = "Home"

private val defaultPrimaryColor = Color.MetroGreen
private val defaultSecondaryColor = Color.Rose

@Composable
fun Theme(isDark: Boolean, content: @Composable () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0E0E0F) else Color(0xFFF5F5FA),
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )

    // TODO: update status_bar here.

    val surface by animateColorAsState(
        targetValue = if (isDark) Color.TrafficBlack else Color.White,
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )

    val primary = defaultPrimaryColor
    val secondary = defaultSecondaryColor


    val colors = Colors(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        primaryVariant = primary.blend(Color.Black, 0.2f),
        secondaryVariant = secondary.blend(Color.Black, 0.2f),
        onPrimary = Color.SignalWhite,
        onSurface = if (isDark) Color.SignalWhite else Color.UmbraGrey,
        onBackground = if (isDark) Color.SignalWhite else Color.Black,
        error = Color.OrientRed,
        onSecondary = Color.SignalWhite,
        onError = Color.SignalWhite,
        isLight = !isDark
    )

    // TODO: update status_bar here.
    val colorize by preference(key = Settings.COLOR_STATUS_BAR)
    val uiController = rememberSystemUiController()
    val isStatusBarHidden by preference(key = Settings.HIDE_STATUS_BAR)
    Log.d(TAG, "Theme: $colorize $isStatusBarHidden")
    SideEffect {
        uiController.setSystemBarsColor(
            if (colorize) colors.primaryVariant else Color.Transparent,
            darkIcons = !colorize && !isDark,
        )
        uiController.isStatusBarVisible = !isStatusBarHidden
    }


    MaterialTheme(
        colors = colors,
        //typography = Typography(fontFamily),
        //  shapes = defaultThemeShapes,
        content = content
    )
}