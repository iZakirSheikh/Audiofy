/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 16-11-2024.
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

package com.prime.media.local.videos

import android.app.Activity
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import com.prime.media.R
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.Route
import com.prime.media.common.menu.Action
import com.zs.core.store.Video
import kotlinx.coroutines.flow.StateFlow

object RouteVideos : Route

/**
 * Represents the view state for the Videos screen.
 *
 * @property data The state flow containing mapped video data.
 * @property query The current text field state for filtering videos.
 * @property order The current filter applied to the video list.
 * @property actions The list of available actions that can be performed.
 * @property orders The list of possible orderings for the video list.
 */
interface VideosViewState {

    companion object {
        val ACTION_RENAME = Action(R.string.rename, Icons.Outlined.Edit)
        val ACTION_DELETE = Action(R.string.delete,Icons.Outlined.Delete)
        val ACTION_SHARE = Action(R.string.share,  Icons.Outlined.Share)
    }

    val data: StateFlow<Mapped<Video>?>
    val query: TextFieldState
    val order: Filter
    val actions: List<Action>
    val orders: List<Action>

    /**
     * Filters the video list based on the given ascending flag and order action.
     *
     * @param ascending Whether the videos should be ordered in ascending order.
     * @param order The action representing the order to apply.
     */
    fun filter(ascending: Boolean = this.order.first, order: Action = this.order.second)

    /**
     *
     */
    fun delete(activity: Activity, video: Video)

    /**
     *
     */
    fun share(activity: Activity, file: Video)

    /**
     * Plays the specified video.
     *
     * @param video The video to be played.
     */
    fun play(video: Video)
}
