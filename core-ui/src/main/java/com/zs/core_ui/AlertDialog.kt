/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 30-01-2025.
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

package com.zs.core_ui

import android.view.Gravity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private val TitleBarHeight = Modifier.height(46.dp)
private val DialogSize = Modifier
    .widthIn(280.dp, 460.dp)
    .padding(horizontal = 16.dp, vertical = 10.dp)

/**
 * Adds a subtle shine effect to components, particularly [Acrylic] ones,
 * mimicking the gleaming edge of glass.
 *
 * This property defines a [BorderStroke] that uses a vertical gradient.
 * The gradient's colors are determined by whether the current theme is light or dark.
 * - In a light theme, it transitions from the `background` color to a slightly darker,
 *   semi-transparent version of the `background`.
 * - In a dark theme, it transitions from a semi-transparent gray to an even more
 *   transparent gray.
 *
 * This creates a visual highlight, suggesting a light source reflecting off the edge
 * of the component.
 */
private val Colors.shine
    @Composable
    get() = BorderStroke(
        0.5.dp,
        Brush.verticalGradient(
            listOf(
                if (isLight) background else Color.Gray.copy(0.24f),
                if (isLight) background.copy(0.3f) else Color.Gray.copy(0.075f),
            )
        )
    )

private val ItemSpace = Arrangement.spacedBy(8.dp)
private val AlertDialogShape = RoundedCornerShape(22.dp)

/**
 * A composable function that displays a Dialog if the `expanded` parameter is true.
 *
 * This function wraps the standard Compose [Dialog] to address potential density issues.
 * Specifically, when there is a change in the system's density and the developer has set
 * a custom density in the app, the system density in dialogs may override the custom
 * density, leading to undesirable visual effects. This function uses [CompositionLocalProvider]
 * to ensure the dialog uses the app's current density.
 *
 * @param expanded Controls the visibility of the dialog. If `false`, the dialog is not shown.
 * @param onDismissRequest Callback invoked when the user tries to dismiss the dialog.
 * @param properties [DialogProperties] for configuring the dialog's behavior.
 * @param content The content of the dialog.
 *
 * @see androidx.compose.ui.window.Dialog
 */
@Composable
@NonRestartableComposable
inline fun Dialog2(
    expanded: Boolean,
    noinline onDismissRequest: () -> Unit,
    gravity: Int = Gravity.CENTER,
    properties: DialogProperties = DialogProperties(),
    crossinline content: @Composable () -> Unit
) {
    // Do not show when not expanded
    if (!expanded) return
    Dialog(onDismissRequest, properties) {
        // Enhance the dialog with additional features:
        // - Explore possibilities for true full-screen dialogs.
        // - Investigate implementing blur-behind and background blur effects.
        // - Consider adding scrim effects and other visual enhancements.
        val view = LocalView.current
        SideEffect {
            val dialogWindowProvider = view.parent as? DialogWindowProvider ?: return@SideEffect
            val window = dialogWindowProvider.window
            window.setGravity(gravity)
        }
        content()
    }
}


/**
 * A customizable alert dialog that provides a standard dialog layout with a title,
 * optional navigation icon, actions, and content area.
 *
 * This dialog is built upon the [Dialog] composable and provides a structured layout
 * using a [TopAppBar] for the title and actions, and a [Surface] for the main content.
 * The appearance of the dialog (colors, shape) is derived from the [AppTheme].
 *
 * @param onDismissRequest Lambda to be invoked when the user requests to dismiss the dialog,
 *   such as by tapping outside the dialog or pressing the back button.
 * @param title A composable lambda that defines the title of the dialog. Typically a [Text] composable.
 * @param navigationIcon A composable lambda that defines an optional icon to be displayed
 *   at the start of the title bar.
 * @param actions A composable lambda that defines the actions to be displayed at the end
 *   of the title bar, within a [RowScope]. Typically [TextButton]s or [IconButton]s.
 * @param properties [DialogProperties] for configuring the dialog's behavior, such as
 *   whether it's dismissible.
 * @param shape The [Shape] to be used for the dialog's container. Defaults to `AppTheme.shapes.xLarge`.
 * @param gravity The gravity of the dialog on the screen. See [android.view.Gravity].
 *   Defaults to `Gravity.CENTER`.
 * @param content A composable lambda that defines the main content of the dialog,
 *   within a [ColumnScope].
 */
@Composable
fun AlertDialog2(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    properties: DialogProperties = DialogProperties(),
    shape: Shape = AlertDialogShape,
    gravity: Int = Gravity.CENTER,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog2(true, onDismissRequest, gravity = gravity, properties = properties) {
        val colors = AppTheme.colors
        Surface(
            color = if (colors.isLight) colors.accent else colors.background(3.dp),
            shape = shape,
            modifier = if (properties.usePlatformDefaultWidth) Modifier else DialogSize,
            border = if (colors.isLight) null else colors.shine,
            content = {
                Column {
                    // Top AppBar
                    TopAppBar(
                        navigationIcon = navigationIcon,
                        title = title,
                        backgroundColor = Color.Transparent,
                        elevation = 0.dp,
                        contentColor = LocalContentColor.current,
                        modifier = TitleBarHeight,
                        actions = actions
                    )

                    // content
                    Surface(
                        color = colors.background(1.dp),
                        shape = shape,
                        modifier = Modifier.padding(horizontal = 2.dp),
                        content = {
                            // Main content: The main content of the dialog.
                            ProvideTextStyle(AppTheme.typography.bodyMedium) {
                                Column(
                                    content = content,
                                    verticalArrangement = ItemSpace,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 16.dp
                                    )
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

/**
 * @see AlertDialog
 */
@Composable
@NonRestartableComposable
fun AlertDialog2(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    properties: DialogProperties = DialogProperties(),
    shape: Shape = AlertDialogShape,
    gravity: Int = Gravity.CENTER,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded)
        return
    AlertDialog2(
        onDismissRequest,
        title,
        navigationIcon,
        actions,
        properties,
        shape,
        gravity,
        content
    )
}