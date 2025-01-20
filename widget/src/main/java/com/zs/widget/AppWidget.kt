/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 15-01-2025.
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

package com.zs.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.PlaybackController
import androidx.glance.GlanceId as ID

private const val TAG = "AppWidget"

class AppWidget : GlanceAppWidgetReceiver() {

    private class RealWidget() : GlanceAppWidget() {

        override val sizeMode: SizeMode = SizeMode.Exact

        override suspend fun provideGlance(context: Context, id: ID) {
            provideContent {
                GlanceTheme {
                    val type = LocalSize.current.let { (width, height) ->
                        when {
                            width > 300.dp -> ViewType.NORMAL
                            height > 100.dp -> ViewType.SQUARE
                            else -> ViewType.COMPACT
                        }
                    }
                    // Observe the playback state
                    val state by remember { PlaybackController.observe(context) }
                        .collectAsState(NowPlaying.EMPTY)
                    Log.d(TAG, "$type")
                    Log.d(TAG, "State: $state")
                    Universal(state, type)
                }
            }
        }
    }

    override val glanceAppWidget: GlanceAppWidget = RealWidget()
}
