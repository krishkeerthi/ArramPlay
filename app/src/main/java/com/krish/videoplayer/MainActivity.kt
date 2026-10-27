package com.krish.videoplayer

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.krish.videoplayer.settings.AppSettings
import com.krish.videoplayer.settings.SettingsRepository
import com.krish.videoplayer.ui.DeviceType
import com.krish.videoplayer.ui.LocalAppSettings
import com.krish.videoplayer.ui.LocalDeviceType
import com.krish.videoplayer.ui.LocalSettingsRepository
import com.krish.videoplayer.ui.deviceType
import com.krish.videoplayer.ui.theme.VideoPlayerTheme
import com.krish.videoplayer.ui.theme.isDark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    // null until DataStore's first read — the splash screen stays up until then so the
    // first frame already has the right theme (no light→dark flash).
    private val settingsState: StateFlow<AppSettings?> by lazy {
        settingsRepository.settings.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    private var isOnPlayerRoute = false
        set(value) {
            field = value
            refreshPipParams()
        }

    var isInPipMode by mutableStateOf(false)
        private set

    // ChannelStream registers itself here so PiP-window play/pause clicks reach the player.
    private var pipPlayerToggle: (() -> Unit)? = null
    private var pipPlayerIsPlaying: () -> Boolean = { false }
    private var pipSourceRect: android.graphics.Rect? = null

    fun registerPipPlayer(toggle: () -> Unit, isPlaying: () -> Boolean) {
        pipPlayerToggle = toggle
        pipPlayerIsPlaying = isPlaying
    }

    fun unregisterPipPlayer() {
        pipPlayerToggle = null
        pipPlayerIsPlaying = { false }
        pipSourceRect = null
    }

    // Never on TV: Home must stop playback there, and Settings hides the PiP toggle.
    fun supportsPip(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            deviceType() != DeviceType.Tv &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /** Player bounds — lets the system morph the video into the PiP window instead of cross-fading. */
    fun updatePipSourceRect(bounds: Rect) {
        val rect = android.graphics.Rect(
            bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt()
        )
        if (rect != pipSourceRect) {
            pipSourceRect = rect
            refreshPipParams()
        }
    }

    /** Call after exoPlayer.isPlaying flips so the PiP window's icon updates. */
    fun refreshPipParams() {
        if (supportsPip()) {
            try { setPictureInPictureParams(buildPipParams()) } catch (_: IllegalStateException) {}
        }
    }

    fun enterPip() {
        if (supportsPip()) {
            try { enterPictureInPictureMode(buildPipParams()) } catch (_: IllegalStateException) {}
        }
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_TOGGLE) {
                pipPlayerToggle?.invoke()
                refreshPipParams()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { settingsState.value == null }
        // On Android 12+ removing the splash re-applies the theme's bar appearance; re-assert ours.
        splash.setOnExitAnimationListener { provider ->
            provider.remove()
            applySystemBarStyle(systemBarsDark)
        }
        enableEdgeToEdge()
        val deviceType = deviceType()

        setContent {
            val settings by settingsState.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val darkTheme = current.themeMode.isDark()

            // Status/navigation bar icons follow the in-app theme, not just the system one.
            DisposableEffect(darkTheme) {
                systemBarsDark = darkTheme
                applySystemBarStyle(darkTheme)
                onDispose {}
            }

            CompositionLocalProvider(
                LocalAppSettings provides current,
                LocalSettingsRepository provides settingsRepository,
                LocalDeviceType provides deviceType,
            ) {
                VideoPlayerTheme(darkTheme = darkTheme, dynamicColor = current.dynamicColor) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AppNavigation(current)
                    }
                }
            }
        }
        checkForAppUpdate()
    }

    private var systemBarsDark = false

    private fun applySystemBarStyle(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onResume() {
        super.onResume()
        // An immediate update that was interrupted (app killed, user left) must be resumed.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startImmediateUpdate(info)
            }
        }
    }

    @Composable
    fun AppNavigation(settings: AppSettings) {
        val navController = rememberNavController()
        val isPhone = LocalDeviceType.current == DeviceType.Phone

        // "Resume last channel": jump straight into the player once per launch.
        var resumeHandled by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!resumeHandled) {
                resumeHandled = true
                val last = settings.lastChannelId
                if (settings.resumeLastChannel && last != null) navController.navigate(Channel(last))
            }
        }

        NavHost(
            navController = navController,
            startDestination = ChannelList,
            enterTransition = { sharedAxisEnter(forward = true) },
            exitTransition = { sharedAxisExit(forward = true) },
            popEnterTransition = { sharedAxisEnter(forward = false) },
            popExitTransition = { sharedAxisExit(forward = false) },
        ) {
            composable<ChannelList> {
                ChannelsPreview(navController)
            }
            composable<Channel>(
                enterTransition = { fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.96f) },
                exitTransition = { fadeOut(tween(250)) },
                popExitTransition = { fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.96f) },
            ) { backStackEntry ->
                val channel: Channel = backStackEntry.toRoute()
                // Phones play in landscape (either way up); tablets and TVs keep their orientation.
                DisposableEffect(isPhone) {
                    isOnPlayerRoute = true
                    if (isPhone) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    onDispose {
                        isOnPlayerRoute = false
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
                ChannelStream(navController, window, channel.channelId)
            }
            composable<Settings> {
                SettingsScreen(navController)
            }
            composable<AboutUs> {
                AboutUs(navController)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 12+ enters PiP on its own via setAutoEnterEnabled (smoother gesture-nav animation).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && isOnPlayerRoute && autoPipEnabled()) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            ContextCompat.registerReceiver(
                this, pipReceiver, IntentFilter(ACTION_PIP_TOGGLE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            try { unregisterReceiver(pipReceiver) } catch (_: IllegalArgumentException) {}
        }
    }

    private fun autoPipEnabled(): Boolean = settingsState.value?.autoPip ?: true

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(buildPlayPauseAction()))
        pipSourceRect?.let { builder.setSourceRectHint(it.centeredAspect(16, 9)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isOnPlayerRoute && autoPipEnabled())
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPlayPauseAction(): RemoteAction {
        val playing = pipPlayerIsPlaying()
        val iconRes = if (playing) android.R.drawable.ic_media_pause
                      else android.R.drawable.ic_media_play
        val title = getString(if (playing) R.string.pause else R.string.play)
        val intent = Intent(ACTION_PIP_TOGGLE).setPackage(packageName)
        val pi = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pi)
    }

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startImmediateUpdate(info)
            }
        }
    }

    private fun startImmediateUpdate(info: com.google.android.play.core.appupdate.AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            info,
            updateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
        )
    }

    companion object {
        private const val ACTION_PIP_TOGGLE = "com.krish.videoplayer.PIP_TOGGLE"
    }
}

// Material "shared axis X" — a short slide plus fade, the standard for peer navigation.
private const val NAV_ANIM_MS = 320

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedAxisEnter(forward: Boolean): EnterTransition =
    slideInHorizontally(tween(NAV_ANIM_MS, easing = FastOutSlowInEasing)) { w -> (if (forward) w else -w) / 10 } +
        fadeIn(tween(NAV_ANIM_MS, delayMillis = 60))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedAxisExit(forward: Boolean): ExitTransition =
    slideOutHorizontally(tween(NAV_ANIM_MS, easing = FastOutSlowInEasing)) { w -> (if (forward) -w else w) / 10 } +
        fadeOut(tween(NAV_ANIM_MS / 2))

/** Largest [w]:[h] rect centred in this one — the area the video actually occupies when letterboxed. */
private fun android.graphics.Rect.centeredAspect(w: Int, h: Int): android.graphics.Rect {
    if (width() <= 0 || height() <= 0) return this
    return if (width() * h > height() * w) {
        val targetW = height() * w / h
        val left = centerX() - targetW / 2
        android.graphics.Rect(left, top, left + targetW, bottom)
    } else {
        val targetH = width() * h / w
        val t = centerY() - targetH / 2
        android.graphics.Rect(left, t, right, t + targetH)
    }
}
