package com.example.carecompanion;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateMedicalProfileActivity extends AppCompatActivity {
    public static final String TAG = "CreateMedicalProfileActivity";
    Spinner registerBloodTypeSpinner, registerOrganSpinner;
    Button gotoMedicalActivityButton, saveChangesButton, addAllergyButton, deleteAllergyButton;
    EditText registerWeight, registerHeight, registerDOB, registerFirstLine, registerSecondLine,
            registerCity, registerPost, allergyNameEditText, allergyDetailsEditText;

    LinearLayout allergiesContainer;
    String userID;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_medical_profile);

        registerHeight = findViewById(R.id.registerHeight);
        registerWeight = findViewById(R.id.registerWeight);
        registerDOB = findViewById(R.id.registerDOB);
        registerFirstLine = findViewById(R.id.registerFirstLineAddress);
        registerSecondLine = findViewById(R.id.registerSecondLineAddress);
        registerCity = findViewById(R.id.registerCity);
        registerPost = findViewById(R.id.registerPostalCode);

        addAllergyButton = findViewById(R.id.addAllergyButton);
        deleteAllergyButton = findViewById(R.id.deleteAllergyButton);
        allergyNameEditText = findViewById(R.id.allergyNameEditText);
        allergyDetailsEditText = findViewById(R.id.allergyDetailsEditText);
        allergiesContainer = findViewById(R.id.allergiesContainer);

        gotoMedicalActivityButton = findViewById(R.id.gotoMedicalActivity);
        saveChangesButton = findViewById(R.id.saveChangesButton);

        registerBloodTypeSpinner = findViewById(R.id.registerBloodTypeSpinner);
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter
                .createFromResource(this, R.array.blood_type_options, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        registerBloodTypeSpinner.setAdapter(bloodAdapter);

        registerOrganSpinner = findViewById(R.id.registerOrganSpinner);
        ArrayAdapter<CharSequence> organAdapter = ArrayAdapter
                .createFromResource(this, R.array.organ_donor_options, android.R.layout.simple_spinner_item);
        organAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        registerOrganSpinner.setAdapter(organAdapter);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        gotoMedicalActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MedicalProfileActivity.class));
            }
        });

        // retrieving profile data
        user = fAuth.getCurrentUser();
        if (user != null) {
            userID = user.getUid();
            DocumentReference documentReference = fStore.collection("profile").document(userID);
            documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        registerHeight.setText(documentSnapshot.getString("height"));
                        registerWeight.setText(documentSnapshot.getString("weight"));
                        registerDOB.setText(documentSnapshot.getString("dob"));

                        // Split the address back into components
                        String address = documentSnapshot.getString("address");
                        if (address != null) {
                            String[] addressParts = address.split(", ");
                            if (addressParts.length == 4) {
                                registerFirstLine.setText(addressParts[0]);
                                registerSecondLine.setText(addressParts[1]);
                                registerCity.setText(addressParts[2]);
                                registerPost.setText(addressParts[3]);
                            }
                        }

                        // Set the correct items in the spinners
                        String bloodType = documentSnapshot.getString("bloodType");
                        if (bloodType != null) {
                            int bloodTypePosition = bloodAdapter.getPosition(bloodType);
                            registerBloodTypeSpinner.setSelection(bloodTypePosition);
                        }

                        String organDonor = documentSnapshot.getString("organDonor");
                        if (organDonor != null) {
                            int organDonorPosition = organAdapter.getPosition(organDonor);
                            registerOrganSpinner.setSelection(organDonorPosition);
                        }

                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: " + e.getMessage());
                }
            });
        } else {
            Log.d(TAG, "User not authenticated");
        }


        saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // extract the data from the form
                String height = registerHeight.getText().toString();
                String weight = registerWeight.getText().toString();
                String dob = registerDOB.getText().toString();
                String firstlineAddress = registerFirstLine.getText().toString();
                String secondlineAddress = registerSecondLine.getText().toString();
                String cityAddress = registerCity.getText().toString();
                String postAddress = registerPost.getText().toString();

                // extract the selected values from the spinners
                String bloodType = registerBloodTypeSpinner.getSelectedItem().toString();
                String organDonor = registerOrganSpinner.getSelectedItem().toString();

                // combine address
                String fullAddress = firstlineAddress + ", " + secondlineAddress + ", " + cityAddress + ", " + postAddress;

                // validate data
                if (height.isEmpty()){
                    registerHeight.setError("Height is required");
                    return;
                }
                if (weight.isEmpty()){
                    registerWeight.setError("Weight is required");
                    return;
                }
                if(dob.isEmpty()){
                    registerDOB.setError("Date of birth is required");
                    return;
                }
                if(postAddress.isEmpty()){
                    registerPost.setError("Postal code is required");
                    return;
                }
                if(cityAddress.isEmpty()){
                    registerCity.setError("City is required");
                    return;
                }
                if (firstlineAddress.isEmpty()){
                    registerFirstLine.setError("First line of address is required");
                    return;
                }


                // check if user is authenticated
                user = fAuth.getCurrentUser();
                if (user != null){
                    userID = fAuth.getCurrentUser().getUid();

                    DocumentReference documentReference = fStore.collection("profile").document(userID);
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("height", height);
                    profileData.put("weight", weight);
                    profileData.put("dob", dob);
                    profileData.put("address", fullAddress);
                    profileData.put("bloodType", bloodType);
                    profileData.put("organDonor", organDonor);

                    //saving profiledata
                    documentReference.set(profileData).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "onSuccess: medical profile is created for " + userID);
                            Toast.makeText(CreateMedicalProfileActivity.this, "Medical Profile Updated", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "onFailure: " + e.getMessage());
                            Toast.makeText(CreateMedicalProfileActivity.this, "Error:data could not be saved " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


                } else {
                    Toast.makeText(CreateMedicalProfileActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: User not authenticated");
                }
            }
        });

    }


}