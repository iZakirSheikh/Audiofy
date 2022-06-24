package com.prime.player.extended

import androidx.annotation.NonNull
import androidx.annotation.Size
import androidx.compose.ui.unit.dp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase


object Anim {
    /**A Duration of 250 mills*/
    const val DURATION_SHORT = 250
    /**A Duration of 500 mills*/
    const val DURATION_MEDIUM = 500
    /**A Duration of 750 mills*/
    const val DURATION_LONG = 750
}

object Padding {
    /**A padding of 4 dp*/
    val SMALL = 4.dp
    /**A padding of 8 dp*/
    val MEDIUM = 8.dp
    /**A padding of 16 dp*/
    val LARGE = 16.dp
    /**A padding of 32 dp*/
    val EXTRA_LARGE = 32.dp
}



object Elevation {
    /**An Elevation of 0 dp*/
    val NONE = 0.dp
    /**An Elevation of 6 dp*/
    val LOW = 6.dp
    /**An Elevation of 12 dp*/
    val MEDIUM = 12.dp
    /**An Elevation of 20 dp*/
    val HIGH = 20.dp
    /**An Elevation of 30 dp*/
    val EXTRA_HIGH = 30.dp
}

object Alpha {
    const val Divider = 0.12f

    const val Indication = 0.1f
}

