package com.krish.videoplayer

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyHorizontalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import com.krish.videoplayer.ui.theme.BlackBackground
import com.krish.videoplayer.ui.theme.BlackHeader

fun getStreamUrl(channelId: Int): String {
    val host = BuildConfig.RTMP_HOST
    return when (channelId) {
        0 -> "rtmp://$host/${BuildConfig.RTMP_PATH_ARRAM}"
        1 -> "rtmp://$host/${BuildConfig.RTMP_PATH_RR}"
        2 -> "rtmp://$host/${BuildConfig.RTMP_PATH_LIVEUPDATES}"
        else -> "rtmp://$host/${BuildConfig.RTMP_PATH_ARRAM}"
    }
}

data class ChannelInfo(
    @DrawableRes val imageUrl: Int,
    val streamUrl: String,
)

val channels = listOf(
    ChannelInfo(R.drawable.arram_bg,getStreamUrl(0)),
    ChannelInfo(R.drawable.rr_channel, getStreamUrl(1)),
    ChannelInfo(R.drawable.live_updates, getStreamUrl(2)),
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ChannelsPreviewPortrait(navController: NavController, focusRequester: FocusRequester){
    Scaffold(
        topBar = { TopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = BlackHeader,
                titleContentColor = Color.White,
            ),
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = dimensionResource(R.dimen.text_size).value.sp
                )
                    },
            navigationIcon = {
                IconButton(onClick = {}) {
                    Image(painter = painterResource(id = R.drawable.arramicon), contentDescription = "App Icon")
                }
            },
        )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(route = AboutUs) },
                modifier = Modifier.size(dimensionResource(R.dimen.fab_button_size)),
                containerColor = BlackHeader,
            ) {
                Icon(
                    modifier = Modifier.size(dimensionResource(R.dimen.fab_icon_size)),
                    painter = painterResource(id = R.drawable.headset_mic_24dp),
                    contentDescription = stringResource(R.string.about_us),
                    tint = Color.White,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,

        content = { innerPadding ->
            Box(modifier = Modifier
                .background(color = BlackBackground)
                .padding(innerPadding)
                .fillMaxWidth()
                .fillMaxHeight()
                ) {
                TvLazyHorizontalGrid(
                    rows = TvGridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(channels) { index, channelInfo ->
                        TvCard(
                            index = index,
                            tvImage = channelInfo.imageUrl,
                            width = dimensionResource(R.dimen.tv_width),
                            height = dimensionResource(R.dimen.tv_height),
                            focusRequester = focusRequester){
                            navController.navigate(route = Channel(channelInfo.streamUrl))
                        }
                    }
                }

//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .verticalScroll(rememberScrollState()),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Top
//                ) {
//                    TvCard(
//                        tvImage = R.drawable.arram_bg, dimensionResource(R.dimen.tv_width), dimensionResource(R.dimen.tv_height)){
//                        navController.navigate(route = Channel(getStreamUrl(0)))
//                    }
//
//                    TvCard(
//                        tvImage = R.drawable.rr_channel, dimensionResource(R.dimen.tv_width), dimensionResource(R.dimen.tv_height)){
//                        navController.navigate(route = Channel(getStreamUrl(1)))
//                    }
//
//                    TvCard(
//                        tvImage = R.drawable.live_updates, dimensionResource(R.dimen.tv_width), dimensionResource(R.dimen.tv_height)){
//                        navController.navigate(route = Channel(getStreamUrl(2)))
//                    }
//                }
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsPreviewLandscape(navController: NavController, focusRequester: FocusRequester){
    Scaffold(
        topBar = { TopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = BlackHeader,
                titleContentColor = Color.White,
            ),
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = dimensionResource(R.dimen.text_size).value.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = {}) {
                    Image(painter = painterResource(id = R.drawable.arramicon), contentDescription = "App Icon")
                }
            },
        )

        },

        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(end = 32.dp).size(dimensionResource(R.dimen.fab_button_size)),
                onClick = { navController.navigate(route = AboutUs) },
                containerColor = BlackHeader,
            ) {
                Icon(
                    modifier = Modifier.size(dimensionResource(R.dimen.fab_icon_size)),
                    painter = painterResource(id = R.drawable.headset_mic_24dp),
                    contentDescription = stringResource(R.string.about_us),
                    tint = Color.White,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,

        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .background(color = Color.Black)
                    .padding(innerPadding)
                    .fillMaxSize()
                    // 5% overscan-safe padding per Android TV guidelines.
                    .padding(horizontal = 48.dp, vertical = 27.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    channels.forEachIndexed { index, channelInfo ->
                        TvCard(
                            index = index,
                            tvImage = channelInfo.imageUrl,
                            width = dimensionResource(R.dimen.tv_width),
                            height = dimensionResource(R.dimen.tv_height),
                            focusRequester = focusRequester
                        ) {
                            navController.navigate(route = Channel(channelInfo.streamUrl))
                        }
                    }
                }
//                Row(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .horizontalScroll(rememberScrollState()),
//                    horizontalArrangement = Arrangement.SpaceEvenly ,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//
//                    TvCard(
//                        index = 0,
//                        tvImage = R.drawable.arram_bg,
//                        width = dimensionResource(R.dimen.tv_width),
//                        height = dimensionResource(R.dimen.tv_height),
//                        focusRequester = focusRequester){
//                        navController.navigate(route = Channel(getStreamUrl(0)))
//                    }
//
//                    TvCard(
//                        index = 1,
//                        tvImage = R.drawable.rr_channel,
//                        width = dimensionResource(R.dimen.tv_width),
//                        height = dimensionResource(R.dimen.tv_height),
//                        focusRequester = focusRequester){
//                        navController.navigate(route = Channel(getStreamUrl(1)))
//                    }
//
//                    TvCard(
//                        index = 2,
//                        tvImage = R.drawable.live_updates,
//                        width = dimensionResource(R.dimen.tv_width),
//                        height = dimensionResource(R.dimen.tv_height),
//                        focusRequester = focusRequester
//                    ){
//                        navController.navigate(route = Channel(getStreamUrl(2)))
//                    }
//                }
            }
        }
    )
}

@Composable
fun TvCard(@DrawableRes tvImage: Int, width: Dp, height: Dp, index: Int, focusRequester: FocusRequester,  onClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "tvCardScale"
    )
    val focusColor = Color(0xFF82B1FF)
    val shape = RoundedCornerShape(16.dp)

    val baseModifier = Modifier
        .padding(dimensionResource(R.dimen.tv_padding))
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = if (isFocused) 24.dp else 6.dp,
            shape = shape,
            ambientColor = focusColor,
            spotColor = focusColor
        )
        .size(width = width, height = height)
        .then(
            if (isFocused) Modifier.border(width = 4.dp, color = focusColor, shape = shape)
            else Modifier
        )
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onClick() }

    val modifier = if (index == 0) baseModifier.focusRequester(focusRequester) else baseModifier

    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 16.dp else 6.dp
        ),
    ) {
        Image(
            painter = painterResource(id = tvImage),
            contentDescription = stringResource(R.string.tv_image),
            contentScale = ContentScale.Crop
        )
    }
}
