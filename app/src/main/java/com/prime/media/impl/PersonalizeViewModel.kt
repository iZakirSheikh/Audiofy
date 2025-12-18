/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 17-10-2024.
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

package com.prime.media.impl

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import com.prime.media.personalize.PersonalizeViewState
import com.prime.media.common.Registry
import com.primex.preferences.Key

class PersonalizeViewModel : KoinViewModel(), PersonalizeViewState {
    override fun setInAppWidget(id: String) {
        preferences[Registry.GLANCE] = id
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, id)
        }
    }

    override fun setArtworkShapeKey(key: String) {
        preferences[Registry.ARTWORK_SHAPE_KEY] = key
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, key)
        }
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        preferences[key] = value
    }
}