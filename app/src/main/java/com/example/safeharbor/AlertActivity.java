package com.example.safeharbor;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AlertActivity extends AppCompatActivity {
    private static final String TAG = "AlertActivity";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        // Get data from intent
        String boundaryName = getIntent().getStringExtra("boundary_name");
        double distance = getIntent().getDoubleExtra("distance", 0);

        // Initialize views
        TextView alertText = findViewById(R.id.alertText);
        TextView distanceText = findViewById(R.id.distanceText);
        Button dismissButton = findViewById(R.id.dismissButton);

        // Set alert text
        alertText.setText("MARITIME BORDER ALERT!");
        distanceText.setText(String.format("You are %.2f km from %s border", distance, boundaryName));

        // Initialize vibrator
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0));
        }

        // Initialize media player with alert sound
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // Setup dismiss button
        dismissButton.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
            finish();
        });
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
} 