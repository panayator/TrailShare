package com.example.trailshare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DisplayPath extends AppCompatActivity implements OnMapReadyCallback {

    private Chronometer chronometer;
    private Button startStopButton;
    private Button resetButton;

    private boolean isRunning = false;
    private long pauseOffset;
    private GoogleMap googleMap;
    private DatabaseReference mDatabaseRef;

    private static final int DEFAULT_ZOOM = 20;

    final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_path);

        mDatabaseRef = FirebaseDatabase.getInstance("https://trailshare-7a707-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        ConstraintLayout mapContainer = findViewById(R.id.map);
        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction().add(mapContainer.getId(), mapFragment).commit();
        mapFragment.getMapAsync(this);
        Log.d("DisplayPathDEBUG", "Set Up the map fragment");

        ImageButton backButton = findViewById(R.id.backButtonDisplay);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Back Button
                onBackPressed();
            }
        });

        chronometer = findViewById(R.id.chronometer);
        startStopButton = findViewById(R.id.start_stop_button);
        resetButton = findViewById(R.id.reset_button);

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    chronometer.stop();
                    pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
                    startStopButton.setText("Start");
                    Drawable timer_on = ContextCompat.getDrawable(DisplayPath.this, R.drawable.timer_on);
                    startStopButton.setCompoundDrawablesWithIntrinsicBounds(null, null, timer_on, null);
                } else {
                    chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                    chronometer.start();
                    startStopButton.setText("Stop");
                    Drawable timer_off = ContextCompat.getDrawable(DisplayPath.this, R.drawable.timer_off);
                    startStopButton.setCompoundDrawablesWithIntrinsicBounds(null, null, timer_off, null);
                }
                isRunning = !isRunning;
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                pauseOffset = 0;
                startStopButton.setText("Start");
                isRunning = false;
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        // Enable the user's location on the map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                        Log.d("DisplayPathDEBUG", "GOT MY LOCATION");

                        // Get the path ID or any other identifier for the selected path from the intent
                        String pathId = getIntent().getStringExtra("pathId");
                        Log.d("DisplayPathDEBUG", "RECEIVED PATH ID IS" + pathId);

                        // Retrieve the path details from the Firebase database
                        DatabaseReference pathRef = mDatabaseRef.child("Users").child(userId).child("Paths").child(pathId).child("Points");
                        pathRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Log.d("DisplayPathDEBUG", "THE SNAPSHOT EXISTS");
                                    List<LatLng> pathCoordinates = new ArrayList<>();
                                    for (DataSnapshot pointSnapshot : snapshot.getChildren()) {
                                        String pointId = pointSnapshot.getKey();
                                        DatabaseReference coordinateRef = pathRef.child(pointId);
                                        coordinateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot coordinateSnapshot) {
                                                if (coordinateSnapshot.exists()) {

                                                    double latitude = coordinateSnapshot.child("Latitude").getValue(Double.class);
                                                    double longitude = coordinateSnapshot.child("Longitude").getValue(Double.class);
                                                    LatLng coordinate = new LatLng(latitude, longitude);
                                                    pathCoordinates.add(coordinate);

                                                    // Display the path on the map
                                                    PolylineOptions polylineOptions = new PolylineOptions()
                                                            .addAll(pathCoordinates)
                                                            .width(5)
                                                            .color(Color.RED);
                                                    googleMap.addPolyline(polylineOptions);

                                                    // Move the camera to fit the entire path within view
                                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                    for (LatLng pathCoordinate : pathCoordinates) {
                                                        builder.include(pathCoordinate);
                                                    }
                                                    LatLngBounds bounds = builder.build();
                                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100)); // Adjust the padding as needed


                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("DisplayPathDEBUG", "Failed to read coordinate data", error.toException());
                                            }
                                        });
                                    }

                                    Log.d("DisplayPathDEBUG", "THE COORDINATE SNAPSHOT EXISTS");
                                    Log.d("DisplayPathDEBUG", "PATH DRAWN ON MAP");
                                    Log.d("DisplayPathDEBUG", "CAMERA FIT TO SHOW THE ENTIRE PATH");



                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("DisplayPathDEBUG", "Failed to read path data", error.toException());
                            }
                        });
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
}
