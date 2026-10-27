package com.krish.videoplayer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Tracks whether the device currently has a network with internet capability. */
@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    val connectivityManager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val online = remember { mutableStateOf(connectivityManager.isCurrentlyOnline()) }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online.value = true
            }

            override fun onLost(network: Network) {
                online.value = connectivityManager.isCurrentlyOnline()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }
    return online
}

private fun ConnectivityManager.isCurrentlyOnline(): Boolean =
    getNetworkCapabilities(activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
