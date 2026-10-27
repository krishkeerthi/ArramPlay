package com.krish.videoplayer.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { System, Light, Dark }

/** How the video frame is fitted to the screen. Maps 1:1 to AspectRatioFrameLayout modes. */
enum class VideoScale { Fit, Fill, Zoom }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val videoScale: VideoScale = VideoScale.Fit,
    val resumeLastChannel: Boolean = false,
    val autoPip: Boolean = true,
    val lastChannelId: Int? = null,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME_MODE].toEnum(ThemeMode.System),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
                videoScale = prefs[Keys.VIDEO_SCALE].toEnum(VideoScale.Fit),
                resumeLastChannel = prefs[Keys.RESUME_LAST_CHANNEL] ?: false,
                autoPip = prefs[Keys.AUTO_PIP] ?: true,
                lastChannelId = prefs[Keys.LAST_CHANNEL_ID],
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setVideoScale(scale: VideoScale) = dataStore.edit { it[Keys.VIDEO_SCALE] = scale.name }
    suspend fun setResumeLastChannel(enabled: Boolean) = dataStore.edit { it[Keys.RESUME_LAST_CHANNEL] = enabled }
    suspend fun setAutoPip(enabled: Boolean) = dataStore.edit { it[Keys.AUTO_PIP] = enabled }
    suspend fun setLastChannelId(id: Int) = dataStore.edit { it[Keys.LAST_CHANNEL_ID] = id }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val VIDEO_SCALE = stringPreferencesKey("video_scale")
        val RESUME_LAST_CHANNEL = booleanPreferencesKey("resume_last_channel")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val LAST_CHANNEL_ID = intPreferencesKey("last_channel_id")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
