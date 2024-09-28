package com.chandra.lushapp.fragementsForMainScreen;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.chandra.lushapp.ChatActivity;
import com.chandra.lushapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class InternetChatFragment extends Fragment {

    Button startChatting;
    FirebaseAuth mAuth;
    SharedPreferences sharedPreferences;

    // Variables for handling the timer
    private Handler handler;
    private Runnable removeChatRoomRunnable;
    private DatabaseReference chatroomsRef;
    private String currentRoomId;
    private boolean userJoined = false; // Flag to check if user has joined

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_internet_chat, container, false);

        startChatting = view.findViewById(R.id.startChatting);
        sharedPreferences = getActivity().getSharedPreferences("__lushapp__", MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "");

        // Initialize Firebase Auth and Database Reference here
        mAuth = FirebaseAuth.getInstance();
        chatroomsRef = FirebaseDatabase.getInstance().getReference("chatrooms");

        startChatting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user = mAuth.getCurrentUser();
                String userId = user != null ? user.getUid() : null;

                if (userId != null) {
                    findOrCreateRoom(userId);
                } else {
                    Toast.makeText(getContext(), "Please log in to start chatting.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }

    private void findOrCreateRoom(String userId) {
        if (chatroomsRef == null) {
            Toast.makeText(getContext(), "Chatroom reference is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        chatroomsRef.orderByChild("user2").equalTo(null).limitToFirst(1)
                .get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Room exists, add user as user2
                            for (DataSnapshot room : snapshot.getChildren()) {
                                currentRoomId = room.getKey();
                                String user1Id = room.child("user1").getValue(String.class);
                                room.getRef().child("user2").setValue(userId);
                                userJoined = true; // User has joined
                                Toast.makeText(getContext(), "Connected to user: " + user1Id, Toast.LENGTH_SHORT).show();
                                navigateToChat(currentRoomId); // Open ChatActivity
                                break;
                            }
                        } else {
                            // No available rooms, create a new room
                            DatabaseReference newRoomRef = chatroomsRef.push();
                            newRoomRef.child("user1").setValue(userId);
                            newRoomRef.child("user2").setValue(null);
                            currentRoomId = newRoomRef.getKey();
                            navigateToChat(currentRoomId); // Open ChatActivity
                        }
                        startRemoveChatRoomTimer(); // Start the timer after trying to find or create a room
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Please try again", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startRemoveChatRoomTimer() {
        // Cancel any existing timer
        if (handler != null) {
            handler.removeCallbacks(removeChatRoomRunnable);
        }

        handler = new Handler();
        removeChatRoomRunnable = new Runnable() {
            @Override
            public void run() {
                if (!userJoined) {
                    if (currentRoomId != null) {
                        chatroomsRef.child(currentRoomId).removeValue();
                        currentRoomId = null;
                    }
                    String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                    if (userId != null) {
                        findOrCreateRoom(userId);
                    }
                }
            }
        };
        handler.postDelayed(removeChatRoomRunnable, 60000);
    }

    private void navigateToChat(String roomId) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("roomId", roomId);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity().getIntent().getBooleanExtra("restartChat", false)) {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("__lushapp__", Context.MODE_PRIVATE);
            String userId = sharedPreferences.getString("credential", "");
            if (!userId.isEmpty()) {
                findOrCreateRoom(userId); // Restart finding or creating a room
            }
            getActivity().getIntent().removeExtra("restartChat"); // Clear the flag
        }
    }
}
