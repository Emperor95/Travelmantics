package com.emperor.travel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText edEmail, edPassword;
    private Button btnDone;
    private RadioButton rdLogin, rdSignUp;
    private SignInButton signInButton;

    private ProgressDialog dialog;
    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9511;

    private DatabaseReference adminReference;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso= new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken("271242601468-i028p0cf54bqf6fmsnc07o0b8atq9rqj.apps.googleusercontent.com")  //a
                .requestIdToken("271242601468-o4gogr87ecube22vc9i1tbi2amb7jqj0.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        adminReference = FirebaseDatabase.getInstance().getReference().child("travel_deals").child("administrators");

        edEmail = findViewById(R.id.ed_email);
        edPassword = findViewById(R.id.ed_password);
        rdSignUp = findViewById(R.id.rd_signup);
        rdLogin = findViewById(R.id.rd_login);
        btnDone = findViewById(R.id.btn_done);
        signInButton = findViewById(R.id.btn_google);


        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authenticate();
            }
        });
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithGoogle();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
//                Toast.makeText(this, "here ...", Toast.LENGTH_SHORT).show();
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
//                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(LoginActivity.this, "Google sign in failed. Try Again !", Toast.LENGTH_SHORT).show();
//                Toast.makeText(LoginActivity.this, "failed - " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                // ...
            }
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    private void authenticate() {
        if (TextUtils.isEmpty(edEmail.getText().toString())) {
            Toast.makeText(this, "Enter Email ...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(edPassword.getText().toString())) {
            Toast.makeText(this, "Enter Password ...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rdLogin.isChecked()) {
            dialog.setMessage("Signing in ...");
            dialog.show();
            signIn();
        } else if (rdSignUp.isChecked()) {
            dialog.setMessage("Creating account ...");
            dialog.show();
            signUp();
        }
    }

    private void signIn() {
        auth.signInWithEmailAndPassword(edEmail.getText().toString(), edPassword.getText().toString())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        dialog.dismiss();
                        Intent intent = new Intent(LoginActivity.this, ListActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                });
    }

    private void signUp() {
        auth.createUserWithEmailAndPassword(edEmail.getText().toString(), edPassword.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        dialog.dismiss();
                        Intent intent = new Intent(LoginActivity.this, ListActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
//                            Log.d(TAG, "signInWithCredential:success");
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                            startActivity(new Intent(LoginActivity.this, ListActivity.class));
//                            overridePendingTransition(R.anim.split_enter, R.anim.split_exit);
                            finish();

//                            FirebaseUser user = mAuth.getCurrentUser();
//                            Toast.makeText(LoginActivity.this, "firebase", Toast.LENGTH_SHORT).show();
//
                        } else {
                            // If sign in fails, display a message to the user.
//                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this, "Authentication failed...", Toast.LENGTH_SHORT).show();
//                            Snackbar.make(findViewById(R.id.sign_in_button), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
//                            updateUI(null);
                        }

                        // ...
                    }
                });
    }
}
