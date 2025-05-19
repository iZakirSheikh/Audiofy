/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 19-05-2025.
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

package com.prime.media.common

import android.content.Intent
import android.net.Uri
import com.zs.core.common.Intent

/**
 * A utility function to create an [Intent] for sending feedback via email.
 *
 * This function constructs an [Intent] with the action [Intent.ACTION_SENDTO]
 * and pre-fills the recipient email address to "helpline.prime.zs@gmail.com".
 * The subject of the email is set to the provided [subject] string.
 *
 * @param subject The subject line for the feedback email.
 * @return An [Intent] configured to send an email to the specified address with the given subject.
 */
fun FeedbackIntent(subject: String) = Intent(Intent.ACTION_SENDTO) {
    data = Uri.parse("mailto:helpline.prime.zs@gmail.com")
    putExtra(Intent.EXTRA_SUBJECT, subject)
}