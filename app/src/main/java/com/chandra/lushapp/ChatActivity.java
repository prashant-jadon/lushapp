package com.chandra.lushapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler; // Import the Handler class
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chandra.lushapp.fragementsForMainScreen.InternetChatFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//TODO instead of runner use something else for timer
public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendMessageButton;
    private TextView otherUsernameTextView;
    private DatabaseReference chatRef;
    private FirebaseUser currentUser;
    private SharedPreferences sharedPreferences;
    private List<String> messageList;
    private ChatAdapter chatAdapter;
    private String otherUserName = null;
    private String roomId;
    private DatabaseReference chatRoomRef;

    private Handler timerHandler;
    private int countdownSeconds = 60; // Countdown time in seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sharedPreferences = getSharedPreferences("__lushapp__", MODE_PRIVATE);
        String currentUserId = sharedPreferences.getString("credential", "");
        roomId = getIntent().getStringExtra("roomId");
        chatRef = FirebaseDatabase.getInstance().getReference("chatrooms").child(roomId).child("messages");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendMessageButton = findViewById(R.id.sendMessageButton);
        otherUsernameTextView = findViewById(R.id.usernameofother);

        Button leaveChatButton = findViewById(R.id.nextChatButton);
        TextView timer = findViewById(R.id.timer); // Timer TextView

        leaveChatButton.setOnClickListener(v -> leaveChatRoom());

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        chatRoomRef = FirebaseDatabase.getInstance().getReference("chatrooms").child(roomId);


        // Add listener for chat room changes
        chatRoomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String user1Id = dataSnapshot.child("user1").getValue(String.class);
                    String user2Id = dataSnapshot.child("user2").getValue(String.class);

                    // Load user data for both users
                    if (user1Id != null) {
                        loadUserData(user1Id, otherUsernameTextView);
                    }
                    if (user2Id != null) {
                        loadUserData(user2Id, otherUsernameTextView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Error loading chat room data", Toast.LENGTH_SHORT).show();
            }
        });

        sendMessageButton.setOnClickListener(view -> {
            String message = messageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(message)) {
                sendMessage(message);
            }
        });

        loadMessages();
        if (!currentUserId.isEmpty()) {
            loadOtherUserName(currentUserId);
        }

        // Start the timer only after a user joins the chat
        startChatTimer(timer);
    }

    private void loadUserData(String userId, TextView usernameTextView) {
        FirebaseDatabase.getInstance().getReference("users").child(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.child("username").getValue(String.class);
                        String profileImageUrl = documentSnapshot.child("profileImage").getValue(String.class);

                        usernameTextView.setText(username);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void startChatTimer(TextView timerView) {
        timerHandler = new Handler();
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (countdownSeconds > 0) {
                    timerView.setText(String.valueOf(countdownSeconds));
                    if (countdownSeconds == 20) {
                        //showFriendRequestDialog(); // Show dialog at 5 seconds left
                        Toast.makeText(ChatActivity.this, "5 sec left", Toast.LENGTH_SHORT).show();
                    }
                    countdownSeconds--;
                    timerHandler.postDelayed(this, 1000); // Repeat every second
                } else {
                    leaveChatRoom();
                    startActivity(new Intent(ChatActivity.this, InternetChatFragment.class));
                }
            }
        }, 1000);
    }


    private void sendMessage(String message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("sender", currentUser.getUid());
        messageData.put("text", message);

        chatRef.push().setValue(messageData);
        messageInput.setText("");
    }

    private void loadOtherUserName(String currentUserId) {
        chatRef.orderByChild("sender").limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String otherUserId = snapshot.child("sender").getValue(String.class);
                    if (otherUserId != null && !otherUserId.equals(currentUserId)) {
                        // Load username and image URL from Firestore
                        loadUserDetailsFromFirestore(otherUserId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error fetching user ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserDetailsFromFirestore(String userId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String username = document.getString("username");
                            String profileImageUrl = document.getString("profilePicture");

                            // Update UI with username and profile image
                            otherUsernameTextView.setText(username);
                            // Load the profile image using an image loading library like Glide or Picasso
                            // Glide.with(this).load(profileImageUrl).into(profileImageView); // Add ImageView in your layout
                        }
                    } else {
                        Toast.makeText(ChatActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.child("text").getValue(String.class);
                    messageList.add(message);
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
                resetChatTimer();

                sharedPreferences = getSharedPreferences("__lushapp__", MODE_PRIVATE);
                String currentUserId = sharedPreferences.getString("credential", "");
                loadOtherUserName(currentUserId);
                // Reset the timer when new messages are loaded
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void resetChatTimer() {
        countdownSeconds = 60; // Reset countdown time to 60 seconds
        // Restart the timer UI
        timerHandler.removeCallbacksAndMessages(null); // Remove any existing callbacks
        startChatTimer((TextView) findViewById(R.id.timer)); // Restart the timer
    }

    @Override
    public void onBackPressed() {
        // Stop the timer
        super.onBackPressed();
        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
        }
        finish(); // Call finish to close this activity
    }

//    private void showFriendRequestDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Add as Friend");
//        builder.setMessage("Do you want to add " + otherUserName + " as your friend?");
//
//        builder.setPositiveButton("Yes", (dialog, which) -> {
//            addFriend( otherUserName);
//            addFriend(otherUserName);
//        });
//
//        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }

//    private void addFriend( String friendId) {
//        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
//        sharedPreferences = getSharedPreferences("__lushapp__", MODE_PRIVATE);
//        String currentUserId = sharedPreferences.getString("credential", "");
//        DocumentReference userDocRef = firestore.collection("users").document(currentUserId);
//
//        userDocRef.update("friends", FieldValue.arrayUnion(friendId))
//                .addOnSuccessListener(aVoid -> Toast.makeText(ChatActivity.this, "Friend added!", Toast.LENGTH_SHORT).show())
//                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Error adding friend", Toast.LENGTH_SHORT).show());
//    }




    private void leaveChatRoom() {
        DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("chatrooms").child(roomId);
        roomRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Navigate to InternetChatFragment
                Intent intent = new Intent(ChatActivity.this, InternetChatFragment.class);
                startActivity(intent);
                finish(); // Call finish to close this activity
            } else {
                Toast.makeText(ChatActivity.this, "Error leaving chat room", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
