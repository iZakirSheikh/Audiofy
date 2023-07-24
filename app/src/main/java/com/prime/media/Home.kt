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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.prime.media.impl.ConsoleViewModel
import com.prime.media.impl.LibraryViewModel
import com.prime.media.impl.SettingsViewModel
import com.prime.media.library.Library
import com.prime.media.settings.Settings
import com.primex.core.OrientRed
import com.primex.core.SignalWhite
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.drawHorizontalDivider
import com.primex.material2.OutlinedButton
import kotlinx.coroutines.launch

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

private val LightPrimaryColor = Color(0xFF17618D)
private val LightPrimaryVariantColor = Color(0xFF14547B)
private val LightSecondaryColor = Color(0xFF8B008B)
private val LightSecondaryVariantColor = Color(0xFF7B0084)
private val DarkPrimaryColor = Color(0xFF17618D)
private val DarkPrimaryVariantColor = Color(0xFF14547B)
private val DarkSecondaryColor = Color(0xFF8B008B)
private val DarkSecondaryVariantColor = Color(0xFF7B0084)

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
        onBackground = if (darkTheme) Color.SignalWhite else Color.Black,
        error = Color.OrientRed,
        onSecondary = Color.SignalWhite,
        onError = Color.SignalWhite,
        isLight = !darkTheme
    )
    // Actual theme compose; in future handle fonts etc.
    MaterialTheme(
        colors = colors,
        content = content,
        typography = Typography(Settings.LatoFontFamily)
    )
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

private val LightSystemBarsColor = Color(0x20000000)
private val DarkSystemBarsColor = Color(0x11FFFFFF)

@Composable
fun Home(
    channel: Channel
) {
    val isDark = isPrefDarkTheme()
    Material(isDark) {
        val navController = rememberNavController()
        // Collapse if expanded and back button is clicked.
        // FixMe: Currently it doesn't work if navGraph ihas not start Dest.
        val scope = rememberCoroutineScope()
        val state = rememberScaffoldState2(initial = SheetValue.COLLAPSED)
        BackHandler(state.isExpanded) { scope.launch { state.collapse(false) } }
        CompositionLocalProvider(LocalNavController provides navController) {
            val vertical = LocalWindowSizeClass.current.widthSizeClass < WindowWidthSizeClass.Medium
            val facade = LocalSystemFacade.current
            Scaffold2(
                vertical = true, // currently don't pass value of vertical unless layout is ready.
                channel = channel,
                state = state,
                sheetPeekHeight = if (facade.isPlayerReady) Settings.MINI_PLAYER_HEIGHT else 0.dp,
                color = MaterialTheme.colors.background,
                progress = facade.inAppUpdateProgress,
                content = { NavGraph(Modifier.drawHorizontalDivider(color = Material.colors.onSurface)) },
                sheet = {
                    val viewModel = hiltViewModel<ConsoleViewModel>()
                    Console(state = viewModel, expanded = state.isExpanded) {
                        scope.launch { state.toggle(false) }
                    }
                }
            )
        }
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
            else -> Color.Transparent
        }
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = color.toArgb()
            window.statusBarColor = color.toArgb()
            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDark && !colorSystemBars
            //
            if (hideStatusBar)
                WindowCompat.getInsetsController(window, view)
                    .hide(WindowInsetsCompat.Type.statusBars())
            else
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.statusBars())
        }
    }
}