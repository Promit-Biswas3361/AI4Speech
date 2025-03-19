package com.example.ai4speech;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profilePicture;
    private ImageButton profileButton, logoutButton;
    private TextView userName, userEmail, lastActive;
    private TextView gameLevelsCompleted, gameAccuracy;
    private TextView speechTestsCompleted, speechTestAccuracy;
    private Button editProfileButton, editProfilePicButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI elements
        profilePicture = findViewById(R.id.profilePicture);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        lastActive = findViewById(R.id.lastActive);
        gameLevelsCompleted = findViewById(R.id.gameLevelsCompleted);
        gameAccuracy = findViewById(R.id.gameAccuracy);
        speechTestsCompleted = findViewById(R.id.speechTestsCompleted);
        speechTestAccuracy = findViewById(R.id.speechTestAccuracy);
        editProfileButton = findViewById(R.id.editProfileButton);
        editProfilePicButton = findViewById(R.id.editProfilePicButton);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Load user profile data from Firestore
        loadUserProfile();

        // Handle profile picture change
        editProfilePicButton.setOnClickListener(v -> openFileChooser());

        // Navigate to EditProfileActivity when Edit Profile button is clicked
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, ProfileActivity.class));
            }
        });

        // Logout and go back to Login Page
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(ProfileActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            finish();
        });
    }

    // Load user profile and stats from Firestore
    private void loadUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to the user document
        DocumentReference userRef = db.collection("users").document(userId);

        // Fetch basic user data
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Load user details
                userName.setText(documentSnapshot.getString("name"));
                userEmail.setText(documentSnapshot.getString("email"));
//                lastActive.setText("Last Active: " + documentSnapshot.getString("last_active"));

                // Fetch game progress from game_progress subcollection
                loadGameStats(userId);

                // Fetch speech test stats from speech_tests subcollection
                loadSpeechTestStats(userId);
            }
        }).addOnFailureListener(e -> {
            userName.setText("Error loading profile");
        });
    }

    // Fetch game stats from game_progress/progress
    private void loadGameStats(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to game_progress/progress document
        DocumentReference gameRef = db.collection("users")
                .document(userId)
                .collection("game_progress")
                .document("progress");

        gameRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long level = documentSnapshot.getLong("current_level");
                Long levelsCompleted = (level == null) ? 0 : level;
                List<Map<String, Object>> attempts = (List<Map<String, Object>>) documentSnapshot.get("attempts");

                // Calculate total attempts and correct attempts
                int totalAttempts = (attempts != null) ? attempts.size() : 0;
                int correctAttempts = 0;

                if (attempts != null) {
                    for (Map<String, Object> attempt : attempts) {
                        String status = (String) attempt.get("status");
                        if ("correct".equalsIgnoreCase(status)) {
                            correctAttempts++;
                        }
                    }
                }

                // Set game levels and accuracy
                gameLevelsCompleted.setText("• Levels Completed: " + (levelsCompleted != null ? levelsCompleted-1. : 0));

                if (totalAttempts > 0) {
                    double accuracy =(correctAttempts * 100.0) / totalAttempts;
                    gameAccuracy.setText("• Accuracy: " + String.format("%.2f", accuracy) + "%");
                } else {
                    gameAccuracy.setText("• Accuracy: 0%");
                }
            }
            else{
                gameLevelsCompleted.setText("• Levels Completed: 0");
                gameAccuracy.setText("• Accuracy: 0%");
            }
        }).addOnFailureListener(e -> {
            gameLevelsCompleted.setText("• Levels Completed: Error");
            gameAccuracy.setText("• Accuracy: Error");
        });
    }

    // Fetch speech test stats by averaging accuracy of all speech test documents
    private void loadSpeechTestStats(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to speech_tests subcollection
        db.collection("users")
                .document(userId)
                .collection("speech_tests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalTests = 0;
                    double totalAccuracy = 0.0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double accuracy = document.getDouble("accuracy");
                        if (accuracy != null) {
                            totalAccuracy += accuracy;
                            totalTests++;
                        }
                    }

                    // Set speech tests completed
                    speechTestsCompleted.setText("• Tests Completed: " + totalTests);

                    // Calculate and set average accuracy
                    if (totalTests > 0) {
                        double avgAccuracy = totalAccuracy / totalTests;
                        speechTestAccuracy.setText("• Accuracy: " + String.format("%.2f", avgAccuracy) + "%");
                    } else {
                        speechTestAccuracy.setText("• Accuracy: 0%");
                    }
                })
                .addOnFailureListener(e -> {
                    speechTestsCompleted.setText("• Tests Completed: Error");
                    speechTestAccuracy.setText("• Accuracy: Error");
                });
    }

    // Open File Picker to choose a profile picture
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    // Handle selected image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profilePicture.setImageBitmap(bitmap);
                saveProfilePicture(imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Save the selected profile picture (Implement storage logic)
    private void saveProfilePicture(Uri imageUri) {
        // Save to local storage or database
        // You can use SharedPreferences or Firebase Storage
    }
}
