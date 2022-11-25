package com.prime.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.prime.player.audio.Player
import com.prime.player.audio.buckets.Buckets
import com.prime.player.audio.buckets.BucketsViewModel
import com.prime.player.audio.console.Console
import com.prime.player.audio.console.ConsoleViewModel
import com.prime.player.audio.library.Library
import com.prime.player.audio.library.LibraryViewModel
import com.prime.player.audio.tracks.Tracks
import com.prime.player.audio.tracks.TracksViewModel
import com.prime.player.common.compose.LocalNavController
import com.prime.player.common.compose.LocalSnackDataChannel
import com.prime.player.common.compose.LocalWindowPadding
import com.prime.player.common.compose.stringResource
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

    // The state of the Snackbar
    val snackbar = remember(::SnackbarHostState)
    //Handle messages etc.
    val state =
        rememberBottomSheetScaffoldState(snackbarHostState = snackbar)

    // observe the channel
    // emit the updates
    val channel = LocalSnackDataChannel.current
    val resource = LocalContext.current.resources
    LaunchedEffect(key1 = channel) {
        channel.receiveAsFlow().collect { (label, message, duration, action) ->
            // dismantle the given snack and use the corresponding components
            val result = snackbar.showSnackbar(
                message = resource.stringResource(message).text,
                actionLabel = resource.stringResource(label)?.text
                    ?: resource.getString(R.string.dismiss),
                duration = duration
            )
            // action based on
            when (result) {
                SnackbarResult.ActionPerformed -> action?.invoke()
                SnackbarResult.Dismissed -> { /*do nothing*/
                }
            }
        }
    }

    BackHandler(state.bottomSheetState.isExpanded) {
        if (state.bottomSheetState.isExpanded)
            scope.launch {
                state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Collapsed)
            }
    }

    var windowPadding by rememberState(initial = PaddingValues(0.dp))
    CompositionLocalProvider(
        LocalWindowPadding provides windowPadding,
        LocalNavController provides controller,
    ) {
        //Bottom sheet
        BottomSheetScaffold(
            backgroundColor = Material.colors.background,
            scaffoldState = state,
            sheetElevation = 0.dp,
            sheetGesturesEnabled = false,
            sheetBackgroundColor = androidx.compose.ui.graphics.Color.Transparent,
            sheetPeekHeight = if (show) Player.MINI_PLAYER_HEIGHT else 0.dp,

            sheetContent = {
                Console(
                    viewModel = consoleViewModel,
                    expanded = state.bottomSheetState.isExpanded
                ) {
                    scope.launch {
                        if (state.bottomSheetState.isExpanded) {
                            state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Collapsed)
                        } else {
                            state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Expanded)
                        }
                    }
                }
            },

            content = { inner ->
                // update window padding when ever it changes.
                windowPadding = inner
                Box(
                    Modifier.fillMaxSize(),
                    content = { NavGraph() }
                )
            },
        )
    }
}


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

