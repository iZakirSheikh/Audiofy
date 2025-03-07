/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 03-03-2025.
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

import androidx.compose.foundation.shape.GenericShape

// TODO: TagShape from a val to a class accepting cornerRadius and maybe use layoutDirection for RTL support.

/**
 * Represents the shape of the [Tag](https://user-images.githubusercontent.com/6584143/125108986-63782580-e100-11eb-9438-19e5594f4fea.png)
 *
 */
val TagShape = GenericShape { size, _ ->
    val w = size.width / 27f
    val h = size.height / 11f

    moveTo(5f * w, 0f * h)
    lineTo(25f * w, 0f * h)
    cubicTo(26f * w, 0f, 27f * w, 1f * h, 27f * w, 2f * h)
    lineTo(27f * w, 9f * h)
    cubicTo(27f * w, 10f * h, 26f * w, 11f * h, 25f * w, 11f * h)
    lineTo(5f * w, 11f * h)
    lineTo(1f * w, 7f * h)
    cubicTo(0f * w, 6f * h, 0f * w, 5f * h, 1f * w, 4f * h)
}