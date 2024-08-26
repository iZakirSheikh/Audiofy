package com.prime.media.impl

import androidx.lifecycle.ViewModel
import com.prime.media.config.PersonalizeViewState
import com.prime.media.core.playback.Remote
import com.prime.media.effects.AudioFx
import com.prime.media.settings.Settings
import com.primex.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class PersonalizeViewModel @Inject constructor(
    private val preferences: Preferences,
) : ViewModel(), PersonalizeViewState {

    override fun setInAppWidget(id: String) {
        preferences[Settings.GLANCE] = id
    }

}