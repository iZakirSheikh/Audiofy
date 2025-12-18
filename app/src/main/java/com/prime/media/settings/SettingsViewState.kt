package com.prime.media.settings

import androidx.compose.runtime.Stable
import com.prime.media.common.SystemFacade
import com.primex.preferences.Key


@Stable
interface SettingsViewState {

    val save: Boolean

    var trashCanEnabled: Boolean
    var preferCachedThumbnails: Boolean
    var enabledBackgroundBlur: Boolean
    var fontScale: Float
    var minTrackLengthSecs: Int
    var inAppAudioEffectsEnabled: Boolean
    var gridItemSizeMultiplier: Float
    var fabLongPressLaunchConsole: Boolean
    var isFileGroupingEnabled: Boolean
    var isSplashAnimWaitEnabled: Boolean

    /**
     * Commits [com.prime.media.common.AppConfig] to memory.
     */
    fun commit(facade: SystemFacade)

    fun discard()

    /** Sets the value of the given [key] to [value]. */
    fun <S, O> set(key: Key<S, O>, value: O)
}