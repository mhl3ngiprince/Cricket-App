package com.finedine.spucricketclub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.finedine.spucricketclub.ai.PlayerRecognitionEngine;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Player.PlayerRole;
import com.finedine.spucricketclub.cricket.Team;
import com.finedine.spucricketclub.data.PlayerDatabase;
import com.finedine.spucricketclub.utils.ImageUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for registering player faces for AI recognition
 */
public class RegisterPlayerFaceActivity extends AppCompatActivity {
    private static final String TAG = "RegisterPlayerFace";
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    private PreviewView previewView;
    private Button captureButton;
    private Button saveButton;
    private Button cancelButton;
    private Spinner playerSpinner;
    private Spinner roleSpinner;
    private TextView registerInstructions;
    private View previewContainer;
    private View capturedImageContainer;
    private View previewImageView;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Bitmap capturedFaceBitmap = null;

    private PlayerDatabase playerDatabase;
    private PlayerRecognitionEngine recognitionEngine;
    private List<Player> availablePlayers = new ArrayList<>();
    private Player selectedPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_player_face);

        // Initialize components
        previewView = findViewById(R.id.register_preview_view);
        captureButton = findViewById(R.id.capture_button);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);
        playerSpinner = findViewById(R.id.player_spinner);
        roleSpinner = findViewById(R.id.role_spinner);
        registerInstructions = findViewById(R.id.register_instructions);
        previewContainer = findViewById(R.id.preview_container);
        capturedImageContainer = findViewById(R.id.captured_image_container);
        previewImageView = findViewById(R.id.preview_image_view);

        // Initialize player database and recognition engine
        playerDatabase = PlayerDatabase.getInstance(this);
        recognitionEngine = PlayerRecognitionEngine.getInstance(this);

        // Set up camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Set up UI components
        setupUI();

        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void setupUI() {
        // Set up player spinner
        setupPlayerSpinner();

        // Set up role spinner with player roles
        ArrayAdapter<CharSequence> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item);
        for (PlayerRole role : PlayerRole.values()) {
            roleAdapter.add(role.name());
        }
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        // Set up buttons
        captureButton.setOnClickListener(v -> captureImage());
        saveButton.setOnClickListener(v -> savePlayerFace());
        cancelButton.setOnClickListener(v -> resetCapture());

        // Initial UI state
        saveButton.setEnabled(false);
        capturedImageContainer.setVisibility(View.GONE);
        previewContainer.setVisibility(View.VISIBLE);
    }

    private void setupPlayerSpinner() {
        // Get all players from the database or create new ones
        availablePlayers = playerDatabase.getAllPlayers();

        // If no players exist, create some examples
        if (availablePlayers.isEmpty()) {
            Team demoTeam = new Team("SPU Cricket Club");
            demoTeam.addPlayer(new Player("Alex Smith", PlayerRole.BATSMAN));
            demoTeam.addPlayer(new Player("James Wilson", PlayerRole.BOWLER));
            demoTeam.addPlayer(new Player("Michael Brown", PlayerRole.ALL_ROUNDER));
            demoTeam.addPlayer(new Player("Daniel Taylor", PlayerRole.WICKET_KEEPER));

            availablePlayers = demoTeam.getPlayers();

            // Save players to database
            for (Player player : availablePlayers) {
                playerDatabase.updatePlayer(player);
            }
        }

        // Set up player spinner
        List<String> playerNames = new ArrayList<>();
        playerNames.add("New Player...");
        for (Player player : availablePlayers) {
            playerNames.add(player.getName());
        }

        ArrayAdapter<String> playerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, playerNames);
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerSpinner.setAdapter(playerAdapter);

        // Handle player selection
        playerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // "New Player..." selected
                    selectedPlayer = null;
                    roleSpinner.setVisibility(View.VISIBLE);
                } else {
                    // Existing player selected
                    selectedPlayer = availablePlayers.get(position - 1);
                    roleSpinner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPlayer = null;
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Select front camera as we're registering faces
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            // Unbind any bound use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            Camera camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture);

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void captureImage() {
        if (imageCapture == null) return;

        // Create output options
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        processImage(imageProxy);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        Log.e(TAG, "Image capture failed: " + error.getMessage(), error);
                        Toast.makeText(RegisterPlayerFaceActivity.this,
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
            // Convert to bitmap and crop to face
            Bitmap bitmap = ImageUtils.imageToBitmap(image);

            // Rotate bitmap for front camera (mirror image)
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f); // Flip horizontally
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Store the captured image
            capturedFaceBitmap = bitmap;

            // Update UI
            updatePreviewWithCapturedImage();

        } catch (Exception e) {
            Log.e(TAG, "Error processing capture: " + e.getMessage());
        } finally {
            imageProxy.close();
        }
    }

    private void updatePreviewWithCapturedImage() {
        if (capturedFaceBitmap != null) {
            // Show the captured image
            previewContainer.setVisibility(View.GONE);
            capturedImageContainer.setVisibility(View.VISIBLE);

            // Enable save button
            saveButton.setEnabled(true);

            // Set preview image
            previewImageView.setBackground(null);
            previewImageView.setBackgroundDrawable(
                    new android.graphics.drawable.BitmapDrawable(getResources(), capturedFaceBitmap));
        }
    }

    private void savePlayerFace() {
        if (capturedFaceBitmap == null) {
            Toast.makeText(this, "No face captured", Toast.LENGTH_SHORT).show();
            return;
        }

        // If new player, create one
        if (selectedPlayer == null) {
            String name = "Player " + (availablePlayers.size() + 1);
            PlayerRole role = PlayerRole.values()[roleSpinner.getSelectedItemPosition()];
            selectedPlayer = new Player(name, role);
            playerDatabase.updatePlayer(selectedPlayer);
        }

        // Register the face
        recognitionEngine.registerPlayerFace(selectedPlayer, capturedFaceBitmap);

        // Show success message
        Snackbar.make(findViewById(R.id.register_coordinator_layout),
                "Face registered for " + selectedPlayer.getName(),
                Snackbar.LENGTH_SHORT).show();

        // Reset for next capture
        resetCapture();
    }

    private void resetCapture() {
        capturedFaceBitmap = null;
        previewContainer.setVisibility(View.VISIBLE);
        capturedImageContainer.setVisibility(View.GONE);
        saveButton.setEnabled(false);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}