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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExperimentalGetImage
public class FaceScanActivity extends AppCompatActivity {
    private static final int CAM_PERMISSION_REQUEST = 1001;
    private static final String TAG = "FaceScanActivity";

    private PreviewView previewView;
    private Button loginButton;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private boolean faceDetectedOnce = false;

    private String email, name, password;
    private FirebaseFirestore firestore;
    public String[] creds;
    public String email2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_facescan);

        previewView    = findViewById(R.id.previewView);
        loginButton    = findViewById(R.id.loginbutton);
        cameraExecutor = Executors.newSingleThreadExecutor();
        firestore      = FirebaseFirestore.getInstance();

        // Ambil data dari Intent sebelumnya
        creds = getIntent().getStringArrayExtra("credentialregister");
        email2 = getIntent().getStringExtra("email");
        if (creds != null && creds.length >= 3) {
            email    = creds[0];
            name     = creds[1];
            password = creds[2];
        }

        loginButton.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                startCam();
                Toast.makeText(this, "Scanning face...", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.CAMERA},
                        CAM_PERMISSION_REQUEST
                );
            }
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCam() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                setupAnalyzer();
            } catch (Exception e) {
                Log.e(TAG, "Camera provider error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupAnalyzer() {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (faceDetectedOnce) {
                imageProxy.close();
                return;
            }

            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.getImageInfo().getRotationDegrees()
                );
                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();
                FaceDetector detector = FaceDetection.getClient(options);

                detector.process(image)
                        .addOnSuccessListener(faces -> {
                            if (!faces.isEmpty()) {
                                faceDetectedOnce = true;
                                Face face = faces.get(0);
                                Bitmap faceBmp = cropFace(mediaImage, face.getBoundingBox(),
                                        imageProxy.getImageInfo().getRotationDegrees());
                                if (creds != null){
                                    if (faceBmp != null) {
                                        try {
                                            FaceEmbedding faceEmbedding =
                                                    new FaceEmbedding(getApplicationContext());
                                            float[] embedding =
                                                    faceEmbedding.getFaceEmbedding(faceBmp);

                                            // Upload ke Firestore
                                            uploadToFirestore(embedding);

                                        } catch (IOException e) {
                                            Log.e(TAG, "Embedding error", e);
                                        }
                                    }
                                }else{
                                    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
                                    firebaseFirestore.collection("users").whereEqualTo("email",email2).get()
                                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                @Override
                                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                    for(DocumentSnapshot documentSnapshot : queryDocumentSnapshots){
                                                        List<Double> embedding2 = (List<Double>) documentSnapshot.get("embedding");
                                                        float[] embeddingArray = new float[embedding2.size()];
                                                        for (int i = 0; i < embedding2.size(); i++) {
                                                            embeddingArray[i] = embedding2.get(i).floatValue();
                                                        }
                                                        try {
                                                            FaceEmbedding faceEmbedding =
                                                                    new FaceEmbedding(getApplicationContext());
                                                            float[] embedding =
                                                                    faceEmbedding.getFaceEmbedding(faceBmp);
                                                            float kesamaan = Kesamaan2(embedding, embeddingArray);
                                                            Log.d(TAG, "embedding camera"+embedding[0]);
                                                            Log.d(TAG, "embedding firestore"+embeddingArray[0]);
                                                            Log.d(TAG, "embedding kesamaan"+kesamaan);
                                                            if (kesamaan > 400){
                                                                Toast.makeText(FaceScanActivity.this, "sudah ada bang", Toast.LENGTH_SHORT).show();
                                                                Intent intent = new Intent(FaceScanActivity.this, Dashboard.class);
                                                            }else{
                                                                Toast.makeText(FaceScanActivity.this, "gk ketemu bang", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }catch (Exception e){

                                                        }
                                                        }

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(FaceScanActivity.this, "error"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }


                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        });

        CameraSelector selector = CameraSelector.DEFAULT_FRONT_CAMERA;
        cameraProvider.bindToLifecycle(this, selector, preview, analysis);
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

            int w = mediaImage.getWidth(), h = mediaImage.getHeight();
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, w, h, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, w, h), 100, os);
            Bitmap full = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.size());
            Matrix m = new Matrix();
            m.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(full, 0, 0,
                    full.getWidth(), full.getHeight(), m, true);

            int left   = Math.max(0, box.left);
            int top    = Math.max(0, box.top);
            int width  = Math.min(box.width(), rotated.getWidth() - left);
            int height = Math.min(box.height(), rotated.getHeight() - top);
            return Bitmap.createBitmap(rotated, left, top, width, height);

        } catch (Exception e) {
            Log.e(TAG, "Crop error", e);
            return null;
        }
    }

    private void uploadToFirestore(float[] embedding) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("name", name);
        data.put("password", password);

        // Konversi float[] ke List<Float>
        List<Float> embeddingList = new ArrayList<>();
        for (float f : embedding) {
            embeddingList.add(f);
        }
        data.put("embedding", embeddingList);

        firestore.collection("users")
                .document(email)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Data saved, proceeding to login",
                            Toast.LENGTH_SHORT).show();
                    startActivity(
                            new Intent(FaceScanActivity.this,
                                    EmailLoginActivity.class)
                    );
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore upload failed", e);
                    Toast.makeText(this,
                            "Upload gagal: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAM_PERMISSION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCam();
            } else {
                Toast.makeText(this,
                        "Camera permission ditolak",
                        Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private float Kesamaan(float[] vec1, float[] vec2) {
        float dot = 0f;
        float normA = 0f;
        float normB = 0f;
        for (int i = 0; i < vec1.length; i++) {
            dot += vec1[i] * vec2[i];
            normA += vec1[i] * vec1[i];
            normB += vec2[i] * vec2[i];
        }
        return dot / ((float)(Math.sqrt(normA) * Math.sqrt(normB)) + 1e-10f);
    }

    private float Kesamaan2(float[] vec1, float[] vec2) {
        int scorekesamaan = 0;
        for(int i=0;i<vec1.length;i++){
            if(vec1[i] - vec2[i] < 1){
                scorekesamaan ++;
        }
        }
        return scorekesamaan;
    }

}
