package com.example.carecompanion;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
    Button gotoMedicalActivityButton, addAllergyButton, saveChangesButton, addMedicationsButton,
            addConditionButton, addContactButton;
    EditText registerWeight, registerHeight, registerDOB, registerFirstLine, registerSecondLine,
            registerCity, registerPost, registerInfo;

    LinearLayout allergiesContainer, dynamicAllergiesContainer, medicationsContainer,
            dynamicMedicationsContainer, conditionsContainer, dynamicConditionsContainer,
            contactsContainer, dynamicContactsContainer;
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
        registerInfo = findViewById(R.id.registerInfo);

        addAllergyButton = findViewById(R.id.addAllergyButton);
        allergiesContainer = findViewById(R.id.allergiesContainer);
        dynamicAllergiesContainer = findViewById(R.id.dynamicAllergiesContainer);

        addMedicationsButton = findViewById(R.id.addMedicationButton);
        medicationsContainer = findViewById(R.id.medicationsContainer);
        dynamicMedicationsContainer = findViewById(R.id.dynamicMedicationsContainer);

        addConditionButton = findViewById(R.id.addConditionButton);
        conditionsContainer = findViewById(R.id.conditionsContainer);
        dynamicConditionsContainer = findViewById(R.id.dynamicConditionsContainer);

        addContactButton = findViewById(R.id.addContactsButton);
        contactsContainer = findViewById(R.id.contactsContainer);
        dynamicContactsContainer = findViewById(R.id.dynamicContactsContainer);

        gotoMedicalActivityButton = findViewById(R.id.gotoMedicalActivity);
        saveChangesButton = findViewById(R.id.saveChangesButton);


        registerBloodTypeSpinner = findViewById(R.id.registerBloodTypeSpinner);
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter
                .createFromResource(this, R.array.blood_type_options, R.layout.spinner_item);
        bloodAdapter.setDropDownViewResource(R.layout.spinner_item);
        registerBloodTypeSpinner.setAdapter(bloodAdapter);


        registerOrganSpinner = findViewById(R.id.registerOrganSpinner);
        ArrayAdapter<CharSequence> organAdapter = ArrayAdapter
                .createFromResource(this, R.array.organ_donor_options,R.layout.spinner_item);
        organAdapter.setDropDownViewResource(R.layout.spinner_item);
        registerOrganSpinner.setAdapter(organAdapter);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        gotoMedicalActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MedicalProfileActivity.class));
            }
        });

        addAllergyButton.setOnClickListener(v -> addItemAllergy("", ""));
        addMedicationsButton.setOnClickListener(v -> addItemMedication("", ""));
        addConditionButton.setOnClickListener(v -> addItemCondition("", ""));
        addContactButton.setOnClickListener(v -> addItemContact("", "", ""));


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
                        registerInfo.setText(documentSnapshot.getString("info"));

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

//                        String organDonor = documentSnapshot.getString("organDonor");
//                        if (organDonor != null) {
//                            int organDonorPosition = organAdapter.getPosition(organDonor);
//                            registerOrganSpinner.setSelection(organDonorPosition);
//                        }

                        // retrieve and display allergies
                        documentReference.collection("allergies").get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(task.isSuccessful()){
                                   for (DocumentSnapshot allergyDoc : task.getResult()){
                                       addItemAllergy(allergyDoc.getString("name"), allergyDoc.getString("details"));
                                   }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: could not retrieve allergies" + e.getMessage());
                                Toast.makeText(CreateMedicalProfileActivity.this, "Allergies not retrieved", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // retrieve and display medications
                        documentReference.collection("medications").get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()){
                                    for (DocumentSnapshot medicationDoc: task.getResult()){
                                        addItemMedication(medicationDoc.getString("name"), medicationDoc.getString("details"));
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: could not retrieve medications" + e.getMessage());
                                Toast.makeText(CreateMedicalProfileActivity.this, "Medications not retrieved", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // retrieve and display conditions
                        documentReference.collection("conditions").get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()){
                                    for (DocumentSnapshot conditionDoc: task.getResult()){
                                        addItemCondition(conditionDoc.getString("name"), conditionDoc.getString("details"));
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: could not retrieve conditions" + e.getMessage());
                                Toast.makeText(CreateMedicalProfileActivity.this, "Conditions not retrieved", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // retrieve and display contacts
                        documentReference.collection("contacts").get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()){
                                            for (DocumentSnapshot contactDoc: task.getResult()){
                                                addItemContact(contactDoc.getString("name"),
                                                        contactDoc.getString("relation"),
                                                        contactDoc.getString("phone"));
                                            }
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "onFailure: could not retrieve contacts" + e.getMessage());
                                        Toast.makeText(CreateMedicalProfileActivity.this, "Contacts not retrieved", Toast.LENGTH_SHORT).show();
                                    }
                                });

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
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
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
                String info = registerInfo.getText().toString();

                // extract the selected values from the spinners
                String bloodType = registerBloodTypeSpinner.getSelectedItem().toString();
                String organDonor = registerOrganSpinner.getSelectedItem().toString();

                // combine address
                String fullAddress = firstlineAddress + ", " + secondlineAddress + ", " + cityAddress + ", " + postAddress;

                // validate data
                if (height.isEmpty()) {
                    registerHeight.setError("Height is required");
                    return;
                }
                if (weight.isEmpty()) {
                    registerWeight.setError("Weight is required");
                    return;
                }
                if (dob.isEmpty()) {
                    registerDOB.setError("Date of birth is required");
                    return;
                }
                if (postAddress.isEmpty()) {
                    registerPost.setError("Postal code is required");
                    return;
                }
                if (cityAddress.isEmpty()) {
                    registerCity.setError("City is required");
                    return;
                }
                if (firstlineAddress.isEmpty()) {
                    registerFirstLine.setError("First line of address is required");
                    return;
                }


                // check if user is authenticated
                user = fAuth.getCurrentUser();
                if (user != null) {
                    userID = fAuth.getCurrentUser().getUid();

                    DocumentReference documentReference = fStore.collection("profile").document(userID);
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("height", height);
                    profileData.put("weight", weight);
                    profileData.put("dob", dob);
                    profileData.put("address", fullAddress);
                    profileData.put("bloodType", bloodType);
                    profileData.put("organDonor", organDonor);
                    profileData.put("info", info);

                    //saving profiledata
                    documentReference.set(profileData)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "onSuccess: medical profile is created for " + userID);
                            Toast.makeText(CreateMedicalProfileActivity.this, "Medical Profile Updated", Toast.LENGTH_SHORT).show();
                        }
                    })
                            .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "onFailure: " + e.getMessage());
                            Toast.makeText(CreateMedicalProfileActivity.this, "Error:data could not be saved " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    // clear existing allergies in Firestore
                    documentReference.collection("allergies")
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                        snapshot.getReference().delete();
                                    }

                                    // Iterate through each allergy view and save to Firestore
                                    for (int i = 0; i < dynamicAllergiesContainer.getChildCount(); i++) {
                                        View view = dynamicAllergiesContainer.getChildAt(i);
                                        EditText nameEditText = view.findViewById(R.id.allergyNameEditText);
                                        EditText detailsEditText = view.findViewById(R.id.allergyDetailsEditText);

                                        String name = nameEditText.getText().toString().trim();
                                        String details = detailsEditText.getText().toString().trim();

                                        // Only save if both name and details are provided
                                        if (!name.isEmpty() && !details.isEmpty()) {
                                            Map<String, Object> allergyData = new HashMap<>();
                                            allergyData.put("name", name);
                                            allergyData.put("details", details);

                                            documentReference.collection("allergies")
                                                    .add(allergyData)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            Log.d(TAG, "onSuccess: allergy successfully saved");
//                                                            Toast.makeText(CreateMedicalProfileActivity.this, "Allergy saved successfully", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d(TAG, "onFailure: failed to save allergy" + e.getMessage());
                                                            Toast.makeText(CreateMedicalProfileActivity.this, "failed to save allergy", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(CreateMedicalProfileActivity.this, "Name and Details are required", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "onFailure: failed to delete existing allergies" + e.getMessage());
                                    Toast.makeText(CreateMedicalProfileActivity.this, "Failed to delete existing allergies", Toast.LENGTH_SHORT).show();
                                }
                            });

                    // clear existing medications in Firestore
                    documentReference.collection("medications")
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                        snapshot.getReference().delete();
                                    }

                                    // Iterate through each medications view and save to Firestore
                                    for (int i = 0; i < dynamicMedicationsContainer.getChildCount(); i++) {
                                        View view = dynamicMedicationsContainer.getChildAt(i);
                                        EditText nameEditText = view.findViewById(R.id.medicationNameEditText);
                                        EditText detailsEditText = view.findViewById(R.id.medicationDetailsEditText);

                                        String name = nameEditText.getText().toString().trim();
                                        String details = detailsEditText.getText().toString().trim();

                                        // Only save if both name and details are provided
                                        if (!name.isEmpty() && !details.isEmpty()) {
                                            Map<String, Object> medicationData = new HashMap<>();
                                            medicationData.put("name", name);
                                            medicationData.put("details", details);

                                            documentReference.collection("medications")
                                                    .add(medicationData)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            Log.d(TAG, "onSuccess: medication successfully saved");
//                                                            Toast.makeText(CreateMedicalProfileActivity.this, "Medication saved successfully", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d(TAG, "onFailure: failed to save medication" + e.getMessage());
                                                            Toast.makeText(CreateMedicalProfileActivity.this, "failed to save medication", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(CreateMedicalProfileActivity.this, "Name and Details are required", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "onFailure: failed to delete existing medications" + e.getMessage());
                                    Toast.makeText(CreateMedicalProfileActivity.this, "Failed to delete existing medications", Toast.LENGTH_SHORT).show();

                                }
                            });


                    // clear existing conditions in Firestore
                    documentReference.collection("conditions")
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                        snapshot.getReference().delete();
                                    }

                                    // Iterate through each condition view and save to Firestore
                                    for (int i = 0; i < dynamicConditionsContainer.getChildCount(); i++) {
                                        View view = dynamicConditionsContainer.getChildAt(i);
                                        EditText nameEditText = view.findViewById(R.id.conditionNameEditText);
                                        EditText detailsEditText = view.findViewById(R.id.conditionDetailsEditText);

                                        String name = nameEditText.getText().toString().trim();
                                        String details = detailsEditText.getText().toString().trim();

                                        // Only save if both name and details are provided
                                        if (!name.isEmpty() && !details.isEmpty()) {
                                            Map<String, Object> conditionData = new HashMap<>();
                                            conditionData.put("name", name);
                                            conditionData.put("details", details);

                                            documentReference.collection("conditions")
                                                    .add(conditionData)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            Log.d(TAG, "onSuccess: conditions successfully saved");
//                                                            Toast.makeText(CreateMedicalProfileActivity.this, "Conditions saved successfully", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d(TAG, "onFailure: failed to save conditions" + e.getMessage());
                                                            Toast.makeText(CreateMedicalProfileActivity.this, "failed to save conditions", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(CreateMedicalProfileActivity.this, "Name and Details are required", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "onFailure: failed to delete existing conditions" + e.getMessage());
                                    Toast.makeText(CreateMedicalProfileActivity.this, "Failed to delete existing conditions", Toast.LENGTH_SHORT).show();

                                }
                            });

                    // clear existing contacts in Firestore
                    documentReference.collection("contacts")
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                        snapshot.getReference().delete();
                                    }

                                    // Iterate through each contact view and save to Firestore
                                    for (int i = 0; i < dynamicContactsContainer.getChildCount(); i++) {
                                        View view = dynamicContactsContainer.getChildAt(i);
                                        EditText nameEditText = view.findViewById(R.id.contactNameEditText);
                                        EditText relationEditText = view.findViewById(R.id.contactRelationEditText);
                                        EditText phoneEditText = view.findViewById(R.id.contactPhoneEditText);

                                        String name = nameEditText.getText().toString().trim();
                                        String relation = relationEditText.getText().toString().trim();
                                        String phone = phoneEditText.getText().toString().trim();

                                        // Only save if both name, relation and phone are provided
                                        if (!name.isEmpty() && !relation.isEmpty() && !phone.isEmpty()) {
                                            Map<String, Object> contactData = new HashMap<>();
                                            contactData.put("name", name);
                                            contactData.put("relation", relation);
                                            contactData.put("phone", phone);

                                            documentReference.collection("contacts")
                                                    .add(contactData)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            Log.d(TAG, "onSuccess: contacts successfully saved");
//                                                            Toast.makeText(CreateMedicalProfileActivity.this, "Emergency contacts successfully saved", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d(TAG, "onFailure: failed to save contacts" + e.getMessage());
                                                            Toast.makeText(CreateMedicalProfileActivity.this, "failed to save contacts", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(CreateMedicalProfileActivity.this, "Name, Relation and Phone Number are required", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "onFailure: failed to delete existing contacts" + e.getMessage());
                                    Toast.makeText(CreateMedicalProfileActivity.this, "Failed to delete existing contacts", Toast.LENGTH_SHORT).show();
                                    

                                }
                            });


                } else {
                    Toast.makeText(CreateMedicalProfileActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: User not authenticated");
                }
            }
        });

    }

    private void addItemAllergy(String name, String details) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemAllergy = inflater.inflate(R.layout.item_allergy, dynamicAllergiesContainer, false);
        EditText allergyNameEditText = itemAllergy.findViewById(R.id.allergyNameEditText);
        EditText allergyDetailsEditText = itemAllergy.findViewById(R.id.allergyDetailsEditText);
        Button deleteAllergyButton = itemAllergy.findViewById(R.id.deleteAllergyButton);

        if (!name.isEmpty()) {
            allergyNameEditText.setText(name);
        } else {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
        }
        if (!details.isEmpty()) {
            allergyDetailsEditText.setText(details);
        } else {
            Toast.makeText(this, "Details are required", Toast.LENGTH_SHORT).show();
        }

        dynamicAllergiesContainer.addView(itemAllergy);

        deleteAllergyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dynamicAllergiesContainer.removeView(itemAllergy);
            }
        });

    }

    private void addItemMedication(String name, String details) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemMedication = inflater.inflate(R.layout.item_medication, dynamicMedicationsContainer, false);
        EditText medicationNameEditText = itemMedication.findViewById(R.id.medicationNameEditText);
        EditText medicationDetailsEditText = itemMedication.findViewById(R.id.medicationDetailsEditText);
        Button deleteMedicationButton = itemMedication.findViewById(R.id.deleteMedicationButton);

        if (!name.isEmpty()) {
            medicationNameEditText.setText(name);
        } else {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
        }
        if (!details.isEmpty()) {
            medicationDetailsEditText.setText(details);
        } else {
            Toast.makeText(this, "Details are required", Toast.LENGTH_SHORT).show();
        }

        dynamicMedicationsContainer.addView(itemMedication);

        deleteMedicationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dynamicMedicationsContainer.removeView(itemMedication);
            }
        });

    }

    private void addItemCondition(String name, String details) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemCondition = inflater.inflate(R.layout.item_condition, dynamicConditionsContainer, false);
        EditText conditionNameEditText = itemCondition.findViewById(R.id.conditionNameEditText);
        EditText conditionDetailsEditText = itemCondition.findViewById(R.id.conditionDetailsEditText);
        Button deleteConditionButton = itemCondition.findViewById(R.id.deleteConditionButton);

        if (!name.isEmpty()) {
            conditionNameEditText.setText(name);
        } else {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
        }
        if (!details.isEmpty()) {
            conditionDetailsEditText.setText(details);
        } else {
            Toast.makeText(this, "Details are required", Toast.LENGTH_SHORT).show();
        }

        dynamicConditionsContainer.addView(itemCondition);

        deleteConditionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dynamicConditionsContainer.removeView(itemCondition);
            }
        });
    }
    private void addItemContact(String name, String relation, String phone) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemContact = inflater.inflate(R.layout.item_contact, dynamicContactsContainer, false);
        EditText contactNameEditText = itemContact.findViewById(R.id.contactNameEditText);
        EditText contactRelationEditText = itemContact.findViewById(R.id.contactRelationEditText);
        EditText contactPhoneEditText = itemContact.findViewById(R.id.contactPhoneEditText);
        Button deleteContactButton = itemContact.findViewById(R.id.deleteContactButton);

        if (!name.isEmpty()) {
            contactNameEditText.setText(name);
        } else {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
        }
        if (!relation.isEmpty()) {
            contactRelationEditText.setText(relation);
        } else {
            Toast.makeText(this, "Relation to contact is required", Toast.LENGTH_SHORT).show();
        }
        if (!phone.isEmpty()) {
            contactPhoneEditText.setText(phone);
        } else {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
        }

        dynamicContactsContainer.addView(itemContact);

        deleteContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dynamicContactsContainer.removeView(itemContact);
            }
        });
    }
}