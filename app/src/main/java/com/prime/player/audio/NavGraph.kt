package com.prime.player.audio

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.prime.player.PlayerTheme
import com.prime.player.audio.groups.Groups
import com.prime.player.audio.groups.GroupsViewModel
import com.prime.player.audio.library.Library
import com.prime.player.audio.library.LibraryViewModel
import com.prime.player.audio.settings.Settings
import com.prime.player.audio.settings.SettingsViewModel
import com.prime.player.audio.tracks.Tracks
import com.prime.player.audio.tracks.TracksViewModel
import com.prime.player.extended.INavActions
import com.prime.player.extended.LocalNavActionProvider

private object Route {
    const val Library = "library"

    const val Groups = "groups"

    const val Tracks = "tracks"

    const val Settings = "settings"
}

class AudioNavigationActions(private val controller: NavHostController) : INavActions {
    override fun getNavController(): NavHostController {
        return controller
    }

    override fun navigateUp() {
        controller.popBackStack()
    }

    fun toSettings() {
        controller.navigate(Route.Settings) {
            // Avoid multiple copies of the same destination when
            // re-selecting the same item
            launchSingleTop = true
            // Restore state when re-selecting a previously selected item
            restoreState = true
        }
    }

    fun toGroups(type: GroupOf) {
        controller.navigate("${Route.Groups}/$type") {
            // Avoid multiple copies of the same destination when
            // re-selecting the same item
            launchSingleTop = true
            // Restore state when re-selecting a previously selected item
            restoreState = true
        }
    }

    fun toGroupViewer(of: GroupOf, uniqueID: String = " ") {
        controller.currentBackStackEntry?.destination?.route?.let {
            if (it.contains(Route.Tracks))
                navigateUp()
        }
        controller.navigate("${Route.Tracks}/$of/$uniqueID") {
            // Avoid multiple copies of the same destination when
            // re-selecting the same item
            launchSingleTop = true
            // Restore state when re-selecting a previously selected item
            restoreState = true
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(padding: State<PaddingValues>) {
    AnimatedNavHost(
        navController = LocalNavActionProvider.current.getNavController(),
        startDestination = Route.Library,
        modifier = Modifier
            .background(color = PlayerTheme.colors.background),
        enterTransition = {
            scaleIn(initialScale = 0.92f) + fadeIn()
        },
        exitTransition = {
            scaleOut(targetScale = 0.92f) + fadeOut()
        }
    ) {
        composable(
            Route.Library,
        ) { entry ->
            val viewModel = hiltViewModel<LibraryViewModel>(entry)
            Library(padding, viewModel = viewModel)
        }

        composable(
            Route.Settings,
        ) { entry ->
            val viewModel = hiltViewModel<SettingsViewModel>(entry)
            Settings(padding = padding, viewModel = viewModel)
        }

        composable(
            "${Route.Groups}/{${GroupsViewModel.TYPE}}",
            arguments = listOf(
                navArgument(GroupsViewModel.TYPE) { type = NavType.StringType },
            ),
        ) { entry ->
            val viewModel = hiltViewModel<GroupsViewModel>(entry)

            val args = requireNotNull(entry.arguments)
            val type = GroupOf.valueOf(requireNotNull(args.getString(GroupsViewModel.TYPE)))
            viewModel.init(type)
            Groups(padding, viewModel)
        }

        composable(
            "${Route.Tracks}/{${TracksViewModel.FROM}}/{${TracksViewModel.UNIQUE_ID}}",
            arguments = listOf(
                navArgument(TracksViewModel.UNIQUE_ID) { type = NavType.StringType },
                navArgument(TracksViewModel.FROM) { type = NavType.StringType },
            ),

            ) { entry ->
            val viewModel = hiltViewModel<TracksViewModel>(entry)

            val args = requireNotNull(entry.arguments)
            val key = requireNotNull(args.getString(TracksViewModel.UNIQUE_ID))
            val from = GroupOf.valueOf(requireNotNull(args.getString(TracksViewModel.FROM)))
            viewModel.init(from, key)
            Tracks(padding, viewModel = viewModel)
        }
    }
}