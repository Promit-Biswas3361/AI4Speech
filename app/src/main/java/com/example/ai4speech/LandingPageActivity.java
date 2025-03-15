package com.example.ai4speech;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

public class LandingPageActivity extends AppCompatActivity {

    private ImageButton profileButton, logoutButton;
    private Button gameButton, speechTestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);
        gameButton = findViewById(R.id.gameButton);
        speechTestButton = findViewById(R.id.speechTestButton);

        // Navigate to Profile Page
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LandingPageActivity.this, ProfileActivity.class));
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

                Toast.makeText(LandingPageActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LandingPageActivity.this, MainActivity.class));
                finish();
            }
        });

        // Navigate to Game Feature
        gameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LandingPageActivity.this, GameActivity.class));
            }
        });

        // Navigate to Speech Test Feature
        speechTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LandingPageActivity.this, SpeechTestActivity.class));
            }
        });
    }
}
