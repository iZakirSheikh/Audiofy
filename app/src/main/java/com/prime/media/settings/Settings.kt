package com.prime.media.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.vector.ImageVector
import com.prime.media.core.FontFamily
import com.prime.media.core.NightMode
import com.primex.core.Text
import com.primex.preferences.Key

interface Settings {

    companion object {
        const val route = "settings"
    }

    val darkUiMode: Preference<NightMode>
    val font: Preference<FontFamily>
    val colorStatusBar: Preference<Boolean>
    val hideStatusBar: Preference<Boolean>
    val forceAccent: Preference<Boolean>
    val fontScale: Preference<Float>
    fun <S, O> set(key: Key<S, O>, value: O)
}

@Immutable
data class Preference<out P>(
    val value: P,
    @JvmField val title: Text,
    val vector: ImageVector? = null,
    @JvmField val summery: Text? = null,
)