/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 12-07-2024.
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

package com.zs.core_ui.toast

import androidx.annotation.IntDef
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FixedThreshold
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primex.core.ImageBrush
import com.primex.core.SignalWhite
import com.primex.core.composableOrNull
import com.primex.core.thenIf
import com.primex.core.verticalFadingEdge
import com.primex.core.visualEffect
import com.primex.material2.Button
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.OutlinedButton
import com.primex.material2.TextButton
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.renderInSharedTransitionScopeOverlay
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

/**
 * Interface to represent a single [Toast] instance to be displayed by the [ToastHost].
 *
 * @property message The text message to be displayed in the Toast.
 * @property action Optional label for an action button to be shown in the Toast.
 * @property duration The duration for which the Toast should be displayed. See [Toast.DURATION_SHORT],
 * [Toast.DURATION_LONG], and [Toast.DURATION_INDEFINITE].
 * @property accent The accent color to be used for this Toast. Defaults to [Color.Unspecified].
 * @property icon Optional leading icon to be displayed in the Toast.
 */
interface Toast {

    companion object {
        /**
         * Show the Toast for a short period of time.
         */
        const val DURATION_SHORT = 0

        /**
         * Show the Toast for a long period of time.
         */
        const val DURATION_LONG = 1

        /*** Show the Toast indefinitely until explicitly dismissed or the action is clicked.
         */
        const val DURATION_INDEFINITE = 2

        /**
         * Result code indicating the Toast's action was performed.
         */
        const val ACTION_PERFORMED = 1

        /**
         * Result code indicating the Toast was dismissed.
         */
        const val RESULT_DISMISSED = 2
    }

    val accent: Color get() = Color.Unspecified
    val icon: ImageVector?

    val message: CharSequence
    val action: CharSequence? get() = null

    @Duration
    val duration: Int

    /**
     * Callback invoked when the Toast's action button is clicked.
     */
    fun action()

    /**
     * Callback invoked when the Toast is dismissed, either by timeout or user interaction.
     */
    fun dismiss()
}


@Stable
internal data class Data(
    override val icon: ImageVector?,
    override val message: CharSequence,
    @Duration override val duration: Int,
    override val action: CharSequence?,
    override val accent: Color,
    private val continuation: CancellableContinuation<Int>,
) : Toast {

    override fun action() {
        if (continuation.isActive) continuation.resume(Toast.ACTION_PERFORMED)
    }

    override fun dismiss() {
        if (continuation.isActive) continuation.resume(Toast.RESULT_DISMISSED)
    }
}

/**
 * Annotation for properties representing Toast duration values.
 */
@IntDef(Toast.DURATION_LONG, Toast.DURATION_SHORT, Toast.DURATION_INDEFINITE)
@Target(
    AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Duration

/**
 * Annotation for properties representing Toast result codes.
 */
@IntDef(Toast.ACTION_PERFORMED, Toast.RESULT_DISMISSED)
@Target(
    AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY, AnnotationTarget.TYPE
)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Result

/**
 * Converts the [Toast] duration to milliseconds, considering accessibility settings.
 *
 * @param hasAction Whether the Toast has anaction button.
 * @param accessibilityManager The [AccessibilityManager] to use for calculating the timeout.
 * @return The recommended timeout in milliseconds, adjusted for accessibility.
 */
// TODO: magic numbers adjustment
internal fun Toast.toMillis(
    hasAction: Boolean,
    accessibilityManager: AccessibilityManager?
): Long {
    val original = when (duration) {
        Toast.DURATION_SHORT -> 4000L
        Toast.DURATION_LONG -> 10000L
        else -> Long.MAX_VALUE
    }
    if (accessibilityManager == null) {
        return original
    }
    return accessibilityManager.calculateRecommendedTimeoutMillis(
        original, containsIcons = true, containsText = true, containsControls = hasAction
    )
}


private val EXPANDED_TOAST_SHAPE = RoundedCornerShape(8)
private val TOAST_SHAPE = RoundedCornerShape(16)

private inline val Colors.toastBackgroundColor
    @Composable
    get() = if (isLight) Color(0xFF0E0E0F) else AppTheme.colors.background(1.dp)

/**
 * A custom Toast composable that provides a richer experience compared to the standard Android Toast.
 *
 * This Toast supports features like expandable content, swipe to dismiss, back_press handle and action buttons.
 * It is designed to be customizable in terms of colors, shapes, and content.
 *
 * @param value The [Toast] data class containing the message, action, and other relevant information.
 * @param modifier The [Modifier] to be applied to the Toast composable.
 * @param backgroundColor The background color of the Toast.
 * @param contentColor The content color (text, icons) of the Toast.
 * @param actionColor The color for action buttons.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun Toast(
    value: Toast,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppTheme.colors.toastBackgroundColor,
    contentColor: Color = Color.SignalWhite,
    actionColor: Color = value.accent.takeOrElse { AppTheme.colors.accent },
) {
    // State to track if Toast is expanded
    var isExpanded: Boolean by remember { mutableStateOf(false) }
    // Handle back press to dismiss expanded Toast or the entire Toast
    // BackHandler(isExpanded) { isExpanded = !isExpanded }
    // State for swipe-to-dismiss gesture
    val dismissState = rememberDismissState(
        confirmStateChange = {
            // Dismiss only if not expanded
            if (isExpanded) return@rememberDismissState false
            // Execute action if confirmed
            if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) value.dismiss()
            true
        }
    )

    // SwipeToDismiss composable for handling swipe gesture
    SwipeToDismiss(
        dismissState,
        background = { },
        dismissThresholds = { FixedThreshold(150.dp) },
        modifier = modifier
            .animateContentSize()
            .renderInSharedTransitionScopeOverlay(1.0f),
        dismissContent = {
            // Shape of the Toast based on expanded state
            val shape = if (isExpanded) EXPANDED_TOAST_SHAPE else AppTheme.shapes.small
            ListTile(
                onColor = contentColor,
                spacing = 4.dp,
                leading = composableOrNull(value.icon != null) {
                    // FixMe: It might cause problems.
                    val icon = value.icon!!
                    Icon(
                        painter = rememberVectorPainter(image = icon),
                        contentDescription = null,
                        tint = actionColor
                    )
                },
                // Trailing action button if available and not expanded
                trailing = composableOrNull(value.action != null && !isExpanded) {
                    OutlinedButton(
                        label = value.action!!,
                        onClick = value::action,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = actionColor,
                            backgroundColor = Color.Transparent,
                        ),
                        shape = CircleShape,
                        modifier = Modifier.scale(0.9f)
                    )
                },
                // Toast message
                headline = {
                    Label(
                        text = value.message,
                        color = contentColor,
                        style = AppTheme.typography.bodyMedium,
                        // Limit lines when not expanded
                        maxLines = if (!isExpanded) 3 else Int.MAX_VALUE,
                        modifier = Modifier
                            // Max height constraint
                            .heightIn(max = 195.dp)
                            .thenIf(isExpanded) {
                                val state = rememberScrollState()
                                verticalFadingEdge(backgroundColor, state, 16.dp)
                                    .verticalScroll(state)
                            }
                    )
                },
                // Footer with action buttons when expanded
                footer = composableOrNull(isExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.thenIf(value.icon != null) { padding(start = 20.dp) },
                        content = {
                            // Action button if available
                            val action = value.action
                            if (action != null)
                                Button(
                                    label = action,
                                    onClick = value::action,
                                    colors = ButtonDefaults.buttonColors(
                                        contentColor = actionColor,
                                        backgroundColor = actionColor.copy(0.2f).compositeOver(
                                            AppTheme.colors.background
                                        )
                                    ),
                                    modifier = Modifier.scale(0.9f),
                                    shape = AppTheme.shapes.compact,
                                    elevation = null
                                )
                            // Cancel button
                            TextButton(
                                stringResource(android.R.string.cancel).uppercase(),
                                value::dismiss,
                                modifier = Modifier.scale(0.9f),
                                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                                shape = AppTheme.shapes.compact
                            )
                        }
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .shadow(6.dp, shape, clip = true)
                    // Toggle expanded state on click
                    .clickable(indication = null, interactionSource = null) {
                        if (value.message.length < 100)
                            return@clickable
                        isExpanded = !isExpanded
                    }
                    // Apply border and visual effect if dark theme
                    .thenIf(!isExpanded) {
                        drawWithContent {
                            drawContent()
                            drawRect(color = actionColor, size = size.copy(width = 3.dp.toPx()))
                        }
                    }
                    .visualEffect(ImageBrush.NoiseBrush, 0.60f, overlay = true)
                    .background(backgroundColor)
                    //.clip(shape)
                    .sizeIn(360.dp, 56.dp, 400.dp, 340.dp)
            )
        }
    )
}