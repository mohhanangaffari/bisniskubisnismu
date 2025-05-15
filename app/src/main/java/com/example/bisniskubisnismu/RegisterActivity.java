package com.example.bisniskubisnismu;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.lang.reflect.Array;
import java.util.Arrays;

@ExperimentalGetImage
public class RegisterActivity extends AppCompatActivity {
    private EditText regemail,regnama,regpassword,regpasswordconf;
    private TextView loginPage;
    private Button regbutton;
    protected String[] credentialregister;

    private  FirebaseAuth mauth;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        credentialregister = new String[3];
        regemail = findViewById(R.id.regemail);
        regnama = findViewById(R.id.regnama);
        regpassword = findViewById(R.id.regpassword);
        regpasswordconf = findViewById(R.id.regpasswordconfirm);
        regbutton = findViewById(R.id.regbutton);
        loginPage = findViewById(R.id.loginpage);
        mauth = FirebaseAuth.getInstance();

        FirebaseApp.initializeApp(this);


        // Tombol Register (opsional: navigasi ke RegisterActivity)
        loginPage.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, EmailLoginActivity.class));
        });

        regbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(regpassword.getText().toString().equals(regpasswordconf.getText().toString()))) {
                    Toast.makeText(RegisterActivity.this, "password tidak sama", Toast.LENGTH_SHORT).show();
                } else {
                    mauth.createUserWithEmailAndPassword(regemail.getText().toString().trim(), regpassword.getText().toString().trim())
                            .addOnCompleteListener(task -> {
                                if(task.isSuccessful()) {
                                    FirebaseUser user = mauth.getCurrentUser();

                                    if (user != null) {
                                        user.sendEmailVerification().addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "email terkirim", Toast.LENGTH_SHORT).show();
                                                FirebaseAuth.getInstance().signOut();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                            }

                                        });
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Failed to create user.", Toast.LENGTH_SHORT).show();
                                    }


                                    credentialregister[0] = regemail.getText().toString();
                                    credentialregister[1] = regnama.getText().toString();
                                    credentialregister[2] = regpassword.getText().toString();
                                    Intent intent = new Intent(RegisterActivity.this, EmailLoginActivity.class);
                                    intent.putExtra("credentialregister", credentialregister);
                                    startActivity(intent);

                                }else{
                                    Toast.makeText(RegisterActivity.this, "gagal boss", Toast.LENGTH_SHORT).show();
                                }
                    }).addOnFailureListener(task -> {Toast.makeText(RegisterActivity.this, "gagal boss "+task.getMessage(), Toast.LENGTH_SHORT).show();});

                }
            }
        });
    }
}