package com.prime.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.prime.player.audio.buckets.Buckets
import com.prime.player.audio.buckets.BucketsViewModel
import com.prime.player.audio.console.Console
import com.prime.player.audio.console.ConsoleViewModel
import com.prime.player.audio.library.Library
import com.prime.player.audio.library.LibraryViewModel
import com.prime.player.audio.tracks.Tracks
import com.prime.player.audio.tracks.TracksViewModel
import com.prime.player.common.compose.*
import com.prime.player.settings.MainGraphRoutes
import com.prime.player.settings.Settings
import com.prime.player.settings.SettingsViewModel
import com.primex.core.rememberState
import cz.levinzonr.saferoute.core.ProvideRouteSpecArgs
import cz.levinzonr.saferoute.core.RouteSpec
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
private val EnterTransition = scaleIn(
    initialScale = 0.98f,
    animationSpec = tween(220, delayMillis = 90)
) + fadeIn(animationSpec = tween(700))

private val ExitTransition = fadeOut(tween(700))

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

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun Home() {
    // Currently; supports only 1 Part
    // add others in future
    // including support for more tools, like direction, prime factorization etc.
    // also support for navGraph.
    val controller = rememberAnimatedNavController()
    val scope = rememberCoroutineScope()
    val consoleViewModel = hiltViewModel<ConsoleViewModel>()

    //FixMe - Needs to be corrected.
    val connected by consoleViewModel.connected
    LaunchedEffect(key1 = connected) {
        //delay(1000)
        consoleViewModel.connect()
    }
    val show = consoleViewModel.current.value != null
    //Handle messages etc.
    val state =
        rememberPlayerState(initial = PlayerValue.COLLAPSED)

    BackHandler(state.isExpanded) {
        scope.launch { state.snapTo(PlayerValue.COLLAPSED) }
    }

    val peekHeight = if (show) Audiofy.MINI_PLAYER_HEIGHT else 0.dp
    val windowPadding by rememberUpdatedState(PaddingValues(bottom = peekHeight))
    CompositionLocalProvider(
        LocalWindowPadding provides windowPadding,
        LocalNavController provides controller,
    ) {
        Player(
            sheet = {
                Console(consoleViewModel, state.isExpanded, { scope.launch { state.toggle() } })
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

private suspend inline fun PlayerState.toggle() {
    if (isExpanded) {
        snapTo(targetValue = PlayerValue.COLLAPSED)
    } else {
        snapTo(targetValue = PlayerValue.EXPANDED)
    }
}