package com.example.bisniskubisnismu;

import static java.lang.System.out;

import android.Manifest;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.graphics.Canvas;
import java.io.ByteArrayOutputStream;

@ExperimentalGetImage public class LoginActivity extends AppCompatActivity {
    private PreviewView previewview;
    private Button loginButton;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private static final int camerapermissionrequest=1001;
    private boolean faceDetectedOnce = false;
    private static final String TAG = "LoginActivity";
    DatabaseReference myRef;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        previewview = findViewById(R.id.previewView);
        loginButton = findViewById(R.id.loginbutton);
        cameraExecutor = Executors.newSingleThreadExecutor();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(izindiberikan()){
                    startCam();
                }else{
                    ActivityCompat.requestPermissions(LoginActivity.this,new  String[]{android.Manifest.permission.CAMERA},camerapermissionrequest);
                }
            }
        });

    }

    private boolean izindiberikan(){
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCam(){
        ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderListenableFuture.addListener(()->{
            try {
                cameraProvider = cameraProviderListenableFuture.get();
                analyzer();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG,"Camera provider error",e);
            }
        },ContextCompat.getMainExecutor(this));
    }

    private void analyzer(){
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewview.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(cameraExecutor,imageProxy -> {
            if(faceDetectedOnce){
                imageProxy.close();
                return;
            }
            @androidx.camera.core.ExperimentalGetImage Image mediaimg = imageProxy.getImage();
            if (mediaimg != null) {
                InputImage image = InputImage.fromMediaImage(mediaimg, imageProxy.getImageInfo().getRotationDegrees());
                FaceDetectorOptions options = new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).build();


                FaceDetector detector = FaceDetection.getClient(options);
                detector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (!faces.isEmpty()) {
                            Face face = faces.get(0);
                            Rect bounds = face.getBoundingBox();

                            Log.d(TAG, "Face detected" + bounds.toShortString());
                            faceDetectedOnce=true;

                            Bitmap facebit = cropFaceFromImage(mediaimg,face.getBoundingBox(),imageProxy.getImageInfo().getRotationDegrees());

                            if (facebit !=null){
                                try {
                                    FaceEmbedding faceEmbedding = new FaceEmbedding(getApplicationContext());
                                    float [] embedding = faceEmbedding.getFaceEmbedding(facebit);
//                                    registerUser("alice",embedding);
                                    matchfacedata(embedding);
                                    Log.d(TAG,"embedding size:"+embedding.length);
                                    Log.d(TAG,"embedding bit value:"+ Arrays.toString(embedding));
                                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Face detected", Toast.LENGTH_SHORT).show());

                                    Toast.makeText(LoginActivity.this, "berhasil", Toast.LENGTH_SHORT).show();
                                }catch (IOException e){
                                    Log.e(TAG,"TFLIte model error",e);
                                }
                            }else {
                                Log.e(TAG,"null bit value");
                            }


                        }
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "Failed to detect face", e)).addOnCompleteListener(task -> imageProxy.close());

            }else{
                imageProxy.close();
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageAnalysis);
    }

    private Bitmap cropFaceFromImage(Image mediaimg,Rect boundingbox,int rotationDegrees){
       try{
           ByteBuffer yBuffer = mediaimg.getPlanes()[0].getBuffer();
           ByteBuffer uBuffer = mediaimg.getPlanes()[1].getBuffer();
           ByteBuffer vBuffer = mediaimg.getPlanes()[2].getBuffer();

           int ySize = yBuffer.remaining();
           int uSize = uBuffer.remaining();
           int vSize = vBuffer.remaining();

           byte[] nv21 = new byte[ySize+uSize+vSize];
           yBuffer.get(nv21,0,ySize);
           vBuffer.get(nv21,ySize,vSize);
           uBuffer.get(nv21,ySize+vSize,uSize);

           int width = mediaimg.getWidth();
           int height = mediaimg.getHeight();

           YuvImage yuvImage = new YuvImage(nv21,ImageFormat.NV21,width,height,null);
           ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
           yuvImage.compressToJpeg(new Rect(0,0,width,height),100,outputStream);
           byte[] jpegbyte = outputStream.toByteArray();

           Bitmap fullbit= android.graphics.BitmapFactory.decodeByteArray(jpegbyte,0,jpegbyte.length);

           Matrix matrix = new Matrix();
           matrix.postRotate(rotationDegrees);
           fullbit = Bitmap.createBitmap(fullbit,0,0,fullbit.getWidth(),fullbit.getHeight(),matrix,true);

           Rect facerect = boundingbox;
           int left = Math.max(0,facerect.left);
           int top = Math.max(0, facerect.top);
           int widthCrop = Math.min(facerect.width(),fullbit.getWidth()-left);
           int heightCrop = Math.min(facerect.height(),fullbit.getHeight()-top);

           return Bitmap.createBitmap(fullbit,left,top,widthCrop,heightCrop);
        }catch (Exception e){
           Log.e(TAG,"error cropping"+e.getMessage());
           return null;
       }
    }

    private void registerUser(String name,float [] embedding){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
        Map<String,Object> userData = new HashMap<>();
        userData.put("name",name);

        List<Float> embeddingList = new ArrayList<>();
        for(float val:embedding) embeddingList.add(val);
        userData.put("embedding",embeddingList);

        databaseReference.child(name).setValue(embeddingList).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(LoginActivity.this, "user registered", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LoginActivity.this, "error"+e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        if (requestCode == camerapermissionrequest){
            if(izindiberikan()){
                startCam();
            }else{
                Toast.makeText(this, "mohon berikan izin penggunaan camera", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private float checkface(float[] vec1,float[] vec2){
       float dot = 0f;
       float norm1 = 0f;
       float norm2 = 0f;

       for(int i=0;i<vec1.length;i++) {
           dot += vec1[i] * vec2[i];
           norm1 += vec1[i] * vec1[i];
           norm2 += vec2[i] * vec2[i];
       }
       return dot / ((float)(Math.sqrt(norm1) * Math.sqrt(norm2))+ 1e-10f);
    }

    private void matchfacedata(float[] currentfacedata){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");

        databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    DataSnapshot dataSnapshot = task.getResult();
                    String matchface = null;
                    float maxsimilarity = -1f;
                    for(DataSnapshot userSnapshot:dataSnapshot.getChildren()){
                        List<Double> doubleList = (List<Double>) userSnapshot.getValue();
                        float[] storedfacedata = new float[doubleList.size()];
                        for(int i=0;i<doubleList.size();i++){
                            storedfacedata[i] = doubleList.get(i).floatValue();
                        }
                        float similarity = checkface(currentfacedata,storedfacedata);
                        if(similarity>maxsimilarity){
                            maxsimilarity = similarity;
                            matchface = userSnapshot.getValue(String.class);
                        }
                    }
                    if (maxsimilarity > 0.85f){
                        Toast.makeText(LoginActivity.this, "Welcome"+matchface, Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(LoginActivity.this, "face not registered", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(LoginActivity.this, "error"+task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}