package com.chandra.lushapp.authentication.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chandra.lushapp.MainActivity;
import com.chandra.lushapp.R;
import com.chandra.lushapp.profileCreation.controller.ProfileCreationActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    FirebaseFirestore db;

    private static final int RC_SIGN_IN = 9001;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("__lushapp__",MODE_PRIVATE);
        String credential =  sharedPreferences.getString("credential","");
        if(!credential.isEmpty()){
            startActivity(new Intent(AuthActivity.this,MainActivity.class));
            finish();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        Button login = findViewById(R.id.loginUsingGoogle);
        TextView termsAndCondition = findViewById(R.id.termsAndConditions);

        termsAndCondition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AuthActivity.this, "Terms and cond", Toast.LENGTH_SHORT).show();
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Toast.makeText(this, "Sign-In Failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String username = documentSnapshot.getString("username");
                                            if(!username.isEmpty()){
                                                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                                                finish();
                                            }else {
                                                Map<String, Object> data = new HashMap<>();
                                                data.put("username", "");
                                                data.put("userId", userId);
                                                data.put("gender", "");
                                                data.put("profilePicture", "");
                                                data.put("profileLookingPictures", new ArrayList<String>());
                                                data.put("isVerified", false);
                                                data.put("bio", "");
                                                data.put("email", acct.getEmail());
                                                data.put("friends", new ArrayList<>());
                                                data.put("rating", 100);
                                                data.put("timeLeft", 40);
                                                data.put("interest", "");
                                                data.put("questionAnswered", new ArrayList<>());

                                                db.collection("users").document(userId).set(data)
                                                        .addOnSuccessListener(unused -> {
                                                            Toast.makeText(AuthActivity.this, "Welcome to the LushApp", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(AuthActivity.this, ProfileCreationActivity.class));
                                                            finish();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(AuthActivity.this, "Failed to create account.", Toast.LENGTH_SHORT).show();
                                                        });
                                            }

                                        }else {
                                            Map<String, Object> data = new HashMap<>();
                                            data.put("username", "");
                                            data.put("userId", userId);
                                            data.put("gender", "");
                                            data.put("profilePicture", "");
                                            data.put("profileLookingPictures", new ArrayList<String>());
                                            data.put("isVerified", false);
                                            data.put("bio", "");
                                            data.put("email", acct.getEmail());
                                            data.put("friends", new ArrayList<>());
                                            data.put("rating", 100);
                                            data.put("timeLeft", 40);
                                            data.put("interest", "");
                                            data.put("questionAnswered", new ArrayList<>());
                                            data.put("upforchat",true);
                                            data.put("status","available");

                                            db.collection("users").document(userId).set(data)
                                                    .addOnSuccessListener(unused -> {
                                                        Toast.makeText(AuthActivity.this, "Welcome to the LushApp", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(AuthActivity.this, ProfileCreationActivity.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(AuthActivity.this, "Failed to create account.", Toast.LENGTH_SHORT).show();
                                                    });
                                        }

                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AuthActivity.this, "Failed to login: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(AuthActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(AuthActivity.this, "Sign-In Successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AuthActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
