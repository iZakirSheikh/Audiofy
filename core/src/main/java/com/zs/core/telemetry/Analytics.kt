/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 09-05-2025.
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

package com.zs.core.telemetry

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.Size

/**
 * Interface for the analytics module. This allows for decoupling the core module from
 * the specific analytics implementation.
 */
interface Analytics {
    /**
     * Records a [Throwable] for error tracking.
     * @param throwable The throwable to record.
     */
    fun record(throwable: Throwable)

    /**
     * Logs an analytics event.
     * @param name The name of the event (must be between 1 and 40 characters).
     * @param params A [Bundle] containing the parameters for the event.
     */
    fun logEvent(@Size(min = 1L, max = 40L) name: String, params: Bundle)


    companion object {

        const val EVENT_SCREEN_VIEW: String = "screen_view"
        const val PARAM_SCREEN_NAME: String = "screen_name"

        /**
         * A fallback implementation of the [Analytics] interface that logs events and errors to Logcat.
         */
        private class FallbackAnalytics : Analytics {

            // Log events to Logcat
            private val TAG = "FallbackAnalytics"

            init {
                logEvent("AnalyticsNotLoaded", params = Bundle.EMPTY)
            }

            override fun record(throwable: Throwable) {
                Log.e(TAG, throwable.message, throwable)
            }

            override fun logEvent(name: String, params: Bundle) {
                Log.i(TAG, "$name : params = $params")
            }
        }

        /**
         * Creates an instance of the [Analytics] interface. This factory method attempts
         * to load a concrete implementation of the analytics interface using reflection.
         * If the implementation is not found or loading fails, a [FallbackAnalytics]
         * instance is returned.
         * @param context The application context.
         */
        operator fun invoke(context: Context): Analytics {
            // Attempt to load the concrete Analytics implementation using reflection.
            // This approach decouples the core module from the specific analytics library.
            return runCatching() {
                // Dynamically load the class for the analytics implementation.
                // "com.zs.telemetry.AnalyticsImplKt" is expected to be the compiled
                // class name of the Kotlin file containing the Analytics implementation.
                val analyticsImpl =
                    Class.forName("com.zs.feature.telemetry.AnalyticsImpl")
                // Get the constructor that takes a Context as an argument.
                val constructor = analyticsImpl.getDeclaredConstructor(Context::class.java)
                // Create a new instance of the Analytics implementation using the constructor.
                // Cast the result to the Analytics interface.
                constructor.newInstance(context) as Analytics
            }.getOrNull() ?: FallbackAnalytics()
            // If any error occurs during the reflection process (e.g., class not found,
            // constructor not found), `getOrNull()` will return null, and we fall back
            // to the FallbackAnalytics implementation.
        }
    }
}