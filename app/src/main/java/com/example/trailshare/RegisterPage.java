package com.example.trailshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterPage extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword, editTextName;
    Button signUp;
    TextView signIn;

    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://trailshare-7a707-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextName = findViewById(R.id.name);
        signUp = findViewById(R.id.sign_up);
        signIn = findViewById(R.id.sign_in);

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterPage.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email, password, name;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());
                name = String.valueOf(editTextName.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(RegisterPage.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterPage.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(RegisterPage.this, "Enter Name", Toast.LENGTH_SHORT).show();
                    return;
                }

                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){

                                    FirebaseUser user = firebaseAuth.getCurrentUser();
                                    String userId = user.getUid();

                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("name", name);
                                    userData.put("email", email);

                                    databaseReference.child("Users").child(userId).setValue(userData)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(RegisterPage.this, "User profile created.", Toast.LENGTH_SHORT).show();
                                                    Log.d("DEBUG", "Data saved successfully");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(RegisterPage.this, "Failed to create user profile.", Toast.LENGTH_SHORT).show();
                                                    Log.d("DEBUG", "Data saving failed");
                                                }
                                            });

                                    Toast.makeText(RegisterPage.this,"Registration Successful",Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterPage.this,MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                                else {
                                    Toast.makeText(RegisterPage.this,"Authentication Failed",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                Log.d("DEBUG", "Name value: " + name);
            }
        });
    }
}
