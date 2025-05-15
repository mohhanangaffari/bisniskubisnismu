package com.example.bisniskubisnismu;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class EmailLoginActivity extends AppCompatActivity {
    private static final String TAG = "EmailLoginActivity";

    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private TextView registerPage;

    private FirebaseFirestore firestore;

    @SuppressLint({"UnsafeOptInUsageError", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_login);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Inisialisasi View
        emailField    = findViewById(R.id.emailfield);
        passwordField = findViewById(R.id.passwordfield);
        loginButton   = findViewById(R.id.loginemailbtn);
        registerPage = findViewById(R.id.registerpage);

        // Inisialisasi Firestore
        firestore = FirebaseFirestore.getInstance();

        // Tombol Register (opsional: navigasi ke RegisterActivity)
        registerPage.setOnClickListener(v -> {
            startActivity(new Intent(EmailLoginActivity.this, RegisterActivity.class));
        });

        // Tombol Login
        loginButton.setOnClickListener(v -> {
            String emailInput    = emailField.getText().toString().trim();
            String passwordInput = passwordField.getText().toString();

            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ambil dokumen user dengan ID = emailInput
            firestore.collection("users")
                    .document(emailInput)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String storedPassword = document.getString("password");
                            if (storedPassword != null && storedPassword.equals(passwordInput)) {
                                // Password cocok → lanjut ke FaceLoginActivity
                                Intent intent = new Intent(EmailLoginActivity.this, FaceScanActivity.class);
                                intent.putExtra("email", emailInput);
                                startActivity(intent);
                                finish();
                            } else {
                                // Password salah
                                Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Dokumen tidak ada → user belum terdaftar
                            Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user", e);
                        Toast.makeText(this, "Terjadi kesalahan: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }
}
