package com.example.ai4speech;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if(checkUserSession()){
            return;
        }
        setContentView(R.layout.activity_main);

//        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
//        boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);
//
//        if (isLoggedIn) {
//            startActivity(new Intent(this, LandingPageActivity.class));
//            finish();
//        } else {
//            setContentView(R.layout.activity_main);
//        }

        Button getStartedButton = findViewById(R.id.getStartedButton);
        Button loginButton = findViewById(R.id.loginButton);

        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean checkUserSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, go to Landing Page
            goToLandingPage();
            return true;
        }
        return false;
    }

    private void goToLandingPage() {
        Intent intent = new Intent(MainActivity.this, LandingPageActivity.class);
        startActivity(intent);
        finish(); // Prevent returning to login after navigating to Landing Page
    }
}
