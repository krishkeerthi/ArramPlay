package com.krish.videoplayer.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.krish.videoplayer.ChannelInfo
import com.krish.videoplayer.R
import com.krish.videoplayer.ui.theme.LiveRed
import kotlinx.coroutines.delay
import java.util.Date

/** Shared spring for focus/press scaling so every focusable surface moves the same way. */
private val FocusSpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

/**
 * Channel artwork card used on every form factor. Grows and gains a ring when focused
 * (D-pad / keyboard), shrinks slightly when pressed (touch).
 */
@Composable
fun ChannelCard(
    channel: ChannelInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusedScale: Float = 1.08f,
    showTitle: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.97f
            isFocused -> focusedScale
            else -> 1f
        },
        animationSpec = FocusSpring,
        label = "cardScale",
    )
    val elevation by animateDpAsState(if (isFocused) 16.dp else 2.dp, label = "cardElevation")
    val ringColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        label = "cardRing",
    )
    val shape = MaterialTheme.shapes.medium

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = elevation.toPx()
                    this.shape = shape
                    clip = true
                }
                .border(3.dp, ringColor, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
        ) {
            Image(
                painter = painterResource(channel.artwork),
                contentDescription = stringResource(channel.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            LiveBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                pulsing = isFocused,
            )
        }
        if (showTitle) {
            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(channel.name),
                style = MaterialTheme.typography.titleSmall,
                color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** Red "● LIVE" pill. The dot pulses when [pulsing] — used on the focused/playing item. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier, pulsing: Boolean = true) {
    // Only run the infinite animation when visible-and-pulsing; idle badges cost no frames.
    val dotAlpha: () -> Float = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "live")
        val pulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "livePulse",
        )
        val read: () -> Float = { pulse }
        read
    } else {
        { 1f }
    }
    Row(
        modifier = modifier
            .background(LiveRed, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .graphicsLayer { alpha = dotAlpha() }
                .background(Color.White, CircleShape)
        )
        Text(
            text = stringResource(R.string.live),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

/**
 * Animated focus treatment for list rows and buttons that aren't [ChannelCard]:
 * tinted background + ring + slight scale. Pair with the same [interactionSource]
 * passed to `clickable`/`toggleable`.
 */
@Composable
fun Modifier.focusHighlight(
    interactionSource: MutableInteractionSource,
    shape: Shape = MaterialTheme.shapes.medium,
    focusedScale: Float = 1.02f,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) focusedScale else 1f, FocusSpring, label = "rowScale")
    val bg by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f) else Color.Transparent,
        label = "rowBg",
    )
    val ring by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        label = "rowRing",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .background(bg, shape)
        .border(2.dp, ring, shape)
}

/** Wall-clock time, refreshed on the minute. Respects the device 12/24h setting. */
@Composable
fun ClockText(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    Text(
        text = DateFormat.getTimeFormat(context).format(Date(now)),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun OfflineBanner(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.offline_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** App icon + name lockup used in top bars. */
@Composable
fun AppBrand(modifier: Modifier = Modifier, iconSize: Dp = 36.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.arramicon),
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Vertical scrim so text stays legible over channel artwork in both themes. */
fun Modifier.bottomScrim(color: Color, startFraction: Float = 0.35f): Modifier = drawBehind {
    drawRect(
        Brush.verticalGradient(
            0f to Color.Transparent,
            startFraction to color.copy(alpha = 0.55f),
            1f to color,
        )
    )
}
