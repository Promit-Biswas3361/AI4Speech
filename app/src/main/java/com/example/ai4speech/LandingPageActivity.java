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

import com.google.firebase.auth.FirebaseAuth;

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
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(LandingPageActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LandingPageActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
            startActivity(intent);
            finish();
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

    public void openTherapistsPage(View view) {
        Intent intent = new Intent(LandingPageActivity.this, MapActivity.class);
        startActivity(intent);
    }
}
