package com.finedine.spucricketclub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import androidx.viewpager2.widget.ViewPager2;

import com.finedine.spucricketclub.ai.PlayerRecognitionEngine;
import com.finedine.spucricketclub.cricket.CricketMatchService;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Team;
import com.finedine.spucricketclub.data.PlayerDatabase;
import com.finedine.spucricketclub.utils.ImageUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coach interface for managing teams, players, and face recognition
 */
@androidx.camera.core.ExperimentalGetImage
public class CoachInterfaceActivity extends AppCompatActivity {
    private static final String TAG = "CoachInterface";
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    // UI components
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // Team management components
    private EditText homeTeamNameEditText;
    private EditText awayTeamNameEditText;
    private EditText playerNameEditText;
    private Spinner playerRoleSpinner;
    private Spinner teamSelectionSpinner;
    private ListView playersListView;
    private Button addPlayerButton;
    private Button saveTeamNamesButton;

    // Face registration components
    private PreviewView previewView;
    private Button captureButton;
    private Button saveButton;
    private Button cancelButton;
    private Spinner playerFaceSpinner;
    private TextView registerInstructions;
    private View previewContainer;
    private View capturedImageContainer;
    private View previewImageView;

    // Camera related
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Bitmap capturedFaceBitmap = null;

    // Data objects
    private CricketMatchService cricketMatchService;
    private PlayerDatabase playerDatabase;
    private PlayerRecognitionEngine recognitionEngine;
    private Match currentMatch;
    private Team homeTeam;
    private Team awayTeam;
    private Team currentTeam;
    private Player selectedPlayer = null;

    // Adapters
    private List<String> playerNames = new ArrayList<>();
    private ArrayAdapter<String> playersAdapter;

    // Current view state
    private int currentTabPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_interface);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Coach Interface");

        // Initialize UI components
        initializeUIComponents();

        // Get data services
        cricketMatchService = CricketMatchService.getInstance(this);
        playerDatabase = PlayerDatabase.getInstance(this);
        recognitionEngine = PlayerRecognitionEngine.getInstance(this);

        // Get or create match and teams
        if (cricketMatchService.getCurrentMatch() == null) {
            currentMatch = cricketMatchService.createMatch("SPU", "Opponent", 20);
        } else {
            currentMatch = cricketMatchService.getCurrentMatch();
        }

        // Get teams from match
        homeTeam = currentMatch.getTeamBatting();
        awayTeam = currentMatch.getTeamBowling();
        currentTeam = homeTeam; // Start with home team selected

        // Set initial team names
        homeTeamNameEditText.setText(homeTeam.getTeamName());
        awayTeamNameEditText.setText(awayTeam.getTeamName());

        // Set up camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Load players for current team
        loadPlayersForCurrentTeam();

        // Set up listeners
        setupListeners();

        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera();
        }
    }

    private void initializeUIComponents() {
        // Tabs setup
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        // Team management UI components
        homeTeamNameEditText = findViewById(R.id.edit_home_team_name);
        awayTeamNameEditText = findViewById(R.id.edit_away_team_name);
        playerNameEditText = findViewById(R.id.edit_player_name);
        playerRoleSpinner = findViewById(R.id.spinner_player_role);
        teamSelectionSpinner = findViewById(R.id.spinner_team_selection);
        playersListView = findViewById(R.id.list_players);
        addPlayerButton = findViewById(R.id.button_add_player);
        saveTeamNamesButton = findViewById(R.id.button_save_team_names);

        // Face registration UI components
        previewView = findViewById(R.id.register_preview_view);
        captureButton = findViewById(R.id.capture_button);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);
        playerFaceSpinner = findViewById(R.id.player_spinner);
        registerInstructions = findViewById(R.id.register_instructions);
        previewContainer = findViewById(R.id.preview_container);
        capturedImageContainer = findViewById(R.id.captured_image_container);
        previewImageView = findViewById(R.id.preview_image_view);

        // Setup ViewPager with new adapter
        if (viewPager != null && tabLayout != null) {
            viewPager.setUserInputEnabled(true);
            viewPager.setAdapter(new com.finedine.spucricketclub.adapters.CoachInterfaceAdapter(this));

            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("Team Management");
                        break;
                    case 1:
                        tab.setText("Face Registration");
                        break;
                }
            }).attach();

            // Listen for tab changes
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentTabPosition = tab.getPosition();
                    updateUIBasedOnTab();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // Not used
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // Not used
                }
            });
        }

        // Set up player role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"BATSMAN", "BOWLER", "ALL_ROUNDER", "WICKET_KEEPER"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerRoleSpinner.setAdapter(roleAdapter);

        // Set up team selection spinner
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[]{"Home Team", "Away Team"});
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamSelectionSpinner.setAdapter(teamAdapter);

        // Set up players list adapter
        playersAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, playerNames);
        playersListView.setAdapter(playersAdapter);

        // Initial UI state for face registration
        if (saveButton != null) saveButton.setEnabled(false);
        if (capturedImageContainer != null) capturedImageContainer.setVisibility(View.GONE);
        if (previewContainer != null) previewContainer.setVisibility(View.VISIBLE);
    }

    private void updateUIBasedOnTab() {
        if (currentTabPosition == 0) {
            // Team Management tab
            if (findViewById(R.id.team_management_layout) != null) {
                findViewById(R.id.team_management_layout).setVisibility(View.VISIBLE);
            }
            if (findViewById(R.id.face_registration_layout) != null) {
                findViewById(R.id.face_registration_layout).setVisibility(View.GONE);
            }
        } else {
            // Face Registration tab
            if (findViewById(R.id.team_management_layout) != null) {
                findViewById(R.id.team_management_layout).setVisibility(View.GONE);
            }
            if (findViewById(R.id.face_registration_layout) != null) {
                findViewById(R.id.face_registration_layout).setVisibility(View.VISIBLE);
            }

            // Update player spinner with current players
            setupPlayerFaceSpinner();

            // If camera permissions are granted, start camera
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        }
    }

    private void setupListeners() {
        // Team selection spinner listener
        teamSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Switch between home and away teams
                if (position == 0) {
                    currentTeam = homeTeam;
                } else {
                    currentTeam = awayTeam;
                }
                loadPlayersForCurrentTeam();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });

        // Add player button listener
        addPlayerButton.setOnClickListener(v -> addNewPlayer());

        // Save team names button listener
        saveTeamNamesButton.setOnClickListener(v -> saveTeamNames());

        // Players list item click listener for editing/deleting
        playersListView.setOnItemClickListener((parent, view, position, id) -> {
            String playerName = playerNames.get(position);
            showPlayerOptionsDialog(playerName);
        });

        // Face registration buttons
        if (captureButton != null) {
            captureButton.setOnClickListener(v -> captureImage());
        }

        if (saveButton != null) {
            saveButton.setOnClickListener(v -> savePlayerFace());
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> resetCapture());
        }
    }

    private void setupPlayerFaceSpinner() {
        if (playerFaceSpinner == null) return;

        // Get all players from both teams
        List<Player> allPlayers = new ArrayList<>();
        if (homeTeam != null) allPlayers.addAll(homeTeam.getPlayers());
        if (awayTeam != null) allPlayers.addAll(awayTeam.getPlayers());

        // Player spinner setup
        List<String> playerNamesList = new ArrayList<>();
        for (Player player : allPlayers) {
            playerNamesList.add(player.getName());
        }

        // If no players, show message
        if (playerNamesList.isEmpty()) {
            playerNamesList.add("No players available - add players first");
        }

        ArrayAdapter<String> playerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, playerNamesList);
        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerFaceSpinner.setAdapter(playerAdapter);

        // Handle player selection
        playerFaceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedName = (String) parent.getItemAtPosition(position);

                // Find player in home team first
                selectedPlayer = homeTeam.findPlayerByName(selectedName);

                // If not found, check away team
                if (selectedPlayer == null) {
                    selectedPlayer = awayTeam.findPlayerByName(selectedName);
                }

                if (selectedPlayer == null && allPlayers.isEmpty()) {
                    // No players available
                    Toast.makeText(CoachInterfaceActivity.this,
                            "Please add players first in Team Management tab",
                            Toast.LENGTH_SHORT).show();
                    captureButton.setEnabled(false);
                } else {
                    captureButton.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPlayer = null;
            }
        });
    }

    private void loadPlayersForCurrentTeam() {
        if (currentTeam == null) return;

        // Clear and reload player names
        playerNames.clear();
        for (Player player : currentTeam.getPlayers()) {
            playerNames.add(player.getName());
        }
        if (playersAdapter != null) {
            playersAdapter.notifyDataSetChanged();
        }
    }

    private void addNewPlayer() {
        String name = playerNameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter player name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if player name already exists
        for (Player player : currentTeam.getPlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                Toast.makeText(this, "Player already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Get selected role
        Player.PlayerRole role;
        switch (playerRoleSpinner.getSelectedItemPosition()) {
            case 0:
                role = Player.PlayerRole.BATSMAN;
                break;
            case 1:
                role = Player.PlayerRole.BOWLER;
                break;
            case 2:
                role = Player.PlayerRole.ALL_ROUNDER;
                break;
            case 3:
                role = Player.PlayerRole.WICKET_KEEPER;
                break;
            default:
                role = Player.PlayerRole.BATSMAN;
        }

        // Create and add new player
        Player newPlayer = new Player(name, role);
        currentTeam.addPlayer(newPlayer);

        // Save player to database
        playerDatabase.updatePlayer(newPlayer);

        // Update the UI
        playerNames.add(name);
        playersAdapter.notifyDataSetChanged();

        // Clear the input field
        playerNameEditText.setText("");

        Toast.makeText(this, "Added player: " + name, Toast.LENGTH_SHORT).show();
    }

    private void saveTeamNames() {
        String homeTeamName = homeTeamNameEditText.getText().toString().trim();
        String awayTeamName = awayTeamNameEditText.getText().toString().trim();

        if (homeTeamName.isEmpty() || awayTeamName.isEmpty()) {
            Toast.makeText(this, "Team names cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update team names
        homeTeam.setTeamName(homeTeamName);
        awayTeam.setTeamName(awayTeamName);

        Toast.makeText(this, "Team names saved", Toast.LENGTH_SHORT).show();
    }

    private void showPlayerOptionsDialog(String playerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(playerName);
        builder.setItems(new String[]{"Edit", "Delete", "Register Face"}, (dialog, which) -> {
            if (which == 0) {
                showEditPlayerDialog(playerName);
            } else if (which == 1) {
                deletePlayer(playerName);
            } else if (which == 2) {
                // Switch to face registration tab and select this player
                viewPager.setCurrentItem(1);
                selectPlayerInFaceSpinner(playerName);
            }
        });
        builder.create().show();
    }

    private void selectPlayerInFaceSpinner(String playerName) {
        if (playerFaceSpinner != null) {
            ArrayAdapter adapter = (ArrayAdapter) playerFaceSpinner.getAdapter();
            if (adapter != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equals(playerName)) {
                        playerFaceSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    private void showEditPlayerDialog(String playerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Player");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_player, null);
        builder.setView(dialogView);

        EditText nameEditText = dialogView.findViewById(R.id.edit_dialog_player_name);
        Spinner roleSpinner = dialogView.findViewById(R.id.spinner_dialog_player_role);

        // Set up role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"BATSMAN", "BOWLER", "ALL_ROUNDER", "WICKET_KEEPER"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        // Find the player
        Player player = currentTeam.findPlayerByName(playerName);
        if (player != null) {
            nameEditText.setText(player.getName());
            roleSpinner.setSelection(player.getRole().ordinal());
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameEditText.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Player name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected role
            Player.PlayerRole role;
            switch (roleSpinner.getSelectedItemPosition()) {
                case 0:
                    role = Player.PlayerRole.BATSMAN;
                    break;
                case 1:
                    role = Player.PlayerRole.BOWLER;
                    break;
                case 2:
                    role = Player.PlayerRole.ALL_ROUNDER;
                    break;
                case 3:
                    role = Player.PlayerRole.WICKET_KEEPER;
                    break;
                default:
                    role = Player.PlayerRole.BATSMAN;
            }

            // Update player
            if (player != null) {
                player.setName(newName);
                player.setRole(role);

                // Update player in database
                playerDatabase.updatePlayer(player);

                // Update the list
                int index = playerNames.indexOf(playerName);
                if (index >= 0) {
                    playerNames.set(index, newName);
                    playersAdapter.notifyDataSetChanged();
                }

                Toast.makeText(this, "Player updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void deletePlayer(String playerName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Player")
                .setMessage("Are you sure you want to delete " + playerName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Find and remove player
                    Player player = currentTeam.findPlayerByName(playerName);
                    if (player != null) {
                        // Create a new list without the player to remove
                        List<Player> updatedPlayers = new ArrayList<>();
                        for (Player p : currentTeam.getPlayers()) {
                            if (!p.getId().equals(player.getId())) {
                                updatedPlayers.add(p);
                            }
                        }
                        currentTeam.setPlayers(updatedPlayers);

                        // Update the UI
                        playerNames.remove(playerName);
                        playersAdapter.notifyDataSetChanged();

                        Toast.makeText(this, "Player deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create().show();
    }

    // FACE REGISTRATION METHODS

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        if (previewView == null) return;

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
        if (previewView == null) return;

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
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(CoachInterfaceActivity.this,
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
        if (capturedFaceBitmap == null || capturedImageContainer == null || previewContainer == null)
            return;

        // Show the captured image
        previewContainer.setVisibility(View.GONE);
        capturedImageContainer.setVisibility(View.VISIBLE);

        // Enable save button
        if (saveButton != null) saveButton.setEnabled(true);

        // Set preview image
        if (previewImageView != null) {
            previewImageView.setBackground(null);
            previewImageView.setBackgroundDrawable(
                    new android.graphics.drawable.BitmapDrawable(getResources(), capturedFaceBitmap));
        }
    }

    private void savePlayerFace() {
        if (capturedFaceBitmap == null || selectedPlayer == null) {
            Toast.makeText(this, "No face captured or player selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register the face with the recognition engine
        recognitionEngine.registerPlayerFace(selectedPlayer, capturedFaceBitmap);

        // Show success message
        Snackbar.make(findViewById(R.id.coach_interface_coordinator_layout),
                "Face registered for " + selectedPlayer.getName(),
                Snackbar.LENGTH_SHORT).show();

        // Reset for next capture
        resetCapture();
    }

    private void resetCapture() {
        capturedFaceBitmap = null;
        if (previewContainer != null) previewContainer.setVisibility(View.VISIBLE);
        if (capturedImageContainer != null) capturedImageContainer.setVisibility(View.GONE);
        if (saveButton != null) saveButton.setEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for face registration",
                        Toast.LENGTH_SHORT).show();
                // Switch back to first tab
                viewPager.setCurrentItem(0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}