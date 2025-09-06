/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.editor

import android.app.Activity
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.zs.audiofy.R
import com.zs.audiofy.common.IAP_TAG_EDITOR_PRO
import com.zs.audiofy.common.compose.FloatingLargeTopAppBar
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.purchase
import com.zs.audiofy.common.compose.rememberAcrylicSurface
import com.zs.audiofy.common.compose.source
import com.zs.compose.foundation.MetroGreen
import com.zs.compose.foundation.composableIf
import com.zs.compose.foundation.foreground
import com.zs.compose.foundation.fullLineSpan
import com.zs.compose.foundation.plus
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.TextButton
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.WindowSize
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.SinglePaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.OutlinedTextField
import com.zs.compose.theme.text.Text
import com.zs.compose.theme.text.TextFieldDefaults
import com.zs.core.billing.Paymaster
import com.zs.core.billing.purchased
import androidx.activity.compose.rememberLauncherForActivityResult as Launcher
import androidx.activity.result.PickVisualMediaRequest as Pick
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.layout.WindowInsetsSides as WIS
import com.zs.audiofy.common.compose.ContentPadding as CP

/**
 * A VisualTransformation that applies styling to timestamps in the [MM:SS:XX] format within text.
 *
 * @property text The input text containing timestamps to be styled.
 * @return A transformed AnnotatedString with timestamps styled in bold.
 */
private val LRCVisualTransformation =
    VisualTransformation { text ->
        // Define a regular expression to match the [MM:SS.XX] format
        val regex = """\[\d{2}:\d{2}.\d{2}\]""".toRegex()
        // Create a new AnnotatedString builder to store the transformed text
        val builder = AnnotatedString.Builder()
        // Loop through the text and append the matched and unmatched parts with different styles
        var cursor = 0
        regex.findAll(text.text).forEach { matchResult ->
            // Append the unmatched part with the original style
            builder.append(text.subSequence(cursor, matchResult.range.first))
            // Append the matched part with a bold style
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            builder.append(text.subSequence(matchResult.range))
            builder.pop()
            // Update the cursor position
            cursor = matchResult.range.last + 1
        }
        // Append the remaining part of the text with the original style
        builder.append(text.subSequence(cursor, text.length))
        // Return the transformed text with an identity offset mapping
        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }


private val PickArtwork =
    Pick(ActivityResultContracts.PickVisualMedia.ImageOnly)


private val DefaultKeyboardOptions =
    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)

/**
 * Represents the header of the editor screen. it is some details placed next to artwork.
 */
@Composable
private fun ExtraInfo(
    isUnlocked: Boolean,
    viewState: EditorViewState,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = CP.LargeArrangement) {
        // Artwork
        val facade = LocalSystemFacade.current
        val launcher =
            Launcher(ActivityResultContracts.PickVisualMedia()) { value ->
                when (value) {
                    is Uri if (isUnlocked) -> viewState.setArtwork(value)
                    null -> return@Launcher // this should not happen
                    else -> facade.showSnackbar(R.string.msg_upgrade_to_pro)
                }
            }
        IconButton(onClick = { launcher.launch(PickArtwork) }) {
            IconButton(onClick = { launcher.launch(PickArtwork) }) {
                Image(
                    painter = when (val value = viewState.artwork) {
                        null -> rememberVectorPainter(image = Icons.Outlined.HideImage)
                        else -> BitmapPainter(value)
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(AppTheme.shapes.xLarge)
                        .background(AppTheme.colors.background(1.dp), AppTheme.shapes.xLarge)
                        .size(180.dp) // different when width > height
                        .foreground(Color.Black.copy(0.3f))
                )
                // Representational Icon.
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        // Info
        Text(
            style = AppTheme.typography.body2,
            modifier = Modifier.weight(0.5f),
            text = viewState.extraInfo ?: ""
        )
    }
}


@Composable
private fun Property(
    @StringRes title: Int,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    @StringRes placeholder: Int = ResourcesCompat.ID_NULL,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = DefaultKeyboardOptions,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Label(text = textResource(id = title)) },
        maxLines = maxLines,
        readOnly = !enabled,
        placeholder = composableIf(placeholder != ResourcesCompat.ID_NULL) {
            Label(text = if (placeholder != ResourcesCompat.ID_NULL) textResource(id = placeholder) else "")
        },
        singleLine = maxLines == 1,
        shape = AppTheme.shapes.small,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = Modifier
            .defaultMinSize(minHeight = 64.dp)
            .onFocusChanged {
                val selection =
                    if (it.hasFocus) TextRange(0, value.text.length) else TextRange.Zero
                onValueChange(value.copy(selection = selection))
            }
    )
}

@Composable
fun Editor(viewState: EditorViewState) {
    val (width, _) = LocalWindowSize.current
    val topAppBarScrollBehavior = AppBarDefaults.exitUntilCollapsedScrollBehavior()
    val inAppNavBarInsets = WindowInsets.content
    val strategy =
        if (width > WindowSize.Category.Medium) HorizontalTwoPaneStrategy(0.5f) else SinglePaneStrategy
    //
    val isProVersion by purchase(Paymaster.IAP_TAG_EDITOR_PRO)
    val facade = LocalSystemFacade.current
    val surface = rememberAcrylicSurface()
    // Layout
    TwoPane(
        strategy = strategy,
        secondary = {
            ExtraInfo(
                isProVersion.purchased,
                viewState,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(WIS.Vertical + WIS.End)
                    )
                    .padding(top = CP.large)
            )
        },
        topBar = {
            val colors = AppTheme.colors
            FloatingLargeTopAppBar(
                title = {
                    Text(
                        textResource(
                            if (isProVersion.purchased)
                                R.string.scr_tag_editor_pro_title
                            else
                                R.string.scr_tag_editor_title
                        ),
                        maxLines = 2,
                        fontWeight = FontWeight.Light,
                        lineHeight = 24.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = topAppBarScrollBehavior,
                background = colors.background(surface),
                insets = WindowInsets.systemBars.only(WIS.Top),
                navigationIcon = {
                    val navController = LocalNavController.current
                    IconButton(
                        Icons.AutoMirrored.Outlined.ReplyAll,
                        onClick = navController::navigateUp,
                        contentDescription = null
                    )
                },
                actions = {
                    TonalIconButton(
                        icon = Icons.Outlined.Restore,
                        contentDescription = null,
                        onClick = viewState::reset,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            )
        },
        floatingActionButton = {
            val facade = LocalSystemFacade.current
            FloatingActionButton(
                onClick = { viewState.save(facade as Activity); /*facade.showAd()*/ },
                content = { Icon(imageVector = Icons.Outlined.Save, contentDescription = null) },
                contentColor = AppTheme.colors.onAccent,
                modifier = Modifier.windowInsetsPadding(inAppNavBarInsets.only(WIS.Bottom + WIS.End))
            )
        },
        primary = {
            LazyVerticalGrid(
                horizontalArrangement = CP.LargeArrangement,
                verticalArrangement = CP.SmallArrangement,
                // In immersive mode, add horizontal padding to prevent settings from touching the screen edges.
                // Immersive layouts typically have a bottom app bar, so extra padding improves aesthetics.
                // Non-immersive layouts only need vertical padding.
                contentPadding =
                    Padding(horizontal = CP.large) +
                            (WindowInsets.content.union(WindowInsets.systemBars)
                                .union(inAppNavBarInsets).only(WIS.Vertical))
                                .asPaddingValues(),
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .imePadding()
                    .source(surface)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fadingEdge2(length = 56.dp),
                content = {
                    // Extra Info
                    if (strategy is SinglePaneStrategy)
                        item(span = fullLineSpan, contentType = "extra_info") {
                            ExtraInfo(isProVersion.purchased, viewState)
                        }

                    // Title
                    item(span = fullLineSpan, contentType = "property") {
                        Property(
                            title = R.string.title,
                            value = viewState.title,
                            onValueChange = { viewState.title = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder
                        )
                    }

                    // Banner
                    if (!isProVersion.purchased)
                        item("buy_me", contentType = "Buy Me", span = fullLineSpan) {
                            BaseListItem(
                                overline = { Spacer(modifier = Modifier) },
                                heading = { Spacer(modifier = Modifier) },
                                footer = {
                                    TextButton(
                                        text = textResource(id = R.string.scr_tag_editor_screen_buy_now_action),
                                        onClick = { facade.initiatePurchaseFlow(Paymaster.IAP_TAG_EDITOR_PRO) })
                                },
                                contentColor = AppTheme.colors.onBackground,
                                leading = {
                                    Icon(
                                        imageVector = Icons.Outlined.Shop,
                                        contentDescription = null,
                                        tint = Color.MetroGreen
                                    )
                                },
                                subheading = { Text(text = textResource(id = R.string.msg_tag_editor_buy_me_banner)) },
                                modifier = Modifier.background(
                                    color = AppTheme.colors.background(1.dp),
                                    AppTheme.shapes.small
                                )
                            )
                        }

                    // Album
                    item(contentType = "property") {
                        Property(
                            title = R.string.album,
                            value = viewState.album,
                            onValueChange = { viewState.album = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder
                        )
                    }
                    // Artist
                    item(contentType = "property") {
                        Property(
                            title = R.string.artist,
                            value = viewState.artist,
                            onValueChange = { viewState.artist = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder
                        )
                    }
                    // Album Artist
                    item(contentType = "property") {
                        Property(
                            title = R.string.album_artist,
                            value = viewState.albumArtist,
                            onValueChange = { viewState.albumArtist = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder
                        )
                    }
                    // Composer
                    item(contentType = "property") {
                        Property(
                            title = R.string.composer,
                            value = viewState.composer,
                            onValueChange = { viewState.composer = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder
                        )
                    }
                    // Genre
                    item(contentType = "property") {
                        Property(
                            title = R.string.genre,
                            value = viewState.genre,
                            onValueChange = { viewState.genre = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                        )
                    }

                    // comment
                    item(span = fullLineSpan, contentType = "property") {
                        Property(
                            title = R.string.comment,
                            value = viewState.comment,
                            onValueChange = { viewState.comment = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                        )
                    }

                    // Original Artist
                    item(contentType = "property") {
                        Property(
                            title = R.string.original_artist,
                            value = viewState.originalArtist,
                            onValueChange = { viewState.originalArtist = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder
                        )
                    }

                    // Publisher
                    item(contentType = "property") {
                        Property(
                            title = R.string.publisher,
                            value = viewState.publisher,
                            onValueChange = { viewState.publisher = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder
                        )
                    }

                    // Copywriter
                    item(contentType = "property") {
                        Property(
                            title = R.string.copywriter,
                            value = viewState.copyright,
                            onValueChange = { viewState.copyright = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                        )
                    }
                    // number
                    val KeyboardTypeNumber =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    // Year
                    item(contentType = "property") {
                        Property(
                            title = R.string.year,
                            value = viewState.year,
                            onValueChange = { viewState.year = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                            keyboardOptions = KeyboardTypeNumber
                        )
                    }
                    // Track Number
                    item(contentType = "property") {
                        Property(
                            title = R.string.track_number,
                            value = viewState.trackNumber,
                            onValueChange = { viewState.trackNumber = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                            keyboardOptions = KeyboardTypeNumber
                        )
                    }
                    // Url
                    item(contentType = "property") {
                        Property(
                            title = R.string.url,
                            value = viewState.url,
                            onValueChange = { viewState.url = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                            //   visualTransformation = AffixVisualTransformation(ctx.resources.getText2(R.string.tag_editor_web_address_prefix))
                        )
                    }
                    // Disk Number
                    item(contentType = "property") {
                        Property(
                            title = R.string.disk_number,
                            value = viewState.diskNumber,
                            onValueChange = { viewState.diskNumber = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                            keyboardOptions = KeyboardTypeNumber
                        )
                    }
                    // Total Disks
                    item(contentType = "property") {
                        Property(
                            title = R.string.total_disks,
                            value = viewState.totalDisks,
                            onValueChange = { viewState.totalDisks = it },
                            placeholder = R.string.scr_tag_editor_property_placeholder,
                            keyboardOptions = KeyboardTypeNumber
                        )
                    }

                    // Header
                    item(contentType = "header", span = fullLineSpan) {
                        Header(
                            stringResource(R.string.lyrics),
                            style = AppTheme.typography.title3,
                            color = AppTheme.colors.accent,
                            modifier = Modifier.padding(top = CP.medium)
                        )
                    }

                    // Lyrics
                    item(contentType = "lyrics", span = fullLineSpan) {
                        OutlinedTextField(
                            value = viewState.lyrics,
                            onValueChange = { viewState.lyrics = it },
                            minLines = 5,
                            maxLines = 8,
                            shape = AppTheme.shapes.small,
                            readOnly = !isProVersion.purchased,
                            visualTransformation = LRCVisualTransformation,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = AppTheme.colors.background(1.dp),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            textStyle = AppTheme.typography.body2,
                            placeholder = {
                                Text(
                                    textResource(id = R.string.scr_tag_editor_lyrics_placeholder),
                                    style = AppTheme.typography.body2
                                )
                            },
                        )
                    }
                }
            )
        }
    )
}