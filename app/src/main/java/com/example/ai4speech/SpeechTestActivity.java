package com.example.ai4speech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ai4speech.HuggingFaceApiClient;
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
                "MARK IS GOING TO SEE ELEPHANT.",
                "https://datasets-server.huggingface.co/cached-assets/mispeech/speechocean762/--/f95618ea1353303f34cf186b9c310fa2c1eb02c8/--/default/test/0/audio/audio.wav?Expires=1744734808&Signature=yPtOlSuEZ6qjPEdmzzn2RUZIqhlcADKCXW8P0pJcT3NKiJhAuEg9EA-oZ66LoT8TLcadzmtQUCKMU4JhOOKmC4IMomLA7kc0GPGiYaech8RqfmhX2zNeuRt-iTsGeU8mZ5LLuNvJQSTIqqv02cprfIEsVe7qBNV0xKH~PycGIpCr9PIh16JKPDYQwMLEg1LNgJMsYRQI4ThSeVlfROkv~kYwavEPfBR3VTi~b9RsAvwu55WtAPIyXVcuCEQWC5ovJwniFlxHPfWF95eX4KhGJ5-RYUofVkFcGGbSNedKAjyGCHTxEpY9vsqqd6msdkDAOrtRQgJj5DGvryQssL1nyw__&Key-Pair-Id=K3EI6M078Z3AC3"
        ));
        
        speechSamples.add(new SpeechSample(
                "sample_002",
                "KATE LOVES CHINA.",
                "https://datasets-server.huggingface.co/cached-assets/mispeech/speechocean762/--/f95618ea1353303f34cf186b9c310fa2c1eb02c8/--/default/test/1/audio/audio.wav?Expires=1744734808&Signature=fDRFQo1KVUzibDXUwcdIRW6ihVKkel2P3SS8ikI1FWrZdcEtqJyaImc5B3QlvpMB1Hm3ZRfX5XP6HOYhavfpXsTqorv8Jy94xJP4qnZu6EmUV2XwUwhmco1J9rfSp0v4lkCkUGW5pPzEEzfKWPlymjn~wXi2GlfSjQCfNR~TZVD0UnllJxv10lHXea0KqdVY6WNtmN~0bkcoXiiq4FO5E3~kQYWzoVrJmDAxqWqtg92Jvf9lmeH-NP64rWTv3InptF~7~RkUSJRJY4iry5Py4mDpv~SYbi0UYR5NrUQicau-BQry4NOJpmPcWYMO4jSqgHViFjSpm30Kz7W7mnr4~Q__&Key-Pair-Id=K3EI6M078Z3AC3"
        ));
        
        speechSamples.add(new SpeechSample(
                "sample_003",
                "TWO SIX FOUR EIGHT.",
                "https://datasets-server.huggingface.co/cached-assets/mispeech/speechocean762/--/f95618ea1353303f34cf186b9c310fa2c1eb02c8/--/default/test/2/audio/audio.wav?Expires=1744734808&Signature=qmJRWX7TSHD3wNNP39nZHByL46rZivUEYB-NM7gBWPGlbOpNKibe7DeL4Mpt-4f18dlfRWYDZRFtZk3jIwT~B4~7byU0i2p3zYzmuCmHmyHrOUQwXmBRvEk5-XXfFULMx9C~6lqE~L3oSSwOx7XeVy6MOyFOkOfHxjostk3S1hR40zD2r6M3isk7XhpdOqRm0-1cmjt9kVbY3jD2VP0bU9e~lVFJP9hJ2A9rrKgPYGFfJXpzY5CgNFW5FJJ4vFoQzI-2zkSu-dSXKEzNmXdJomK1vhP0U0Oz5DQyJQtsDE0iQpU13TN1kTrbA6Zmy8zZwQtkSkx1jMXatC8hAxgv3w__&Key-Pair-Id=K3EI6M078Z3AC3"
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
        String evaluationUrl = "https://api-inference.huggingface.co/models/openai/whisper-small";

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

                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseBody = response.body().string();
                                // Log or display the actual API response
                                Log.d(TAG, "API response: " + responseBody);
                                Toast.makeText(SpeechTestActivity.this, "API Success: " + responseBody, Toast.LENGTH_LONG).show();
                                // TODO: Parse responseBody as needed for your app
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing response", e);
                                Toast.makeText(SpeechTestActivity.this,
                                        "Error processing response: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (!response.isSuccessful()) {
                            String errorMessage = "API call failed: " + response.code();
                            if (response.errorBody() != null) {
                                try {
                                    errorMessage += " - " + response.errorBody().string();
                                } catch (IOException ignored) {}
                            }
                            Log.e(TAG, errorMessage);
                            Toast.makeText(SpeechTestActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            return;
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