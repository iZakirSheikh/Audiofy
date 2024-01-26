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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ChipDefaults
import androidx.compose.material.Colors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SelectableChipColors
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.prime.media.core.compose.LocalWindowSize
import com.prime.media.core.compose.NavigationRailItem2
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.Reach
import com.prime.media.core.compose.Scaffold2
import com.prime.media.core.compose.colorsNavigationItem2
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
import com.primex.core.BlueLilac
import com.primex.core.DahliaYellow
import com.primex.core.ImageBrush
import com.primex.core.OrientRed
import com.primex.core.SignalWhite
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.hsl
import com.primex.core.visualEffect
import com.primex.material2.OutlinedButton
import kotlinx.coroutines.delay
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
    @Composable inline get() = (if (isLight) Color.Black else Color.White).copy(0.04f)

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
 * Computes the tonal color at different elevation levels for the [background] color.
 *
 * This function calculates the tonal elevation effect by adjusting the alpha of the
 * [Colors.primary] color overlaid on the [background] color. The resulting color is
 * influenced by the logarithmic function.
 *
 * @param background The base color on which the tonal elevation is applied.
 * @param elevation  Elevation value used to compute the alpha of the color overlay layer.
 *
 * @return The [background] color with an alpha overlay of the [Colors.primary] color.
 * @see applyTonalElevation
 */
private fun Colors.applyTonalElevation(
    background: Color,
    elevation: Dp
) = primary.copy(alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f).compositeOver(background)

/**
 * @see applyTonalElevation
 */
fun Colors.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    return applyTonalElevation(surface, elevation)
}

/**
 * @see applyTonalElevation
 */
fun Colors.backgroundColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    return applyTonalElevation(surface, elevation)
}

/**
 * Returns true if the system bars are required to be light-themed, false otherwise.
 * @see WindowInsetsControllerCompat.isAppearanceLightStatusBars
 */
inline val Colors.isAppearanceLightSystemBars
    @Composable inline get() = isLight && !preference(key = Settings.COLOR_STATUS_BAR).value


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
        vertical = LocalWindowSize.current.widthReach == Reach.Compact
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

private val LightPrimaryColor = Color.BlueLilac
private val LightPrimaryVariantColor = LightPrimaryColor.hsl(lightness = 0.25f)
private val LightSecondaryColor = Color(0xFF008000)
private val LightSecondaryVariantColor = LightSecondaryColor.hsl(lightness = 0.2f)
private val DarkPrimaryColor = Color.Amber
private val DarkPrimaryVariantColor = /*Color.Amber*/ DarkPrimaryColor.hsl(lightness = 0.6f)
private val DarkSecondaryColor = Color.DahliaYellow
private val DarkSecondaryVariantColor = Color(0xFFf57d00)

private val LightSystemBarsColor = Color(0x10000000)
private val DarkSystemBarsColor = Color(0x11FFFFFF)

@Composable
private fun Material(
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

    // This block handles the logic of color of SystemBars.
    val view = LocalView.current
    // If the application is in edit mode, we do not need to handle status_bar related tasks, so we return early.
    if (view.isInEditMode) return@Material
    // Update the system bars appearance with a delay to avoid splash screen issue.
    // Use flag to avoid hitting delay multiple times.
    var isFirstPass by remember { mutableStateOf(true) }
    val colorSystemBars by preference(key = Settings.COLOR_STATUS_BAR)
    val hideStatusBar by preference(key = Settings.HIDE_STATUS_BAR)
    val color = when {
        !colorSystemBars -> Color.Transparent
        darkTheme -> DarkSystemBarsColor
        else -> LightSystemBarsColor
    }
    val isAppearanceLightSystemBars = !darkTheme && !colorSystemBars
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
        window.navigationBarColor = color.toArgb()
        window.statusBarColor = color.toArgb()
        // Set the color of the navigation bar and the status bar to the determined color.
        controller.isAppearanceLightStatusBars = isAppearanceLightSystemBars
        controller.isAppearanceLightNavigationBars = isAppearanceLightSystemBars
        // Hide or show the status bar based on the user's preference.
        if (hideStatusBar)
            controller.hide(WindowInsetsCompat.Type.statusBars())
        else
            controller.show(WindowInsetsCompat.Type.statusBars())
    }
}

/**
 * A simple structure of the NavGraph.
 */
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
        builder = {
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
        },
    )
}

/**
 * The array of routes that are required to hide the miniplayer.
 */
private val HIDDEN_DEST_ROUTES =
    arrayOf(Console.route, PERMISSION_ROUTE, AudioFx.route)

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

/**
 * Represents a navigation item either in a [NavigationRail] (when [isNavRail] is true)
 * or a [BottomNavigation] (when [isNavRail] is false).
 *
 * @param label The text label associated with the navigation item.
 * @param icon The vector graphic icon representing the navigation item.
 * @param onClick The callback function to be executed when the navigation item is clicked.
 * @param modifier The modifier for styling and layout customization of the navigation item.
 * @param checked Indicates whether the navigation item is currently selected.
 * @param isNavRail Specifies whether the navigation item is intended for a [NavigationRail].
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun Route(
    label: CharSequence,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    isNavRail: Boolean = false,
    colors: SelectableChipColors = ChipDefaults.colorsNavigationItem2()
) {

    when (isNavRail) {
        true -> NavigationRailItem2(
            onClick = onClick,
            icon = icon,
            label = label,
            checked = checked,
            modifier = modifier,
            colors = colors,
            border = null
        )

        else -> BottomNavigationItem2(
            onClick = onClick,
            icon = icon,
            label = label,
            checked = checked,
            modifier = modifier,
            colors = colors,
            border = null
        )
    }
}

private val NAV_RAIL_WIDTH = 96.dp

/**
 * A composable function that represents a navigation bar, combining both rail and bottom bar elements.
 *
 * @param isNavRail Specifies whether the navigation bar includes a [NavigationRail] or [BottomNavigation] component.
 * @param navController The NavController to manage navigation within the navigation bar.
 * @param modifier The modifier for styling and layout customization of the navigation bar.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun NavBar(
    isNavRail: Boolean,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    // Create a movable content container to define the routes
    val routes = remember {
        movableContentOf {
            // Get the current navigation destination from NavController
            val current by navController.currentBackStackEntryAsState()
            val colors = ChipDefaults.colorsNavigationItem2(
                leadingIconColor = LocalContentColor.current,
                selectedBackgroundColor = Material.colors.primary.copy(0.15f),
                selectedLeadingIconColor = Material.colors.primary,
                selectedContentColor = MaterialTheme.colors.primary,
                contentColor = LocalContentColor.current
            )
            // Home
            Route(
                label = "    Home   ",
                icon = Icons.Outlined.Home,
                checked = current?.destination?.route == Library.route,
                onClick = { navController.toRoute(Library.direction()) },
                isNavRail = isNavRail,
                colors = colors
            )

            // Audios
            Route(
                label = "  Folders ",
                icon = Icons.Outlined.FolderCopy,
                checked = current?.destination?.route == Folders.route,
                onClick = { navController.toRoute(Folders.direction()) },
                isNavRail = isNavRail,
                colors = colors
            )

            // Videos
            val context = LocalContext.current as MainActivity
            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
                    if (it == null) return@rememberLauncherForActivityResult
                    val intnet = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(it, MIME_TYPE_VIDEO)
                        this.`package` = context.packageName
                    }
                    context.startActivity(intnet)
                }
            Route(
                label = "  Videos ",
                icon = Icons.Outlined.VideoLibrary,
                checked = false,
                onClick = { launcher.launch(arrayOf(MIME_TYPE_VIDEO)) },
                isNavRail = isNavRail,
                colors = colors
            )

            // Playlists
            Route(
                label = "Playlists",
                icon = Icons.Outlined.PlaylistPlay,
                checked = current?.destination?.route == Playlists.route,
                onClick = { navController.toRoute(Playlists.direction()) },
                isNavRail = isNavRail,
                colors = colors
            )

            // Settings
            Route(
                label = "Settings",
                icon = Icons.Outlined.Settings,
                checked = current?.destination?.route == Settings.route,
                onClick = { navController.toRoute(Settings.route) },
                isNavRail = isNavRail,
                colors = colors
            )
        }
    }
    // Depending on whether it's a bottom app bar or a navigation rail, apply the appropriate composable
    when (isNavRail) {
        false -> BottomAppBar(
            modifier = modifier,
            windowInsets = WindowInsets.navigationBars,
            backgroundColor = Color.Transparent,
            contentColor = Material.colors.onSurface,
            elevation = 0.dp,
            contentPadding = PaddingValues(
                horizontal = ContentPadding.normal,
                vertical = ContentPadding.medium
            )
        ) {
            var expanded by remember { mutableStateOf(false) }
            PopupMedia(
                expanded = expanded,
                onRequestToggle = { expanded = !expanded }
            )

            Spacer(Modifier.weight(1f))
            // Display routes at the contre of available space
            routes()

            Spacer(modifier = Modifier.weight(1f))
        }

        else -> androidx.compose.material.NavigationRail(
            modifier = modifier.width(NAV_RAIL_WIDTH),
            windowInsets = WindowInsets.systemBars,
            backgroundColor = Color.Transparent,
            contentColor = Material.colors.onSurface,
            elevation = 0.dp,
        ) {
            // Display routes at the top of the navRail.
            routes()
            // Some Space between naves and Icon.
            Spacer(modifier = Modifier.weight(1f))

            var expanded by remember { mutableStateOf(false) }
            PopupMedia(
                expanded = expanded,
                onRequestToggle = { expanded = !expanded },
                offset = DpOffset(30.dp, (-30).dp)
            )
        }
    }
}

private val CONTENT_SHAPE = RoundedCornerShape(5)

@Composable
fun Home(channel: Channel) {
    // Determine if the app is in dark mode based on user preferences
    val isDark = isPrefDarkTheme()
    Material(isDark) {
        val navController = rememberNavController()
        CompositionLocalProvider(LocalNavController provides navController) {
            // Determine the window size class and access the system facade
            val clazz = LocalWindowSize.current.widthReach
            val facade = LocalSystemFacade.current
            // Check if the layout should be vertical based on the window size class
            val vertical = clazz < Reach.Medium
            // Determine whether to hide the navigation bar based on the current destination
            val hideNavigationBar = navController.current in HIDDEN_DEST_ROUTES
            Scaffold2(
                vertical = vertical,
                channel = channel,
                hideNavigationBar = hideNavigationBar,
                progress = facade.inAppUpdateProgress,
                background = Material.colors.overlay.compositeOver(Material.colors.background),
                // Set up the navigation bar using the NavBar composable
                navBar = {
                    NavBar(
                        isNavRail = !vertical,
                        navController = navController,
                        modifier = Modifier.visualEffect(
                            ImageBrush.NoiseBrush, 0.5f
                        )
                    )
                },
                // Display the main content of the app using the NavGraph composable
                content = {
                    NavGraph(
                        modifier = Modifier
                            .clip(if (!hideNavigationBar) CONTENT_SHAPE else RectangleShape)
                            .background(Material.colors.background)
                            .fillMaxSize()
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
        if (LocalInspectionMode.current) return@Material
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