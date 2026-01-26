/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 11-10-2024.
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

package com.prime.media.common

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.SelectableChipColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.HorizontalChainScope
import androidx.constraintlayout.compose.VerticalChainScope
import androidx.constraintlayout.compose.Visibility
import com.prime.media.common.menu.Action
import com.primex.core.composableOrNull
import com.primex.core.fadingEdge
import com.primex.core.foreground
import com.primex.material2.Label
import com.primex.material2.Text
import com.zs.core.playback.VideoSize
import com.zs.core_ui.AppTheme
import com.zs.core_ui.adaptive.BottomNavItem
import com.zs.core_ui.adaptive.BottomNavItem2
import com.zs.core_ui.adaptive.NavRailItem
import com.zs.core_ui.adaptive.NavigationItemDefaults
import com.zs.core_ui.lottieAnimationPainter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.rememberScrollState as ScrollState
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "Delegates"

//This file holds the simple extension, utility methods of compose.
/**
 * Composes placeholder with lottie icon.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Placeholder(
    title: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: CharSequence? = null,
    noinline action: @Composable (() -> Unit)? = null
) {
    com.primex.material2.Placeholder(
        modifier = modifier, vertical = vertical,
        message = composableOrNull(message != null) {
            Text(
                text = message!!,
                color = AppTheme.colors.onBackground,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        },
        title = {
            Label(
                text = title.ifEmpty { " " },
                maxLines = 2,
                color = AppTheme.colors.onBackground
            )
        },
        icon = {
            Image(
                painter = lottieAnimationPainter(id = iconResId),
                contentDescription = null
            )
        },
        action = action,
    )
}

/**
 * Item header.
 * //TODO: Handle padding in parent composable.
 */
private val HEADER_MARGIN = Padding(0.dp, CP.xLarge, 0.dp, CP.medium)
private val CHAR_HEADER_MARGIN = Padding(0.dp, CP.normal, 0.dp, CP.medium)
private val CHAR_HEADER_SHAPE = RoundedCornerShape(50, 25, 25, 25)
private val NORMAL_HEADER_SHAPE = RoundedCornerShape(0, 50, 50, 50)

/**
 * Represents header for list/grid item groups.
 * Displays a single-character header as a circular "dew drop" or a multi-character header in a
 * two-line circular shape with a Material 3 background and subtle border.
 *
 * @param value The header text.
 * @param modifier The [Modifier] to apply.
 */
@NonRestartableComposable
@Composable
fun ListHeader(
    value: CharSequence,
    modifier: Modifier = Modifier
) {
    when {
        // If the value has only one character, display it as a circular header.
        // Limit the width of the circular header
        value.length == 1 -> Label(
            text = value,
            style = AppTheme.typography.headlineLarge,
            modifier = modifier
                .padding(CHAR_HEADER_MARGIN)
                .border(0.5.dp, AppTheme.colors.background(30.dp), CHAR_HEADER_SHAPE)
                .background(AppTheme.colors.background(1.dp), CHAR_HEADER_SHAPE)
                .padding(horizontal = CP.large, vertical = CP.medium),
        )
        // If the value has more than one character, display it as a label.
        // Limit the label to a maximum of two lines
        // Limit the width of the label
        else -> Label(
            text = value,
            maxLines = 2,
            style = AppTheme.typography.titleSmall,
            modifier = modifier
                .padding(HEADER_MARGIN)
                .widthIn(max = 220.dp)
                .border(0.5.dp, AppTheme.colors.background(30.dp), NORMAL_HEADER_SHAPE)
                .background(AppTheme.colors.background(1.dp), NORMAL_HEADER_SHAPE)
                .padding(horizontal = CP.normal, vertical = CP.small)
        )
    }
}


private val ITEM_SPACING = Arrangement.spacedBy(CP.small)

/**
 * Represents a [Row] of [Chip]s for ordering and filtering.
 *
 * @param current The currently selected filter.
 * @param values The list of supported filter options.
 * @param onRequest Callback function to be invoked when a filter option is selected. null
 * represents ascending/descending toggle.
 */
// TODO - Migrate to LazyRow instead.
@Composable
fun Filters(
    current: Filter,
    values: List<Action>,
    padding: Padding = AppTheme.padding.None,
    onRequest: (order: Action?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Early return if values are empty.
    if (values.isEmpty()) return
    // TODO - Migrate to LazyRow
    val state = ScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .fadingEdge(state, true)
            .horizontalScroll(state),
        horizontalArrangement = ITEM_SPACING,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            // Chip for ascending/descending order
            val (ascending, order) = current
            val padding = Padding(vertical = 6.dp)
            Chip(
                onClick = { onRequest(null) },
                content = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = "ascending",
                        modifier = Modifier.rotate(if (ascending) 0f else 180f)
                    )
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = AppTheme.colors.accent,
                    contentColor = AppTheme.colors.onAccent
                ),
                modifier = Modifier
                    .padding(end = CP.medium),
                shape = AppTheme.shapes.compact
            )
            // Rest of the chips for selecting filter options
            val colors = ChipDefaults.filterChipColors(
                backgroundColor = AppTheme.colors.background(0.5.dp),
                selectedBackgroundColor = AppTheme.colors.background(2.dp),
                selectedContentColor = AppTheme.colors.accent
            )

            for (value in values) {
                val selected = value == order
                val label = stringResource(value.label)
                FilterChip(
                    selected = selected,
                    onClick = { onRequest(value) },
                    content = {
                        Label(label, modifier = Modifier.padding(padding))
                    },
                    leadingIcon = composableOrNull(value.icon != null){
                        Icon(value.icon!!, contentDescription = label.toString())
                    },
                    colors = colors,
                    border = if (!selected) null else BorderStroke(
                        0.5.dp,
                        AppTheme.colors.accent.copy(0.12f)
                    ),
                    shape = AppTheme.shapes.compact
                )
            }
        }
    )
}

/**
 * Represents Navigation Item for navigation component.
 * @see NavRailItem
 * @see BottomNavItem
 */
@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalMaterialApi::class)
@Composable
inline fun NavItem(
    noinline onClick: () -> Unit,
    noinline icon: @Composable () -> Unit,
    noinline label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    typeRail: Boolean = false,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors(),
) = when (typeRail) {
    true -> NavRailItem(onClick, icon, label, modifier, checked, colors = colors)
    else -> BottomNavItem2(checked, onClick, icon, label, modifier.padding(horizontal = 4.dp),  colors = colors)
}


private val MASK_TOP_EDGE = listOf(Color.Black, Color.Transparent)
private val MASK_BOTTOM_EDGE = listOf(Color.Transparent, Color.Black)
/**
 * Applies a fading edge effect to content.
 *
 * Creates a gradient that fades the content to transparency at the edges.
 *
 * @param colors Gradient colors, e.g., `listOf(backgroundColor, Color.Transparent)`. Horizontal if `vertical` is `false`.
 * @param length Fade length from the edge.
 * @param vertical `true` for top/bottom fade, `false` for left/right. Defaults to `true`.
 * @return A [Modifier] with the fading edge effect.
 */
// TODO - Add logic to make fading edge apply/exclude content padding in real one.
fun Modifier.fadingEdge2(
    length: Dp = 10.dp,
    vertical: Boolean = true,
) = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    drawRect(
        Brush.verticalGradient(
            MASK_TOP_EDGE, endY = length.toPx(), startY = 0f
        ), blendMode = BlendMode.DstOut, size = size.copy(height = length.toPx())
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = MASK_BOTTOM_EDGE,
            startY = size.height - length.toPx(),
            endY = size.height
        ),
        //  topLeft = Offset(0f, size.height - length.toPx()),
        // size = size.copy(height = length.toPx()),
        blendMode = BlendMode.DstOut
    )
}

private val _StartAligned = ChainStyle.Packed(0f)
private val _EndAligned = ChainStyle.Packed(1f)

/**
 * A [ChainStyle] where all elements are tightly grouped and positioned
 * towards the **start edge** of the chain.
 *
 * Example usage:
 * ```
 * createHorizontalChain(item1, item2, chainStyle = ChainStyle.StartAligned)
 * ```
 */
val ChainStyle.Companion.StartAligned get() = _StartAligned

/**
 * A [ChainStyle] where all elements are tightly grouped and positioned
 * towards the **end edge** of the chain.
 *
 * Example usage:
 * ```
 * createHorizontalChain(item1, item2, chainStyle = ChainStyle.EndAligned)
 * ```
 */
val ChainStyle.Companion.EndAligned get() = _EndAligned


/** A shorthand method to create a horizontal chain.*/
fun ConstraintSetScope.horizontal(
    vararg elements: ConstrainedLayoutReference,
    chainStyle: ChainStyle = ChainStyle.Packed,
    alignBy: ConstrainedLayoutReference? = null,
    spacing: Dp = 0.dp,
    constrainBlock: HorizontalChainScope.() -> Unit
): ConstrainedLayoutReference {
    // If spacing is specified, apply it as a start margin to all elements except the first one.
    if (spacing != 0.dp)
        for (i in 1 until elements.size)
            elements[i].withHorizontalChainParams(startMargin = spacing)

    // Create the horizontal chain with the given elements and chain style.
    val chain = createHorizontalChain(*elements, chainStyle = chainStyle)
    constrain(chain, constrainBlock)

    // Vertically align all elements in the chain with a reference element.
    // The reference is `alignBy` if provided, otherwise it's the first element in the chain.
    val first = alignBy ?: elements.first()
    for (element in elements) {
        if (element == first) continue
        constrain(element) {
            top.linkTo(first.top)
            bottom.linkTo(first.bottom)
        }
    }

    // Return the first element, which can be useful for constraining other composables relative to the chain.
    return first
}

/** @see horizontal */
fun ConstraintSetScope.vertical(
    vararg elements: ConstrainedLayoutReference,
    chainStyle: ChainStyle = ChainStyle.Packed,
    constrainBlock: VerticalChainScope.() -> Unit
): ConstrainedLayoutReference {
    val chain = createVerticalChain(*elements, chainStyle = chainStyle)

    constrain(chain, constrainBlock)
    // align all with first
    val first = elements.first()
    for (element in elements) {
        if (element == first) continue
        constrain(element) {
            start.linkTo(first.start)
            end.linkTo(first.end)
        }
    }
    return first
}

fun HorizontalChainScope.linkTo(
    start: ConstraintLayoutBaseScope.VerticalAnchor,
    end: ConstraintLayoutBaseScope.VerticalAnchor,
    startMargin: Dp = 0.dp,
    endMargin: Dp = 0.dp,
    startGoneMargin: Dp = 0.dp,
    endGoneMargin: Dp = 0.dp,
) {
    this.start.linkTo(
        anchor = start,
        margin = startMargin,
        goneMargin = startGoneMargin,
    )
    this.end.linkTo(
        anchor = end,
        margin = endMargin,
        goneMargin = endGoneMargin,
    )

}


fun ConstraintSetScope.hide(
    vararg elements: ConstrainedLayoutReference,
    visibility: Visibility = Visibility.Invisible
) {
    for (element in elements)
        constrain(element) {
            this.visibility = visibility
        }
}

/**
 * Sets the width and height of this [ConstrainScope] to the same [androidx.constraintlayout.compose.Dimension] value.
 *
 * This property allows you to easily set both width and height to a common dimension,
 * simplifying the layout constraints.
 *
 * **Note:** Getting the value of this property is not supported and will result in an error.
 *
 * @see androidx.constraintlayout.compose.Dimension
 */
var ConstrainScope.dimensions: androidx.constraintlayout.compose.Dimension
    set(value) {
        width = value
        height = value
    }
    get() = error("Operation not supported.")

/**
 * @see androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
 */
@Composable
@Deprecated("Find better solutions.")
inline fun rememberAnimatedVectorPainter(@DrawableRes id: Int, atEnd: Boolean) =
    androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = id), atEnd = atEnd
    )

/**
 * Resizes and lays out content using the given [ContentScale],
 * ensuring the [original] [VideoSize] is properly inscribed into
 * the destination constraints.
 *
 * - Skips resizing if [original] is unspecified.
 * - Computes a scale factor using [contentScale].
 * - Applies scaled width/height as layout constraints.
 */
fun Modifier.resize(
    contentScale: ContentScale,
    original: VideoSize,
): Modifier =
    if (!original.isSpecified) foreground(Color.Black) else layout { measurable, constraints ->
        // Compute the "source size" in pixels based on video ratio.
        val scrSizePx = original.let {
            val par = it.ratio
            return@let when {
                par < 1.0 -> Size(it.height.toFloat(), it.width.toFloat() * par) // Taller/narrower
                par > 1.0 -> Size(it.width.toFloat(), it.height / par)           // Wider
                else -> Size(it.width.toFloat(), it.height.toFloat())            // Normal aspect
            }
        }

        // Destination size: area available to draw into.
        val dstSizePx = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

        // Compute how the source needs to scale to fit into the destination.
        val scaleFactor = contentScale.computeScaleFactor(scrSizePx, dstSizePx)
        Log.d(TAG, "resizeWithContentScale: $scrSizePx, $dstSizePx, $scaleFactor")

        // Measure the child with scaled constraints.
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = 0, // allow shrinking if needed
                minHeight = 0,
                maxWidth = (scrSizePx.width * scaleFactor.scaleX).roundToInt(),
                maxHeight = (scrSizePx.height * scaleFactor.scaleY).roundToInt(),
            )
        )

        // Layout the child at (0,0).
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

@Suppress("StateFlowValueCalledInComposition")
@Composable
public fun <T: Any> StateFlow<T?>.collectAsState(
    default: T,
    context: CoroutineContext = EmptyCoroutineContext
): State<T> =   produceState(value ?: default, this, context) {
    if (context == EmptyCoroutineContext) {
        collect { value = it ?: default }
    } else withContext(context) { collect { value = it ?: default } }
}

/**
 * A convenient [Modifier] for configuring the size and scale of a Lottie animation,
 * particularly for use as button icons.
 *
 * This modifier streamlines the creation of Lottie-based button icons by defaulting
 * to the standard Material Design icon size (24.dp) and allowing for easy scaling.
 */
fun Modifier.lottie(scale: Float = 1f) = this
    .requiredSize(24.dp)
    .then(Modifier.scale(scale))