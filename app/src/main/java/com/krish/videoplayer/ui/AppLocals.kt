package com.krish.videoplayer.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import com.krish.videoplayer.settings.AppSettings
import com.krish.videoplayer.settings.SettingsRepository

/** Current persisted settings, provided once by MainActivity. */
val LocalAppSettings = compositionLocalOf { AppSettings() }

val LocalSettingsRepository = staticCompositionLocalOf<SettingsRepository> {
    error("SettingsRepository not provided")
}
