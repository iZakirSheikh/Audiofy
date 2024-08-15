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

package com.prime.media.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.prime.media.R
import com.prime.media.feedback.Feedback
import com.prime.media.feedback.FeedbackViewState
import com.primex.core.MetroGreen2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FeedbackViewModel"

/**
 * The name of the Firebase event used to log feedback submissions.
 */
private const val FIREBASE_EVENT_FEEDBACK = "app_feedback"

/**
 * The key for the rating parameter in the feedback event.
 * This parameter is optional and represents a rating from 1 to 5.
 */
private const val PARAM_RATING = "rating"

/**
 * The prefix for the feedback text parameters in the feedback event.
 * Feedback text is split into multiple lines, each with a maximum length of [MAX_LINE_CHAR_SIZE].
 * The key for each line is constructed by appending an index to this prefix (e.g., "feedback_line_0", "feedback_line_1", etc.).
 * Currently, up to 10 lines are supported, but this may be increased in the future.
 */
private const val PARAM_FEEDBACK_LINE = "feedback_line_"

/**
 * The key for the feedback type parameter in the feedback event.
 * This parameter indicates the category of the feedback, such as "Suggestion", "Bug Report", "Question", or "Other".
 * It corresponds to the `tag` property in the feedback submission data.
 */
private const val PARAM_TAG = "tag"

/**
 * The maximum number of characters allowed in each line of feedback text.
 */
private const val MAX_LINE_CHAR_SIZE = 100


@HiltViewModel
class FeedbackViewModel @Inject constructor(
    delegate: SystemDelegate
) : ViewModel(), FeedbackViewState, SystemDelegate by delegate {

    override var rate: Int by mutableIntStateOf(Feedback.RATING_NOT_SET)
    val analytics = Firebase.analytics
    override var feedback: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var tag: String by mutableStateOf(Feedback.FEEDBACK_TAG_FEEDBACK)

    /**
     * Splits a given text into lines with a maximum length, preserving word boundaries.
     *
     * @param text The text to split into lines.
     * @return A list of lines, each with a length less than or equal to [MAX_LINE_CHAR_SIZE], and words not broken across lines.
     */
    private fun process(text: String): List<String> {
        if (text.isBlank()) return emptyList() // Handle empty or blank input
        val words = text.split(" ") // Split the text into words
        val lines = mutableListOf<String>() // Initialize a list to store the resulting lines
        var currentLine = StringBuilder() // Use StringBuilder for efficient string concatenation

        for (word in words) {
            // Check if adding the next word would exceed the maximum line length
            if (currentLine.length + word.length + 1 > MAX_LINE_CHAR_SIZE) {
                lines.add(
                    currentLine.toString().trim()
                ) // Add the current line to the list (trimmed)
                currentLine = StringBuilder() // Start a new line
            }
            currentLine.append("$word ") // Append the word and a space to the current line
        }

        // Add the last line if it's not empty
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString().trim())
        }

        return lines
    }

    override fun submit(navController: NavController) {
        viewModelScope.launch {
            val status = kotlin.runCatching {
                val text = feedback.text
                val lines = process(text) // Split feedback into lines with max length

                // Validate feedback length
                if (text.isBlank()) {
                    showToast("The Feedback text is blank")
                    return@launch // Exit the coroutine if feedback is too short
                }

                // Log the feedback event to Firebase Analytics
                analytics.logEvent(FIREBASE_EVENT_FEEDBACK) {
                    param(PARAM_RATING, rate.toLong()) // Log the rating
                    param(PARAM_TAG, tag) // Log the feedback tag (category)
                    // Log each line of feedback text
                    for ((index, line) in lines.withIndex()) {
                        param("$PARAM_FEEDBACK_LINE$index", line)
                    }
                }

                // Show a thank you message
                GlobalScope.launch {
                    showSnackbar(
                        icon = Icons.Outlined.Feedback,
                        message = R.string.feedback_msg_success,
                        accent = Color.MetroGreen2
                    )
                }
                // Navigate back to the previous screen
                navController.navigateUp()
            }
            if (status.isFailure)
                showToast(R.string.feedback_msg_failure)
        }
    }
}