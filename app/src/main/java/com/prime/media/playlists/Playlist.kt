/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 19-10-2024.
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

@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.playlists

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonDefaults.OutlinedBorderOpacity
import androidx.compose.material.ButtonDefaults.OutlinedBorderSize
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.MetaData
import com.prime.media.common.MetaDataTopAppBar
import com.primex.core.textResource
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.primex.material2.Text
import com.primex.material2.appbar.TopAppBarDefaults
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets

private const val TAG = "Playlist"

@Composable
fun Playlist(viewState: PlaylistViewState) {
    val data by viewState.data.collectAsState(null)

    TwoPane(
        primary = {
            Log.d(TAG, "Playlist: ${data?.values?.joinToString(" ")}")
        },
        topBar = {
            val meta = viewState.metadata
            MetaDataTopAppBar(
                meta,
                onAction = {},
                insets = WindowInsets.statusBars,
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            )
        }
    )
}

