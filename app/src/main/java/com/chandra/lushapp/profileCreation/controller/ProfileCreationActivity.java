package com.chandra.lushapp.profileCreation.controller;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chandra.lushapp.MainActivity;
import com.chandra.lushapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ProfileCreationActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 22;
    FirebaseStorage storage;
    StorageReference storageReference;
    ImageView profile;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_creation);

        TextView username = findViewById(R.id.username);
        TextView bio = findViewById(R.id.bio);
        TextView interest = findViewById(R.id.interest);
        Button male = findViewById(R.id.male);
        Button female = findViewById(R.id.female);
        Button others = findViewById(R.id.others);
        TextView gender = findViewById(R.id.gender);
        profile = findViewById(R.id.profileImage);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        sharedPreferences = getSharedPreferences("__lushapp__",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();


        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectImage();
            }
        });

        male.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gender.setText(male.getText());
            }
        });

        female.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gender.setText(female.getText());
            }
        });

        others.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gender.setText(others.getText());
            }
        });

        Button letmeinbutton = findViewById(R.id.letmeinbutton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        letmeinbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usname = username.getText().toString();
                String usbio = bio.getText().toString();
                String usinterest = interest.getText().toString();
                String usgender = gender.getText().toString();

                editor.putString("name",usname);
                editor.putString("male",usgender);

                if(usgender.contains("Gender")){
                    Toast.makeText(ProfileCreationActivity.this, "Select Gender", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!usname.isEmpty()){

                   db.collection("usernames").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                       @Override
                       public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                           boolean found = false;
                           for(DocumentSnapshot documentSnapshot: queryDocumentSnapshots){

                               if(!usname.equals(documentSnapshot.getId())){
                                   FirebaseUser user = mAuth.getCurrentUser();
                                   String userId = user.getUid();
                                   if(!userId.isEmpty()){
                                       Map<String, Object> data = new HashMap<>();
                                       data.put("username",usname);
                                       data.put("bio",usbio);
                                       data.put("interest",usinterest);
                                       data.put("gender",usgender);
                                       db.collection("users").document(userId).update(data)
                                               .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                   @Override
                                                   public void onSuccess(Void unused) {
                                                       Map<String ,Object> data = new HashMap<>();
                                                       data.put("userid",userId);
                                                       db.collection("usernames").document(usname).set(data);
                                                       Toast.makeText(ProfileCreationActivity.this, "Username updated successsfully", Toast.LENGTH_SHORT).show();

                                                       startActivity(new Intent(ProfileCreationActivity.this, MainActivity.class));
                                                       finish();
                                                   }
                                               })
                                               .addOnFailureListener(new OnFailureListener() {
                                                   @Override
                                                   public void onFailure(@NonNull Exception e) {
                                                       Toast.makeText(ProfileCreationActivity.this, "Failed to add username", Toast.LENGTH_SHORT).show();
                                                   }
                                               });
                                   }
                               }else {
                                   Toast.makeText(ProfileCreationActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                               }
                           }
                       }
                   });
                }else{
                    Toast.makeText(ProfileCreationActivity.this, "Username is empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void SelectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image from here..."), PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(),
                        filePath);
                profile.setImageBitmap(bitmap);
                UploadImage();
            }
            catch (IOException e) {
                // Log the exception
                e.printStackTrace();
            }
        }
    }

    private void UploadImage() {
        if (filePath != null) {
            ProgressDialog progressDialog
                    = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            StorageReference ref = storageReference.child("profileimages/" + user.getUid() + "/" + 0);
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri downloadUri) {
                                    // Download URL available here
                                    String imageUrl = downloadUri.toString();

                                    // Save the image URL to Firestore
                                    saveImageToUrl(getApplicationContext(),imageUrl);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            progressDialog.dismiss();
                            Toast.makeText(ProfileCreationActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploaded " + (int)progress + "%");
                        }
                    });
        }
    }

    private void saveImageToUrl(Context context, String profileImage) {
        mAuth = FirebaseAuth.getInstance();
        String user = mAuth.getCurrentUser().getUid();

        if (!user.isEmpty()) {
            db = FirebaseFirestore.getInstance();
            DocumentReference documentReference = db.collection("users").document(user);
            Map<String, Object> updateInfo = new HashMap<>();
            updateInfo.put("ImageForLook0", profileImage);


            documentReference.update(updateInfo).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(context, "Profile Created Successfully", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(context, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}