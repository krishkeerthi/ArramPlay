package com.krish.videoplayer

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.navigation.NavController

@Composable
fun ChannelsPreview(navController: NavController) {
    val configuration = LocalConfiguration.current
    val focusRequester = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current

    // Only pre-focus for keyboard / D-pad users (Chromebooks, tablets with keyboards);
    // touch users would otherwise see a focus ring on launch.
    LaunchedEffect(configuration.orientation) {
        if (inputModeManager.inputMode == InputMode.Keyboard) runCatching { focusRequester.requestFocus() }
    }

    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> ChannelsPreviewLandscape(navController, focusRequester)
        else -> ChannelsPreviewPortrait(navController, focusRequester)
    }
}
