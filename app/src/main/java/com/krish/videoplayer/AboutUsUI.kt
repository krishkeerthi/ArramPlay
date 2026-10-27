package com.krish.videoplayer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.krish.videoplayer.ui.DeviceType
import com.krish.videoplayer.ui.LocalDeviceType
import com.krish.videoplayer.ui.components.focusHighlight
import java.util.Calendar

private data class ContactEntry(
    @DrawableRes val icon: Int,
    val label: Int,
    val value: String,
    val contact: Contact,
)

private val contacts = listOf(
    ContactEntry(R.drawable.telephone, R.string.contact_us, "+91 9677033113", Contact.Phone),
    ContactEntry(R.drawable.communication, R.string.email_at, "arramtv@gmail.com", Contact.Email),
    ContactEntry(R.drawable.instagram, R.string.instagram, "https://www.instagram.com/arramtv", Contact.Social),
    ContactEntry(R.drawable.youtube, R.string.youtube, "https://www.youtube.com/@arramtv", Contact.Social),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUs(navController: NavController) {
    val isTv = LocalDeviceType.current == DeviceType.Tv

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_us)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_arrow))
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val wide = maxWidth >= 720.dp
            val horizontalPadding = if (isTv) 48.dp else 16.dp
            if (wide) {
                // Tablet / TV / landscape: brand on the left, contact options on the right.
                Row(
                    Modifier
                        .fillMaxSize()
                        .widthIn(max = 1100.dp)
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandColumn(Modifier.weight(1f))
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ContactList(interactive = !isTv)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BrandColumn(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    ContactList(interactive = !isTv)
                }
            }
        }
    }
}

@Composable
private fun BrandColumn(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.arramicon),
            contentDescription = stringResource(R.string.app_icon),
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.extraLarge),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.rights_reserved, Calendar.getInstance().get(Calendar.YEAR)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContactList(interactive: Boolean) {
    contacts.forEach { entry ->
        ContactItem(
            drawable = entry.icon,
            label = stringResource(entry.label),
            value = entry.value,
            contact = entry.contact,
            interactive = interactive,
        )
    }
}

@Composable
fun ContactItem(
    @DrawableRes drawable: Int,
    label: String,
    value: String,
    contact: Contact,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large

    // TVs have no dialer/mail app and Play TV review rejects dead-end actions, so on TV
    // the rows are read-only info. Elsewhere the whole row is one click target.
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .then(
                if (interactive) {
                    Modifier
                        .focusHighlight(interactionSource, shape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            role = Role.Button,
                            onClick = { context.openContact(contact, value) },
                        )
                } else Modifier
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = drawable),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    value.removePrefix("https://www."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (interactive) {
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** TVs and some tablets have no dialer/mail/browser — fail gracefully instead of crashing. */
private fun Context.openContact(contact: Contact, value: String) {
    val intent = when (contact) {
        is Contact.Phone -> Intent(Intent.ACTION_DIAL, "tel:$value".toUri())
        is Contact.Email -> Intent(Intent.ACTION_SENDTO, "mailto:$value".toUri())
        is Contact.Social -> Intent(Intent.ACTION_VIEW, value.toUri())
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.no_app_found_for, value), Toast.LENGTH_LONG).show()
    }
}

sealed class Contact {
    data object Phone : Contact()
    data object Email : Contact()
    data object Social : Contact()
}
