/*
 * Copyright (c)  2025 Zakir Sheikh
 *
 * Created by sheik on 12 of Dec 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prime.media.library

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.repeatable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.about.RouteAboutUs
import com.prime.media.common.AdaptiveLargeTopAppBar
import com.prime.media.common.Banner
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.Regular
import com.prime.media.common.Route
import com.prime.media.common.dynamicBackdrop
import com.prime.media.common.fadingEdge2
import com.prime.media.common.floatingTopBarShape
import com.prime.media.common.rememberHazeState
import com.prime.media.old.common.LocalNavController
import com.prime.media.personalize.RoutePersonalize
import com.prime.media.settings.AppConfig
import com.prime.media.settings.RouteSettings
import com.primex.core.ImageBrush
import com.primex.core.lerp
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.core.visualEffect
import com.primex.material2.IconButton
import com.primex.material2.OutlinedButton
import com.primex.material2.Text
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core.store.MediaProvider
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.LongDurationMills
import com.zs.core_ui.Range
import com.zs.core_ui.adaptive.HorizontalTwoPaneStrategy
import com.zs.core_ui.adaptive.SinglePaneStrategy
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import com.zs.core_ui.lottieAnimationPainter
import com.zs.core_ui.shimmer.pulsate
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeSource
import com.zs.core_ui.ContentPadding as CP

object RouteLibrary : Route {
    //
    private val DefaultRepeatableSpec =
        repeatable(3, tween<Float>(Anim.LongDurationMills, 750))
    private val Colors.border
        @Composable inline get() = BorderStroke(0.2.dp, accent.copy(0.3f))
    private val HeaderMargin = Modifier.padding(top = CP.small)
    private val LinePadding =
        PaddingValues(CP.normal, CP.medium, CP.normal)

    @Composable
    private fun TopAppBar(
        immersive: Boolean,
        viewState: LibraryViewState,
        behavior: TopAppBarScrollBehavior,
        backdrop: HazeState?,
        modifier: Modifier = Modifier,
    ) {
        val navController = LocalNavController.current
        val colors = AppTheme.colors
        AdaptiveLargeTopAppBar(
            immersive = immersive,
            behavior = behavior,
            modifier = modifier,
            style = TopAppBarDefaults.largeAppBarStyle(
                height = 56.dp,
                maxHeight = 220.dp,
                scrolledContentColor = colors.onBackground,
                contentColor = colors.onBackground,
            ),
            insets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            actions = {
                val contentColor = LocalContentColor.current
                // Support
                val ctx = LocalContext.current

                // Settings
                IconButton(
                    imageVector = Icons.Outlined.Settings,
                    onClick = { navController.navigate(RouteSettings()) },
                    modifier = Modifier,
                    tint = contentColor
                )
                // Just return if the app is free version app.
                val provider = LocalSystemFacade.current
                if (provider.isAdFreeVersion)
                    return@AdaptiveLargeTopAppBar

                // Buy full version button.
                OutlinedButton(
                    label = textResource(R.string.library_ads),
                    onClick = { provider.initiatePurchaseFlow(BuildConfig.IAP_NO_ADS) },
                    icon = painterResource(id = R.drawable.ic_remove_ads),
                    modifier = Modifier.scale(0.75f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = contentColor.copy(0.24f),
                        contentColor = contentColor
                    ),
                    shape = CircleShape
                )

                // check if rewarded video is available
                val available = provider.isRewardedVideoAvailable
                val color = AppTheme.colors.accent
                IconButton(
                    imageVector = Icons.Outlined.MoreTime,
                    onClick = provider::showRewardedVideo,
                    tint = contentColor.copy(if (available) ContentAlpha.high else ContentAlpha.disabled),
                    enabled = available,
                    modifier = (if (!available) Modifier else Modifier
                        .pulsate(color, animationSpec = DefaultRepeatableSpec)),
                )
            },
            title = {
                Text(
                    text = textResource(id = R.string.library_title),
                    fontWeight = FontWeight.Light,
                    maxLines = 2,
                    lineHeight = 23.sp
                )
            },
            navigationIcon = {
                IconButton(
                    Icons.Default.Info,
                    onClick = { navController.navigate(RouteAboutUs()) },
                    contentDescription = null
                )
            },
            background = {
                //

                if (fraction > 0.05f) {
                    val current by viewState.carousel.collectAsState()
                    val alpha = lerp(0f, 1f, fraction)
                    val veil =
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.background
                            )
                        )
                    Crossfade(
                        targetState = current,
                        animationSpec = tween(4_000),
                        content = { value ->
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                model = MediaProvider.buildAlbumArtUri(value),
                                contentDescription = null,
                                onState = {
                                    if (it is AsyncImagePainter.State.Error)
                                        Log.d(
                                            "Library",
                                            "TopAppBar: ${it.result.throwable.message}"
                                        )
                                },
                                contentScale = ContentScale.Crop
                            )
                        },
                        modifier = Modifier
                            .thenIf(!immersive) {
                                graphicsLayer {
                                    clip = true
                                    shape = AppBarDefaults.floatingTopBarShape
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
                // This Spacer provides a background that fades in as the app bar collapses.
                if (fraction < 0.9f)
                    Spacer(
                        modifier = Modifier
                            .graphicsLayer() {
                                this.alpha = lerp(1f, 0f, fraction * 3f)
                            }
                            .thenIf(!immersive) { clip(AppBarDefaults.floatingTopBarShape) }
                            .dynamicBackdrop(
                                backdrop,
                                HazeStyle.Regular(
                                    colors.background(0.4.dp),
                                    //if (colors.isLight) 0.24f else 0.63f
                                ),
                                colors.background,
                                colors.accent
                            )
                    )
            }
        )
    }

    @Composable
    @NonRestartableComposable
    private fun LibraryHeader(
        text: CharSequence,
        modifier: Modifier = Modifier,
        style: TextStyle = AppTheme.typography.headlineSmall,
        padding: PaddingValues? = LinePadding
    ) {
        Text(
            text = text,
            modifier = modifier
                .thenIf(padding != null) { padding(padding!!) }
                .fillMaxWidth(),
            style = style,
            maxLines = 2,
            lineHeight = 23.sp,
            overflow = TextOverflow.Ellipsis
        )
    }

    private val ModifierLinePadding = Modifier.padding(LinePadding)
    @Composable
    operator fun invoke(viewState: LibraryViewState) {
        // Retrieve the current window size
        val (width, _) = LocalWindowSize.current
        val facade = LocalSystemFacade.current
        // Determine the two-pane strategy based on window width range
        // when in mobile portrait; we don't show second pane;
        val strategy = when {
            // TODO  -Replace with OnePane Strategy when updating TwoPane Layout.
            width < Range.Medium -> SinglePaneStrategy
            else -> HorizontalTwoPaneStrategy(0.55f) // Use horizontal layout with 50% split for large screens
        }
        // obtain the padding of BottomNavBar/NavRail
        val inAppNavBarInsets = WindowInsets.contentInsets
        val surface = if (AppConfig.isBackgroundBlurEnabled) rememberHazeState() else null
        val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        TwoPane(
            spacing = CP.normal,
            strategy = strategy,
            topBar = {
                TopAppBar(
                    immersive = strategy is SinglePaneStrategy,
                    viewState = viewState,
                    behavior = topAppBarScrollBehavior,
                    backdrop = surface
                )
            },
            secondary = {
                if (strategy is SinglePaneStrategy) return@TwoPane
                val insets =
                    WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                Surface(
                    modifier = Modifier
                        .windowInsetsPadding(insets)
                        .padding(end = CP.small)
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
                                style = AppTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppTheme.colors.background(2.dp))
                                    .padding(horizontal = CP.small, vertical = CP.small)
                            )

                            Shortcuts(
                                Modifier
                                    .padding(horizontal = CP.medium, vertical = CP.small)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                )
            },
            primary = {
                LazyColumn(
                    modifier = Modifier
                        .thenIf(surface != null) { hazeSource(surface!!) }
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .fadingEdge2(length = 50.dp),
                    contentPadding = (WindowInsets.contentInsets
                        .union(inAppNavBarInsets)
                        .union(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)))
                        .asPaddingValues(),
                    content = {
                        // Shortcuts - show only here in SinglePaneLayout.
                        if (strategy is SinglePaneStrategy) {
                            // header
                            item(contentType = "header") {
                                LibraryHeader(
                                    text = textResource(R.string.library_shortcuts)
                                )
                            }
                            // Shortcuts
                            item {
                                Shortcuts(
                                    Modifier
                                        .then(ModifierLinePadding)
                                        .padding(horizontal = CP.medium)
                                        .fillMaxWidth(),
                                )
                            }
                        }

                        // Promotions
                        item {
                            Promotions(
                                modifier = Modifier
                                    .then(ModifierLinePadding)
                                    .fillMaxWidth(),
                            )
                        }

                        // Personalize
                        item {
                            val navigator = LocalNavController.current
                            // Personalize
                            DropdownMenuItem(
                                onClick = { navigator.navigate(RoutePersonalize()) },
                                modifier = Modifier.padding(vertical = CP.medium),
                                content = {
                                    Icon(
                                        painter = lottieAnimationPainter(R.raw.lt_settings_roll, iterations = 2),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp).scale(1.5f),
                                    )

                                    Text(
                                        textResource(R.string.scr_personalize_title_desc),
                                        modifier = Modifier.padding(start = CP.medium)
                                    )

                                    Spacer(Modifier.weight(1f))

                                    Icon(
                                        painter = lottieAnimationPainter(R.raw.lt_skip_to_next_circular_bordered, iterations = 2),
                                        contentDescription = null,
                                        modifier = Modifier.padding(horizontal = CP.normal).size(44.dp),
                                    )
                                },
                            )
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
                                contentPadding = LinePadding,
                            )
                        }

                        // Ad
                        if (!facade.isAdFree)
                            item {
                            Banner(
                                modifier = ModifierLinePadding,
                                key = "Banner_1"
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
                            Latest(
                                viewState,
                                contentPadding = LinePadding
                            )
                        }
                    }
                )
            }
        )
    }
}