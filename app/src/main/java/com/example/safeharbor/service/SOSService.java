package com.example.safeharbor.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import com.example.safeharbor.data.EmergencyContact;
import com.example.safeharbor.repository.EmergencyContactRepository;
import com.example.safeharbor.utils.LocationUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import android.util.Log;
import java.util.ArrayList;

public class SOSService extends Service {
    private static final String TAG = "SOSService";
    private static final String OFFLINE_PREFS = "OfflineMessages";
    private EmergencyContactRepository contactRepository;
    private LocationUtils locationUtils;

    @Override
    public void onCreate() {
        super.onCreate();
        contactRepository = new EmergencyContactRepository(this);
        locationUtils = new LocationUtils(this);
        Log.d(TAG, "SOSService created");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SOSService started with action: " + (intent != null ? intent.getAction() : "null"));
        
        if (intent != null && "SEND_SOS".equals(intent.getAction())) {
            handleSOS();
        } else if (intent != null && "RETRY_MESSAGES".equals(intent.getAction())) {
            retrySendingMessages();
        }
        return START_STICKY;
    }

    private void handleSOS() {
        Log.d(TAG, "Handling SOS request");
        
        // Check SMS permission first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted");
            Toast.makeText(this, "Cannot send SOS: SMS permission not granted", Toast.LENGTH_LONG).show();
            return;
        }

        // Get emergency contacts
        List<EmergencyContact> contacts = contactRepository.getContacts();
        if (contacts.isEmpty()) {
            Log.w(TAG, "No emergency contacts found");
            Toast.makeText(this, "No emergency contact set! Please add an emergency contact first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Get current location
        Location location = locationUtils.getLastKnownLocation();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Create and send message
        String message = createSOSMessage(location, timestamp);
        EmergencyContact contact = contacts.get(0); // We only have one contact
        sendSOSMessage(contact.getPhoneNumber(), message);
    }

    private void sendSOSMessage(String phoneNumber, String message) {
        Log.d(TAG, "Attempting to send SOS message to: " + phoneNumber);
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> messageParts = smsManager.divideMessage(message);
            
            if (messageParts.size() > 1) {
                // For long messages that need to be split
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    messageParts,
                    null,
                    null
                );
            } else {
                // For single messages
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                );
            }
            
            Log.d(TAG, "SOS message sent successfully");
            Toast.makeText(this, "SOS alert sent to emergency contact", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SOS message", e);
            Toast.makeText(this, "Failed to send SOS. Saving for retry when possible.", Toast.LENGTH_LONG).show();
            storeOfflineMessage(phoneNumber, message);
        }
    }

    private String createSOSMessage(Location location, String timestamp) {
        StringBuilder message = new StringBuilder();
        message.append("🆘 EMERGENCY SOS ALERT!\n");
        message.append("Time: ").append(timestamp).append("\n\n");
        
        if (location != null) {
            message.append("Current Location:\n");
            message.append(String.format(Locale.US, "Latitude: %.6f\n", location.getLatitude()));
            message.append(String.format(Locale.US, "Longitude: %.6f\n\n", location.getLongitude()));
            
            // Add Google Maps link
            message.append("View location on map:\n");
            message.append(String.format(Locale.US, 
                "https://www.google.com/maps?q=%.6f,%.6f",
                location.getLatitude(), location.getLongitude()
            ));
        } else {
            message.append("Location: Not available\n");
            message.append("Please contact authorities immediately!");
        }
        
        return message.toString();
    }

    private void storeOfflineMessage(String phoneNumber, String message) {
        Log.d(TAG, "Storing message for offline sending");
        SharedPreferences prefs = getSharedPreferences(OFFLINE_PREFS, Context.MODE_PRIVATE);
        Set<String> messages = prefs.getStringSet("messages", new HashSet<>());
        Set<String> updatedMessages = new HashSet<>(messages);
        updatedMessages.add(phoneNumber + "||" + message);
        prefs.edit().putStringSet("messages", updatedMessages).apply();
        
        Log.d(TAG, "Message stored successfully");
    }

    private void retrySendingMessages() {
        Log.d(TAG, "Retrying to send stored messages");
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted for retry");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(OFFLINE_PREFS, Context.MODE_PRIVATE);
        Set<String> messages = prefs.getStringSet("messages", new HashSet<>());
        Set<String> remainingMessages = new HashSet<>();

        for (String entry : messages) {
            String[] parts = entry.split("\\|\\|");
            if (parts.length == 2) {
                try {
                    sendSOSMessage(parts[0], parts[1]);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to retry sending message", e);
                    remainingMessages.add(entry);
                }
            }
        }

        prefs.edit().putStringSet("messages", remainingMessages).apply();
    }
} 