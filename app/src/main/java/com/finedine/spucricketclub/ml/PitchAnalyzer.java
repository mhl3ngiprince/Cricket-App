package com.finedine.spucricketclub.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.finedine.spucricketclub.interfaces.PitchAnalysisListener;
import com.finedine.spucricketclub.models.PitchAnalysisResult;
import com.finedine.spucricketclub.utils.ImageUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Advanced pitch analyzer that uses ML Kit to analyze cricket pitch conditions
 * and provide insights about bounce, grass coverage, cracks and expected behavior.
 */
public class PitchAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "PitchAnalyzer";

    // Pitch analyzer states
    private static final int STATE_INACTIVE = 0;
    private static final int STATE_CALIBRATING = 1;
    private static final int STATE_ANALYZING = 2;

    private final Context context;
    private final PitchAnalysisListener listener;
    private Segmenter segmenter;
    private ObjectDetector objectDetector;
    private int currentState = STATE_INACTIVE;
    private int frameCount = 0;

    // Pitch area coordinates (will be calibrated)
    private float pitchStartX = 0f;
    private float pitchStartY = 0f;
    private float pitchEndX = 0f;
    private float pitchEndY = 0f;

    // Pitch analysis results
    private float grassCoveragePercentage = 0f;
    private float cracksPercentage = 0f;
    private float moistureLevel = 0f;
    private float predictedBounce = 0f;
    private float predictedTurn = 0f;
    private String pitchType = "Unknown";

    // History tracking for better predictions
    private List<Float> previousBounceValues = new ArrayList<>();
    private Map<String, Float> pitchFeatures = new HashMap<>();

    public PitchAnalyzer(Context context, PitchAnalysisListener listener) {
        this.context = context;
        this.listener = listener;
        setupMlModels();
    }

    private void setupMlModels() {
        // Set up segmenter for surface analysis
        SelfieSegmenterOptions options = new SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build();
        segmenter = Segmentation.getClient(options);

        // Set up custom object detector for pitch features (crease lines, cracks, etc.)
        LocalModel localModel = new LocalModel.Builder()
                .setAssetFilePath("custom_models/cricket_pitch_model.tflite")
                .build();

        CustomObjectDetectorOptions objectOptions = new CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableClassification()
                .setClassificationConfidenceThreshold(0.7f)
                .setMaxPerObjectLabelCount(3)
                .build();

        objectDetector = ObjectDetection.getClient(objectOptions);
    }

    public void startCalibration() {
        currentState = STATE_CALIBRATING;
        frameCount = 0;
        Log.d(TAG, "Starting pitch calibration");
    }

    public void startAnalysis() {
        if (currentState != STATE_CALIBRATING) {
            Log.w(TAG, "Cannot start analysis without calibration");
            return;
        }

        currentState = STATE_ANALYZING;
        Log.d(TAG, "Starting pitch analysis");
    }

    public void stop() {
        currentState = STATE_INACTIVE;
        Log.d(TAG, "Stopping pitch analysis");
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (currentState == STATE_INACTIVE) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        frameCount++;

        // Convert the image to a bitmap for processing
        Bitmap bitmap = ImageUtils.imageToBitmap(mediaImage);
        InputImage inputImage = InputImage.fromBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());

        // Process based on the current state
        if (currentState == STATE_CALIBRATING) {
            processPitchCalibration(inputImage, imageProxy);
        } else if (currentState == STATE_ANALYZING) {
            analyzeCurrentFrame(inputImage, imageProxy);
        }
    }

    private void processPitchCalibration(InputImage inputImage, ImageProxy imageProxy) {
        // Run object detection to find pitch boundaries and creases
        Task<List<DetectedObject>> result = objectDetector.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
                    @Override
                    public void onSuccess(List<DetectedObject> detectedObjects) {
                        // Look for pitch markers
                        for (DetectedObject object : detectedObjects) {
                            for (DetectedObject.Label label : object.getLabels()) {
                                // Check for pitch elements
                                if (label.getText().contains("pitch_area") && label.getConfidence() > 0.8f) {
                                    // Extract pitch boundaries
                                    pitchStartX = object.getBoundingBox().left;
                                    pitchStartY = object.getBoundingBox().top;
                                    pitchEndX = object.getBoundingBox().right;
                                    pitchEndY = object.getBoundingBox().bottom;

                                    Log.d(TAG, "Pitch area detected: " +
                                            pitchStartX + "," + pitchStartY + " to " +
                                            pitchEndX + "," + pitchEndY);

                                    // When we've processed enough frames, finish calibration
                                    if (frameCount > 30) {
                                        currentState = STATE_ANALYZING;
                                        listener.onCalibrationComplete();
                                    }
                                }
                            }
                        }
                        imageProxy.close();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Pitch detection failed", e);
                        imageProxy.close();
                    }
                });
    }

    private void analyzeCurrentFrame(InputImage inputImage, ImageProxy imageProxy) {
        // Only process every 15 frames to reduce computational load
        if (frameCount % 15 != 0) {
            imageProxy.close();
            return;
        }

        // Using segmentation to analyze the pitch surface
        segmenter.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<SegmentationMask>() {
                    @Override
                    public void onSuccess(SegmentationMask segmentationMask) {
                        ByteBuffer buffer = segmentationMask.getBuffer();
                        int width = segmentationMask.getWidth();
                        int height = segmentationMask.getHeight();

                        // Calculate pitch characteristics
                        analyzeSegmentation(buffer, width, height);
                        classifyPitch();

                        // Send results to listener
                        PitchAnalysisResult result = new PitchAnalysisResult.Builder()
                                .setGrassCoverage(grassCoveragePercentage)
                                .setCracks(cracksPercentage)
                                .setMoisture(moistureLevel)
                                .setPredictedBounce(predictedBounce)
                                .setPredictedTurn(predictedTurn)
                                .setPitchType(pitchType)
                                .build();

                        listener.onPitchAnalysisResult(result);
                        imageProxy.close();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Segmentation failed", e);
                        imageProxy.close();
                    }
                });
    }

    private void analyzeSegmentation(ByteBuffer buffer, int width, int height) {
        float totalGreen = 0;
        float totalCracks = 0;
        float totalMoisture = 0;
        int totalPixels = 0;

        buffer.rewind();

        // Process pixels in the segmentation map
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float r = buffer.getFloat();
                float g = buffer.getFloat();
                float b = buffer.getFloat();

                // Calculate the green component (for grass)
                if (g > r * 1.3f && g > b * 1.3f) {
                    totalGreen++;
                }

                // Estimate cracks (dark lines)
                if (r < 0.2f && g < 0.2f && b < 0.2f) {
                    totalCracks++;
                }

                // Estimate moisture (darker blue areas)
                if (b > r * 1.2f && b > g * 1.2f) {
                    totalMoisture++;
                }

                totalPixels++;
            }
        }

        // Calculate percentages
        grassCoveragePercentage = (totalGreen / totalPixels) * 100;
        cracksPercentage = (totalCracks / totalPixels) * 100;
        moistureLevel = (totalMoisture / totalPixels) * 100;

        // Store features for classification
        pitchFeatures.put("grass", grassCoveragePercentage);
        pitchFeatures.put("cracks", cracksPercentage);
        pitchFeatures.put("moisture", moistureLevel);

        Log.d(TAG, String.format("Pitch analysis: Grass=%.1f%%, Cracks=%.1f%%, Moisture=%.1f%%",
                grassCoveragePercentage, cracksPercentage, moistureLevel));
    }

    private void classifyPitch() {
        // Calculate expected bounce and spin based on pitch features
        // These formulas are approximations for demonstration
        predictedBounce = 5.0f - (moistureLevel * 0.03f) + (cracksPercentage * 0.02f);
        predictedTurn = 1.0f + (cracksPercentage * 0.1f) - (moistureLevel * 0.05f) - (grassCoveragePercentage * 0.01f);

        // Clamp values to reasonable ranges
        predictedBounce = Math.max(1.0f, Math.min(10.0f, predictedBounce));
        predictedTurn = Math.max(1.0f, Math.min(10.0f, predictedTurn));

        previousBounceValues.add(predictedBounce);
        if (previousBounceValues.size() > 10) {
            previousBounceValues.remove(0);
        }

        // Categorize pitch type based on characteristics
        if (grassCoveragePercentage > 40) {
            pitchType = "Green";
        } else if (cracksPercentage > 15) {
            pitchType = "Dry/Cracked";
        } else if (moistureLevel > 30) {
            pitchType = "Damp";
        } else if (cracksPercentage > 5 && grassCoveragePercentage < 15) {
            pitchType = "Dusty";
        } else {
            pitchType = "Balanced";
        }
    }
}