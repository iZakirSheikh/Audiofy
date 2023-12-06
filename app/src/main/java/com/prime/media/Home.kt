@file:Suppress("AnimateAsStateLabel")

package com.prime.media

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.prime.media.console.Console
import com.prime.media.console.PopupMedia
import com.prime.media.core.ContentPadding
import com.prime.media.core.NightMode
import com.prime.media.core.compose.BottomNavigationItem2
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowSizeClass
import com.prime.media.core.compose.NavigationRailItem2
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.Scaffold2
import com.prime.media.core.compose.current
import com.prime.media.core.compose.preference
import com.prime.media.core.playback.MediaItem
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
import com.prime.media.impl.AudioFxViewModel
import com.prime.media.impl.ConsoleViewModel
import com.prime.media.impl.LibraryViewModel
import com.prime.media.impl.SettingsViewModel
import com.prime.media.impl.TagEditorViewModel
import com.prime.media.library.Library
import com.prime.media.settings.Settings
import com.primex.core.Amber
import com.primex.core.DahliaYellow
import com.primex.core.OrientRed
import com.primex.core.SignalWhite
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.drawHorizontalDivider
import com.primex.core.drawVerticalDivider
import com.primex.material2.OutlinedButton
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
 * This Composable function provides a primary container color with reduced emphasis as compared to
 * the primary color.
 * It is used for styling elements that require a less prominent color.
 *
 * The color returned by this function is derived from the primary color of the current
 * MaterialTheme with an alpha value equal to [MaterialTheme.CONTAINER_COLOR_ALPHA].
 *
 * @return a [Color] object representing the primary container color.
 */
val Colors.primaryContainer
    @Composable inline get() = MaterialTheme.colors.primary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * Returns a color that is suitable for content (icons, text, etc.) that sits on top of the primary container color.
 * This color is simply the primary color of the current theme.
 *
 * @return [Color] object that represents the on-primary container color
 */
val Colors.onPrimaryContainer
    @Composable inline get() = MaterialTheme.colors.primary

/**
 * Secondary container is applied to elements needing less emphasis than secondary
 */
val Colors.secondaryContainer
    @Composable inline get() = MaterialTheme.colors.secondary.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * On-secondary container is applied to content (icons, text, etc.) that sits on top of secondary
 * container
 */
val Colors.onSecondaryContainer @Composable inline get() = MaterialTheme.colors.secondary

/**
 * Error container is applied to elements associated with an error state
 */
val Colors.errorContainer
    @Composable inline get() = MaterialTheme.colors.error.copy(MaterialTheme.CONTAINER_COLOR_ALPHA)

/**
 * On-error container is applied to content (icons, text, etc.) that sits on top of error container
 */
val Colors.onErrorContainer @Composable inline get() = MaterialTheme.colors.error

/**
 * The overlay color used for backgrounds and shadows.
 * The color is black with alpha 0.04 on light themes and white with alpha 0.04 on dark themes.
 */
val Colors.overlay
    @Composable inline get() = (if (isLight) Color.Black else Color.White).copy(0.04f)

/**
 * The outline color used in the light/dark theme.
 *
 * The color is semi-transparent white/black, depending on the current theme, with an alpha of 0.12.
 */
inline val Colors.outline
    get() = (if (isLight) Color.Black else Color.White).copy(0.12f)
val Colors.onOverlay
    @Composable inline get() = (MaterialTheme.colors.onBackground).copy(alpha = ContentAlpha.medium)
val Colors.lightShadowColor
    @Composable inline get() = if (isLight) Color.White else Color.White.copy(0.025f)
val Colors.darkShadowColor
    @Composable inline get() = if (isLight) Color(0xFFAEAEC0).copy(0.7f) else Color.Black.copy(0.6f)

/**
 * Computes the surface tonal color at different elevation levels e.g. surface1 through surface5. This function is inspired by the Material 3 design system.
 *
 * @param elevation Elevation value used to compute alpha of the color overlay layer.
 *
 * @return the [ColorScheme.surface] color with an alpha of the [Colors.primary] color
 * overlaid on top of it.

 */
fun Colors.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return primary.copy(alpha = alpha).compositeOver(surface)
}

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
        title = stringResource(R.string.permission_screen_title),
        message = stringResource(R.string.permission_screen_desc),
        vertical = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact
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

private val LightPrimaryColor = Color(0xFF244285)
private val LightPrimaryVariantColor = Color(0xFF305EA5)
private val LightSecondaryColor = Color(0xFF8B008B)
private val LightSecondaryVariantColor = Color(0xFF7B0084)
private val DarkPrimaryColor = Color(0xFFff8f00)
private val DarkPrimaryVariantColor = Color.Amber
private val DarkSecondaryColor = Color.DahliaYellow
private val DarkSecondaryVariantColor = Color(0xFFf57d00)

@Composable
@NonRestartableComposable
fun Material(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF0E0E0F) else Color(0xFFF5F5FA),
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )
    val surface by animateColorAsState(
        targetValue = if (darkTheme) Color.TrafficBlack else Color.White,
        animationSpec = tween(AnimationConstants.DefaultDurationMillis)
    )
    val primary = if (darkTheme) DarkPrimaryColor else LightPrimaryColor
    val primaryVariant = if (darkTheme) DarkPrimaryVariantColor else LightPrimaryVariantColor
    val secondary = if (darkTheme) DarkSecondaryColor else LightSecondaryColor
    val secondaryVariant = if (darkTheme) DarkSecondaryVariantColor else LightSecondaryVariantColor
    val colors = Colors(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        primaryVariant = primaryVariant,
        secondaryVariant = secondaryVariant,
        onPrimary = Color.SignalWhite,
        onSurface = if (darkTheme) Color.SignalWhite else Color.UmbraGrey,
        onBackground = if (darkTheme) Color.SignalWhite else Color.UmbraGrey,
        error = Color.OrientRed,
        onSecondary = Color.SignalWhite,
        onError = Color.SignalWhite,
        isLight = !darkTheme
    )
    // Actual theme compose; in future handle fonts etc.
    MaterialTheme(
        colors = colors,
        content = content,
        typography = Typography(Settings.DefaultFontFamily)
    )

    // In this block, we handle status_bar related tasks, but this occurs only when
    // the application is in edit mode.
    // FixMe: Consider handling scenarios where the current activity is not MainActivity,
    //  as preferences may depend on the MainActivity.
    // Handle SystemBars logic.
    val view = LocalView.current
    // Early return
    if (view.isInEditMode)
        return@Material
    // FixMe: It seems sideEffect is not working for hideSystemBars.
    val colorSystemBars by preference(key = Settings.COLOR_STATUS_BAR)
    val hideStatusBar by preference(key = Settings.HIDE_STATUS_BAR)
    val color = when {
        colorSystemBars -> Material.colors.primary
        darkTheme -> DarkSystemBarsColor
        else -> LightSystemBarsColor
    }
    SideEffect {
        val window = (view.context as Activity).window
        window.navigationBarColor = color.toArgb()
        window.statusBarColor = color.toArgb()
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

        composable(TagEditor.route) {
            val viewModel = hiltViewModel<TagEditorViewModel>()
            TagEditor(state = viewModel)
        }

        dialog(AudioFx.route) {
            val viewModel = hiltViewModel<AudioFxViewModel>()
            AudioFx(state = viewModel)
        }

        composable(Console.route) {
            val viewModel = hiltViewModel<ConsoleViewModel>()
            Console(state = viewModel)
        }
    }
}

private val LightSystemBarsColor = /*Color(0x10000000)*/ Color.Transparent
private val DarkSystemBarsColor = /*Color(0x11FFFFFF)*/ Color.Transparent

/**
 * The array of routes that are required to hide the miniplayer.
 */
private val HIDDEN_DEST_ROUTES = arrayOf(
    Console.route,
    PERMISSION_ROUTE,
    AudioFx.route
)

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

private const val MIME_TYPE_VIDEO = "video/*"

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun BottomNav(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    BottomAppBar(
        modifier = modifier,
        windowInsets = WindowInsets.navigationBars,
        backgroundColor = Material.colors.background,
        contentColor = Material.colors.onSurface,
        contentPadding = PaddingValues(
            horizontal = ContentPadding.normal,
            vertical = ContentPadding.medium
        )
    ) {
        var expanded by remember { mutableStateOf(false) }
        PopupMedia(
            expanded = expanded,
            onRequestToggle = {expanded = !expanded },
            modifier = Modifier.padding(end = ContentPadding.normal)
        )

        // Space of normal.
        val current by navController.currentBackStackEntryAsState()

        // Home
        BottomNavigationItem2(
            label = "Home",
            icon = Icons.Outlined.Home,
            checked = current?.destination?.route == Library.route,
            onClick = { navController.toRoute(Library.direction()) }
        )

        // Audios
        BottomNavigationItem2(
            label = "Audios",
            icon = Icons.Outlined.LibraryMusic,
            checked = current?.destination?.route == Audios.route,
            onClick = { navController.toRoute(Audios.direction(Audios.GET_EVERY)) }
        )

        // Videos
        val context = LocalContext.current as MainActivity
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it == null) return@rememberLauncherForActivityResult
            val intnet = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(it,  MIME_TYPE_VIDEO)
                this.`package` = context.packageName
            }
            context.startActivity(intnet)
        }
        BottomNavigationItem2(
            label = "Videos",
            icon = Icons.Outlined.VideoLibrary,
            checked = false,
            onClick = { launcher.launch(arrayOf(MIME_TYPE_VIDEO)) }
        )

        // Playlists
        BottomNavigationItem2(
            label = "Playlists",
            icon = Icons.Outlined.PlaylistPlay,
            checked = current?.destination?.route == Playlists.route,
            onClick = { navController.toRoute(Playlists.direction()) }
        )

        // Settings
        BottomNavigationItem2(
            label = "Settings",
            icon = Icons.Outlined.Settings,
            checked = current?.destination?.route == Settings.route,
            onClick = { navController.toRoute(Settings.route) }
        )
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NavRail(modifier: Modifier = Modifier) {
    androidx.compose.material.NavigationRail(
        modifier = modifier.width(94.dp),
        windowInsets = WindowInsets.systemBars,
        backgroundColor = Material.colors.background,
        contentColor = Material.colors.onSurface,
        elevation = 0.dp,
    ) {
        val navController = LocalNavController.current
        // Space of normal.
        val current by navController.currentBackStackEntryAsState()

        // Home
        NavigationRailItem2(
            label = "Home",
            icon = Icons.Outlined.Home,
            checked = current?.destination?.route == Library.route,
            onClick = { navController.toRoute(Library.direction()) }
        )

        // Audios
        NavigationRailItem2(
            label = "Audios",
            icon = Icons.Outlined.LibraryMusic,
            checked = current?.destination?.route == Audios.route,
            onClick = { navController.toRoute(Audios.direction(Audios.GET_EVERY)) }
        )

        // Videos
        val context = LocalContext.current as MainActivity
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it == null) return@rememberLauncherForActivityResult
            val intnet = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(it,  MIME_TYPE_VIDEO)
                this.`package` = context.packageName
            }
            context.startActivity(intnet)
        }
        NavigationRailItem2(
            label = "Videos",
            icon = Icons.Outlined.VideoLibrary,
            checked = false,
            onClick = { launcher.launch(arrayOf(MIME_TYPE_VIDEO)) }
        )

        // Playlists
        NavigationRailItem2(
            label = "Playlists",
            icon = Icons.Outlined.PlaylistPlay,
            checked = current?.destination?.route == Playlists.route,
            onClick = { navController.toRoute(Playlists.direction()) }
        )

        // Settings
        NavigationRailItem2(
            label = "Settings",
            icon = Icons.Outlined.Settings,
            checked = current?.destination?.route == Settings.route,
            onClick = { navController.toRoute(Settings.route) }
        )

        // Some Space between navs and Icon.
        Spacer(modifier = Modifier.weight(1f))

        var expanded by remember { mutableStateOf(false) }
        PopupMedia(
            expanded = expanded,
            onRequestToggle = {expanded = !expanded },
            modifier = Modifier.padding(end = ContentPadding.normal)
        )
    }
}


// TODO: Add capability in original API to reverse the drawing of divider.
private inline fun Modifier.divider(vertical: Boolean, color: Color) =
    if (vertical) drawVerticalDivider(color) else drawHorizontalDivider(color)

@Composable
fun Home(
    channel: Channel
) {
    val isDark = isPrefDarkTheme()
    Material(isDark) {
        val navController = rememberNavController()
        CompositionLocalProvider(LocalNavController provides navController) {
            val clazz = LocalWindowSizeClass.current.widthSizeClass
            val facade = LocalSystemFacade.current
            Scaffold2(
                // TODO: Make it dependent LocalWindowSizeClass once horizontal layout of MiniPlayer is Ready.
                vertical = clazz < WindowWidthSizeClass.Medium,
                channel = channel,
                hideNavigationBar = navController.current in HIDDEN_DEST_ROUTES,
                progress = facade.inAppUpdateProgress,
                content = { NavGraph(Modifier.divider(clazz > WindowWidthSizeClass.Medium,  Material.colors.onSurface)) },
                navBar = {
                    when (clazz) {
                        // Always display the bottom navigation when in compact mode.
                        WindowWidthSizeClass.Compact -> BottomNav()

                        // Display the navigation rail for other size classes.
                        // TODO: Consider displaying a larger version of the navigation UI in expanded mode.
                        else -> NavRail()
                    }
                }
            )
        }

        // In this section, we handle incoming intents.
        // Intents can be of two types: video or audio. If it's a video intent,
        // we navigate to the video screen; otherwise, we play the media item in the MiniPlayer.
        // In both cases, we trigger a remote action to initiate playback.
        // Create a coroutine scope to handle asynchronous operations.
        val scope = rememberCoroutineScope()
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
            // Register the intent listener with the activity.
            activity.addOnNewIntentListener(listener)
            // Unregister the intent listener when this composable is disposed.
            onDispose { activity.removeOnNewIntentListener(listener) }
        }
    }
}