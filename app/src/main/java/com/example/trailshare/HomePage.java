package com.example.trailshare;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;



public class HomePage extends AppCompatActivity implements OnMapReadyCallback {

    private TextView allTimeStepsTextView;
    private Integer steps = 0;

    // Sensor variables
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private double MagnitudePrevious = 0;
    private GoogleMap mMap;
    private static final int DEFAULT_ZOOM = 15;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private TextView textViewUserName;

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://trailshare-7a707-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Set up the welcome message
        textViewUserName = findViewById(R.id.username);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            databaseReference.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        if (name != null) {
                            Log.d("HomePage", "Name retrieved: " + name);
                            textViewUserName.setTypeface(null, Typeface.BOLD);
                            textViewUserName.setText("Welcome, " + name);
                        } else {
                            Log.d("HomePage", "Name is null");
                        }
                    } else {
                        Log.d("HomePage", "Data snapshot does not exist");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("HomePage", "Error retrieving data from database: " + databaseError.getMessage());
                }
            });
        } else {
            Log.d("HomePage", "User is not logged in");
        }

        //Set up the Menu button
        ImageButton menuButton = findViewById(R.id.menu);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the MenuActivity
                Intent intent = new Intent(HomePage.this, MenuActivity.class);
                startActivity(intent);
            }
        });


        // Set up the map fragment
        ConstraintLayout mapContainer = findViewById(R.id.map_container);
        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction().add(mapContainer.getId(), mapFragment).commit();
        mapFragment.getMapAsync(this);
        Log.d("DEBUG", "Set up the map fragment");

        // Set up the step counter
        allTimeStepsTextView = findViewById(R.id.all_time_steps);

        // Initialize sensor variables
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Retrieve the all-time steps count from SharedPreferences
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        steps = sharedPreferences.getInt("stepCount", 0);

        // Display the all-time steps count
        allTimeStepsTextView.setText(steps.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(stepDetector, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveStepCountToSharedPreferences();
        sensorManager.unregisterListener(stepDetector);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveStepCountToSharedPreferences();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("DEBUG", "Inside onMapReady");
        // Check for permission to access the user's location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, get the user's location and update the map
            mMap.setMyLocationEnabled(true);
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                    }
                }
            });
        } else {
            // Permission not granted, request it from the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, get the user's location and update the map
                    mMap.setMyLocationEnabled(true);
                    FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                            }
                        }
                    });
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void saveStepCountToSharedPreferences() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("stepCount", steps);
        editor.apply();
    }

    SensorEventListener stepDetector = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent != null) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                double Magnitude = Math.sqrt(x * x + y * y + z * z);
                double MagnitudeDelta = Magnitude - MagnitudePrevious;
                MagnitudePrevious = Magnitude;
                // Sensitivity changes here
                if (MagnitudeDelta > 8) {
                    steps++;
                    allTimeStepsTextView.setText(steps.toString()); // Update the displayed step count
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
}

