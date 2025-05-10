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

package com.prime.media.common

import android.net.Uri
import android.view.Window
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.prime.media.common.Route.ArgsFactory
import com.zs.compose.theme.LocalNavAnimatedVisibilityScope

/**
 * Represents a sorting order and associated grouping or ordering action.
 *
 * @property first Specifies whether the sorting is ascending or descending.
 * @property second Specifies the action to group by or order by.
 */
typealias Filter = Pair<Boolean, Action>

/**
 * Represents a mapping from a string key to a list of items of type T.
 *
 * @param T The type of items in the list.
 */
typealias Mapped<T> = Map<CharSequence, List<T>>

/**
 * Retrieves the [SafeArgs] object of type[T] associated with the given [SafeArgs] instance
 * from this [SavedStateHandle].
 *
 * @param route The [SafeArgs] instance representing the navigation route and arguments.
 * @return The args object of type [T] containing the deserialized arguments.
 **/
operator fun <T> SavedStateHandle.get(route: ArgsFactory<T>): T = route.build(this)



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


/**
 * Controls the color of both the status bar and the navigation bar.
 *
 * - When setting a value, it sets the same color for both the status bar and navigation bar.
 * - The color value is converted to an ARGB format using [Color.toArgb].
 *
 * Note: Getting the current system bar color is not supported and will throw an error if accessed.
 *
 * @property value The color to be applied to both system bars.
 * @throws UnsupportedOperationException when trying to retrieve the current system bar color.
 */
var Window.systemBarsColor: Color
    set(value) {
        statusBarColor = value.toArgb()
        navigationBarColor = value.toArgb()
    }
    get() = error("Not supported!")

private const val ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)

/**
 * Ellipsizes this CharSequence, adding a horizontal ellipsis (…) if it is longer than [after] characters.
 *
 * @param after The maximum length of the CharSequence before it is ellipsized.
 * @return The ellipsized CharSequence.
 */
fun CharSequence.ellipsize(after: Int): CharSequence =
    if (this.length > after) this.substring(0, after) + ELLIPSIS_NORMAL else this

/**
 * Operator function to retrieve a value from [SavedStateHandle] using a specified key.
 * It supports type-safe retrieval for common types such as String, Int, Long, Float, Boolean, and Uri.
 *
 * This function uses [SavedStateHandle.get] to retrieve the value and then casts or converts it
 * to the specified type [T]. If the value cannot be converted, it returns null.
 *
 * @param T The reified type to which the retrieved value should be converted.
 * @param key The key under which the value is stored in [SavedStateHandle].
 * @return The value associated with the key, cast to type [T], or null if the value is not found or cannot be cast.
 * @throws IllegalArgumentException if an unsupported type is requested.
 */
inline operator fun <reified T : Any> SavedStateHandle.invoke(key: String): T? {
    // Retrieve the value from SavedStateHandle as String.
    val value = get<String>(key) ?: return null
    // Check the requested type.
    val result = when (T::class) {
        // If String, return the value directly.
        String::class -> value
        Int::class -> value.toInt()
        Long::class -> value.toLong()
        Float::class -> value.toFloat()
        Boolean::class -> value.toBoolean()
        Uri::class -> Uri.parse(value)
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
    // Safely cast the result to the requested type.
    return result as T
}