package com.finedine.spucricketclub.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Player.PlayerRole;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database for storing player information and facial recognition data
 */
public class PlayerDatabase {
    private static final String TAG = "PlayerDatabase";

    // Firebase paths
    private static final String FB_PLAYERS_PATH = "players";
    private static final String FB_FACE_EMBEDDINGS_PATH = "face_embeddings";
    private static final String STORAGE_FACE_IMAGES_PATH = "player_faces";

    // Singleton instance
    private static PlayerDatabase instance;

    // Context
    private final Context context;

    // Firebase references
    private final FirebaseDatabase database;
    private final DatabaseReference playersRef;
    private final DatabaseReference faceEmbeddingsRef;
    private final FirebaseStorage storage;
    private final StorageReference faceImagesRef;

    // In-memory caches
    private List<Player> players = new ArrayList<>();
    private Map<String, float[]> faceEmbeddings = new HashMap<>();
    private boolean isInitialized = false;

    // Listener for player data changes
    private ValueEventListener playersListener;
    private ValueEventListener embeddingsListener;

    // Private constructor for singleton pattern
    private PlayerDatabase(Context context) {
        this.context = context.getApplicationContext();

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        playersRef = database.getReference(FB_PLAYERS_PATH);
        faceEmbeddingsRef = database.getReference(FB_FACE_EMBEDDINGS_PATH);
        storage = FirebaseStorage.getInstance();
        faceImagesRef = storage.getReference(STORAGE_FACE_IMAGES_PATH);

        // Load data from Firebase
        loadPlayers();
        loadFaceEmbeddings();
    }

    /**
     * Get singleton instance
     */
    public static synchronized PlayerDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerDatabase(context);
        }
        return instance;
    }

    /**
     * Load players from Firebase
     */
    private void loadPlayers() {
        playersListener = playersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                players.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Player player = snapshot.getValue(Player.class);
                    if (player != null) {
                        players.add(player);
                    }
                }
                isInitialized = true;
                Log.d(TAG, "Loaded " + players.size() + " players from Firebase");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading players from Firebase", error.toException());
            }
        });
    }

    /**
     * Save player to Firebase
     */
    private void savePlayerToFirebase(Player player) {
        playersRef.child(player.getId()).setValue(player)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Player saved to Firebase: " + player.getName());
                        } else {
                            Log.e(TAG, "Error saving player to Firebase", task.getException());
                        }
                    }
                });
    }

    /**
     * Load face embeddings from Firebase
     */
    private void loadFaceEmbeddings() {
        embeddingsListener = faceEmbeddingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                faceEmbeddings.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String playerId = snapshot.getKey();
                    if (playerId != null) {
                        String base64Embedding = snapshot.getValue(String.class);
                        if (base64Embedding != null) {
                            float[] embedding = decodeEmbedding(base64Embedding);
                            if (embedding != null) {
                                faceEmbeddings.put(playerId, embedding);
                            }
                        }
                    }
                }
                Log.d(TAG, "Loaded " + faceEmbeddings.size() + " face embeddings from Firebase");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading face embeddings from Firebase", error.toException());
            }
        });
    }

    /**
     * Encode float array to Base64 string
     */
    private String encodeEmbedding(float[] embedding) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
            for (float value : embedding) {
                buffer.putFloat(value);
            }
            return android.util.Base64.encodeToString(buffer.array(), android.util.Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding embedding", e);
            return null;
        }
    }

    /**
     * Decode Base64 string to float array
     */
    private float[] decodeEmbedding(String base64Embedding) {
        try {
            byte[] bytes = android.util.Base64.decode(base64Embedding, android.util.Base64.DEFAULT);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            FloatBuffer floatBuffer = buffer.asFloatBuffer();

            float[] embedding = new float[floatBuffer.capacity()];
            floatBuffer.get(embedding);

            return embedding;
        } catch (Exception e) {
            Log.e(TAG, "Error decoding embedding", e);
            return null;
        }
    }

    /**
     * Get all players
     */
    public List<Player> getAllPlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Get player by ID
     */
    public Player getPlayerById(String id) {
        for (Player player : players) {
            if (player.getId().equals(id)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Add a new player
     */
    public Player addPlayer(String name, PlayerRole role) {
        Player player = new Player(name, role);
        savePlayerToFirebase(player);
        return player;
    }

    /**
     * Update player information
     */
    public void updatePlayer(Player player) {
        savePlayerToFirebase(player);
    }

    /**
     * Delete player
     */
    public void deletePlayer(String playerId) {
        // Remove player from Firebase
        playersRef.child(playerId).removeValue();

        // Remove face embedding from Firebase
        faceEmbeddingsRef.child(playerId).removeValue();

        // Remove face image from storage
        StorageReference faceImageRef = faceImagesRef.child(playerId + ".jpg");
        faceImageRef.delete();

        // Remove from local cache
        players.removeIf(player -> player.getId().equals(playerId));
        faceEmbeddings.remove(playerId);
    }

    /**
     * Save player face image
     */
    public void savePlayerFaceImage(String playerId, Bitmap faceBitmap) {
        try {
            // Compress image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] data = baos.toByteArray();

            // Upload to Firebase Storage
            StorageReference faceImageRef = faceImagesRef.child(playerId + ".jpg");
            UploadTask uploadTask = faceImageRef.putBytes(data);

            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Face image uploaded for player: " + playerId);

                        // Also save locally for quick access
                        saveImageLocally(playerId, data);
                    } else {
                        Log.e(TAG, "Error uploading face image", task.getException());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing face image", e);
        }
    }

    /**
     * Save image data to local storage
     */
    private void saveImageLocally(String playerId, byte[] imageData) {
        try {
            File faceFile = getFaceImageFile(playerId);
            FileOutputStream fos = new FileOutputStream(faceFile);
            fos.write(imageData);
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error saving face image locally", e);
        }
    }

    /**
     * Load player face image
     */
    public Bitmap loadPlayerFaceImage(String playerId) {
        // First try to load from local cache
        File faceFile = getFaceImageFile(playerId);
        if (faceFile.exists()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(faceFile.getAbsolutePath());
                if (bitmap != null) {
                    return bitmap;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading local face image", e);
            }
        }

        // If not in cache, download from Firebase Storage
        // This is a synchronous call - in a real app, you would do this asynchronously
        try {
            StorageReference faceImageRef = faceImagesRef.child(playerId + ".jpg");
            Task<byte[]> downloadTask = faceImageRef.getBytes(Long.MAX_VALUE);

            while (!downloadTask.isComplete()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            if (downloadTask.isSuccessful()) {
                byte[] data = downloadTask.getResult();
                saveImageLocally(playerId, data);
                return BitmapFactory.decodeByteArray(data, 0, data.length);
            } else {
                Log.e(TAG, "Error downloading face image", downloadTask.getException());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in face image download process", e);
        }

        return null;
    }

    /**
     * Get file for face image storage
     */
    private File getFaceImageFile(String playerId) {
        File dir = new File(context.getFilesDir(), "player_faces");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, playerId + ".jpg");
    }

    /**
     * Save player face embedding
     */
    public void savePlayerFaceEmbedding(String playerId, float[] embedding) {
        String base64Embedding = encodeEmbedding(embedding);
        if (base64Embedding != null) {
            // Save to Firebase
            faceEmbeddingsRef.child(playerId).setValue(base64Embedding)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Face embedding saved to Firebase for player: " + playerId);

                                // Update local cache
                                faceEmbeddings.put(playerId, embedding);
                            } else {
                                Log.e(TAG, "Error saving face embedding to Firebase", task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Get player face embedding
     */
    public float[] getPlayerFaceEmbedding(String playerId) {
        return faceEmbeddings.get(playerId);
    }

    /**
     * Check if database is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (playersListener != null) {
            playersRef.removeEventListener(playersListener);
        }

        if (embeddingsListener != null) {
            faceEmbeddingsRef.removeEventListener(embeddingsListener);
        }
    }
}