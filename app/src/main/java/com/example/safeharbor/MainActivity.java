package com.example.safeharbor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location permission if not already granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                Toast.makeText(this, "Lat: " + lat + ", Lon: " + lon, Toast.LENGTH_LONG).show();

                List<double[]> polygon = loadEEZPolygon();
                if (polygon.isEmpty()) {
                    Toast.makeText(this, "Failed to load EEZ boundaries", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isPointInsidePolygon(lat, lon, polygon)) {
                    Toast.makeText(this, "⚠️ Warning: You're outside India's EEZ!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "✅ You are within Indian waters.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<double[]> loadEEZPolygon() {
        List<double[]> polygon = new ArrayList<>();
        try {
            InputStream is = getAssets().open("india_eez_boundaries.geojson"); // ✅ FIX: case-sensitive file name\n"); // ✅ FIX: case-sensitive file name
            int size = is.available();
            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();

            if (bytesRead != size) {
                throw new IOException("Incomplete file read");
            }

            String geoJson = new String(buffer, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(geoJson);
            JSONArray features = obj.getJSONArray("features");

            if (features.length() > 0) {
                JSONObject feature = features.getJSONObject(0);
                JSONObject geometry = feature.getJSONObject("geometry");

                if (geometry.getString("type").equals("Polygon")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates").getJSONArray(0);
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray point = coordinates.getJSONArray(i);
                        double lon = point.getDouble(0);
                        double lat = point.getDouble(1);
                        polygon.add(new double[]{lat, lon});
                    }
                }
            }
        } catch (IOException | JSONException e) {
            Log.e("SafeHarbor", "GeoJSON parsing error", e);
        }
        return polygon;
    }

    private boolean isPointInsidePolygon(double lat, double lon, List<double[]> polygon) {
        boolean inside = false;
        int j = polygon.size() - 1;
        for (int i = 0; i < polygon.size(); i++) {
            double[] pi = polygon.get(i);
            double[] pj = polygon.get(j);
            if ((pi[1] > lon) != (pj[1] > lon)) {
                double intersect = (pj[0] - pi[0]) * (lon - pi[1]) / (pj[1] - pi[1]) + pi[0];
                if (lat < intersect) {
                    inside = !inside;
                }
            }
            j = i;
        }
        return inside;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
