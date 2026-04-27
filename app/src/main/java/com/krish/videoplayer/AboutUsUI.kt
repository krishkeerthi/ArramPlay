package com.krish.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.krish.videoplayer.ui.theme.BlackBackground
import com.krish.videoplayer.ui.theme.BlackHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUs(navController: NavController, window: Window) {
    LaunchedEffect(Unit) {
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    Scaffold(
        topBar = { TopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = BlackHeader,
                titleContentColor = Color.White,
            ),
            title = {
                Text(
                    text = stringResource(R.string.about_us),
                    color = Color.White,
                    fontSize = dimensionResource(R.dimen.text_size).value.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Image(painter = painterResource(id = R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.back_arrow))
                }
            },

        )

        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BlackBackground)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer)))

                Image(
                    painter = painterResource(id = R.drawable.arramicon),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.about_us_icon_size))
                        .background(Color.White, shape = CircleShape)
                        .padding(dimensionResource(R.dimen.about_us_icon_padding))
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer)))

                Text(
                    text = stringResource(R.string.about_us),
                    fontWeight = FontWeight.Bold,
                    fontSize = dimensionResource(R.dimen.about_us_text_size).value.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.app_description),
                    fontSize = dimensionResource(R.dimen.app_description_text_size).value.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                ContactItem(
                    drawable = R.drawable.telephone,
                    label = stringResource(R.string.contact_us),
                    value = "+91 9677033113",
                    Contact.Phone
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer)))

                ContactItem(
                    drawable = R.drawable.communication,
                    label = stringResource(R.string.email_at),
                    value = "arramtv@gmail.com",
                    Contact.Email
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer)))

                ContactItem(
                    drawable = R.drawable.instagram,
                    label = stringResource(R.string.instagram),
                    value = "https://www.instagram.com/arramtv",
                    Contact.Social
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer)))

                ContactItem(
                    drawable = R.drawable.youtube,
                    label = stringResource(R.string.youtube),
                    value = "https://www.youtube.com/@arramtv",
                    Contact.Social
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.rights_reserved),
                    fontSize = dimensionResource(R.dimen.rights_reserved_text_size).value.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer)))
            }
        }
    )
}

@Composable
fun ContactItem(@DrawableRes drawable: Int, label: String, value: String, contact: Contact) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.tv_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = drawable),
            contentDescription = label,
            modifier = Modifier.size(dimensionResource(R.dimen.social_icon_size)),
        )

        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer)))

        Column {
            when(contact){
                is Contact.Phone -> {
                    val annotatedString = AnnotatedString.Builder().apply {
                        pushStringAnnotation(tag = "PHONE", annotation = value)
                        withStyle(style = SpanStyle(color = Color.White, textDecoration = TextDecoration.Underline)) {
                            append(value)
                        }
                        pop()
                    }.toAnnotatedString()

                    Text(text = annotatedString, fontSize = dimensionResource(R.dimen.about_us_social_text_size).value.sp, modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$value"))
                        context.startActivity(intent)
                    })
                }
                is Contact.Email -> {
                    val annotatedString = AnnotatedString.Builder().apply {
                        pushStringAnnotation(tag = "EMAIL", annotation = value)
                        withStyle(style = SpanStyle(color = Color.White, textDecoration = TextDecoration.Underline)) {
                            append(value)
                        }
                        pop()
                    }.toAnnotatedString()

                    Text(text = annotatedString, fontSize = dimensionResource(R.dimen.about_us_social_text_size).value.sp, modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$value"))
                        context.startActivity(intent)
                    })
                }
                is Contact.Social -> {
                    val annotatedString = AnnotatedString.Builder().apply {
                        pushStringAnnotation(tag = "URL", annotation = value)
                        withStyle(style = SpanStyle(color = Color.White, textDecoration = TextDecoration.Underline)) {
                            append(value)
                        }
                        pop()
                    }.toAnnotatedString()

                    Text(text = annotatedString, fontSize = dimensionResource(R.dimen.about_us_social_text_size).value.sp, modifier = Modifier.clickable {
                        uriHandler.openUri(value)
                    })
                }
            }
        }
    }
}

sealed class Contact{
    data object Phone : Contact()
    data object Email : Contact()
    data object Social : Contact()
}