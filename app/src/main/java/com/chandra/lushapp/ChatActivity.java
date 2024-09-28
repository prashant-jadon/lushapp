package com.chandra.lushapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler; // Import the Handler class
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private void startChatTimer(TextView timerView) {
        timerHandler = new Handler();
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (countdownSeconds > 0) {
                    timerView.setText(String.valueOf(countdownSeconds));
                    if (countdownSeconds == 5) {
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
                        otherUsernameTextView.setText(otherUserId); // Set user ID instead of user name
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error fetching user ID", Toast.LENGTH_SHORT).show();
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
                resetChatTimer(); // Reset the timer when new messages are loaded
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

        // Instead of calling super.onBackPressed(), transition to InternetChatFragment
        Intent intent = new Intent(ChatActivity.this, InternetChatFragment.class);
        startActivity(intent);
        finish(); // Call finish to close this activity
    }


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
