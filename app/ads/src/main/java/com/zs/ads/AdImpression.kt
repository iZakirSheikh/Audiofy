/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 06-07-2024.
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

package com.zs.ads

import com.ironsource.mediationsdk.impressionData.ImpressionData

/**
 * @see AdInfo
 */
@JvmInline
value class AdImpression internal constructor(private val value: ImpressionData){
    val format: String get() = value.adUnit
    val network: String get() = value.adNetwork
    val id: String get() = value.instanceId
    val country: String get() = value.country
    val revenue: Double get() = value.revenue
    val name: String get() = value.instanceName
}