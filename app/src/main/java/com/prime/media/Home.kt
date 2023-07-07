@file:Suppress("AnimateAsStateLabel")

package com.prime.media

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.prime.media.console.Console
import com.prime.media.console.ConsoleViewModel
import com.prime.media.core.NightMode
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowSizeClass
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.Scaffold2
import com.prime.media.core.compose.SheetValue
import com.prime.media.core.compose.preference
import com.prime.media.core.compose.rememberScaffoldState2
import com.prime.media.directory.playlists.Members
import com.prime.media.directory.playlists.MembersViewModel
import com.prime.media.directory.playlists.Playlists
import com.prime.media.directory.playlists.PlaylistsViewModel
import com.prime.media.directory.store.Albums
import com.prime.media.directory.store.AlbumsViewModel
import com.prime.media.directory.store.Artists
import com.prime.media.directory.store.ArtistsViewModel
import com.prime.media.directory.store.Audios
import com.prime.media.directory.store.AudiosViewModel
import com.prime.media.directory.store.Folders
import com.prime.media.directory.store.FoldersViewModel
import com.prime.media.directory.store.Genres
import com.prime.media.directory.store.GenresViewModel
import com.prime.media.impl.LibraryViewModel
import com.prime.media.impl.SettingsViewModel
import com.prime.media.library.Library
import com.prime.media.settings.Settings
import com.primex.core.MetroGreen
import com.primex.core.OrientRed
import com.primex.core.Rose
import com.primex.core.SignalWhite
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.blend
import com.primex.material2.OutlinedButton
import kotlinx.coroutines.launch

private const val TAG = "Home"

/**
 * A short-hand alias of [MaterialTheme]
 */
typealias Material = MaterialTheme

/**
 * A simple composable that helps in resolving the current app theme as suggested by the [Gallery.NIGHT_MODE]
 */
@Composable
@NonRestartableComposable
private fun isPrefDarkTheme(): Boolean {
    val mode by preference(key = Settings.NIGHT_MODE)
    return when (mode) {
        NightMode.YES -> true
        NightMode.FOLLOW_SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> false
    }
}

// Default Enter/Exit Transitions.
@OptIn(ExperimentalAnimationApi::class)
private val EnterTransition =
    scaleIn(tween(220, 90), 0.98f) + fadeIn(tween(700))
private val ExitTransition = fadeOut(tween(700))

/**
 * The route to permission screen.
 */
private const val PERMISSION_ROUTE = "_route_storage_permission"

/**
 * The permission screen.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Permission() {
    val controller = LocalNavController.current
    // Compose the permission state.
    // Once granted set different route like folders as start.
    // Navigate from here to there.
    val permission = rememberPermissionState(permission = Audiofy.STORAGE_PERMISSION) {
        if (!it) return@rememberPermissionState
        controller.graph.setStartDestination(Library.route)
        controller.navigate(Library.route) { popUpTo(PERMISSION_ROUTE) { inclusive = true } }
    }
    Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.storage_permission),
        message = stringResource(R.string.storage_permission_message),
    ) {
        OutlinedButton(
            onClick = { permission.launchPermissionRequest() },
            modifier = Modifier.size(width = 200.dp, height = 46.dp),
            elevation = null,
            label = "ALLOW",
            border = ButtonDefaults.outlinedBorder,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
        )
    }
}

private val defaultPrimaryColor = Color.MetroGreen
private val defaultSecondaryColor = Color.Rose

@Composable
private inline fun Material(
    darkTheme: Boolean = isPrefDarkTheme(),
    noinline content: @Composable () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF0E0E0F) else Color(0xFFF5F5FA),
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )
    val surface by animateColorAsState(
        targetValue = if (darkTheme) Color.TrafficBlack else Color.White,
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
        onSurface = if (darkTheme) Color.SignalWhite else Color.UmbraGrey,
        onBackground = if (darkTheme) Color.SignalWhite else Color.Black,
        error = Color.OrientRed,
        onSecondary = Color.SignalWhite,
        onError = Color.SignalWhite,
        isLight = !darkTheme
    )
    // Actual theme compose; in future handle fonts etc.
    MaterialTheme(colors = colors, content = content)
}

/**
 * A simple structure of the NavGraph.
 */
@OptIn(ExperimentalAnimationApi::class)
@NonRestartableComposable
@Composable
private fun NavGraph(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Load start destination based on if storage permission is set or not.
    val startDestination =
        when (ContextCompat.checkSelfPermission(context, Audiofy.STORAGE_PERMISSION)) {
            PackageManager.PERMISSION_GRANTED -> Library.route
            else -> PERMISSION_ROUTE
        }
    // In order to navigate and remove the need to pass controller below UI components.
    NavHost(
        navController = LocalNavController.current,
        modifier = modifier,
        startDestination = startDestination, //
        enterTransition = { EnterTransition },
        exitTransition = { ExitTransition },
    ) {
        //Permission
        composable(PERMISSION_ROUTE) {
            Permission()
        }
        composable(Library.route) {
            val viewModel = hiltViewModel<LibraryViewModel>()
            Library(viewModel = viewModel)
        }
        composable(Settings.route) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            Settings(state = viewModel)
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
}

@Composable
fun Home(channel: Channel) {
    val navController = rememberNavController()
    Material {
        // handle the color of navBars.
        val view = LocalView.current
        // Observe if the user wants to color the SystemBars
        val colorSystemBars by preference(key = Settings.COLOR_STATUS_BAR)
        val systemBarsColor =
            if (colorSystemBars) Material.colors.primary else Color.Transparent
        val hideStatusBar by preference(key = Settings.HIDE_STATUS_BAR)
        val darkTheme = !MaterialTheme.colors.isLight
        if (!view.isInEditMode)
            SideEffect {
                val window = (view.context as Activity).window
                window.navigationBarColor = systemBarsColor.toArgb()
                window.statusBarColor = systemBarsColor.toArgb()
                WindowCompat
                    .getInsetsController(window, view)
                    .isAppearanceLightStatusBars = !darkTheme && !colorSystemBars
                //
                if (hideStatusBar)
                    WindowCompat.getInsetsController(window, view)
                        .hide(WindowInsetsCompat.Type.statusBars())
                else
                    WindowCompat.getInsetsController(window, view)
                        .show(WindowInsetsCompat.Type.statusBars())
            }
        //Place the content.
        val vertical = LocalWindowSizeClass.current.widthSizeClass < WindowWidthSizeClass.Medium
        val facade = LocalSystemFacade.current
        val state = rememberScaffoldState2(initial = SheetValue.COLLAPSED)
        val scope = rememberCoroutineScope()
        // Collapse if expanded and back button is clicked.
        BackHandler(state.isExpanded) { scope.launch { state.collapse() } }
        // Create the viewModel
        val viewModel = hiltViewModel<ConsoleViewModel>()
        CompositionLocalProvider(LocalNavController provides navController) {
            Scaffold2(
                vertical = vertical,
                channel = channel,
                state = state,
                sheetPeekHeight = if (viewModel.isLoaded) Settings.MINI_PLAYER_HEIGHT else 0.dp,
                color = MaterialTheme.colors.background,
                progress = facade.inAppUpdateProgress,
                content = { NavGraph() },
                sheet = {
                    Console(viewModel, state.progress.value) {
                        scope.launch { state.toggle() }
                    }
                }
            )
        }
    }
}