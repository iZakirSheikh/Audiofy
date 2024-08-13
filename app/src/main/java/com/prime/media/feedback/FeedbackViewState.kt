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

import androidx.annotation.IntRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavController
import com.prime.media.core.Route

/**
 * A route for submitting feedback.
 */
object RouteFeedback : Route

/**
 * Typealias for [FeedbackViewState.Companion] for easy access to its constants.
 */
typealias Feedback = FeedbackViewState.Companion

/**
 * Represents the view state for the feedback screen.
 */
interface FeedbackViewState {
    companion object {
        /**
         * The maximum number of characters allowed in the feedback text.*/
        const val TEXT_LIMIT_CHARS = 500

        /**
         * The default value for [rate] indicating that no rating has been set.
         */
        const val RATING_NOT_SET = -1

        /**
         * The valid range for the rating value.
         */
        val RATING_RANGE = 1..5

        /**
         * The maximum number of characters allowed in the feedback text.
         */
        const val FEEDBACK_MAX_CHAR_COUNT = 500

        val FEEDBACK_TAG_OTHER = "tag_other"
        val FEEDBACK_TAG_BUG = "tag_bug_report"
        val FEEDBACK_TAG_SUGGESTION = "tag_suggestion"
        val FEEDBACK_TAG_FEEDBACK = "tag_feedback"
        val FEEDBACK_TAG_FEATURE_REQUEST = "tag_feature_request"
    }

    /**
     * The type or category of feedback this submission represents.
     * For example, this could be "Suggestion", "Bug Report","Question", etc.
     * Defaults to [FEEDBACK_TAG_FEEDBACK] if not explicitly set.
     */
    var tag: String

    /**
     * A value between 1 and 5 that represents the rating given by the user.
     * A value of [RATING_NOT_SET] indicates that no rating has been set.
     */
    @setparam:IntRange(from = 1, to = 5)
    var rate: Int

    /**
     * The feedback text entered by the user.
     */
    var feedback: TextFieldValue

    /**
     * Sends the feedback to the server.
     */
    fun submit(navController: NavController)
}
