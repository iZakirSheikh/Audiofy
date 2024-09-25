/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 31-07-2024.
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

import com.ironsource.mediationsdk.model.Placement


/**
 * A value class representing a reward earned from an ad placement.
 *
 * @property value The underlying [Placement] object that holds the reward details.
 * @property amount  The amount of the reward.
 * @property name The name or type of the reward.
 */
@JvmInline
value class Reward(internal val value: Placement){
    val amount get() = value.rewardAmount
    val name get() = value.rewardName
}