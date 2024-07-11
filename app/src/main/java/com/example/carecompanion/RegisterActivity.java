package com.example.carecompanion;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    public static final String TAG = "TAG";
    EditText registerForename, registerSurnames, registerEmail, registerCCode, registerPhone, registerPassword, registerConfirmPassword;
    Button registerButton, gotoLogin;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerForename = findViewById(R.id.registerForename);
        registerSurnames = findViewById(R.id.registerSurnames);
        registerEmail = findViewById(R.id.registerEmail);
        registerCCode = findViewById(R.id.registerCCode);
        registerPhone = findViewById(R.id.registerPhone);
        registerPassword = findViewById(R.id.registerPassword);
        registerConfirmPassword = findViewById(R.id.registerConfirmPassword);
        registerButton = findViewById(R.id.registerButton);
        gotoLogin = findViewById(R.id.gotoLogin);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        gotoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // extract the data from the form
                String forename = registerForename.getText().toString();
                String surnames = registerSurnames.getText().toString();
                String email = registerEmail.getText().toString();
                String ccode = registerCCode.getText().toString();
                String phone = registerPhone.getText().toString();
                String password = registerPassword.getText().toString();
                String confirmPassword = registerConfirmPassword.getText().toString();

                if (forename.isEmpty()){
                    registerForename.setError("First name required");
                    return;
                }
                if (surnames.isEmpty()){
                    registerSurnames.setError("Surname(s) required");
                    return;
                }

                if (email.isEmpty()){
                    registerEmail.setError("Email is required");
                    return;
                }

                if (ccode.isEmpty()){
                    registerCCode.setError("Country Code is required");
                    return;
                }

                if (phone.isEmpty()){
                    registerPhone.setError("Phone is required");
                    return;
                }

                if (password.isEmpty()){
                    registerPassword.setError("Password is required");
                    return;
                }

                if (confirmPassword.isEmpty()){
                    registerConfirmPassword.setError("Please confirm your password");
                    return;
                }

                if(!password.equals(confirmPassword)){
                    registerConfirmPassword.setError("Passwords do not match");
                    return;
                }

                // data is validated
                // register the user

                fAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // send user to main page
                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();

                        // create user collection in firestore
                        userID = fAuth.getCurrentUser().getUid();

                        // Public data
                        DocumentReference publicDocRef = fStore.collection("public").document(userID);
                        Map<String, Object> publicData = new HashMap<>();
                        publicData.put("forename", forename);
                        publicData.put("ccode", ccode);
                        publicData.put("phone", phone);

                        // Private data
                        DocumentReference privateDocRef = fStore.collection("private").document(userID);
                        Map<String, Object> privateData = new HashMap<>();
                        privateData.put("surnames", surnames);
                        privateData.put("email", email);

                        // Save public data
                        publicDocRef.set(publicData).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "onSuccess: user public profile is created for " + userID);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e.getMessage());
                            }
                        });

                        // Save private data
                        privateDocRef.set(privateData).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "onSuccess: user private profile is created for " + userID);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e.getMessage());
                            }
                        });

                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });
    }
}



//public class RegisterActivity extends AppCompatActivity {
//
//    public static final String TAG = "TAG";
//    EditText registerForename, registerSurnames, registerEmail, registerCCode, registerPhone, registerPassword, registerConfirmPassword;
//    Button registerButton, gotoLogin;
//    FirebaseAuth fAuth;
//    FirebaseFirestore fStore;
//    String userID;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_register);
//
//        registerForename = findViewById(R.id.registerForename);
//        registerSurnames = findViewById(R.id.registerSurnames);
//        registerEmail = findViewById(R.id.registerEmail);
//        registerCCode = findViewById(R.id.registerCCode);
//        registerPhone = findViewById(R.id.registerPhone);
//        registerPassword = findViewById(R.id.registerPassword);
//        registerConfirmPassword = findViewById(R.id.registerConfirmPassword);
//        registerButton = findViewById(R.id.registerButton);
//        gotoLogin = findViewById(R.id.gotoLogin);
//
//        fAuth = FirebaseAuth.getInstance();
//        fStore = FirebaseFirestore.getInstance();
//
//        gotoLogin.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
//                finish();
//            }
//        });
//
//        registerButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // extract the data from the form
//                String forename = registerForename.getText().toString();
//                String surnames = registerSurnames.getText().toString();
//                String email = registerEmail.getText().toString();
//                String ccode = registerCCode.getText().toString();
//                String phone = registerPhone.getText().toString();
//                String password = registerPassword.getText().toString();
//                String confirmPassword = registerConfirmPassword.getText().toString();
//
//                if (forename.isEmpty()){
//                    registerForename.setError("First name required");
//                    return;
//                }
//                if (surnames.isEmpty()){
//                    registerSurnames.setError("Surname(s) required");
//                    return;
//                }
//
//                if (email.isEmpty()){
//                    registerEmail.setError("Email is required");
//                    return;
//                }
//
//                if (ccode.isEmpty()){
//                    registerCCode.setError("Country Code is required");
//                    return;
//                }
//
//                if (phone.isEmpty()){
//                    registerPhone.setError("Phone is required");
//                    return;
//                }
//
//                if (password.isEmpty()){
//                    registerPassword.setError("Password is required");
//                    return;
//                }
//
//                if (confirmPassword.isEmpty()){
//                    registerConfirmPassword.setError("Please confirm your password");
//                    return;
//                }
//
//                if(!password.equals(confirmPassword)){
//                    registerConfirmPassword.setError("Passwords do not match");
//                    return;
//                }
//
//                // data is validated
//                // register the user
//
//                fAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
//                    @Override
//                    public void onSuccess(AuthResult authResult) {
//                        // send user to main page
//                        Toast.makeText(RegisterActivity.this, "Registration Successfull", Toast.LENGTH_SHORT).show();
//
//                        // create user collection in firestore
//                        userID = fAuth.getCurrentUser().getUid();
//                        DocumentReference documentReference = fStore.collection("users").document(userID);
//
//                        Map<String, Object> user = new HashMap<>();
//                        user.put("forename",forename);
//                        user.put("surnames", surnames);
//                        user.put("email", email);
//                        user.put("ccode", ccode);
//                        user.put("phone", phone);
//
//                        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void unused) {
//                                Log.d(TAG, "onSuccess: user profile is created for "+ userID);
//                            }
//                        }).addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.d(TAG, "onFailure: " + e.getMessage());
//                            }
//                        });
//
//                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
//                        finish();
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//
//            }
//        });
//    }
//}