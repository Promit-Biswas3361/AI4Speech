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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
    private FirebaseFirestore db;
    private String userId;

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

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            userId = user.getUid();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }

        submitButton.setEnabled(false);
        playButton.setVisibility(View.GONE);

        checkMicrophonePermission();

        regenerateButton.setOnClickListener(view -> generateNewText());
        micButton.setOnClickListener(view -> toggleRecording());
        playButton.setOnClickListener(view -> playRecordedAudio());
        submitButton.setOnClickListener(view -> submitRecording());

        profileButton.setOnClickListener(v -> startActivity(new Intent(SpeechTestActivity.this, ProfileActivity.class)));

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(SpeechTestActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(SpeechTestActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            finish();
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

        File audioFile = new File(getExternalCacheDir(), "speech_test.3gp");
        audioFilePath = audioFile.getAbsolutePath();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

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
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            isRecorded = true;
            micButton.setImageResource(R.drawable.mic);
            playButton.setVisibility(View.VISIBLE);
            submitButton.setEnabled(true);
            Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show();
        }
    }

    private void playRecordedAudio() {
        if (!isRecorded || audioFilePath == null) {
            Toast.makeText(this, "No recording available", Toast.LENGTH_SHORT).show();
            return;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            playButton.setImageResource(R.drawable.pause);

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
            String accuracyScore = String.format("%.2f", accuracy) + "%";
            accuracyText.setText("Accuracy: " + accuracyScore);
            accuracyText.setVisibility(View.VISIBLE);

            saveSpeechTestResult(generatedText.getText().toString(), accuracy);
        }, 4000);
    }

    private void saveSpeechTestResult(String text, double accuracy) {
        if (userId == null) return;

        String testId = "test_" + System.currentTimeMillis();
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("text", text);
        testResult.put("accuracy", accuracy);
        testResult.put("timestamp", System.currentTimeMillis());
        testResult.put("formattedDate", getCurrentDate());

        db.collection("users")
                .document(userId)
                .collection("speech_tests")
                .document(testId)
                .set(testResult)
                .addOnSuccessListener(aVoid -> Toast.makeText(SpeechTestActivity.this, "Test result saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SpeechTestActivity.this, "Failed to save test: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
