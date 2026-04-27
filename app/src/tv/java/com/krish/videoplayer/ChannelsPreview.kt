package com.krish.videoplayer

import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController

@Composable
fun ChannelsPreview(navController: NavController, window: Window) {
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

    ChannelsPreviewLandscape(navController, focusRequester)
}
