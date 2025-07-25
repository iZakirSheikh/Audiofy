/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
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

package com.zs.audiofy.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.rememberAcrylicSurface
import com.zs.audiofy.common.compose.source
import com.zs.audiofy.settings.RouteSettings
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.background
import com.zs.compose.foundation.lerp
import com.zs.compose.foundation.plus
import com.zs.compose.foundation.textResource
import com.zs.compose.foundation.thenIf
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.AppTheme.colors
import com.zs.compose.theme.Colors
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.Surface
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.SinglePaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.AdaptiveLargeTopAppBar
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.appbar.TopAppBarScrollBehavior
import com.zs.compose.theme.text.Text
import com.zs.core.store.MediaProvider
import com.zs.audiofy.common.compose.ContentPadding as CP

@Composable
private fun LibraryTopAppBar(
    immersive: Boolean,
    viewState: LibraryViewState,
    behavior: TopAppBarScrollBehavior,
    background: Background,
    modifier: Modifier = Modifier,
) {
    AdaptiveLargeTopAppBar(
        immersive,
        behavior = behavior,
        modifier = modifier.clipToBounds(),
        style = AppBarDefaults.largeAppBarStyle(height = 56.dp, maxHeight = 220.dp),
        insets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
        actions = {
            // Settings
            val navController = LocalNavController.current
            IconButton(
                icon = Icons.Outlined.Settings,
                onClick = { navController.navigate(RouteSettings()) },
                contentDescription = null,
                modifier = Modifier,
            )
        },
        title = {
            Text(
                text = textResource(id = R.string.scr_library_title),
                fontWeight = FontWeight.Light,
                maxLines = 2,
                lineHeight = 23.sp
            )
        },
        navigationIcon = {
            IconButton(
                Icons.Default.Info,
                onClick = {},
                contentDescription = null
            )
        },
        background = {
            // The id of the current album art to show in SlideShow
            val current by viewState.carousel.collectAsState()
            val alpha = lerp(0f, 1f, fraction)

            if (alpha > 0.05f) {
                val veil =
                    Brush.verticalGradient(colors = listOf(Color.Transparent, colors.background))
                Crossfade(
                    targetState = current,
                    animationSpec = tween(4_000),
                    content = { value ->
                        AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                            model = MediaProvider.buildAlbumArtUri(value),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    },
                    modifier = Modifier
                        .thenIf(!immersive) {
                            graphicsLayer {
                                clip = true
                                shape = AppBarDefaults.FloatingTopBarShape
                                this.alpha = alpha
                            }
                        }
                        .drawWithContent {
                            // some shade
                            drawContent()
                            drawRect(Color.Black.copy(0.2f))
                            drawRect(veil)
                        }
                        .visualEffect(ImageBrush.NoiseBrush, alpha = 0.35f, true)
                )
            }
            if (alpha < 0.9) {
                Spacer(
                    modifier = Modifier
                        .graphicsLayer() {
                            this.alpha = lerp(1f, 0f, alpha * 3f)
                        }
                        .thenIf(!immersive) { clip(AppBarDefaults.FloatingTopBarShape) }
                        .background(background)
                )
            }
        }
    )
}

@Composable
@NonRestartableComposable
private fun LibraryHeader(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.headline3,
    padding: PaddingValues = PaddingValues.Zero
) {
    Text(
        text = text,
        modifier = modifier
            .thenIf(padding !== PaddingValues.Zero) { padding(padding) }
            .fillMaxWidth(),
        style = style,
        maxLines = 2,
        lineHeight = 23.sp,
        overflow = TextOverflow.Ellipsis
    )
}

private val Colors.border
    @Composable inline get() = BorderStroke(0.2.dp, accent.copy(0.3f))

private val HeaderMargin = Modifier.padding(top = CP.medium)

@Composable
fun Library(viewState: LibraryViewState) {
    // Retrieve the current window size
    val (width, _) = LocalWindowSize.current
    // Determine the two-pane strategy based on window width range
    // when in mobile portrait; we don't show second pane;
    val strategy = when {
        // TODO  -Replace with OnePane Strategy when updating TwoPane Layout.
        width < Category.Medium -> SinglePaneStrategy
        else -> HorizontalTwoPaneStrategy(0.55f) // Use horizontal layout with 50% split for large screens
    }

    // obtain the padding of BottomNavBar/NavRail
    val inAppNavBarInsets = WindowInsets.content
    val surface = rememberAcrylicSurface()
    val topAppBarScrollBehavior = AppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = AppTheme.colors
    // Layout
    TwoPane(
        spacing = CP.normal,
        strategy = strategy,
        // TODO - Observe if AppNavBar is Positioned in Side or Bottom.
        topBar = {
            LibraryTopAppBar(
                immersive = strategy is SinglePaneStrategy,
                viewState = viewState,
                behavior = topAppBarScrollBehavior,
                background = colors.background(surface),
            )
        },
        // Show only when strategy is TwoPane.
        secondary = {
            if (strategy is SinglePaneStrategy) return@TwoPane
            val insets =
                WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            Surface(
                modifier = Modifier
                    .windowInsetsPadding(insets)
                    .padding(end = ContentPadding.medium)
                    .sizeIn(maxWidth = 300.dp),
                // Use the outline color as the border stroke or null based on the lightness
                // of the material colors
                border = AppTheme.colors.border,
                // Use the overlay color or the background color based on the lightness of
                // the material colors
                color = Color.Transparent,
                // Use the ContentShape as the shape of the surface
                shape = AppTheme.shapes.medium,
                contentColor = AppTheme.colors.onBackground,
                content = {
                    Column {
                        LibraryHeader(
                            text = textResource(R.string.library_shortcuts),
                            style = AppTheme.typography.title2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppTheme.colors.background(2.dp))
                                .padding(horizontal = CP.medium, vertical = CP.small)
                        )

                        Shortcuts(
                            Modifier
                                .padding(ContentPadding.medium)
                                .fillMaxWidth(),
                        )
                    }
                }
            )
        },
        //
        primary = {
            LazyColumn(
                modifier = Modifier
                    .source(surface)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fadingEdge2(length = 50.dp),
                contentPadding = PaddingValues(
                    if (strategy is SinglePaneStrategy) CP.normal else 0.dp,
                    vertical = CP.medium
                ) +
                        (WindowInsets.content
                            .union(inAppNavBarInsets)
                            .union(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)))
                            .asPaddingValues(),
                content = {
                    // Shortcuts.
                    if (strategy is SinglePaneStrategy) {
                        // header
                        item(contentType = "header") {
                            LibraryHeader(
                                text = textResource(R.string.library_shortcuts),
                                padding = PaddingValues.Zero
                            )
                        }
                        // Shortcuts
                        item {
                            Shortcuts(
                                Modifier
                                    .padding(horizontal = CP.medium)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                    // Resents.
                    item {
                        LibraryHeader(
                            modifier = Modifier.fillMaxWidth() then HeaderMargin,
                            text = textResource(R.string.library_history),
                        )
                    }
                    item {
                        Recents(
                            viewState,
                            modifier = Modifier.clip(AppTheme.shapes.xLarge),
                        )
                    }

                    item {
                        Promotions(
                            modifier = Modifier
                                .padding(horizontal = CP.medium, vertical = CP.medium)
                                .fillMaxWidth(),
                        )
                    }
                    // Newly Added
                    item {
                        LibraryHeader(
                            modifier = Modifier.fillMaxWidth() then HeaderMargin,
                            text = textResource(id = R.string.library_recently_added),
                        )
                    }
                    item {
                        NewlyAdded(
                            viewState,
                            modifier = Modifier.clip(AppTheme.shapes.xLarge)
                        )
                    }
                }
            )
        }
    )
}