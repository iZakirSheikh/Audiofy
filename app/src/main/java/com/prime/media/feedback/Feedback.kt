/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-08-2024.
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

package com.prime.media.feedback

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.ReportGmailerrorred
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.zs.core_ui.ContentPadding
import com.prime.media.common.LocalNavController
import com.prime.media.common.LocalSystemFacade
import com.prime.media.settings.Settings
import com.primex.core.TrafficYellow
import com.primex.core.textResource
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.Text

@Composable
private fun Toolbar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Label(text = textResource(R.string.feedback)) },
        navigationIcon = {
            Icon(
                imageVector = Icons.Outlined.Feedback,
                contentDescription = "feedback",
                modifier = Modifier.padding(horizontal = ContentPadding.normal)
            )
        },
        backgroundColor = com.zs.core_ui.AppTheme.colors.background(2.dp),
        contentColor = com.zs.core_ui.AppTheme.colors.onBackground,
        elevation = 0.dp,
        modifier = modifier,
        actions = {
            val navController = LocalNavController.current
            val facade = LocalSystemFacade.current
            // Launch Telegram
            IconButton(
                imageVector = Icons.Outlined.SupportAgent,
                onClick = { facade.launch(Settings.TelegramIntent) }
            )

            // Launch AppStore
            IconButton(
                imageVector = Icons.Outlined.Star,
                tint = Color.TrafficYellow,
                onClick = facade::launchAppStore
            )

            // Dismiss Feedback Dialog.
            IconButton(
                imageVector = Icons.Outlined.Close,
                onClick = navController::navigateUp
            )
        }
    )
}

@NonRestartableComposable
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Tag(
    selected: Boolean,
    onClick: () -> Unit,
    label: CharSequence,
    icon: ImageVector,
    modifier: Modifier = Modifier
) = FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
) {
    Icon(imageVector = icon, contentDescription = label.toString())
    Text(text = label, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing), style = com.zs.core_ui.AppTheme.typography.caption)
}

@Composable
fun Vertical(
    viewState: FeedbackViewState
) = Column(
    modifier = Modifier
        .wrapContentHeight()
        .widthIn(250.dp, 400.dp),
    verticalArrangement = Arrangement.spacedBy(ContentPadding.normal),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    // The TopAppBar
    Toolbar()

    // The Tags
    Row(
        horizontalArrangement = Arrangement.spacedBy(ContentPadding.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ContentPadding.normal),
        content = {
            val current = viewState.tag
            // Feedback
            Tag(
                selected = current == Feedback.FEEDBACK_TAG_FEEDBACK,
                onClick = { viewState.tag = Feedback.FEEDBACK_TAG_FEEDBACK },
                label = stringResource(R.string.feedback),
                icon = Icons.Outlined.Feedback,
            )

            // Bug Report
            Tag(
                selected = current == Feedback.FEEDBACK_TAG_BUG,
                onClick = { viewState.tag = Feedback.FEEDBACK_TAG_BUG },
                label = stringResource(R.string.bug_report),
                icon = Icons.Outlined.BugReport,
            )

            // Suggestion
            Tag(
                selected = current == Feedback.FEEDBACK_TAG_SUGGESTION,
                onClick = { viewState.tag = Feedback.FEEDBACK_TAG_SUGGESTION },
                label = stringResource(R.string.suggestion),
                icon = Icons.Outlined.ReportGmailerrorred
            )

            // Feature Request
            Tag(
                selected = current == Feedback.FEEDBACK_TAG_FEATURE_REQUEST,
                onClick = { viewState.tag = Feedback.FEEDBACK_TAG_FEATURE_REQUEST },
                label = stringResource(R.string.feature_request),
                icon = Icons.Outlined.AddTask
            )

            // Other
            Tag(
                selected = current == Feedback.FEEDBACK_TAG_OTHER,
                onClick = { viewState.tag = Feedback.FEEDBACK_TAG_OTHER },
                label = stringResource(R.string.general),
                icon = Icons.Outlined.Category
            )
        }
    )

    // Instructions
    Text(
        text = textResource(R.string.feedback_extra_info),
        modifier = Modifier.padding(horizontal = ContentPadding.normal),
    )

    // Feedback Message
    val fedback = viewState.feedback
    val focusRequester = remember { FocusRequester() }
    OutlinedTextField(
        value = fedback,
        onValueChange = { viewState.feedback = it },
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .focusRequester(focusRequester),
        maxLines = 8,
        minLines = 4,
        shape = com.zs.core_ui.AppTheme.shapes.compact,
        colors = TextFieldDefaults.outlinedTextFieldColors(),
        placeholder = { Text(text = textResource(id = R.string.feedback_placeholder)) },
        label = { Text(text = stringResource(R.string.message)) },
        isError = fedback.text.length > Feedback.FEEDBACK_MAX_CHAR_COUNT,
    )

    DisposableEffect(key1 = Unit){
        focusRequester.requestFocus()
        onDispose {  }
    }

    // counter
    Label(
        text = "${fedback.text.length}/${Feedback.FEEDBACK_MAX_CHAR_COUNT}",
        modifier = Modifier
            .offset(y = -ContentPadding.medium)
            .align(Alignment.End)
            .padding(end = ContentPadding.normal),
        style = com.zs.core_ui.AppTheme.typography.caption
    )

    // Submit button
    val navController = LocalNavController.current
    Button(
        label = stringResource(R.string.submit),
        onClick = { viewState.submit(navController) },
        icon = rememberVectorPainter(image = Icons.AutoMirrored.Outlined.Send),
        modifier = Modifier
            .align(Alignment.End)
            .padding(bottom = ContentPadding.medium, end = ContentPadding.normal),
        colors = ButtonDefaults.buttonColors(),
        elevation = null,
        enabled = fedback.text.isNotEmpty() && fedback.text.length <= Feedback.FEEDBACK_MAX_CHAR_COUNT
    )
}

@Composable
@NonRestartableComposable
fun Feedback(viewState: FeedbackViewState) {
    Surface(
        modifier = Modifier.padding(
            horizontal = ContentPadding.large,
            vertical = ContentPadding.normal
        ).windowInsetsPadding(WindowInsets.ime),
        shape = com.zs.core_ui.AppTheme.shapes.compact,
        color = com.zs.core_ui.AppTheme.colors.background(0.5.dp),
        contentColor = com.zs.core_ui.AppTheme.colors.onBackground,
        elevation = 0.dp
    ) {
        Vertical(viewState = viewState)
    }
}


