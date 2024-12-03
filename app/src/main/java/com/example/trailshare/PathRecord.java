package com.example.trailshare;
import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.trailshare.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class PathRecord extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_PERMISSION_CODE = 1;

    private static final int DEFAULT_ZOOM = 20;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private List<Location> recordedLocations;

    private GoogleMap googleMap;
    private Polyline routePolyline;

    private ImageButton recordButton;
    private boolean isRecording = false;

    private DatabaseReference mDatabaseRef;

    private String mPathId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_record);
        mDatabaseRef = FirebaseDatabase.getInstance("https://trailshare-7a707-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // Initialize the FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Create an empty list to store recorded locations
        recordedLocations = new ArrayList<>();

        // Request necessary permissions
        requestPermissions();
        // Set up the map fragment

        ConstraintLayout mapContainer = findViewById(R.id.map);
        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction().add(mapContainer.getId(), mapFragment).commit();
        mapFragment.getMapAsync(this);
        Log.d("DEBUG", "Set Up the map fragment");
        // Obtain the SupportMapFragment and get notified when the map is ready to be used
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up the record button
        recordButton = findViewById(R.id.startRec);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    // Start recording
                    startRecording();
                } else {
                    // Stop recording and show path name dialog
                    stopRecording();
                    showPathNameDialog();
                }
            }
        });

        ImageButton backButton = findViewById(R.id.backButtonRecord);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the result data to indicate success
                setResult(Activity.RESULT_OK);
                // Finish the activity and go back to the previous activity
                finish();
                // Back Button
                //onBackPressed();
//                Intent intent = new Intent(PathRecord.this, PathsActivity.class);
//                startActivity(intent);
            }
        });

    }

    private void requestPermissions() {
        // Check if the required permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_CODE);
        }
    }

    private void setupLocationUpdates() {
        // Check if the location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, handle accordingly
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a location request with desired parameters
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // Update interval in milliseconds

        // Create a location callback to handle updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // Process location updates
                for (Location location : locationResult.getLocations()) {
                    // Add location to recorded locations
                    recordedLocations.add(location);

                    // Update the route on the map
                    if (googleMap != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updateRouteOnMap(latLng);
                    }
                }
            }
        };

        // Request location updates
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    private void updateRouteOnMap(LatLng latLng) {
        if (routePolyline == null) {
            // Create a new polyline if it doesn't exist
            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.RED)
                    .width(5f)
                    .add(latLng);
            routePolyline = googleMap.addPolyline(polylineOptions);
        } else {
            // Add the new point to the existing polyline
            List<LatLng> points = routePolyline.getPoints();
            points.add(latLng);
            routePolyline.setPoints(points);
        }

        // Move the camera to the latest location
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void clearRouteOnMap() {
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }
    }

    private void startRecording() {
        isRecording = true;
        recordButton.setBackgroundResource(android.R.drawable.ic_menu_save);

        // Clear previous recorded locations (needs to be here so that the path is saved properly)
        recordedLocations.clear();
        //clearRouteOnMap();

        // Start location updates
        setupLocationUpdates();
    }

    private void stopRecording() {
        isRecording = false;
        recordButton.setBackgroundResource(android.R.drawable.ic_menu_mylocation);

        // Stop location updates
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        // Clear previous recorded path on screen (clears right after saving)
        clearRouteOnMap();
    }

    private void showPathNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Path Name");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pathName = input.getText().toString().trim();
                saveRoute(pathName); // Pass the path name to the saveRoute() method
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Show the dialog
        builder.show();
    }


    private void saveRoute(String pathName) {
        // Get the current user's ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Create a new child node for the current user in the "Paths" node with a unique identifier
        String pathId = mDatabaseRef.child("Users").child(userId).child("Paths").push().getKey();
        DatabaseReference pathRef = mDatabaseRef.child("Users").child(userId).child("Paths").child(pathId);

        // Save the path name
        pathRef.child("PathName").setValue(pathName);

        for (Location location : recordedLocations) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            System.out.println("Latitude: " + latitude + ", Longitude: " + longitude);

            String pointId = pathRef.child("Points").push().getKey();

            // Save the latitude and longitude values under the point node
            pathRef.child("Points").child(pointId).child("Latitude").setValue(latitude);
            pathRef.child("Points").child(pointId).child("Longitude").setValue(longitude);
        }


    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start location updates if recording is in progress
                if (isRecording) {
                    startRecording();
                }
            } else {
                // Permission denied, show a toast or handle accordingly
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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
                    }
                }
            });
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Check if location updates are being requested and locationCallback is initialized
        if (isRecording && locationCallback != null) {
            // Stop location updates
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

}

