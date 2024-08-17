package com.example.carecompanion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = "SettingsActivity";
    Button homeButton;
    LinearLayout editProfileButton, editMedicalButton, logoutButton;
    AlertDialog.Builder resetEmailAlert, deleteAlert;
    LayoutInflater inflater;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        homeButton = findViewById(R.id.homeButton);
        logoutButton = findViewById(R.id.logoutButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        editMedicalButton = findViewById(R.id.editMedicalButton);

        resetEmailAlert = new AlertDialog.Builder(this);
        deleteAlert = new AlertDialog.Builder(this);

        inflater = this.getLayoutInflater();

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        homeButton.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), MainActivity.class)));

        editProfileButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), EditProfileActivity.class));
            finish();
        });

        editMedicalButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), CreateMedicalProfileActivity.class));
            finish();
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            // Sign out from Google
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    GoogleSignInOptions.DEFAULT_SIGN_IN);
            googleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    // Google Sign out successful
                    Toast.makeText(SettingsActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    finish();
                }
            });

            // Optionally disconnect the client as well
            googleSignInClient.revokeAccess().addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    // Google revoke access successful
                }
            });


        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.resetUserPassword) {
            startActivity(new Intent(getApplicationContext(), ResetPasswordActivity.class));
        }

        if (item.getItemId() == R.id.updateEmailMenu) {
            updateEmailDialog();
        }

        if (item.getItemId() == R.id.deleteAccountMenu) {
            deleteAccountDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateEmailDialog() {
        View view = inflater.inflate(R.layout.reset_email_password_alert, null);

        EditText email = view.findViewById(R.id.resetPasswordEmailAlert);

        AlertDialog alertDialog = resetEmailAlert.setTitle("Update Email")
                .setMessage("Enter new email address")
                .setPositiveButton("Update", (dialog, which) -> {
                    String newEmail = email.getText().toString().trim();
                    if (newEmail.isEmpty()) {
                        email.setError("Required Field");
                        return;
                    }

                    FirebaseUser user = fAuth.getCurrentUser();
                    if (user != null) {
                        user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener(unused -> {
                            String userId = user.getUid();
                            fStore.collection("private").document(userId)
                                    .update("email", newEmail)
                                    .addOnSuccessListener(unused1 -> Toast.makeText(SettingsActivity.this, "Email update requested. Check your email for verification.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to update email in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }).addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Email update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Return", null)
                .setView(view)
                .create();

        alertDialog.getContext().setTheme(R.style.AlertDialogTheme);
        alertDialog.show();

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

    private void deleteAccountDialog() {
        AlertDialog alertDialog = deleteAlert.setTitle("Delete account permanently")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete Account", (dialog, which) -> {
                    FirebaseUser user = fAuth.getCurrentUser();

                    if (user != null) {
                        String userId = user.getUid();

                        deleteFirestoreData(userId, task -> {
                            if (task.isSuccessful()) {
                                user.delete().addOnSuccessListener(unused -> {
                                    Toast.makeText(SettingsActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                                    fAuth.signOut();
                                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                    finish();
                                }).addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(SettingsActivity.this, "Failed to delete Firestore data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).setNegativeButton("Cancel", null)
                .create();

        alertDialog.getContext().setTheme(R.style.AlertDialogTheme);
        alertDialog.show();

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

    private void deleteFirestoreData(String userId, OnCompleteListener<Void> onCompleteListener) {
        // Start the recursive deletion process
        deleteDocumentWithSubcollections(fStore.collection("private").document(userId))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Continue deleting from other collections
                        deleteDocumentWithSubcollections(fStore.collection("public").document(userId))
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        // Continue deleting from the last collection
                                        deleteDocumentWithSubcollections(fStore.collection("profile").document(userId))
                                                .addOnCompleteListener(onCompleteListener);
                                    } else {
                                        // If failed, pass the failure up
                                        onCompleteListener.onComplete(task1);
                                    }
                                });
                    } else {
                        // If failed, pass the failure up
                        onCompleteListener.onComplete(task);
                    }
                });
    }

    private Task<Void> deleteDocumentWithSubcollections(DocumentReference docRef) {
        // Get all subcollections in the document
        return docRef.get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        List<Task<Void>> deleteTasks = new ArrayList<>();

                        // Retrieve subcollections of the document
                        return docRef.getFirestore().collectionGroup(document.getId()).get().continueWithTask(task1 -> {
                            if (task1.isSuccessful()) {
                                for (QueryDocumentSnapshot subcollectionDoc : task1.getResult()) {
                                    deleteTasks.add(deleteCollection(subcollectionDoc.getReference().getParent()));
                                }

                                // Combine all delete tasks into one task
                                return Tasks.whenAll(deleteTasks)
                                        .continueWithTask(task2 -> docRef.delete());
                            } else {
                                throw task1.getException();
                            }
                        });
                    } else {
                        throw task.getException();
                    }
                });
    }

    private Task<Void> deleteCollection(CollectionReference collection) {
        return collection.get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        List<Task<Void>> deleteTasks = new ArrayList<>();

                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            deleteTasks.add(deleteDocumentWithSubcollections(document.getReference()));
                        }

                        return Tasks.whenAll(deleteTasks);
                    } else {
                        throw task.getException();
                    }
                });
    }
}

//
//public class SettingsActivity extends AppCompatActivity {
//
//    public static final String TAG = "SettingsActivity";
//    Button homeButton;
//    LinearLayout editProfileButton, editMedicalButton, logoutButton;
//    AlertDialog.Builder resetEmailAlert, deleteAlert;
//    LayoutInflater inflater;
//    FirebaseAuth fAuth;
//    FirebaseFirestore fStore;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_settings);
//
//        homeButton = findViewById(R.id.homeButton);
//        logoutButton = findViewById(R.id.logoutButton);
//        editProfileButton = findViewById(R.id.editProfileButton);
//        editMedicalButton = findViewById(R.id.editMedicalButton);
//
//        resetEmailAlert = new AlertDialog.Builder(this);
//        deleteAlert = new AlertDialog.Builder(this);
//
//        inflater = this.getLayoutInflater();
//
//        fAuth = FirebaseAuth.getInstance();
//        fStore = FirebaseFirestore.getInstance();
//
//        homeButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getApplicationContext(), MainActivity.class));
//            }
//        });
//
//        editProfileButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getApplicationContext(), EditProfileActivity.class));
//                finish();
//            }
//        });
//
//        editMedicalButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getApplicationContext(), CreateMedicalProfileActivity.class));
//                finish();
//            }
//        });
//
//        logoutButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                FirebaseAuth.getInstance().signOut();
//                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
//                finish();
//            }
//        });
//
//
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu (Menu menu){
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.option_menu, menu);
//
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if(item.getItemId() == R.id.resetUserPassword){
//            startActivity(new Intent(getApplicationContext(), ResetPasswordActivity.class));
//        }
//
//        if(item.getItemId() == R.id.updateEmailMenu){
//            // start alert (pop up) dialog
//            View view = inflater.inflate(R.layout.reset_email_password_alert, null);
//
//            EditText email = view.findViewById(R.id.resetPasswordEmailAlert);
//
//            AlertDialog alertDialog = resetEmailAlert.setTitle("Update Email")
//                    .setMessage("Enter new email address")
//                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
//
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            // validate the email address
//                            String newEmail = email.getText().toString().trim();
//                            if (newEmail.isEmpty()){
//                                email.setError("Required Field");
//                                return;
//                            }
//
//                            // Update email
//                            FirebaseUser user = fAuth.getCurrentUser();
//                            if (user != null) {
//                                user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener(new OnSuccessListener<Void>() {
//                                    @Override
//                                    public void onSuccess(Void unused) {
//                                        // update email in firestore
//                                        String userId = user.getUid();
//                                        fStore.collection("private").document(userId)
//                                                .update("email", newEmail)
//                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                    @Override
//                                                    public void onSuccess(Void unused) {
//                                                        Toast.makeText(SettingsActivity.this, "Email update requested. Check your email for verification.", Toast.LENGTH_SHORT).show();
//                                                    }
//                                                }).addOnFailureListener(new OnFailureListener() {
//                                                    @Override
//                                                    public void onFailure(@NonNull Exception e) {
//                                                        Toast.makeText(SettingsActivity.this, "Failed to update email in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                                    }
//                                                });
//                                    }
//                                }).addOnFailureListener(new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        Toast.makeText(SettingsActivity.this, "Email update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            }
//                        }
//                    })
//                    .setNegativeButton("Return", null)
//                    .setView(view)
//                    .create();
//
//            alertDialog.getContext().setTheme(R.style.AlertDialogTheme);
//            alertDialog.show();
//
//            // Customizing title and message text programmatically
//            TextView alertTitle = alertDialog.findViewById(androidx.appcompat.R.id.alertTitle);
//            if (alertTitle != null) {
//                alertTitle.setTextColor(getResources().getColor(R.color.dark_blue));
//                alertTitle.setTextSize(20);
//            }
//
//            TextView alertMessage = alertDialog.findViewById(android.R.id.message);
//            if (alertMessage != null) {
//                alertMessage.setTextColor(getResources().getColor(R.color.dark_blue));
//                alertMessage.setTextSize(16);
//            }
//        }
//
//        if (item.getItemId()==R.id.deleteAccountMenu){
//            AlertDialog alertDialog = deleteAlert.setTitle("Delete account permanently")
//                    .setMessage("Are you sure?")
//                    .setPositiveButton("Delete Account", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            FirebaseUser user = fAuth.getCurrentUser();
//
//                            if (user != null) {
//                                String userId = user.getUid();
//
//                                // Delete Firestore data from multiple collections
//
//                                fStore.collection("private").document(userId).delete()
//                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                            @Override
//                                            public void onSuccess(Void unused) {
//                                                // Continue deleting from other collections
//                                                fStore.collection("public").document(userId).delete()
//                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                            @Override
//                                                            public void onSuccess(Void unused) {
//                                                                fStore.collection("profile").document(userId).delete()
//                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                                            @Override
//                                                                            public void onSuccess(Void unused) {
//                                                                                user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                                                    @Override
//                                                                                    public void onSuccess(Void unused) {
//                                                                                        Toast.makeText(SettingsActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
//                                                                                        fAuth.signOut();
//                                                                                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
//                                                                                        finish();
//                                                                                    }
//                                                                                }).addOnFailureListener(new OnFailureListener() {
//                                                                                    @Override
//                                                                                    public void onFailure(@NonNull Exception e) {
//                                                                                        Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                                                                                    }
//                                                                                });
//                                                                            }
//                                                                        })
//                                                                        .addOnFailureListener(new OnFailureListener() {
//                                                                            @Override
//                                                                            public void onFailure(@NonNull Exception e) {
//                                                                                Toast.makeText(SettingsActivity.this, "profile collection not deleted", Toast.LENGTH_SHORT).show();
//                                                                                Log.e(TAG, "onFailure: Profile collection not deleted"+e.getMessage());
//                                                                            }
//                                                                        });
//                                                            }
//                                                        }).addOnFailureListener(new OnFailureListener() {
//                                                            @Override
//                                                            public void onFailure(@NonNull Exception e) {
//                                                                Toast.makeText(SettingsActivity.this, "public collection not deleted", Toast.LENGTH_SHORT).show();
//                                                                Log.e(TAG, "onFailure: public collection not deleted"+e.getMessage());
//                                                            }
//                                                        });
//                                            }
//                                        }).addOnFailureListener(new OnFailureListener() {
//                                            @Override
//                                            public void onFailure(@NonNull Exception e) {
//                                                Toast.makeText(SettingsActivity.this, "private collection not deleted", Toast.LENGTH_SHORT).show();
//                                                Log.e(TAG, "onFailure: private collection not deleted"+e.getMessage());
//                                            }
//                                        });
//                            }
//                        }
//                    }).setNegativeButton("Cancel", null)
//                    .create();
//
//            alertDialog.getContext().setTheme(R.style.AlertDialogTheme);
//            alertDialog.show();
//
//            // Customizing title and message text programmatically
//            TextView alertTitle = alertDialog.findViewById(androidx.appcompat.R.id.alertTitle);
//            if (alertTitle != null) {
//                alertTitle.setTextColor(getResources().getColor(R.color.dark_blue));
//                alertTitle.setTextSize(20);
//            }
//
//            TextView alertMessage = alertDialog.findViewById(android.R.id.message);
//            if (alertMessage != null) {
//                alertMessage.setTextColor(getResources().getColor(R.color.dark_blue));
//                alertMessage.setTextSize(16);
//            }
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//
//}