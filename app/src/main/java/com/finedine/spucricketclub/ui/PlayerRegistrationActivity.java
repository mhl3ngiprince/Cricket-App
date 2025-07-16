package com.finedine.spucricketclub.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.finedine.spucricketclub.R;
import com.finedine.spucricketclub.ai.PlayerRecognitionEngine;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Player.PlayerRole;
import com.finedine.spucricketclub.utils.ImageUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.camera.core.ExperimentalGetImage
/**
 * Activity for registering player faces for the AI recognition system
 */
public class PlayerRegistrationActivity extends AppCompatActivity {
    private static final String TAG = "PlayerRegistrationAct";
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    // UI components
    private PreviewView faceCameraPreview;
    private View faceOverlay;
    private ImageView capturedFaceImageView;
    private TextView faceStatusTextView;
    private TextInputEditText playerNameEditText;
    private RadioGroup playerRoleRadioGroup;
    private Button captureFaceButton;
    private Button retakeButton;
    private Button registerButton;

    // Camera components
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    // Face recognition components
    private PlayerRecognitionEngine recognitionEngine;
    private boolean recognitionEngineInitialized = false;

    // Captured face data
    private Bitmap capturedFaceBitmap;
    private boolean faceDetected = false;
    private boolean faceCaptured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_registration);

        // Initialize UI components
        initializeUI();

        // Set up camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // We'll initialize the recognition engine lazily when needed, not here in onCreate
        // This avoids potential crashes during app startup

        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    private void initializeUI() {
        faceCameraPreview = findViewById(R.id.face_camera_preview);
        faceOverlay = findViewById(R.id.face_overlay);
        capturedFaceImageView = findViewById(R.id.image_captured_face);
        faceStatusTextView = findViewById(R.id.text_face_status);
        playerNameEditText = findViewById(R.id.edit_player_name);
        playerRoleRadioGroup = findViewById(R.id.radio_player_role);
        captureFaceButton = findViewById(R.id.button_capture_face);
        retakeButton = findViewById(R.id.button_retake);
        registerButton = findViewById(R.id.button_register);

        // Add null checks for view references
        if (captureFaceButton != null) {
            captureFaceButton.setOnClickListener(v -> captureFace());
        }
        if (retakeButton != null) {
            retakeButton.setOnClickListener(v -> resetCapture());
        }
        if (registerButton != null) {
            registerButton.setOnClickListener(v -> registerPlayer());
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Preview use case
        Preview preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(faceCameraPreview.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Face analysis use case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new FaceAnalyzer());

        // Select front camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            // Unbind previous use cases
            cameraProvider.unbindAll();

            // Bind new use cases to camera
            Camera camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis);

            // Check if camera is available
            if (camera.getCameraInfo() == null) {
                throw new IllegalStateException("Camera not available");
            }

        } catch (IllegalStateException e) {
            Log.e(TAG, "Camera binding failed: " + e.getMessage(), e);
            Toast.makeText(this, "Camera not available. Please try again.", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error binding camera: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to start camera. Please try again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void captureFace() {
        if (imageCapture == null) return;

        if (!faceDetected) {
            Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show();
            return;
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        processImage(imageProxy);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Toast.makeText(PlayerRegistrationActivity.this,
                                "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImage(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }

        try {
            // Convert to bitmap
            capturedFaceBitmap = ImageUtils.imageToBitmap(image);

            // Show captured image
            capturedFaceImageView.setImageBitmap(capturedFaceBitmap);

            // Update UI state
            faceCaptured = true;
            faceStatusTextView.setText("Face captured!");
            retakeButton.setEnabled(true);
            registerButton.setEnabled(true);

        } catch (Exception e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        } finally {
            imageProxy.close();
        }
    }

    private void resetCapture() {
        capturedFaceBitmap = null;
        faceCaptured = false;
        capturedFaceImageView.setImageDrawable(null);
        faceStatusTextView.setText("Position your face in the frame");
        retakeButton.setEnabled(false);
        registerButton.setEnabled(false);
    }

    private void registerPlayer() {
        String name = playerNameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a player name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capturedFaceBitmap == null) {
            Toast.makeText(this, "Please capture a face first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize recognition engine if needed
        if (!recognitionEngineInitialized) {
            try {
                recognitionEngine = PlayerRecognitionEngine.getInstance(this);
                recognitionEngineInitialized = true;
            } catch (Exception e) {
                Log.e(TAG, "Error initializing PlayerRecognitionEngine", e);
                Toast.makeText(this, "Failed to initialize face recognition. Please try again.", Toast.LENGTH_LONG).show();
                return; // Cannot proceed without recognition engine
            }
        }

        // Get selected player role
        PlayerRole role;
        int radioId = playerRoleRadioGroup.getCheckedRadioButtonId();

        if (radioId == R.id.radio_batsman) {
            role = PlayerRole.BATSMAN;
        } else if (radioId == R.id.radio_bowler) {
            role = PlayerRole.BOWLER;
        } else if (radioId == R.id.radio_all_rounder) {
            role = PlayerRole.ALL_ROUNDER;
        } else if (radioId == R.id.radio_wicket_keeper) {
            role = PlayerRole.WICKET_KEEPER;
        } else {
            Toast.makeText(this, "Please select a player role", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new player
        Player player = new Player(name, role);

        // Register face with the recognition engine
        try {
            if (recognitionEngine != null) {
                recognitionEngine.registerPlayerFace(player, capturedFaceBitmap);
            } else {
                Toast.makeText(this, "Face recognition engine not available", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering face", e);
            Toast.makeText(this, "Error registering face: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Show success message
        Snackbar.make(findViewById(android.R.id.content),
                "Player " + name + " registered successfully",
                Snackbar.LENGTH_LONG).show();

        // Reset UI
        resetCapture();
        playerNameEditText.setText("");
        playerRoleRadioGroup.clearCheck();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permissions required",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Face analyzer class for detecting faces in real-time
     */
    private class FaceAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        @androidx.camera.core.ExperimentalGetImage
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image image = imageProxy.getImage();
            if (image == null) {
                imageProxy.close();
                return;
            }

            try {
                // Here we would do actual face detection with ML Kit
                // For simplicity, we'll simulate face detection
                simulateFaceDetection();

            } finally {
                imageProxy.close();
            }
        }

        private void simulateFaceDetection() {
            // In a real implementation, this would use ML Kit face detection
            // Here we just simulate it by showing the overlay
            runOnUiThread(() -> {
                if (!faceDetected) {
                    faceDetected = true;
                    faceOverlay.setVisibility(View.VISIBLE);
                    faceStatusTextView.setText("Face detected! Tap 'Capture' button");
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
