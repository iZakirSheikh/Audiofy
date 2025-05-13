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

package com.prime.media

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.prime.media.audios.RouteAudios
import com.prime.media.common.ColorizationStrategy
import com.prime.media.common.NightMode
import com.prime.media.common.Route
import com.prime.media.common.SystemFacade
import com.prime.media.common.WindowStyle
import com.prime.media.common.compose.ContentPadding
import com.prime.media.common.compose.LocalNavController
import com.prime.media.common.compose.LocalSystemFacade
import com.prime.media.common.compose.background
import com.prime.media.common.compose.composable
import com.prime.media.common.compose.preference
import com.prime.media.common.compose.rememberAcrylicSurface
import com.prime.media.common.compose.source
import com.prime.media.common.domain
import com.prime.media.common.shapes.EndConcaveShape
import com.prime.media.console.RouteConsole
import com.prime.media.impl.LibraryViewModel
import com.prime.media.impl.SettingsViewModel
import com.prime.media.library.Library
import com.prime.media.library.RouteLibrary
import com.prime.media.playlists.RoutePlaylists
import com.prime.media.settings.RouteSettings
import com.prime.media.settings.Settings
import com.prime.media.videos.RouteVideos
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.ClaretViolet
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.OutlinedButton
import com.zs.compose.theme.WindowSize.Category
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
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient as hGradient
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient as vGradient
import com.google.accompanist.permissions.rememberMultiplePermissionsState as Permissions

private const val TAG = "Home"

private val NAV_RAIL_MIN_WIDTH = 106.dp
private val BOTTOM_NAV_MIN_HEIGHT = 56.dp

private val LightAccentColor = Color.ClaretViolet
private val DarkAccentColor = Color(0xFFD8A25E)

private val NavRailShape = EndConcaveShape(16.dp)

private val RailBorder = BorderStroke(
    0.5.dp,
    hGradient(
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
 * The set of domains that require the navigation bar to be shown.
 * For other domains, the navigation bar will be hidden.
 */
private val DOMAINS_REQUIRING_NAV_BAR =
    arrayOf(RouteLibrary.domain, RouteAudios.domain, RouteVideos.domain, RoutePlaylists.domain)

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
    com.prime.media.common.compose.Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.permission_screen_title),
        message = textResource(R.string.permission_screen_desc),
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
}


private val NavIconSize = Modifier.size(20.dp)

/**
 * A composable function that represents a navigation bar, combining both rail and bottom bar elements.
 *
 * @param isBottomNav Specifies whether the navigation bar includes a [NavigationRail] or [BottomNavigation] component.
 * @param navController The NavController to manage navigation within the navigation bar.
 * @param modifier The modifier for styling and layout customization of the navigation bar.
 */
@Composable
@NonRestartableComposable
private fun NavigationBar(
    isBottomNav: Boolean,
    background: Background,
    contentColor: Color,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    // Get the current theme colors
    val colors = AppTheme.colors
    val navItems = @Composable {
        // Get the current navigation destination from NavController
        val current by navController.currentBackStackEntryAsState()
        val facade = LocalSystemFacade.current
        val domain = current?.destination?.domain
        val colors = NavigationItemDefaults.colors(
            selectedIndicatorColor = if (contentColor == colors.onAccent) contentColor.copy(
                ContentAlpha.indication
            ) else colors.accent,
            selectedTextColor = if (contentColor == colors.onAccent) contentColor else colors.accent,
            unselectedIconColor = if (contentColor == colors.onAccent) colors.onAccent else colors.onBackground,
            unselectedTextColor = if (contentColor == colors.onAccent) colors.onAccent else colors.onBackground
        )

        // Library
        NavigationItem(
            label = { Label(text = textResource(R.string.home)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Weekend,
                    contentDescription = null,
                    modifier = NavIconSize
                )
            },
            selected = domain == RouteLibrary.domain,
            onClick = { facade.initiateReviewFlow(); navController.toRoute(RouteLibrary) },
            isBottomNav = isBottomNav,
            colors = colors
        )

        // Audios
        NavigationItem(
            label = { Label(text = textResource(R.string.audios)) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Headset,
                    contentDescription = null,
                    modifier = NavIconSize
                )
            },
            selected = domain == RouteAudios.domain,
            onClick = { facade.initiateReviewFlow(); /*navController.toRoute(RouteSettings)*/ },
            isBottomNav = isBottomNav,
            colors = colors
        )

        // Videos
        NavigationItem(
            label = { Label(text = textResource(R.string.videos)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Subscriptions,
                    contentDescription = null,
                    modifier = NavIconSize
                )
            },
            selected = domain == RouteVideos.domain,
            onClick = { facade.initiateReviewFlow(); /*navController.toRoute(RouteSettings)*/ },
            isBottomNav = isBottomNav,
            colors = colors
        )

        // Playlists
        NavigationItem(
            label = { Label(text = textResource(R.string.playlists)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.FeaturedPlayList,
                    contentDescription = null,
                    modifier = NavIconSize
                )
            },
            selected = domain == RoutePlaylists.domain,
            onClick = { facade.initiateReviewFlow(); /*navController.toRoute(RouteSettings)*/ },
            isBottomNav = isBottomNav,
            colors = colors
        )

    }
    // Load appropriate navigation bar.
    when {
        !isBottomNav -> SideBar(
            modifier = Modifier
                .then(modifier)
                .width(NAV_RAIL_MIN_WIDTH),
            windowInsets = AppBarDefaults.sideBarWindowInsets,
            contentColor = contentColor,
            border = RailBorder,
            shape = NavRailShape,
            background = background,
            elevation = 0.dp,
            content = {
                // Display routes at the top of the navRail.
                navItems()
            },
        )

        else -> FloatingBottomNavigationBar(
            contentColor = contentColor,
            background = background,
            elevation = 12.dp,
            border = BorderStroke(
                0.5.dp,
                vGradient(
                    listOf(
                        if (colors.isLight) colors.background else Color.Gray.copy(0.24f),
                        if (colors.isLight) colors.background.copy(0.3f) else Color.Gray.copy(0.075f),
                    )
                )
            ),
            shape = CircleShape,
            modifier = modifier.padding(bottom = ContentPadding.medium),
            // Display routes at the contre of available space
            content = { navItems() }
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
    val current by navController.currentBackStackEntryAsState()

    // properties
    val style = (activity as SystemFacade).style
    val isNavBarRequired = when (style.flagNavBarVisibility) {
        WindowStyle.FLAG_APP_NAV_BAR_HIDDEN -> false
        WindowStyle.FLAG_APP_NAV_BAR_VISIBLE -> true
        else -> current?.destination?.domain in DOMAINS_REQUIRING_NAV_BAR  // auto
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
            vertical = portrait,
            snackbarHostState = snackbarHostState,
            hideNavigationBar = !isNavBarRequired,
            containerColor = AppTheme.colors.background,
            progress = activity.inAppUpdateProgress,
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
            strategy == ColorizationStrategy.Wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicAccentColor(
                activity,
                isDark
            )

            isDark -> DarkAccentColor
            else -> LightAccentColor
        },
        content = {
            // Provide the navController, newWindowClass through LocalComposition.
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSystemFacade provides (activity as SystemFacade),
                LocalDensity provides activity.density,
                LocalWindowSize provides when {
                    !isNavBarRequired -> clazz
                    !portrait -> clazz.consume(BOTTOM_NAV_MIN_HEIGHT)
                    else -> clazz.consume(NAV_RAIL_MIN_WIDTH)
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