/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 16-06-2025.
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

package com.zs.core.playback

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory

/**
 * Creates a new instance of [NextRenderersFactory] using reflection. The renderer is provided as a dynamic feature module and might not be available at install time. The feature needs to be added to the APK on-demand.
 *
 * @param context The application context.
 * @return A new instance of [DefaultRenderersFactory], or `null` if an error occurs.
 */
@SuppressLint("UnsafeOptInUsageError")
internal fun DynamicRendererFactory(context: Context): DefaultRenderersFactory? {
    return runCatching() {
        val codexClass =
            Class.forName("com.zs.feature.codex.CodexKt") // Assuming the functionis in a Kotlin file named Codex.kt
        val codexMethod = codexClass.getDeclaredMethod("Codex", Context::class.java)
        codexMethod.invoke(
            null,
            context
        ) as? DefaultRenderersFactory // Static method, so first argument is null
    }.getOrNull()
}