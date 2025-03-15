package com.example.ai4speech;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

public class SpeechTestActivity extends AppCompatActivity {

    private TextView generatedText, accuracyText;
    private ImageButton regenerateButton, micButton, playButton, profileButton, logoutButton;
    private Button submitButton;
    private ProgressBar loadingIcon;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private boolean isRecording = false;
    private boolean isRecorded = false;
    private static final int REQUEST_MIC_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_test);

        generatedText = findViewById(R.id.generatedText);
        accuracyText = findViewById(R.id.accuracyText);
        regenerateButton = findViewById(R.id.regenerateButton);
        micButton = findViewById(R.id.micButton);
        playButton = findViewById(R.id.playButton);
        submitButton = findViewById(R.id.submitButton);
        loadingIcon = findViewById(R.id.loadingIcon);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        submitButton.setEnabled(false);
        playButton.setVisibility(View.GONE);

        checkMicrophonePermission();

        regenerateButton.setOnClickListener(view -> generateNewText());
        micButton.setOnClickListener(view -> toggleRecording());
        playButton.setOnClickListener(view -> playRecordedAudio());
        submitButton.setOnClickListener(view -> submitRecording());

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SpeechTestActivity.this, ProfileActivity.class));
            }
        });

        // Logout and go back to Login Page
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isLoggedIn", false);
                editor.apply();

                Toast.makeText(SpeechTestActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SpeechTestActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
        }
    }

    private void generateNewText() {
        generatedText.setText("This is a new randomly generated text for speech test.");
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set correct file path
        File audioFile = new File(getExternalCacheDir(), "speech_test.3gp");
        audioFilePath = audioFile.getAbsolutePath();

        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);  // Better format
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  // High-quality encoder

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            micButton.setImageResource(R.drawable.mic_recording);
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Recording failed! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                isRecorded = true;
                micButton.setImageResource(R.drawable.mic);
                playButton.setVisibility(View.VISIBLE);
                submitButton.setEnabled(true);
                Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error stopping recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void playRecordedAudio() {
        if (!isRecorded || audioFilePath == null) {
            Toast.makeText(this, "No recording available", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            Toast.makeText(this, "Audio file not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            playButton.setImageResource(R.drawable.pause);
            Toast.makeText(this, "Playing recorded audio...", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> playButton.setImageResource(R.drawable.play));
        } catch (IOException e) {
            Toast.makeText(this, "Playback failed! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void submitRecording() {
        if (!isRecorded) {
            Toast.makeText(this, "Please record your speech first", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingIcon.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        new Handler().postDelayed(() -> {
            loadingIcon.setVisibility(View.GONE);
            double accuracy = Math.random() * 20 + 80;
            accuracyText.setText("Accuracy: " + String.format("%.2f", accuracy) + "%");
            accuracyText.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Analysis complete!", Toast.LENGTH_SHORT).show();
        }, 4000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
