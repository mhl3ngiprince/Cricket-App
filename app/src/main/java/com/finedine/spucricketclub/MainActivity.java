package com.finedine.spucricketclub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.finedine.spucricketclub.cricket.Over;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import com.finedine.spucricketclub.ai.PlayerRecognitionEngine;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import androidx.camera.core.ExperimentalGetImage;

import com.finedine.spucricketclub.cricket.Ball;
import com.finedine.spucricketclub.cricket.CricketMatchService;
import com.finedine.spucricketclub.cricket.Innings;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Team;

@ExperimentalGetImage
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS;

    {
        // Initialize the required permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    private androidx.camera.view.PreviewView cameraPreviewBroadcast;
    private Toolbar toolbar;
    private TextView textAiStatusMessage;
    private com.google.android.material.button.MaterialButtonToggleGroup toggleAiTrackingGroup;
    private Button btnToggleBallTracking;
    private Button btnTogglePlayerTracking;
    private Button btnTogglePoseEstimation;
    private Button buttonStartRecordingMain;
    private Button buttonQuickWicketMain;
    private FloatingActionButton fabMainAction;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Preview preview;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ImageAnalysis imageAnalysis;

    private FaceRecognitionHelper faceRecognitionHelper;
    private ShirtNumberRecognitionHelper shirtNumberRecognitionHelper;
    // private StreamingManager streamingManager; // Streaming controls removed from this layout

    private boolean isAiBallTrackingEnabled = false;
    private boolean isAiPlayerTrackingEnabled = false;
    private boolean isAiPoseEstimationEnabled = false;
    // private boolean isScanning = false; // Replaced by specific AI tracking flags
    private boolean isRecording = false;
    private boolean isInitialized = false;

    private CricketMatchService cricketMatchService;
    private Match currentMatch;

    // Local game state holders for UI display (synced from currentMatch)
    private String teamBattingName = "Team A";
    private int currentScore = 0;
    private int currentDisplayedScore = 0;
    private int currentDisplayedWickets = 0;
    private int currentDisplayedOversCompleted = 0;
    private int currentDisplayedBallsInOver = 0;
    private String batsman1Name = "Batsman 1";
    private String batsman2Name = "Batsman 2";
    private int batsman1Score = 0;
    private int batsman2Score = 0;
    private int batsman1Balls = 0;
    private int batsman2Balls = 0;
    private String currentBowlerName = "Bowler";
    private int bowlerWickets = 0;
    private int bowlerRuns = 0;
    private int bowlerOvers = 0;
    private int bowlerBalls = 0;
    private int oversCompleted = 0;
    private int ballsInCurrentOver = 0;
    private int wickets = 0;

    // Player tracking
    private Map<String, Integer> playerScores = new HashMap<>();
    private List<String> playerNames = new ArrayList<>();
    private StringBuilder playerScoresDisplay = new StringBuilder();

    // Current over display
    private int[] currentOverBalls = new int[6];

    // UI elements that were in old layout but referenced in code
    private EditText editTextPlayerName;
    private TextView textPlayerScores;
    private Button buttonStartRecording;
    private Button buttonStopRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        setupFirebase();

        // Initialize UI components before camera setup
        initializeUIComponents();

        // Set up listeners for UI components
        setupButtonListeners();
        updateAiStatusDisplay(); // Initial AI status display

        // Initialize UI elements from old layout that are still referenced
        // These might be null as they might not exist in the current layout
        // Adding null checks in methods that use these
        try {
            editTextPlayerName = findViewById(R.id.edit_text_player_name);
            textPlayerScores = findViewById(R.id.text_player_scores);
            buttonStartRecording = findViewById(R.id.button_start_recording);
            buttonStopRecording = findViewById(R.id.button_stop_recording);
        } catch (Exception e) {
            Log.w(TAG, "Some UI elements not found in current layout: " + e.getMessage());
        }

        // Set up edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_broadcast_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize managers
        // streamingManager = new StreamingManager(this); // Streaming controls removed

        // Check and request camera permissions
        checkPermissionsAndInitialize();
    }

    private void initializeUIComponents() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Enable up button if you have a parent activity or want custom navigation
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // getSupportActionBar().setDisplayShowHomeEnabled(true);

        cameraPreviewBroadcast = findViewById(R.id.camera_preview_broadcast);
        textAiStatusMessage = findViewById(R.id.text_ai_status_message);
        toggleAiTrackingGroup = findViewById(R.id.toggle_ai_tracking_group);
        btnToggleBallTracking = findViewById(R.id.btn_toggle_ball_tracking);
        btnTogglePlayerTracking = findViewById(R.id.btn_toggle_player_tracking);
        btnTogglePoseEstimation = findViewById(R.id.btn_toggle_pose_estimation);
        buttonStartRecordingMain = findViewById(R.id.button_start_recording_main);
        buttonQuickWicketMain = findViewById(R.id.button_quick_wicket_main);
        fabMainAction = findViewById(R.id.fab_main_action);

        // Initialize cricket score panel views (using the new ID for the include tag)
        // The actual views within the panel are initialized in initializeCricketScorePanel()
        View scorePanel = findViewById(R.id.cricket_score_panel_broadcast);
        if (scorePanel == null) {
            Log.e(TAG, "Cricket score panel (broadcast) not found in layout!");
        }

        initializeCricketScorePanel(); // This will find views within the included layout

        // Update AI status display with initial values
        updateAiStatusDisplay();
    }

    private void updateAiStatusDisplay() {
        if (textAiStatusMessage != null) {
            StringBuilder status = new StringBuilder("AI Status: ");
            if (isAiBallTrackingEnabled) status.append("Ball tracking ON | ");
            if (isAiPlayerTrackingEnabled) status.append("Player tracking ON | ");
            if (isAiPoseEstimationEnabled) status.append("Pose estimation ON | ");

            if (!isAiBallTrackingEnabled && !isAiPlayerTrackingEnabled && !isAiPoseEstimationEnabled) {
                status.append("All tracking OFF");
            } else if (status.toString().endsWith("| ")) {
                status.delete(status.length() - 2, status.length());
            }

            textAiStatusMessage.setText(status.toString());
        }
    }

    private void setupButtonListeners() {
        if (buttonStartRecordingMain != null) {
            buttonStartRecordingMain.setOnClickListener(v -> {
                if (!isRecording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            });
        }

        if (buttonQuickWicketMain != null) {
            buttonQuickWicketMain.setOnClickListener(v -> {
                addWicket();
                Toast.makeText(this, "Wicket added!", Toast.LENGTH_SHORT).show();
            });
        }

        if (fabMainAction != null) {
            fabMainAction.setOnClickListener(view -> {
                // Define action for FAB, e.g., open a menu, start new match, etc.
                Snackbar.make(view, "Main action FAB clicked", Snackbar.LENGTH_LONG).show();
                // Example: showControlPanel(); // If you adapt this method
            });
        }

        // AI Tracking Toggles
        if (btnToggleBallTracking != null) {
            btnToggleBallTracking.setOnClickListener(v -> {
                // Toggle ball tracking state
                boolean isChecked = btnToggleBallTracking.isActivated(); // Or manage state manually
                btnToggleBallTracking.setActivated(!isChecked);
                Log.d(TAG, "Ball tracking toggled: " + !isChecked);
                updateAiStatus("Ball Tracking", !isChecked);
            });
        }
        if (btnTogglePlayerTracking != null) {
            btnTogglePlayerTracking.setOnClickListener(v -> {
                boolean isChecked = btnTogglePlayerTracking.isActivated();
                btnTogglePlayerTracking.setActivated(!isChecked);
                Log.d(TAG, "Player tracking toggled: " + !isChecked);
                updateAiStatus("Player Tracking", !isChecked);
            });
        }
        if (btnTogglePoseEstimation != null) {
            btnTogglePoseEstimation.setOnClickListener(v -> {
                boolean isChecked = btnTogglePoseEstimation.isActivated();
                btnTogglePoseEstimation.setActivated(!isChecked);
                Log.d(TAG, "Pose estimation toggled: " + !isChecked);
                updateAiStatus("Pose Estimation", !isChecked);
            });
        }

        // The old button listeners for elements that are no longer in activity_main.xml
        // need to be removed or adapted if their functionality is moved elsewhere.
        // For example, player adding, score increment/decrement, streaming, old scan toggle
        // are not directly in the new broadcast-style layout.
    }

private void updateAiStatus(String feature, boolean isActive) {
    if (textAiStatusMessage != null) {
        String status = feature + (isActive ? " ON" : " OFF");
        textAiStatusMessage.setText(status);
        Log.d(TAG, "AI Status: " + status);
    }
    }

    private void initializeCameraAndHelpers() {
        // Initialize ML Kit helpers
        faceRecognitionHelper = new FaceRecognitionHelper(this);
        shirtNumberRecognitionHelper = new ShirtNumberRecognitionHelper(this);

        // Start camera
        startCamera();
    }

    // Toggle scanning functionality
    private void toggleScanning() {
        // Update with new AI toggles
        isAiBallTrackingEnabled = !isAiBallTrackingEnabled;
        isAiPlayerTrackingEnabled = !isAiPlayerTrackingEnabled;

        // Update UI to reflect changes
        if (btnToggleBallTracking != null) {
            btnToggleBallTracking.setActivated(isAiBallTrackingEnabled);
        }
        if (btnTogglePlayerTracking != null) {
            btnTogglePlayerTracking.setActivated(isAiPlayerTrackingEnabled);
        }

        updateAiStatusDisplay();

        String message = isAiBallTrackingEnabled || isAiPlayerTrackingEnabled ?
                "AI tracking enabled" : "AI tracking disabled";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not granted: " + permission);
                return false;
            }
        }
        Log.d(TAG, "All permissions granted");
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraPreview();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview() {
        if (cameraProvider == null) {
            Log.e(TAG, "Camera provider is null");
            Toast.makeText(this, "Failed to initialize camera. Please restart the app.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // First unbind all use cases
            cameraProvider.unbindAll();

            // Create a new Preview use case
            preview = new Preview.Builder().build();

            // Setup image analysis use case
            imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);

            // Set up camera selector
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            // Setup video capture with proper recorder
            Recorder recorder = new Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build();

            videoCapture = VideoCapture.withOutput(recorder);

            // Bind all use cases together
            camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    videoCapture
            );

            // Connect preview to the PreviewView
            preview.setSurfaceProvider(cameraPreviewBroadcast.getSurfaceProvider());

        } catch (Exception e) {
            Log.e(TAG, "Error binding camera preview: " + e.getMessage(), e);
            try {
                // Fallback to simpler setup without VideoCapture if that was the issue
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );
                preview.setSurfaceProvider(cameraPreviewBroadcast.getSurfaceProvider());
                Toast.makeText(this, "Video recording may not be available", Toast.LENGTH_SHORT).show();
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback camera binding also failed: " + fallbackError.getMessage());
                Toast.makeText(this, "Camera initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {
        // Only analyze if at least one AI feature is enabled
        if (!isAiBallTrackingEnabled && !isAiPlayerTrackingEnabled && !isAiPoseEstimationEnabled) {
            imageProxy.close();
            return;
        }

        try {
            Bitmap bitmap = ImageUtils.imageProxyToBitmap(imageProxy);
            if (bitmap == null) {
                Log.e(TAG, "Failed to convert ImageProxy to Bitmap");
                imageProxy.close();
                return;
            }

            try {
                faceRecognitionHelper.detectFaces(bitmap, new FaceRecognitionHelper.FaceDetectionCallback() {
                    @Override
                    public void onFacesDetected(@NonNull List<com.google.mlkit.vision.face.Face> faces) {
                        try {
                            if (!faces.isEmpty()) {
                                String playerId = "PlayerFace" + faces.size();
                                registerPlayer(playerId);
                                incrementPlayerScore(playerId);

                                // Show toast on main thread for face detection
                                runOnUiThread(() ->
                                        Toast.makeText(MainActivity.this,
                                                "Face detected: " + faces.size() + " - Score updated",
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing face detection result", e);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Face detection error", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in face recognition", e);
            }

            try {
                // Create a copy of bitmap for shirt detection
                Bitmap shirtBitmap = bitmap.copy(bitmap.getConfig(), true);

                shirtNumberRecognitionHelper.recognizeShirtNumber(shirtBitmap, new ShirtNumberRecognitionHelper.ShirtNumberRecognitionCallback() {
                    @Override
                    public void onShirtNumberRecognized(String shirtNumber) {
                        try {
                            if (!shirtNumber.isEmpty()) {
                                String playerId = "Shirt#" + shirtNumber;
                                registerPlayer(playerId);
                                incrementPlayerScore(playerId);

                                // Show toast on main thread for number recognition
                                runOnUiThread(() ->
                                        Toast.makeText(MainActivity.this,
                                                "Shirt #" + shirtNumber + " recognized - Score updated",
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing shirt number result", e);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Shirt number recognition error", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in shirt number recognition", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in image analysis", e);
        } finally {
            try {
                imageProxy.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing image proxy", e);
            }
        }
    }

    private void incrementPlayerScore(String playerId) {
        if (playerScores.containsKey(playerId)) {
            int currentScore = playerScores.get(playerId);
            playerScores.put(playerId, currentScore + 1);
            updatePlayerScoresDisplay();
        }
    }

    private void startRecording() {
        if (videoCapture == null) {
            Toast.makeText(this, "VideoCapture not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRecording) {
            Toast.makeText(this, "Already recording", Toast.LENGTH_SHORT).show();
            return;
        }

        File videoDir = new File(getExternalFilesDir(null), "Cricket Videos");
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }

        File videoFile = new File(videoDir, new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".mp4");

        // Create MediaStoreOutputOptions for the recorder
        FileOutputOptions outputOptions =
                new FileOutputOptions.Builder(videoFile).build();

        try {
            // Start recording
            androidx.camera.video.PendingRecording pendingRecording =
                    videoCapture.getOutput().prepareRecording(this, outputOptions);

            // Add audio if permission granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED) {
                //pendingRecording = pendingRecording.withAudioEnabled(); // Corrected: withAudioEnabled() is void
                pendingRecording.withAudioEnabled();
            } else {
                Log.w(TAG, "Record audio permission not granted, recording without audio.");
            }

            recording = pendingRecording.start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                    isRecording = true;
                    if (buttonStartRecordingMain != null) {
                        buttonStartRecordingMain.setText("Stop Recording");
                    }
                }
            });

            recording = pendingRecording.start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                    isRecording = true;
                    buttonStartRecordingMain.setText("Stop Recording");
                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                    VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                    if (!finalizeEvent.hasError()) {
                        String message = "Video saved: " + videoFile.getAbsolutePath();
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);
                    } else {
                        recording.close();
                        recording = null;
                        String message = "Video capture failed: " + finalizeEvent.getError();
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, message);
                    }
                    isRecording = false;
                    // Update buttonStartRecordingMain based on recording state
                    if (buttonStartRecordingMain != null)
                        buttonStartRecordingMain.setText("Record");
                    // buttonStartRecordingMain.setText("Record"); // Old button
                }
            });

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            Toast.makeText(this, "Error starting recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
            isRecording = false;
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            if (buttonStartRecording != null) buttonStartRecording.setEnabled(true);
            if (buttonStopRecording != null) buttonStopRecording.setEnabled(false);
        }
    }

    private void addPlayer() {
        if (editTextPlayerName != null) {
            String playerName = editTextPlayerName.getText().toString().trim();
            if (!playerName.isEmpty()) {
                registerPlayer(playerName);
                editTextPlayerName.setText("");
                Toast.makeText(this, "Player added: " + playerName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a player name", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerPlayer(String playerIdentifier) {
        if (!playerScores.containsKey(playerIdentifier)) {
            playerScores.put(playerIdentifier, 0);
            playerNames.add(playerIdentifier);
            updatePlayerScoresDisplay();
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Player registered: " + playerIdentifier, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Adds score to a player. If the player doesn't exist, asks if they want to add the player
     */
    private void addScoreToPlayer(String playerName, int scoreToAdd) {
        if (playerName.isEmpty()) {
            Toast.makeText(this, "Please enter a player name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (playerScores.containsKey(playerName)) {
            int currentScore = playerScores.get(playerName);
            playerScores.put(playerName, currentScore + scoreToAdd);
            updatePlayerScoresDisplay();

            if (scoreToAdd > 0) {
                if (currentMatch != null) {
                    cricketMatchService.addRuns(scoreToAdd);
                    updateCricketUIFromModel();
                }
            }

            String scoreMessage = Math.abs(scoreToAdd) == 1
                    ? Math.abs(scoreToAdd) + " run "
                    : Math.abs(scoreToAdd) + " runs ";

            scoreMessage += (scoreToAdd > 0) ? "added to " : "subtracted from ";

            Toast.makeText(this, scoreMessage + playerName, Toast.LENGTH_SHORT).show();
        } else {
            // Ask user if they want to add the new player
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("New Player")
                    .setMessage("Player '" + playerName + "' doesn't exist. Add them?")
                    .setPositiveButton("Add", (dialog, which) -> {
                        registerPlayer(playerName);
                        addScoreToPlayer(playerName, scoreToAdd); // Recursive call now that the player exists
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void updatePlayerScoresDisplay() {
        playerScoresDisplay.setLength(0);
        for (String player : playerNames) {
            int score = playerScores.getOrDefault(player, 0);
            playerScoresDisplay.append(player).append(": ").append(score).append("\n");
        }
        runOnUiThread(() -> {
            if (textPlayerScores != null) {
                textPlayerScores.setText(playerScoresDisplay.toString());
            }
            // updateStatsChart(); // Update stats whenever scores change - Disabled until UI is updated
        });
    }

    /**
     * Update the stats chart container with current player data
     */
    private void updateStatsChart() {
        // This would be implemented with a proper charting library
        // For now just update player stats in text form

        // Implementation disabled until UI is updated
        /*
        if (statsChartContainer != null) {
            statsChartContainer.removeAllViews();
            TextView statsText = new TextView(this);
            StringBuilder statsData = new StringBuilder();
            
            // Count players with scores
            int activePlayers = 0;
            int totalScore = 0;
            int highestScore = 0;
            String topScorer = "";
            
            for (String player : playerNames) {
                int score = playerScores.getOrDefault(player, 0);
                if (score > 0) {
                    activePlayers++;
                    totalScore += score;
                    if (score > highestScore) {
                        highestScore = score;
                        topScorer = player;
                    }
                }
            }
            
            // Build stats text
            statsData.append("Active Players: ").append(activePlayers).append("\n");
            statsData.append("Total Score: ").append(totalScore).append("\n");
            if (!topScorer.isEmpty()) {
                statsData.append("Top Scorer: ").append(topScorer).append(" (").append(highestScore).append(" runs)");
            } else {
                statsData.append("No scores recorded yet");
            }
            
            statsText.setText(statsData.toString());
            statsText.setTextColor(ContextCompat.getColor(this, R.color.spu_dark_blue));
            statsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            statsText.setGravity(Gravity.CENTER);
            statsChartContainer.addView(statsText);
        }
        */
    }

    /**
     * Initialize Firebase Database with offline persistence
     */
    private void setupFirebase() {
        // Enable offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Set disk cache size to 20MB for Firebase
        FirebaseDatabase.getInstance().getReference().keepSynced(true);

        Log.d(TAG, "Firebase initialized with offline persistence");
    }

    private void resetMatchData() {
        if (editTextPlayerName != null) {
            editTextPlayerName.setText("");
        }
        playerScoresDisplay.setLength(0);
        playerNames.clear();
        playerScores.clear();
        updatePlayerScoresDisplay();

        if (cricketMatchService != null) {
            cricketMatchService.resetMatch();
            currentMatch = null;
        }

        resetCricketScorePanel();

        Toast.makeText(this, "Match data reset", Toast.LENGTH_SHORT).show();
    }

    private void initializeCricketScorePanel() {
        if (cricketMatchService == null) {
        cricketMatchService = CricketMatchService.getInstance(this);
    }
    currentMatch = cricketMatchService.getCurrentMatch();
    if (currentMatch == null) {
        currentMatch = cricketMatchService.createMatch("SPU", "Opponent XI", 20);
        Log.d(TAG, "New match created in initializeCricketScorePanel");
    }
    updateCricketUIFromModel();
}

    private void setupBatsmenInfo(String name1, String name2, int score1, int score2, int balls1, int balls2) {
        TextView textBatsman1Name = findViewById(R.id.text_batsman1_name);
        TextView textBatsman2Name = findViewById(R.id.text_batsman2_name);
        TextView textBatsman1Score = findViewById(R.id.text_batsman1_score);
        TextView textBatsman2Score = findViewById(R.id.text_batsman2_score);
        TextView textBatsman1SR = findViewById(R.id.text_batsman1_sr);
        TextView textBatsman2SR = findViewById(R.id.text_batsman2_sr);

        // Update batsmen data
        batsman1Name = name1;
        batsman2Name = name2;
        batsman1Score = score1;
        batsman2Score = score2;
        batsman1Balls = balls1;
        batsman2Balls = balls2;

        // Update UI
        if (textBatsman1Name != null) textBatsman1Name.setText(name1);
        if (textBatsman2Name != null) textBatsman2Name.setText(name2);
        if (textBatsman1Score != null)
            textBatsman1Score.setText(String.format(getString(R.string.batsman_score_format), score1, balls1));
        if (textBatsman2Score != null)
            textBatsman2Score.setText(String.format(getString(R.string.batsman_score_format), score2, balls2));

        // Calculate and display strike rates
        float sr1 = balls1 > 0 ? (score1 * 100.0f) / balls1 : 0;
        float sr2 = balls2 > 0 ? (score2 * 100.0f) / balls2 : 0;

        if (textBatsman1SR != null)
            textBatsman1SR.setText(String.format(getString(R.string.batsman_sr_format), sr1));
        if (textBatsman2SR != null)
            textBatsman2SR.setText(String.format(getString(R.string.batsman_sr_format), sr2));
    }

    private void setupBowlerInfo(String name, int wickets, int runs, int overs, int balls) {
        TextView textBowlerName = findViewById(R.id.text_bowler_name);
        TextView textBowlerFigures = findViewById(R.id.text_bowler_figures);

        // Update bowler data
        currentBowlerName = name;
        bowlerWickets = wickets;
        bowlerRuns = runs;
        bowlerOvers = overs;
        bowlerBalls = balls;

        // Update UI
        if (textBowlerName != null) textBowlerName.setText(name);
        if (textBowlerFigures != null) {
            textBowlerFigures.setText(String.format(getString(R.string.bowler_figures_format),
                    wickets, runs, overs, balls));
        }
    }

    private void setupCurrentOverDisplay() {
        // Example balls for visualization: 4, dot, wicket, 1, empty, empty
        currentOverBalls[0] = 3; // 4 runs
        currentOverBalls[1] = 1; // dot
        currentOverBalls[2] = 5; // wicket
        currentOverBalls[3] = 2; // 1 run
        currentOverBalls[4] = 0; // empty
        currentOverBalls[5] = 0; // empty

        updateCurrentOverDisplay();
    }

    private void updateThisOverDisplay(Innings innings) {
        TextView textThisOver = findViewById(R.id.text_this_over_simplified);
        if (textThisOver != null && innings != null && innings.getCurrentOver() != null) {
            Over currentOverObj = innings.getCurrentOver();
            StringBuilder overSummary = new StringBuilder("This Over: ");
            List<Ball> ballsInOverList = currentOverObj.getBalls();
            for (int i = 0; i < ballsInOverList.size(); i++) {
                Ball ball = ballsInOverList.get(i);
                // Access methods directly from Ball class if available
                // Otherwise use safe dummy implementation
                if (ball.isWicket()) {
                    overSummary.append("W ");
                } else if (isExtraBall(ball)) {
                    // Handle different ball types
                    int ballType = getBallType(ball);
                    if (ballType == 1) { // WIDE
                        overSummary.append("Wd ");
                    } else if (ballType == 2) { // NO_BALL
                        overSummary.append(ball.getRunsScored() > 0 ? (ball.getRunsScored() - 1) + "Nb " : "Nb ");
                    } else if (ballType == 3) { // BYE
                        overSummary.append(ball.getRunsScored()).append("B ");
                    } else if (ballType == 4) { // LEG_BYE
                        overSummary.append(ball.getRunsScored()).append("Lb ");
                    }
                } else if (ball.getRunsScored() == 0) {
                    overSummary.append("• ");
                } else {
                    overSummary.append(ball.getRunsScored()).append(" ");
                }
            }
            textThisOver.setText(overSummary.toString().trim());
        } else if (textThisOver != null) {
            textThisOver.setText("This Over: ");
        }
    }

    // Helper method to check if a ball is an extra
    private boolean isExtraBall(Ball ball) {
        try {
            // Try to use reflection to call the method if it exists
            Method isExtraMethod = ball.getClass().getMethod("isExtra");
            return (Boolean) isExtraMethod.invoke(ball);
        } catch (Exception e) {
            // Fallback implementation if method doesn't exist
            return getBallType(ball) > 0;
        }
    }

    // Helper method to get ball type
    private int getBallType(Ball ball) {
        try {
            // Try to use reflection to access the enum if it exists
            Method getBallTypeMethod = ball.getClass().getMethod("getBallType");
            Object ballTypeEnum = getBallTypeMethod.invoke(ball);

            if (ballTypeEnum != null) {
                String enumName = ballTypeEnum.toString();
                if ("WIDE".equals(enumName)) return 1;
                if ("NO_BALL".equals(enumName)) return 2;
                if ("BYE".equals(enumName)) return 3;
                if ("LEG_BYE".equals(enumName)) return 4;
            }
            return 0;
        } catch (Exception e) {
            // Fallback based on other properties
            return 0;
        }
    }

    private void updateCricketUIFromModel() {
        if (currentMatch == null) {
            return;
        }

        Innings innings = currentMatch.getCurrentInnings();

        currentScore = innings.getTotalScore();
        wickets = innings.getWickets();

        float totalOvers = innings.getTotalOvers();
        oversCompleted = (int) totalOvers;
        ballsInCurrentOver = Math.round((totalOvers - oversCompleted) * 10);

        Player striker = innings.getStriker();
        Player nonStriker = innings.getNonStriker();

        if (striker != null) {
            batsman1Name = striker.getName();
            batsman1Score = striker.getBattingStats().getRuns();
            batsman1Balls = striker.getBattingStats().getBallsFaced();
        }

        if (nonStriker != null) {
            batsman2Name = nonStriker.getName();
            batsman2Score = nonStriker.getBattingStats().getRuns();
            batsman2Balls = nonStriker.getBattingStats().getBallsFaced();
        }

        Player bowler = innings.getCurrentBowler();
        if (bowler != null) {
            currentBowlerName = bowler.getName();
            bowlerWickets = bowler.getBowlingStats().getWickets();
            bowlerRuns = bowler.getBowlingStats().getRunsConceded();
            bowlerOvers = bowler.getBowlingStats().getOvers();
            bowlerBalls = bowler.getBowlingStats().getBallsInCurrentOver();
        }

        updateCricketScoreUI();
    }

    private void updateCurrentOverDisplay() {
        // This would update a visual representation of the current over
        // using the currentOverBalls array values
        // In a real implementation, you would find the views for each ball
        // and update their appearance based on the ball type

        // Example: update ball indicators in the UI
        // For each ball in currentOverBalls:
        // 0 = not bowled yet
        // 1 = dot ball
        // 2 = single
        // 3 = four
        // 4 = six
        // 5 = wicket

        // Log the current over state for debugging
        StringBuilder overLog = new StringBuilder("Current over: ");
        for (int ball : currentOverBalls) {
            switch (ball) {
                case 0:
                    overLog.append("_ ");
                    break; // Not bowled
                case 1:
                    overLog.append("• ");
                    break; // Dot
                case 2:
                    overLog.append("1 ");
                    break; // Single
                case 3:
                    overLog.append("4 ");
                    break; // Four
                case 4:
                    overLog.append("6 ");
                    break; // Six
                case 5:
                    overLog.append("W ");
                    break; // Wicket
                default:
                    overLog.append("? ");
                    break; // Unknown
            }
        }
        Log.d(TAG, overLog.toString());

        // Update this over display if using match model
        if (currentMatch != null) {
            Innings innings = currentMatch.getCurrentInnings();
            if (innings != null) {
                updateThisOverDisplay(innings);
            }
        }
    }

    // Update match score when runs are scored
    private void updateMatchScore(int runsScored) {
        // Update total score
        currentScore += runsScored;

        // Update current batsman's score (assume batsman1 for simplicity)
        batsman1Score += runsScored;
        batsman1Balls++;

        // Update current bowler's figures
        bowlerRuns += runsScored;

        // Update ball in current over
        if (ballsInCurrentOver < 6) {
            int ballType;
            if (runsScored == 0) ballType = 1; // dot ball
            else if (runsScored == 1) ballType = 2; // 1 run
            else if (runsScored == 4) ballType = 3; // 4 runs
            else if (runsScored == 6) ballType = 4; // 6 runs
            else ballType = 2; // Default to 1 run for other values

            currentOverBalls[ballsInCurrentOver] = ballType;
            ballsInCurrentOver++;
            bowlerBalls++;

            // Check if over complete
            if (ballsInCurrentOver == 6) {
                oversCompleted++;
                ballsInCurrentOver = 0;
                bowlerOvers++;
                bowlerBalls = 0;

                // Complete the over in the match model
                if (currentMatch != null) {
                    cricketMatchService.completeOver();
                }
            }
        }

        // Update UI components
        updateCricketScoreUI();
    }

    private void updateCricketScoreUI() {
        TextView textScore = findViewById(R.id.text_score);
        if (textScore != null) {
            textScore.setText(String.format("%d/%d", currentScore, wickets));
        }

        // Update overs display
        TextView textOvers = findViewById(R.id.text_overs);
        if (textOvers != null) {
            textOvers.setText(String.format("(%d.%d ov)", oversCompleted, ballsInCurrentOver));
        }

        // Update batsmen
        TextView textBatsman1Score = findViewById(R.id.text_batsman1_score);
        TextView textBatsman1SR = findViewById(R.id.text_batsman1_sr);

        if (textBatsman1Score != null) {
            textBatsman1Score.setText(String.format(getString(R.string.batsman_score_format), batsman1Score, batsman1Balls));
        }

        if (textBatsman1SR != null && batsman1Balls > 0) {
            float sr = (batsman1Score * 100.0f) / batsman1Balls;
            textBatsman1SR.setText(String.format(getString(R.string.batsman_sr_format), sr));
        }

        // Update bowler figures
        TextView textBowlerFigures = findViewById(R.id.text_bowler_figures);
        if (textBowlerFigures != null) {
            textBowlerFigures.setText(String.format(getString(R.string.bowler_figures_format),
                    bowlerWickets, bowlerRuns, bowlerOvers, bowlerBalls));
        }

        // Update current over display
        updateCurrentOverDisplay();
    }

    private void addWicket() {
        wickets++;

        // Update UI
        TextView textScore = findViewById(R.id.text_score);
        if (textScore != null) {
            textScore.setText(String.format("%d/%d", currentScore, wickets));
        }

        // Add wicket to current over
        if (ballsInCurrentOver < 6) {
            currentOverBalls[ballsInCurrentOver] = 5; // wicket
            ballsInCurrentOver++;
            bowlerBalls++;
            batsman1Balls++;
            bowlerWickets++;

            if (ballsInCurrentOver == 6) {
                oversCompleted++;
                ballsInCurrentOver = 0;
                bowlerOvers++;
                bowlerBalls = 0;
            }

            // Update UI
            updateCricketScoreUI();

            // Update the match model
            if (currentMatch != null) {
                cricketMatchService.addWicket();
                updateCricketUIFromModel();
            }
        }
    }

    private void resetCricketScorePanel() {
        // Reset match data
        currentScore = 0;
        wickets = 0;
        oversCompleted = 0;
        ballsInCurrentOver = 0;

        // Reset batsmen
        batsman1Score = 0;
        batsman2Score = 0;
        batsman1Balls = 0;
        batsman2Balls = 0;

        // Reset bowler
        bowlerWickets = 0;
        bowlerRuns = 0;
        bowlerOvers = 0;
        bowlerBalls = 0;

        // Reset current over
        for (int i = 0; i < 6; i++) {
            currentOverBalls[i] = 0;
        }

        // Update UI
        updateCricketScoreUI();
    }

    /**
     * Shows the control panel as a bottom sheet dialog
     */
    private void showControlPanel() {
        // Create a bottom sheet dialog
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.control_panel_dialog, null);
        dialog.setContentView(dialogView);

        // Get buttons from dialog layout
        Button dialogStartRecordingButton = dialogView.findViewById(R.id.dialog_button_start_recording);
        Button dialogStopRecordingButton = dialogView.findViewById(R.id.dialog_button_stop_recording);
        Button dialogAddScoreButton = dialogView.findViewById(R.id.dialog_button_add_score);
        Button dialogAddWicketButton = dialogView.findViewById(R.id.dialog_button_add_wicket);
        Button dialogToggleScanningButton = dialogView.findViewById(R.id.dialog_button_toggle_scanning);
        Button dialogManagePlayersButton = dialogView.findViewById(R.id.dialog_button_manage_players);
        EditText dialogPlayerNameInput = dialogView.findViewById(R.id.dialog_player_name_input);

        // Set up button listeners
        dialogStartRecordingButton.setOnClickListener(v -> {
            startRecording();
            dialog.dismiss();
        });

        dialogStopRecordingButton.setOnClickListener(v -> {
            stopRecording();
            dialog.dismiss();
        });

        dialogAddScoreButton.setOnClickListener(v -> {
            String playerName = dialogPlayerNameInput.getText().toString().trim();
            if (!playerName.isEmpty()) {
                addScoreToPlayer(playerName, 1);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter player name", Toast.LENGTH_SHORT).show();
            }
        });

        dialogAddWicketButton.setOnClickListener(v -> {
            addWicket();
            dialog.dismiss();
        });

        dialogToggleScanningButton.setOnClickListener(v -> {
            toggleScanning();
            dialog.dismiss();
        });

        dialogManagePlayersButton.setOnClickListener(v -> {
            showPlayersManagementPanel();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Shows the players management panel
     */
    private void showPlayersManagementPanel() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.players_management_dialog, null);
        dialog.setContentView(dialogView);

        // Initialize RecyclerView for showing players
        RecyclerView playersRecyclerView = dialogView.findViewById(R.id.players_recycler_view);
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get players from current match
        List<Player> allPlayers = new ArrayList<>();
        if (currentMatch != null) {
            allPlayers.addAll(currentMatch.getTeamBatting().getPlayers());
            allPlayers.addAll(currentMatch.getTeamBowling().getPlayers());

            // Create and set adapter for players
            PlayersAdapter playersAdapter = new PlayersAdapter(allPlayers);
            playersRecyclerView.setAdapter(playersAdapter);
        } else {
            TextView emptyText = dialogView.findViewById(R.id.empty_players_text);
            emptyText.setVisibility(View.VISIBLE);
            playersRecyclerView.setVisibility(View.GONE);
        }

        dialog.show();
    }

    /**
     * Simple adapter for players list
     */
    private class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder> {
        private List<Player> players;

        public PlayersAdapter(List<Player> players) {
            this.players = players;
        }

        @NonNull
        @Override
        public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new PlayerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
            Player player = players.get(position);
            holder.name.setText(player.getName());

            // Show stats based on player role
            String stats;
            if (player.isBowler()) {
                stats = String.format("Wickets: %d, Economy: %.2f",
                        player.getBowlingStats().getWickets(),
                        player.calculateEconomyRate());
            } else {
                stats = String.format("Runs: %d, SR: %.2f",
                        player.getBattingStats().getRuns(),
                        player.calculateStrikeRate());
            }
            holder.stats.setText(stats);
        }

        @Override
        public int getItemCount() {
            return players.size();
        }

        class PlayerViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView stats;

            public PlayerViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(android.R.id.text1);
                stats = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.add(Menu.NONE, 1001, Menu.NONE, "Register Player Faces");
        menu.add(Menu.NONE, 1002, Menu.NONE, "Register Players (Alt)");
        menu.add(Menu.NONE, 1003, Menu.NONE, "Coach Interface");
        menu.add(Menu.NONE, 1004, Menu.NONE, "Modern Dashboard");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_reset) {
            resetMatchData();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, "Settings Selected", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == 1001) {
            // Navigate to player registration
            Intent intent = new Intent(this, RegisterPlayerFaceActivity.class);
            startActivity(intent);
            return true;
        } else if (id == 1002) {
            // Navigate to player registration (alternative UI)
            Intent intent = new Intent();
            intent.setClassName(this, "com.finedine.spucricketclub.ui.PlayerRegistrationActivity");
            startActivity(intent);
            return true;
        } else if (id == 1003) {
            // Navigate to coach interface
            Intent intent = new Intent(this, CoachInterfaceActivity.class);
            startActivity(intent);
            return true;
        } else if (id == 1004) {
            // Navigate to modern dashboard
            try {
                Intent intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to dashboard: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening dashboard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Check if ALL permissions were granted
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Permissions granted, check and initialize
                Log.d(TAG, "All permissions granted in onRequestPermissionsResult");
                checkPermissionsAndInitialize();
            } else {
                // Check if we should show the rationale or send to settings
                boolean shouldShowRationale = false;
                for (String permission : REQUIRED_PERMISSIONS) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (shouldShowRationale) {
                    // User denied but not permanently, show rationale
                    Log.d(TAG, "Should show permission rationale");
                    new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                            .setMessage("This app needs camera and recording permissions to function properly.")
                            .setPositiveButton("Grant Permissions", (dialog, which) -> {
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        REQUIRED_PERMISSIONS,
                                        REQUEST_CODE_PERMISSIONS
                                );
                            })
                            .setNegativeButton("Exit", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                } else {
                    // User permanently denied, send to settings
                    Log.d(TAG, "Sending user to settings for permissions");
                    Toast.makeText(this, R.string.permissions_not_granted, Toast.LENGTH_SHORT).show();

                    new AlertDialog.Builder(this)
                            .setTitle("Permissions Required")
                        .setMessage("This app needs camera and storage permissions to function properly. Please grant the permissions in settings.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            // Open app settings
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Exit", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check permissions again when returning from settings
        if (!isInitialized && allPermissionsGranted()) {
            initializeCameraAndHelpers();
            isInitialized = true;
        }
    }

    private void checkPermissionsAndInitialize() {
        if (allPermissionsGranted()) {
            // Permissions are already granted, proceed with initialization
            initializeCameraAndHelpers();
            isInitialized = true;
        } else {
            // Request needed permissions
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * Downloads the face recognition model if not already available
     */
    private void downloadFaceRecognitionModelIfNeeded() {
        File modelFile = new File(getFilesDir(), "facenet_mobile.tflite");

        // Only download if file doesn't exist
        if (!modelFile.exists()) {
            new Thread(() -> {
                try {
                    // Show download message
                    runOnUiThread(() -> {
                        Snackbar.make(findViewById(R.id.coordinator_layout),
                                "Downloading face recognition model...",
                                Snackbar.LENGTH_LONG).show();
                    });

                    // URL to model (would be on your server)
                    URL url = new URL("https://storage.googleapis.com/cricket-ai-models/facenet_mobile.tflite");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        throw new IOException("HTTP error: " + connection.getResponseCode());
                    }

                    // Download the file
                    InputStream input = connection.getInputStream();
                    OutputStream output = new FileOutputStream(modelFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }

                    output.close();
                    input.close();

                    // Show success message
                    runOnUiThread(() -> {
                        Snackbar.make(findViewById(R.id.coordinator_layout),
                                "Face recognition model downloaded successfully",
                                Snackbar.LENGTH_SHORT).show();
                    });

                    // Initialize the recognition engine now that we have the model
                    PlayerRecognitionEngine.getInstance(MainActivity.this);

                } catch (Exception e) {
                    e.printStackTrace();
                    // Show error message
                    runOnUiThread(() -> {
                        Snackbar.make(findViewById(R.id.coordinator_layout),
                                "Failed to download model: " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    });
                }
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceRecognitionHelper != null) {
            faceRecognitionHelper.close();
        }
        if (shirtNumberRecognitionHelper != null) {
            shirtNumberRecognitionHelper.close();
        }
        if (isRecording) {
            stopRecording();
        }
    }
}