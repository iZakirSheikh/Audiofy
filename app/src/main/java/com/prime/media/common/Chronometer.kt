/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 05-01-2025.
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

import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf

/**
 * A composable chronometer that tracks elapsed time.
 *
 * This class provides a way to manage and display a time duration, similar to a stopwatch.
 * It uses a [MutableLongState] internally to hold the current time value, allowing for
 * manual setting and retrieval of the elapsed time.
 *
 * The chronometer's value represents the elapsed time in milliseconds.
 *
 * @property value The current elapsed time in milliseconds. This property can be used to
 *                 get or set the chronometer's current value.
 */
@JvmInline
value class Chronometer private constructor(private val state: MutableLongState){

    /**
     * Constructs a [Chronometer] with an initial time value.
     *
     * @param initial The initial time value in milliseconds.
     */
    constructor(initial: Long): this(mutableLongStateOf(initial))

    /**
     * Gets or sets the current elapsed time in milliseconds.
     *
     * When setting the value, the chronometer's internal state is updated to reflect the new time.
     * When getting the value, the current elapsed time is returned.
     */
    var value get() = state.longValue
        set(value){
            state.longValue = value
        }
}