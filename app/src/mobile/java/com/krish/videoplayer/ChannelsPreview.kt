package com.krish.videoplayer

import android.content.res.Configuration
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController

@Composable
fun ChannelsPreview(navController: NavController, window: Window) {
    val configuration = LocalConfiguration.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> ChannelsPreviewLandscape(navController, focusRequester)
        else -> ChannelsPreviewPortrait(navController, focusRequester)
    }
}
