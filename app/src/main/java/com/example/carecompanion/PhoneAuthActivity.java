package com.example.carecompanion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    public static final String TAG = "PhoneAuthActivity";
    EditText cCode, phoneNumber, inputOTP;
    Button sendOTPButton, resendOTPButton, verifyOTPButton, gotoLoginActivityButton;
    String userPhoneNumber, verificationID;
    FirebaseAuth fAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    PhoneAuthProvider.ForceResendingToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        cCode = findViewById(R.id.cCode);
        phoneNumber = findViewById(R.id.phoneNumber);
        inputOTP = findViewById(R.id.inputOTP);
        sendOTPButton = findViewById(R.id.sendOTPButton);
        resendOTPButton = findViewById(R.id.resendOTPButton);
        verifyOTPButton = findViewById(R.id.verifyOTPButton);
        gotoLoginActivityButton = findViewById(R.id.gotoLoginActivityButton);

        FirebaseApp.initializeApp(this);
        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();

        // Check if user is already authenticated

        if (currentUser != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        } else {
            Log.d(TAG, "User not authenticated");
        }

        gotoLoginActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });

        sendOTPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cCode.getText().toString().isEmpty()) {
                    cCode.setError("Required field");
                    return;
                }
                if (phoneNumber.getText().toString().isEmpty()) {
                    phoneNumber.setError("Required field");
                    return;
                }

                // Construct the full phone number
                userPhoneNumber = "+" + cCode.getText().toString() + phoneNumber.getText().toString();

                // Check if user exists in Firestore
                db.collection("users")
                        .whereEqualTo("ccode", cCode.getText().toString())
                        .whereEqualTo("phone", phoneNumber.getText().toString())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    if (!task.getResult().isEmpty()) {
                                        // User exists, send OTP for login
                                        verifyPhoneNumber(userPhoneNumber);
                                        Toast.makeText(getApplicationContext(), "Sending OTP to " + userPhoneNumber, Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "onComplete: sending OTP");
                                    } else {
                                        // User does not exist or combination of ccode and phone not found
                                        Toast.makeText(getApplicationContext(), "User does not exist or invalid combination of country code and phone number.", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "onComplete: user does not exist or invalid combination of country code and phone number");
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "Error checking user existence: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "onComplete: error checking user existence" + task.getException().getMessage());
                                    Log.e(TAG, "onComplete: error checking user existencse", task.getException());
                                }
                            }
                        });
            }
        });

        resendOTPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userPhoneNumber != null) {
                    verifyPhoneNumber(userPhoneNumber);
                    resendOTPButton.setEnabled(false);
                } else {
                    Toast.makeText(getApplicationContext(), "User phone number not set.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        verifyOTPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputOTP.getText().toString().isEmpty()) {
                    inputOTP.setError("Enter OTP");
                    return;
                }
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationID, inputOTP.getText().toString());
                authenticateUser(credential);
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                authenticateUser(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(PhoneAuthActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onVerificationFailed: " + e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                verificationID = s;
                token = forceResendingToken;

                cCode.setVisibility(View.GONE);
                phoneNumber.setVisibility(View.GONE);
                sendOTPButton.setVisibility(View.GONE);

                inputOTP.setVisibility(View.VISIBLE);
                verifyOTPButton.setVisibility(View.VISIBLE);
                resendOTPButton.setVisibility(View.VISIBLE);

                resendOTPButton.setEnabled(false);
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                resendOTPButton.setEnabled(true);
            }
        };
    }

    public void verifyPhoneNumber(String phoneNum) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(fAuth)
                .setActivity(this)
                .setPhoneNumber(phoneNum)
                .setTimeout(30L, TimeUnit.SECONDS)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void authenticateUser(PhoneAuthCredential credential) {
        fAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // User authentication successful, proceed to MainActivity
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // User authentication failed
                        Toast.makeText(PhoneAuthActivity.this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Authentication failed: " + e.getMessage());
                    }
                });
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }
}
