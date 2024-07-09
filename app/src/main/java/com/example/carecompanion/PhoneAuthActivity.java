package com.example.carecompanion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    EditText cCode, phoneNumber, inputOTP;
    Button sendOTPButton, resendOTPButton, verifyOTPButton, gotoLoginActivityButton;
    String userPhoneNumber, verificationID;
    FirebaseAuth fAuth;
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
        resendOTPButton.setEnabled(false);

        verifyOTPButton = findViewById(R.id.verifyOTPButton);
        gotoLoginActivityButton = findViewById(R.id.gotoLoginActivityButton);

        fAuth = FirebaseAuth.getInstance();


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
                if(cCode.getText().toString().isEmpty()){
                    cCode.setError("Required field");
                    return;
                }
                if(phoneNumber.getText().toString().isEmpty()){
                    phoneNumber.setError("Required field");
                    return;
                }
                userPhoneNumber = "+"+ cCode.getText().toString() + " " + phoneNumber.getText().toString();
                verifyPhoneNumber(userPhoneNumber);
                Toast.makeText(getApplicationContext(), userPhoneNumber, Toast.LENGTH_SHORT).show();

            }
        });

        resendOTPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPhoneNumber(userPhoneNumber);
                resendOTPButton.setEnabled(false);
            }
        });

        verifyOTPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the OTP
                if(inputOTP.getText().toString().isEmpty()){
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

    public void verifyPhoneNumber (String phoneNum){
        // send OTP
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(fAuth)
                .setActivity(this)
                .setPhoneNumber(phoneNum)
                .setTimeout(30L, TimeUnit.SECONDS)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void authenticateUser(PhoneAuthCredential credential){
        fAuth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Toast.makeText(PhoneAuthActivity.this, "Login successfull", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PhoneAuthActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser() != null ){
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }
}

