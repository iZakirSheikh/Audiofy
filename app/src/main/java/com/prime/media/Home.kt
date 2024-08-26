@file:Suppress("AnimateAsStateLabel")
@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.prime.media

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.SelectableChipColors
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.prime.media.about.AboutUs
import com.prime.media.config.Personalize
import com.prime.media.config.RoutePersonalize
import com.prime.media.console.Console
import com.prime.media.core.Anim
import com.prime.media.core.ContentPadding
import com.prime.media.core.NightMode
import com.prime.media.core.composable
import com.prime.media.core.compose.BottomNavItem
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.LocalAnimatedVisibilityScope
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSharedTransitionScope
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowSize
import com.prime.media.core.compose.NavRailItem
import com.prime.media.core.compose.NavigationItemDefaults
import com.prime.media.core.compose.NavigationSuiteScaffold
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.Range
import com.prime.media.core.compose.WallpaperAccentColor
import com.prime.media.core.compose.WindowSize
import com.prime.media.core.compose.current
import com.prime.media.core.compose.preference
import com.prime.media.core.playback.MediaItem
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.util.getAlbumArt
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
import com.prime.media.editor.TagEditor
import com.prime.media.effects.AudioFx
import com.prime.media.feedback.Feedback
import com.prime.media.feedback.RouteFeedback
import com.prime.media.impl.AudioFxViewModel
import com.prime.media.impl.ConsoleViewModel
import com.prime.media.impl.FeedbackViewModel
import com.prime.media.impl.LibraryViewModel
import com.prime.media.impl.PersonalizeViewModel
import com.prime.media.impl.SettingsViewModel
import com.prime.media.impl.TagEditorViewModel
import com.prime.media.library.Library
import com.prime.media.settings.ColorizationStrategy
import com.prime.media.settings.Settings
import com.prime.media.widget.Glance
import com.primex.core.Amber
import com.primex.core.AzureBlue
import com.primex.core.MetroGreen
import com.primex.core.MetroGreen2
import com.primex.core.OrientRed
import com.primex.core.SignalWhite
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.hsl
import com.primex.core.textResource
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.ln

private const val TAG = "Home"

/**
 * A short-hand alias of [MaterialTheme]
 */
typealias Material = MaterialTheme

/**
 * A variant of caption.
 */
private val caption2 =
    TextStyle(fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.4.sp)

/**
 * Calculates and returns the lightness component of this color in the HSL color space.
 * The lightness value is represented as a float in the range [0.0, 1.0], where 0.0 is the darkest
 * and 1.0 is the lightest.
 */
private val Color.lightness: Float
    get() {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(toArgb(), hsl)
        return hsl[2]
    }

/**
 * A variant of [caption] with a smaller font size and tighter letter spacing.
 * Use this style for captions that require less emphasis or in situations where space is constrained.
 *
 * @see caption
 */
val Typography.caption2 get() = com.prime.media.caption2

/**
 * The alpha value for the container colors.
 *
 * This constant value represents the alpha (transparency) of the container colors in the current
 * [MaterialTheme]. The value is a Float between 0.0 and 1.0, where 0.0 is completely transparent
 * and 1.0 is completely opaque. This value can be used to adjust the transparency of container
 * backgrounds and other elements in your app that use the container color.
 */
@Deprecated("The reason for deprivation is that it is cumbersome to use.")
val MaterialTheme.CONTAINER_COLOR_ALPHA get() = 0.15f

/**
 * A variant of [MaterialTheme.shapes.small] with a corner radius of 8dp.
 */
private val small2 = RoundedCornerShape(8.dp)

/**
 * A variant of [MaterialTheme.shapes.small] with a radius of 8dp.
 */
val Shapes.small2 get() = com.prime.media.small2

/**
 * The overlay color used for backgrounds and shadows.
 * The color is black with alpha 0.04 on light themes and white with alpha 0.04 on dark themes.
 */
val Colors.overlay
    @Composable inline get() = if (isLight) Color.Black.copy(0.04f) else Color.White.copy(0.01f)

/**
 * The outline color used in the light/dark theme.
 *
 * The color is semi-transparent white/black, depending on the current theme, with an alpha of 0.12.
 */
inline val Colors.outline
    get() = (if (isLight) Color.Black else Color.White).copy(0.12f)

val Colors.lightShadowColor
    @Composable inline get() = if (isLight) Color.White else Color.White.copy(0.025f)
val Colors.darkShadowColor
    @Composable inline get() = if (isLight) Color(0xFFAEAEC0).copy(0.7f) else Color.Black.copy(0.6f)

/**
 * Calculates a tonal elevation color by overlaying a semi-transparent version of the accent
 * color over the background color. The transparency of the accent color is determined by
 * the given elevation value.
 *
 * @param accent The accent color to use for tonal elevation.
 * @param background The background color to apply tonal elevation to.
 * @param elevation The elevation value in Dp to determine the transparency of the accent color.
 * @return The calculated tonal elevation color.
 */
private fun applyTonalElevation(accent: Color, background: Color, elevation: Dp) =
    accent.copy(alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f).compositeOver(background)

/**
 * @see applyTonalElevation
 */
fun Colors.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    return applyTonalElevation(primary, surface, if (isLight) elevation else 0.5f * elevation)
}

/**
 * @see applyTonalElevation
 */
fun Colors.backgroundColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return background
    return applyTonalElevation(primary, background, if (isLight) elevation else 0.5f * elevation)
}

/**
 * Returns true if the system bars are required to be light-themed, false otherwise.
 * @see WindowInsetsControllerCompat.isAppearanceLightStatusBars
 */
inline val Colors.isAppearanceLightSystemBars
    @Composable inline get() = isLight

/**
 * Observes whether the app is in light mode based on the user's preference and system settings.
 *
 * @return `true` if the app is in light mode, `false` otherwise.
 */
@Composable
@NonRestartableComposable
private fun isPreferenceDarkTheme(): Boolean {
    val mode by preference(key = Settings.NIGHT_MODE)
    return when (mode) {
        NightMode.YES -> true
        NightMode.FOLLOW_SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> false
    }
}

/**
 * return the navigation type based on the window size.
 */
private inline val WindowSize.navTypeRail get() = widthRange > Range.Medium

private val NAV_RAIL_WIDTH = 96.dp

/**
 * Calculates an returns newWindowSizeClass after consuming sapce occupied by  [navType].
 *
 * @return consumed window class.
 * @see [navType]
 */
private inline val WindowSize.remaining
    get() = when {
        !navTypeRail -> consume(height = 56.dp)
        else -> consume(width = NAV_RAIL_WIDTH)
    }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    typeRail: Boolean = false,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors(),
) {
    when (typeRail) {
        true -> NavRailItem(onClick, icon, label, modifier, checked, colors = colors)
        else -> BottomNavItem(onClick, icon, label, modifier, checked, colors = colors)
    }
}


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
        title = stringResource(R.string.permission_screen_title),
        message = stringResource(R.string.permission_screen_desc),
        vertical = LocalWindowSize.current.widthRange == Range.Compact
    ) {
        OutlinedButton(
            onClick = { permission.launchPermissionRequest() },
            modifier = Modifier.size(width = 200.dp, height = 46.dp),
            elevation = null,
            label = stringResource(R.string.allow),
            border = ButtonDefaults.outlinedBorder,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
        )
    }
}

private val DefaultColorSpec = tween<Color>(Anim.DefaultDurationMillis)
private val LightAccentColor = Color.AzureBlue
private val DarkAccentColor = Color(0xFFFFDE3F)

/**
 * Provides a dynamic accent color based on the current theme (dark/light) and the user's
 * chosen colorization strategy. This composable reacts to changes in both the theme and
 * the currently playing media item, updating the accent color accordingly.
 *
 * @param isDark Indicates whether the current theme is dark.
 * @return A [State] object holding the calculated accent color, which will trigger
 * recomposition when the color changes.
 */
@Composable
private fun observeAccentColor(
    isDark: Boolean
): State<Color> {
    // Get the activity context for accessing resources and services
    val activity = LocalView.current.context as MainActivity
    // Default accent color based on the current theme
    val default = if (isDark) DarkAccentColor else LightAccentColor
    return produceState(initialValue = default, key1 = isDark) {
        // Observe changes in the strategy
        val preferences = activity.preferences
        var job: Job? = null
        preferences[Settings.COLORIZATION_STRATEGY].collect { strategy ->
            // cancel the old job
            job?.cancel()
            when (strategy) {
                ColorizationStrategy.Manual -> TODO("Not yet implemented!")
                ColorizationStrategy.Wallpaper -> TODO("Not yet implemented!")
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
                            value = Color(accent)
                        }
                        .flowOn(Dispatchers.Default)
                        .launchIn(this)
                }
            }
        }
    }
}

/**
 * Defines theme for app.
 *
 * @param darkTheme Whether to use the dark theme.
 * @param content The content to be displayed.
 */
@Composable
private fun Material(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF0E0E0F) else Color(0xFFF5F5FA),
        animationSpec = DefaultColorSpec
    )
    val surface by animateColorAsState(
        targetValue = if (darkTheme) Color.TrafficBlack else Color.White,
        animationSpec = DefaultColorSpec
    )
    val accent by observeAccentColor(darkTheme)
    val primary by animateColorAsState(accent)
    val colors = Colors(
        primary = primary,
        secondary = primary,
        background = background,
        surface = surface,
        primaryVariant = primary.hsl(lightness = primary.lightness * 0.9f), // make a bit darker.
        secondaryVariant = primary.hsl(lightness = primary.lightness * 0.9f), // make a bit darker.,
        onPrimary = if (primary.luminance() > 0.5) Color.TrafficBlack else Color.SignalWhite,
        onSurface = if (darkTheme) Color.SignalWhite else Color.UmbraGrey,
        onBackground = if (darkTheme) Color.SignalWhite else Color.UmbraGrey,
        error = Color.OrientRed,
        onSecondary =  if (primary.luminance() > 0.5) Color.TrafficBlack else Color.SignalWhite,
        onError = Color.SignalWhite,
        isLight = !darkTheme
    )

    // Actual theme compose; in future handle fonts etc.
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            MaterialTheme(
                colors = colors,
                content = content,
                typography = Typography(Settings.DefaultFontFamily)
            )
        }
    }

    // This block handles the logic of color of SystemBars.
    val view = LocalView.current
    // If the application is in edit mode, we do not need to handle status_bar related tasks, so we return early.
    if (view.isInEditMode) return@Material
    // Update the system bars appearance with a delay to avoid splash screen issue.
    // Use flag to avoid hitting delay multiple times.
    var isFirstPass by remember { mutableStateOf(true) }
    val translucent by preference(key = Settings.TRANSLUCENT_SYSTEM_BARS)
    val hideStatusBar by preference(key = Settings.IMMERSIVE_VIEW)
    // Set the color for status and navigation bars based on translucency
    val color = when (translucent) {
        false -> Color.Transparent.toArgb()
        else -> Color(0x20000000).toArgb()
    }
    val isAppearanceLightSystemBars = !darkTheme
    LaunchedEffect(isAppearanceLightSystemBars, hideStatusBar) {
        // A Small Delay to override the change of system bar after splash screen.
        // This is a workaround for a problem with using sideEffect to hideSystemBars.
        if (isFirstPass) {
            delay(2500)
            isFirstPass = false
        }
        val window = (view.context as Activity).window
        // Obtain the controller for managing the insets of the window.
        val controller = WindowCompat.getInsetsController(window, view)
        window.navigationBarColor = color
        window.statusBarColor = color
        // Set the color of the navigation bar and the status bar to the determined color.
        controller.isAppearanceLightStatusBars = isAppearanceLightSystemBars
        controller.isAppearanceLightNavigationBars = isAppearanceLightSystemBars
        // Hide or show the status bar based on the user's preference.
        if (hideStatusBar)
            controller.hide(WindowInsetsCompat.Type.systemBars())
        else
            controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Extension function for the NavController that facilitates navigation to a specified destination route.
 *
 * @param route The destination route to navigate to.
 *
 * This function uses the provided route to navigate using the navigation graph.
 * It includes additional configuration to manage the back stack and ensure a seamless navigation experience.
 * - It pops up to the start destination of the graph to avoid a buildup of destinations on the back stack.
 * - It uses the `launchSingleTop` flag to prevent multiple copies of the same destination when re-selecting an item.
 * - The `restoreState` flag is set to true, ensuring the restoration of state when re-selecting a previously selected item.
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
 * A composable function that represents a navigation bar, combining both rail and bottom bar elements.
 *
 * @param type Specifies whether the navigation bar includes a [NavigationRail] or [BottomNavigation] component.
 * @param navController The NavController to manage navigation within the navigation bar.
 * @param modifier The modifier for styling and layout customization of the navigation bar.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun NavigationBar(
    typeRail: Boolean,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val routes = remember {
        movableContentOf {
            // Get the current navigation destination from NavController
            val current by navController.currentBackStackEntryAsState()
            val colors = NavigationItemDefaults.navigationItemColors()
            val route = current?.destination?.route
            val facade = LocalSystemFacade.current
            // Home
            NavItem(
                label = { Label(text = textResource(R.string.home)) },
                icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = null) },
                checked = route == Library.route,
                onClick = { navController.toRoute(Library.direction()); facade.launchReviewFlow() },
                typeRail = typeRail,
                colors = colors
            )

            // Folders
            NavItem(
                label = { Label(text = textResource(R.string.folders)) },
                icon = { Icon(imageVector = Icons.Filled.FolderCopy, contentDescription = null) },
                checked = route == Folders.route,
                onClick = { navController.toRoute(Folders.direction()); facade.launchReviewFlow() },
                typeRail = typeRail,
                colors = colors
            )

            // Albums
            NavItem(
                label = { Label(text = textResource(R.string.albums)) },
                icon = { Icon(imageVector = Icons.Filled.Album, contentDescription = null) },
                checked = route == Albums.route,
                onClick = { navController.toRoute(Albums.direction()); facade.launchReviewFlow() },
                typeRail = typeRail,
                colors = colors
            )

            // Playlists
            NavItem(
                label = { Label(text = textResource(R.string.playlists)) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.PlaylistPlay,
                        contentDescription = null
                    )
                },
                checked = route == Playlists.route,
                onClick = { navController.toRoute(Playlists.direction()); facade.launchReviewFlow() },
                typeRail = typeRail,
                colors = colors
            )

            // Settings
            NavItem(
                label = { Label(text = textResource(R.string.settings)) },
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = null) },
                checked = route == Settings.route,
                onClick = { navController.toRoute(Settings.route); facade.launchReviewFlow() },
                typeRail = typeRail,
                colors = colors
            )
        }
    }
    when (typeRail) {
        true -> NavigationRail(
            modifier = modifier.width(NAV_RAIL_WIDTH),
            windowInsets = WindowInsets.systemBars,
            contentColor = Material.colors.onBackground,
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            content = {
                // Display routes at the top of the navRail.
                routes()
                // Some Space between naves and Icon.
                Spacer(modifier = Modifier.weight(1f))
                // Ensures adequate spacing at the bottom of the NavRail to accommodate pixel
                // composable.
                Spacer(modifier = Modifier.requiredSize(Glance.MIN_SIZE))
            },
        )

        else -> BottomAppBar(
            modifier = modifier/*.height(110.dp)*/,
            windowInsets = WindowInsets.navigationBars,
            backgroundColor = Material.colors.backgroundColorAtElevation(1.dp),
            contentColor = Material.colors.onBackground,
            elevation = 4.dp,
            contentPadding = PaddingValues(
                horizontal = ContentPadding.normal,
                vertical = ContentPadding.medium
            ),
            content = {
                // Ensures adequate spacing at the start of the BottomBar to accommodate pixel
                // composable.
                Spacer(modifier = Modifier.requiredSize(Glance.MIN_SIZE))
                Spacer(Modifier.weight(1f))
                // Display routes at the contre of available space
                routes()
                Spacer(modifier = Modifier.weight(1f))
            }
        )
    }
}

private val NavGraph: NavGraphBuilder.() -> Unit = {
    //AboutUs
    composable(AboutUs.route) {
        AboutUs()
    }
    //Permission
    composable(PERMISSION_ROUTE) {
        Permission()
    }
    // Library
    composable(Library.route) {
        val viewModel = hiltViewModel<LibraryViewModel>()
        Library(viewModel)
    }
    // Settings
    composable(Settings.route) {
        val viewModel = hiltViewModel<SettingsViewModel>()
        Settings(viewModel)
    }
    // Albums
    composable(Albums.route) {
        val viewModel = hiltViewModel<AlbumsViewModel>()
        Albums(viewModel = viewModel)
    }
    // Artists
    composable(Artists.route) {
        val viewModel = hiltViewModel<ArtistsViewModel>()
        Artists(viewModel = viewModel)
    }
    // Audios
    composable(Audios.route) {
        val viewModel = hiltViewModel<AudiosViewModel>()
        Audios(viewModel = viewModel)
    }
    // Folders
    composable(Folders.route) {
        val viewModel = hiltViewModel<FoldersViewModel>()
        Folders(viewModel = viewModel)
    }
    // Genres
    composable(Genres.route) {
        val viewModel = hiltViewModel<GenresViewModel>()
        Genres(viewModel = viewModel)
    }
    // Playlists
    composable(Playlists.route) {
        val viewModel = hiltViewModel<PlaylistsViewModel>()
        Playlists(viewModel = viewModel)
    }
    // Members
    composable(Members.route) {
        val viewModel = hiltViewModel<MembersViewModel>()
        Members(viewModel = viewModel)
    }
    // Tag Editor
    composable(TagEditor.route) {
        val viewModel = hiltViewModel<TagEditorViewModel>()
        TagEditor(state = viewModel)
    }
    // AudioFx
    dialog(AudioFx.route) {
        val viewModel = hiltViewModel<AudioFxViewModel>()
        AudioFx(state = viewModel)
    }
    // Console
    composable(Console.route) {
        val viewModel = hiltViewModel<ConsoleViewModel>()
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            Console(state = viewModel)
        }
    }
    // Feedback
    dialog(
        RouteFeedback.route,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ){
        val viewModel = hiltViewModel<FeedbackViewModel>()
        Feedback(viewModel)
    }
    // ControlCentre
    composable(RoutePersonalize){
        val viewModel = hiltViewModel<PersonalizeViewModel>()
        Personalize(viewModel)
    }
}

/**
 * The array of routes that are part of [NavigationBar]
 */
private val ROUTES_IN_NAV_BAR =
    arrayOf(Settings.route, Folders.route, Albums.route, Playlists.route, Library.route)

/**
 * The shape of the content inside the scaffold.
 */
private val CONTENT_SHAPE = RoundedCornerShape(topStartPercent = 8, bottomStartPercent = 8)

@Composable
fun Home(channel: Channel) {
    // Determine if the app is in dark mode based on user preferences
    val navController = rememberNavController()
    Material(isPreferenceDarkTheme()) {
        // Get the window size class
        val clazz = LocalWindowSize.current
        // Provide the navController, newWindowClass through LocalComposition.
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalWindowSize provides clazz.remaining,
            content = {
                // Determine the navigation type based on the window size class and access the system facade
                val facade = LocalSystemFacade.current
                // Determine whether to hide the navigation bar based on the current destination
                val hideNavigationBar = navController.current !in ROUTES_IN_NAV_BAR
                Log.d(TAG, "Home: ${navController.current} hide: $hideNavigationBar")
                val vertical = clazz.widthRange < Range.Medium
                NavigationSuiteScaffold(
                    vertical = vertical,
                    channel = channel,
                    hideNavigationBar = hideNavigationBar,
                    progress = facade.inAppUpdateProgress,
                    background = Material.colors.backgroundColorAtElevation(1.dp),
                    // Set up the navigation bar using the NavBar composable
                    pixel = { Glance() },
                    navBar = { NavigationBar(clazz.navTypeRail, navController) },
                    shape = when {
                        hideNavigationBar || !vertical -> RectangleShape
                        else -> CONTENT_SHAPE
                    },
                    content = {
                        val context = LocalContext.current
                        // Load start destination based on if storage permission is set or not.
                        val startDestination =
                            when (ContextCompat.checkSelfPermission(
                                context,
                                Audiofy.STORAGE_PERMISSION
                            )) {
                                PackageManager.PERMISSION_GRANTED -> Library.route
                                else -> PERMISSION_ROUTE
                            }
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            builder = NavGraph,
                            modifier = Modifier
                                .clip(CONTENT_SHAPE)
                                .background(Material.colors.background)
                                .fillMaxSize(),
                            enterTransition = {
                                scaleIn(
                                    tween(220, 90),
                                    0.98f
                                ) + fadeIn(tween(700))
                            },
                            exitTransition = { fadeOut(animationSpec = tween(700)) }
                        )
                    }
                )
            }
        )
    }
    // In this section, we handle incoming intents.
    // Intents can be of two types: video or audio. If it's a video intent,
    // we navigate to the video screen; otherwise, we play the media item in the MiniPlayer.
    // In both cases, we trigger a remote action to initiate playback.
    // Create a coroutine scope to handle asynchronous operations.
    val scope = rememberCoroutineScope()
    // Check if the current composition is in inspection mode.
    // Inspection mode is typically used during UI testing or debugging to isolate and analyze
    // specific UI components. If in inspection mode, return to avoid executing the rest of the code.
    if (LocalInspectionMode.current) return
    val activity = LocalView.current.context as MainActivity
    // Construct the DisposableEffect and listen for events.
    DisposableEffect(Unit) {
        // Create a listener for observing changes in incoming intents.
        val listener = listener@{ intent: Intent ->
            // Check if the intent action is not ACTION_VIEW; if so, return.
            if (intent.action != Intent.ACTION_VIEW)
                return@listener
            // Obtain the URI from the incoming intent data.
            val data = intent.data ?: return@listener
            // Use a coroutine to handle the media item construction and playback.
            scope.launch {
                // Construct a MediaItem using the obtained parameters.
                // (Currently, details about playback queue setup are missing.)
                val item = MediaItem(activity, data)
                // Play the media item by replacing the existing queue.
                activity.remote.set(listOf(item))
                activity.remote.play()
            }
            // If the intent is related to video content, navigate to the video player screen.
            navController.navigate(Console.direction())
        }
        val firebase = Firebase.analytics
        // Listen for navDest and log in firebase.
        val navDestChangeListener =
            { _: NavController, destination: NavDestination, _: Bundle? ->
                // Log the event.
                firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    // create params for the event.
                    val route = destination.route ?: "unknown"
                    val index = route.indexOf('/')
                    val name = if (index == -1) route else route.substring(0, index)
                    Log.d(TAG, "onNavDestChanged: $name")
                    param(FirebaseAnalytics.Param.SCREEN_NAME, name)
                }
            }
        // Register the intent listener with the activity.
        activity.addOnNewIntentListener(listener)
        navController.addOnDestinationChangedListener(navDestChangeListener)
        // Unregister the intent listener when this composable is disposed.
        onDispose {
            activity.removeOnNewIntentListener(listener)
            navController.removeOnDestinationChangedListener(navDestChangeListener)
        }
    }
}