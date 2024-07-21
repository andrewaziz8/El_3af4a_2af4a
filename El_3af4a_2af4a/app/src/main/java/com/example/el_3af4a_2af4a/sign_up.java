package com.example.el_3af4a_2af4a;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;



public class sign_up extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;

    private TextView sign_in;
    private Button start;
    private Button google;
    private EditText email, password, rewrite_password;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isPasswordVisible = false;
    private boolean isRePasswordVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linear_sign_up);
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("744666409410-rvp5ec88h019mig8un92qm3lgej8sbmj")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        sign_in = (TextView)findViewById(R.id.SIGNUP);
        start = (Button)findViewById(R.id.getstarted);
        google = (Button)findViewById(R.id.google);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        rewrite_password = findViewById(R.id.rewrite_password);

        sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(sign_up.this, MainActivity.class);
                startActivity(i);
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String em = email.getText().toString().trim();
                String pas = password.getText().toString().trim();
                String re_pas = rewrite_password.getText().toString().trim();

                if (em.isEmpty()) {
                    email.setError("Email is required");
                    email.requestFocus();
                    return;
                }

                if (pas.isEmpty()) {   //More constraints on password
                    password.setError("Password is required");
                    password.requestFocus();
                    return;
                }

                if (re_pas.isEmpty() || !pas.equals(re_pas)) {
                    rewrite_password.setError("Password does not match");
                    rewrite_password.requestFocus();
                    return;
                }

                // Call the Firebase sign-up method
                signUpUser(em, pas);
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAuth.getCurrentUser() != null) {
                    signOut();
                }
                //signOut();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        // Set the onTouchListener for password visibility toggle
        password.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (password.getRight() - password.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        togglePasswordVisibility();
                        password.performClick(); // Call performClick() to handle click event
                        return true;
                    }
                }
                return false;
            }
        });

        // Set the onTouchListener for password visibility toggle
        rewrite_password.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (rewrite_password.getRight() - rewrite_password.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        toggleRePasswordVisibility();
                        rewrite_password.performClick(); // Call performClick() to handle click event
                        return true;
                    }
                }
                return false;
            }
        });

    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide Password
            password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_off_24, 0);
        } else {
            // Show Password
            password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_24, 0);
        }
        isPasswordVisible = !isPasswordVisible;
        // Move the cursor to the end of the text
        password.setSelection(password.getText().length());
    }

    private void toggleRePasswordVisibility() {
        if (isRePasswordVisible) {
            // Hide Password
            rewrite_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            rewrite_password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_off_24, 0);
        } else {
            // Show Password
            rewrite_password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            rewrite_password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_24, 0);
        }
        isRePasswordVisible = !isRePasswordVisible;
        // Move the cursor to the end of the text
        rewrite_password.setSelection(rewrite_password.getText().length());
    }

    private void signUpUser(String em, String pas) {
        mAuth.createUserWithEmailAndPassword(em, pas)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success, update UI with the signed-in user's information
                            Toast.makeText(sign_up.this, "Sign up successful!",
                                    Toast.LENGTH_SHORT).show();

                            // Proceed to the next activity or perform other actions
                            Intent intent = new Intent(sign_up.this, map.class);
                            startActivity(intent);
                            finish(); // Close the sign-up activity
                        } else {
                            // If sign up fails, display a message to the user.
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                // Email already in use
                                Toast.makeText(sign_up.this, "This email address is already in use.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(sign_up.this, "Sign up failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.d("sign_up", "Google sign up failed", e);
                String errorMessage = e.getMessage(); // Get the specific error message from ApiException
                Toast.makeText(this, "Google sign up failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(sign_up.this, "Google sign up successful", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(sign_up.this, map.class);
                            startActivity(i);
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(sign_up.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                task -> {
                    // User is now signed out from Google account
                });
        mAuth.signOut();
    }

}
