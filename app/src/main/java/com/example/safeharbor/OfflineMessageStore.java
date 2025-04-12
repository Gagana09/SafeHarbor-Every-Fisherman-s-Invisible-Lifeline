package com.example.safeharbor;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class OfflineMessageStore {

    private static final String PREFS_NAME = "OfflineMessages";
    private static final String KEY_MESSAGES = "messages";

    // Save message to SharedPreferences
    public static void saveMessage(Context context, String phoneNumber, String message) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> messages = prefs.getStringSet(KEY_MESSAGES, new HashSet<>());

        Set<String> updatedMessages = new HashSet<>(messages);
        updatedMessages.add(phoneNumber + ";;" + message);

        prefs.edit().putStringSet(KEY_MESSAGES, updatedMessages).apply();
    }

    // Send all pending messages
    public static void sendPendingMessages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> messages = prefs.getStringSet(KEY_MESSAGES, new HashSet<>());

        if (messages != null && !messages.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();
            for (String entry : messages) {
                String[] parts = entry.split(";;");
                if (parts.length == 2) {
                    String number = parts[0];
                    String msg = parts[1];
                    try {
                        smsManager.sendTextMessage(number, null, msg, null, null);
                        Toast.makeText(context, "Sent stored SOS to " + number, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Failed to send stored SOS", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            prefs.edit().remove(KEY_MESSAGES).apply(); // Clear after sending
        }
    }
}
