package com.example.safeharbor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.safeharbor.service.BorderMonitoringService;
import com.example.safeharbor.service.NetworkMonitorService;
import com.example.safeharbor.service.SOSService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final double WARNING_DISTANCE_KM = 35;
    private static final String TAG = "SafeHarbor";
    private static final String PREFS_NAME = "SafeHarborPrefs";
    private static final String KEY_LAST_LAT = "last_latitude";
    private static final String KEY_LAST_LON = "last_longitude";
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private static final long MAX_LOCATION_AGE = 30 * 60 * 1000; // 30 minutes

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView tvLocation;
    private TextView tvCountry;
    private TextView tvDistance;
    private TextView tvWarning;
    private Map<String, List<List<double[]>>> boundaries;
    private View alertLayout;
    private TextView distanceWarningText;
    private Button btnDismissAlert;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean isAlertShowing = false;
    private View offlineBar;
    private TextView tvOfflineStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "onCreate started");
            
            setContentView(R.layout.activity_main);
            Log.d(TAG, "Layout inflated");

            initializeViews();
            Log.d(TAG, "Views initialized");

            setupLocation();
            Log.d(TAG, "Location client setup");

            setupAlert();
            Log.d(TAG, "Alert setup completed");

            loadBoundaries();
            Log.d(TAG, "Border data loaded");

            checkLocationPermission();
            Log.d(TAG, "Location permission check completed");

            // Start network monitoring service
            startService(new Intent(this, NetworkMonitorService.class));

            // Initialize SOS button
            Button sosButton = findViewById(R.id.sosButton);
            sosButton.setOnClickListener(v -> triggerSOS());

            // Initialize emergency contacts button
            Button emergencyContactsButton = findViewById(R.id.emergencyContactsButton);
            emergencyContactsButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, EmergencyContactsActivity.class);
                startActivity(intent);
            });

            // Start the background service
            startLocationMonitoringService();

            // Start border monitoring service
            startBorderMonitoring();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "App initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        try {
            Log.d(TAG, "Starting view initialization");
            
            // Initialize basic views
            tvLocation = findViewById(R.id.tvLocation);
            tvCountry = findViewById(R.id.tvCountry);
            tvDistance = findViewById(R.id.tvDistance);
            offlineBar = findViewById(R.id.offlineBar);
            tvOfflineStatus = findViewById(R.id.offlineText);
            alertLayout = findViewById(R.id.alertLayout);
            distanceWarningText = findViewById(R.id.distanceWarningText);
            btnDismissAlert = findViewById(R.id.btnDismissAlert);

            // Log each view initialization
            Log.d(TAG, "tvLocation: " + (tvLocation != null));
            Log.d(TAG, "tvCountry: " + (tvCountry != null));
            Log.d(TAG, "tvDistance: " + (tvDistance != null));
            Log.d(TAG, "offlineBar: " + (offlineBar != null));
            Log.d(TAG, "tvOfflineStatus: " + (tvOfflineStatus != null));
            Log.d(TAG, "alertLayout: " + (alertLayout != null));
            Log.d(TAG, "distanceWarningText: " + (distanceWarningText != null));
            Log.d(TAG, "btnDismissAlert: " + (btnDismissAlert != null));

            // Check for null views
            if (tvLocation == null || tvCountry == null || tvDistance == null || 
                offlineBar == null || tvOfflineStatus == null ||
                alertLayout == null || distanceWarningText == null || btnDismissAlert == null) {
                String errorMessage = "Missing views: ";
                if (tvLocation == null) errorMessage += "tvLocation ";
                if (tvCountry == null) errorMessage += "tvCountry ";
                if (tvDistance == null) errorMessage += "tvDistance ";
                if (offlineBar == null) errorMessage += "offlineBar ";
                if (tvOfflineStatus == null) errorMessage += "tvOfflineStatus ";
                if (alertLayout == null) errorMessage += "alertLayout ";
                if (distanceWarningText == null) errorMessage += "distanceWarningText ";
                if (btnDismissAlert == null) errorMessage += "btnDismissAlert ";
                
                Log.e(TAG, errorMessage);
                Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                return;
            }

            // Initialize vibrator
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null) {
                Log.w(TAG, "Vibrator service not available");
            }
            
            // Setup alert button
            btnDismissAlert.setOnClickListener(v -> {
                try {
                    hideAlert();
                } catch (Exception e) {
                    Log.e(TAG, "Error in alert button click", e);
                }
            });
            
            Log.d(TAG, "All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in initializeViews", e);
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
        checkLocationPermission();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocationInfo(location);
                    saveLastKnownLocation(location);
                }
            }
        };
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                    .setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                    .build();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage());
        }
    }

    private void updateLocationInfo(Location location) {
        if (location == null) {
            loadLastKnownLocation();
            return;
        }

                double lat = location.getLatitude();
                double lon = location.getLongitude();

        // Check if we're offline
        boolean isOffline = !NetworkUtils.isNetworkAvailable(this);
        if (isOffline) {
            showOfflineWarning(System.currentTimeMillis() - location.getTime());
        } else {
            offlineBar.setVisibility(View.GONE);
        }

        // Calculate distances to each boundary
        double distanceToSriLanka = 0;
        double distanceToMaldives = 0;
        double distanceToBangladesh = 0;

        if (boundaries.isEmpty()) {
            Log.w(TAG, "No boundaries found! Using sample data for testing");
            distanceToSriLanka = 500;
            distanceToMaldives = 800;
            distanceToBangladesh = 1200;
        } else {
            try {
                List<List<double[]>> sriLankaBoundary = boundaries.get("Sri Lanka - India");
                if (sriLankaBoundary != null && !sriLankaBoundary.isEmpty()) {
                    distanceToSriLanka = calculateDistanceToBoundary(lat, lon, sriLankaBoundary);
                }

                List<List<double[]>> maldivesBoundary = boundaries.get("Maldives - India");
                if (maldivesBoundary != null && !maldivesBoundary.isEmpty()) {
                    distanceToMaldives = calculateDistanceToBoundary(lat, lon, maldivesBoundary);
                }

                List<List<double[]>> bangladeshBoundary = boundaries.get("Bangladesh - India");
                if (bangladeshBoundary != null && !bangladeshBoundary.isEmpty()) {
                    distanceToBangladesh = calculateDistanceToBoundary(lat, lon, bangladeshBoundary);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating distances: " + e.getMessage());
            }
        }

        // Format the distance strings
        String sriLankaDistStr = distanceToSriLanka > 0 ? String.format("%.2f km", distanceToSriLanka) : "N/A";
        String maldivesDistStr = distanceToMaldives > 0 ? String.format("%.2f km", distanceToMaldives) : "N/A";
        String bangladeshDistStr = distanceToBangladesh > 0 ? String.format("%.2f km", distanceToBangladesh) : "N/A";

        // Update UI with location and distance information
        tvLocation.setText(String.format("Location: %.6f, %.6f", lat, lon));
        tvCountry.setText(String.format("Distance to Sri Lanka: %s\nDistance to Maldives: %s\nDistance to Bangladesh: %s",
                sriLankaDistStr, maldivesDistStr, bangladeshDistStr));

        // Check if any distance is within warning threshold
        double minDistance = Math.min(Math.min(distanceToSriLanka, distanceToMaldives), distanceToBangladesh);
        if (minDistance <= WARNING_DISTANCE_KM && minDistance > 0) {
            showAlert(minDistance);
            } else {
            hideAlert();
        }
    }

    private void loadBoundaries() {
        boundaries = loadBoundariesFromCSV("indian_cleaned.csv");
        if (boundaries.isEmpty()) {
            Log.w(TAG, "No boundaries loaded, using test data");
            addTestBoundaries(boundaries);
        }
    }

    private double calculateDistanceToBoundary(double lat, double lon, List<List<double[]>> boundarySegments) {
        if (boundarySegments == null || boundarySegments.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double minDistance = Double.MAX_VALUE;

        for (List<double[]> segment : boundarySegments) {
            if (segment != null && segment.size() >= 2) {
                double segmentDistance = calculateDistanceToLineString(lat, lon, segment);
                if (segmentDistance < minDistance && segmentDistance > 0) {
                    minDistance = segmentDistance;
                }
            }
        }

        return minDistance < Double.MAX_VALUE ? minDistance : 0;
    }

    private double calculateDistanceToLineString(double lat, double lon, List<double[]> lineString) {
        if (lineString == null || lineString.size() < 2) {
            return Double.MAX_VALUE;
        }

        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < lineString.size() - 1; i++) {
            double[] p1 = lineString.get(i);
            double[] p2 = lineString.get(i + 1);

            if (p1 != null && p2 != null && p1.length >= 2 && p2.length >= 2) {
                double distance = distanceToLineSegment(lat, lon, p1[0], p1[1], p2[0], p2[1]);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
        }

        return minDistance;
    }

    private double distanceToLineSegment(double lat, double lon, double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;

        double d1 = calculateHaversineDistance(lat, lon, lat1, lon1);
        double d2 = calculateHaversineDistance(lat, lon, lat2, lon2);

        double segmentLength = calculateHaversineDistance(lat1, lon1, lat2, lon2);
        if (segmentLength < 0.1) {
            return Math.min(d1, d2);
        }

        double[] p = getCartesian(lat, lon, EARTH_RADIUS);
        double[] p1 = getCartesian(lat1, lon1, EARTH_RADIUS);
        double[] p2 = getCartesian(lat2, lon2, EARTH_RADIUS);

        double[] v = {p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]};
        double[] w = {p[0] - p1[0], p[1] - p1[1], p[2] - p1[2]};

        double c1 = dot(w, v);
        double c2 = dot(v, v);

        if (c1 <= 0) return d1;
        if (c2 <= c1) return d2;

        double b = c1 / c2;
        double[] pb = {p1[0] + b * v[0], p1[1] + b * v[1], p1[2] + b * v[2]};

        return Math.sqrt(
                Math.pow(p[0] - pb[0], 2) +
                Math.pow(p[1] - pb[1], 2) +
                Math.pow(p[2] - pb[2], 2)
        );
    }

    private double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private double[] getCartesian(double lat, double lon, double radius) {
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);
        double x = radius * Math.cos(latRad) * Math.cos(lonRad);
        double y = radius * Math.cos(latRad) * Math.sin(lonRad);
        double z = radius * Math.sin(latRad);
        return new double[] {x, y, z};
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    private Map<String, List<List<double[]>>> loadBoundariesFromCSV(String csvFileName) {
        Map<String, List<List<double[]>>> boundaries = new HashMap<>();

        try {
            InputStream is = getAssets().open(csvFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 6) {
                    String lineName = parts[0].trim();
                    String coordinatesStr = parts[5].trim();
                    List<double[]> coordinates = parseCoordinates(coordinatesStr);

                    if (!coordinates.isEmpty()) {
                        if (!boundaries.containsKey(lineName)) {
                            boundaries.put(lineName, new ArrayList<>());
                        }
                        boundaries.get(lineName).add(coordinates);
                    }
                }
            }

            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Error loading CSV: " + e.getMessage());
        }

        return boundaries;
    }

    private List<double[]> parseCoordinates(String coordinatesStr) {
        List<double[]> coordinates = new ArrayList<>();

        if (coordinatesStr == null || coordinatesStr.isEmpty()) {
            return coordinates;
        }

        try {
            Pattern pattern = Pattern.compile("\\((\\d+\\.\\d+),\\s*(\\d+\\.\\d+)\\)");
            Matcher matcher = pattern.matcher(coordinatesStr);

            while (matcher.find()) {
                try {
                    double lon = Double.parseDouble(matcher.group(1));
                    double lat = Double.parseDouble(matcher.group(2));
                    coordinates.add(new double[] { lat, lon });
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse coordinate values: " + e.getMessage());
                }
            }

            if (coordinates.isEmpty()) {
                String[] parts = coordinatesStr.split("\\),\\s*\\(");
                for (String part : parts) {
                    part = part.replaceAll("[\\[\\]\\(\\)]", "").trim();
                    String[] coords = part.split(",");
                    if (coords.length >= 2) {
                        try {
                            double lon = Double.parseDouble(coords[0].trim());
                            double lat = Double.parseDouble(coords[1].trim());
                            coordinates.add(new double[] { lat, lon });
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Failed to parse alternative coordinate format: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing coordinates: " + e.getMessage());
        }

        return coordinates;
    }

    private void addTestBoundaries(Map<String, List<List<double[]>>> boundaries) {
        List<double[]> sriLankaCoords = new ArrayList<>();
        sriLankaCoords.add(new double[] {9.0, 80.0});
        sriLankaCoords.add(new double[] {8.0, 80.0});

        List<List<double[]>> sriLankaList = new ArrayList<>();
        sriLankaList.add(sriLankaCoords);
        boundaries.put("Sri Lanka - India", sriLankaList);

        List<double[]> maldivesCoords = new ArrayList<>();
        maldivesCoords.add(new double[] {4.0, 73.0});
        maldivesCoords.add(new double[] {3.0, 73.0});

        List<List<double[]>> maldivesList = new ArrayList<>();
        maldivesList.add(maldivesCoords);
        boundaries.put("Maldives - India", maldivesList);

        List<double[]> bangladeshCoords = new ArrayList<>();
        bangladeshCoords.add(new double[] {24.0, 90.0});
        bangladeshCoords.add(new double[] {23.0, 90.0});

        List<List<double[]>> bangladeshList = new ArrayList<>();
        bangladeshList.add(bangladeshCoords);
        boundaries.put("Bangladesh - India", bangladeshList);
    }

    private void setupAlert() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        mediaPlayer.setLooping(true);

        btnDismissAlert.setOnClickListener(v -> hideAlert());
    }

    private void showAlert(double distance) {
        if (alertLayout.getVisibility() == View.VISIBLE) return;

        // Use system default notification sound
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // Vibrate
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0));
        }

        String warningText = String.format("Warning: You are %.2f nautical miles from international waters!", distance);
        distanceWarningText.setText(warningText);

        // Show overlay with fade animation
        View overlay = findViewById(R.id.alertOverlay);
        overlay.setAlpha(0f);
        overlay.setVisibility(View.VISIBLE);
        overlay.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        // Show alert with fade and scale animation
        alertLayout.setAlpha(0f);
        alertLayout.setScaleX(0.8f);
        alertLayout.setScaleY(0.8f);
        alertLayout.setVisibility(View.VISIBLE);
        alertLayout.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();

        isAlertShowing = true;
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "maritime_alerts")
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
    }

    private void hideAlert() {
        if (isAlertShowing) {
            isAlertShowing = false;

            // Hide overlay with fade animation
            View overlay = findViewById(R.id.alertOverlay);
            overlay.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> overlay.setVisibility(View.GONE))
                    .start();

            // Hide alert with fade and scale animation
            alertLayout.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> alertLayout.setVisibility(View.GONE))
                    .start();

            if (vibrator != null) {
                vibrator.cancel();
            }

            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0);
            }
        }
    }

    private void saveLastKnownLocation(Location location) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_LAST_LAT, (float) location.getLatitude());
        editor.putFloat(KEY_LAST_LON, (float) location.getLongitude());
        editor.putLong(KEY_LAST_UPDATE, location.getTime());
        editor.apply();
    }

    private void loadLastKnownLocation() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float lat = prefs.getFloat(KEY_LAST_LAT, 0);
        float lon = prefs.getFloat(KEY_LAST_LON, 0);
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);

        if (lat != 0 && lon != 0) {
            long age = System.currentTimeMillis() - lastUpdate;
            if (age <= MAX_LOCATION_AGE) {
                Location location = new Location("cached");
                location.setLatitude(lat);
                location.setLongitude(lon);
                location.setTime(lastUpdate);
                updateLocationInfo(location);
                showOfflineWarning(age);
            } else {
                tvLocation.setText("Location data too old");
                tvCountry.setText("Please enable location services");
                tvDistance.setText("Distance: N/A");
            }
        }
    }

    private void showOfflineWarning(long age) {
        offlineBar.setVisibility(View.VISIBLE);
        String timeText;
        if (age < 60000) {
            timeText = "less than a minute ago";
        } else if (age < 3600000) {
            timeText = (age / 60000) + " minutes ago";
        } else {
            timeText = (age / 3600000) + " hours ago";
        }
        tvOfflineStatus.setText("Offline - Last update: " + timeText);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            tvLocation.setText("Location permission denied");
            tvCountry.setText("Please enable location permission");
            tvDistance.setText("Distance: N/A");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void startLocationMonitoringService() {
        try {
            Intent serviceIntent = new Intent(this, LocationMonitoringService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Location monitoring service started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting location monitoring service", e);
            Toast.makeText(this, "Error starting background monitoring", Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerSOS() {
        Intent sosIntent = new Intent(this, SOSService.class);
        sosIntent.setAction("SEND_SOS");
        startService(sosIntent);
        Toast.makeText(this, "Sending SOS alert...", Toast.LENGTH_LONG).show();
    }

    private void startBorderMonitoring() {
        Intent monitoringIntent = new Intent(this, BorderMonitoringService.class);
        startService(monitoringIntent);
    }
}
