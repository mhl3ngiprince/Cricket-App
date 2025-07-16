package com.finedine.spucricketclub;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.finedine.spucricketclub.interfaces.PitchAnalysisListener;
import com.finedine.spucricketclub.ml.PitchAnalyzer;
import com.finedine.spucricketclub.models.PitchAnalysisResult;
import com.finedine.spucricketclub.ui.PitchHeatmapView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for cricket pitch analysis using computer vision
 */
public class PitchVisionActivity extends AppCompatActivity implements PitchAnalysisListener {

    private static final String TAG = "PitchVisionActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    // UI components
    private PreviewView previewView;
    private Button calibrateButton;
    private Button analyzeButton;
    private Button saveButton;
    private CardView resultsCard;
    private LinearLayout pitchStatsContainer;
    private ProgressBar calibrationProgress;
    private TextView statusText;
    private TextView pitchTypeText;
    private TextView grassCoverageText;
    private TextView cracksText;
    private TextView moistureText;
    private TextView bounceText;
    private TextView turnText;
    private TextView recommendationText;
    private PitchHeatmapView heatmapView;

    // Bottom sheet for detailed stats
    private View bottomSheetView;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    // Analysis components
    private ProcessCameraProvider cameraProvider;
    private PitchAnalyzer pitchAnalyzer;
    private ExecutorService cameraExecutor;

    // Analysis state
    private boolean isCalibrated = false;
    private boolean isAnalyzing = false;
    private PitchAnalysisResult latestResult;
    private Map<String, Float> historicalData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pitch_vision);

        // Initialize UI components
        setupUI();

        // Initialize pitch analyzer
        pitchAnalyzer = new PitchAnalyzer(this, this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Request camera permission if needed
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void setupUI() {
        previewView = findViewById(R.id.camera_preview);
        calibrateButton = findViewById(R.id.btn_calibrate);
        analyzeButton = findViewById(R.id.btn_analyze);
        saveButton = findViewById(R.id.btn_save_analysis);
        resultsCard = findViewById(R.id.card_results);
        pitchStatsContainer = findViewById(R.id.container_pitch_stats);
        calibrationProgress = findViewById(R.id.progress_calibration);
        statusText = findViewById(R.id.text_status);
        pitchTypeText = findViewById(R.id.text_pitch_type);
        grassCoverageText = findViewById(R.id.text_grass_coverage);
        cracksText = findViewById(R.id.text_cracks);
        moistureText = findViewById(R.id.text_moisture);
        bounceText = findViewById(R.id.text_bounce);
        turnText = findViewById(R.id.text_turn);
        recommendationText = findViewById(R.id.text_recommendation);
        heatmapView = findViewById(R.id.pitch_heatmap_view);

        // Set up bottom sheet
        bottomSheetView = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Set up button click listeners
        calibrateButton.setOnClickListener(v -> startCalibration());
        analyzeButton.setOnClickListener(v -> startAnalysis());
        analyzeButton.setEnabled(false);

        saveButton.setOnClickListener(v -> savePitchAnalysis());
        saveButton.setEnabled(false);

        resultsCard.setVisibility(View.GONE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }

        // Unbind any bound use cases
        cameraProvider.unbindAll();

        // Camera selector - back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image analysis use case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, pitchAnalyzer);

        // Bind use cases to camera
        Camera camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalysis);
    }

    private void startCalibration() {
        statusText.setText("Calibrating... Point camera at pitch");
        calibrationProgress.setVisibility(View.VISIBLE);
        calibrateButton.setEnabled(false);
        analyzeButton.setEnabled(false);
        resultsCard.setVisibility(View.GONE);

        pitchAnalyzer.startCalibration();
    }

    private void startAnalysis() {
        if (!isCalibrated) {
            Toast.makeText(this, "Please calibrate first", Toast.LENGTH_SHORT).show();
            return;
        }

        statusText.setText("Analyzing pitch conditions...");
        analyzeButton.setText("Stop Analysis");
        isAnalyzing = true;
        resultsCard.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        pitchAnalyzer.startAnalysis();
    }

    private void stopAnalysis() {
        statusText.setText("Analysis stopped");
        analyzeButton.setText("Start Analysis");
        isAnalyzing = false;
        saveButton.setEnabled(true);

        pitchAnalyzer.stop();
    }

    private void savePitchAnalysis() {
        if (latestResult == null) {
            return;
        }

        // Save analysis result to database
        // This would be implemented to store results for later reference

        Snackbar.make(findViewById(R.id.coordinator_layout),
                "Pitch analysis saved", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onCalibrationComplete() {
        runOnUiThread(() -> {
            calibrationProgress.setVisibility(View.GONE);
            statusText.setText("Calibration complete");
            analyzeButton.setEnabled(true);
            calibrateButton.setEnabled(true);
            isCalibrated = true;
        });
    }

    @Override
    public void onPitchAnalysisResult(PitchAnalysisResult result) {
        latestResult = result;

        // Store historical data for heatmap
        historicalData.put("grass_" + System.currentTimeMillis(), result.getGrassCoverage());
        historicalData.put("moisture_" + System.currentTimeMillis(), result.getMoisture());

        runOnUiThread(() -> {
            // Update UI with analysis results
            pitchTypeText.setText(result.getPitchType());
            grassCoverageText.setText(String.format("%.1f%%", result.getGrassCoverage()));
            cracksText.setText(String.format("%.1f%%", result.getCracks()));
            moistureText.setText(String.format("%.1f%%", result.getMoisture()));
            bounceText.setText(String.format("%.1f/10", result.getPredictedBounce()));
            turnText.setText(String.format("%.1f/10", result.getPredictedTurn()));
            recommendationText.setText(result.getBowlingRecommendation());

            // Update heatmap
            updateHeatmap(result);

            // Set text colors based on values
            setPitchTypeColor(pitchTypeText, result.getPitchType());
            setBounceColor(bounceText, result.getPredictedBounce());
            setTurnColor(turnText, result.getPredictedTurn());
        });
    }

    private void updateHeatmap(PitchAnalysisResult result) {
        // Update the heatmap view with latest data
        Map<String, Float> heatmapData = new HashMap<>();
        heatmapData.put("grass", result.getGrassCoverage());
        heatmapData.put("moisture", result.getMoisture());
        heatmapData.put("cracks", result.getCracks());
        heatmapData.put("bounce", result.getPredictedBounce() * 10); // Scale to percentage
        heatmapData.put("turn", result.getPredictedTurn() * 10);     // Scale to percentage

        heatmapView.updateData(heatmapData);
    }

    private void setPitchTypeColor(TextView textView, String pitchType) {
        int color;
        switch (pitchType) {
            case "Green":
                color = ContextCompat.getColor(this, R.color.spu_success);
                break;
            case "Dry/Cracked":
                color = ContextCompat.getColor(this, R.color.spu_danger);
                break;
            case "Damp":
                color = ContextCompat.getColor(this, R.color.spu_info);
                break;
            case "Dusty":
                color = ContextCompat.getColor(this, R.color.spu_warning);
                break;
            default:
                color = ContextCompat.getColor(this, R.color.spu_gold);
                break;
        }
        textView.setTextColor(color);
    }

    private void setBounceColor(TextView textView, float bounce) {
        int color;
        if (bounce > 7.5f) {
            color = ContextCompat.getColor(this, R.color.spu_danger);
        } else if (bounce > 5.0f) {
            color = ContextCompat.getColor(this, R.color.spu_warning);
        } else if (bounce > 3.0f) {
            color = ContextCompat.getColor(this, R.color.spu_success);
        } else {
            color = ContextCompat.getColor(this, R.color.spu_info);
        }
        textView.setTextColor(color);
    }

    private void setTurnColor(TextView textView, float turn) {
        int color;
        if (turn > 7.5f) {
            color = ContextCompat.getColor(this, R.color.spu_danger);
        } else if (turn > 5.0f) {
            color = ContextCompat.getColor(this, R.color.spu_warning);
        } else if (turn > 3.0f) {
            color = ContextCompat.getColor(this, R.color.spu_success);
        } else {
            color = ContextCompat.getColor(this, R.color.spu_info);
        }
        textView.setTextColor(color);
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
                Toast.makeText(this, "Camera permissions are required for pitch analysis",
                        Toast.LENGTH_SHORT).show();
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