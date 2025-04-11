package com.example.safeharbor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

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

public class LocationMonitoringService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationMonitoringChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final double WARNING_DISTANCE_KM = 2.0; // 2 kilometers
    private static final double ALERT_DISTANCE_KM = 2.0; // 2 kilometers

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Vibrator vibrator;
    private Map<String, List<List<double[]>>> boundaries;
    private boolean isAlertShowing = false;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        boundaries = new HashMap<>();
        loadBoundaries();
        setupLocationCallback();
        acquireWakeLock();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeHarbor:LocationService");
        wakeLock.acquire();
        Log.d(TAG, "Wake lock acquired");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        startLocationUpdates();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Monitoring",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Monitors location for maritime border alerts");
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[]{0, 500, 500});
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Safe Harbor")
            .setContentText("Monitoring location for maritime alerts")
            .setSmallIcon(R.drawable.ic_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
                    checkLocationAgainstBoundaries(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                .setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(0)
                .build();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                Log.d(TAG, "Location updates started");
            } else {
                Log.e(TAG, "Location permission not granted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates", e);
        }
    }

    private void checkLocationAgainstBoundaries(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        for (Map.Entry<String, List<List<double[]>>> entry : boundaries.entrySet()) {
            String boundaryName = entry.getKey();
            List<List<double[]>> boundarySegments = entry.getValue();

            double distance = calculateDistanceToBoundary(lat, lon, boundarySegments);
            Log.d(TAG, "Distance to " + boundaryName + ": " + distance + " km");
            
            if (distance <= ALERT_DISTANCE_KM && distance > 0 && !isAlertShowing) {
                triggerAlert(boundaryName, distance);
            }
        }
    }

    private void triggerAlert(String boundaryName, double distance) {
        isAlertShowing = true;
        Log.d(TAG, "Triggering alert for " + boundaryName + " at distance " + distance + " km");

        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Maritime Border Alert!")
            .setContentText(String.format("Approaching %s border (%.2f km)", boundaryName, distance))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(createAlertIntent(boundaryName, distance), true)
            .setVibrate(new long[]{0, 500, 500})
            .setLights(0xFF0000, 1000, 1000);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(2, builder.build());

        // Launch AlertActivity
        Intent alertIntent = new Intent(this, AlertActivity.class);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                           Intent.FLAG_ACTIVITY_SINGLE_TOP |
                           Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        alertIntent.putExtra("boundary_name", boundaryName);
        alertIntent.putExtra("distance", distance);
        startActivity(alertIntent);

        // Vibrate
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    private PendingIntent createAlertIntent(String boundaryName, double distance) {
        Intent alertIntent = new Intent(this, AlertActivity.class);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                           Intent.FLAG_ACTIVITY_SINGLE_TOP |
                           Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        alertIntent.putExtra("boundary_name", boundaryName);
        alertIntent.putExtra("distance", distance);
        return PendingIntent.getActivity(this, 0, alertIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void loadBoundaries() {
        // Load boundaries from CSV file
        try {
            InputStream is = getAssets().open("indian_cleaned.csv");
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
            Log.e(TAG, "Error loading boundaries", e);
        }
    }

    private List<double[]> parseCoordinates(String coordinatesStr) {
        List<double[]> coordinates = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\((\\d+\\.\\d+),\\s*(\\d+\\.\\d+)\\)");
        Matcher matcher = pattern.matcher(coordinatesStr);

        while (matcher.find()) {
            try {
                double lon = Double.parseDouble(matcher.group(1));
                double lat = Double.parseDouble(matcher.group(2));
                coordinates.add(new double[]{lat, lon});
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing coordinates", e);
            }
        }
        return coordinates;
    }

    private double calculateDistanceToBoundary(double lat, double lon, List<List<double[]>> boundarySegments) {
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
        return new double[]{x, y, z};
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 