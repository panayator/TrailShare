package com.example.trailshare;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;



import java.util.ArrayList;
import java.util.List;


public class FriendsActivity extends AppCompatActivity {
    private EditText search_bar;
    private Button search_button;
    Button scan_btn;
    Button show_btn;

    private RecyclerView friendsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private List<Friend> friendList;
    ImageView arrow_left;
    TextView noFriends;

    String friendUserId;



    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://trailshare-7a707-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);


        search_bar = findViewById(R.id.search_bar);
        search_button = findViewById(R.id.search_button);

        scan_btn = findViewById(R.id.scanner);
        show_btn = findViewById(R.id.show_qr);
        arrow_left = findViewById(R.id.arrow_left);
        noFriends = findViewById(R.id.noFriends);
        arrow_left.setVisibility(View.INVISIBLE);
        noFriends.setVisibility(View.INVISIBLE);
        //ImageView imageView = findViewById(R.id.qr_code);

        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        friendList = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friendList);
        friendsRecyclerView.setAdapter(friendsAdapter);


        //QR RELATED
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator intentIntegrator = new IntentIntegrator(FriendsActivity.this);
                intentIntegrator.setPrompt("Scan a QR Code");
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                intentIntegrator.setCameraId(0); // Set the camera to rear-facing
                intentIntegrator.setOrientationLocked(false); // Allow orientation changes
                //intentIntegrator.setBeepEnabled(true); // Enable beep sound on successful scan
                intentIntegrator.setCaptureActivity(CustomCaptureActivity.class); // Custom capture activity to force portrait mode
                intentIntegrator.initiateScan();
            }
        });


        show_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = firebaseAuth.getCurrentUser().getEmail();
                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                try {
                    BitMatrix bitMatrix = multiFormatWriter.encode(email, BarcodeFormat.QR_CODE, 300, 300);
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                    showQRCodePopup(bitmap);
                } catch (WriterException e) {
                    throw new RuntimeException(e);
                }
            }
        });




        ImageButton backButton = findViewById(R.id.backButtonFriends);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Back Button
                onBackPressed();
            }
        });

        // Add a click listener to each item in the RecyclerView
        friendsAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Friend friend = friendList.get(position);
                String friendName = friend.getName();
                String friendId = friend.getId();
                showFriendDetailsPopup(friendName, friendId);
            }
        });

        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailToSearch = search_bar.getText().toString().trim();

                Query query = databaseReference.child("Users").orderByChild("email").equalTo(emailToSearch);

                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                String userId = childSnapshot.getKey();

                                // Add the friend's user ID to the current user's "Friends" node
                                databaseReference.child("Users").child(currentUser.getUid()).child("Friends").child(userId).setValue(true);


                                // Add the current user's user ID to the friend's "Friends" node
                                databaseReference.child("Users").child(userId).child("Friends").child(currentUser.getUid()).setValue(true);
                                Toast.makeText(FriendsActivity.this, "User found and added to Friends list.", Toast.LENGTH_SHORT).show();



                            }
                           refreshFriendsList();
                            arrow_left.setVisibility(View.INVISIBLE);
                            noFriends.setVisibility(View.INVISIBLE);

                        } else {
                            Toast.makeText(FriendsActivity.this, "No user found with that email.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(FriendsActivity.this, "Error searching for user: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Load friend data from Firebase
        loadFriendsData();

    }
    private void showQRCodePopup(Bitmap qrBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);
        ImageView imageView = dialogView.findViewById(R.id.qr_code_image);
        imageView.setImageBitmap(qrBitmap);
        builder.setView(dialogView);
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void loadFriendsData() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference friendsRef = databaseReference.child("Users").child(uid).child("Friends");
            friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        friendList.clear();
                        List<String> friendIds = new ArrayList<>();
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            String friendId = childSnapshot.getKey();
                            friendIds.add(friendId);
                        }
                        DatabaseReference usersRef = databaseReference.child("Users");
                        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (String friendId : friendIds) {
                                    if (dataSnapshot.hasChild(friendId)) {
                                        DataSnapshot friendSnapshot = dataSnapshot.child(friendId);
                                        String friendName = friendSnapshot.child("name").getValue(String.class);
                                        if (friendName != null) {
                                            Friend friend = new Friend(friendName, friendId);
                                            friendList.add(friend);
                                        }
                                    }
                                }
                                friendsAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    } else {
                        NoFriendsMessage();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }






    private void showFriendDetailsPopup(String friendName, String friendId) {
        // Retrieve the friend's email from Firebase using the friendId
        DatabaseReference friendEmailRef = databaseReference.child("Users").child(friendId).child("email");
        friendEmailRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String friendEmail = dataSnapshot.getValue(String.class);

                // What to show in the popup
                String popupContent = "Name: " + friendName + "\nEmail: " + friendEmail;

                AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
                builder.setTitle("Friend Details");
                builder.setMessage(popupContent);
                builder.setCancelable(true);
                builder.create().show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void NoFriendsMessage(){
        noFriends.setVisibility(View.VISIBLE);
        arrow_left.setVisibility(View.VISIBLE);
        // Set the initial position of the arrow
        arrow_left.setY(0f);

        // animation properties
        float startY = 0f; // Starting position (top of the screen)
        float endY = 100f; // Ending position (100 pixels below the starting position)
        long duration = 500; // Duration of the animation in milliseconds

        // Create the ObjectAnimator for the Y property of the ImageView
        ObjectAnimator animator = ObjectAnimator.ofFloat(arrow_left, "translationX", startY, endY);
        animator.setRepeatCount(ObjectAnimator.INFINITE); // Repeat the animation indefinitely
        animator.setRepeatMode(ObjectAnimator.REVERSE); // Reverse the animation after each iteration
        animator.setDuration(duration); // Set the duration of the animation

        // Start the animation
        animator.start();

    }





    private void refreshFriendsList() {
        friendList.clear(); // Clear the existing friendList
        loadFriendsData(); // Load friend data from Firebase again
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            String contents = intentResult.getContents();
            if (contents != null) {
                search_bar.setText(intentResult.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showRemoveFriendDialog(Context context, Friend friend) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String friendName = friend.getName();
        String friendId = friend.getId();
        builder.setTitle("Remove Friend");
        builder.setMessage("Are you sure you want to remove " + friendName + " from your friends?");
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Remove the friend from the Firebase Realtime Database
                String currentUserUid = firebaseAuth.getCurrentUser().getUid();
                DatabaseReference currentUserRef = databaseReference.child("Users").child(currentUserUid).child("Friends").child(friendId);
                currentUserRef.removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        friendsAdapter.removeFriend(friendId); // Remove friend item from the adapter
                        Toast.makeText(FriendsActivity.this, "Friend removed successfully", Toast.LENGTH_SHORT).show();
                        // Handle the case when there are no friends left
                        if (friendList.isEmpty()) {
                            NoFriendsMessage();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(FriendsActivity.this, "Failed to remove friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                DatabaseReference friendUserRef = databaseReference.child("Users").child(friendId).child("Friends").child(currentUserUid);
                friendUserRef.removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                friendsAdapter.removeFriend(friendId); // Remove friend item from the adapter
                                Toast.makeText(FriendsActivity.this, "Friend removed successfully", Toast.LENGTH_SHORT).show();
                                // Handle the case when there are no friends left
                                if (friendList.size() == 0) {
                                    //NoFriendsMessage();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(FriendsActivity.this, "Failed to remove friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                // Handle the case when there are no friends left
//                if (friendList.size() == 0) {
//                    NoFriendsMessage();
//                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }



    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    // RecyclerView Adapter
    public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

        private List<Friend> friendList;



        private OnItemClickListener listener;

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        public FriendsAdapter(List<Friend> friendList) {
            this.friendList = friendList;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView friendNameTextView;
            public ImageButton removeFriendButton;

            public ViewHolder(View itemView, final OnItemClickListener listener) {
                super(itemView);
                friendNameTextView = itemView.findViewById(R.id.friendNameText);
                removeFriendButton = itemView.findViewById(R.id.btnRemoveFriend);


                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                listener.onItemClick(position);
                            }
                        }
                    }
                });
            }
        }


        public void removeFriend(String friendId) {
            for (int i = 0; i < friendList.size(); i++) {
                Friend friend = friendList.get(i);
                if (friend.getId().equals(friendId)) {
                    friendList.remove(i);
                    notifyItemRemoved(i);
                    notifyItemRangeChanged(i, friendList.size());
                    break;
                }
            }
        }







        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            return new ViewHolder(view, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Friend friendName = friendList.get(position);
            holder.friendNameTextView.setText(friendName.getName());

            holder.removeFriendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRemoveFriendDialog(v.getContext(), friendName);
                }
            });
        }


        @Override
        public int getItemCount() {
            return friendList.size();
        }

    }





    public class Friend {
        private String name;
        private String id;

        public Friend(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }
    }

}
