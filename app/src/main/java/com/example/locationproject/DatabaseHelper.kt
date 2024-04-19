package com.example.locationproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "locationsDatabase"
        // Increment database version to trigger onUpgrade for existing installations
        private const val DATABASE_VERSION = 2

        private const val TABLE_LOCATIONS = "locations"
        private const val COLUMN_ID = "id"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_WIFI_SSID = "wifi_ssid"
        private const val COLUMN_WIFI_COUNT = "wifi_count"
        private const val COLUMN_SOURCE = "source" // New column for the data source (L or W)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableStatement = """
            CREATE TABLE $TABLE_LOCATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_WIFI_SSID TEXT,
                $COLUMN_WIFI_COUNT INTEGER,
                $COLUMN_TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_SOURCE TEXT DEFAULT 'L'
            )
        """.trimIndent()
        db.execSQL(createTableStatement)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add case statements for each database version
        // This allows for incremental upgrades
        when (oldVersion) {
            1 -> {
                // If the old version is 1, then we just need to add the source column
                // to upgrade to version 2.
                db.execSQL("ALTER TABLE $TABLE_LOCATIONS ADD COLUMN $COLUMN_SOURCE TEXT DEFAULT 'L'")
                // For future database versions, add additional cases here
            }
            // Future upgrade paths can be added here as else-if branches
        }
    }


    fun insertLocation(latitude: Double, longitude: Double, source: String = "L") {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_SOURCE, source) // Include the source of the data
        }
        db.insert(TABLE_LOCATIONS, null, values)
        db.close()
    }

    fun insertLocationWithWifi(latitude: Double, longitude: Double, wifiSSID: String, wifiCount: Int, source: String = "L") {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_WIFI_SSID, wifiSSID)
            put(COLUMN_WIFI_COUNT, wifiCount)
            put(COLUMN_SOURCE, source) // Include the source of the data
        }
        db.insert(TABLE_LOCATIONS, null, values)
        db.close()
    }

    fun getAllLocations(): List<LocationModel> {
        val locations = mutableListOf<LocationModel>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_LOCATIONS, null, null, null, null, null, "$COLUMN_TIMESTAMP DESC")

        if (cursor.moveToFirst()) {
            do {
                val location = LocationModel(
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_WIFI_SSID)),
                    cursor.getInt(cursor.getColumnIndex(COLUMN_WIFI_COUNT)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_SOURCE)) // Fetching the source
                )
                locations.add(location)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return locations
    }


    fun getLastTenLocations(): List<LocationModel> {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_LOCATIONS ORDER BY $COLUMN_TIMESTAMP DESC LIMIT 10"
        val cursor = db.rawQuery(query, null)
        val locations = mutableListOf<LocationModel>()

        if (cursor.moveToFirst()) {
            do {
                val location = LocationModel(
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_WIFI_SSID)),
                    cursor.getInt(cursor.getColumnIndex(COLUMN_WIFI_COUNT)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_SOURCE)) // Fetching the source
                )
                locations.add(location)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return locations
    }


    fun clearDatabase() {
        val db = this.writableDatabase

        // Option 1: Delete all entries from the locations table
        db.delete(TABLE_LOCATIONS, null, null)

        // Option 2: Use execSQL if you prefer raw SQL statement
        // db.execSQL("DELETE FROM $TABLE_LOCATIONS")

        db.close()
    }

}

data class LocationModel(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val wifiSSID: String? = null, // New, nullable to handle locations without WiFi data
    val wifiCount: Int? = null, // New, nullable for the same reason
    val source: String
) {
    override fun toString(): String {
        return "Location: $latitude, $longitude, $timestamp, $wifiSSID, $wifiCount, $source"
    }
}
