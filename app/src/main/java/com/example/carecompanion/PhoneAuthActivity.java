package com.example.carecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    public static final String TAG = "PhoneAuthActivity";
    EditText cCode, phoneNumber, inputOTP, emailEnter;
    Button sendOTPButton, resendOTPButton, verifyOTPButton, gotoLoginActivityButton, sendEmailButton;
    String userPhoneNumber, verificationID;
    FirebaseAuth fAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;
    TextView textView4;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    PhoneAuthProvider.ForceResendingToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        cCode = findViewById(R.id.cCode);
        phoneNumber = findViewById(R.id.phoneNumber);
        inputOTP = findViewById(R.id.inputOTP);
        emailEnter = findViewById(R.id.emailEnter);
        textView4 = findViewById(R.id.textView4);

        sendOTPButton = findViewById(R.id.sendOTPButton);
        resendOTPButton = findViewById(R.id.resendOTPButton);
        verifyOTPButton = findViewById(R.id.verifyOTPButton);
        gotoLoginActivityButton = findViewById(R.id.gotoLoginActivityButton);
        sendEmailButton = findViewById(R.id.sendEmailButton);

        FirebaseApp.initializeApp(this);
        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
            }
        });

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

                verifyPhoneNumber(userPhoneNumber);

                //authenticate anonymously
                fAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            //annoymous sign-in successful, check if user exists

                            db.collection("private")
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
                                            }
                                        }
                                    });
                        } else {

                            Log.e(TAG, "Anonymous authentication failed: " + task.getException().getMessage());
                            Toast.makeText(PhoneAuthActivity.this, "Anonymous authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(PhoneAuthActivity.this, "Authenticated method failed", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onFailure: Authentication method failed" + e.getMessage() );
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

                emailEnter.setVisibility(View.VISIBLE);
                sendEmailButton.setVisibility(View.VISIBLE);
                textView4.setVisibility(View.VISIBLE);
            }
        });

        sendEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEnter.getText().toString().trim();
//                validate the email address
                if (emailEnter.getText().toString().isEmpty()){
                    emailEnter.setError("Required Field");
                    return;
                } else if (!isValidEmail(email)){
                    emailEnter.setError("Invalid email address");
                    return;
                }

                // send the reset link
                fAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(PhoneAuthActivity.this, "Reset email sent", Toast.LENGTH_SHORT).show();
                        emailEnter.setText(""); // Clear the email field
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(PhoneAuthActivity.this, "Resent email unable to send", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onFailure: Failed to send reset email", e);
                    }
                });
            }
        });


        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {


                emailEnter.setVisibility(View.VISIBLE);
                sendEmailButton.setVisibility(View.VISIBLE);
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


    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            startActivity(new Intent(getApplicationContext(), PhoneAuthActivity.class));
            finish();
        }
    }
}