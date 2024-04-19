package com.example.locationproject

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.gson.Gson
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.locationproject.ui.theme.LocationProjectTheme
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.locationproject.WiFiDirectBroadcastReceiver

class MainActivity : ComponentActivity(), WiFiDirectBroadcastReceiver.PeerUpdateListener {
    private val permissionsRequestCode = 1000
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: Channel
    private lateinit var receiver: BroadcastReceiver
    private val intentFilter = IntentFilter()
    private val discoveredPeers = mutableListOf<WifiP2pDevice>()
    private val databaseHelper = DatabaseHelper(this)
    private val handler = Handler(Looper.getMainLooper())
    private val logMessages = mutableStateListOf<String>() // Add this line to hold log messages

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION, // For location updates and WiFi Direct
        Manifest.permission.ACCESS_WIFI_STATE, // To access WiFi state information
        Manifest.permission.CHANGE_WIFI_STATE // To change WiFi state (if necessary for scanning)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this) // Initialize the database helper

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        receiver = WiFiDirectBroadcastReceiver(manager, channel, this, this, dbHelper)


        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        setContent {
            LocationProjectTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { mutableStateOf("main") }

                    when (currentScreen) {
                        "main" -> MainScreen(
                            onShowDb = { currentScreen = "db" },
                            onShowMap = { startActivity(Intent(this@MainActivity, MapsActivity::class.java)) },
                            onStartServer = { startServer() }, // New function for starting the server
                            onStartClient = { startClient() }, // New function for starting the client
                            onDiscoverPeers = { discoverPeers() },
                            onClearLog = { logMessages.clear() },// New discover peers function
                            logMessages = logMessages
                            )
                        "db" -> DatabaseScreen(
                            onBack = { currentScreen = "main" },
                            dbHelper = dbHelper
                        )
                    }
                }
            }
        }



        checkPermissions()
    }

    private fun checkPermissions() {
        if (requiredPermissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                permissionsRequestCode
            )
        } else {
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            // Permissions granted, you can start WiFi P2P discovery or other operations here if needed immediately after permissions are granted
        } else {
            Log.e("MainActivity", "One or more permissions were denied by the user.")
            // Consider guiding the user to the app settings for manually granting permissions
        }
    }

    private fun startLocationService() {
        Log.d("MainActivity", "Starting the location service")
        val serviceIntent = Intent(this, MyService::class.java)
        startService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
    @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onShowDb: () -> Unit, onShowMap: () -> Unit, onDiscoverPeers: () -> Unit ,onStartClient: () -> Unit, onStartServer: () -> Unit, onClearLog: () -> Unit ,logMessages: SnapshotStateList<String>){

    Column {
        TopAppBar(
            title = { Text("Main Screen") },
            actions = {
                IconButton(onClick = onShowDb) {
                    Icon(Icons.Filled.Home, contentDescription = "Show DB")
                }
                IconButton(onClick = onShowMap) {
                    Icon(Icons.Filled.Map, contentDescription = "Show Map")
                }
                IconButton(onClick = onStartServer) { // New button for starting the server
                    Icon(Icons.Filled.Download, contentDescription = "Start Server")
                }
                IconButton(onClick = onStartClient) { // New button for starting the client
                    Icon(Icons.Filled.Upload, contentDescription = "Start Client")
                }
                IconButton(onClick = onDiscoverPeers) {
                    Icon(Icons.Filled.Person, contentDescription = "Discover Peers")
                }
                IconButton(onClick = onClearLog) { // New button for clearing the log
                    Icon(Icons.Filled.Delete, contentDescription = "Clear Log")
                }
            }
        )

        Spacer(Modifier.height(16.dp))
        Text("Log Messages:", style = MaterialTheme.typography.bodyLarge)
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(logMessages) { message ->
                Text(message)
            }
        }
    }
}
    private fun addLogMessage(message: String) {
        logMessages.add(message)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DatabaseScreen(onBack: () -> Unit, dbHelper: DatabaseHelper) {
        var dbContent by remember { mutableStateOf(listOf<LocationModel>()) }

        LaunchedEffect(Unit) {
            dbContent = dbHelper.getAllLocations()
        }

        Column {
            TopAppBar(
                title = { Text("Database Screen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { dbContent = dbHelper.getAllLocations() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh DB")
                    }
                    IconButton(onClick = {
                        dbHelper.clearDatabase()
                        dbContent = listOf() // Clear the displayed data
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear DB")
                    }
                }
            )

            LazyColumn {
                items(dbContent) { location ->
                    Text(location.toString())
                }
            }
        }
    }





private fun startServer() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        addLogMessage("Location permission not granted")
        return
    }

    if (discoveredPeers.isNotEmpty()) {
        val device = discoveredPeers.first()
        val config = WifiP2pConfig().apply {
            this.deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
            groupOwnerIntent = 15 // Set this device as the group owner
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                addLogMessage("Connection initiation successful.")
                manager.requestConnectionInfo(channel, object : WifiP2pManager.ConnectionInfoListener {
                    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
                        if (info?.groupFormed == true && info.isGroupOwner){
                            addLogMessage("ServerThread: Starting server...")
                            val serverThread = ServerThread({ receivedData ->
                                addLogMessage("ServerThread: Data received: $receivedData")
                            }, databaseHelper)
                            serverThread.start()
                        }else if (info?.groupFormed == true){
                            addLogMessage("Group formed but not group owner")
                        }
                    }
                })
            }

            override fun onFailure(reason: Int) {
                addLogMessage("Connection initiation failed: $reason")
            }
        })
    }
}

private fun startClient() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        addLogMessage("Location permission not granted")
        return
    }

    if (discoveredPeers.isNotEmpty()) {
        val device = discoveredPeers.first()
        Log.d("MainActivity", "Connecting to device: ${device.deviceAddress}")
        val config = WifiP2pConfig().apply {
            this.deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                addLogMessage("Connection initiation successful.")
                manager.requestConnectionInfo(channel, object : WifiP2pManager.ConnectionInfoListener {
                    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
                        if (info?.groupFormed == true && !info.isGroupOwner){
                            addLogMessage("ClientThread: Starting client...")
                            val dataToSend = prepareDataToSend()
                            val clientThread = ClientThread(info.groupOwnerAddress.hostAddress, dataToSend)
                            clientThread.start()
                        }else if (info?.groupFormed == true){
                            addLogMessage("Group formed but is group owner")
                        }
                    }
                })
            }

            override fun onFailure(reason: Int) {
                addLogMessage("Connection initiation failed: $reason")
            }
        })
    }
}

private fun prepareDataToSend(): String {
    // Assuming your DatabaseHelper has a method getLastTenLocations() that returns List<LocationModel>
    val latestData = databaseHelper.getLastTenLocations()
    // Convert this list to JSON string (assuming you have Gson)
    return Gson().toJson(latestData)
}

    

    override fun onPeersUpdated(peers: List<WifiP2pDevice>) {
        discoveredPeers.clear()
        discoveredPeers.addAll(peers)
        // Optionally, notify the UI to refresh the list of peers for the user to connect to.
        addLogMessage("Peers updated: ${discoveredPeers.size} found.")
        // If automatic connection to the first peer is desired, initiate here.
    }


    private fun discoverPeers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            addLogMessage("Location permission not granted")
            return
        }
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                addLogMessage("Peer discovery initiated")
            }

            override fun onFailure(reasonCode: Int) {
                addLogMessage("Peer discovery initiation failed: $reasonCode")
                if (reasonCode == WifiP2pManager.BUSY) {
                    addLogMessage("System is busy, will retry discovery in a few seconds")
                    // Retry discovery after a delay if the system is busy.
                    handler.postDelayed({ discoverPeers() }, 5000)
                }
            }
        })
    }

}
