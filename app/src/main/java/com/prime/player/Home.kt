package com.prime.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.prime.player.buckets.Buckets
import com.prime.player.buckets.BucketsViewModel
import com.prime.player.common.*
import com.prime.player.console.Console
import com.prime.player.console.ConsoleViewModel
import com.prime.player.library.Library
import com.prime.player.library.LibraryViewModel
import com.prime.player.settings.Settings
import com.prime.player.settings.SettingsViewModel
import com.prime.player.tracks.MainGraphRoutes
import com.prime.player.tracks.Tracks
import com.prime.player.tracks.TracksViewModel
import cz.levinzonr.saferoute.core.ProvideRouteSpecArgs
import cz.levinzonr.saferoute.core.RouteSpec
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
private val EnterTransition =
    scaleIn(initialScale = 0.98f, animationSpec = tween(220, delayMillis = 90)) +
            fadeIn(animationSpec = tween(700))
private val ExitTransition = fadeOut(tween(700))


///missing fun
@OptIn(ExperimentalAnimationApi::class)
private fun NavGraphBuilder.composable(
    spec: RouteSpec<*>,
    enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    exitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    popEnterTransition: (
    AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?
    )? = enterTransition,
    popExitTransition: (
    AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?
    )? = exitTransition,
    content: @Composable (NavBackStackEntry) -> Unit
) = composable(
    spec.route,
    spec.navArgs,
    spec.deepLinks,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    popEnterTransition = popEnterTransition,
    popExitTransition = popExitTransition
) {
    ProvideRouteSpecArgs(spec = spec, entry = it) {
        content.invoke(it)
    }
}

private suspend inline fun PlayerState.toggle() = if (isExpanded) collapse() else expand()

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun NavGraph() {
    AnimatedNavHost(
        navController = LocalNavController.current,
        startDestination = MainGraphRoutes.Library.route,
        modifier = Modifier,
        enterTransition = { EnterTransition },
        exitTransition = { ExitTransition },
        builder = {

            composable(MainGraphRoutes.Library) {
                val viewModel = hiltViewModel<LibraryViewModel>()
                Library(viewModel = viewModel)
            }

            composable(MainGraphRoutes.Buckets) {
                val viewModel = hiltViewModel<BucketsViewModel>()
                Buckets(viewModel = viewModel)
            }

            composable(MainGraphRoutes.Tracks) {
                val viewModel = hiltViewModel<TracksViewModel>()
                Tracks(viewModel = viewModel)
            }

            composable(MainGraphRoutes.Settings) {
                val viewModel = hiltViewModel<SettingsViewModel>()
                Settings(viewModel = viewModel)
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
                Surface(modifier = Modifier.fillMaxSize(), color = Material.colors.background) {
                    NavGraph()
                }
            }
        )
    }
}