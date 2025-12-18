/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 11-03-2025.
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

package com.prime.media.personalize

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.isFreemium
import com.prime.media.common.purchase
import com.prime.media.common.Registry
import com.primex.core.ImageBrush
import com.primex.core.fadingEdge
import com.primex.core.visualEffect
import com.primex.material2.Label
import com.zs.core.paymaster.purchased
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding
import com.zs.core.paymaster.ProductInfo as Product

private val Keys = arrayOf(
    BuildConfig.IAP_ARTWORK_SHAPE_LEAF,
    BuildConfig.IAP_ARTWORK_SHAPE_HEART,
    BuildConfig.IAP_ARTWORK_SHAPE_CIRCLE,
    BuildConfig.IAP_ARTWORK_SHAPE_ROUNDED_RECT,
    BuildConfig.IAP_ARTWORK_SHAPE_CUT_CORNORED_RECT,
    BuildConfig.IAP_ARTWORK_SHAPE_SCOPED_RECT,
    BuildConfig.IAP_ARTWORK_SHAPE_SQUIRCLE,
    BuildConfig.IAP_ARTWORK_SHAPE_WAVY_CIRCLE,
    BuildConfig.IAP_ARTWORK_SHAPE_DISK,
    BuildConfig.IAP_ARTWORK_SHAPE_PENTAGON,
    BuildConfig.IAP_ARTWORK_SHAPE_SKEWED_RECT
)

private val MAX_WIDTH = 90.dp

@Composable
fun ArtworkShapeRow(
    artwork: Uri?,
    selected: String,
    details: Map<String, Product>,
    modifier: Modifier = Modifier,
    onRequestApply: (id: String) -> Unit
) {
    val facade = LocalSystemFacade.current
    val purchasableChipColors = ChipDefaults.outlinedChipColors(
        backgroundColor = Color.Transparent,
        contentColor = AppTheme.colors.accent
    )
    val selectedChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = AppTheme.colors.accent,
        selectedContentColor = AppTheme.colors.onAccent,
        backgroundColor = AppTheme.colors.background(3.dp),
        contentColor = AppTheme.colors.accent
    )

    val state = rememberLazyListState()
    // Content
    LazyRow(
        modifier = modifier.fadingEdge(state, true),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal),
        content = {
            items(
                items = Keys,
                key = { it },
                itemContent = { key ->
                    Column(
                        modifier = Modifier.width(MAX_WIDTH),
                        content = {
                            val shape = Registry.mapKeyToShape(key)
                            AsyncImage(
                                artwork,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .border(1.dp, LocalContentColor.current, shape)
                                    .shadow(ContentElevation.medium, shape)
                                    .visualEffect(ImageBrush.NoiseBrush, 0.5f, true)
                                    .background(AppTheme.colors.background(1.dp)),
                            )

                            val detail = details[key]
                            // Title
                            Label(
                                detail?.title
                                    ?: androidx.compose.ui.res.stringResource(R.string.abbr_not_available),
                                style = AppTheme.typography.caption,
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp)
                            )

                            val purchase by purchase(key)
                            when {
                                !purchase.purchased && detail?.isFreemium == false -> Chip(
                                    onClick = { facade.initiatePurchaseFlow(key) },
                                    content = { Label("${detail.formattedPrice}") },
                                    shape = AppTheme.shapes.compact,
                                    colors = purchasableChipColors,
                                    border = ChipDefaults.outlinedBorder
                                )

                                else -> FilterChip(
                                    selected = selected == key,
                                    onClick = { onRequestApply(key) },
                                    content = { Label(androidx.compose.ui.res.stringResource(R.string.apply)) },
                                    shape = AppTheme.shapes.compact,
                                    colors = selectedChipColors
                                )
                            }
                        }
                    )
                }
            )
        }
    )
}
