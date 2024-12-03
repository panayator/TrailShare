package com.example.trailshare;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PathsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SavedPathAdapter adapter;

    private static final int REQUEST_RECORD_PATH = 1;

    private DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance("https://trailshare-7a707-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

    final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    ImageView arrow_up;

    TextView noPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paths);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        arrow_up = findViewById(R.id.arrow_up);
        noPaths = findViewById(R.id.noPaths);

        DatabaseReference pathRef = mDatabaseRef.child("Users").child(userId).child("Paths");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SavedPathAdapter();
        recyclerView.setAdapter(adapter);

        pathRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<PathItem> pathItems = new ArrayList<>();
                    for (DataSnapshot pathSnapshot : snapshot.getChildren()) {
                        String pathId = pathSnapshot.getKey();
                        pathRef.child(pathId).child("PathName").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    String pathName = dataSnapshot.getValue(String.class);
                                    PathItem pathItem = new PathItem(pathId, pathName);
                                    pathItems.add(pathItem);
                                    adapter.setPathItems(pathItems);
                                    adapter.notifyDataSetChanged();
                                    Log.d("PathsActivity", "Path Name: " + pathName);
                                    Log.d("PathsActivity", "Path Snapshot: " + pathSnapshot.getValue());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.d("PathsActivity", "Error retrieving path name: " + databaseError.getMessage());
                            }
                        });
                    }
                } else {
                    NoPathsMessage();
                    Log.d("PathsActivity", "No paths found for user");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("PathsActivity", "Error retrieving path list: " + error.getMessage());
            }
        });


        ImageButton pathRec = findViewById(R.id.pathRec);
        pathRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the PathRecord Activity
                Intent intent = new Intent(PathsActivity.this, PathRecord.class);
                startActivityForResult(intent, REQUEST_RECORD_PATH);
//                // Launch the RecordPath activity
//                Intent intent = new Intent(PathsActivity.this, PathRecord.class);
//                startActivity(intent);
            }
        });

        ImageButton backButton = findViewById(R.id.backButtonPaths);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Back Button
                onBackPressed();
            }
        });

    }

    public void NoPathsMessage(){
        noPaths.setVisibility(View.VISIBLE);
        arrow_up.setVisibility(View.VISIBLE);
        // Set the initial position of the arrow
        arrow_up.setY(0f);

        // Define the animation properties
        float startY = 0f; // Starting position (top of the screen)
        float endY = 100f; // Ending position (100 pixels below the starting position)
        long duration = 500; // Duration of the animation in milliseconds

        // Create the ObjectAnimator for the Y property of the ImageView
        ObjectAnimator animator = ObjectAnimator.ofFloat(arrow_up, "translationY", startY, endY);
        animator.setRepeatCount(ObjectAnimator.INFINITE); // Repeat the animation indefinitely
        animator.setRepeatMode(ObjectAnimator.REVERSE); // Reverse the animation after each iteration
        animator.setDuration(duration); // Set the duration of the animation

        // Start the animation
        animator.start();

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_RECORD_PATH) {
            if (resultCode == Activity.RESULT_OK) {
                // Refresh the paths list here
                // You can call a method to update the list or re-fetch the paths from the database
                refreshPathsList();
            }
        }
    }

    private void refreshPathsList() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }



    public class PathItem {
        private String pathId;
        private String pathName;

        public PathItem(String pathId, String pathName) {
            this.pathId = pathId;
            this.pathName = pathName;
        }

        public String getPathId() {
            return pathId;
        }

        public String getPathName() {
            return pathName;
        }
    }


    // Inner class for the adapter
    class SavedPathAdapter extends RecyclerView.Adapter<SavedPathAdapter.SavedPathViewHolder> {
        private List<PathItem> pathItems;

        public SavedPathAdapter() {
            this.pathItems = new ArrayList<>();
        }

        public void setPathItems(List<PathItem> pathItems) {
            this.pathItems = pathItems;
            notifyDataSetChanged();
        }

        public void removePathItem(String pathId) {
            for (int i = 0; i < pathItems.size(); i++) {
                PathItem pathItem = pathItems.get(i);
                if (pathItem.getPathId().equals(pathId)) {
                    pathItems.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
            if (pathItems.size() == 0){
                NoPathsMessage();
            }
        }

        @NonNull
        @Override
        public SavedPathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saved_path, parent, false);
            return new SavedPathViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SavedPathViewHolder holder, int position) {
            PathItem pathItem = pathItems.get(position);
            String pathName = pathItem.getPathName();
            holder.txtSavedPathName.setText("\u2022 " + pathName);

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteConfirmationDialog(v.getContext(), pathItem.getPathId());
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Launch the DisplayPath activity and pass the path ID
                    Intent intent = new Intent(v.getContext(), DisplayPath.class);
                    intent.putExtra("pathId", pathItem.getPathId());
                    startActivity(intent);
                }
            });
        }




        @Override
        public int getItemCount() {
            return pathItems.size();
        }

        class SavedPathViewHolder extends RecyclerView.ViewHolder {
            TextView txtSavedPathName;
            ImageButton btnDelete;

            SavedPathViewHolder(@NonNull View itemView) {
                super(itemView);
                txtSavedPathName = itemView.findViewById(R.id.txtSavedPathName);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    private void showDeleteConfirmationDialog(Context context, String pathId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Deleting Path");
        builder.setMessage("Are you sure you want to delete this path? It will be gone permanently.");
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Delete the path from the database
                DatabaseReference pathRef = mDatabaseRef.child("Users").child(userId).child("Paths").child(pathId);
                pathRef.removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                adapter.removePathItem(pathId); // Remove path item from the adapter
                                Toast.makeText(PathsActivity.this, "Path deleted successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(PathsActivity.this, "Failed to delete path: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }



}
