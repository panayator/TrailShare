package com.example.trailshare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.firebase.auth.FirebaseAuth;

import java.nio.file.Paths;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        //FRIENDS BUTTON
        Button friendsButton = findViewById(R.id.friends);
        friendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the FriendsActivity
                Intent intent = new Intent(MenuActivity.this, FriendsActivity.class);
                startActivity(intent);
            }
        });


        //PATHS BUTTON
        Button pathsButton = findViewById(R.id.paths);
        pathsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the FriendsActivity
                Intent intent = new Intent(MenuActivity.this, PathsActivity.class);
                startActivity(intent);
            }
        });

        //LOGOUT BUTTON
        Button logoutButton;
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        // Set up the logout button
        logoutButton = findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        ImageButton backButton = findViewById(R.id.backButtonMenu);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Back Button
                onBackPressed();
            }
        });




    }
}