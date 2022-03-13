package com.example.mobileclient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.biometric.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // features requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                    }
                });

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_CONNECT) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_SCAN) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        else
        {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_ADMIN) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, QRScan.class);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build();

        Button scanButton = findViewById(R.id.ScanButton);

        scanButton.setOnClickListener(view -> {
            biometricPrompt.authenticate(promptInfo);
        });
    }

}