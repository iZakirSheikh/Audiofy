package com.prime.player.audio

import Console
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.prime.player.Material
import com.prime.player.R
import com.prime.player.audio.buckets.Buckets
import com.prime.player.audio.buckets.BucketsViewModel
import com.prime.player.audio.console.ConsoleViewModel
import com.prime.player.audio.library.Library
import com.prime.player.audio.library.LibraryViewModel
import com.prime.player.audio.tracks.Tracks
import com.prime.player.audio.tracks.TracksRoute
import com.prime.player.audio.tracks.TracksViewModel
import com.prime.player.common.compose.*
import com.prime.player.settings.MainGraphRoutes
import com.prime.player.settings.Settings
import com.prime.player.settings.SettingsViewModel
import com.primex.core.rememberState
import cz.levinzonr.saferoute.core.ProvideRouteSpecArgs
import cz.levinzonr.saferoute.core.RouteSpec
import cz.levinzonr.saferoute.core.navigateTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun Home() {

    // Currently; supports only 1 Part
    // add others in future
    // including support for more tools, like direction, prime factorization etc.
    // also support for navGraph.
    val controller = rememberAnimatedNavController()

    // channel/Messenger to handle events using SnackBar
    // A message channel
    val channel = remember { SnackDataChannel() }

    // The state of the Snackbar
    val snackbar = remember { SnackbarHostState() }

    // observe channel and emit snackbars
    val context = LocalContext.current
    LaunchedEffect(key1 = channel) {
        channel.receiveAsFlow().collect {
            // dismantle the given snack and use the corresponding components
            with(it.dismantle(context)) {
                val result = snackbar.showSnackbar(
                    message = message,
                    actionLabel = label.ifBlank { context.getString(R.string.dismiss) },
                    duration = duration
                )

                // action based on
                when (result) {
                    SnackbarResult.ActionPerformed -> action?.invoke()
                    SnackbarResult.Dismissed -> {
                        //do nothing
                    }
                }
            }
        }
    }

    //Handle messages etc.
    val state = rememberBottomSheetScaffoldState(snackbarHostState = snackbar)
    val scope = rememberCoroutineScope()
    val consoleViewModel = hiltViewModel<ConsoleViewModel>()

    /**
     * The sheet of the [BottoSheetScaffold]
     */
    val sheetContent = @Composable {
        Console(viewModel = consoleViewModel, expanded = state.bottomSheetState.isExpanded) {
            scope.launch {
                if (state.bottomSheetState.isExpanded) {
                    state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Collapsed)
                } else {
                    state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Expanded)
                }
            }
        }
    }

    var windowPadding by rememberState(initial = PaddingValues(0.dp))
    val hide = true
    CompositionLocalProvider(
        LocalWindowPadding provides windowPadding,
        LocalNavController provides controller,
        LocalSnackDataChannel provides channel,
    ) {
        //Bottom sheet
        BottomSheetScaffold(
            backgroundColor = Material.colors.background,
            scaffoldState = state,
            sheetElevation = 0.dp,
            sheetGesturesEnabled = false,
            sheetBackgroundColor = androidx.compose.ui.graphics.Color.Transparent,
            sheetPeekHeight = if (!hide) Tokens.MINI_PLAYER_HEIGHT else 0.dp,
            sheetContent = { sheetContent() },
        ) { inner ->
            // update window padding when ever it changes.
            windowPadding = inner
            Box(Modifier.navigationBarsPadding().fillMaxSize()){
                AnimatedNavHost(
                    navController = LocalNavController.current,
                    startDestination = MainGraphRoutes.Library.route,
                    modifier = Modifier,
                    enterTransition = { scaleIn(initialScale = 0.96f) + fadeIn(tween(700)) },
                    exitTransition = { scaleOut(targetScale = 0.96f) + fadeOut(tween(700)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(700)) },
                    popExitTransition = { fadeOut(animationSpec = tween(700)) }
                ) {
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
            }
        }
    }
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


private fun Snack.dismantle(context: Context) =
    object {
        val label: String
        val message: String
        val action: (() -> Unit)? = this@dismantle.action
        val duration: SnackbarDuration = this@dismantle.duration

        // init variables from the received snack
        init {
            when (this@dismantle) {
                is Snack.Resource -> {
                    label =
                        if (this@dismantle.label != ResourcesCompat.ID_NULL) context.getString(this@dismantle.label) else ""
                    message =
                        if (this@dismantle.message != ResourcesCompat.ID_NULL) context.getString(
                            this@dismantle.message
                        ) else ""
                }
                is Snack.Text -> {
                    label = this@dismantle.label
                    message = this@dismantle.message
                }
            }
        }
    }