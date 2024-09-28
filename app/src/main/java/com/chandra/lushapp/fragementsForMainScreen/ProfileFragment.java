package com.chandra.lushapp.fragementsForMainScreen;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chandra.lushapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    TextView username, bio, interest, gender, email, rating,timeleft;
    ImageView profileImage, ImageProfile1, ImageProfile2, ImageProfile3, ImageProfile4, ImageProfile5, ImageProfile6;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri filePath;
    private int selectedImageIndex = -1; // To track which image is being selected

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        username = view.findViewById(R.id.username);
        bio = view.findViewById(R.id.bio);
        interest = view.findViewById(R.id.interest);
        gender = view.findViewById(R.id.gender);
        email = view.findViewById(R.id.email);
        rating = view.findViewById(R.id.rating);
        timeleft = view.findViewById(R.id.timeleft);

        profileImage = view.findViewById(R.id.user_image);
        ImageProfile1 = view.findViewById(R.id.image1);
        ImageProfile2 = view.findViewById(R.id.image2);
        ImageProfile3 = view.findViewById(R.id.image3);
        ImageProfile4 = view.findViewById(R.id.image4);
        ImageProfile5 = view.findViewById(R.id.image5);
        ImageProfile6 = view.findViewById(R.id.image6);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        if (user != null) {
            String userId = user.getUid();
            db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("users").document(userId);

            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                loadUserData(documentSnapshot);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Check your internet", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        setImageClickListeners();

        return view;
    }

    private void loadUserData(DocumentSnapshot documentSnapshot) {
        String usname = documentSnapshot.getString("username");
        String usbio = documentSnapshot.getString("bio");
        String usemail = documentSnapshot.getString("email");
        String usgender = documentSnapshot.getString("gender");
        String usinterest = documentSnapshot.getString("interest");
        Long usrating = documentSnapshot.getLong("rating");
        String usprofilePicture = documentSnapshot.getString("ImageForLook0");
        String image1 = documentSnapshot.getString("ImageForLook1");
        String image2 = documentSnapshot.getString("ImageForLook2");
        String image3 = documentSnapshot.getString("ImageForLook3");
        String image4 = documentSnapshot.getString("ImageForLook4");
        String image5 = documentSnapshot.getString("ImageForLook5");
        Long ustimeleft = documentSnapshot.getLong("timeLeft");
        String image6 = documentSnapshot.getString("ImageForLook6");

        username.setText(usname);
        bio.setText(usbio);
        interest.setText(usinterest);
        gender.setText(usgender);
        email.setText(usemail);
        rating.setText(usrating.toString());
        timeleft.setText(ustimeleft.toString());

        Glide.with(getContext())
                .load(usprofilePicture)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.profile)
                        .override(200, 200))
                .into(profileImage);

        Glide.with(getContext())
                .load(image1)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.profile)
                        .override(200, 200))
                .into(ImageProfile1);

        Glide.with(getContext())
                .load(image2)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.profile)
                        .override(200, 200))
                .into(ImageProfile2);

        Glide.with(getContext())
                .load(image3)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.profile)
                        .override(200, 200))
                .into(ImageProfile3);

        Glide.with(getContext())
                .load(image4)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.profile)
                        .override(200, 200))
                .into(ImageProfile4);

        Glide.with(getContext())
                .load(image5)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.profile)
                        .override(200, 200))
                .into(ImageProfile5);

        Glide.with(getContext())
                .load(image6)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.profile)
                        .override(200, 200))
                .into(ImageProfile6);









    }

    private void setImageClickListeners() {
        profileImage.setOnClickListener(view -> selectImage(7));
        ImageProfile1.setOnClickListener(v -> selectImage(1));
        ImageProfile2.setOnClickListener(v -> selectImage(2));
        ImageProfile3.setOnClickListener(v -> selectImage(3));
        ImageProfile4.setOnClickListener(v -> selectImage(4));
        ImageProfile5.setOnClickListener(v -> selectImage(5));
        ImageProfile6.setOnClickListener(v -> selectImage(6));
    }

    private void selectImage(int index) {
        selectedImageIndex = index;
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image from here..."), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            uploadImageToFirebase(filePath);
        }
    }

    private void uploadImageToFirebase(Uri filePath) {
        if (filePath != null) {
            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            FirebaseUser user = mAuth.getCurrentUser();
            String userId = user.getUid();
            StorageReference ref = storageReference.child("profileimages/" + userId + "/" + selectedImageIndex);

            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri downloadUri) {
                                    saveImageUrlToFirestore(userId, downloadUri.toString(), selectedImageIndex);
                                    loadImage(downloadUri.toString(), selectedImageIndex);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveImageUrlToFirestore(String userId, String imageUrl, int index) {
        DocumentReference docRef = db.collection("users").document(userId);
        Map<String, Object> imageData = new HashMap<>();
        imageData.put("ImageForLook" + index, imageUrl); // image1, image2, etc.

        docRef.update(imageData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(getContext(), "Failed to save image URL", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadImage(String imageUrl, int index) {
        ImageView imageView = null;
        switch (index) {
            case 0:
                imageView = profileImage;
            case 1:
                imageView = ImageProfile1;
                break;
            case 2:
                imageView = ImageProfile2;
                break;
            case 3:
                imageView = ImageProfile3;
                break;
            case 4:
                imageView = ImageProfile4;
                break;
            case 5:
                imageView = ImageProfile5;
                break;
            case 6:
                imageView = ImageProfile6;
                break;
        }

        if (imageView != null) {
            Glide.with(getContext())
                    .load(imageUrl)
                    .circleCrop()
                    .apply(new RequestOptions()
                            .placeholder(R.mipmap.ic_launcher_round)
                            .error(R.drawable.profile))
                    .into(imageView);
        }
    }
}
