package com.example.locationproject

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager


class MyService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var wifiManager: WifiManager

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 1000 * 60 * 10// Update interval in milliseconds (set to your X amount of time)
        //fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dbHelper = DatabaseHelper(this) // Initialize the database helper

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    // Log the location data


                    if (ContextCompat.checkSelfPermission(this@MyService, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this@MyService, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
                    // Start WiFi scan
                    val success = wifiManager.startScan()
                    if (success) {
                        // Usually, a BroadcastReceiver would listen for SCAN_RESULTS_AVAILABLE_ACTION to get results
                        val results = wifiManager.scanResults
                        // For simplicity, let's take the first available network (if any)
                        val firstNetwork = results.firstOrNull { it.SSID.isNotEmpty() }
                        val filteredResults = results.filter { it.SSID.isNotEmpty() }
                        val uniqueNetworks = filteredResults
                            .filter { it.SSID.isNotEmpty() }
                            .map { it.SSID }
                            .distinct()
                        val wifiCount = uniqueNetworks.size

                        firstNetwork?.let {
                            dbHelper.insertLocationWithWifi(location.latitude, location.longitude, it.SSID, wifiCount)
                        }
                    }
                    }

                    Log.d("MyService", "Location: ${location.latitude}, ${location.longitude}, WiFi: ${wifiManager.connectionInfo.ssid}, ${wifiManager.connectionInfo.rssi}")

                    }
                }
            }


        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        startLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */)
        } catch (unlikely: SecurityException) {
            // Log or handle the exception
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        dbHelper.close() // Close the database when the service is destroyed
    }
}
