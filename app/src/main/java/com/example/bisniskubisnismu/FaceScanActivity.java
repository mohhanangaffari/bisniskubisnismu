package com.example.bisniskubisnismu;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExperimentalGetImage
public class FaceScanActivity extends AppCompatActivity {
    private static final int camerapermissionrequest = 1001;
    private static final String TAG = "FaceScanActivity";

    private PreviewView previewview;
    private Button loginButton;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private boolean faceDetectedOnce = false;

    private String[] credentialregister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_facescan);

        previewview = findViewById(R.id.previewView);
        loginButton = findViewById(R.id.loginbutton);
        cameraExecutor = Executors.newSingleThreadExecutor();

        credentialregister = getIntent().getStringArrayExtra("credentialregister");

        loginButton.setOnClickListener(v -> {
            if (izindiberikan()) {
                startCam();
                Toast.makeText(FaceScanActivity.this, "Scanning face...", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(FaceScanActivity.this, new String[]{android.Manifest.permission.CAMERA}, camerapermissionrequest);
            }
        });
    }

    private boolean izindiberikan() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCam() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                analyzer();
            } catch (Exception e) {
                Log.e(TAG, "Camera provider error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzer() {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewview.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (faceDetectedOnce) {
                imageProxy.close();
                return;
            }

            Image mediaimg = imageProxy.getImage();
            if (mediaimg != null) {
                InputImage image = InputImage.fromMediaImage(mediaimg, imageProxy.getImageInfo().getRotationDegrees());
                FaceDetectorOptions options = new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).build();
                FaceDetector detector = FaceDetection.getClient(options);

                detector.process(image)
                        .addOnSuccessListener(faces -> {
                            if (!faces.isEmpty()) {
                                Face face = faces.get(0);
                                faceDetectedOnce = true;
                                Bitmap facebit = cropFaceFromImage(mediaimg, face.getBoundingBox(), imageProxy.getImageInfo().getRotationDegrees());

                                if (facebit != null) {
                                    try {
                                        FaceEmbedding faceEmbedding = new FaceEmbedding(getApplicationContext());
                                        float[] embedding = faceEmbedding.getFaceEmbedding(facebit);

                                        Intent intent = new Intent(FaceScanActivity.this, FingerPrintActivity.class);
                                        intent.putExtra("email", credentialregister[0]);
                                        intent.putExtra("name", credentialregister[1]);
                                        intent.putExtra("password", credentialregister[2]);
                                        intent.putExtra("embedding", embedding);
                                        startActivity(intent);
                                        finish();

                                    } catch (IOException e) {
                                        Log.e(TAG, "Face embedding error", e);
                                    }
                                } else {
                                    Log.e(TAG, "Null face bitmap");
                                }
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private Bitmap cropFaceFromImage(Image mediaimg, Rect boundingbox, int rotationDegrees) {
        try {
            ByteBuffer yBuffer = mediaimg.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = mediaimg.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = mediaimg.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            int width = mediaimg.getWidth();
            int height = mediaimg.getHeight();

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, outputStream);
            byte[] jpegbyte = outputStream.toByteArray();

            Bitmap fullbit = android.graphics.BitmapFactory.decodeByteArray(jpegbyte, 0, jpegbyte.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            fullbit = Bitmap.createBitmap(fullbit, 0, 0, fullbit.getWidth(), fullbit.getHeight(), matrix, true);

            Rect faceRect = boundingbox;
            int left = Math.max(0, faceRect.left);
            int top = Math.max(0, faceRect.top);
            int widthCrop = Math.min(faceRect.width(), fullbit.getWidth() - left);
            int heightCrop = Math.min(faceRect.height(), fullbit.getHeight() - top);

            return Bitmap.createBitmap(fullbit, left, top, widthCrop, heightCrop);
        } catch (Exception e) {
            Log.e(TAG, "Error cropping: " + e.getMessage());
            return null;
        }
    }
}
