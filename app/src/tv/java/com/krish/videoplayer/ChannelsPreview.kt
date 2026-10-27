package com.krish.videoplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavController

@Composable
fun ChannelsPreview(navController: NavController) {
    val focusRequester = remember { FocusRequester() }

    // TV is always D-pad driven: land focus on the featured channel card.
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    ChannelsPreviewLandscape(navController, focusRequester)
}
