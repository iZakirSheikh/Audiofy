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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.menu.Action
import com.primex.core.SignalWhite
import com.primex.core.rememberVectorPainter
import com.primex.core.textResource
import com.primex.core.withSpanStyle
import com.primex.material2.DropDownMenuItem
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.primex.material2.menu.DropDownMenu2
import com.zs.core.store.Video
import com.zs.core.util.PathUtils
import com.zs.core_ui.AppTheme
import com.zs.core_ui.AppTheme.colors
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
    actions: List<Action>,
    onAction: (action: Action?, value: Video) -> Unit,
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
                    withSpanStyle(color.copy(ContentAlpha.medium), background = color.copy(0.12f)) {
                        append(" ${Formatter.formatShortFileSize(ctx, value.size)} ")
                    }
                    append("  ")
                    withSpanStyle(color.copy(ContentAlpha.medium), background = color.copy(0.12f)) {
                        append(" ")
                        append(stringResource(R.string.postfix_p_s, value.height))
                        append(" ")
                    }
                },
                fontWeight = FontWeight.SemiBold,
                style = AppTheme.typography.caption2
            )
        },
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .clickable { onAction(null, value) }
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
        },
        trailing = {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = !expanded }) {
                // icon
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "more")
                // menu
                DropDownMenu2(
                    expanded,
                    onDismissRequest = { expanded = false },
                    shape = AppTheme.shapes.compact,
                    elevation = 9.dp,
                    modifier = Modifier.scale(0.95f),
                    //  backgroundColor = Color.Transparent,
                    border = if (AppTheme.colors.isLight) null else BorderStroke(
                        0.5.dp,
                        colors.background(20.dp)
                    ),
                    content = {
                        actions.forEach { action ->
                            DropDownMenuItem(
                                title = textResource(action.label),
                                icon = rememberVectorPainter(action.icon!!),
                                enabled = action.enabled,
                                modifier = Modifier.widthIn(min = 170.dp),
                                onClick = {
                                    onAction(action, value)
                                    expanded = false
                                }
                            )
                        }
                    }
                )
            }
        }
    )
}