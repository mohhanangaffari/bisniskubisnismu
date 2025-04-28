package com.example.bisniskubisnismu;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExperimentalGetImage
public class FaceLoginActivity extends AppCompatActivity {
    private static final int CAM_REQUEST = 2001;
    private static final String TAG = "FaceLoginActivity";
    private static final double THRESHOLD = 1.0;

    private PreviewView previewView;
    private Button scanButton;
    private ExecutorService cameraExecutor;
    private boolean scanning = false;

    private String email;
    private List<Double> storedEmbedding;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_face_login);

        email = getIntent().getStringExtra("email");

        previewView    = findViewById(R.id.previewView);
        scanButton     = findViewById(R.id.scanButton);
        cameraExecutor = Executors.newSingleThreadExecutor();
        firestore      = FirebaseFirestore.getInstance();

        scanButton.setOnClickListener(v -> {
            if (scanning) return;
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                fetchEmbeddingAndStart();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.CAMERA},
                        CAM_REQUEST
                );
            }
        });
    }

    private void fetchEmbeddingAndStart() {
        firestore.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(this::onEmbeddingLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fetch user failed", e);
                    Toast.makeText(this, "Gagal mengambil data user", Toast.LENGTH_SHORT).show();
                });
    }

    private void onEmbeddingLoaded(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Double> emb = (List<Double>) doc.get("embedding");
        if (emb == null || emb.isEmpty()) {
            Toast.makeText(this, "Data embedding kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        storedEmbedding = emb;
        startCameraAnalysis();
    }

    private void startCameraAnalysis() {
        scanning = true;
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindAnalysis(provider);
            } catch (Exception e) {
                Log.e(TAG, "Provider error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAnalysis(ProcessCameraProvider provider) {
        provider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, proxy -> {
            Image mediaImage = proxy.getImage();
            if (mediaImage == null) {
                proxy.close();
                return;
            }

            InputImage input = InputImage.fromMediaImage(
                    mediaImage, proxy.getImageInfo().getRotationDegrees()
            );
            FaceDetector detector = FaceDetection.getClient(
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .build()
            );
            detector.process(input)
                    .addOnSuccessListener(faces -> {
                        proxy.close();
                        scanning = false;
                        if (!faces.isEmpty()) {
                            handleFace(faces.get(0), mediaImage,
                                    proxy.getImageInfo().getRotationDegrees());
                        }
                    })
                    .addOnFailureListener(e -> {
                        proxy.close();
                        scanning = false;
                        Log.e(TAG, "Detection error", e);
                    });
        });

        provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
        );
    }

    private void handleFace(Face face, Image mediaImage, int rotation) {
        Bitmap cropped = cropFace(mediaImage, face.getBoundingBox(), rotation);
        if (cropped == null) {
            Toast.makeText(this, "Gagal memotong wajah", Toast.LENGTH_SHORT).show();
            return;
        }

        float[] liveEmbedding;
        try {
            liveEmbedding = new FaceEmbedding(this).getFaceEmbedding(cropped);
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "Embedding error", e);
            Toast.makeText(this, "Gagal menghitung embedding", Toast.LENGTH_SHORT).show();
            return;
        }

        if (liveEmbedding.length != storedEmbedding.size()) {
            Toast.makeText(this, "Data embedding mismatch", Toast.LENGTH_SHORT).show();
            return;
        }

        double dist = 0;
        for (int i = 0; i < liveEmbedding.length; i++) {
            double d = liveEmbedding[i] - storedEmbedding.get(i);
            dist += d * d;
        }
        dist = Math.sqrt(dist);

        if (dist <= THRESHOLD) {
            Toast.makeText(this, "Verifikasi sukses", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, ProfileActivity.class)
                        .putExtra("email", email));
                finish();
            }, 1500);
        } else {
            Toast.makeText(this, "Wajah tidak cocok", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap cropFace(Image mediaImage, Rect box, int rotation) {
        try {
            ByteBuffer y = mediaImage.getPlanes()[0].getBuffer();
            ByteBuffer u = mediaImage.getPlanes()[1].getBuffer();
            ByteBuffer v = mediaImage.getPlanes()[2].getBuffer();

            byte[] nv21 = new byte[y.remaining() + u.remaining() + v.remaining()];
            y.get(nv21, 0, y.remaining());
            v.get(nv21, y.remaining(), v.remaining());
            u.get(nv21, y.remaining() + v.remaining(), u.remaining());

            int width = mediaImage.getWidth();
            int height = mediaImage.getHeight();
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 100, os);
            Bitmap full = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.size());

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(full, 0, 0, full.getWidth(), full.getHeight(), matrix, true);

            int left = Math.max(0, box.left);
            int top  = Math.max(0, box.top);
            int cw   = Math.min(box.width(), rotated.getWidth() - left);
            int ch   = Math.min(box.height(), rotated.getHeight() - top);
            if (cw <= 0 || ch <= 0) return null;
            return Bitmap.createBitmap(rotated, left, top, cw, ch);
        } catch (Exception e) {
            Log.e(TAG, "Crop error", e);
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == CAM_REQUEST && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            fetchEmbeddingAndStart();
        } else {
            Toast.makeText(this, "Izin kamera dibutuhkan", Toast.LENGTH_SHORT).show();
        }
    }
}
