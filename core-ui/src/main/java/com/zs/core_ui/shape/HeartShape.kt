/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 03-03-2025.
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

/**
 * Represents the shape of the [Heart](https://user-images.githubusercontent.com/6584143/125109896-8e16ae00-e101-11eb-99ee-4d4a7696c667.png)
 */
val HeartShape = GenericShape { size, _ ->
    val h = size.height
    val w = size.width
    lineTo(0.5f*w, 	0.25f*h)
    cubicTo(0.5f*w, 	0.225f*h, 	0.458333333333333f*w, 	0.125f*h, 	0.291666666666667f*w, 	0.125f*h)
    cubicTo(0.0416666666666667f*w, 	0.125f*h, 	0.0416666666666667f*w, 	0.4f*h, 	0.0416666666666667f*w, 	0.4f*h)
    cubicTo(0.0416666666666667f*w, 	0.583333333333333f*h, 	0.208333333333333f*w, 	0.766666666666667f*h, 	0.5f*w, 	0.916666666666667f*h)
    cubicTo(0.791666666666667f*w, 	0.766666666666667f*h, 	0.958333333333333f*w, 	0.583333333333333f*h, 	0.958333333333333f*w, 	0.4f*h)
    cubicTo(0.958333333333333f*w, 	0.4f*h, 	0.958333333333333f*w, 	0.125f*h, 	0.708333333333333f*w, 	0.125f*h)
    cubicTo(0.583333333333333f*w, 	0.125f*h, 	0.5f*w, 	0.225f*h, 	0.5f*w, 	0.25f*h)
    close()
}