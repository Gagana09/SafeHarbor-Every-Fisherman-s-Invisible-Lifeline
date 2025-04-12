package com.example.safeharbor;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 3500; // 3.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_splash);
            Log.d(TAG, "Splash screen layout inflated");

            // Initialize views
            ImageView boatImage = findViewById(R.id.boatImage);
            TextView titleText = findViewById(R.id.titleText);
            TextView subtitleText = findViewById(R.id.subtitleText);
            TextView taglineText = findViewById(R.id.taglineText);

            if (boatImage == null || titleText == null || subtitleText == null || taglineText == null) {
                Log.e(TAG, "One or more views not found");
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set initial states
            boatImage.setAlpha(0f);
            boatImage.setTranslationX(-500f);
            titleText.setAlpha(0f);
            subtitleText.setAlpha(0f);
            taglineText.setAlpha(0f);

            // Create boat entrance animation
            ObjectAnimator boatFadeIn = ObjectAnimator.ofFloat(boatImage, "alpha", 0f, 1f);
            boatFadeIn.setDuration(1000);

            ObjectAnimator boatSlideIn = ObjectAnimator.ofFloat(boatImage, "translationX", -500f, 0f);
            boatSlideIn.setDuration(1500);
            boatSlideIn.setInterpolator(new AccelerateDecelerateInterpolator());

            // Create boat rocking animation
            ObjectAnimator boatRock = ObjectAnimator.ofFloat(boatImage, "rotation", -10f, 10f);
            boatRock.setDuration(2000);
            boatRock.setRepeatMode(ObjectAnimator.REVERSE);
            boatRock.setRepeatCount(ObjectAnimator.INFINITE);
            boatRock.setInterpolator(new AccelerateDecelerateInterpolator());

            // Create text animations
            ObjectAnimator titleFadeIn = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f);
            titleFadeIn.setDuration(1000);
            titleFadeIn.setStartDelay(500);

            ObjectAnimator subtitleFadeIn = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f);
            subtitleFadeIn.setDuration(1000);
            subtitleFadeIn.setStartDelay(1000);

            ObjectAnimator taglineFadeIn = ObjectAnimator.ofFloat(taglineText, "alpha", 0f, 1f);
            taglineFadeIn.setDuration(1000);
            taglineFadeIn.setStartDelay(1500);

            // Combine and play animations
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(boatFadeIn, boatSlideIn, boatRock, titleFadeIn, subtitleFadeIn, taglineFadeIn);
            animatorSet.start();

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