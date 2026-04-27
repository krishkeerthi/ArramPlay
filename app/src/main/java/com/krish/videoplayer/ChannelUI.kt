package com.krish.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException


@SuppressLint("UnrememberedMutableInteractionSource")
@OptIn(UnstableApi::class)
@Composable
fun ChannelStream(navController: NavController, window: Window, streamUrl: String) {
    Log.d("logurl", "ChannelStream: url $streamUrl")

    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var controllerVisibility by remember { mutableStateOf(false) }
    var isScreenTapped by remember { mutableStateOf(false) }

    val exoPlayer = createExoPlayer(context)

    LaunchedEffect(Unit) {
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController.hide(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        initializeExoPlayer(exoPlayer, streamUrl) { isLoading ->
            loading = isLoading
        }
        playRtmpFromDomain(BuildConfig.RTMP_DOMAIN)
    }

    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> exoPlayer.play()
            }
        }
        activity?.lifecycle?.addObserver(observer)

        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.lifecycle?.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
            ) {
                isScreenTapped = !isScreenTapped
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        controllerVisibility = when(visibility){
                            View.VISIBLE -> {
                                true
                            }
                            else -> {
                                false
                            }
                        }
                    })
                    setShowNextButton(true)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                }
            }
        )

        if(isScreenTapped){
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        dimensionResource(R.dimen.back_icon_padding_horizontal),
                        dimensionResource(R.dimen.back_icon_padding_vertical)
                    )
                    .background(
                        color = Color.Black.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back_arrow),
                    tint = Color.White
                )
            }
            LaunchedEffect(1){
                delay(3000)
                isScreenTapped = false
            }
        }

    }

}

@Composable
fun createExoPlayer(context: Context): ExoPlayer {
    return remember {
        ExoPlayer.Builder(context).build()
    }
}

private fun playRtmpFromDomain(domainUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Resolve domain to IP
            val addresses = InetAddress.getByName(domainUrl)
//            if (addresses.isEmpty()) throw UnknownHostException("No IPs found") as Throwable
            val ip = addresses.hostAddress
            Log.d("dns", "DNS DNS Resolved $domainUrl → $ip")

            val originalUrl = "rtmp://$domainUrl:1935/${BuildConfig.RTMP_PATH_ARRAM}"
            val ipUrl = originalUrl.replace(domainUrl, ip)

            withContext(Dispatchers.Main) {
//                initializeExoPlayer(createExoPlayer(), streamUrl) { isLoading ->
//                    loading = isLoading
//                }
                Log.d("DNS", "DNS Resolved $originalUrl → $ipUrl")
            }
        } catch (e: Exception) {
            Log.d("DNS", "DNS resolution failed for $domainUrl")
            withContext(Dispatchers.Main) {
//                showMessage("Could not resolve $domainUrl")
            }
        }
    }
}

fun initializeExoPlayer(
    exoPlayer: ExoPlayer,
    streamUrl: String,
    onLoadingChanged: (Boolean) -> Unit
) {
    exoPlayer.apply {
        playWhenReady = true
//        repeatMode = Player.REPEAT_MODE_ALL
        setMediaItem(
            MediaItem.Builder()
                .setUri(streamUrl)
//                .setLiveConfiguration(MediaItem.LiveConfiguration.UNSET)//MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(3000).build()
//                .setMimeType("video/x-flv") //MimeTypes.VIDEO_AVI
                .build()
        )
        prepare()
//        addListener(object : Player.Listener {
//            override fun onPlaybackStateChanged(playbackState: Int) {
//                when (playbackState) {
//                    Player.STATE_BUFFERING -> onLoadingChanged(true)
//                    Player.STATE_READY -> onLoadingChanged(false)
//                    Player.STATE_ENDED -> onLoadingChanged(false)
//                    Player.STATE_IDLE -> onLoadingChanged(true)
//                }
//            }
//        })
    }
}


