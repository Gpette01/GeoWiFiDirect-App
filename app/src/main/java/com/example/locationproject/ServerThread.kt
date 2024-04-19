package com.example.locationproject

import android.util.Log
import java.io.IOException
import java.net.ServerSocket
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.locationproject.LocationModel


class ServerThread(
    private val onReceive: (data: String) -> Unit,
    private val databaseHelper: DatabaseHelper // Add this line
) : Thread() {
    private val serverSocketPort = 8888

    override fun run() {
        try {
            val serverSocket = ServerSocket(serverSocketPort)
            Log.d("Server", "Socket opened")

            while (!isInterrupted) {
                val client = serverSocket.accept()
                Log.d("Server"," Connection accepted")

                // Here you read data sent by the client
                // For simplicity, let's assume the client sends a string
                // Adjust this logic to fit your actual data transfer needs
                val inputstream = client.getInputStream()
                val receivedString = inputstream.bufferedReader().use { it.readLine() }

                // Handle the received data as needed
                onReceive(receivedString)
                Log.d("Server", "Data received: $receivedString")
                
                // Parse the received JSON back into a list of locations
                val gson = Gson()
                val receivedLocations = gson.fromJson<List<LocationModel>>(receivedString, object : TypeToken<List<LocationModel>>() {}.type)

                // Store the received locations in the database
                for (location in receivedLocations) {
                    // If wifiSSID is null, use an empty string or a placeholder as the default value
                    val wifiSSID = location.wifiSSID ?: ""
                    val wifiCount = location.wifiCount ?: 0
                    databaseHelper.insertLocationWithWifi(location.latitude, location.longitude, wifiSSID, wifiCount, "W")
                }

                client.close()
            }
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Server", "Error in server thread", e)
        }
    }
}
