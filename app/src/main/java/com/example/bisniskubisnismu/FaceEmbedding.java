package com.example.bisniskubisnismu;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FaceEmbedding {

    private static final String MODEL_PATH = "facenet.tflite";
    private static final int INPUT_SIZE = 160;
    private static final int EMBEDDING_SIZE = 512;

    private Interpreter tflite;

    public FaceEmbedding(Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = fileDescriptor.createInputStream();
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] getFaceEmbedding(Bitmap facebit) {
        // Resize image to input size
        Bitmap resize = Bitmap.createScaledBitmap(facebit, INPUT_SIZE, INPUT_SIZE, true);
        resize = rotateBitmap(resize, -90);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();

        // Convert pixels to float values
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int px = resize.getPixel(x, y);
                byteBuffer.putFloat(((px >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((px >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((px & 0xFF) / 255.0f);
            }
        }

        float[][] embedding = new float[1][EMBEDDING_SIZE];
        tflite.run(byteBuffer, embedding);
        return embedding[0];
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
