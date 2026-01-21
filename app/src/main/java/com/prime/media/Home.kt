@file:OptIn(ExperimentalLayoutApi::class)

package com.prime.media

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areNavigationBarsVisible
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.NavigationRail
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.prime.media.about.AboutUs
import com.prime.media.about.RouteAboutUs
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.NavItem
import com.prime.media.common.Regular
import com.prime.media.common.SystemFacade
import com.prime.media.common.collectNowPlayingAsState
import com.prime.media.common.composable
import com.prime.media.common.dynamicBackdrop
import com.prime.media.impl.AlbumsViewModel
import com.prime.media.impl.ArtistsViewModel
import com.prime.media.impl.FoldersViewModel
import com.prime.media.impl.GenresViewModel
import com.prime.media.impl.LibraryViewModel
import com.prime.media.impl.PlaylistViewModel
import com.prime.media.impl.PlaylistsViewModel
import com.prime.media.impl.SettingsViewModel
import com.prime.media.impl.VideosViewModel
import com.prime.media.library.RouteLibrary
import com.prime.media.local.Albums
import com.prime.media.local.Artists
import com.prime.media.local.Folders
import com.prime.media.local.Genres
import com.prime.media.local.RouteAlbums
import com.prime.media.local.RouteArtists
import com.prime.media.local.RouteFolders
import com.prime.media.local.RouteGenres
import com.prime.media.local.videos.RouteVideos
import com.prime.media.local.videos.Videos
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.common.util.getAlbumArt
import com.prime.media.old.console.Console
import com.prime.media.old.core.playback.artworkUri
import com.prime.media.old.directory.playlists.Members
import com.prime.media.old.directory.playlists.MembersViewModel
import com.prime.media.old.directory.store.Audios
import com.prime.media.old.directory.store.AudiosViewModel
import com.prime.media.old.editor.TagEditor
import com.prime.media.old.effects.AudioFx
import com.prime.media.old.impl.AudioFxViewModel
import com.prime.media.old.impl.ConsoleViewModel
import com.prime.media.old.impl.TagEditorViewModel
import com.prime.media.permission.Permission
import com.prime.media.permission.RoutePermission
import com.prime.media.personalize.Personalize
import com.prime.media.personalize.RoutePersonalize
import com.prime.media.playlists.Playlist
import com.prime.media.playlists.Playlists
import com.prime.media.playlists.RoutePlaylist
import com.prime.media.playlists.RoutePlaylists
import com.prime.media.common.AppConfig
import com.prime.media.common.ColorizationStrategy
import com.prime.media.settings.RouteSettings
import com.prime.media.common.Registry
import com.prime.media.console.RouteConsole2
import com.prime.media.impl.ConsoleViewModel2
import com.prime.media.widget.Glance
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.material2.Label
import com.zs.core.playback.PlaybackController
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.LocalNavAnimatedVisibilityScope
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.NightMode
import com.zs.core_ui.Range
import com.zs.core_ui.WallpaperAccentColor
import com.zs.core_ui.WindowSize
import com.zs.core_ui.WindowStyle
import com.zs.core_ui.adaptive.NavigationItemDefaults
import com.zs.core_ui.adaptive.NavigationSuiteScaffold
import com.zs.core_ui.calculateWindowSizeClass
import com.zs.core_ui.checkSelfPermissions
import com.zs.core_ui.dynamicAccentColor
import com.zs.core_ui.isAppearanceLightSystemBars
import com.zs.core_ui.renderInSharedTransitionScopeOverlay
import com.zs.core_ui.shape.EndConcaveShape
import com.zs.core_ui.shape.TopConcaveShape
import com.zs.core_ui.toast.ToastHostState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient as HorizontalGradient
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient as VerticalGradient
import androidx.navigation.compose.currentBackStackEntryAsState as current
import com.prime.media.common.rememberHazeState as backdropProvider
import dev.chrisbanes.haze.haze as backdropObserver

private const val TAG = "Home"

private val NAV_RAIL_MIN_WIDTH = 106.dp
private val BOTTOM_NAV_MIN_HEIGHT = 56.dp

/**
 * Provides a dynamic accent color based on the current theme (dark/light) and the user's
 * chosen colorization strategy. This composable reacts to changes in both the theme and
 * the currently playing media item, updating the accent color accordingly.
 *
 * @param isDark Indicates whether the current theme is dark.
 * @return A [State] object holding the calculated accent color, which will trigger
 * recomposition when the color changes.
 */
@SuppressLint("ProduceStateDoesNotAssignValue")
@Composable
private fun resolveAccentColor(
    isDark: Boolean
): State<Color> {
    // Default accent color based on the current theme
    val default = if (isDark) Registry.DarkAccentColor else Registry.LightAccentColor
    // Get the activity context for accessing resources and services
    val activity = LocalView.current.context as MainActivity
    // Get the colorization strategy preference
    val strategy by activity.observeAsState(Registry.COLORIZATION_STRATEGY)
    // Observe the accent color based on the colorization strategy
    if (strategy == ColorizationStrategy.Manual) {
        val state =
            activity.observeAsState(if (isDark) Registry.COLOR_ACCENT_DARK else Registry.COLOR_ACCENT_LIGHT)
        Log.d(TAG, "resolveAccentColor: ${state.value}")
        return state
    }
    return produceState(default, isDark, strategy) {
        var job: Job? = null
        when (strategy) {
            ColorizationStrategy.Wallpaper -> value =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    dynamicAccentColor(activity, isDark) else default
            // just return the default color
            ColorizationStrategy.Default -> value = default
            else -> {
                // observe current playing item for accent color
                val remote = activity.remote
                // launch a new job.
                job = remote.loaded
                    .map {
                        val current = remote.current
                        val uri = current?.artworkUri ?: return@map null
                        // Get the Bitmap Colors
                        activity.getAlbumArt(uri)
                    }
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.Main)
                    .onEach {
                        val accent = WallpaperAccentColor(it?.toBitmap(), isDark, default)
                        this@produceState.value = Color(accent)
                    }.catch {
                        Log.d(TAG, "observeAccentColor: ${it.message}")
                        this@produceState.value = default
                    }
                    .flowOn(Dispatchers.Default)
                    .launchIn(this)
            }
        }
        // Cancel the job when the coroutine is no longer active
        awaitDispose { job?.cancel() }
    }
}

/**
 *Navigates to the specified route, managing the back stack for a seamless experience.
 * Pops up to the start destination and uses launchSingleTop to prevent duplicate destinations.
 *
 * @param route The destination route.
 */
private fun NavController.toRoute(route: String) {
    navigate(route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when
        // re-selecting the same item
        launchSingleTop = true
        // Restore state when re-selecting a previously selected item
        restoreState = true
    }
}

/**
 *  The navigation graph for the app.
 */
private val navGraphBuilder: NavGraphBuilder.() -> Unit = {
    //AboutUs
    composable(RouteAboutUs) {
        AboutUs()
    }
    //Permission
    composable(RoutePermission) {
        Permission()
    }
    // Library
    composable(RouteLibrary) {
        val viewModel = koinViewModel<LibraryViewModel>()
        RouteLibrary(viewModel)
    }
    // Settings
    composable(RouteSettings) {
        val viewModel = koinViewModel<SettingsViewModel>()
        RouteSettings(viewModel)
    }
    // Albums
    composable(RouteAlbums) {
        val viewModel = koinViewModel<AlbumsViewModel>()
        Albums(viewState = viewModel)
    }
    // Artists
    composable(RouteArtists) {
        val viewModel = koinViewModel<ArtistsViewModel>()
        Artists(viewState = viewModel)
    }
    // Audios
    composable(Audios.route) {
        val viewModel = koinViewModel<AudiosViewModel>()
        Audios(viewModel = viewModel)
    }
    // Folders
    composable(RouteFolders) {
        val viewModel = koinViewModel<FoldersViewModel>()
        Folders(viewState = viewModel)
    }
    // Genres
    composable(RouteGenres) {
        val viewModel = koinViewModel<GenresViewModel>()
        Genres(viewModel)
    }
    // Playlists
    composable(RoutePlaylists) {
        val viewModel = koinViewModel<PlaylistsViewModel>()
        Playlists(viewModel)
    }
    // Members
    composable(RoutePlaylist) {
        val viewModel = koinViewModel<PlaylistViewModel>()
        Playlist(viewModel)
    }
    // Tag Editor
    composable(TagEditor.route) {
        val viewModel = koinViewModel<TagEditorViewModel>()
        TagEditor(state = viewModel)
    }
    // AudioFx
    dialog(AudioFx.route) {
        val viewModel = koinViewModel<AudioFxViewModel>()
        AudioFx(state = viewModel)
    }
    // Console
    composable(Console.route) {
        val viewModel = koinViewModel<ConsoleViewModel>()
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            Console(state = viewModel)
        }
    }
    // ControlCentre
    composable(RoutePersonalize) {
        val viewModel = koinViewModel<com.prime.media.impl.PersonalizeViewModel>()
        Personalize(viewModel)
    }
    // Members
    composable(MembersViewModel.route) {
        val viewModel = koinViewModel<MembersViewModel>()
        Members(viewModel)
    }
    // Videos
    composable(RouteVideos) {
        val viewModel = koinViewModel<VideosViewModel>()
        Videos(viewState = viewModel)
    }
    // New Console
    composable(RouteConsole2){
        val viewState = koinViewModel<ConsoleViewModel2>()
        RouteConsole2(viewState)
    }
}

private val BottomNavShape = TopConcaveShape(radius = 20.dp)
private val NavRailShape = EndConcaveShape(16.dp)

private val NoBottomPaddingBottomNavBar = PaddingValues(
    start = ContentPadding.normal,
    end = ContentPadding.normal,
    top = 24.dp
)
private val PaddingBottomNavBar = PaddingValues(
    start = ContentPadding.normal,
    end = ContentPadding.normal,
    top = 24.dp,
    bottom = ContentPadding.medium
)

/**
 * A composable function that represents a navigation bar, combining both rail and bottom bar elements.
 *
 * @param typeRail Specifies whether the navigation bar includes a [NavigationRail] or [BottomNavigation] component.
 * @param navController The NavController to manage navigation within the navigation bar.
 * @param modifier The modifier for styling and layout customization of the navigation bar.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun NavigationBar(
    spacer: Boolean,
    typeRail: Boolean,
    contentColor: Color,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val routes = @Composable {
        // Get the current navigation destination from NavController
        val current by navController.current()
        val color = LocalContentColor.current
        val colors = when {
            typeRail -> NavigationItemDefaults.navigationItemColors(
                selectedContentColor = color,
                selectedBackgroundColor = color.copy(0.12f)
            )

            contentColor == AppTheme.colors.onAccent -> NavigationItemDefaults.bottomNavItem2Colors(
                selectedIndicatorColor = color.copy(0.12f),
                selectedIconColor = color,
                unselectedIconColor = color,
                selectedTextColor = color,
                unselectedTextColor = color
            )

            else -> NavigationItemDefaults.bottomNavItem2Colors()
        }
        val route = current?.destination?.route
        val facade = LocalSystemFacade.current
        // Home
        NavItem(
            label = { Label(text = textResource(R.string.home)) },
            icon = { Icon(imageVector = Icons.Filled.Weekend, contentDescription = null) },
            checked = route == RouteLibrary(),
            onClick = { navController.toRoute(RouteLibrary()); facade.initiateReviewFlow() },
            typeRail = typeRail,
            colors = colors
        )
        // Folders
        NavItem(
            label = { Label(text = textResource(R.string.folders)) },
            icon = { Icon(imageVector = Icons.Filled.FolderCopy, contentDescription = null) },
            checked = route == RouteFolders(),
            onClick = { navController.toRoute(RouteFolders()); facade.initiateReviewFlow() },
            typeRail = typeRail,
            colors = colors
        )

        // Albums
        NavItem(
            label = { Label(text = textResource(R.string.videos)) },
            icon = { Icon(imageVector = Icons.Outlined.VideoLibrary, contentDescription = null) },
            checked = route == RouteVideos(),
            onClick = { navController.toRoute(RouteVideos()); facade.initiateReviewFlow() },
            typeRail = typeRail,
            colors = colors
        )

        // Playlists
        NavItem(
            label = { Label(text = textResource(R.string.playlists)) },
            icon = { Icon(imageVector = Icons.Outlined.PlaylistPlay, contentDescription = null) },
            checked = route == RoutePlaylists(),
            onClick = { navController.toRoute(RoutePlaylists()); facade.initiateReviewFlow() },
            typeRail = typeRail,
            colors = colors
        )
    }
    val colors = AppTheme.colors
    // Actual Layouts
    when {
        typeRail -> NavigationRail(
            modifier = Modifier
                .border(
                    0.5.dp,
                    HorizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Gray.copy(if (colors.isLight) 0.16f else 0.24f),
                            Color.Transparent,
                        )
                    ),
                    NavRailShape
                )
                .clip(NavRailShape)
                .then(modifier)
                .widthIn(NAV_RAIL_MIN_WIDTH),
            windowInsets = WindowInsets.statusBars,
            contentColor = contentColor,
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            content = {
                // Display routes at the top of the navRail.
                routes()
                // Some Space between naves and Icon.
                if (spacer)
                Spacer(modifier = Modifier.weight(1f))
            },
        )

        else -> BottomAppBar(
            windowInsets = WindowInsets.navigationBars,
            contentColor = contentColor,
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            contentPadding = if (WindowInsets.areNavigationBarsVisible) NoBottomPaddingBottomNavBar else PaddingBottomNavBar,
            modifier = Modifier
                .border(
                    0.5.dp,
                    VerticalGradient(
                        listOf(
                            if (colors.isLight) colors.background(2.dp) else Color.Gray.copy(0.24f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                        )
                    ),
                    BottomNavShape
                )
                .clip(BottomNavShape)
                .then(modifier)
                .heightIn(BOTTOM_NAV_MIN_HEIGHT),
            content = {
                // Ensures adequate spacing at the start of the BottomBar to accommodate pixel
                // composable.
                if (spacer)
                Spacer(modifier = Modifier.requiredSize(Glance.MIN_SIZE))
                Spacer(Modifier.weight(1f))
                // Display routes at the contre of available space
                routes()
                Spacer(modifier = Modifier.weight(1f))
            }
        )
    }
}

/**
 * The set of domains that require the navigation bar to be shown.
 * For other domains, the navigation bar will be hidden.
 */
private val DOMAINS_REQUIRING_NAV_BAR =
    arrayOf(/*RouteSettings(),*/ RouteFolders(), RouteVideos(), RoutePlaylists(), RouteLibrary())

/**
 * Adjusts the [WindowSize] by consuming either the navigation rail width or the bottom navigation height.
 *
 * @param rail Boolean indicating whether to consume the navigation rail width.
 * @return [WindowSize] with the specified dimension consumed.
 */
private fun WindowSize.consume(rail: Boolean) =
    if (rail) consume(width = NAV_RAIL_MIN_WIDTH) else consume(height = BOTTOM_NAV_MIN_HEIGHT)

/**
 * Represents the main entry to the UI
 */
@Composable
fun App(
    toastHostState: ToastHostState,
    navController: NavHostController
) {
    val activity = LocalView.current.context as MainActivity
    val clazz = calculateWindowSizeClass(activity = activity)
    val current by navController.current()
    // properties
    val style = (activity as SystemFacade).style
    val requiresNavBar = when (style.flagAppNavBar) {
        WindowStyle.FLAG_APP_NAV_BAR_HIDDEN -> false
        WindowStyle.FLAG_APP_NAV_BAR_VISIBLE -> true
        else -> current?.destination?.route in DOMAINS_REQUIRING_NAV_BAR   // Auto
    }

    // Determine the screen orientation.
    // This check assesses whether to display NavRail or BottomBar.
    // BottomBar appears only if the window size suits a mobile screen.
    // Consider this scenario: a large screen that fits the mobile description, like a desktop screen in portrait mode.
    // In this case, showing the BottomBar is preferable!
    val portrait = clazz.widthRange < Range.Medium
    // Create only backdrop provider for android 12 onwards
    val provider: HazeState? = when {
        AppConfig.isBackgroundBlurEnabled -> backdropProvider()
        else -> null
    }
    // The navHost
    val state by PlaybackController.collectNowPlayingAsState()
    val content = @Composable {
        NavigationSuiteScaffold(
            vertical = portrait,
            toastHostState = toastHostState,
            hideNavigationBar = !requiresNavBar,
            background = AppTheme.colors.background,
            progress = activity.inAppTaskProgress,
            widget = {
                if (state.hasActiveMedia && requiresNavBar) Glance(state) else Spacer(Modifier)
            },
            // Set up the navigation bar using the NavBar composable
            navBar = {
                val colors = AppTheme.colors
                val useAccent by (activity as SystemFacade).observeAsState(Registry.USE_ACCENT_IN_NAV_BAR)
                NavigationBar(
                    state.hasActiveMedia,
                    !portrait,
                    if (useAccent) colors.onAccent else colors.onBackground,
                    navController,
                    when {
                        useAccent -> Modifier.background(colors.accent)
                        else -> Modifier.dynamicBackdrop(
                            if (!portrait) null else provider,
                            HazeStyle.Regular(
                                colors.background(0.4.dp),
                                //if (colors.isLight) 0.24f else 0.63f
                            ),
                            colors.background,
                            colors.accent
                        )
                    }.renderInSharedTransitionScopeOverlay(0.2f),
                )
            },
            // Display the main content of the app using the NavGraph composable
            content = {
                // Load start destination based on if storage permission is set or not.
                val hasStorageAccess = activity.checkSelfPermissions(Registry.REQUIRED_PERMISSIONS)
                NavHost(
                    navController = navController,
                    startDestination = if (!hasStorageAccess) RoutePermission() else RouteLibrary(),
                    builder = navGraphBuilder,
                    modifier = Modifier.thenIf(provider != null) { backdropObserver(provider!!) }
                )
            },
        )
    }
    // Observe the theme changes
    // and update content accordingly.
    val isDark = run {
        val mode by activity.observeAsState(key = Registry.NIGHT_MODE)
        when (mode) {
            NightMode.YES -> true
            NightMode.NO -> false
            NightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
    }

    val accent by resolveAccentColor(isDark)
    // Setup App Theme and provide necessary dependencies.
    // Provide the navController and window size class to child composable.
    AppTheme(
        isLight = !isDark,
        fontFamily = Registry.DefaultFontFamily,
        accent = accent,
        content = {
            // Provide the navController, newWindowClass through LocalComposition.
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalElevationOverlay provides null,  // Disable absolute elevation.
                LocalSystemFacade provides (activity as SystemFacade),
                LocalWindowSize provides if (!requiresNavBar) clazz else clazz.consume(!portrait),
                content = content
            )
        }
    )

    // Observe the state of the IMMERSE_VIEW setting
    val transparentSystemBars by activity.observeAsState(Registry.TRANSPARENT_SYSTEM_BARS)
    LaunchedEffect( style, isDark, transparentSystemBars) {
        // Get the WindowInsetsController for managing system bars
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        // Determine the visibility of system bars based on the current style settings
        val visible = when (style.flagSystemBarVisibility) {
            WindowStyle.FLAG_SYSTEM_BARS_HIDDEN -> false  // Hide system bars
            WindowStyle.FLAG_SYSTEM_BARS_VISIBLE -> true  // Show system bars
            else -> true  // If not explicitly set, use the immersiveView setting
        }
        // Apply the visibility setting to the system bars
        if (!visible) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
        // Determine the appearance of system bars (dark or light) based on the current style settings
        controller.isAppearanceLightSystemBars = when (style.flagSystemBarAppearance) {
            WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_DARK -> false  // Use dark system bars appearance
            WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_LIGHT -> true  // Use light system bars appearance
            else -> !isDark  // If not explicitly set, use the isDark setting
        }
        // Configure the system bars background color based on the current style settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
            return@LaunchedEffect // No supported from here.
        window.apply {
            val color = when (style.flagSystemBarBackground) {
                WindowStyle.FLAG_SYSTEM_BARS_BG_TRANSLUCENT -> Color(0x20000000).toArgb()  // Translucent background
                WindowStyle.FLAG_SYSTEM_BARS_BG_TRANSPARENT -> Color.Transparent.toArgb()  // Transparent background
                else -> (if (!transparentSystemBars) Color(0x20000000) else Color.Transparent).toArgb()// automate using the setting
            }
            // Set the status and navigation bar colors
            statusBarColor = color
            navigationBarColor = color
        }
    }
}