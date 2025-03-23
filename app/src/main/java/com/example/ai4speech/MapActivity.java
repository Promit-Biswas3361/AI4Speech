package com.example.ai4speech;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private WebView webView;
    private ImageButton profileButton, logoutButton;
    private double userLat, userLon;
    private LocationManager locationManager;
    private StringBuilder mapData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        webView = findViewById(R.id.webView);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);
        setupWebView();

        // Request Location Permission and Get User's Location
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserLocation();
        }

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapActivity.this, ProfileActivity.class));
            }
        });

        // Logout and go back to Login Page
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MapActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MapActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            finish();
        });
    }

    // WebView Setup
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
    }

    // Request User's Location
    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                userLat = location.getLatitude();
                userLon = location.getLongitude();

                Log.d("LOCATION", "Lat: " + userLat + ", Lon: " + userLon);

                // Fetch OSM data dynamically based on user location
                new FetchOSMData().execute();
                locationManager.removeUpdates(this);
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        });
    }

    // Handle Permission Result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted — Fetch user's location
            getUserLocation();
        } else {
            // Permission denied — Show error message
            Toast.makeText(this, "Location permission denied. Cannot fetch data.", Toast.LENGTH_LONG).show();
        }
    }


//     Generate OSM API URL with Dynamic Coordinates
//    private String getOSMUrl(double lat, double lon) {
//        return "https://overpass-api.de/api/interpreter?data=[out:json];" +
//                "node(around:8000," + lat + "," + lon + ")" +
//                "[\"speciality\"~\"speech|speech therapy|therapy|speech language|communication disorder\", i];out;";
//    }

    private String getOSMUrl(double lat, double lon) {
        return "https://overpass-api.de/api/interpreter?data=[out:json];" +
                "(" +
                "node(around:8000," + lat + "," + lon + ")[\"healthcare\"=\"speech_therapy\"];" +  // Direct speech therapy clinics
                "node(around:8000," + lat + "," + lon + ")[\"amenity\"=\"clinic\"][\"speciality\"~\"speech|therapy|language|communication\", i];" +  // Clinics with related specialities
                "node(around:8000," + lat + "," + lon + ")[\"office\"=\"therapist\"][\"speciality\"~\"speech|therapy\", i];" +  // Therapist offices
                ");" +
                "out;";
    }

    // Async Task to Fetch OSM Data
    private class FetchOSMData extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(getOSMUrl(userLat, userLon));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                parseOSMData(result);
            } else {
                Toast.makeText(MapActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Parse OSM Data and Add Markers
    private void parseOSMData(String jsonData) {
        mapData = new StringBuilder();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray elements = jsonObject.getJSONArray("elements");

            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                double lat = element.getDouble("lat");
                double lon = element.getDouble("lon");

                JSONObject tags = element.getJSONObject("tags");
                String name = tags.has("name") ? tags.getString("name") : "Speech Therapist";

                // Add Markers to Leaflet Map
                mapData.append("L.marker([").append(lat).append(", ").append(lon).append("])")
                        .append(".addTo(map).bindPopup('").append(name).append("');\n");
            }

            // Load Map with Markers
            loadMapInWebView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load Leaflet Map with Markers in WebView
    private void loadMapInWebView() {
        String leafletHtml = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.css\" />\n" +
                "    <style>#map { height: 100vh; width: 100vw; }</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.js\"></script>\n" +
                "    <script>\n" +
                "        var map = L.map('map').setView([" + userLat + ", " + userLon + "], 13);\n" +
                "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "            attribution: '© OpenStreetMap contributors'\n" +
                "        }).addTo(map);\n" +
                mapData.toString() +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        webView.loadDataWithBaseURL(null, leafletHtml, "text/html", "UTF-8", null);
    }
}
