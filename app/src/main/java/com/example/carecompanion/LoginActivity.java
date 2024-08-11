package com.example.carecompanion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = null;
    EditText loginEmail, loginPassword;
    Button createAccountButton, loginButton, forgetPasswordButton;
    FirebaseAuth fAuth;
    FirebaseUser currentUser;
    AlertDialog.Builder resetPasswordAlert;
    LayoutInflater inflater;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        fAuth = FirebaseAuth.getInstance();
        currentUser = fAuth.getCurrentUser();
        resetPasswordAlert = new AlertDialog.Builder(this);
        inflater = this.getLayoutInflater();


        createAccountButton = findViewById(R.id.createAccountButton);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            }
        });

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);

        forgetPasswordButton = findViewById(R.id.forgetPasswordButton);

        forgetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = resetPasswordAlert.setTitle("Reset Forgotten Password")
                        .setMessage("Click the button and follow the instructions to reset your forgotten password")
                        .setNegativeButton("Return", null)
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(getApplicationContext(), PhoneAuthActivity.class));
                            }
                        })
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

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // extract and validate
                if (loginEmail.getText().toString().isEmpty()){
                    loginEmail.setError("Email is required");
                    return;
                }

                if (loginPassword.getText().toString().isEmpty()){
                    loginPassword.setError("Password is required");
                    return;
                }

                // data is validated
                // login user
                fAuth.signInWithEmailAndPassword(loginEmail.getText().toString(), loginPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // login is successfull
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onFailure: Authentication failed: " + e.getMessage() );
                    }
                });

            }
        });




    }

    @Override
    protected void onStart() {
        super.onStart();
        if(currentUser != null){
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }
}