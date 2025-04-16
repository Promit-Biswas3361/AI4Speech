package com.example.ai4speech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView levelIndicator;
    private ImageView objectImage, feedbackIcon;
    private LinearLayout letterBoxesContainer;
    private GridLayout alphabetKeyboard;
    private Button submitButton, nextButton;
    private ImageButton logoutButton, profileButton;
    private List<GameObject> objectList;
    private ImageButton speakerButton;
    private MediaPlayer mediaPlayer;

    private HashSet<Integer> usedObjects = new HashSet<>();
    private String currentWord = "";
    private StringBuilder userInput = new StringBuilder();
    private int currentLevel = 1;
    private final int TOTAL_LEVELS = 15;

    private void playWordAudio(String word) {
        // Ensure previous audio is released
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        int resId = getResources().getIdentifier(word.toLowerCase(), "raw", getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(this, resId);
            mediaPlayer.start();
        } else {
            Toast.makeText(this, "Audio not found for " + word, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        levelIndicator = findViewById(R.id.levelIndicator);
        objectImage = findViewById(R.id.objectImage);
        letterBoxesContainer = findViewById(R.id.letterBoxesContainer);
        alphabetKeyboard = findViewById(R.id.alphabetKeyboard);
        feedbackIcon = findViewById(R.id.feedbackIcon);
        submitButton = findViewById(R.id.submitButton);
        nextButton = findViewById(R.id.nextButton);
        logoutButton = findViewById(R.id.logoutButton);
        profileButton = findViewById(R.id.profileButton);
        speakerButton = findViewById(R.id.speakerButton);
        speakerButton.setOnClickListener(v -> playWordAudio(currentWord));


        // Handle logout button click
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(GameActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            finish();
        });

        // Handle profile button click
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        objectList = GameObjectList.getObjects();
        setupKeyboard();
        loadUserProgress(); // Load progress from Firestore
        submitButton.setOnClickListener(v -> onSubmit());
        nextButton.setOnClickListener(v -> onNext());
    }

    // Load user's game progress from Firestore
    private void loadUserProgress() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to 'game_progress/progress' document
        DocumentReference progressRef = db.collection("users")
                .document(userId)
                .collection("game_progress")
                .document("progress");

        progressRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long savedLevel = documentSnapshot.getLong("current_level");
                if (savedLevel != null) {
                    currentLevel = savedLevel.intValue() ; // Load saved level
                } else {
                    currentLevel = 1; // Start from level 1 if no data found
                }
            } else {
                currentLevel = 1; // No progress document, start fresh
            }
            loadNewLevel(); // Load the correct level after fetching progress
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load progress: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            currentLevel = 1; // Fallback to level 1 in case of error
            loadNewLevel(); // Load level 1 if progress can't be fetched
        });
    }


    // Load a new level or finish the game if all levels are completed
    private void loadNewLevel() {
        if (currentLevel > TOTAL_LEVELS) {
            Toast.makeText(this, "Game Completed! Well Done!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        levelIndicator.setText("Level " + currentLevel);
        userInput.setLength(0);

        GameObject selectedObject = getRandomObject();
        objectImage.setImageResource(selectedObject.getImageRes());
        currentWord = selectedObject.getName();

        setupLetterBoxes(currentWord.length());
        submitButton.setVisibility(View.GONE);
        feedbackIcon.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        speakerButton.setVisibility(View.GONE);
    }

    // Get a random object ensuring no repetition
    private GameObject getRandomObject() {
        List<GameObject> availableObjects = new ArrayList<>();
        for (GameObject obj : objectList) {
            if (!usedObjects.contains(obj.getId())) {
                availableObjects.add(obj);
            }
        }
        if (availableObjects.isEmpty()) {
            usedObjects.clear();
            availableObjects.addAll(objectList);
        }
        GameObject selected = availableObjects.get(new Random().nextInt(availableObjects.size()));
        usedObjects.add(selected.getId());
        return selected;
    }

    // Create letter boxes dynamically based on word length
    private void setupLetterBoxes(int count) {
        letterBoxesContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            TextView letterBox = new TextView(this);
            letterBox.setText("_");
            letterBox.setTextSize(24);
            letterBox.setPadding(10, 10, 10, 10);
            letterBoxesContainer.addView(letterBox);
        }
    }

    // Setup alphabet keyboard with correct size and spacing
    private void setupKeyboard() {
        alphabetKeyboard.removeAllViews();
        char[] letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray(); // Letters in order

        for (char letter : letters) {
            Button key = new Button(this);
            key.setText(String.valueOf(letter));
            key.setTextSize(16);  // Reduced text size
            key.setPadding(2, 2, 2, 2);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 172;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.setMargins(2, 2, 2, 2); // Small margins to fit properly
            key.setLayoutParams(params);// Reduced padding

            key.setOnClickListener(v -> onLetterPressed(letter));
            alphabetKeyboard.addView(key);
        }

        // Delete Key
        Button deleteKey = new Button(this);
        deleteKey.setText("DEL");
        deleteKey.setTextSize(16);
        deleteKey.setPadding(2,2, 2, 2);

        GridLayout.LayoutParams deleteParams = new GridLayout.LayoutParams();
        deleteParams.width = 170;
        deleteParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        deleteParams.setMargins(2, 2, 2, 2);
        deleteKey.setLayoutParams(deleteParams);

        deleteKey.setOnClickListener(v -> onDeletePressed());
        alphabetKeyboard.addView(deleteKey);
    }

    private void onLetterPressed(char letter) {
        if (userInput.length() < currentWord.length()) {
            userInput.append(letter);
            updateLetterBoxes();
            if (userInput.length() == currentWord.length()) {
                submitButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void onDeletePressed() {
        if (userInput.length() > 0) {
            userInput.deleteCharAt(userInput.length() - 1);
            updateLetterBoxes();
            submitButton.setVisibility(View.GONE);
        }
    }

    // Update letter boxes with the user's input
    private void updateLetterBoxes() {
        for (int i = 0; i < letterBoxesContainer.getChildCount(); i++) {
            TextView letterBox = (TextView) letterBoxesContainer.getChildAt(i);
            letterBox.setText(i < userInput.length() ? String.valueOf(userInput.charAt(i)) : "_");
        }
    }

    public void onSubmit() {
        boolean isCorrect = userInput.toString().equalsIgnoreCase(currentWord);

        if (isCorrect) {
            feedbackIcon.setImageResource(R.drawable.accept); // Correct Answer
            feedbackIcon.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.GONE);

            // Show speaker button and allow playback
            speakerButton.setVisibility(View.VISIBLE);

            saveGameProgress(true);
        } else {
            feedbackIcon.setImageResource(R.drawable.remove); // Incorrect Answer
            feedbackIcon.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show();
            speakerButton.setVisibility(View.GONE); // hide if wrong
            saveGameProgress(false);
        }

    }

    public void onNext() {
        if (currentLevel > TOTAL_LEVELS) {
            Toast.makeText(this, "Game Completed! Well Done!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        loadNewLevel(); // Move to the next level
    }

    // Save the user's game progress to Firestore
    private void saveGameProgress(boolean isCorrect) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to the 'game_progress/progress' document
        DocumentReference progressRef = db.collection("users")
                .document(userId)
                .collection("game_progress")
                .document("progress");

        // Create the attempt data {word, status}
        Map<String, Object> attempt = new HashMap<>();
        attempt.put("word", currentWord);
        attempt.put("status", isCorrect ? "correct" : "wrong");

        // Add attempt to the attempts array and update current_level
        progressRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Document exists, append to attempts array
                List<Map<String, Object>> attemptsList = (List<Map<String, Object>>) documentSnapshot.get("attempts");
                if (attemptsList == null) {
                    attemptsList = new ArrayList<>();
                }
                attemptsList.add(attempt);

                // Update document with new data
                Map<String, Object> gameData = new HashMap<>();
                gameData.put("current_level", currentLevel);
                gameData.put("attempts", attemptsList);

                progressRef.update(gameData);
            } else {
                // Create a new document with initial data
                List<Map<String, Object>> attemptsList = new ArrayList<>();
                attemptsList.add(attempt);

                Map<String, Object> gameData = new HashMap<>();
                gameData.put("current_level", currentLevel);
                gameData.put("attempts", attemptsList);

                progressRef.set(gameData);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });

        if(isCorrect)
            currentLevel++;
    }

}
