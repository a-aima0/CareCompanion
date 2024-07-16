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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicalProfileActivity extends AppCompatActivity {

    public static final String TAG = "MedicalProfileActivity";
    Button gotoCreateMedicalProfileButton, gotoProfileButon;
    TextView medicalWeight, medicalHeight, medicalDOB, medicalAddress, medicalBlood, medicalOrgan;
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