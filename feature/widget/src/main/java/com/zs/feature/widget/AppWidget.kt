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

package com.zs.feature.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import com.zs.feature.widget.common.LocalRemote
import com.zs.feature.widget.common.ViewType

private val NonePlaying = NowPlaying(null, null)

private const val TAG = "AppWidget"

private class GlanceWidget() : GlanceAppWidget() {
    private lateinit var remote: Remote

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "ProvideGlance: ID: $id")
        // TODO - Investigate how to release the Remote instance.
        if (!::remote.isInitialized) {
            remote = Remote(context.applicationContext)
        }
        // TODO - Investigate why we need trigger both widgetupdate as well as state update
        provideContent {
            // Determine the widget's view type based on its current size.
            // LocalSize.current provides the width and height of the widget.
            val type = LocalSize.current.let { (width, height) ->
                when {
                    // If the width is greater than 300dp, use the NORMAL view type.
                    width > 300.dp -> ViewType.NORMAL
                    // If the height is greater than 100dp (and width is not > 300dp), use the SQUARE view type.
                    height > 100.dp -> ViewType.SQUARE
                    // Otherwise, use the COMPACT view type.
                    else -> ViewType.COMPACT
                }
            }
            val state by remote.state.collectAsState()
            Log.d(TAG, "provideGlance: ViewType: $type")
            CompositionLocalProvider(LocalRemote provides remote) {
                GlanceTheme {
                    Universal(state ?: NonePlaying, type)
                }
            }
        }
    }
}

internal class AppWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceWidget()
}
