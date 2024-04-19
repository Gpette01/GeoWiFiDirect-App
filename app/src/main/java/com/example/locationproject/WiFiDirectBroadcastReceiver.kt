package com.example.locationproject

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.Gson

class WiFiDirectBroadcastReceiver(
        private val manager: WifiP2pManager,
        private val channel: WifiP2pManager.Channel,
        private val activity: MainActivity,
        private val callback: PeerUpdateListener,
        private val databaseHelper: DatabaseHelper,
        private var discoveredPeers: MutableList<WifiP2pDevice> = mutableListOf()
) : BroadcastReceiver() {

    interface PeerUpdateListener {
        fun onPeersUpdated(peers: List<WifiP2pDevice>)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WiFiDirectReceiver", "Received action: ${intent.action}")
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("WiFiDirectReceiver", "Wi-Fi Direct is enabled.")
                } else {
                    Log.d("WiFiDirectReceiver", "Wi-Fi Direct is not enabled.")
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d("WiFiDirectReceiver", "WIFI_P2P_PEERS_CHANGED_ACTION received")
                if (ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    manager.requestPeers(channel) { peerList ->
                        val peers = peerList.deviceList.toList()
                        callback.onPeersUpdated(peers)
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

            }
        }
    }

    fun updateDiscoveredPeers(peers: List<WifiP2pDevice>) {
        discoveredPeers.clear()
        discoveredPeers.addAll(peers)

        // Optionally, update your UI here to reflect the new list of peers
    }
}
