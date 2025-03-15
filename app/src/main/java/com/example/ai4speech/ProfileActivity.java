package com.example.ai4speech;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profilePicture;
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

        // Load user details (Replace this with actual data from DB or SharedPreferences)
        loadUserProfile();

        // Handle profile picture change
        editProfilePicButton.setOnClickListener(v -> openFileChooser());

        // Navigate to EditProfileActivity when Edit Profile button is clicked
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        // Fetch user details from database or shared preferences (Dummy data for now)
        userName.setText("John Doe");
        userEmail.setText("johndoe@email.com");
        lastActive.setText("Last Active: 2 days ago");

        // Fetch game statistics (Dynamic values from DB)
        gameLevelsCompleted.setText("• Levels Completed: " + getGameLevels());
        gameAccuracy.setText("• Accuracy: " + getGameAccuracy() + "%");

        // Fetch speech test statistics (Dynamic values from DB)
        speechTestsCompleted.setText("• Tests Completed: " + getSpeechTests());
        speechTestAccuracy.setText("• Accuracy: " + getSpeechTestAccuracy() + "%");
    }

    // Dummy methods to fetch statistics (Replace with actual DB calls)
    private int getGameLevels() {
        return 10; // Example value
    }

    private int getGameAccuracy() {
        return 85; // Example value
    }

    private int getSpeechTests() {
        return 5; // Example value
    }

    private int getSpeechTestAccuracy() {
        return 90; // Example value
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
