package com.example.locationproject

import android.util.Log
import java.io.IOException
import java.io.PrintStream
import java.net.InetSocketAddress
import java.net.Socket

class ClientThread(private val hostAddress: String, private val dataToSend: String) : Thread() {
    private val serverSocketPort = 8888

    override fun run() {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(hostAddress, serverSocketPort), 5000)
            Log.d("Client" ,"Connected to server")

            // Send data to the server
            val printStream = PrintStream(socket.getOutputStream())
            printStream.println(dataToSend)
            Log.d("Client", "Data sent: $dataToSend")

            printStream.close()
            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Client", "Error in client thread", e)
        }
    }
}
