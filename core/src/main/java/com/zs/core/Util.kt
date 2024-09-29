/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 29-09-2024.
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

package com.zs.core

import android.content.SharedPreferences
import kotlin.reflect.KProperty


/**
 * Gets a value of type [T] from [SharedPreferences] with an optional default value.
 *
 * This is an inline extension function with operator overloading for the `get` operator,
 * allowing you to access values using the `[]` syntax (e.g., `prefs["key"]`).
 *
 * @param key The key for the preference.
 * @param defaultValue An optional default value to return if the key is not found.
 * @return The value of type [T] associated with the key, or `defaultValue` if the key is not found.
 *         Returns `null` if the key is not found and no default value is provided, or if the type [T] is not supported.
 */
@Suppress("UNCHECKED_CAST")
inline operator fun <reified T> SharedPreferences.get(key: String, defaultValue: T): T {
    return when (T::class) {
        String::class -> getString(key, defaultValue as? String) as T
        Int::class -> getInt(key, defaultValue as? Int ?: 0) as T
        Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T
        Float::class -> getFloat(key, defaultValue as? Float ?: 0f) as T
        Long::class -> getLong(key, defaultValue as? Long ?: 0L) as T
        Set::class -> getStringSet(key, defaultValue as? Set<String>) as T
        else -> error("Unsupported type ${T::class}")
    }
}

/**
 * Puts a value of type [T] into [SharedPreferences].
 *
 * This is an inline extension function with operator overloading for the `set` operator,
 * allowing you to put values using the `[]` syntax (e.g., `prefs["key"] = value`).
 *
 * @param key The key for the preference.
 * @param value The value of type [T] to be put.
 * @return Returns `true` if the value was successfully put, `false` otherwise.
 */
@Suppress("UNCHECKED_CAST")
inline operator fun <reified T> SharedPreferences.set(key: String, value: T) {
    with(edit()) {
        when (T::class) {
            String::class -> putString(key, value as? String)
            Int::class -> putInt(key, value as? Int ?: 0)
            Boolean::class -> putBoolean(key, value as? Boolean ?: false)
            Float::class -> putFloat(key, value as? Float ?: 0f)
            Long::class -> putLong(key, value as? Long ?: 0L)
            Set::class -> putStringSet(key, value as? Set<String>)
            else -> error("Unsupported type ${T::class}")
        }.apply()
    }
}

/** Permits property delegation of `val`s using `by` for [Result]. */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> Result<T>.getValue(thisObj: Any?, property: KProperty<*>): T? = getOrNull()

/**
 * Returns the value if this [Result] is a [Result.Success], or `null` if it is a [Result.Failure].
 */
operator fun <T> Result<T>.component1(): T? = getOrNull()

/**
 * Returns the exception if this [Result] is a [Result.Failure], or `null` if it is a [Result.Success].
 */
operator fun <T> Result<T>.component2(): Throwable? = exceptionOrNull()

