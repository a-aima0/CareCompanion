package com.example.carecompanion;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditProfileActivity extends AppCompatActivity {

    public static final String TAG = "EditProfileActivity";
    EditText profileEditForename, profileEditSurname, profileEditEmail, profileEditPhone, profileEditCCode;
    Button saveProfileInfoButton, gotoProfileButton;
    ImageView profileEditImage;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    FirebaseUser user;
    StorageReference storageReference;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Intent profileData = getIntent();
        String forename = profileData.getStringExtra("forename");
        String surnames = profileData.getStringExtra("surnames");
        String email = profileData.getStringExtra("email");
        String phone = profileData.getStringExtra("phone");
        String ccode = profileData.getStringExtra("ccode");

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        user = fAuth.getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference();

        profileEditForename = findViewById(R.id.profileEditForename);
        profileEditSurname = findViewById(R.id.profileEditSurname);
        profileEditEmail = findViewById(R.id.profileEditEmail);
        profileEditPhone = findViewById(R.id.profileEditPhone);
        profileEditCCode = findViewById(R.id.profileEditCCode);
        profileEditImage = findViewById(R.id.profileEditImage);
        saveProfileInfoButton = findViewById(R.id.saveProfileInfoButton);
        gotoProfileButton = findViewById(R.id.gotoProfile);

//        // Retrieve public data
//        DocumentReference publicDocRef = fStore.collection("public").document(user.getUid());
//        publicDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//            @Override
//            public void onSuccess(DocumentSnapshot documentSnapshot) {
//                if (documentSnapshot.exists()) {
//                    profileEditForename.setText(documentSnapshot.getString("forename"));
//                    profileEditCCode.setText(documentSnapshot.getString("ccode"));
//                    profileEditPhone.setText(documentSnapshot.getString("phone"));
//                } else {
//                    Log.d(TAG, "No public document found");
//                }
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.d(TAG, "Failed to retrieve public document: " + e.getMessage());
//            }
//        });

        // Retrieve private data
        DocumentReference privateDocRef = fStore.collection("private").document(user.getUid());
        privateDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    profileEditSurname.setText(documentSnapshot.getString("surnames"));
                    profileEditEmail.setText(documentSnapshot.getString("email"));
                    profileEditForename.setText(documentSnapshot.getString("forename"));
                    profileEditCCode.setText(documentSnapshot.getString("ccode"));
                    profileEditPhone.setText(documentSnapshot.getString("phone"));
                } else {
                    Log.d(TAG, "No private document found");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Failed to retrieve private document: " + e.getMessage());
            }
        });

        if (user != null) {
            StorageReference profileReference = storageReference.child("users/" + user.getUid() + "/profileImage.jpg");
            profileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.get().load(uri).into(profileEditImage);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Set a placeholder image if profile image is not found
                    profileEditImage.setImageResource(R.drawable.placeholder);
                    Log.e(TAG, "onFailure: Profile image not found, setting placeholder", e);
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onCreate: User not authenticated");
        }


        // intialise ActivityResultLauncher
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        uploadImageToFirebase(imageUri);
                    }
                }
        );

        profileEditImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(openGalleryIntent);
            }
        });

        saveProfileInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (profileEditForename.getText().toString().isEmpty()
                        || profileEditSurname.getText().toString().isEmpty()
                        || profileEditPhone.getText().toString().isEmpty()
                        || profileEditCCode.getText().toString().isEmpty()
                        || profileEditEmail.getText().toString().isEmpty()) {
                    Toast.makeText(EditProfileActivity.this, "One or many fields are empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Prompt the user for their current password
                promptPasswordAndReauthenticate(profileEditEmail.getText().toString());
            }
        });

        profileEditForename.setText(forename);
        profileEditSurname.setText(surnames);
        profileEditEmail.setText(email);
        profileEditPhone.setText(phone);
        profileEditCCode.setText(ccode);

        Log.d(TAG, "onCreate: " + forename + " " + surnames + " " + email + " " + "+" + ccode + " " + phone);

        gotoProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                finish();
            }
        });

    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (user != null) {
            // Upload image to Firebase Storage
            StorageReference fileReference = storageReference.child("users/" + user.getUid() + "/profileImage.jpg");
            fileReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get download URL and set image to ImageView using Picasso
                    fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get()
                                    .load(uri)
                                    .transform(new CircularTransformation())
                                    .into(profileEditImage);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditProfileActivity.this, "Failed to retrieve uploaded image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to retrieve uploaded image URL", e);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to upload image", e);
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "User not authenticated");
        }
    }


    private void promptPasswordAndReauthenticate(String newEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle("Reauthenticate");
        builder.setMessage("Please enter your current password to continue:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setBackgroundResource(R.drawable.edittext_outline_blue);


        builder.setView(input);

        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();

                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

                user.reauthenticate(credential).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        // Update private data
                        DocumentReference privateDocRef = fStore.collection("private").document(user.getUid());
                        Map<String, Object> privateUpdates = new HashMap<>();
                        privateUpdates.put("email", newEmail);
                        privateUpdates.put("surnames", profileEditSurname.getText().toString());
                        privateUpdates.put("forename", profileEditForename.getText().toString());
                        privateUpdates.put("ccode", profileEditCCode.getText().toString());
                        privateUpdates.put("phone", profileEditPhone.getText().toString());

                        privateDocRef.update(privateUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        Toast.makeText(EditProfileActivity.this, "If email changed, verification link sent", Toast.LENGTH_SHORT).show();

                                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                                        finish();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(EditProfileActivity.this, "Error updating private data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Customizing title and message text programmatically
        TextView alertTitle = alertDialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (alertTitle != null) {
            alertTitle.setTextColor(getResources().getColor(R.color.dark_blue));
            alertTitle.setTextSize(20);
        }

        TextView alertMessage = alertDialog.findViewById(android.R.id.message);
        if (alertMessage != null) {
            alertMessage.setTextColor(getResources().getColor(R.color.dark_blue));
            alertMessage.setTextSize(16);
        }
    }

}
