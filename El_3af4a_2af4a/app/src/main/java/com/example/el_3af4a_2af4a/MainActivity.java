package com.example.el_3af4a_2af4a;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;


import androidx.annotation.NonNull;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;

    private TextView signup;
    private Button start;
    private Button google;
    private EditText email, password;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isPasswordVisible = false; // Track password visibility

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linear_sign_in);

        FirebaseApp.initializeApp(this);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("744666409410-rvp5ec88h019mig8un92qm3lgej8sbmj")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // *** IMPORTANT: Assign to the global instance!
        AppUtils.mGoogleSignInClient = mGoogleSignInClient;

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, proceed to the map activity
            startMapActivity();
        }

        signup = (TextView)findViewById(R.id.SIGNUP);
        start = (Button)findViewById(R.id.sign);
        google = (Button)findViewById(R.id.google);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, sign_up.class);
                startActivity(i);
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String em = email.getText().toString().trim();
                String pas = password.getText().toString().trim();

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

                signIn(em, pas);
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAuth.getCurrentUser() != null) {
                    signOut();
                }
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

    private void signIn(String em, String pas) {
        mAuth.signInWithEmailAndPassword(em, pas)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            Intent i = new Intent(MainActivity.this, map.class);
                            startActivity(i);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Invalid Email or Password",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                //String email = account.getEmail(); // Get the email address!
                //firebaseAuthWithGoogle(account.getIdToken(), email); // Pass email
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.d("MainActivity", "Google sign in failed", e);
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
                            Toast.makeText(MainActivity.this, "Google sign in successful", Toast.LENGTH_SHORT).show();
                            startMapActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
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

//    private void firebaseAuthWithGoogle(String idToken, String email) {
//        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//
//        mAuth.fetchSignInMethodsForEmail(email)
//                .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
//                        if (task.isSuccessful()) {
//                            boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
//
//                            // **CORRECT LOGIC**
//                            if (isNewUser) {
//                                // New user, prevent automatic signup
//                                mAuth.signOut();
//                                AppUtils.mGoogleSignInClient.signOut();
//                                Toast.makeText(MainActivity.this, "This Google account is not signed up. Please sign up first.", Toast.LENGTH_LONG).show();
//                                Intent i = new Intent(MainActivity.this, sign_up.class);
//                                startActivity(i);
//                            } else {
//                                // Existing user, proceed with sign-in
//                                mAuth.signInWithCredential(credential)
//                                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
//                                            @Override
//                                            public void onComplete(@NonNull Task<AuthResult> task) {
//                                                if (task.isSuccessful()) {
//                                                    // Sign in success
//                                                    startMapActivity();
//                                                } else {
//                                                    // Sign-in failed
//                                                    Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
//                                                    Log.e("MainActivity", "Authentication failed.", task.getException());
//                                                }
//                                            }
//                                        });
//                            }
//
//                        } else {
//                            // Error checking user existence
//                            Toast.makeText(MainActivity.this, "Error checking user existence.", Toast.LENGTH_SHORT).show();
//                            Log.e("MainActivity", "Error checking user existence.", task.getException());
//                        }
//                    }
//                });
//    }

//    public void firebaseAuthWithGoogle(String idToken) {
//        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//        firebaseAuth.signInWithCredential(credential)
//                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (task.isSuccessful()) {
//                            FirebaseUser user = firebaseAuth.getCurrentUser();
//                            if (user != null) {
//                                checkIfNewUser(user.getUid());
//                            }
//                        } else {
//                            // If sign in fails, display a message to the user.
//                            Log.w(TAG, "signInWithCredential:failure", task.getException());
//                        }
//                    }
//                });
//    }
//
//    private void checkIfNewUser(String uid) {
//        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
//        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Log.d(TAG, "Snapshot content: " + snapshot.toString()); // Add this line to log the snapshot contents
//                boolean isNewUser = !snapshot.exists();
//                Log.d(TAG, "isNewUser: " + isNewUser);
//                if (isNewUser) {
//                    // Handle new user sign-up
//                    createUserInDatabase(uid);
//                } else {
//                    // Handle existing user sign-in
//                    goToHomeActivity();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.w(TAG, "checkIfNewUser:onCancelled", error.toException());
//            }
//        });
//    }


//    private void firebaseAuthWithGoogle(String idToken, String email) {
//        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//        mAuth.signInWithCredential(credential)
//                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (task.isSuccessful()) {
//                            // Check if the user is new
//                            FirebaseUser user = mAuth.getCurrentUser();
//                            if (user != null && user.getMetadata().getCreationTimestamp() == user.getMetadata().getLastSignInTimestamp()) {
//                                // User is new, show a message and sign out
//                                Toast.makeText(MainActivity.this, "This Google account is not signed up.", Toast.LENGTH_SHORT).show();
//                                deleteUser(user); // Delete user from Firebase
//                                //mGoogleSignInClient.signOut(); // Sign out to prevent automatic sign-in
//                            } else {
//                                // User is existing, proceed to map activity
//                                Intent i = new Intent(MainActivity.this, map.class);
//                                startActivity(i);
//                            }
//                        } else {
//                            // Sign in failed
//                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//    }
//
//    private void deleteUser(FirebaseUser user) {
//        user.delete()
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        if (task.isSuccessful()) {
//                            // Sign out from Google
//                            mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
//                                @Override
//                                public void onComplete(@NonNull Task<Void> task) {
//                                    Log.d("MainActivity", "Account deleted successfully.");
//                                }
//                            });
//                        } else {
//                            Log.d("MainActivity", "Account is failed to be deleted.");
//                        }
//                    }
//                });
//    }

    private void startMapActivity() {
        Intent intent = new Intent(MainActivity.this, map.class);
        startActivity(intent);
        finish(); // Optional: Finish MainActivity to prevent going back
        // to the login screen with the back button
    }
}