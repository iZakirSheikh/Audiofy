/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 26-10-2024.
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonDefaults.OutlinedBorderOpacity
import androidx.compose.material.ButtonDefaults.OutlinedBorderSize
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.prime.media.common.menu.Action
import com.prime.media.old.common.LocalNavController
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.primex.material2.Text
import com.primex.material2.appbar.CollapsableTopBarLayout
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.None
import coil.compose.rememberAsyncImagePainter as ImagePainter
import com.primex.core.textResource as stringResource
import com.primex.material2.appbar.TopAppBarDefaults as Defaults
import com.zs.core_ui.ContentElevation as CE
import com.zs.core_ui.ContentPadding as CP

/**
 * Represents the state of information about the data being shown to the user.
 *
 * @property title The title of the dataset. Can be up to two lines.
 * @property subtitle An optional secondary text, displayed on a single line.
 * @property desc An optional description text.
 * @property extra Extra information text, such as count or size. Each unit comprises two lines: data and label.
 * @property artwork An optional artwork image URL or path associated with the data.
 * @property options A list of options (e.g., delete, share, search) supported by the data.
 * The first two elements represent primary actions and might be null if not available.
 */
data class MetaData(
    val title: CharSequence,
    val subtitle: CharSequence? = null,
    val desc: CharSequence? = null,
    val extra: List<CharSequence> = emptyList(),
    val artwork: String? = null,
    val options: List<Action?> = emptyList()
)

private val HeaderArtWorkShape = RoundedCornerShape(12)

@Composable
@NonRestartableComposable
private fun com.zs.core_ui.Typography.style(fraction: Float): TextStyle {
    return androidx.compose.ui.text.lerp(titleMedium, headlineMedium, fraction)
}

private val IN_SPACE = Arrangement.spacedBy(12.dp)
private val Colors.topBarBrush
    @Composable
    inline get() = Brush.verticalGradient(listOf(background(10.dp), Color.Transparent))

/**
 * Preview
 * ```
 * ┌────────────────────────────────────────┐
 * │ ◂ │ Title/Subtitle               │ ... │
 * ├────────────────────────────────────────┤
 * │ [Artwork]  Title                       │
 * │            Extra Info (Scrollable)     │
 * │            Button 1  | Button 2        │
 * ├────────────────────────────────────────┤
 * │     Description (Optional)             │
 * └────────────────────────────────────────┘
 * ```
 */
@Composable
fun MetaDataTopAppBar(
    value: MetaData? = null,
    onAction: (action: Action) -> Unit,
    insets: WindowInsets = WindowInsets.None,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier
) {
    // If metadata is not provided, display a spacer.
    if (value == null) return Spacer(modifier)
    val height = (280 + if (!value.desc.isNullOrBlank()) 60 else 0).dp
    CollapsableTopBarLayout(
        56.dp,
        height,
        insets = insets,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        content = {
            // Back button
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.Outlined.ArrowBack,
                onClick = navController::navigateUp,
                modifier = Modifier.layoutId(Defaults.LayoutIdNavIcon)
            )

            // Collapsable title (supports 2 lines for subtitle).
            Label(
                maxLines = 2,
                text = value.title,
                style = AppTheme.typography.style(fraction),
                modifier = Modifier
                    .offset { IntOffset(124.dp.roundToPx(), 50.dp.roundToPx()) * fraction }
                    .road(Alignment.CenterStart, Alignment.TopStart)
                    .layoutId(Defaults.LayoutIdCollapsable_title)
            )

            // Secondary actions (e.g., more options).
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .layoutId(Defaults.LayoutIdAction)
                    .padding(end = Defaults.TopAppBarHorizontalPadding),
                content = {
                    for (i in 2 until value.options.size) { // Iterate through secondary actions.
                        val option = value.options[i] ?: continue // Skip null actions.
                        IconButton(
                            imageVector = option.icon!!,
                            onClick = { onAction(option) },
                            enabled = option.enabled
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .background(AppTheme.colors.topBarBrush)
                    .layoutId(TopAppBarDefaults.LayoutIdBackground)
                    .fillMaxSize()
                    // This ensures layout's is not clipped while collapsing.
                    .requiredHeightIn(min = height)
                    // The top padding is below actual appbar; it includes padding for status bar.
                    .windowInsetsPadding(insets)
                    .padding(start = CP.large, end = CP.normal, top = 50.dp)
                    .graphicsLayer {
                        // Fade out during collapse.
                        alpha = lerp(1f, 0f, ((1f - fraction) / 0.50f).coerceIn(0f, 1f))
                    },
                verticalArrangement = IN_SPACE,
                content = {
                    // Background and content of the expanded top app bar.
                    if (fraction == 0f) return@Column
                    // Artwork, title, and primary buttons.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        IN_SPACE,
                        content = {
                            // Image (artwork).
                            Image(
                                painter = ImagePainter(value.artwork),
                                contentDescription = value.title.toString(),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .shadow(CE.high, HeaderArtWorkShape)
                                    .size(76.dp, 132.dp)
                            )

                            // Content to the right of the artwork (title, buttons).
                            Column(
                                verticalArrangement = IN_SPACE,
                                modifier = Modifier.padding(horizontal = CP.normal),
                                content = {
                                    // Extra info row (may be scrollable).
                                    Row(
                                        modifier = Modifier.height(IntrinsicSize.Min).padding(top = 50.dp),
                                        horizontalArrangement = IN_SPACE,
                                        content = {
                                            for (i in 0 until value.extra.size) {
                                                val info = value.extra[i]
                                                Label(
                                                    text = info,
                                                    maxLines = 2,
                                                    style = AppTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                if (i == value.extra.lastIndex) continue
                                                // Divider 2
                                                Divider(
                                                    Modifier
                                                        .width(1.dp)
                                                        .fillMaxHeight(0.8f)
                                                )
                                            }
                                        }
                                    )

                                    // Primary buttons.
                                    Row(
                                        horizontalArrangement = IN_SPACE,
                                        content = {
                                            // primary button
                                            val option1 = value.options[0]
                                            if (option1 != null)
                                                OutlinedButton(
                                                    label = stringResource(option1.label),
                                                    onClick = { onAction(option1) },
                                                    icon = rememberVectorPainter(image = option1.icon!!),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        backgroundColor = Color.Transparent
                                                    ),
                                                    border = BorderStroke(
                                                        OutlinedBorderSize,
                                                        AppTheme.colors.accent.copy(alpha = OutlinedBorderOpacity)
                                                    ),
                                                    shape = AppTheme.shapes.compact,
                                                )

                                            val option2 = value.options[1]
                                            if (option2 != null)
                                                Button(
                                                    label = stringResource(option2.label),
                                                    onClick = { onAction(option2) },
                                                    icon = rememberVectorPainter(image = Icons.Rounded.PlaylistPlay),
                                                    // modifier = Modifier.weight(1f),
                                                    elevation = null,
                                                    shape = AppTheme.shapes.compact,
                                                    colors = ButtonDefaults.buttonColors(
                                                        backgroundColor = AppTheme.colors.background(
                                                            3.dp
                                                        )
                                                    )
                                                )
                                        }
                                    )
                                }
                            )
                        }
                    )

                    // Description (displayed if available).
                    if (!value.desc.isNullOrBlank()) {
                        Text(
                            value.desc,
                            maxLines = 3,
                            style = AppTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .padding(horizontal = CP.medium)
                                .background(AppTheme.colors.background(3.dp), AppTheme.shapes.compact)
                                .padding(12.dp)
                                .fillMaxWidth(),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
    )
}