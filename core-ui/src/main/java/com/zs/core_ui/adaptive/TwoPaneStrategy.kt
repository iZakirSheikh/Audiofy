
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 03-10-2024.
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

package com.zs.core_ui.adaptive

import androidx.annotation.FloatRange
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zs.core_ui.ContentPadding
import kotlin.math.roundToInt

// TODO: In the future, consider adding functionality to specify the preferred split location,
//   Similar to Accompanist's approach.

/**
 * Strategy for positioning the details pane relative to the main content pane.
 */
sealed interface TwoPaneStrategy {
    /**
     * Calculates the position of the details pane.
     *
     * @param size The available space for both panes.
     * @return For horizontal/vertical strategies, the pixel at which the split occurs.
     *         For stacked strategies, the Y coordinate of the center of the details pane.
     */
    fun calculate(size: IntSize): Int
}

/**
 * A strategy for positioning the two panes horizontally, with the content pane on the left
 * and the details pane on the right. The content pane occupies at least the specified fraction
 * of the screen width. If the details pane requires less space, the content pane expands to fill
 * the remaining area.
 *
 * @param fraction The minimum fraction of the screen width to be occupied by the content pane.
 *                  Must be between 0.0 (exclusive) and 1.0 (inclusive).
 */
@JvmInline
value class HorizontalTwoPaneStrategy(
    @FloatRange(from = 0.0, to = 1.0) val fraction: Float
) : TwoPaneStrategy {

    /**
     * Calculates the horizontal split point, determining the width of the content pane.
     *
     * @param size The total available width.
     * @return The pixel position where the split between content and details panes occurs.
     */
    override fun calculate(size: IntSize): Int {
        return (size.width * fraction).roundToInt()
    }
}

/**
 * A strategy for positioning the two panes vertically, with the content pane on top
 * and the details pane below. The content pane occupies at least the specified fraction
 * of the screen height. If the details pane requires less space, the content pane expands
 * to fill the remaining area.
 *
 * @param fraction The minimum fraction of the screen height to be occupied by the content pane.
 *                  Must be between 0.0 (exclusive) and 1.0 (inclusive).
 */
@JvmInline
value class VerticalTwoPaneStrategy(
    @FloatRange(from = 0.0, to = 1.0) val fraction: Float,
) : TwoPaneStrategy {
    /**
     * Calculates the horizontal split point, determining the height of the content pane.
     *
     * @param size The total available height.
     * @return The pixel position where the split between content and details panes occurs.
     */
    override fun calculate(size: IntSize): Int {
        return (size.height * fraction).roundToInt()
    }
}

/**
 * A strategy for positioning the two panes in a stacked configuration, where the details pane
 * is displayed above the content pane. The details pane's vertical position is determined by
 * the given bias.
 *
 * @param bias The vertical bias of the details pane, ranging from 0.0 (top) to 1.0 (bottom).
 *             A value of 0.5 centers the details pane vertically.
 */
@JvmInline
value class StackedTwoPaneStrategy(
    @FloatRange(from = 0.0, to = 1.0) val bias: Float
) : TwoPaneStrategy {
    /**
     * Calculates the vertical position of the center of the details pane.
     *
     * @param size The total available screen height.
     * @return The Y coordinate of the center of the details pane.
     */
    override fun calculate(size: IntSize): Int {
        return (size.height * bias).roundToInt()
    }
}


/**
 * The recommended details pane exit animation for the given [TwoPaneStrategy].
 */
val TwoPaneStrategy.exitAnimation: ExitTransition
    get() {
        return when (this) {
            is HorizontalTwoPaneStrategy -> fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 })
            is VerticalTwoPaneStrategy -> fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            is StackedTwoPaneStrategy -> fadeOut() + scaleOut(targetScale = 0.95f)
        }
    }

/**
 * @see exitAnimation
 */
val TwoPaneStrategy.enterAnimation: EnterTransition
    get() = when (this) {
        is HorizontalTwoPaneStrategy -> fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 })
        is VerticalTwoPaneStrategy -> fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
        is StackedTwoPaneStrategy -> fadeIn() + scaleIn(initialScale = 0.95f)
    }

/**
 * The recommended details pane content padding for the given [TwoPaneStrategy].
 */
inline val TwoPaneStrategy.padding: PaddingValues
    @Composable
    inline get() = when (this) {
        is HorizontalTwoPaneStrategy -> WindowInsets.systemBars.asPaddingValues()
        is VerticalTwoPaneStrategy -> WindowInsets.navigationBars.asPaddingValues()
        is StackedTwoPaneStrategy -> WindowInsets.systemBars.asPaddingValues()
    }

/**
 * The recommended details pane margin for the given [TwoPaneStrategy].
 */
inline val TwoPaneStrategy.margin: PaddingValues
    inline get() = when (this) {
        is StackedTwoPaneStrategy -> PaddingValues(
            horizontal = ContentPadding.large,
            vertical = ContentPadding.normal
        )

        else -> PaddingValues(0.dp)
    }

/**
 * The recommended details pane shape for the given [TwoPaneStrategy].
 */
inline val TwoPaneStrategy.shape
    inline get() = when(this){
        is HorizontalTwoPaneStrategy -> RoundedCornerShape(topStartPercent = 8, bottomStartPercent = 8)
        is VerticalTwoPaneStrategy -> RoundedCornerShape(topStartPercent = 8, topEndPercent = 8)
        is StackedTwoPaneStrategy -> RoundedCornerShape(topStartPercent = 8, topEndPercent = 8, bottomStartPercent = 2, bottomEndPercent = 2)
    }