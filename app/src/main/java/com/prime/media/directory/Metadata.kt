/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-08-2024.
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

package com.prime.media.directory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.media.Material
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.Artwork
import com.prime.media.core.util.DateUtils
import com.prime.media.small2
import com.primex.core.stringResource
import com.primex.core.value
import com.primex.material2.Button
import com.primex.material2.Label
import com.primex.material2.OutlinedButton


private val HeaderArtWorkShape = RoundedCornerShape(20)

/**
 * A composable that display the [value] of the list of directory.
 *
 * @param value The metadata for an item in the directory.
 * @param modifier A modifier to apply to the metadata composable.
 *
 * Usage:
 * ```
 * Metadata(
 *     value = itemMetadata,
 *     modifier = Modifier.padding(8.dp)
 * )
 * ```
 */
@Composable
fun <T : Any> Metadata(
    resolver: DirectoryViewModel<T>,
    onPerformAction: (action: Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    // return spacer is meta is null
    val meta = resolver.meta ?: return Spacer(modifier = modifier)
    // What is the meta?
    // A MetaData provides additional info regarding the directory.
    // The 2nd two slots of mActions are filled by this.
    // TODO: Future versions might animate between vertical/horizontal.
    Column(
        verticalArrangement = Arrangement.spacedBy(ContentPadding.large),
        modifier = modifier.padding(ContentPadding.large),
        content = {
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal),
                content = {
                    // Artwork.
                    // Because this composable only is hown when artwork isn't null; so
                    Artwork(
                        data = meta.artwork ?: "",
                        modifier = Modifier
                            .shadow(ContentElevation.high, HeaderArtWorkShape)
                            .background(Material.colors.surface)
                            .width(76.dp)
                            .aspectRatio(0.61f)
                    )

                    // title + info
                    Column(
                        verticalArrangement = Arrangement.Top,
                        content = {
                            // Title
                            // since meta is Text hence annotated string can be used to populate subtitle.
                            Label(
                                text = stringResource(value = meta.title),
                                style = Material.typography.h4,
                                maxLines = 2,
                                textAlign = TextAlign.Start,
                            )

                            Row(
                                modifier = Modifier.height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal),
                                content = {
                                    //Tracks
                                    val count = meta.cardinality
                                    val color = LocalContentColor.current
                                    Text(
                                        text = buildAnnotatedString {
                                            append("$count\n")
                                            withStyle(
                                                SpanStyle(
                                                    color = color.copy(ContentAlpha.medium),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    baselineShift = BaselineShift(0.3f)
                                                )
                                            ) {
                                                append("Files")
                                            }
                                        },
                                        textAlign = TextAlign.Center,
                                        style = Material.typography.h6,
                                        fontWeight = FontWeight.SemiBold,
                                    )

                                    //Divider 2
                                    Divider(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                    )

                                    val date =
                                        if (meta.dateModified == -1L) "N/A" else DateUtils.formatAsRelativeTimeSpan(
                                            meta.dateModified
                                        )
                                    Text(
                                        text = buildAnnotatedString {
                                            append("$date\n")
                                            withStyle(
                                                SpanStyle(
                                                    color = color.copy(ContentAlpha.medium),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    baselineShift = BaselineShift(0.3f)
                                                )
                                            ) {
                                                append("Last Updated")
                                            }
                                        },
                                        textAlign = TextAlign.Center,
                                        style = Material.typography.h6,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(0.7f),
                                    )
                                }
                            )
                        }
                    )
                }
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal),
                content = {
                    val actions = resolver.mActions
                    val second = actions.getOrNull(2)
                    if (second != null)
                        OutlinedButton(
                            label = stringResource(value = second.title),
                            onClick = { onPerformAction(second) },
                            icon = rememberVectorPainter(image = second.icon),
                            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                            border = ButtonDefaults.outlinedBorder,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(11.dp),
                            shape = Material.shapes.small2
                        )

                    val first = actions.getOrNull(1)
                    if (first != null)
                        Button(
                            label = stringResource(value = first.title),
                            onClick = { onPerformAction(first) },
                            icon = rememberVectorPainter(image = first.icon),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(9.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 0.dp
                            ),
                            shape = Material.shapes.small2
                        )
                }
            )
        }
    )
}