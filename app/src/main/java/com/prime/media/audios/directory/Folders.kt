/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 04-02-2025.
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

package com.prime.media.audios.directory

import android.content.Context
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prime.media.common.Route
import com.prime.media.common.compose.ContentPadding
import com.prime.media.common.compose.LocalNavController
import com.prime.media.common.compose.directory.Directory
import com.prime.media.common.compose.directory.DirectoryViewState
import com.zs.compose.foundation.runCatching
import com.zs.compose.foundation.shapes.SquircleShape
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.text.Label
import com.zs.core.store.models.Folder
import java.io.File

object RouteAudioFolders : Route

/**
 * Formats a file size in bytes to a human-readable string.
 *
 * @param bytes Thefile size in bytes.
 * @return The formatted file size string.
 */
private fun Context.formattedFileSize(bytes: Long) =
    Formatter.formatFileSize(this, bytes)

/**
 * Checks if a given path corresponds to removable storage.
 *
 * @param path The path to check.
 * @return True if the path is on removable storage, false otherwise.
 */
private fun isRemovableStorage(path: String): Boolean {
    val externalStorageDirectory = Environment.getExternalStorageDirectory().absolutePath
    return runCatching(TAG) {
        !path.startsWith(externalStorageDirectory) && Environment.isExternalStorageRemovable(
            File(
                path
            ))
    } == true
}

private const val TAG = "Folders"
private val FOLDER_SHAPE = SquircleShape(0.55f)

@Composable
private fun Folder(
    value: Folder,
    modifier: Modifier = Modifier
) = Column(
    modifier = Modifier
        .clip(AppTheme.shapes.medium)  // clip the ripple
        .then(modifier),
    horizontalAlignment = Alignment.Start,
    content = {
        val elevation = if (kotlin.random.Random.nextBoolean()) 0.5.dp else 1.dp
        // Image
        Log.d(TAG, "Folder: ${value.artworkUri}")
        Box(
            content = {
                AsyncImage(
                    model = value.artworkUri,
                    contentDescription = value.name,
                    modifier = Modifier
                        .aspectRatio(1.0f)
                        .padding(ContentPadding.small)
                        .shadow(2.dp, FOLDER_SHAPE)
                        .background(AppTheme.colors.background(elevation = elevation)),
                    contentScale = ContentScale.Crop,
                    onError = {
                        Log.d(TAG, "Folder: ${it.result.throwable.message}")
                    }
                )

                val isRemovable = isRemovableStorage(value.path)
                if (!isRemovable) return@Box
                Icon(
                    imageVector = Icons.Outlined.SdStorage,
                    contentDescription = "removable card",
                    modifier = Modifier
                        .scale(0.8f)
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    tint = Color.White
                )
            },
        )

        val ctx = LocalContext.current
        // TextLabel
        Label(
            modifier = Modifier.padding(
                top = ContentPadding.medium,
                start = ContentPadding.medium
            ),
            style = AppTheme.typography.body2,
            fontWeight = FontWeight.Normal,
            text = value.name
        )

        // More Info
        Label(
            text = "${value.count} Files - ${ctx.formattedFileSize(value.size.toLong())}",
            style = AppTheme.typography.body3,
            color = AppTheme.colors.onBackground.copy(ContentAlpha.medium),
            modifier = Modifier.padding(start = ContentPadding.medium),
        )
    }
)

@Composable
fun Folders(viewState: DirectoryViewState<Folder>) {
    val navController = LocalNavController.current
    Directory(
        viewState,
        key = Folder::path,
        minSize = 90.dp,
        itemContent = {
            Folder(
                it,
                modifier = Modifier
                    .animateItem()
                    .clickable {
//                        val direction = Audios.direction(Audios.GET_FROM_FOLDER, it.path)
//                        navController.navigate(direction)
                    },
            )
        }
    )
}