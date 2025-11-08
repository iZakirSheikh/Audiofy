package com.prime.media.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.core_ui.AppTheme
import com.zs.core_ui.AppTheme.colors
import com.zs.core_ui.Colors
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.None
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient as VerticalGradient


private val FloatingTopBarShape = RoundedCornerShape(20)
private val Colors.floatingTopBarBorder: BorderStroke
    @Composable
    inline get() =  BorderStroke(0.5.dp, VerticalGradient(
        listOf(
            if (isLight) colors.background(2.dp) else Color.Gray.copy(0.24f),
            if (isLight) colors.background(2.dp) else Color.Gray.copy(0.24f),
        )
    ))

@Composable
fun FloatingTopAppBar(
    title: @Composable () -> Unit,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    contentColor: Color = AppTheme.colors.onBackground,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    backdropProvider: HazeState? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                VerticalGradient(
                    listOf(
                        colors.background(1.dp),
                        colors.background.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            ),
        content = {
            TopAppBar(
                navigationIcon = navigationIcon,
                title = title,
                windowInsets = WindowInsets.None,
                actions =actions,
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                contentColor = contentColor,
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .windowInsetsPadding(windowInsets)
                    .padding(horizontal = ContentPadding.xLarge, vertical = ContentPadding.small)
                    .shadow(elevation, FloatingTopBarShape)
                    .border(colors.floatingTopBarBorder, FloatingTopBarShape)
                    .height(52.dp)
                    .dynamicBackdrop(
                        backdropProvider,
                        HazeStyle.Regular(colors.background(0.4.dp)),
                        colors.background,
                        colors.accent
                    )
            )
        }
    )
}