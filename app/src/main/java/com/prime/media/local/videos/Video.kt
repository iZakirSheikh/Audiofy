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

import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.primex.core.SignalWhite
import com.primex.core.withSpanStyle
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.zs.core.store.Video
import com.zs.core.util.PathUtils
import com.zs.core_ui.AppTheme
import coil.compose.rememberAsyncImagePainter as AsyncPainter
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "Video"

private val ITEM_PADDING = PaddingValues(horizontal = 0.dp, vertical = CP.medium)
private val THUMBNAIL_SIZE = DpSize(128.dp, 72.dp)

/**
 * Represents the [Video] list item.
 */
@Composable
fun Video(
    value: Video,
    modifier: Modifier = Modifier
) {
    ListTile(
        // Title
        overline = {
            Label(
                value.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                style = AppTheme.typography.bodyMedium
            )
        },
        // Path
        headline = {
            Text(
                PathUtils.parent(value.path),
                overflow = TextOverflow.StartEllipsis,
                maxLines = 1,
                style = AppTheme.typography.caption,
                modifier = Modifier.padding(vertical = CP.small),
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        },
        // Properties
        subtitle = {
            val ctx = LocalContext.current
            val color =
                if (AppTheme.colors.isLight) AppTheme.colors.accent else AppTheme.colors.onBackground
            Label(
                buildAnnotatedString {
                    withSpanStyle(color, background = color.copy(0.12f)) {
                        append(" ${Formatter.formatShortFileSize(ctx, value.size)} ")
                    }
                    append("  ")
                    withSpanStyle(color, background = color.copy(0.12f)) {
                        append(" ${value.height}p ")
                    }
                },
                fontWeight = FontWeight.SemiBold,
                style = AppTheme.typography.caption
            )
        },
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .then(modifier),
        padding = ITEM_PADDING,
        leading = {
            // Thumbnail
            Box(
                modifier = Modifier
                    .clip(AppTheme.shapes.compact)
                    .size(THUMBNAIL_SIZE),
                content = {
                    // Thumbnail
                    Image(
                        AsyncPainter(value.contentUri),
                        contentDescription = value.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    // Duration
                    Label(
                        " ${android.text.format.DateUtils.formatElapsedTime(value.duration / 1000)} ",
                        modifier = Modifier
                            .padding(end = CP.small, bottom = CP.small)
                            .background(Color.Black.copy(0.36f), AppTheme.shapes.small)
                            .padding(1.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.SignalWhite,
                        style = AppTheme.typography.caption2,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    )
}