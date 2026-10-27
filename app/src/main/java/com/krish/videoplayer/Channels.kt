package com.krish.videoplayer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

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
    val id: Int,
    @StringRes val name: Int,
    @DrawableRes val artwork: Int,
    val streamUrl: String,
) {
    /** 1-based number shown on the zapping banner, like a TV channel number. */
    val number: Int get() = id + 1
}

val channels = listOf(
    ChannelInfo(0, R.string.channel_arram, R.drawable.arram_bg, getStreamUrl(0)),
    ChannelInfo(1, R.string.channel_rr, R.drawable.rr_channel, getStreamUrl(1)),
    ChannelInfo(2, R.string.channel_live_updates, R.drawable.live_updates, getStreamUrl(2)),
)

fun channelById(id: Int): ChannelInfo = channels.firstOrNull { it.id == id } ?: channels.first()
