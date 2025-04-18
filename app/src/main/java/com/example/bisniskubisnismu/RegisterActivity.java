package com.example.bisniskubisnismu;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Array;
import java.util.Arrays;

@ExperimentalGetImage
public class RegisterActivity extends AppCompatActivity {
    private EditText regemail,regnama,regpassword,regpasswordconf;
    private Button regbutton;
    protected String[] credentialregister;

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

        regbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(regpassword.getText().toString().equals(regpasswordconf.getText().toString()))){
                    Toast.makeText(RegisterActivity.this, "password tidak sama", Toast.LENGTH_SHORT).show();
                }else{
                    credentialregister[0] = regemail.getText().toString();
                    credentialregister[1] = regnama.getText().toString();
                    credentialregister[2] = regpassword.getText().toString();
                    Intent intent = new Intent(RegisterActivity.this, FaceScanActivity.class);
                    intent.putExtra("credentialregister",credentialregister);
                    startActivity(intent);
                }
            }
        });

    }
}