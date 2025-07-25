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

package com.zs.audiofy.common

import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import com.zs.core.common.Intent
import com.zs.core.store.MediaProvider

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

/**
 * Creates an Intent to share files using Google's Nearby Share (Quick Share) feature.
 *
 * This function constructs an intent that targets the specific activity in Google Play Services
 * responsible for sending files via Nearby Share.
 *
 * @param uri Vararg parameter representing one or more [Uri]s of the files to be shared.
 * @return An [Intent] configured to initiate a Nearby Share operation with the specified files.
 */
fun NearByShareIntent(vararg uri: Uri) =
    Intent("com.google.android.gms.SHARE_NEARBY") {
        component = ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.nearby.sharing.send.SendActivity"
        )
        // Add the URIs as extras.
        putParcelableArrayListExtra(
            Intent.EXTRA_STREAM,
            uri.toMutableList() as ArrayList<Uri>
        )
        // Set the MIME type to allow sharing of various file types.
        type = "*/*"
        // Specify supported MIME types.
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }

fun NearByShareIntent(vararg id: Long) =
    NearByShareIntent(
        *id.map {
            ContentUris.withAppendedId(MediaProvider.EXTERNAL_CONTENT_URI, it)
        }.toTypedArray()
    )

/**
 * @see ShareFilesIntent
 */
fun ShareFilesIntent(vararg uri: Uri) = Intent.createChooser(
    Intent(Intent.ACTION_SEND_MULTIPLE) {
        // Map selected IDs to content URIs.
        // TODO - Construct custom content uri.
        // Set the action to send multiple items.
        action = Intent.ACTION_SEND_MULTIPLE
        // Add the URIs as extras.
        putParcelableArrayListExtra(
            Intent.EXTRA_STREAM,
            uri.toMutableList() as ArrayList<Uri>
        )
        // Grant read permission to the receiving app.
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Set the MIME type to allow sharing of various file types.
        type = "*/*"
        // Specify supported MIME types.
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    },
    "Share files..."
)

/**
 * Creates an Intent to share multiple files using the system's share dialog.
 *
 * This function constructs an intent that allows the user to share one or more files
 * identified by their content URIs. It leverages the system's `Intent.createChooser`
 * to display a list of apps capable of handling the sharing action.
 *
 * @param id Vararg parameter representing the IDs of the media items to be shared.
 *           These IDs are used to construct content URIs.
 * @return An [Intent] configured to share multiple files, wrapped in a chooser dialog.
 */
fun ShareFilesIntent(vararg id: Long) = ShareFilesIntent(*id.map {
    ContentUris.withAppendedId(MediaProvider.EXTERNAL_CONTENT_URI, it)
}.toTypedArray())