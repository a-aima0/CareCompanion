package com.example.carecompanion;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicalProfileActivity extends AppCompatActivity {

    public static final String TAG = "MedicalProfileActivity";
    Button gotoCreateMedicalProfileButton, gotoProfileButon;
    TextView medicalWeight, medicalHeight, medicalDOB, medicalAddress, medicalBlood, medicalOrgan,
            medicalAllergies, medicalConditions, medicalMedication, medicalContacts, medicalInfo;
    FirebaseFirestore fStore;
    FirebaseAuth fAuth;
    FirebaseUser user;
    String userID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_profile);

        medicalWeight = findViewById(R.id.medicalWeight);
        medicalHeight = findViewById(R.id.medicalHeight);
        medicalDOB = findViewById(R.id.medicalDOB);
        medicalAddress = findViewById(R.id.medicalAddress);
        medicalBlood = findViewById(R.id.medicalBloodType);
        medicalOrgan = findViewById(R.id.medicalOrgan);
        medicalInfo = findViewById(R.id.medicalInfo);

        medicalAllergies = findViewById(R.id.medicalAllergies);
        medicalConditions = findViewById(R.id.medicalConditions);
        medicalMedication = findViewById(R.id.medicalMedications);
        medicalContacts = findViewById(R.id.medicalContacts);

        gotoCreateMedicalProfileButton = findViewById(R.id.gotoCreateMedicalActivity);
        gotoProfileButon = findViewById(R.id.gotoProfile);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // check if user is authenticated
        user = fAuth.getCurrentUser();
        if (user != null){
            userID = fAuth.getCurrentUser().getUid();

            DocumentReference documentReference = fStore.collection("profile").document(userID);
            documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()){
                        medicalHeight.setText(documentSnapshot.getString("height"));
                        medicalWeight.setText(documentSnapshot.getString("weight"));
                        medicalDOB.setText(documentSnapshot.getString("dob"));
                        medicalAddress.setText(documentSnapshot.getString("address"));
                        medicalBlood.setText(documentSnapshot.getString("bloodType"));
                        medicalOrgan.setText(documentSnapshot.getString("organDonor"));
                        medicalInfo.setText(documentSnapshot.getString("info"));

                        documentReference.collection("allergies").get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        StringBuilder allergiesBuilder = new StringBuilder();
                                        for (DocumentSnapshot allergyDoc : queryDocumentSnapshots){

                                            allergiesBuilder.append("Allergy: ").append(allergyDoc.getString("name")).append("\n");
                                            allergiesBuilder.append("Details: ").append(allergyDoc.getString("details")).append("\n");
                                            allergiesBuilder.append("\n"); // Add a blank line to separate each allergy
                                        }
                                        medicalAllergies.setText(allergiesBuilder.toString().trim());
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "onFailure: Error fetching allergies: " + e.getMessage());
                                        Toast.makeText(MedicalProfileActivity.this, "Error fetching allergies", Toast.LENGTH_SHORT).show();
                                    }
                                });

                        documentReference.collection("contacts").get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        StringBuilder contactBuilder = new StringBuilder();
                                        for (DocumentSnapshot contactDoc : queryDocumentSnapshots){

                                            contactBuilder.append("Contact: ").append(contactDoc.getString("name")).append(" ")
                                                    .append("(").append(contactDoc.getString("relation")).append(") ").append("\n");
                                            contactBuilder.append("Phone: ").append(contactDoc.getString("phone")).append("\n");
                                            contactBuilder.append("\n");
                                        }
                                        medicalContacts.setText(contactBuilder.toString().trim());
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "onFailure: Error fetching allergies: " + e.getMessage());
                                        Toast.makeText(MedicalProfileActivity.this, "Error fetching allergies", Toast.LENGTH_SHORT).show();
                                    }
                                });

                        documentReference.collection("conditions").get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        StringBuilder conditionsBuilder = new StringBuilder();
                                        for (DocumentSnapshot conditionsDoc : queryDocumentSnapshots){

                                            conditionsBuilder.append("Condition: ").append(conditionsDoc.getString("name")).append("\n");
                                            conditionsBuilder.append("Details: ").append(conditionsDoc.getString("details")).append("\n");
                                            conditionsBuilder.append("\n"); // Add a blank line to separate each allergy
                                        }
                                        medicalConditions.setText(conditionsBuilder.toString().trim());
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "onFailure: Error fetching conditions: " + e.getMessage());
                                        Toast.makeText(MedicalProfileActivity.this, "Error fetching conditions", Toast.LENGTH_SHORT).show();
                                    }
                                });

                        documentReference.collection("medications").get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        StringBuilder medicationsBuilder = new StringBuilder();
                                        for (DocumentSnapshot medicationDoc : queryDocumentSnapshots){

                                            medicationsBuilder.append("Medication: ").append(medicationDoc.getString("name")).append("\n");
                                            medicationsBuilder.append("Details: ").append(medicationDoc.getString("details")).append("\n");
                                            medicationsBuilder.append("\n"); // Add a blank line to separate each allergy
                                        }
                                        medicalMedication.setText(medicationsBuilder.toString().trim());
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "onFailure: Error fetching medications: " + e.getMessage());
                                        Toast.makeText(MedicalProfileActivity.this, "Error fetching medications", Toast.LENGTH_SHORT).show();
                                    }
                                });



                    } else {
                        Log.d(TAG, "No such document exists in profile collections");
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error fetching private profile data: " + e.getMessage());
                }
            });

        } else {
            Toast.makeText(MedicalProfileActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onClick: User not authenticated");
        }

        gotoCreateMedicalProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CreateMedicalProfileActivity.class));
            }
        });

        gotoProfileButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
            }
        });

    }


}