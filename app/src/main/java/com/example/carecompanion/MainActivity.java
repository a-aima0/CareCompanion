package com.example.carecompanion;

import static android.view.View.VISIBLE;
import static com.example.carecompanion.ProfileActivity.TAG;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_LAYOUT3_VISIBLE_PREFIX = "layout3_visible";
    private static final String KEY_CHECKBOX_PREFIX = "checkbox_";
    private static final String KEY_CHECKBOX3_STATE = "checkbox3_state";
    private static final String KEY_CHECKBOX4_STATE = "checkbox4_state";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";
    CardView profileCard, medicalCard, settingsCard;
    Button verifyEmailButton, cancelOnboarding;
    TextView nameTitle, progressPercentage;
    ProgressBar progressBar;
    String userID;
    AlertDialog.Builder deleteAlert;
    CheckBox checkBox1, checkBox2, checkBox3, checkBox4;
    androidx.constraintlayout.widget.ConstraintLayout layout3;
    FirebaseFirestore fStore;
    FirebaseAuth fAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyEmailButton = findViewById(R.id.verifyEmailButton);
        cancelOnboarding = findViewById(R.id.cancelOnboarding);

        profileCard = findViewById(R.id.ProfileCard);
        medicalCard = findViewById(R.id.medicalProfileCard);
        settingsCard = findViewById(R.id.settingsCard);

        deleteAlert = new AlertDialog.Builder(this);

        nameTitle = findViewById(R.id.nameTitle);
        progressPercentage = findViewById(R.id.progressPercentage);

        progressBar = findViewById(R.id.progressBar);

        checkBox1 = findViewById(R.id.checkBox1);
        checkBox2 = findViewById(R.id.checkBox2);
        checkBox3 = findViewById(R.id.checkBox3);
        checkBox4 = findViewById(R.id.checkBox4);

        layout3 = findViewById(R.id.layout3);

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        userID = fAuth.getCurrentUser().getUid();


        if (!fAuth.getCurrentUser().isEmailVerified()){
            verifyEmailButton.setVisibility(VISIBLE);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_LAYOUT3_VISIBLE_PREFIX + userID, true)) {
            layout3.setVisibility(View.GONE);
        }

        checkBox3.setChecked(prefs.getBoolean(KEY_CHECKBOX3_STATE, false));
        checkBox4.setChecked(prefs.getBoolean(KEY_CHECKBOX4_STATE, false));


        CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(KEY_CHECKBOX_PREFIX + userID + "_" + buttonView.getId(), isChecked);
                editor.apply();
                updateProgress();
            }
        };

        // Update progress based on restored checkbox states
        updateProgress();

        checkBox1.setOnCheckedChangeListener(checkboxListener);
        checkBox2.setOnCheckedChangeListener(checkboxListener);

        verifyEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send verification email
                fAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(MainActivity.this, "Verification Email Sent", Toast.LENGTH_SHORT).show();
                        verifyEmailButton.setVisibility(View.GONE);
                        checkBox2.setChecked(true);
                        updateProgress();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failure to send verification: " +e.getMessage(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        cancelOnboarding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog alertDialog = deleteAlert.setTitle("Skip onboarding")
                        .setMessage("Do you want to skip the onboarding?")
                        .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                layout3.setVisibility(View.GONE);
                                // Save visibility state of layout3
                                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                                editor.putBoolean(KEY_LAYOUT3_VISIBLE_PREFIX + userID, false);
                                editor.apply();

                            }
                        })
                        .setNegativeButton("Return", null)
                        .create();

                alertDialog.getContext().setTheme(R.style.AlertDialogTheme);
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
        });



        profileCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
            }
        });

        medicalCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MedicalProfileActivity.class));
            }
        });

        settingsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });



        DocumentReference publicDocRef = fStore.collection("public").document(userID);
        publicDocRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "Error fetching public profile data: " + e.getMessage());
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    nameTitle.setText(documentSnapshot.getString("forename"));
                } else {
                    Log.d(TAG, "No such document in public collection");
                }
            }
        });

        checkBox3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the checkbox state
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_CHECKBOX3_STATE, isChecked);
            editor.apply();
        });

        checkBox3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), EditProfileActivity.class));
            }
        });

        checkBox4.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the checkbox state
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_CHECKBOX4_STATE, isChecked);
            editor.apply();
        });

        checkBox4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CreateMedicalProfileActivity.class));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh user information
        FirebaseUser user = fAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (user.isEmailVerified()) {
                        verifyEmailButton.setVisibility(View.GONE);
                        checkBox2.setChecked(true);
                        updateProgress();
                    }


                }
            });
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        checkBox3.setChecked(prefs.getBoolean(KEY_CHECKBOX3_STATE, false));
        checkBox4.setChecked(prefs.getBoolean(KEY_CHECKBOX4_STATE, false));

    }

    private void updateProgress() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int checkedCount = 0;
        if (checkBox1.isChecked()) checkedCount++;
        if (checkBox2.isChecked()) checkedCount++;
        if (checkBox3.isChecked()) checkedCount++;
        if (checkBox4.isChecked()) checkedCount++;

        int progress = (checkedCount * 100) / 4;
        progressBar.setProgress(progress);
        progressPercentage.setText(progress + "% Completed");

        if (progress == 100 && !prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)) {
            showCompletionDialog();
        }
    }
    private void showCompletionDialog() {

        AlertDialog alertDialog = deleteAlert.setTitle("Onboarding Complete!")
                .setMessage("Congratulation! You have completed the onboarding process")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Hide layout3 and save its state
                        layout3.setVisibility(View.GONE);
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putBoolean(KEY_LAYOUT3_VISIBLE_PREFIX + userID, false);
                        editor.putBoolean(KEY_ONBOARDING_COMPLETE, true);
                        editor.apply();
                    }
                })
                .setNegativeButton("Return", null)
                .create();

        alertDialog.getContext().setTheme(R.style.AlertDialogTheme);
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

