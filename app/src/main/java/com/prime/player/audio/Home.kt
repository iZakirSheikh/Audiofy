package com.prime.player.audio


import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.console.Console
import com.prime.player.extended.LocalMessenger
import com.prime.player.extended.ProvideNavActions
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


/**
 * peek Height of [BottomSheetScaffold], also height of [MiniPlayer]
 */
val AUDIO_BOTTOM_SHEET_PEEK_HEIGHT = 70.dp

@ExperimentalAnimationApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Home(viewModel: HomeViewModel) {
    val controller: NavHostController = rememberAnimatedNavController()
    val actions = remember {
        AudioNavigationActions(controller)
    }

    // A message channel
    val channel = remember {
        Channel<Pair<List<String>, (() -> Unit)?>>(Channel.CONFLATED)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    //Handle messages etc.
    val state = rememberBottomSheetScaffoldState(snackbarHostState = snackbarHostState)
    val dismiss = stringResource(id = R.string.dismiss)
    LaunchedEffect(key1 = channel) {
        channel.receiveAsFlow().collect { (com, action) ->
            val label = com[0]
            val msg = com[1]
            val res = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (label.isBlank()) dismiss else label,
                duration = SnackbarDuration.Short
            )
            when (res) {
                SnackbarResult.ActionPerformed -> action?.invoke()
                SnackbarResult.Dismissed -> {
                    //do nothing
                }
            }
        }
    }

    ProvideNavActions(actions = actions) {
        with(viewModel) {
            val scope = rememberCoroutineScope()

            val connected by connected
            LaunchedEffect(key1 = connected) {
                //delay(1000)
                connect()
            }

            // hide bottom sheet when service is not initialized
            val hide = current.value == null

            val sheetContent = @Composable {
                Console(viewModel = viewModel) {
                    scope.launch {
                        if (state.bottomSheetState.isExpanded) {
                            state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Collapsed)
                            expanded.value = false
                        } else {
                            state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Expanded)
                            expanded.value = true
                        }
                    }
                }
            }

            BackHandler(state.bottomSheetState.isExpanded) {
                if (state.bottomSheetState.isExpanded)
                    scope.launch {
                        state.bottomSheetState.snapTo(targetValue = BottomSheetValue.Collapsed)
                        expanded.value = false
                    }
            }

            //Bottom sheet
            BottomSheetScaffold(
                backgroundColor = PlayerTheme.colors.background,
                scaffoldState = state,
                sheetElevation = 0.dp,
                sheetGesturesEnabled = false,
                sheetBackgroundColor = Color.Transparent,
                sheetPeekHeight = if (!hide) AUDIO_BOTTOM_SHEET_PEEK_HEIGHT else 0.dp,
                sheetContent = {
                    CompositionLocalProvider(LocalMessenger provides channel) {
                        sheetContent()
                    }
                }
            ) { inner ->
                val padding = remember { mutableStateOf(inner) }.also { it.value = inner }
                CompositionLocalProvider(LocalMessenger provides channel) {
                    NavGraph(padding = padding)
                }
            }
        }
    }
}