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

import androidx.compose.foundation.text.input.TextFieldState
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.Route
import com.prime.media.common.menu.Action
import com.zs.core.store.Video
import kotlinx.coroutines.flow.StateFlow

object RouteVideos : Route

interface VideosViewState {
    val orders: List<Action>
    val data: StateFlow<Mapped<Video>?>

    // filter.
    val query: TextFieldState
    val order: Filter

    // actions
    fun filter(ascending: Boolean = this.order.first, order: Action = this.order.second)
    fun play(video: Video)
}