/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 27-02-2025.
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

package com.zs.core_ui.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection as LD

/**
 *
 * Defines the Cloud Shape.
 *
 * Created by Zakir Sheikh on 27-02-2025.
 *
 * [Cloud Shape](https://private-user-images.githubusercontent.com/72340294/349795284-10875742-2277-45e5-b2a8-2dc3787a10c2.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NDA2MzY0OTMsIm5iZiI6MTc0MDYzNjE5MywicGF0aCI6Ii83MjM0MDI5NC8zNDk3OTUyODQtMTA4NzU3NDItMjI3Ny00NWU1LWIyYTgtMmRjMzc4N2ExMGMyLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTAyMjclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUwMjI3VDA2MDMxM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWUyZGU2Zjc2YWI4YzJkMmI2Nzg0YTY0YTI4NDkyNmEzYmMwODYwY2NkZmVhODU2MGQ5NjVhNmM2Nzc5OTBmMzYmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.3XE68Niu9EfcjMtU_BNQ4_xuksJKNVk0Pop-7lpHCjc)
 */
class CloudShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LD,
        density: Density,
    ): Outline {
        val path = Path()

        val width = size.width * 0.8f // Adjusted width
        val height = width * 0.4f // Adjusted height
        val offsetX = size.width * 0.1f // Adjusted offset
        val offsetY = size.height * 0.3f //Adjusted offset

        val radius = width * 0.1f

        path.moveTo(offsetX + width * 0.15f, offsetY + height * 0.5f)

        // Curve 1
        path.cubicTo(
            offsetX + width * 0.1f, offsetY + height * 0.3f,
            offsetX + width * 0.2f, offsetY + height * 0.1f,
            offsetX + width * 0.35f, offsetY + height * 0.2f
        )

        // Curve 2
        path.cubicTo(
            offsetX + width * 0.45f, offsetY + height * 0.1f,
            offsetX + width * 0.55f, offsetY + height * 0.1f,
            offsetX + width * 0.65f, offsetY + height * 0.2f
        )

        // Curve 3
        path.cubicTo(
            offsetX + width * 0.8f, offsetY + height * 0.1f,
            offsetX + width * 0.9f, offsetY + height * 0.3f,
            offsetX + width * 0.85f, offsetY + height * 0.5f
        )

        // Curve 4
        path.cubicTo(
            offsetX + width * 0.9f, offsetY + height * 0.7f,
            offsetX + width * 0.8f, offsetY + height * 0.9f,
            offsetX + width * 0.65f, offsetY + height * 0.8f
        )

        // Curve 5
        path.cubicTo(
            offsetX + width * 0.55f, offsetY + height * 0.9f,
            offsetX + width * 0.45f, offsetY + height * 0.9f,
            offsetX + width * 0.35f, offsetY + height * 0.8f
        )

        // Curve 6
        path.cubicTo(
            offsetX + width * 0.2f, offsetY + height * 0.9f,
            offsetX + width * 0.1f, offsetY + height * 0.7f,
            offsetX + width * 0.15f, offsetY + height * 0.5f
        )

        path.close()

        return Outline.Generic(path)
    }
}
