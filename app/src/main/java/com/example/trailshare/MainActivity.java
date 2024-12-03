package com.example.trailshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;

    //private ConstraintLayout activity = findViewById(R.id.activity);

    Button signIn;

    TextView signUp, logIn, alreadyAcc, welcomeBack;

    ImageView logo;

    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    CheckBox rememberMe;

    SharedPreferences sharedPreferences;

    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        signIn = findViewById(R.id.sign_in);
        signUp = findViewById(R.id.sign_up);
        rememberMe = findViewById(R.id.remember_me);
        progressBar = findViewById(R.id.progressBar);
        logIn = findViewById(R.id.textView5);
        alreadyAcc = findViewById(R.id.textView3);
        logo = findViewById(R.id.logo);
        welcomeBack = findViewById(R.id.textView4);


        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        boolean remember = sharedPreferences.getBoolean("remember", false);
        rememberMe.setChecked(remember);

        if (remember) {
            String email = sharedPreferences.getString("email", "");
            String password = sharedPreferences.getString("password", "");

            if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                signIn(email, password);
            }
        }

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterPage.class);
                startActivity(intent);
                finish();
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(MainActivity.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(MainActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                signIn(email, password);
            }
        });

        rememberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("email");
                    editor.remove("password");
                    editor.apply();
                }
            }
        });
    }


    private void signIn(String email, String password) {

        findViewById(R.id.activity).setBackgroundResource(R.color.white);
        progressBar.setVisibility(View.VISIBLE); // Show the progress bar
        logo.setVisibility(View.VISIBLE);
        editTextEmail.setVisibility(View.INVISIBLE);
        editTextPassword.setVisibility(View.INVISIBLE);
        signIn.setVisibility(View.INVISIBLE);
        signUp.setVisibility(View.INVISIBLE);
        rememberMe.setVisibility(View.INVISIBLE);
        logIn.setVisibility(View.INVISIBLE);
        alreadyAcc.setVisibility(View.INVISIBLE);
        welcomeBack.setVisibility(View.INVISIBLE);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            //findViewById(R.id.activity).setBackgroundResource(R.drawable.screen); // Restore the original background

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("remember", rememberMe.isChecked());
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.apply();

                            Intent intent = new Intent(MainActivity.this, HomePage.class);
                            startActivity(intent);
//                            logo.setVisibility(View.INVISIBLE);
//                            findViewById(R.id.activity).setBackgroundResource(R.drawable.screen);
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                            findViewById(R.id.activity).setBackgroundResource(R.drawable.login);
                            progressBar.setVisibility(View.INVISIBLE); // Show the progress bar
                            logo.setVisibility(View.INVISIBLE);
                            editTextEmail.setVisibility(View.VISIBLE);
                            editTextPassword.setVisibility(View.VISIBLE);
                            signIn.setVisibility(View.VISIBLE);
                            signUp.setVisibility(View.VISIBLE);
                            rememberMe.setVisibility(View.VISIBLE);
                            logIn.setVisibility(View.VISIBLE);
                            alreadyAcc.setVisibility(View.VISIBLE);
                            welcomeBack.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }
}
