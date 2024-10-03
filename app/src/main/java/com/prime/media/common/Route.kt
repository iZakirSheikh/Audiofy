/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-08-2024.
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

package com.prime.media.common

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zs.core_ui.LocalNavAnimatedVisibilityScope

private val SPLIT_REGEX = Regex("(?=[A-Z])")

/**
 * Represents a navigation route.
 * Intended for use with singleton objects representing unique routes within the application.
 * Implementing classes can use the `invoke` operator to construct a navigation direction.
 * @property domain The domain of the route, used for grouping related routes (e.g., "route_files").
 * @property route The route template, composed of the domain and argument placeholders
 *           (e.g., `"$domain/{${PARAM_KEY}}/{${PARAM_QUERY}}/{${PARAM_GROUP_BY}}"`).
 */
interface Route {
    val route: String get() = domain
    val domain: String
        get() {
            val name = this::class.java.simpleName
            if (!name.startsWith("Route"))
                error("Route name: ($name) must start with 'Route'")
            // Split by uppercase letters
            // Join with underscores and lowercase
            return name.split(SPLIT_REGEX).joinToString("_") { it.lowercase() }
        }

    /**
     * Constructs the navigation direction for this route, without any arguments. if the route doesnt support
     * direction without arguments; override this and throw an exception.
     *
     * @return The navigation direction for this route.
     */
    operator fun invoke(): String = route
}

/**
 * Extends [Route] to provide mapping to and from arguments, typically using [SavedStateHandle].
 *
 * @param T The type of arguments this route accepts.
 */
interface SafeArgs<T> : Route {

    override fun invoke() = error("Not Supported")

    /**
     * Constructs the navigation direction for this route, replacing placeholders in [route]
     * with the provided arguments.
     *
     * @param arg The arguments to use for constructing the direction.
     */
    operator fun invoke(arg: T): String

    /**
     * Builds the arguments for this route from the provided [SavedStateHandle].
     *
     * @param handle The [SavedStateHandle] to use for building the arguments.
     * @return The arguments for this route.
     */
    fun build(handle: SavedStateHandle): T
}

/**
 * Retrieves the [SafeArgs] object of type[T] associated with the given [SafeArgs] instance
 * from this [SavedStateHandle].
 *
 * @param route The [SafeArgs] instance representing the navigation route and arguments.
 * @return The args object of type [T] containing the deserialized arguments.
 **/
operator fun <T> SavedStateHandle.get(route: SafeArgs<T>): T = route.build(this)


/**
 * Adds a composable route to the [NavGraphBuilder] for the given [Route].
 *
 * @param route The [Route] object representing the navigation destination.
 * @param content The composable content to display for this route.
 */
fun NavGraphBuilder.composable(
    route: Route,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable(route = route.route, content = { id ->
    CompositionLocalProvider(value = LocalNavAnimatedVisibilityScope provides this) {
        content(id)
    }
})

/**
 * @see androidx.navigation.NavGraph.setStartDestination(java.lang.String)
 */
fun <T> NavGraph.setStartDestination(route: Route) =
    setStartDestination(route.route)

/**
 * Extracts the domain portion from a [NavDestination]'s route.
 *
 * The domain is considered to be the part of the route before the first '/'.
 * For example, for the route "settings/profile", the domain would be "settings".
 *
 * @return The domain portion of the route, or null if the route is null or does not contain a '/'.
 */
val NavDestination.domain: String?
    get() {
        // Get the route, or return null if it's not available.
        val route = route ?: return null

        // Find the index of the first '/' character.
        val index = route.indexOf('/')

        // Return the substring before the '/' if it exists, otherwise return the entire route.
        return if (index == -1) route else route.substring(0, index)
    }