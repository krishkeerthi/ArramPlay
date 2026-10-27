package com.krish.videoplayer.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

enum class DeviceType { Phone, Tablet, Tv }

val LocalDeviceType = staticCompositionLocalOf { DeviceType.Phone }

fun Context.deviceType(): DeviceType {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    return when {
        isTv -> DeviceType.Tv
        resources.configuration.smallestScreenWidthDp >= 600 -> DeviceType.Tablet
        else -> DeviceType.Phone
    }
}

/** 5% overscan-safe area per the Android TV design guidelines (48dp × 27dp on a 960×540dp screen). */
val TvSafePadding = PaddingValues(horizontal = 48.dp, vertical = 27.dp)
