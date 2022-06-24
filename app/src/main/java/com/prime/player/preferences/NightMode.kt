package com.prime.player.preferences

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG = "NightMode"
@Composable
fun isCurrentThemeDark(): Boolean {
    val preferences = Preferences.get(LocalContext.current)
    val mode by with(preferences) {
        getDefaultNightMode().collectAsState()
    }
    var manager: AutoNightModeManager? = remember {
        null
    }

    return when (mode) {
        NightMode.YES -> true
        NightMode.NO -> false
        NightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        NightMode.AUTO_BATTER -> {
            val app = (LocalContext.current.applicationContext as Application)
            manager?.cleanup()
            manager = remember {
                AutoBatteryNightModeManager(app).apply {
                    init()
                }
            }
            manager.isDark.value
        }
        NightMode.AUTO_TIME -> {
            val app = (LocalContext.current.applicationContext as Application)
            manager?.cleanup()
            manager = remember {
                AutoTimeNightModeManager(app).apply {
                    init()
                }
            }
            manager.isDark.value
        }
    }

}


private abstract class AutoNightModeManager(
    private val context: Application,
) {
    private var mReceiver: BroadcastReceiver? = null
    lateinit var isDark: State<Boolean>


    fun init() {
        cleanup()
        // update for first time
        isDark = mutableStateOf(
            shouldApplyNightMode()
        )
        val filter = createIntentFilterForBroadcastReceiver()
        if (filter.countActions() == 0) {
            // Null or empty IntentFilter, skip
            return
        }
        if (mReceiver == null) {
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    runBlocking {
                        val isDark = shouldApplyNightMode()
                        Log.d(TAG, "onReceive: $isDark")
                        withContext(Dispatchers.IO) {
                            (this@AutoNightModeManager.isDark as MutableState).value = isDark
                        }
                    }
                }
            }
        }
        context.registerReceiver(mReceiver, filter)
    }


    protected abstract fun shouldApplyNightMode(): Boolean

    fun cleanup() {
        if (mReceiver != null) {
            try {
                context.unregisterReceiver(mReceiver)
            } catch (e: IllegalArgumentException) {
                // If the receiver has already been unregistered, unregisterReceiver() will
                // throw an exception. Just ignore and carry-on...
            }
            mReceiver = null
        }
    }

    fun isListening(): Boolean = mReceiver != null

    abstract fun createIntentFilterForBroadcastReceiver(): IntentFilter
}

private class AutoTimeNightModeManager(context: Application) :
    AutoNightModeManager(context) {

    private val mTwilightManager: TwilightManager by lazy {
        TwilightManager.getInstance(context)
    }

    override fun shouldApplyNightMode() = mTwilightManager.isNight

    override fun createIntentFilterForBroadcastReceiver(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        filter.addAction(Intent.ACTION_TIME_TICK)
        return filter
    }
}

private class AutoBatteryNightModeManager(context: Application) :
    AutoNightModeManager(context) {
    private val mPowerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    override fun shouldApplyNightMode() = mPowerManager.isPowerSaveMode

    override fun createIntentFilterForBroadcastReceiver(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        return filter
    }
}




