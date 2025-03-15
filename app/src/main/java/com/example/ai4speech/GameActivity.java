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
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView levelIndicator;
    private ImageView objectImage, feedbackIcon;
    private LinearLayout letterBoxesContainer;
    private GridLayout alphabetKeyboard;
    private Button submitButton, nextButton;
    private ImageButton logoutButton, profileButton;
    private List<GameObject> objectList;
    private HashSet<Integer> usedObjects = new HashSet<>();
    private String currentWord = "";
    private StringBuilder userInput = new StringBuilder();
    private int currentLevel = 1;
    private final int TOTAL_LEVELS = 15;

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

        // Handle logout button click
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
        loadNewLevel();

        submitButton.setOnClickListener(v -> onSubmit(v));
        nextButton.setOnClickListener(v -> onNext(v));
    }

    private void loadNewLevel() {
        if (currentLevel > TOTAL_LEVELS) {
            Toast.makeText(this, "Game Completed!", Toast.LENGTH_LONG).show();
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
    }

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

    private void updateLetterBoxes() {
        for (int i = 0; i < letterBoxesContainer.getChildCount(); i++) {
            TextView letterBox = (TextView) letterBoxesContainer.getChildAt(i);
            letterBox.setText(i < userInput.length() ? String.valueOf(userInput.charAt(i)) : "_");
        }
    }

    public void onSubmit(View view) {
        if (userInput.toString().equals(currentWord)) {
            feedbackIcon.setImageResource(R.drawable.accept); // Correct Answer
            feedbackIcon.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.GONE); // Hide submit button on correct answer
        } else {
            feedbackIcon.setImageResource(R.drawable.remove); // Incorrect Answer
            feedbackIcon.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onNext(View view) {
        currentLevel++;
        if (currentLevel > TOTAL_LEVELS) {
            Toast.makeText(this, "Game Completed! Well Done!", Toast.LENGTH_LONG).show();
            finish(); // End game
            return;
        }
        loadNewLevel();
    }
}
