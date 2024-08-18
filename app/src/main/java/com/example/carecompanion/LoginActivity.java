package com.example.carecompanion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "LoginActivity";
    int RC_SIGN_IN = 1499;

    // Views
    EditText loginEmail, loginPassword;
    Button createAccountButton, loginButton, forgetPasswordButton, googleLoginButton;
    FirebaseAuth fAuth;
    FirebaseUser currentUser;
    AlertDialog.Builder resetPasswordAlert, resetEmailAlert;
    LayoutInflater inflater;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        fAuth = FirebaseAuth.getInstance();
        currentUser = fAuth.getCurrentUser();
        resetPasswordAlert = new AlertDialog.Builder(this);
        inflater = this.getLayoutInflater();
        fStore = FirebaseFirestore.getInstance();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        gsc = GoogleSignIn.getClient(LoginActivity.this, gso);

        googleLoginButton = findViewById(R.id.googleLoginButton);
        googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignIn();
            }
        });

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
                loginWithEmail();
            }
        });
    }


    private void loginWithEmail() {
        if (loginEmail.getText().toString().isEmpty()) {
            loginEmail.setError("Email is required");
            return;
        }

        if (loginPassword.getText().toString().isEmpty()) {
            loginPassword.setError("Password is required");
            return;
        }

        fAuth.signInWithEmailAndPassword(loginEmail.getText().toString(), loginPassword.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onFailure: Authentication failed: " + e.getMessage());
                    }
                });
    }

    private void SignIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    handleSignInWithGoogle(account);
                } else {
                    Toast.makeText(this, "Sign-In failed: No account returned", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Sign-In failed, statuscode: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Sign-In failed with status code: " + e.getStatusCode(), e);
            }
        } else {
            Toast.makeText(this, "Request code does not match RC_SIGN_IN", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSignInWithGoogle(GoogleSignInAccount account) {
        String email = account.getEmail();
        Log.d(TAG, "Handling Google Sign-In for email: " + email);

        fAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                    @Override
                    public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                        if (task.isSuccessful()) {
                            boolean emailAuthExists = !task.getResult().getSignInMethods().isEmpty();
                            if (emailAuthExists) {
                                View view = inflater.inflate(R.layout.reset_email_password_alert, null);

                                EditText email = view.findViewById(R.id.resetPasswordEmailAlert);

                                AlertDialog alertDialog = resetEmailAlert.setTitle("Account Merg")
                                        .setMessage("An EmailAuth account with this email exists. Signing in with Gmail will take priority and you will only be able to sign in with Gmail from now on. Do you want to continue?")
                                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setNegativeButton("Return", null)
                                        .setView(view)
                                        .create();

                                alertDialog.getContext().setTheme(R.style.AlertDialogTheme);
                                alertDialog.show();

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
                            } else {
                                signInWithGoogle(account.getIdToken());
                            }
                        } else {
                            Log.e(TAG, "Error checking sign-in methods", task.getException());
                            Toast.makeText(LoginActivity.this, "Error checking sign-in methods", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void signInWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        fAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = fAuth.getCurrentUser();
                            if (user != null) {
                                DocumentReference privateDocRef = fStore.collection("private").document(user.getUid());
                                Map<String, Object> privateSet = new HashMap<>();
                                privateSet.put("email", user.getEmail());
//                                privateSet.put("surnames", user.getDisplayName() != null ? "" : user.getDisplayName());
//                                privateSet.put("forename", user.getDisplayName());

                                privateDocRef.set(privateSet, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "User private profile created for " + user);

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "Error creating user profile: " + e.getMessage());
                                    }
                                });
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Google Sign-In failed", task.getException());
                        }
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

