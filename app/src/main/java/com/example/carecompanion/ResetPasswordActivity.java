package com.example.carecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText currentUserPassword, newUserPassword, newConfirmPassword;
    Button resetPasswordButton, returnSettingsButton;
    FirebaseUser user;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        currentUserPassword = findViewById(R.id.currentUserPassword);
        newUserPassword = findViewById(R.id.newUserPassword);
        newConfirmPassword = findViewById(R.id.newConfirmPassword);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        returnSettingsButton = findViewById(R.id.returnSettingsButton);

        returnSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                finish();
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        auth = FirebaseAuth.getInstance();

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(newUserPassword.getText().toString().isEmpty()){
                    newUserPassword.setError("Required Field");
                    return;
                }

                if(newConfirmPassword.getText().toString().isEmpty()){
                    newConfirmPassword.setError("Required Field");
                    return;
                }

                if(!newUserPassword.getText().toString().equals(newConfirmPassword.getText().toString())){
                    newUserPassword.setError("New Passwords do not match");
                    return;
                }
                // Validate password strength
                if (!isValidPassword(newUserPassword.getText().toString())) {
                    newUserPassword.setError("Password must be at least 8 characters long, contain uppercase, lowercase, number, and special character.");
                    return;
                }

                reauthenticateAndChangePassword(currentUserPassword.getText().toString(), newUserPassword.getText().toString());

            }
        });

    }

    private boolean isValidPassword(String password) {
        // Password pattern: Minimum 8 characters, at least one uppercase letter, one lowercase letter, one number, and one special character
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }

    private void reauthenticateAndChangePassword(String currentUserPassword, String newUserPassword) {
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentUserPassword);

            user.reauthenticate(credential).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    user.updatePassword(newUserPassword).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(ResetPasswordActivity.this, "Password Updated", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ResetPasswordActivity.this, "Password update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ResetPasswordActivity.this, "Reauthentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User not logged in or email not available", Toast.LENGTH_SHORT).show();
        }
    }
}