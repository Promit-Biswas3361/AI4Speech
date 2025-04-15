package com.example.ai4speech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

import com.example.ai4speech.api.HuggingFaceApiClient;
import com.example.ai4speech.api.PronunciationRequest;
import com.example.ai4speech.api.PronunciationResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SpeechTestActivity extends AppCompatActivity {

    private static final String TAG = "SpeechTestActivity";
    private TextView generatedText, accuracyText, fluencyText, pronunciationText;
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
    
    // Dataset information
    private List<SpeechSample> speechSamples;
    private SpeechSample currentSample;
    private static final String DATASET_URL = "https://huggingface.co/datasets/mispeech/speechocean762";
    private static final String API_ENDPOINT = "models/speechbrain/asr-wav2vec2-commonvoice-en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_test);

        generatedText = findViewById(R.id.generatedText);
        accuracyText = findViewById(R.id.accuracyText);
        fluencyText = findViewById(R.id.fluencyText);
        pronunciationText = findViewById(R.id.pronunciationText);
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

        // Initialize dataset samples
        initializeSpeechSamples();
        
        // Get a random sample to start with
        getRandomSpeechSample();

        checkMicrophonePermission();

        regenerateButton.setOnClickListener(view -> getRandomSpeechSample());
        micButton.setOnClickListener(view -> toggleRecording());
        playButton.setOnClickListener(view -> playRecordedAudio());
        submitButton.setOnClickListener(view -> submitRecording());

        profileButton.setOnClickListener(v -> startActivity(new Intent(SpeechTestActivity.this, ProfileActivity.class)));

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(SpeechTestActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(SpeechTestActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void initializeSpeechSamples() {
        // This would ideally come from a local database or API call
        // For now, we'll hardcode a few samples
        speechSamples = new ArrayList<>();
        
        // Add sample entries from the dataset
        // In a real app, you would load these from a local database or API
        speechSamples.add(new SpeechSample(
                "sample_001",
                "The quick brown fox jumps over the lazy dog.",
                "https://huggingface.co/datasets/mispeech/speechocean762/resolve/main/audio/sample_001.wav"
        ));
        
        speechSamples.add(new SpeechSample(
                "sample_002",
                "How much wood would a woodchuck chuck if a woodchuck could chuck wood?",
                "https://huggingface.co/datasets/mispeech/speechocean762/resolve/main/audio/sample_002.wav"
        ));
        
        speechSamples.add(new SpeechSample(
                "sample_003",
                "She sells seashells by the seashore.",
                "https://huggingface.co/datasets/mispeech/speechocean762/resolve/main/audio/sample_003.wav"
        ));
    }
    
    private void getRandomSpeechSample() {
        if (speechSamples == null || speechSamples.isEmpty()) {
            Toast.makeText(this, "No speech samples available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Random random = new Random();
        currentSample = speechSamples.get(random.nextInt(speechSamples.size()));
        generatedText.setText(currentSample.getText());
        
        // Reset UI elements
        accuracyText.setVisibility(View.GONE);
        fluencyText.setVisibility(View.GONE);
        pronunciationText.setVisibility(View.GONE);
        playButton.setVisibility(View.GONE);
        submitButton.setEnabled(false);
        isRecorded = false;
    }

    private void checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
        }
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

        File audioFile = new File(getExternalCacheDir(), "speech_test.wav"); // Changed to WAV format
        audioFilePath = audioFile.getAbsolutePath();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setAudioSamplingRate(44100);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            micButton.setImageResource(R.drawable.mic_recording);
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Recording failed! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Recording failed", e);
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
                Log.e(TAG, "Error stopping recording", e);
                Toast.makeText(this, "Error stopping recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
            Log.e(TAG, "Playback failed", e);
        }
    }

    private void submitRecording() {
        if (!isRecorded) {
            Toast.makeText(this, "Please record your speech first", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingIcon.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        // Prepare file for upload
        File audioFile = new File(audioFilePath);
        
        // Create multipart request
        MultipartBody.Part audioPart = HuggingFaceApiClient.getInstance().prepareAudioFilePart(audioFile);
        RequestBody referenceAudioUrl = RequestBody.create(
                MediaType.parse("text/plain"), 
                currentSample.getAudioUrl()
        );
        
        // API endpoint URL
        String evaluationUrl = "https://api-inference.huggingface.co/models/speechbrain/speech-recognition-wav2vec2-librispeech-asr";
        
        // Make API call
        HuggingFaceApiClient.getInstance().getApiService()
                .uploadAudioFile(
                        evaluationUrl,
                        HuggingFaceApiClient.getInstance().getAuthorizationHeader(),
                        audioPart,
                        referenceAudioUrl
                )
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        loadingIcon.setVisibility(View.GONE);
                        
                        if (response.isSuccessful()) {
                            try {
                                // For now, we'll simulate the response as the actual API integration
                                // would depend on a specific Hugging Face pronunciation evaluation model
                                simulatePronunciationEvaluation();
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing response", e);
                                Toast.makeText(SpeechTestActivity.this, 
                                        "Error processing response: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "API call failed: " + response.code());
                            Toast.makeText(SpeechTestActivity.this, 
                                    "API call failed: " + response.code(), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        loadingIcon.setVisibility(View.GONE);
                        Log.e(TAG, "API call error", t);
                        Toast.makeText(SpeechTestActivity.this, 
                                "API call error: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    // This simulates the response from the Hugging Face API
    // In a real implementation, you would parse the actual response
    private void simulatePronunciationEvaluation() {
        // Simulate scores between 0.7 and 1.0 for a more realistic range
        double accuracy = 0.7 + (Math.random() * 0.3);
        double fluency = 0.7 + (Math.random() * 0.3);
        double pronunciation = 0.7 + (Math.random() * 0.3);
        
        // Calculate percentage scores
        int accuracyPercent = (int) (accuracy * 100);
        int fluencyPercent = (int) (fluency * 100);
        int pronunciationPercent = (int) (pronunciation * 100);
        
        // Update UI
        accuracyText.setText("Accuracy: " + accuracyPercent + "%");
        accuracyText.setVisibility(View.VISIBLE);
        
        fluencyText.setText("Fluency: " + fluencyPercent + "%");
        fluencyText.setVisibility(View.VISIBLE);
        
        pronunciationText.setText("Pronunciation: " + pronunciationPercent + "%");
        pronunciationText.setVisibility(View.VISIBLE);
        
        // Save results to Firebase
        saveSpeechTestResult(generatedText.getText().toString(), accuracy, fluency, pronunciation);
    }

    private void saveSpeechTestResult(String text, double accuracy, double fluency, double pronunciation) {
        if (userId == null) return;

        String testId = "test_" + System.currentTimeMillis();
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("text", text);
        testResult.put("accuracy", accuracy);
        testResult.put("fluency", fluency);
        testResult.put("pronunciation", pronunciation);
        testResult.put("timestamp", System.currentTimeMillis());
        testResult.put("formattedDate", getCurrentDate());
        testResult.put("sampleId", currentSample.getId());

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
    
    // Data class to represent speech samples from the dataset
    private static class SpeechSample {
        private final String id;
        private final String text;
        private final String audioUrl;
        
        public SpeechSample(String id, String text, String audioUrl) {
            this.id = id;
            this.text = text;
            this.audioUrl = audioUrl;
        }
        
        public String getId() {
            return id;
        }
        
        public String getText() {
            return text;
        }
        
        public String getAudioUrl() {
            return audioUrl;
        }
    }
}