package com.finedine.spucricketclub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.spucricketclub.adapters.CricketMatchAdapter;
import com.finedine.spucricketclub.ai.PlayerRecognitionEngine;
import com.finedine.spucricketclub.cricket.Ball;
import com.finedine.spucricketclub.cricket.CricketMatchService;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for a clean web-like cricket scoring interface with AI player recognition
 */
public class CricketScoringActivity extends AppCompatActivity implements PlayerRecognitionEngine.PlayerRecognitionListener {

    private static final String TAG = "CricketScoringActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    private CricketMatchService cricketMatchService;
    private Match currentMatch;
    private CricketMatchAdapter matchAdapter;

    // Player Recognition
    private PlayerRecognitionEngine playerRecognitionEngine;
    private ExecutorService cameraExecutor;
    private boolean recognitionEnabled = true;
    private long lastRecognitionTime = 0;
    private static final long RECOGNITION_COOLDOWN_MS = 5000; // 5 seconds between updates

    // Camera UI Components
    private PreviewView cameraPreviewView;
    private Button toggleCameraButton;

    // UI Components
    private RecyclerView playersRecyclerView;
    private View bottomSheetView;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView scoreboardTitle;
    private TextView recognitionStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cricket_scoring);

        // Initialize UI components
        scoreboardTitle = findViewById(R.id.scoreboard_title);
        playersRecyclerView = findViewById(R.id.players_recycler_view);
        bottomSheetView = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        cameraPreviewView = findViewById(R.id.camera_preview_view);
        toggleCameraButton = findViewById(R.id.toggle_camera_button);
        recognitionStatusText = findViewById(R.id.recognition_status_text);

        // Set up camera button
        toggleCameraButton.setOnClickListener(v -> togglePlayerRecognition());

        // Set up RecyclerView
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        matchAdapter = new CricketMatchAdapter(this);
        playersRecyclerView.setAdapter(matchAdapter);

        // Set up scoring buttons
        setupScoringButtons();

        // Initialize cricket match
        cricketMatchService = CricketMatchService.getInstance(this);
        if (cricketMatchService.getCurrentMatch() == null) {
            currentMatch = cricketMatchService.createMatch("SPU", "Opponent", 20);
        } else {
            currentMatch = cricketMatchService.getCurrentMatch();
        }

        // Initialize player recognition engine
        playerRecognitionEngine = PlayerRecognitionEngine.getInstance(this);
        playerRecognitionEngine.addRecognitionListener(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Update UI
        updateUI();

        // Set up bottom sheet callbacks
        setupBottomSheetCallbacks();

        // Request camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        Button registerPlayerButton = findViewById(R.id.register_player_button);
        registerPlayerButton.setOnClickListener(v -> {
            Intent intent = new Intent(CricketScoringActivity.this, RegisterPlayerFaceActivity.class);
            startActivity(intent);
        });
    }

    private void setupScoringButtons() {
        Button buttonRun0 = findViewById(R.id.button_run_0);
        Button buttonRun1 = findViewById(R.id.button_run_1);
        Button buttonRun2 = findViewById(R.id.button_run_2);
        Button buttonRun3 = findViewById(R.id.button_run_3);
        Button buttonRun4 = findViewById(R.id.button_run_4);
        Button buttonRun6 = findViewById(R.id.button_run_6);

        buttonRun0.setOnClickListener(v -> addRuns(0));
        buttonRun1.setOnClickListener(v -> addRuns(1));
        buttonRun2.setOnClickListener(v -> addRuns(2));
        buttonRun3.setOnClickListener(v -> addRuns(3));
        buttonRun4.setOnClickListener(v -> addRuns(4));
        buttonRun6.setOnClickListener(v -> addRuns(6));

        // Set up wicket and extras buttons
        Button buttonWicket = findViewById(R.id.button_wicket);
        Button buttonExtras = findViewById(R.id.button_extras);
        Button buttonCompleteOver = findViewById(R.id.button_complete_over);

        buttonWicket.setOnClickListener(v -> addWicket());
        buttonExtras.setOnClickListener(v -> showExtrasDialog());
        buttonCompleteOver.setOnClickListener(v -> completeOver());
    }

    private void setupBottomSheetCallbacks() {
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    updateDetailedStats();
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                // Not needed
            }
        });
    }

    private void addRuns(int runs) {
        cricketMatchService.addRuns(runs);
        updateUI();
    }

    private void addWicket() {
        cricketMatchService.addWicket();
        updateUI();
    }

    private void showExtrasDialog() {
        // Implementation for extras dialog
        // Will be similar to wicket dialog but with options for wide, no ball, bye, etc.
    }

    private void completeOver() {
        cricketMatchService.completeOver();
        updateUI();
    }

    private void updateUI() {
        // Update match adapter
        matchAdapter.setMatch(currentMatch);

        // Update title
        String title = String.format("%s vs %s",
                currentMatch.getTeamBatting().getTeamName(),
                currentMatch.getTeamBowling().getTeamName());
        scoreboardTitle.setText(title);
    }

    private void updateDetailedStats() {
        // This method will update the bottom sheet with detailed statistics
        // Including partnership info, run rates, projected scores, etc.
    }

    /**
     * Player Recognition Implementation
     */

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
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        // Image analysis for player recognition
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            @androidx.camera.core.ExperimentalGetImage
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (recognitionEnabled) {
                    android.media.Image image = imageProxy.getImage();
                    if (image != null) {
                        // Only process every few seconds to avoid overloading
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastRecognitionTime > RECOGNITION_COOLDOWN_MS) {
                            playerRecognitionEngine.analyzeFrame(image);
                            lastRecognitionTime = currentTime;
                        }
                    }
                }
                imageProxy.close();
            }
        });

        // Select back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            // Unbind any bound use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            Camera camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis);

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void togglePlayerRecognition() {
        recognitionEnabled = !recognitionEnabled;
        if (recognitionEnabled) {
            toggleCameraButton.setText(R.string.disable_recognition);
            recognitionStatusText.setText(R.string.recognition_active);
            cameraPreviewView.setVisibility(View.VISIBLE);
        } else {
            toggleCameraButton.setText(R.string.enable_recognition);
            recognitionStatusText.setText(R.string.recognition_disabled);
            cameraPreviewView.setVisibility(View.INVISIBLE);
        }
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
    public void onPlayerRecognized(Player player, float confidence) {
        runOnUiThread(() -> {
            // Update player in match stats if relevant
            boolean isPlaying = currentMatch.isPlayerInMatch(player);

            if (isPlaying) {
                Snackbar.make(findViewById(R.id.coordinator_layout),
                        "Recognized player: " + player.getName() + " (confidence: " +
                                String.format("%.2f", confidence) + ")",
                        Snackbar.LENGTH_SHORT).show();

                // Automatically select player for scoring if appropriate
                cricketMatchService.setActivePlayer(player);
                updateUI();
            }
        });
    }

    @Override
    public void onRecognitionError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Recognition error: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerRecognitionEngine.removeRecognitionListener(this);
        cameraExecutor.shutdown();
    }
}