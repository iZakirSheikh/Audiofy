/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 14-05-2025.
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

package com.zs.audiofy.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.audios.RouteAudios
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.directory.Directory
import com.zs.audiofy.common.fileSizeFormatted
import com.zs.audiofy.videos.RouteVideos
import com.zs.compose.foundation.foreground
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.text.Label
import com.zs.core.common.PathUtils
import com.zs.core.store.models.Folder
import kotlin.math.abs

private const val TAG = "Folders"
private val FOLDER_SHAPE =
    RoundedCornerShape(13)

private fun colorFrom(name: String): Color {
    val hash = abs(name.hashCode())

    // Restrict hue to warm tones: orange → red → brownish
    val warmHue = 10f + (hash % 40)      // 10° to 50°

    // Low-to-mid saturation for muted tones
    val saturation = 0.3f + (hash % 20) / 100f  // 0.3 to 0.5

    // Lowered lightness (for a more grounded, warm background)
    val lightness = 0.38f + (hash % 10) / 100f   // 0.68 to 0.78

    return Color.hsl(warmHue, saturation, lightness)
}

val IndicatorModifier = Modifier.graphicsLayer {
    clip = true
    scaleX = 0.93f
    shape = FOLDER_SHAPE
}
val ImageModifier = Modifier
    .graphicsLayer {
        shape = FOLDER_SHAPE
        clip = true
        translationY = 6.dp.toPx()
    }
    .aspectRatio(1.7f / 1f)
    .foreground(Color.Black.copy(0.3f))


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

        // Top Image
        Box(
            content = {
                // IndicatorLine
                Spacer(
                    Modifier
                        .then(IndicatorModifier)
                        .matchParentSize()
                        .drawBehind() {
                            drawRect(colorFrom(value.name), size = size.copy(height = 5.dp.toPx()))
                        }
                )

                // Image
                val elevation = if (kotlin.random.Random.nextBoolean()) 0.5.dp else 1.dp
                AsyncImage(
                    model = value.artworkUri,
                    contentDescription = value.name,
                    modifier = Modifier
                        .then(ImageModifier)
                        .background(AppTheme.colors.background(elevation = elevation)),
                    contentScale = ContentScale.Crop
                )

                //
                val isRemovable = PathUtils.isRemovableStorage(value.path)
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
            }
        )


        val ctx = LocalContext.current
        // TextLabel
        Label(
            modifier = Modifier.padding(
                top = ContentPadding.normal,
                start = ContentPadding.small
            ),
            style = AppTheme.typography.body2,
            fontWeight = FontWeight.Normal,
            text = value.name
        )

        // More Info
        Label(
            text = pluralStringResource(
                R.plurals.files_d,
                value.count,
                value.count
            ) + " - " + ctx.fileSizeFormatted(value.size.toLong()),
            style = AppTheme.typography.body3,
            color = AppTheme.colors.onBackground.copy(ContentAlpha.medium),
            modifier = Modifier.padding(start = ContentPadding.small),
        )
    }
)

@Composable
fun Folders(viewState: FoldersViewState) {
    val navController = LocalNavController.current
    Directory(
        viewState,
        key = Folder::path,
        minSize = 100.dp,
        itemContent = {
            Folder(
                it,
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        navController.navigate(
                            when {
                                viewState.ofAudios -> RouteAudios(
                                    RouteAudios.SOURCE_FOLDER,
                                    it.path
                                )

                                else -> RouteVideos(it.path)
                            }
                        )
                    },
            )
        }
    )
}