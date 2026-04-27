package com.krish.videoplayer

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.krish.videoplayer.ui.theme.BlackHeader
import com.krish.videoplayer.ui.theme.VideoPlayerTheme


class MainActivity : ComponentActivity() {

    private val REQUEST_CODE_UPDATE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = BlackHeader.toArgb()
        window.navigationBarColor = BlackHeader.toArgb()

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            VideoPlayerTheme {
                AppNavigation()
            }
        }
        checkForAppUpdate(this)
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = ChannelList) {
            composable<ChannelList> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ChannelsPreview(navController, window)
            }
            composable<Channel> { backStackEntry ->
                val channel: Channel = backStackEntry.toRoute()
                requestedOrientation = if (!isPhone(LocalContext.current)) {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                ChannelStream(navController, window, channel.url)
            }
            composable<AboutUs>{ backStackEntry ->
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                AboutUs(navController, window)
            }
        }
    }

    private fun checkForAppUpdate(activity: Activity) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    REQUEST_CODE_UPDATE
                )
            }
        }
    }

    private fun isPhone(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val screenLayout = context.resources.configuration.screenLayout
        val screenSize = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val isWatch = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_WATCH

        return !isWatch && (screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL || screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL)
    }

}

