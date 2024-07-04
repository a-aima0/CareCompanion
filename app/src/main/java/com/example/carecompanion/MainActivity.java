package com.example.carecompanion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    TextView verifyEmailBanner;
    Button logoutButton, verifyEmailButton;
    FirebaseAuth fAuth;
    AlertDialog.Builder resetEmailAlert, deleteAlert;
    LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoutButton = findViewById(R.id.logoutButton);
        verifyEmailButton = findViewById(R.id.verifyEmailButton);
        verifyEmailBanner = findViewById(R.id.verifyEmailBanner);

        resetEmailAlert = new AlertDialog.Builder(this);
        deleteAlert = new AlertDialog.Builder(this);

        inflater = this.getLayoutInflater();

        fAuth = FirebaseAuth.getInstance();

        if (!fAuth.getCurrentUser().isEmailVerified()){
            verifyEmailButton.setVisibility(View.VISIBLE);
            verifyEmailBanner.setVisibility(View.VISIBLE);
        }

        verifyEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send verification email
                fAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(MainActivity.this, "Verification Email Sent", Toast.LENGTH_SHORT).show();

                        verifyEmailButton.setVisibility(View.GONE);
                        verifyEmailBanner.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failure to send verification: " +e.getMessage(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh user information
        FirebaseUser user = fAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (user.isEmailVerified()) {
                        verifyEmailButton.setVisibility(View.GONE);
                        verifyEmailBanner.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.resetUserPassword){
            startActivity(new Intent(getApplicationContext(), ResetPasswordActivity.class));
        }

        if(item.getItemId() == R.id.updateEmailMenu){
            // start alert (pop up) dialog
            View view = inflater.inflate(R.layout.reset_email_password_alert, null);

            EditText email = view.findViewById(R.id.resetPasswordEmailAlert);

            resetEmailAlert.setTitle("Update Email")
                    .setMessage("Enter new email address")
                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // validate the email address
                            String newEmail = email.getText().toString().trim();
                            if (newEmail.isEmpty()){
                                email.setError("Required Field");
                                return;
                            }

                            // Update email
                            FirebaseUser user = fAuth.getCurrentUser();
                            if (user != null) {
                                user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(MainActivity.this, "Email update requested. Check your email for verification.", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Email update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    })
                    .setNegativeButton("Return", null)
                    .setView(view)
                    .create().show();
        }

        if (item.getItemId()==R.id.deleteAccountMenu){
            deleteAlert.setTitle("Delete account permanently")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Delete Account", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FirebaseUser user = fAuth.getCurrentUser();
                            user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(MainActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                                    fAuth.signOut();
                                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).setNegativeButton("Cancel", null)
                    .create()
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }
}
