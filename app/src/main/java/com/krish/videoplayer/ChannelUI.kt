package com.krish.videoplayer

import android.content.Context
import android.net.Uri
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.krish.videoplayer.settings.VideoScale
import com.krish.videoplayer.ui.DeviceType
import com.krish.videoplayer.ui.LocalAppSettings
import com.krish.videoplayer.ui.LocalDeviceType
import com.krish.videoplayer.ui.LocalSettingsRepository
import com.krish.videoplayer.ui.components.ChannelCard
import com.krish.videoplayer.ui.components.LiveBadge
import com.krish.videoplayer.ui.components.focusHighlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val CONTROLS_TIMEOUT_MS = 4_000L
private const val ZAP_BANNER_MS = 2_500L
private const val MAX_AUTO_RETRIES = 5

/** Snapshot of what the overlay needs to know about the player. */
private data class PlaybackUi(
    val isPlaying: Boolean = false,
    /** False only when the viewer paused; buffering/reconnecting keep it true. */
    val playWhenReady: Boolean = true,
    val isBuffering: Boolean = true,
    val firstFrameRendered: Boolean = false,
    val error: PlaybackException? = null,
)

@OptIn(UnstableApi::class)
@Composable
fun ChannelStream(navController: NavController, window: Window, initialChannelId: Int) {
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings = LocalAppSettings.current
    val settingsRepository = LocalSettingsRepository.current
    val deviceType = LocalDeviceType.current
    val scope = rememberCoroutineScope()
    val isOnline by rememberIsOnline()

    var channelId by rememberSaveable { mutableIntStateOf(initialChannelId) }
    val channel = channelById(channelId)
    val channelName = stringResource(channel.name)
    val appName = stringResource(R.string.app_name)

    var ui by remember { mutableStateOf(PlaybackUi()) }
    var controlsVisible by remember { mutableStateOf(false) }
    var controlsOpenedByKey by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var zapBannerTick by remember { mutableIntStateOf(0) }
    var retryAttempt by remember { mutableIntStateOf(0) }
    val isInPip = mainActivity?.isInPipMode == true

    val exoPlayer = remember { buildExoPlayer(context) }
    val rootFocus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }

    fun showControls(byKey: Boolean) {
        controlsOpenedByKey = byKey
        controlsVisible = true
        interactionTick++
    }

    fun zapTo(id: Int) {
        if (id == channelId) return
        channelId = id
        zapBannerTick++
    }

    fun zap(step: Int) {
        val index = channels.indexOfFirst { it.id == channelId }
        zapTo(channels[(index + step).mod(channels.size)].id)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun retry() {
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Load whichever channel is current; persist it so "resume last channel" and the
    // home screen's "Continue watching" pick it up.
    LaunchedEffect(channelId) {
        retryAttempt = 0
        ui = ui.copy(firstFrameRendered = false, error = null, isBuffering = true)
        exoPlayer.setMediaSource(buildMediaSource(context, channel, channelName, appName))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        settingsRepository.setLastChannelId(channelId)
    }

    // Live streams drop: retry automatically with backoff, and immediately once the
    // network comes back. After MAX_AUTO_RETRIES the error panel offers a manual retry.
    LaunchedEffect(ui.error, isOnline) {
        if (ui.error == null || retryAttempt >= MAX_AUTO_RETRIES) return@LaunchedEffect
        if (!isOnline) {
            snapshotFlow { isOnline }.first { it }
        } else {
            delay(2_000L shl retryAttempt.coerceAtMost(3))
        }
        retryAttempt++
        retry()
    }

    val showErrorPanel = ui.error != null && retryAttempt >= MAX_AUTO_RETRIES

    // Auto-hide controls unless the viewer paused or the final error panel is up.
    LaunchedEffect(controlsVisible, interactionTick, ui.playWhenReady, showErrorPanel) {
        if (controlsVisible && ui.playWhenReady && !showErrorPanel) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    // D-pad focus: buttons only grab focus when the overlay was opened from a remote/keyboard,
    // so touch users never see a stray focus ring. Otherwise the root keeps focus for key events.
    LaunchedEffect(controlsVisible, showErrorPanel) {
        when {
            showErrorPanel -> runCatching { retryFocus.requestFocus() }
            controlsVisible && controlsOpenedByKey -> {
                delay(50) // let AnimatedVisibility compose the buttons
                runCatching { playPauseFocus.requestFocus() }
            }
            !controlsVisible -> runCatching { rootFocus.requestFocus() }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                ui = ui.copy(isPlaying = isPlaying)
                mainActivity?.refreshPipParams()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                ui = ui.copy(playWhenReady = playWhenReady)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                ui = ui.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    retryAttempt = 0
                    ui = ui.copy(error = null)
                }
            }

            override fun onRenderedFirstFrame() {
                ui = ui.copy(firstFrameRendered = true)
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                    return
                }
                ui = ui.copy(error = error, isBuffering = false)
            }
        }
        exoPlayer.addListener(listener)

        // MediaSession exposes the player to remotes, Bluetooth headsets, Google Assistant
        // and the system "now playing" surfaces.
        val mediaSession = MediaSession.Builder(context, exoPlayer).setId("live").build()

        val observer = LifecycleEventObserver { _, event ->
            // isInPictureInPictureMode is set before ON_PAUSE is dispatched on PiP entry,
            // so playback continues in the floating window. ON_STOP means PiP was closed
            // or the app went to the background — always pause then.
            val inPip = mainActivity?.isInPictureInPictureMode == true
            when (event) {
                Lifecycle.Event.ON_PAUSE -> if (!inPip) exoPlayer.pause()
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_START -> {
                    // A live stream that sat paused in the background is stale; jump back to the live edge.
                    if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        mainActivity?.registerPipPlayer(
            toggle = { togglePlayPause() },
            isPlaying = { exoPlayer.isPlaying },
        )

        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            lifecycleOwner.lifecycle.removeObserver(observer)
            mainActivity?.unregisterPipPlayer()
            exoPlayer.removeListener(listener)
            mediaSession.release()
            exoPlayer.release()
        }
    }

    BackHandler(enabled = controlsVisible && !isInPip) { controlsVisible = false }

    val swipeThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                interactionTick++
                when (event.key) {
                    Key.MediaPlayPause, Key.Spacebar -> { togglePlayPause(); showControls(byKey = true); true }
                    Key.MediaPlay -> { exoPlayer.play(); true }
                    Key.MediaPause -> { exoPlayer.pause(); showControls(byKey = true); true }
                    Key.ChannelUp, Key.MediaNext -> { zap(+1); true }
                    Key.ChannelDown, Key.MediaPrevious -> { zap(-1); true }
                    else -> digitOf(event.key)?.let { n ->
                        channels.firstOrNull { it.number == n }?.let { zapTo(it.id) }
                        true
                    } ?: false
                }
            }
            // Reached only when no focused child consumed the key, i.e. the overlay is hidden
            // or focus hit an edge. Up/Down zap like a TV remote; anything else opens the overlay.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || (controlsVisible && !showErrorPanel)) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { zap(+1); true }
                    Key.DirectionDown -> { zap(-1); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.DirectionLeft,
                    Key.DirectionRight, Key.Menu -> { showControls(byKey = true); true }
                    else -> false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (controlsVisible) controlsVisible = false else showControls(byKey = false)
                })
            }
            .pointerInput(Unit) {
                var dragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onDragEnd = { if (abs(dragX) > swipeThresholdPx) zap(if (dragX < 0) +1 else -1) },
                    onHorizontalDrag = { _, delta -> dragX += delta },
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    isFocusable = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { it.resizeMode = settings.videoScale.toResizeMode() },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { mainActivity?.updatePipSourceRect(it.boundsInWindow()) },
        )

        // Channel artwork stands in for the video until the first frame arrives, so zapping
        // never flashes a black screen.
        AnimatedVisibility(
            visible = !ui.firstFrameRendered,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(500)),
        ) {
            AnimatedContent(
                targetState = channel.artwork,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "poster",
            ) { artwork ->
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(artwork),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                }
            }
        }

        val showSpinner = ui.error == null && (ui.isBuffering || !ui.firstFrameRendered)
        AnimatedVisibility(
            // While the overlay is open its subtitle carries this status instead.
            visible = (showSpinner || (ui.error != null && !showErrorPanel)) && !(controlsVisible && !isInPip),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                if (ui.error != null && !isInPip) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(if (isOnline) R.string.reconnecting else R.string.waiting_for_network),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        if (!isInPip) {
            AnimatedVisibility(
                visible = controlsVisible && !showErrorPanel,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300)),
            ) {
                ControlsOverlay(
                    channel = channel,
                    status = when {
                        ui.error != null -> stringResource(if (isOnline) R.string.reconnecting else R.string.waiting_for_network)
                        showSpinner -> stringResource(R.string.loading)
                        else -> null
                    },
                    isPlaying = ui.isPlaying,
                    videoScale = settings.videoScale,
                    showPip = deviceType != DeviceType.Tv && mainActivity?.supportsPip() == true,
                    playPauseFocus = playPauseFocus,
                    onBack = { navController.popBackStack() },
                    onPlayPause = { togglePlayPause(); interactionTick++ },
                    onPrevious = { zap(-1); interactionTick++ },
                    onNext = { zap(+1); interactionTick++ },
                    onSelectChannel = { zapTo(it); interactionTick++ },
                    onCycleScale = {
                        val next = VideoScale.entries[(settings.videoScale.ordinal + 1) % VideoScale.entries.size]
                        scope.launch { settingsRepository.setVideoScale(next) }
                        interactionTick++
                    },
                    onPip = { controlsVisible = false; mainActivity?.enterPip() },
                )
            }

            ZapBanner(
                channel = channel,
                trigger = zapBannerTick,
                suppressed = controlsVisible,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
            )

            AnimatedVisibility(
                visible = showErrorPanel,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                ErrorPanel(
                    isOnline = isOnline,
                    retryFocus = retryFocus,
                    onRetry = { retryAttempt = 0; retry() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }
}

@Composable
private fun ControlsOverlay(
    channel: ChannelInfo,
    status: String?,
    isPlaying: Boolean,
    videoScale: VideoScale,
    showPip: Boolean,
    playPauseFocus: FocusRequester,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectChannel: (Int) -> Unit,
    onCycleScale: () -> Unit,
    onPip: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.75f),
                    0.3f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.85f),
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top: back, channel name, live badge
        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverlayIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back_arrow), onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(channel.name), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    listOfNotNull(stringResource(R.string.channel_number, channel.number), status).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            LiveBadge()
        }

        // Center: previous / play-pause / next channel
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            OverlayIconButton(Icons.Rounded.SkipPrevious, stringResource(R.string.previous_channel), onPrevious)
            OverlayIconButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                description = stringResource(if (isPlaying) R.string.pause else R.string.play),
                onClick = onPlayPause,
                size = 72.dp,
                modifier = Modifier.focusRequester(playPauseFocus),
            )
            OverlayIconButton(Icons.Rounded.SkipNext, stringResource(R.string.next_channel), onNext)
        }

        // Bottom: channel strip + scale / PiP
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.all_channels),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                OverlayIconButton(
                    Icons.Rounded.AspectRatio,
                    stringResource(R.string.video_scale_cycle, stringResource(videoScale.labelRes())),
                    onCycleScale,
                    size = 44.dp,
                )
                if (showPip) {
                    Spacer(Modifier.width(8.dp))
                    OverlayIconButton(Icons.Rounded.PictureInPictureAlt, stringResource(R.string.picture_in_picture), onPip, size = 44.dp)
                }
            }
            LazyRow(
                contentPadding = PaddingValues(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(channels, key = { it.id }) { item ->
                    val current = item.id == channel.id
                    ChannelCard(
                        channel = item,
                        onClick = { onSelectChannel(item.id) },
                        showTitle = false,
                        focusedScale = 1.06f,
                        modifier = Modifier
                            .width(150.dp)
                            .graphicsLayer { alpha = if (current) 1f else 0.8f }
                            .then(
                                if (current) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                                else Modifier
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.15f else 1f, label = "overlayBtn")
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(if (focused) Color.White else Color.Black.copy(alpha = 0.35f), CircleShape)
            .focusHighlight(interaction, CircleShape, focusedScale = 1f)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** "3  Live Updates" banner shown briefly after zapping — the classic TV channel-change OSD. */
@Composable
private fun ZapBanner(channel: ChannelInfo, trigger: Int, suppressed: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        delay(ZAP_BANNER_MS)
        visible = false
    }
    AnimatedVisibility(
        visible = visible && !suppressed,
        enter = slideInHorizontally { -it / 3 } + fadeIn(),
        exit = slideOutHorizontally { -it / 3 } + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                channel.number.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(channel.name), style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(4.dp))
                LiveBadge(pulsing = false)
            }
        }
    }
}

@Composable
private fun ErrorPanel(isOnline: Boolean, retryFocus: FocusRequester, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .padding(24.dp)
            .background(Color.Black.copy(alpha = 0.8f), MaterialTheme.shapes.large)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.playback_error_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (isOnline) R.string.playback_error_message else R.string.offline_message),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus)) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.retry))
            }
        }
    }
}

fun VideoScale.labelRes(): Int = when (this) {
    VideoScale.Fit -> R.string.video_scale_fit
    VideoScale.Fill -> R.string.video_scale_fill
    VideoScale.Zoom -> R.string.video_scale_zoom
}

@OptIn(UnstableApi::class)
private fun VideoScale.toResizeMode(): Int = when (this) {
    VideoScale.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoScale.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    VideoScale.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}

private fun digitOf(key: Key): Int? = when (key) {
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

@OptIn(UnstableApi::class)
private fun buildExoPlayer(context: Context): ExoPlayer =
    ExoPlayer.Builder(context)
        // Small start buffer = fast channel changes; generous max smooths over network hiccups.
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 2_500,
                    /* maxBufferMs = */ 30_000,
                    /* bufferForPlaybackMs = */ 1_000,
                    /* bufferForPlaybackAfterRebufferMs = */ 2_500,
                )
                .build()
        )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

/** The URL scheme picks the source: RTMP → progressive FLV over librtmp, RTSP, else HLS/DASH/progressive. */
@OptIn(UnstableApi::class)
private fun buildMediaSource(context: Context, channel: ChannelInfo, title: String, artist: String): MediaSource {
    val item = MediaItem.Builder()
        .setUri(channel.streamUrl)
        .setMediaId(channel.id.toString())
        .setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setIsPlayable(true)
                .build()
        )
        .build()
    return when (Uri.parse(channel.streamUrl).scheme?.lowercase()) {
        "rtmp" -> ProgressiveMediaSource.Factory(RtmpDataSource.Factory()).createMediaSource(item)
        "rtsp" -> RtspMediaSource.Factory().createMediaSource(item)
        else -> DefaultMediaSourceFactory(context).createMediaSource(item)
    }
}
