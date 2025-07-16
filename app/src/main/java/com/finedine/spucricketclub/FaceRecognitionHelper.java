package com.finedine.spucricketclub;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceRecognitionHelper {

    private static final String TAG = "FaceRecognitionHelper";

    private FaceDetector faceDetector;

    public FaceRecognitionHelper(Context context) {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        faceDetector = FaceDetection.getClient(options);
    }

    public void detectFaces(Bitmap bitmap, FaceDetectionCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d(TAG, "Faces detected: " + faces.size());
                    callback.onFacesDetected(faces);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed", e);
                    callback.onFailure(e);
                });
    }

    public interface FaceDetectionCallback {
        void onFacesDetected(@NonNull List<Face> faces);
        void onFailure(@NonNull Exception e);
    }

    public void close() {
        faceDetector.close();
    }
}