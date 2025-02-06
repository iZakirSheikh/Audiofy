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

package com.prime.media.local

import android.content.Context
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.prime.media.common.Route
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.store.Audios
import com.primex.core.textResource
import com.primex.material2.Label
import com.zs.core.store.Artist
import com.zs.core.store.Folder
import com.zs.core_ui.AppTheme
import com.zs.core_ui.shape.FolderShape
import java.io.File
import com.prime.media.R

object RouteFolders : Route

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
    return !path.startsWith(externalStorageDirectory) && Environment.isExternalStorageRemovable(
        File(
            path
        )
    )
}

private const val TAG = "Folders"
private val FOLDER_SHAPE = FolderShape

@Composable
private fun Folder(
    value: Folder,
    modifier: Modifier = Modifier
) = Column(
    modifier = Modifier
        .clip(FOLDER_SHAPE)  // clip the ripple
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
                        .padding(AppTheme.padding.small)
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
                top = AppTheme.padding.medium,
                start = AppTheme.padding.medium
            ),
            style = AppTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            text = value.name
        )

        // More Info
        Label(
            text = "${value.count} Files - ${ctx.formattedFileSize(value.size.toLong())}",
            style = AppTheme.typography.caption.copy(fontSize = 10.sp),
            color = AppTheme.colors.onBackground.copy(ContentAlpha.medium),
            modifier = Modifier.padding(start = AppTheme.padding.medium),
        )
    }
)

@Composable
fun Folders(viewState: DirectoryViewState<Folder>) {
    val navController = LocalNavController.current
    Directory(
        viewState,
        key = Folder::path,
        itemContent = {
            Folder(
                it,
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        val direction = Audios.direction(Audios.GET_FROM_FOLDER, it.path)
                        navController.navigate(direction)
                    },
            )
        }
    )
}