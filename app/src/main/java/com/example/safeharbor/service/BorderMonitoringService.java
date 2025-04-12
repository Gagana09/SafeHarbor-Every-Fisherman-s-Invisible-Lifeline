package com.example.safeharbor.service;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.example.safeharbor.utils.LocationUtils;
import android.widget.Toast;

public class BorderMonitoringService extends Service {
    private static final long CHECK_INTERVAL = 60000; // Check every minute
    private static final float BORDER_DISTANCE_THRESHOLD = 0; // 0 km from border
    private Handler handler;
    private LocationUtils locationUtils;
    private boolean isMonitoring = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        locationUtils = new LocationUtils(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isMonitoring) {
            startMonitoring();
        }
        return START_STICKY;
    }

    private void startMonitoring() {
        isMonitoring = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkBorderDistance();
                if (isMonitoring) {
                    handler.postDelayed(this, CHECK_INTERVAL);
                }
            }
        });
    }

    private void checkBorderDistance() {
        Location currentLocation = locationUtils.getLastKnownLocation();
        if (currentLocation != null) {
            float distanceToBorder = calculateDistanceToBorder(currentLocation);
            
            if (distanceToBorder <= BORDER_DISTANCE_THRESHOLD) {
                triggerSOS();
            }
        }
    }

    private float calculateDistanceToBorder(Location location) {
        // This is a simplified example. In a real app, you would:
        // 1. Have a list of border coordinates
        // 2. Calculate the minimum distance to any border segment
        // For demonstration, we'll use a simple point as border
        Location borderPoint = new Location("");
        borderPoint.setLatitude(12.3456); // Example border point
        borderPoint.setLongitude(78.9012);
        
        return location.distanceTo(borderPoint) / 1000; // Convert meters to kilometers
    }

    private void triggerSOS() {
        Intent sosIntent = new Intent(this, SOSService.class);
        sosIntent.setAction("SEND_SOS");
        startService(sosIntent);
        
        Toast.makeText(this, "Border distance alert! Sending SOS...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isMonitoring = false;
        handler.removeCallbacksAndMessages(null);
    }
} 