/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 07-07-2024.
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

package com.prime.media.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.about.AboutUs
import com.zs.core_ui.Anim
import com.zs.core_ui.LongDurationMills
import com.prime.media.common.Artwork
import com.prime.media.common.LocalNavController
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.None
import com.zs.core_ui.shimmer.pulsate
import com.prime.media.feedback.RouteFeedback
import com.prime.media.impl.Repository
import com.primex.core.ImageBrush
import com.primex.core.blend
import com.primex.core.foreground
import com.primex.core.lerp
import com.primex.core.textResource
import com.primex.core.visualEffect
import com.primex.material2.IconButton
import com.primex.material2.OutlinedButton
import com.primex.material2.Text
import com.primex.material2.appbar.CollapsableTopBarLayout
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core_ui.AppTheme

/**
 * Defines the typography style for the large top bar title.
 */
private val LargeTopBarTitle
    @Composable inline get() = AppTheme.typography.headlineLarge.copy(lineHeight = 20.sp)

/**
 * Defines the typography style for the normal top bar title.
 */
private val NormalTopBarTitle
    @Composable inline get() = AppTheme.typography.bodyLarge

private val com.zs.core_ui.Colors.topBar
    @Composable inline get() = accent.blend(background, 0.96f)

private val DefaultRepeatableSpec =
    repeatable(3, tween<Float>(Anim.LongDurationMills, 750))

/**
 * Composable function to display the app bar for the library screen.
 *
 * @param state: The current Library state containing information for the UI.
 * @param modifier: Optional modifier to apply to the top bar.
 * @param behaviour: Optional scroll behavior for the top bar.
 * @param insets: Window insets to consider for layout.
 */
@Composable
fun CarousalAppBar(
    state: Library,
    modifier: Modifier = Modifier,
    behaviour: TopAppBarScrollBehavior? = null,
    insets: WindowInsets = WindowInsets.None
) {
    CollapsableTopBarLayout(
        height = 56.dp,
        maxHeight = 220.dp,
        insets = insets,
        scrollBehavior = behaviour,
        modifier = modifier.clipToBounds()
    ) {
        // Background with image representation and gradient
        val id by state.carousel.collectAsState()
        val colors = AppTheme.colors
        val gradient =
            Brush.verticalGradient(colors = listOf(Color.Transparent, colors.background))
        // Background
        val curtain =
            lerp(colors.topBar, Color.Transparent, fraction)
        Crossfade(
            targetState = Repository.toAlbumArtUri(id ?: 0),
            animationSpec = tween(4_000),
            modifier = Modifier
                .visualEffect(ImageBrush.NoiseBrush, alpha = 0.35f, true)
                .foreground(curtain)
                .parallax(0.2f)
                .layoutId(TopAppBarDefaults.LayoutIdBackground)
                .fillMaxSize(),
            content = { value ->
                Artwork(
                    modifier = Modifier
                        .foreground(Color.Black.copy(0.2f))
                        .fillMaxSize(),
                    data = value,
                )
            }
        )

        // FixMe - The gradient is also enlarged by parallax and hence this.
        Box(
            modifier = Modifier
                .alpha(lerp(0f, 1f, fraction))
                .foreground(gradient)
                .fillMaxSize()
        )

        // Navigation Icon.
        val contentColor = lerp(LocalContentColor.current, Color.White, fraction)
        val navController = LocalNavController.current
        IconButton(
            onClick = { navController.navigate(AboutUs.route) },
            painter = rememberVectorPainter(image = Icons.Filled.Info),
            contentDescription = "about us",
            modifier = Modifier
                .layoutId(TopAppBarDefaults.LayoutIdNavIcon),
            tint = contentColor
        )

        // Actions  (Buy and settings)
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.layoutId(TopAppBarDefaults.LayoutIdAction),
                    content = { Actions() }
                )
            }
        )

        // Title with smooth transition between sizes and positions
        Text(
            text = textResource(id = R.string.library_title),
            fontWeight = FontWeight.Light,
            style = lerp(NormalTopBarTitle, LargeTopBarTitle, fraction),
            modifier = Modifier
                .road(Alignment.CenterStart, Alignment.BottomStart)
                .layoutId(TopAppBarDefaults.LayoutIdCollapsable_title)
                .offset {
                    val dp = lerp(0.dp, 16.dp, fraction)
                    IntOffset(dp.roundToPx(), -dp.roundToPx())
                },
            maxLines = 2
        )
    }
}

context(RowScope)
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun Actions() {
    val contentColor = LocalContentColor.current
    // Support
    val ctx = LocalContext.current
    val navController = LocalNavController.current
    IconButton(
        imageVector = Icons.Outlined.Feedback,
        onClick = { navController.navigate(RouteFeedback()) },
        modifier = Modifier,
        tint = contentColor
    )
    // Just return if the app is free version app.
    val provider = LocalSystemFacade.current
    if (provider.isAdFreeVersion)
        return

    // Buy full version button.
    OutlinedButton(
        label = textResource(R.string.library_ads),
        onClick = { provider.launchBillingFlow(BuildConfig.IAP_NO_ADS) },
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
}