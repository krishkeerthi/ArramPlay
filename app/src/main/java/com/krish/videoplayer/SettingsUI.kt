package com.krish.videoplayer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.krish.videoplayer.settings.ThemeMode
import com.krish.videoplayer.settings.VideoScale
import com.krish.videoplayer.ui.DeviceType
import com.krish.videoplayer.ui.LocalAppSettings
import com.krish.videoplayer.ui.LocalDeviceType
import com.krish.videoplayer.ui.LocalSettingsRepository
import com.krish.videoplayer.ui.components.focusHighlight
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val repository = LocalSettingsRepository.current
    val deviceType = LocalDeviceType.current
    val isTv = deviceType == DeviceType.Tv
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val firstFocus = remember { FocusRequester() }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isTv
    val supportsPip = !isTv && (context as? MainActivity)?.supportsPip() == true

    // D-pad users need a starting point; touch users don't want a focus ring.
    if (isTv) LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_arrow))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                ),
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = if (isTv) 48.dp else 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item { SectionTitle(stringResource(R.string.appearance)) }
                item {
                    SettingsCard {
                        ChoiceSetting(
                            icon = Icons.Rounded.Palette,
                            title = stringResource(R.string.theme),
                            options = ThemeMode.entries,
                            selected = settings.themeMode,
                            optionLabel = { stringResource(it.labelRes()) },
                            optionIcon = { it.icon() },
                            onSelect = { scope.launch { repository.setThemeMode(it) } },
                            firstOptionModifier = Modifier.focusRequester(firstFocus),
                        )
                        if (supportsDynamicColor) {
                            SwitchSetting(
                                icon = Icons.Rounded.Palette,
                                title = stringResource(R.string.dynamic_color),
                                subtitle = stringResource(R.string.dynamic_color_summary),
                                checked = settings.dynamicColor,
                                onCheckedChange = { scope.launch { repository.setDynamicColor(it) } },
                            )
                        }
                    }
                }

                item { SectionTitle(stringResource(R.string.playback)) }
                item {
                    SettingsCard {
                        ChoiceSetting(
                            icon = Icons.Rounded.AspectRatio,
                            title = stringResource(R.string.video_scale),
                            options = VideoScale.entries,
                            selected = settings.videoScale,
                            optionLabel = { stringResource(it.labelRes()) },
                            onSelect = { scope.launch { repository.setVideoScale(it) } },
                        )
                        SwitchSetting(
                            icon = Icons.Rounded.History,
                            title = stringResource(R.string.resume_last_channel),
                            subtitle = stringResource(R.string.resume_last_channel_summary),
                            checked = settings.resumeLastChannel,
                            onCheckedChange = { scope.launch { repository.setResumeLastChannel(it) } },
                        )
                        if (supportsPip) {
                            SwitchSetting(
                                icon = Icons.Rounded.PictureInPictureAlt,
                                title = stringResource(R.string.auto_pip),
                                subtitle = stringResource(R.string.auto_pip_summary),
                                checked = settings.autoPip,
                                onCheckedChange = { scope.launch { repository.setAutoPip(it) } },
                            )
                        }
                    }
                }

                item { SectionTitle(stringResource(R.string.about)) }
                item {
                    SettingsCard {
                        LinkSetting(
                            icon = Icons.Outlined.SupportAgent,
                            title = stringResource(R.string.contact_support),
                            onClick = { navController.navigate(AboutUs) },
                        )
                        if (!isTv) {
                            LinkSetting(
                                icon = Icons.Rounded.StarRate,
                                title = stringResource(R.string.rate_app),
                                onClick = {
                                    val market = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
                                    try {
                                        context.startActivity(market)
                                    } catch (_: ActivityNotFoundException) {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
                                        )
                                    }
                                },
                            )
                            val shareText = stringResource(R.string.share_text, "https://play.google.com/store/apps/details?id=${context.packageName}")
                            val shareTitle = stringResource(R.string.share_app)
                            LinkSetting(
                                icon = Icons.Rounded.Share,
                                title = shareTitle,
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, shareText)
                                    try {
                                        context.startActivity(Intent.createChooser(send, shareTitle))
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                        InfoSetting(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.version),
                            value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.rights_reserved, Calendar.getInstance().get(Calendar.YEAR)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) { content() }
    }
}

@Composable
private fun SettingRowContent(icon: ImageVector, title: String, subtitle: String?, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun SwitchSetting(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .focusHighlight(interaction)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
    ) {
        SettingRowContent(icon, title, subtitle) {
            // The whole row toggles; the switch itself is display-only so D-pad focus lands once per row.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun LinkSetting(icon: ImageVector, title: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .focusHighlight(interaction)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            )
    ) {
        SettingRowContent(icon, title, null) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoSetting(icon: ImageVector, title: String, value: String) {
    Box(Modifier.padding(horizontal = 6.dp)) {
        SettingRowContent(icon, title, value)
    }
}

/** Title row followed by a segmented group of pill options — works with touch and D-pad alike. */
@Composable
private fun <T> ChoiceSetting(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    optionIcon: ((T) -> ImageVector)? = null,
    firstOptionModifier: Modifier = Modifier,
) {
    Column(Modifier.padding(horizontal = 6.dp)) {
        SettingRowContent(icon, title, null)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                val interaction = remember { MutableInteractionSource() }
                val container by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    label = "choiceBg",
                )
                val content by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "choiceFg",
                )
                Surface(
                    color = container,
                    contentColor = content,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (index == 0) firstOptionModifier else Modifier)
                        .focusHighlight(interaction, MaterialTheme.shapes.extraLarge, focusedScale = 1.04f)
                        .selectable(
                            selected = isSelected,
                            interactionSource = interaction,
                            indication = ripple(),
                            role = Role.RadioButton,
                            onClick = { onSelect(option) },
                        ),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        optionIcon?.let {
                            Icon(it(option), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(optionLabel(option), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.System -> Icons.Rounded.BrightnessAuto
    ThemeMode.Light -> Icons.Rounded.LightMode
    ThemeMode.Dark -> Icons.Rounded.DarkMode
}
