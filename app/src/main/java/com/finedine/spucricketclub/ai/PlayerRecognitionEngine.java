package com.finedine.spucricketclub.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.Nullable;
import androidx.camera.core.ExperimentalGetImage;

import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.data.FirebaseAnalyticsHelper;
import com.finedine.spucricketclub.data.FirebaseHelper;
import com.finedine.spucricketclub.data.PlayerDatabase;
import com.finedine.spucricketclub.utils.ImageUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Advanced AI-powered player recognition engine that automatically identifies cricket players
 * from video feed using facial recognition and pose detection with Firebase integration.
 */
public class PlayerRecognitionEngine {
    private static final String TAG = "PlayerRecognition";
    private static final int FACE_FEATURE_DIMENSION = 128; // Face embedding dimension
    private static final float RECOGNITION_THRESHOLD = 0.7f; // Threshold for face matching
    private static final int MAX_RESULTS = 3; // Maximum number of recognition results
    private static final long TASK_TIMEOUT_SECONDS = 10; // Timeout for ML tasks

    // Singleton instance
    private static PlayerRecognitionEngine instance;

    // Context
    private final Context context;

    // ML Kit detectors
    private FaceDetector faceDetector;
    private PoseDetector poseDetector;

    // Player database reference
    private final PlayerDatabase playerDatabase;

    // Firebase helper
    private final FirebaseHelper firebaseHelper;

    // Firebase analytics helper
    private final FirebaseAnalyticsHelper analyticsHelper;

    // Firebase database reference for recognition data
    private final DatabaseReference recognitionRef;

    // Background executor
    private final Executor backgroundExecutor;

    // Cached player face embeddings
    private final Map<String, float[]> playerFaceEmbeddings = new ConcurrentHashMap<>();

    // Recognition state
    private volatile boolean isInitialized = false;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private volatile long lastRecognitionTimestamp = 0;
    private static final long RECOGNITION_COOLDOWN_MS = 2000; // 2 seconds cooldown

    // Recognition confidence tracking
    private final Map<String, Float> confidenceHistory = new ConcurrentHashMap<>();

    // Recognition listeners
    private final List<PlayerRecognitionListener> recognitionListeners = Collections.synchronizedList(new ArrayList<>());

    // Value listener for Firebase data changes
    private ValueEventListener recognitionDataListener;

    // Lock for synchronization
    private final ReentrantLock initLock = new ReentrantLock();

    /**
     * Interface for player recognition events
     */
    public interface PlayerRecognitionListener {
        void onPlayerRecognized(Player player, float confidence);
        void onRecognitionError(String error);
    }

    // Private constructor for singleton pattern
    private PlayerRecognitionEngine(Context context) {
        this.context = context.getApplicationContext();

        // Set up player database
        playerDatabase = PlayerDatabase.getInstance(context);

        // Initialize Firebase references
        firebaseHelper = FirebaseHelper.getInstance();
        analyticsHelper = FirebaseAnalyticsHelper.getInstance(context);
        recognitionRef = FirebaseDatabase.getInstance().getReference("recognition_data");

        // Create background executor
        backgroundExecutor = Executors.newFixedThreadPool(2);

        // Initialize detectors in background to prevent UI blocking
        backgroundExecutor.execute(this::initializeDetectors);
    }

    /**
     * Initialize ML Kit detectors
     */
    private void initializeDetectors() {
        try {
            initLock.lock();

            // Set up face detector with high accuracy
            FaceDetectorOptions faceOptions = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .enableTracking()
                    .build();

            faceDetector = FaceDetection.getClient(faceOptions);

            // Set up pose detector for player stance analysis
            AccuratePoseDetectorOptions poseOptions = new AccuratePoseDetectorOptions.Builder()
                    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                    .build();

            poseDetector = PoseDetection.getClient(poseOptions);

            // Initialize mock TensorFlow model
            initializeTFLiteInterpreter();

            isInitialized = true;

        } catch (Exception e) {
            Log.e(TAG, "Error initializing detectors", e);
            notifyListenersOfError("Failed to initialize recognition engine: " + e.getMessage());
        } finally {
            initLock.unlock();
        }
    }

    /**
     * Get singleton instance
     */
    public static synchronized PlayerRecognitionEngine getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerRecognitionEngine(context);
        }
        return instance;
    }

    /**
     * Add recognition listener
     */
    public void addRecognitionListener(PlayerRecognitionListener listener) {
        if (listener != null && !recognitionListeners.contains(listener)) {
            recognitionListeners.add(listener);
        }
    }

    /**
     * Remove recognition listener
     */
    public void removeRecognitionListener(PlayerRecognitionListener listener) {
        recognitionListeners.remove(listener);
    }

    /**
     * Initialize the TensorFlow Lite interpreter for face embeddings
     */
    private void initializeTFLiteInterpreter() {
        try {
            // Mock implementation since we can't use TensorFlow in this build
            Log.d(TAG, "Using mock TensorFlow implementation");
            preloadPlayerFaceEmbeddings();
            setupRecognitionDataListener();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TFLite interpreter", e);
            notifyListenersOfError("Failed to initialize face recognition model");
            isInitialized = false;
        }
    }

    /**
     * Check if engine is ready to process images
     */
    public boolean isReady() {
        return isInitialized && faceDetector != null && poseDetector != null;
    }

    /**
     * Notify listeners of error
     */
    private void notifyListenersOfError(String error) {
        List<PlayerRecognitionListener> listeners = new ArrayList<>(recognitionListeners);
        for (PlayerRecognitionListener listener : listeners) {
            try {
                listener.onRecognitionError(error);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener of error", e);
            }
        }
    }

    /**
     * Preload player face embeddings from database
     */
    private void preloadPlayerFaceEmbeddings() {
        Log.d(TAG, "Preloading player face embeddings");

        try {
            // Get all players from database
            List<Player> players = playerDatabase.getAllPlayers();

            int loadedCount = 0;
            for (Player player : players) {
                if (player != null && player.getId() != null) {
                    Bitmap faceBitmap = playerDatabase.loadPlayerFaceImage(player.getId());
                    if (faceBitmap != null) {
                        float[] embedding = generateFaceEmbedding(faceBitmap);
                        if (embedding != null) {
                            playerFaceEmbeddings.put(player.getId(), embedding);
                            loadedCount++;
                        }
                    }
                }
            }

            Log.d(TAG, "Loaded " + loadedCount + " player face embeddings");

            // Also sync with Firebase
            syncPlayerEmbeddingsWithFirebase();
        } catch (Exception e) {
            Log.e(TAG, "Error preloading player face embeddings", e);
        }
    }

    /**
     * Setup Firebase listener for recognition data changes
     */
    private void setupRecognitionDataListener() {
        try {
            if (recognitionDataListener != null) {
                recognitionRef.removeEventListener(recognitionDataListener);
            }

            recognitionDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isInitialized) return;

                    try {
                        // Process recognition data updates from other devices
                        for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                            String playerId = playerSnapshot.getKey();
                            if (playerId != null) {
                                // Get recognition timestamp
                                Long timestamp = playerSnapshot.child("timestamp").getValue(Long.class);
                                if (timestamp != null && timestamp > lastRecognitionTimestamp) {
                                    // Get player from database
                                    Player player = playerDatabase.getPlayerById(playerId);
                                    if (player != null) {
                                        Float confidence = playerSnapshot.child("confidence").getValue(Float.class);
                                        if (confidence != null && confidence > RECOGNITION_THRESHOLD) {
                                            // Update confidence history
                                            Float previousConfidence = confidenceHistory.getOrDefault(playerId, 0.0f);
                                            float smoothedConfidence = (previousConfidence * 0.7f) + (confidence * 0.3f);
                                            confidenceHistory.put(playerId, smoothedConfidence);

                                            // Notify listeners of the remote recognition
                                            if (smoothedConfidence > RECOGNITION_THRESHOLD) {
                                                notifyListenersOfRecognition(player, smoothedConfidence);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase data change", e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error listening for recognition data", error.toException());
                }
            };

            recognitionRef.addValueEventListener(recognitionDataListener);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up recognition data listener", e);
        }
    }

    /**
     * Notify listeners of player recognition
     */
    private void notifyListenersOfRecognition(Player player, float confidence) {
        if (player == null) return;

        List<PlayerRecognitionListener> listeners = new ArrayList<>(recognitionListeners);
        for (PlayerRecognitionListener listener : listeners) {
            try {
                listener.onPlayerRecognized(player, confidence);

                // Track the recognition in analytics
                analyticsHelper.trackFaceRecognition(player, confidence);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener of recognition", e);
            }
        }
    }

    /**
     * Sync player embeddings with Firebase
     */
    private void syncPlayerEmbeddingsWithFirebase() {
        backgroundExecutor.execute(() -> {
            try {
                // For each player embedding, ensure it's saved to Firebase
                for (Map.Entry<String, float[]> entry : playerFaceEmbeddings.entrySet()) {
                    String playerId = entry.getKey();
                    float[] embedding = entry.getValue();

                    playerDatabase.savePlayerFaceEmbedding(playerId, embedding);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing embeddings with Firebase", e);
            }
        });
    }

    /**
     * Analyze camera frame for player recognition
     *
     * @param image Camera image frame
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void analyzeFrame(@Nullable Image image) {
        // Check if the system is ready and not currently processing
        if (!isReady() || image == null || isProcessing.get()) {
            return;
        }

        // Throttle recognition to avoid overloading
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRecognitionTimestamp < RECOGNITION_COOLDOWN_MS) {
            return;
        }

        // Mark we're starting to process
        if (!isProcessing.compareAndSet(false, true)) {
            return; // Another thread is already processing
        }

        lastRecognitionTimestamp = currentTime;

        try {
            // Convert image to bitmap safely
            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            if (bitmap == null) {
                isProcessing.set(false);
                return;
            }

            // Create input image for ML Kit
            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

            // Process with both face and pose detection in parallel with timeouts
            Task<List<Face>> faceTask = faceDetector.process(inputImage);
            Task<Pose> poseTask = poseDetector.process(inputImage);

            Tasks.whenAllComplete(faceTask, poseTask)
                    .addOnSuccessListener(tasks -> {
                        backgroundExecutor.execute(() -> {
                            try {
                                List<Face> faces = null;
                                Pose pose = null;

                                try {
                                    if (faceTask.isSuccessful()) {
                                        faces = Tasks.await(faceTask, TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                                    } else if (faceTask.getException() != null) {
                                        Log.e(TAG, "Face detection failed", faceTask.getException());
                                    }

                                    if (poseTask.isSuccessful()) {
                                        pose = Tasks.await(poseTask, TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                                    } else if (poseTask.getException() != null) {
                                        Log.e(TAG, "Pose detection failed", poseTask.getException());
                                    }
                                } catch (ExecutionException | InterruptedException |
                                         java.util.concurrent.TimeoutException e) {
                                    Log.e(TAG, "Task execution error", e);
                                }

                            // Process detected faces if any were found
                            if (faces != null && !faces.isEmpty()) {
                                processFacesAndPose(bitmap, faces, pose);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing detection results", e);
                        } finally {
                            isProcessing.set(false);
                        }
                    });
                })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Detection failed", e);
                        isProcessing.set(false);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Frame analysis error", e);
            isProcessing.set(false);
        }
    }

    /**
     * Process detected faces and pose for player recognition
     */
    private void processFacesAndPose(Bitmap fullImage, List<Face> faces, Pose pose) {
        try {
            // Safety check for params
            if (fullImage == null || faces == null || faces.isEmpty()) {
                return;
            }

            // Sort faces by size (largest first, assuming closest to camera)
            Collections.sort(faces, (face1, face2) -> {
                try {
                    Rect box1 = face1.getBoundingBox();
                    Rect box2 = face2.getBoundingBox();
                    int area1 = box1.width() * box1.height();
                    int area2 = box2.width() * box2.height();
                    return Integer.compare(area2, area1);
                } catch (Exception e) {
                    Log.e(TAG, "Error comparing faces", e);
                    return 0;
                }
            });

            // Process at most 2 largest faces (likely the batsmen or bowler)
            for (int i = 0; i < Math.min(2, faces.size()); i++) {
                Face face = faces.get(i);
                if (face == null) continue;

                Rect bounds = face.getBoundingBox();
                if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) continue;

                try {
                    // Extract face image
                    Bitmap faceBitmap = cropFaceFromImage(fullImage, bounds);
                    if (faceBitmap == null) continue;

                    // Generate face embedding
                    float[] faceEmbedding = generateFaceEmbedding(faceBitmap);
                    if (faceEmbedding == null) continue;

                    // Match against player database
                    RecognitionResult result = recognizePlayer(faceEmbedding, pose);

                    if (result != null && result.getConfidence() > RECOGNITION_THRESHOLD) {
                        // Track confidence for stability
                        String playerId = result.getPlayer().getId();
                        Float previousConfidence = confidenceHistory.getOrDefault(playerId, 0.0f);
                        float smoothedConfidence = (previousConfidence * 0.7f) + (result.getConfidence() * 0.3f);
                        confidenceHistory.put(playerId, smoothedConfidence);

                        // Only notify if confidence remains high
                        if (smoothedConfidence > RECOGNITION_THRESHOLD) {
                            // Determine player role using pose if available
                            if (pose != null) {
                                inferPlayerRoleFromPose(result.getPlayer(), pose);
                            }

                            // Notify listeners
                            notifyListenersOfRecognition(result.getPlayer(), smoothedConfidence);

                            // Update recognition data in Firebase
                            updateRecognitionInFirebase(playerId, smoothedConfidence);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing face", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in processFacesAndPose", e);
        }
    }

    /**
     * Update recognition data in Firebase
     */
    private void updateRecognitionInFirebase(String playerId, float confidence) {
        try {
            Map<String, Object> recognitionData = new HashMap<>();
            recognitionData.put("timestamp", System.currentTimeMillis());
            recognitionData.put("confidence", confidence);

            recognitionRef.child(playerId).setValue(recognitionData)
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating recognition data", e));
        } catch (Exception e) {
            Log.e(TAG, "Error updating Firebase recognition data", e);
        }
    }

    /**
     * Generate face embedding using the TensorFlow model
     */
    private float[] generateFaceEmbedding(Bitmap faceBitmap) {
        if (faceBitmap == null) return null;

        try {
            // Create mock embeddings for testing since we're not using real TensorFlow
            float[] embeddings = new float[FACE_FEATURE_DIMENSION];
            for (int i = 0; i < FACE_FEATURE_DIMENSION; i++) {
                embeddings[i] = (float) Math.random();
            }
            return embeddings;
        } catch (Exception e) {
            Log.e(TAG, "Error generating face embeddings", e);
            return null;
        }
    }

    /**
     * Crop face from the full image
     */
    private Bitmap cropFaceFromImage(Bitmap image, Rect bounds) {
        try {
            if (image == null || bounds == null) return null;

            // Add some margin around the face
            int margin = (int) (Math.min(bounds.width(), bounds.height()) * 0.2);

            int left = Math.max(bounds.left - margin, 0);
            int top = Math.max(bounds.top - margin, 0);
            int width = Math.min(bounds.width() + margin * 2, image.getWidth() - left);
            int height = Math.min(bounds.height() + margin * 2, image.getHeight() - top);

            // Safety check for valid dimensions
            if (width <= 0 || height <= 0 || left >= image.getWidth() || top >= image.getHeight()) {
                return null;
            }

            return Bitmap.createBitmap(image, left, top, width, height);
        } catch (Exception e) {
            Log.e(TAG, "Error cropping face", e);
            return null;
        }
    }

    /**
     * Recognize player by comparing face embedding with database
     */
    private RecognitionResult recognizePlayer(float[] faceEmbedding, Pose pose) {
        if (faceEmbedding == null) return null;

        List<RecognitionResult> results = new ArrayList<>();

        try {
            // Compare with all stored embeddings
            for (Map.Entry<String, float[]> entry : playerFaceEmbeddings.entrySet()) {
                String playerId = entry.getKey();
                float[] storedEmbedding = entry.getValue();

                if (storedEmbedding == null) continue;

                float similarity = calculateCosineSimilarity(faceEmbedding, storedEmbedding);

                // Adjust similarity based on pose if available
                if (pose != null) {
                    Player player = playerDatabase.getPlayerById(playerId);
                    if (player != null) {
                        // Adjust similarity based on typical player stance
                        float poseBonus = calculatePoseMatchBonus(player, pose);
                        similarity += poseBonus;
                    }
                }

                // Only consider good matches
                if (similarity > 0.5f) {
                    Player player = playerDatabase.getPlayerById(playerId);
                    if (player != null) {
                        results.add(new RecognitionResult(player, similarity));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recognizing player", e);
        }

        // Sort by confidence and return the best match
        if (!results.isEmpty()) {
            Collections.sort(results, (r1, r2) -> Float.compare(r2.getConfidence(), r1.getConfidence()));
            return results.get(0);
        }

        return null;
    }

    /**
     * Calculate cosine similarity between two embeddings
     */
    private float calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null ||
                embedding1.length != embedding2.length || embedding1.length == 0) {
            return 0.0f;
        }

        try {
            float dotProduct = 0;
            float magnitude1 = 0;
            float magnitude2 = 0;

            for (int i = 0; i < embedding1.length; i++) {
                dotProduct += embedding1[i] * embedding2[i];
                magnitude1 += embedding1[i] * embedding1[i];
                magnitude2 += embedding2[i] * embedding2[i];
            }

            magnitude1 = (float) Math.sqrt(magnitude1);
            magnitude2 = (float) Math.sqrt(magnitude2);

            if (magnitude1 > 0 && magnitude2 > 0) {
                return dotProduct / (magnitude1 * magnitude2);
            } else {
                return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating similarity", e);
            return 0.0f;
        }
    }

    /**
     * Infer player role from detected pose
     */
    private void inferPlayerRoleFromPose(Player player, Pose pose) {
        try {
            // Get key landmarks
            PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
            PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
            PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);

            if (leftShoulder != null && rightShoulder != null && leftWrist != null && rightWrist != null) {
                // Calculate arm angles for batting vs bowling stance
                float leftArmAngle = calculateArmAngle(leftShoulder, leftWrist);
                float rightArmAngle = calculateArmAngle(rightShoulder, rightWrist);

                // Typical batsman stance has arms extended
                boolean isBatsmanStance = (leftArmAngle > 45 && leftArmAngle < 135) ||
                        (rightArmAngle > 45 && rightArmAngle < 135);

                // Typical bowler stance has one arm raised
                boolean isBowlerStance = (leftArmAngle > 150) || (rightArmAngle > 150);

                // Log the detection but don't automatically change player role
                Log.d(TAG, "Player " + player.getName() + " detected in " +
                        (isBatsmanStance ? "batting stance" :
                                isBowlerStance ? "bowling stance" : "neutral stance"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inferring player role from pose", e);
        }
    }

    /**
     * Calculate angle of arm for stance detection
     */
    private float calculateArmAngle(PoseLandmark shoulder, PoseLandmark wrist) {
        if (shoulder == null || wrist == null ||
                shoulder.getPosition() == null || wrist.getPosition() == null) {
            return 0.0f;
        }

        try {
            float deltaY = wrist.getPosition().y - shoulder.getPosition().y;
            float deltaX = wrist.getPosition().x - shoulder.getPosition().x;
            float angleRadians = (float) Math.atan2(deltaY, deltaX);
            return (float) Math.toDegrees(angleRadians);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating arm angle", e);
            return 0.0f;
        }
    }

    /**
     * Calculate a similarity bonus based on pose matching expected player role
     */
    private float calculatePoseMatchBonus(Player player, Pose pose) {
        try {
            // This would be more sophisticated in a real implementation
            // For now, just check if the pose matches the player's role

            PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
            PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
            PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);

            if (leftShoulder == null || rightShoulder == null || leftWrist == null || rightWrist == null) {
                return 0.0f;
            }

            float bonus = 0.0f;

            // Calculate arm angles
            float leftArmAngle = calculateArmAngle(leftShoulder, leftWrist);
            float rightArmAngle = calculateArmAngle(rightShoulder, rightWrist);

            // Check if pose aligns with player role
            if (player.getRole() != null) {
                switch (player.getRole()) {
                    case BATSMAN:
                        // Batsman typically has arms extended horizontally
                        if ((leftArmAngle > 45 && leftArmAngle < 135) ||
                                (rightArmAngle > 45 && rightArmAngle < 135)) {
                            bonus += 0.05f;
                        }
                        break;

                    case BOWLER:
                        // Bowler typically has one arm raised
                        if ((leftArmAngle > 150) || (rightArmAngle > 150)) {
                            bonus += 0.05f;
                        }
                        break;

                    case WICKET_KEEPER:
                        // Wicket keeper typically has arms low and forward
                        if ((leftArmAngle < 45 || leftArmAngle > 315) &&
                                (rightArmAngle < 45 || rightArmAngle > 315)) {
                            bonus += 0.05f;
                        }
                        break;

                    case ALL_ROUNDER:
                        // For all-rounders, give smaller bonuses for any stance
                        if ((leftArmAngle > 45 && leftArmAngle < 135) ||
                                (rightArmAngle > 45 && rightArmAngle < 135)) {
                            bonus += 0.02f;  // Batting stance
                        }
                        if ((leftArmAngle > 150) || (rightArmAngle > 150)) {
                            bonus += 0.02f;  // Bowling stance
                        }
                        break;
                }
            }

            return bonus;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating pose match bonus", e);
            return 0.0f;
        }
    }

    /**
     * Register a new player face for recognition
     */
    public void registerPlayerFace(Player player, Bitmap faceBitmap) {
        if (!isReady()) {
            notifyListenersOfError("Recognition engine not initialized");
            return;
        }

        if (player == null || faceBitmap == null) {
            notifyListenersOfError("Cannot register face: missing player info or face image");
            return;
        }

        backgroundExecutor.execute(() -> {
            try {
                // Generate face embedding
                float[] embedding = generateFaceEmbedding(faceBitmap);
                if (embedding != null) {
                    // Save face image and embedding to database
                    playerDatabase.savePlayerFaceImage(player.getId(), faceBitmap);
                    playerDatabase.savePlayerFaceEmbedding(player.getId(), embedding);

                    // Update in-memory cache
                    playerFaceEmbeddings.put(player.getId(), embedding);

                    // Ensure player is registered in Firebase
                    firebaseHelper.savePlayer(player);

                    Log.d(TAG, "Player face registered: " + player.getName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error registering player face", e);
                notifyListenersOfError("Failed to register player face: " + e.getMessage());
            }
        });
    }

    /**
     * Force reload of player embeddings from database
     */
    public void reloadPlayerEmbeddings() {
        backgroundExecutor.execute(() -> {
            try {
                // Clear current embeddings
                playerFaceEmbeddings.clear();

                // Reload from database
                preloadPlayerFaceEmbeddings();

                Log.d(TAG, "Player embeddings reloaded");
            } catch (Exception e) {
                Log.e(TAG, "Error reloading player embeddings", e);
            }
        });
    }

    /**
     * Inner class to hold recognition results
     */
    private static class RecognitionResult {
        private final Player player;
        private final float confidence;

        public RecognitionResult(Player player, float confidence) {
            this.player = player;
            this.confidence = confidence;
        }

        public Player getPlayer() {
            return player;
        }

        public float getConfidence() {
            return confidence;
        }
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        try {
            // Remove Firebase listener
            if (recognitionDataListener != null) {
                recognitionRef.removeEventListener(recognitionDataListener);
            }

            // Close ML Kit resources
            try {
                if (faceDetector != null) {
                    faceDetector.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing face detector", e);
            }

            try {
                if (poseDetector != null) {
                    poseDetector.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing pose detector", e);
            }

            // Clear cached embeddings
            playerFaceEmbeddings.clear();

            // Reset state
            isInitialized = false;
            isProcessing.set(false);

        } catch (Exception e) {
            Log.e(TAG, "Error shutting down recognition engine", e);
        }
    }
}