package com.prime.media.core

import androidx.compose.ui.graphics.vector.ImageVector
import com.primex.core.Text

/**
 * Represents the route/screen in the [NavGraph]
 */
interface Route {

    /**
     * The title of this [Route]
     */
    val title: Text

    /**
     * The representational [icon] of this [Route]
     */
    val icon: ImageVector

    /**
     * The structure of the path to the [Route].
     */
    val route: String
}