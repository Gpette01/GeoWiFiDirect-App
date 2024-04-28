# GeoWiFiDirect-App
This project was implemented as part of the UCY course on mobile computer networks.

# Project Description
**LocationProject** is an Android application designed to collect and store location data. Developed using Kotlin and Java, it employs Gradle as its build system. Key features include:

- **Location Data Collection:** Collects data such as latitude, longitude, timestamp, and source.
- **WiFi Data Collection:** Collects WiFi data including SSID and the count of WiFi networks.
- **Data Storage:** Uses a SQLite database for data storage.
- **Map Display:** Displays location data on a map, showing the first WiFi SSID stored for each location.
- **Data Sharing:** Allows sharing of data with other devices that have the app installed.

# How to Use
Here’s how to navigate and use the application:

- **Home Button:** Opens the database where you can:
  - Refresh the database screen.
  - Delete all data from the database.
- **Map Button:** Displays the 10 most recent locations from the database on a map.
- **Download Button:** Starts the server thread.
- **Upload Button:** Starts the client thread.
- **WiFi Direct Search:** Initiated by tapping the human icon.
- **Clear Log:** The trash icon clears the log on the screen.

**Usage Workflow:**
1. Provide all necessary permissions to the app.
2. The app automatically creates a new record every 10 minutes or upon restart.
3. To share data, both devices must tap the human icon. When a nearby device is detected, it shows the count of nearby devices.
4. When at least one nearby device is detected by both devices, the group owner should press the download button, followed by the upload button on the other device.
5. Error messages are displayed on the log if non-group owners attempt to start a server thread or if a group owner tries to open a client thread.


This is the main screen showing the available buttons on top: **Home**, **Map**, **Download**, **Upload**, **Peer Connection**, **Clear Log**

<img src="https://github.com/Gpette01/GeoWiFiDirect-App/blob/main/images/MainScreenEmpty.png" width="50%">

If WiFi is not enabled then this error will show up, no connection to WiFi is needed just enabled WiFi
<img src="https://github.com/Gpette01/GeoWiFiDirect-App/blob/main/images/MainScreenNoWiFi.png" width="50%">

Shows an empty DB screen

<img src="https://github.com/Gpette01/GeoWiFiDirect-App/blob/main/images/DBEmpty.png" width="50%">

A basic Log of the database, Location coordinates and SSID are blocked for security reasons.

**Cordinates, Timestamp, SSID, WiFi_Count, L/W(Local/WiFi)**

<img src="https://github.com/Gpette01/GeoWiFiDirect-App/blob/main/images/DB.jpg" width="50%">

Workflow: First clicking the **Human** button a peer connection is initiated showing how many devices are available, **Peers updated: x found.** then when 2 devices both have initiated a connection the message **Connection initiation succesful.** will appear. Then the group owner by pressing the download button start the server thread(**ServerThread: Starting server...**), then the other user by pressing the upload button(not shown in this screenshot: **ClientThread: Starting client...**) then data from client will go to server and when received the message **ServerThread: Data receieved:** will appear and the data that have been received.

<img src="https://github.com/Gpette01/GeoWiFiDirect-App/blob/main/images/MainScreenLog.jpg" width="50%">

Here the updated DB after receieving the data from the cleint and we see that the timestamp has been updated to the current time and also next to the receieved data instead of **L** we see **W** showing that they have been receieved by WiFi Direct

<img src="https://github.com/Gpette01/GeoWiFiDirect-App/blob/main/images/DBWiFi.jpg" width="50%">

Also by clicking on the map button it will shot the pins, a screenshot in not provided due to security reasons

# Basic Code Explanation
## `MainActivity.kt`
The main entry point of the application. Responsibilities include:
- Checking and requesting necessary permissions.
- Managing the user interface.
- Implementing WiFi P2P Manager.
- Handling WiFi Direct operations.
- Updating peers (calls the Client and Server thread).

## `DatabaseHelper.kt`
Manages the SQLite database operations:
- Creates and upgrades the database.
- Inserts and queries data.
- Deletes data.

## `MyService.kt`
Handles data collection in both foreground and background:
- Updates location.
- Performs WiFi scanning.

## `MapsActivity.java`
Utilizes the Google Maps API to display the last 10 locations from the database on a map.

## `ClientThread.kt`
Functions as a client to send data to the server:
- Creates a socket connection.
- Transmits data.

## `ServerThread.kt`
Operates as a server to receive data from the client:
- Establishes a socket connection.
- Manages client connections.
- Receives data.
- Processes JSON data.
- Adds data to the database.

## `WifiDirectBroadcastReceiver.kt`
Listens for specific broadcasted intents related to WiFi Direct operations:
- Called when a WiFi Direct change happens
- Notifies for WiFi Direct status
- Updates Peer list

The source files are located under: app\src\main\java\com\example\locationproject

Disclaimer, the project requires an API key that can be created by the Google Cloud Platform and must be insterted in the AndroidManifest.xml
