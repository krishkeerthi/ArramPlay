package com.krish.videoplayer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.krish.videoplayer.ui.DeviceType
import com.krish.videoplayer.ui.LocalAppSettings
import com.krish.videoplayer.ui.LocalDeviceType
import com.krish.videoplayer.ui.TvSafePadding
import com.krish.videoplayer.ui.components.AppBrand
import com.krish.videoplayer.ui.components.ChannelCard
import com.krish.videoplayer.ui.components.ClockText
import com.krish.videoplayer.ui.components.LiveBadge
import com.krish.videoplayer.ui.components.OfflineBanner
import com.krish.videoplayer.ui.components.bottomScrim
import com.krish.videoplayer.ui.components.focusHighlight

/** The channel to feature: whatever the viewer watched last, otherwise the first channel. */
@Composable
private fun featuredChannel(): ChannelInfo =
    LocalAppSettings.current.lastChannelId?.let(::channelById) ?: channels.first()

/**
 * Phone portrait: top app bar + adaptive grid. The grid's column count follows the
 * window width, so the same composable also covers foldables and split-screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsPreviewPortrait(navController: NavController, focusRequester: FocusRequester) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isOnline by rememberIsOnline()
    val featured = featuredChannel()
    val hasHistory = LocalAppSettings.current.lastChannelId != null

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { AppBrand() },
                actions = {
                    IconButton(onClick = { navController.navigate(Settings) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = { navController.navigate(AboutUs) }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_us))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                ),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineBanner(visible = !isOnline)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                    HeroCard(
                        channel = featured,
                        label = stringResource(if (hasHistory) R.string.continue_watching else R.string.featured),
                        onPlay = { navController.navigate(Channel(featured.id)) },
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                    SectionHeader(stringResource(R.string.all_channels), Modifier.padding(top = 8.dp))
                }
                items(channels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        focusedScale = 1.04f,
                        onClick = { navController.navigate(Channel(channel.id)) },
                    )
                }
            }
        }
    }
}

/**
 * Landscape phone, tablet and Android TV: an immersive "lean-back" home in the style of
 * Google TV / Pluto TV — the focused channel's artwork fills the screen as a backdrop,
 * with details on the left and a focusable row of channels along the bottom.
 */
@Composable
fun ChannelsPreviewLandscape(navController: NavController, focusRequester: FocusRequester) {
    val isTv = LocalDeviceType.current == DeviceType.Tv
    val isOnline by rememberIsOnline()
    val initial = featuredChannel()
    var selectedId by rememberSaveable { mutableIntStateOf(initial.id) }
    val selected = channelById(selectedId)
    val background = MaterialTheme.colorScheme.background

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(background)
    ) {
        val compact = maxHeight < 480.dp
        // TV: rows bleed to the screen edge and carry the overscan margin as content padding,
        // so focused (scaled-up) cards are never clipped.
        val hPad = if (isTv) TvSafePadding.calculateLeftPadding(LayoutDirection.Ltr) else 24.dp
        val cardWidth = (maxWidth * 0.24f).coerceIn(180.dp, 320.dp)

        // Backdrop: crossfades as focus moves between channels.
        Crossfade(
            targetState = selected.artwork,
            animationSpec = tween(durationMillis = 600),
            label = "backdrop",
            modifier = Modifier.fillMaxSize(),
        ) { artwork ->
            Image(
                painter = painterResource(artwork),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Scrims: left-to-right for the details column, bottom-up for the channel row.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val horizontal = Brush.horizontalGradient(
                        0f to background,
                        0.35f to background.copy(alpha = 0.85f),
                        0.75f to background.copy(alpha = 0.2f),
                        1f to Color.Transparent,
                    )
                    val vertical = Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to background,
                    )
                    onDrawBehind {
                        drawRect(horizontal)
                        drawRect(vertical)
                    }
                }
        )

        Column(
            Modifier
                .fillMaxSize()
                .then(if (isTv) Modifier.padding(vertical = TvSafePadding.calculateTopPadding()) else Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
        ) {
            OfflineBanner(visible = !isOnline, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad, vertical = 8.dp)
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppBrand(iconSize = if (isTv) 40.dp else 32.dp)
                Spacer(Modifier.weight(1f))
                if (isTv) ClockText(Modifier.padding(end = 16.dp))
                TopNavButton(
                    label = stringResource(R.string.settings),
                    onClick = { navController.navigate(Settings) },
                ) { Icon(Icons.Outlined.Settings, contentDescription = null) }
                TopNavButton(
                    label = stringResource(R.string.about_us),
                    onClick = { navController.navigate(AboutUs) },
                ) { Icon(Icons.Outlined.Info, contentDescription = null) }
            }

            Spacer(Modifier.weight(1f))

            Column(
                Modifier
                    .padding(horizontal = hPad)
                    .widthIn(max = 520.dp)
            ) {
                LiveBadge()
                Spacer(Modifier.height(12.dp))
                Crossfade(targetState = selected.name, label = "title") { name ->
                    Text(
                        text = stringResource(name),
                        style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (!compact) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.channel_subtitle, selected.number),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
                WatchNowButton(onClick = { navController.navigate(Channel(selected.id)) })
            }

            Spacer(Modifier.height(if (compact) 12.dp else 28.dp))
            SectionHeader(
                stringResource(R.string.all_channels),
                Modifier.padding(horizontal = hPad),
            )

            LazyRow(
                contentPadding = PaddingValues(
                    horizontal = hPad,
                    vertical = 14.dp, // room for the focus scale-up
                ),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(channels, key = { it.id }) { channel ->
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    LaunchedEffect(focused) { if (focused) selectedId = channel.id }
                    ChannelCard(
                        channel = channel,
                        interactionSource = interaction,
                        onClick = { navController.navigate(Channel(channel.id)) },
                        modifier = Modifier
                            .width(cardWidth)
                            .then(if (channel.id == initial.id) Modifier.focusRequester(focusRequester) else Modifier),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    channel: ChannelInfo,
    label: String,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .focusHighlight(interaction, shape, focusedScale = 1.02f)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onPlay)
    ) {
        Image(
            painter = painterResource(channel.artwork),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier
                .matchParentSize()
                .bottomScrim(Color.Black.copy(alpha = 0.85f))
        )
        LiveBadge(Modifier.align(Alignment.TopStart).padding(14.dp))
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                Text(stringResource(channel.name), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
            WatchNowButton(onClick = onPlay, focusable = false)
        }
    }
}

@Composable
private fun WatchNowButton(onClick: () -> Unit, focusable: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "watchScale")
    Button(
        onClick = onClick,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focusable) Modifier else Modifier.focusPropertiesDisabled()),
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.watch_now))
    }
}

/** Icon + label pill for the lean-back top bar; label always visible so D-pad users know where they are. */
@Composable
private fun TopNavButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.extraLarge
    Row(
        modifier = Modifier
            .focusHighlight(interaction, shape, focusedScale = 1.06f)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.semantics { heading() },
    )
}

private fun Modifier.focusPropertiesDisabled(): Modifier =
    this.then(Modifier.focusProperties { canFocus = false })
