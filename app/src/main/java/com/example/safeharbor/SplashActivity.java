package com.example.safeharbor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_splash);
            Log.d(TAG, "Splash screen layout inflated");

            // Initialize views
            TextView titleText = findViewById(R.id.textView);
            TextView subtitleText = findViewById(R.id.subtitleText);
            TextView taglineText = findViewById(R.id.taglineText);

            if (titleText == null || subtitleText == null || taglineText == null) {
                Log.e(TAG, "One or more views not found");
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start fade-in animation
            titleText.setAlpha(0f);
            subtitleText.setAlpha(0f);
            taglineText.setAlpha(0f);

            titleText.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .start();

            subtitleText.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setStartDelay(500)
                    .start();

            taglineText.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setStartDelay(1000)
                    .start();

            // Navigate to MainActivity after delay
            new Handler().postDelayed(() -> {
                try {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting MainActivity", e);
                    Toast.makeText(this, "Error starting app", Toast.LENGTH_SHORT).show();
                }
            }, SPLASH_DELAY);

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "App initialization failed", Toast.LENGTH_SHORT).show();
        }
    }
} 