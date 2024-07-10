package com.example.carecompanion;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    public static final String TAG = "TAG";
    TextView profileForename, profileSurname, profileEmail, profilePhone, profileCCode;
    Button gotoHomeButton, changeProfileButton;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userID;
    ImageView profileImage;
    StorageReference storageReference;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileForename = findViewById(R.id.profileForename);
        profileSurname = findViewById(R.id.profileSurname);
        profileEmail = findViewById(R.id.profileEmail);
        profileCCode = findViewById(R.id.profileCCode);
        profilePhone = findViewById(R.id.profilePhone);
        profileImage = findViewById(R.id.profileImage);
        changeProfileButton = findViewById(R.id.changeProfileButton);
        gotoHomeButton = findViewById(R.id.gotoHomeButton);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        StorageReference profileReference = storageReference.child("users/" + Objects.requireNonNull(fAuth.getCurrentUser()).getUid() + "/profileImage.jpg");
        profileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(profileImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Set a placeholder image if profile image is not found
                profileImage.setImageResource(R.drawable.placeholder);
                Log.e(TAG, "onFailure: Profile image not found, setting placeholder", e);
            }
        });


        userID = fAuth.getCurrentUser().getUid();

        gotoHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });


        DocumentReference documentReference = fStore.collection("users").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                assert documentSnapshot != null;
                if (documentSnapshot.exists()) {
                    profileForename.setText(documentSnapshot.getString("forename"));
                    profileSurname.setText(documentSnapshot.getString("surnames"));
                    profileEmail.setText(documentSnapshot.getString("email"));
                    profileCCode.setText(documentSnapshot.getString("ccode"));
                    profilePhone.setText(documentSnapshot.getString("phone"));
                } else {
                    Log.d(TAG, "onEvent: Document does not exist");
                }
            }
        });


        changeProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(v.getContext(), EditProfileActivity.class);
                i.putExtra("forename",profileForename.getText().toString());
                i.putExtra("surnames", profileSurname.getText().toString());
                i.putExtra("email", profileEmail.getText().toString());
                i.putExtra("ccode", profileCCode.getText().toString());
                i.putExtra("phone", profilePhone.getText().toString());
                startActivity(i);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Deprecated and not used in the refactored code
    }


}
