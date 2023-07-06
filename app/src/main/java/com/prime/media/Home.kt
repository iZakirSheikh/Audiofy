package com.prime.media

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.media.core.compose.*
import com.prime.media.console.Console
import com.prime.media.console.ConsoleViewModel
import com.prime.media.core.compose.Scaffold2
import com.prime.media.core.compose.ScaffoldState
import com.prime.media.core.compose.SheetValue
import com.prime.media.core.compose.rememberScaffoldState2
import com.prime.media.directory.playlists.Members
import com.prime.media.directory.playlists.MembersViewModel
import com.prime.media.directory.playlists.Playlists
import com.prime.media.directory.playlists.PlaylistsViewModel
import com.prime.media.directory.store.*
import com.prime.media.library.Library
import com.prime.media.library.LibraryViewModel
import com.prime.media.settings.Settings
import com.prime.media.settings.SettingsViewModel
import com.primex.core.MetroGreen
import com.primex.core.OrientRed
import com.primex.core.Rose
import com.primex.core.SignalWhite
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.blend
import kotlinx.coroutines.launch

private const val TAG = "Home"

private val EnterTransition =
    scaleIn(
        initialScale = 0.98f,
        animationSpec = tween(220, delayMillis = 90)
    ) + fadeIn(animationSpec = tween(700))

private val ExitTransition =
    fadeOut(tween(700))

private suspend inline fun ScaffoldState.toggle() = if (isExpanded) collapse() else expand()

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


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Home(show: Boolean) {
    // construct necessary variables.
    val controller = rememberAnimatedNavController()
    // required for toggling player.
    val scope = rememberCoroutineScope()
    //Handle messages etc.
    val state =
        rememberScaffoldState2(initial = SheetValue.COLLAPSED)
    // collapse if expanded and
    // back button is clicked.
    BackHandler(state.isExpanded) { scope.launch { state.collapse() } }
    val peekHeight = if (show) Audiofy.MINI_PLAYER_HEIGHT else 0.dp
    val windowPadding by rememberUpdatedState(PaddingValues(bottom = peekHeight))
    val provider = LocalsSystemFacade.current
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
            toast = provider.channel,
            progress = provider.inAppUpdateProgress.value,
            content = {
                Surface(modifier = Modifier.fillMaxSize(), color = Material.colors.background) {
                    NavGraph()
                }
            },
            modifier = Modifier
                .background(Material.colors.background)
                .navigationBarsPadding()
        )
    }
}
/**
 * A simple/shortcut typealias of [MaterialTheme]
 */
typealias Material = MaterialTheme

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
    val colorize by preference(key = Audiofy.COLOR_STATUS_BAR)
    val uiController = rememberSystemUiController()
    val isStatusBarHidden by preference(key = Audiofy.HIDE_STATUS_BAR)
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
        content = content
    )
}