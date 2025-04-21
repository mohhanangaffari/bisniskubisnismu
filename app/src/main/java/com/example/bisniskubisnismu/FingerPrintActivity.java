package com.example.bisniskubisnismu;

import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.P)
public class FingerPrintActivity extends AppCompatActivity {

    private String email, name, password;
    private float[] embedding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        email = getIntent().getStringExtra("email");
        name = getIntent().getStringExtra("name");
        password = getIntent().getStringExtra("password");
        embedding = getIntent().getFloatArrayExtra("embedding");

        BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(this)
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Verify to complete registration")
                .setDescription("Place your finger on the sensor")
                .setNegativeButton("Cancel", this.getMainExecutor(), (dialogInterface, i) -> {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }).build();

        biometricPrompt.authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                saveToFirestore();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(FingerPrintActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        List<Float> embeddingList = new ArrayList<>();
        for (float val : embedding) embeddingList.add(val);

        userData.put("email", email);
        userData.put("name", name);
        userData.put("password", password);
        userData.put("embedding", embeddingList);

        db.collection("users").document(email).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(FingerPrintActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(FingerPrintActivity.this, "Failed to save data", Toast.LENGTH_SHORT).show());
    }
}
