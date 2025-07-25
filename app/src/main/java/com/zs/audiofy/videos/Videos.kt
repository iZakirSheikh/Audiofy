/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

package com.zs.audiofy.videos


import android.app.Activity
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.INFO
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.LottieAnimatedIcon
import com.zs.audiofy.common.compose.OverflowMenu
import com.zs.audiofy.common.compose.directory.Files
import com.zs.audiofy.properties.RouteProperties
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.common.PathUtils
import com.zs.core.store.models.Video
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.combinedClickable as clickable
import com.zs.audiofy.common.compose.ContentPadding as CP

private const val TAG = "Video"

private val VideoItemPadding = PaddingValues(horizontal = ContentPadding.large)
private val VideoThumbnailModifier = Modifier.size(128.dp, 72.dp)

/**
 * Represents the [Video] list item.
 */
@Composable
private fun Video(
    value: Video,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseListItem(
        modifier = modifier,
        trailing = actions,
        padding = VideoItemPadding,
        centerAlign = true,
        // Title
        overline = {
            Label(
                value.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                style = AppTheme.typography.body2
            )
        },
        // Path
        heading = {
            Text(
                PathUtils.parent(value.path),
                overflow = TextOverflow.StartEllipsis,
                maxLines = 1,
                style = AppTheme.typography.label2,
                modifier = Modifier.padding(vertical = CP.small),
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        },
        // Properties
        subheading = {
            val ctx = LocalContext.current
            val color = AppTheme.colors.onBackground
            val style =
                SpanStyle(color.copy(ContentAlpha.medium), background = color.copy(0.12f))
            Label(
                buildAnnotatedString {
                    withStyle(style) {
                        append(" ${Formatter.formatShortFileSize(ctx, value.size)} ")
                    }
                    append("  ")
                    withStyle(style) {
                        append(" ")
                        append(stringResource(R.string.pixels_d, value.height))
                        append(" ")
                    }
                },
                fontWeight = FontWeight.SemiBold,
                style = AppTheme.typography.label3
            )
        },
        // Thumbnail
        leading = {
            Box(
                modifier = Modifier.clip(AppTheme.shapes.small) then VideoThumbnailModifier,
                content = {
                    // Thumbnail
                    Image(
                        rememberAsyncImagePainter(value.contentUri),
                        contentDescription = value.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    // Duration
                    Label(
                        " ${DateUtils.formatElapsedTime(value.duration / 1000)} ",
                        modifier = Modifier
                            .padding(end = CP.small, bottom = CP.small)
                            .background(Color.Black.copy(0.36f), AppTheme.shapes.small)
                            .padding(1.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.SignalWhite,
                        style = AppTheme.typography.label3,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    )
}

@Composable
fun Videos(viewState: VideosViewState) {
    val selected = viewState.selected
    val favourites by viewState.favourites.collectAsState(emptySet())
    val facade = LocalSystemFacade.current
    val navController = LocalNavController.current
    val surface = rememberHazeState()

    Files(
        viewState,
        surface = surface,
        onTapAction = { viewState.onPerformAction(it, facade as Activity) },
        key = Video::id,
        itemContent = { video ->
            Video(
                value = video,
                modifier = Modifier
                    .animateItem()
                    .clickable(
                        onClick = {
                            if (viewState.isInSelectionMode) viewState.select(video.id) else viewState.play(video)
                        },
                        onLongClick = { viewState.select(video.id) }
                    ),
                // actions
                actions = {
                    // show checkbox
                    if (viewState.isInSelectionMode)
                        return@Video LottieAnimatedIcon(
                            R.raw.lt_checkbox,
                            animationSpec = AppTheme.motionScheme.slowSpatialSpec(),
                            atEnd = video.id in selected, // if fav
                            contentDescription = null,
                            progressRange = 0.05f..0.30f,
                            scale = 1.6f,
                            tint = AppTheme.colors.accent,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .padding(end = ContentPadding.medium)
                        )

                    // show actions
                    Row {
                        // Heart
                        LottieAnimatedButton(
                            R.raw.lt_twitter_heart_filled_unfilled,
                            onClick = { viewState.toggleLiked(video) },
                            animationSpec = tween(800),
                            atEnd = video.contentUri.toString() in favourites, // if fav
                            contentDescription = null,
                            progressRange = 0.13f..1.0f,
                            scale = 3.5f,
                            tint = AppTheme.colors.accent,
                        )
                        // More
                        val onPerformAction = {action: Action ->
                            when (action) {
                                Action.INFO -> navController.navigate(RouteProperties(video.path))
                                else -> viewState.onPerformAction(action, facade as Activity, video)
                            }
                        }

                        OverflowMenu(
                            collapsed = 0,
                            items = viewState.actions,
                            expanded = 5,
                            onItemClicked = onPerformAction
                        )
                    }
                }
            )
        }
    )
}