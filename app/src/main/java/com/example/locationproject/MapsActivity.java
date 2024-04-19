package com.example.locationproject;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.locationproject.databinding.ActivityMapBinding;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapBinding binding;
    private DatabaseHelper dbHelper; // Assume you have a DatabaseHelper class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this); // Initialize your DatabaseHelper

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Query the database for the last 10 locations
        List<LocationModel> locations = dbHelper.getLastTenLocations(); // Ensure this method correctly fetches data

        if (locations != null && locations.size() > 0) {
            for (LocationModel location : locations) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                // Use wifiSSID as the title. If wifiSSID is null or empty, show "Unknown SSID"
                String markerTitle = location.getWifiSSID() != null && !location.getWifiSSID().isEmpty()
                        ? location.getWifiSSID()
                        : "Unknown SSID";
                mMap.addMarker(new MarkerOptions().position(latLng).title(markerTitle));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            }
        } else {
            // Fallback location if there are no entries in the database
            LatLng fallbackLocation = new LatLng(-34, 151); // Example: Sydney
            mMap.addMarker(new MarkerOptions().position(fallbackLocation).title("Fallback Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(fallbackLocation));
        }
    }

}
