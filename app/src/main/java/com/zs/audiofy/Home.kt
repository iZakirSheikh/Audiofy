/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.audiofy

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides as WIS
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.zs.audiofy.about.AboutUs
import com.zs.audiofy.about.RouteAboutUs
import com.zs.audiofy.audios.Audios
import com.zs.audiofy.audios.RouteAudios
import com.zs.audiofy.audios.directory.Albums
import com.zs.audiofy.audios.directory.Artists
import com.zs.audiofy.audios.directory.Genres
import com.zs.audiofy.audios.directory.RouteAlbums
import com.zs.audiofy.audios.directory.RouteArtists
import com.zs.audiofy.audios.directory.RouteGenres
import com.zs.audiofy.common.ColorizationStrategy
import com.zs.audiofy.common.NightMode
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.SystemFacade
import com.zs.audiofy.common.WindowStyle
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.composable
import com.zs.audiofy.common.compose.preference
import com.zs.audiofy.common.compose.rememberAcrylicSurface
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.common.compose.source
import com.zs.audiofy.common.domain
import com.zs.audiofy.common.impl.AlbumsViewModel
import com.zs.audiofy.common.impl.ArtistsViewModel
import com.zs.audiofy.common.impl.AudioFxViewModel
import com.zs.audiofy.common.impl.AudiosViewModel
import com.zs.audiofy.common.impl.ConsoleViewModel
import com.zs.audiofy.common.impl.EditorViewModel
import com.zs.audiofy.common.impl.FoldersViewModel
import com.zs.audiofy.common.impl.GenresViewModel
import com.zs.audiofy.common.impl.LibraryViewModel
import com.zs.audiofy.common.impl.MembersViewModel
import com.zs.audiofy.common.impl.PlaylistsViewModel
import com.zs.audiofy.common.impl.PropertiesViewModel
import com.zs.audiofy.common.impl.SettingsViewModel
import com.zs.audiofy.common.impl.VideosViewModel
import com.zs.audiofy.common.shapes.EndConcaveShape
import com.zs.audiofy.console.Console
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.audiofy.editor.Editor
import com.zs.audiofy.editor.RouteEditor
import com.zs.audiofy.effects.AudioFx
import com.zs.audiofy.effects.RouteAudioFx
import com.zs.audiofy.folders.Folders
import com.zs.audiofy.folders.RouteFolders
import com.zs.audiofy.library.Library
import com.zs.audiofy.library.RouteLibrary
import com.zs.audiofy.playlists.Playlists
import com.zs.audiofy.playlists.RoutePlaylists
import com.zs.audiofy.playlists.members.Members
import com.zs.audiofy.playlists.members.RouteMembers
import com.zs.audiofy.properties.Properties
import com.zs.audiofy.properties.RouteProperties
import com.zs.audiofy.settings.RouteSettings
import com.zs.audiofy.settings.Settings
import com.zs.audiofy.videos.RouteVideos
import com.zs.audiofy.videos.Videos
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.textResource
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.OutlinedButton
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.adaptive.FabPosition
import com.zs.compose.theme.adaptive.NavigationSuiteScaffold
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.appbar.FloatingBottomNavigationBar
import com.zs.compose.theme.appbar.NavigationItem
import com.zs.compose.theme.appbar.NavigationItemDefaults
import com.zs.compose.theme.appbar.SideBar
import com.zs.compose.theme.calculateWindowSizeClass
import com.zs.compose.theme.dynamicAccentColor
import com.zs.compose.theme.renderInSharedTransitionScopeOverlay
import com.zs.compose.theme.snackbar.SnackbarHostState
import com.zs.compose.theme.text.Label
import com.zs.core.common.checkSelfPermissions
import com.zs.core.common.isAppearanceLightSystemBars
import org.koin.androidx.compose.koinViewModel
import com.google.accompanist.permissions.rememberMultiplePermissionsState as Permissions

private const val TAG = "Home"

private val SIDE_BAR_WIDTH = 100.dp

/**
 * Determines the primary navigation route domain from the current [NavController] back stack.
 *
 * This extension property observes the current back stack entry and identifies the domain of
 * the top-level destination if it's one of the known primary routes (Library, Audios, Videos,
 * Playlists) and has no arguments.
 *
 * The result is memoized using [remember] and updated efficiently with [derivedStateOf]
 * whenever the back stack changes.
 *
 * @return A [State] holding the domain string of the primary route, or `null` if none is active.
 */
private val NavController.primary: State<String?>
    @Composable
    inline get() {
        // Observe the current back stack entry as state
        val entry by currentBackStackEntryAsState()
        return remember {
            derivedStateOf {
                // Determine if the current destination is a primary route without arguments.
                // If the current destination is a DialogNavigator, the primary route check
                // needs to be performed on the previous destination. This is because dialogs
                // are typically floating overlays and shouldn't affect the visibility of the
                // navigation bar, even though the underlying destination changes.
                val curr = entry?.destination
                val dest =
                    if (curr is DialogNavigator.Destination) previousBackStackEntry?.destination else curr
                // if primary is null- there is none.
                if (dest == null) {
                    Log.d(TAG, "entry: ${curr?.domain}")
                    return@derivedStateOf null
                }

                // Check if the destination is one of the known top-level domains
                val isPrimary = when (dest.domain) {
                    RouteLibrary.domain, RouteAudios.domain, RouteVideos.domain, RoutePlaylists.domain -> true
                    else -> false
                }

                // Determines if the current route is a primary, top-level screen without arguments.
                //
                // TODO: Investigate why arguments are sometimes present in `dest.arguments`
                //       and other times in `entry?.arguments`.
                //
                // The logic is as follows:
                // 1. Check if the current route's domain is one of the primary routes.
                // 2. Verify that there are no arguments in `dest.arguments`.
                // 3. Additionally, ensure that `entry?.arguments` is either null or contains only one entry
                //    (which might be a default or system-added argument).
                // If all conditions are met, the domain of the primary route is returned.
                // Otherwise, `null` is returned, indicating it's not a primary, argument-less route.
                Log.d(TAG, "args: ${dest.arguments} | ${entry?.arguments?.size()}")
                //if (isPrimary &&( dest.arguments.isEmpty() && (entry?.arguments == null || entry?.arguments?.size() == 1))) dest.domain else null
                val args = entry?.arguments
                val noRealArgs = args == null || args.isEmpty || args.keySet().all { key ->
                    val value = args.get(key)
                    Log.d(TAG, "$value: $key")
                    key.startsWith("android-support-nav:controller") || value == null || value == "{$key}"
                }
                if (isPrimary && noRealArgs) dest.domain else null
            }
        }
    }

private val NavIconSizeModifier = Modifier.size(20.dp)
private val NavRailShape = EndConcaveShape(12.dp)
private val NavRailBorder = BorderStroke(
    0.5.dp,
    Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            Color.Transparent,
            Color.Gray.copy(0.20f),
            Color.Transparent,
        )
    )
)

/**
 * Provides a [Density] object that reflects the user's preferred font scale.
 *
 * This extension function on [Preferences] observes the `KEY_FONT_SCALE` preference
 * and returns a modified [Density] object if the user has set a custom font scale.
 * If the font scale is set to -1 (default), the current [LocalDensity] is returned.
 *
 * @return A [Density] object with the appropriate font scale applied.
 */
private val SystemFacade.density: Density
    @NonRestartableComposable
    @Composable
    inline get() {
        // Observe font scale preference and create a modified Density if necessary
        val fontScale by observeAsState(key = Settings.FONT_SCALE)
        val density = LocalDensity.current
        return if (fontScale == -1f) density else Density(density.density, fontScale)
    }

/**
 *Navigates to the specified route, managing the back stack for a seamless experience.
 * Pops up to the start destination and uses launchSingleTop to prevent duplicate destinations.
 *
 * @param route The destination route.
 */
private fun NavController.toRoute(route: Route) {
    navigate(route()) {
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
 * List of permissions required to run the app.
 *
 * This list is constructed based on the device's Android version to ensure
 * compatibility with scoped storage and legacy storage access.
 */
@SuppressLint("BuildListAdds")
private val REQUIRED_PERMISSIONS = buildList {
    // For Android Tiramisu (33) and above, use media permissions for scoped storage
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this += android.Manifest.permission.ACCESS_MEDIA_LOCATION
        this += android.Manifest.permission.READ_MEDIA_VIDEO
        this += android.Manifest.permission.READ_MEDIA_AUDIO
    }
    // For Android Upside Down Cake (34) and above, add permission for user-selected visual media
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        this += android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    // For Android versions below Tiramisu 10(29), request WRITE_EXTERNAL_STORAGE for
    // legacy storage access
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
        this += android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        this += android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private object RoutePermission : Route

/**
 * Represents the permission screen
 * @see REQUIRED_PERMISSIONS
 * @see RoutePermission
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Permission() {
    val controller = LocalNavController.current
    // Compose the permission state.
    // Once granted set different route like folders as start.
    // Navigate from here to there.
    val permission =
        Permissions(permissions = REQUIRED_PERMISSIONS) {
            if (!it.all { (_, state) -> state }) return@Permissions
            controller.graph.setStartDestination(RouteLibrary())
            controller.navigate(RouteLibrary()) {
                popUpTo(RoutePermission()) {
                    inclusive = true
                }
            }
        }
    // If the permissions are not granted, show the permission screen.
    com.zs.audiofy.common.compose.Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.scr_permission_title),
        message = textResource(R.string.scr_permission_desc),
        vertical = LocalWindowSize.current.width == Category.Small,
        action = {
            OutlinedButton(
                onClick = permission::launchMultiplePermissionRequest,
                modifier = Modifier.size(width = 200.dp, height = 46.dp),
                text = stringResource(R.string.allow),
                shape = CircleShape
            )
        }
    )
}

/** The navigation graph for the app. */
private val navGraphBuilder: NavGraphBuilder.() -> Unit = {
    // Permission
    composable(RoutePermission) {
        Permission()
    }
    // Library
    composable(RouteLibrary) {
        val viewModel = koinViewModel<LibraryViewModel>()
        Library(viewModel)
    }
    // Settings
    composable(RouteSettings) {
        val viewModel = koinViewModel<SettingsViewModel>()
        Settings(viewModel)
    }

    // Audios
    composable(RouteAudios) {
        val viewState = koinViewModel<AudiosViewModel>()
        Audios(viewState)
    }

    // Albums
    composable(RouteAlbums) {
        val viewState = koinViewModel<AlbumsViewModel>()
        Albums(viewState)
    }

    // Artists
    composable(RouteArtists) {
        val viewState = koinViewModel<ArtistsViewModel>()
        Artists(viewState)
    }

    // Genres
    composable(RouteGenres) {
        val viewState = koinViewModel<GenresViewModel>()
        Genres(viewState)
    }

    // Folders
    composable(RouteFolders) {
        val viewState = koinViewModel<FoldersViewModel>()
        Folders(viewState)
    }

    // Playlists
    composable(RoutePlaylists) {
        val viewModel = koinViewModel<PlaylistsViewModel>()
        Playlists(viewModel)
    }

    // Videos
    composable(RouteVideos) {
        val viewState = koinViewModel<VideosViewModel>()
        Videos(viewState)
    }
    // Members
    composable(RouteMembers) {
        val viewState = koinViewModel<MembersViewModel>()
        Members(viewState)
    }
    // Properties
    dialog(
        RouteProperties.route,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val viewState = koinViewModel<PropertiesViewModel>()
        Properties(viewState)
    }
    // Console
    composable(RouteConsole) {
        val viewState = koinViewModel<ConsoleViewModel>()
        Console(viewState)
    }
    // AudioFx
    dialog(
        RouteAudioFx.route,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ){
        val viewState = koinViewModel<AudioFxViewModel>()
        AudioFx(viewState)
    }

    // TagEditor
    composable(RouteEditor){
        val viewState = koinViewModel<EditorViewModel>()
        Editor(viewState)
    }

    // AboutUs
    composable(RouteAboutUs){
        AboutUs()
    }
}

/**
 * A composable function that represents a navigation bar, combining both rail and bottom bar elements.
 *
 * @param isBottomAligned Specifies whether the navigation bar includes a [NavigationRail] or [BottomNavigation] component.
 * @param navController The NavController to manage navigation within the navigation bar.
 * @param modifier The modifier for styling and layout customization of the navigation bar.
 */
@Composable
@NonRestartableComposable
private fun NavigationBar(
    isBottomAligned: Boolean,
    background: Background,
    contentColor: Color,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    // Get the current theme colors
    val colors = AppTheme.colors
    val routes = @Composable {
        val colors = NavigationItemDefaults.colors(
            selectedIndicatorColor = if (contentColor == colors.onAccent) contentColor.copy(
                ContentAlpha.indication
            ) else colors.accent,
            selectedTextColor = if (contentColor == colors.onAccent) contentColor else colors.accent,
            unselectedIconColor = if (contentColor == colors.onAccent) colors.onAccent else colors.onBackground,
            unselectedTextColor = if (contentColor == colors.onAccent) colors.onAccent else colors.onBackground
        )
        val domain by navController.primary

        // Required to launch review.
        val facade = LocalSystemFacade.current
        // Library
        NavigationItem(
            label = { Label(text = textResource(R.string.home)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Weekend,
                    contentDescription = null,
                    modifier = NavIconSizeModifier
                )
            },
            selected = domain == RouteLibrary.domain,
            onClick = { facade.initiateReviewFlow(); navController.toRoute(RouteLibrary) },
            isBottomNav = isBottomAligned,
            colors = colors
        )

        // Audios
        NavigationItem(
            label = { Label(text = textResource(R.string.audios)) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Headset,
                    contentDescription = null,
                    modifier = NavIconSizeModifier
                )
            },
            selected = domain == RouteAudios.domain,
            onClick = { facade.initiateReviewFlow(); navController.toRoute(RouteAudios) },
            isBottomNav = isBottomAligned,
            colors = colors
        )

        // Videos
        NavigationItem(
            label = { Label(text = textResource(R.string.videos)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Subscriptions,
                    contentDescription = null,
                    modifier = NavIconSizeModifier
                )
            },
            selected = domain == RouteVideos.domain,
            onClick = { facade.initiateReviewFlow(); navController.toRoute(RouteVideos) },
            isBottomNav = isBottomAligned,
            colors = colors
        )

        // Playlists
        NavigationItem(
            label = { Label(text = textResource(R.string.playlists)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.FeaturedPlayList,
                    contentDescription = null,
                    modifier = NavIconSizeModifier
                )
            },
            selected = domain == RoutePlaylists.domain,
            onClick = { facade.initiateReviewFlow(); navController.toRoute(RoutePlaylists) },
            isBottomNav = isBottomAligned,
            colors = colors
        )
    }
    // Load appropriate navigation bar.
    when {
        isBottomAligned -> FloatingBottomNavigationBar(
            contentColor = contentColor,
            background = background,
            elevation = 12.dp,
            border = colors.shine,
            shape = CircleShape,
            modifier = modifier.padding(bottom = ContentPadding.small),
            // Display routes at the contre of available space
            content = { routes() }
        )

        else -> SideBar(
            modifier = modifier.width(SIDE_BAR_WIDTH),
            windowInsets = AppBarDefaults.sideBarWindowInsets,
            contentColor = contentColor,
            border = NavRailBorder,
            shape = NavRailShape,
            background = background,
            elevation = 0.dp,
            content = { routes() },
        )
    }
}

/** The main navigation host for the app. */
@Composable
fun Home(
    origin: Route,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController,
) {
    // dependencies
    val activity = LocalView.current.context as MainActivity
    val clazz = calculateWindowSizeClass(activity = activity)
    val primary by navController.primary

    // properties
    val style = (activity as SystemFacade).style
    val requiresNavBar = when (style.flagNavBarVisibility) {
        WindowStyle.FLAG_APP_NAV_BAR_HIDDEN -> false
        WindowStyle.FLAG_APP_NAV_BAR_VISIBLE -> true
        else -> primary != null // auto
    }
    // Determine the screen orientation.
    // This check assesses whether to display NavRail or BottomBar.
    // BottomBar appears only if the window size suits a mobile screen.
    // Consider this scenario: a large screen that fits the mobile description, like a desktop screen in portrait mode.
    // In this case, maybe showing the BottomBar is preferable!
    val portrait = clazz.width < Category.Medium
    val surface = rememberAcrylicSurface()

    // content
    val content = @Composable {
        NavigationSuiteScaffold(
            fabPosition = FabPosition.End,
            vertical = portrait,
            snackbarHostState = snackbarHostState,
            hideNavigationBar = !requiresNavBar,
            containerColor = AppTheme.colors.background,
            progress = activity.inAppUpdateProgress,
            floatingActionButton = {
                Widget(
                    surface = surface,
                    modifier = Modifier.thenIf(!requiresNavBar && portrait) {
                        windowInsetsPadding(WindowInsets.navigationBars.only(WIS.Bottom + WIS.End))
                    }
                )
            },
            // Set up the navigation bar using the NavBar composable
            navBar = {
                val useAccent by preference(Settings.USE_ACCENT_IN_NAV_BAR)
                val colors = AppTheme.colors
                NavigationBar(
                    portrait,
                    when {
                        useAccent -> Background(colors.accent)
                        !portrait -> Background(colors.background(2.dp))
                        else -> colors.background(surface)
                    },
                    if (useAccent) colors.onAccent else colors.onBackground,
                    navController,
                    Modifier.renderInSharedTransitionScopeOverlay(0.3f),
                )
            },
            // Display the main content of the app using the NavGraph composable
            content = {
                // Load start destination based on if storage permission is set or not.
                val granted = activity.checkSelfPermissions(REQUIRED_PERMISSIONS)
                NavHost(
                    navController = navController,
                    startDestination = if (origin != RouteConsole && !granted) RoutePermission() else origin(),
                    builder = navGraphBuilder,
                    modifier = Modifier.source(surface),
                    enterTransition = { scaleIn(tween(220, 90), 0.98f) + fadeIn(tween(700)) },
                    exitTransition = { fadeOut(tween(700)) },
                )
            }
        )
    }
    // Check if light theme is preferred
    val isDark = run {
        val mode by activity.observeAsState(key = Settings.NIGHT_MODE)
        when (mode) {
            NightMode.YES -> true
            NightMode.NO -> false
            NightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
    }

    // Setup App Theme and provide necessary dependencies.
    // Provide the navController and window size class to child composable.
    val strategy by activity.observeAsState(Settings.COLORIZATION_STRATEGY)
    AppTheme(
        isLight = !isDark,
        fontFamily = Settings.DefaultFontFamily,
        accent = when {
            strategy == ColorizationStrategy.Wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                dynamicAccentColor(activity, isDark)

            isDark -> Settings.DarkAccentColor
            else -> Settings.LightAccentColor
        },
        content = {
            // Provide the navController, newWindowClass through LocalComposition.
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSystemFacade provides (activity as SystemFacade),
                LocalDensity provides activity.density,
                LocalWindowSize provides when {
                    !requiresNavBar -> clazz
                    portrait -> clazz.consume(height = 56.dp)
                    else -> clazz.consume(SIDE_BAR_WIDTH)
                },
                content = content
            )
        }
    )

    // Observe the state of the IMMERSE_VIEW setting
    // Observe the state of the IMMERSE_VIEW setting
    val immersiveView by activity.observeAsState(Settings.IMMERSIVE_VIEW)
    val transparentSystemBars by activity.observeAsState(Settings.TRANSPARENT_SYSTEM_BARS)
    LaunchedEffect(immersiveView, style, isDark, transparentSystemBars) {
        // Get the WindowInsetsController for managing system bars
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        // Determine the visibility of system bars based on the current style settings
        val visible = when (style.flagSystemBarVisibility) {
            WindowStyle.FLAG_SYSTEM_BARS_HIDDEN -> false  // Hide system bars
            WindowStyle.FLAG_SYSTEM_BARS_VISIBLE -> true  // Show system bars
            else -> !immersiveView  // If not explicitly set, use the immersiveView setting
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