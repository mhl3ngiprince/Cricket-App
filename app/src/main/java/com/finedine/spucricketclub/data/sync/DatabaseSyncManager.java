package com.finedine.spucricketclub.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.finedine.spucricketclub.data.db.AppDatabase;
import com.finedine.spucricketclub.data.db.entity.MatchEntity;
import com.finedine.spucricketclub.data.db.entity.PlayerEntity;
import com.finedine.spucricketclub.data.db.entity.TeamEntity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages bidirectional synchronization between Firebase Realtime Database and local Room database
 * Ensures data consistency and offline capability while maintaining real-time updates
 */
public class DatabaseSyncManager {
    private static final String TAG = "DatabaseSyncManager";
    private static final String SYNC_WORK_NAME = "database_sync_work";
    private static final long SYNC_INTERVAL_MINUTES = 15;

    private static DatabaseSyncManager instance;

    private final Context context;
    private final AppDatabase localDb;
    private final FirebaseDatabase firebaseDb;
    private final Executor dbExecutor;
    private final Map<String, ValueEventListener> activeListeners = new HashMap<>();
    private boolean isInitialized = false;

    // Constructor is private for singleton
    private DatabaseSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.localDb = AppDatabase.getInstance(context);
        this.firebaseDb = FirebaseDatabase.getInstance();
        this.dbExecutor = Executors.newFixedThreadPool(4);

        // Enable Firebase offline capabilities
        firebaseDb.setPersistenceEnabled(true);
    }

    // Get singleton instance
    public static synchronized DatabaseSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseSyncManager(context);
        }
        return instance;
    }

    /**
     * Initialize the sync manager and setup listeners for real-time updates
     */
    public void initialize() {
        if (isInitialized) {
            return;
        }

        // Schedule periodic sync using WorkManager
        schedulePeriodicSync();

        // Start real-time listeners for critical data
        setupPlayerSyncListener();
        setupTeamSyncListener();
        setupMatchSyncListener();

        isInitialized = true;
        Log.d(TAG, "Database sync manager initialized");
    }

    /**
     * Schedule periodic sync for when the app might have been offline
     */
    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                DatabaseSyncWorker.class,
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Keep existing if there is one
                syncWorkRequest
        );
    }

    /**
     * Set up listener for player data changes from Firebase
     */
    private void setupPlayerSyncListener() {
        DatabaseReference playersRef = firebaseDb.getReference("players");
        ValueEventListener playerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PlayerEntity> players = new ArrayList<>();
                for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                    try {
                        PlayerEntity player = playerSnapshot.getValue(PlayerEntity.class);
                        if (player != null) {
                            players.add(player);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing player data: " + e.getMessage());
                    }
                }

                if (!players.isEmpty()) {
                    savePlayersToLocalDb(players);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Player sync failed: " + error.getMessage());
            }
        };

        playersRef.addValueEventListener(playerListener);
        activeListeners.put("players", playerListener);
    }

    /**
     * Set up listener for team data changes from Firebase
     */
    private void setupTeamSyncListener() {
        DatabaseReference teamsRef = firebaseDb.getReference("teams");
        ValueEventListener teamListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<TeamEntity> teams = new ArrayList<>();
                for (DataSnapshot teamSnapshot : snapshot.getChildren()) {
                    try {
                        TeamEntity team = teamSnapshot.getValue(TeamEntity.class);
                        if (team != null) {
                            teams.add(team);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing team data: " + e.getMessage());
                    }
                }

                if (!teams.isEmpty()) {
                    saveTeamsToLocalDb(teams);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Team sync failed: " + error.getMessage());
            }
        };

        teamsRef.addValueEventListener(teamListener);
        activeListeners.put("teams", teamListener);
    }

    /**
     * Set up listener for match data changes from Firebase
     */
    private void setupMatchSyncListener() {
        DatabaseReference matchesRef = firebaseDb.getReference("matches");
        ValueEventListener matchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MatchEntity> matches = new ArrayList<>();
                for (DataSnapshot matchSnapshot : snapshot.getChildren()) {
                    try {
                        MatchEntity match = matchSnapshot.getValue(MatchEntity.class);
                        if (match != null) {
                            matches.add(match);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing match data: " + e.getMessage());
                    }
                }

                if (!matches.isEmpty()) {
                    saveMatchesToLocalDb(matches);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Match sync failed: " + error.getMessage());
            }
        };

        matchesRef.addValueEventListener(matchListener);
        activeListeners.put("matches", matchListener);
    }

    /**
     * Save player data to local Room database
     */
    private void savePlayersToLocalDb(final List<PlayerEntity> players) {
        dbExecutor.execute(() -> {
            try {
                localDb.playerDao().insertAll(players);
                Log.d(TAG, "Saved " + players.size() + " players to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving players to local database: " + e.getMessage());
            }
        });
    }

    /**
     * Save team data to local Room database
     */
    private void saveTeamsToLocalDb(final List<TeamEntity> teams) {
        dbExecutor.execute(() -> {
            try {
                for (TeamEntity team : teams) {
                    localDb.teamDao().insert(team);
                }
                Log.d(TAG, "Saved " + teams.size() + " teams to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving teams to local database: " + e.getMessage());
            }
        });
    }

    /**
     * Save match data to local Room database
     */
    private void saveMatchesToLocalDb(final List<MatchEntity> matches) {
        dbExecutor.execute(() -> {
            try {
                for (MatchEntity match : matches) {
                    localDb.matchDao().insert(match);
                }
                Log.d(TAG, "Saved " + matches.size() + " matches to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving matches to local database: " + e.getMessage());
            }
        });
    }

    /**
     * Push a player update to Firebase
     * (Will trigger the Firebase listener to update Room DB)
     */
    public void pushPlayerToFirebase(PlayerEntity player) {
        if (player == null || player.getId() == null) {
            Log.e(TAG, "Cannot push player with null ID");
            return;
        }

        DatabaseReference playerRef = firebaseDb.getReference("players").child(player.getId());
        playerRef.setValue(player)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Player pushed to Firebase: " + player.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to push player to Firebase: " + e.getMessage()));
    }

    /**
     * Push a team update to Firebase
     * (Will trigger the Firebase listener to update Room DB)
     */
    public void pushTeamToFirebase(TeamEntity team) {
        if (team == null || team.getId() == null) {
            Log.e(TAG, "Cannot push team with null ID");
            return;
        }

        DatabaseReference teamRef = firebaseDb.getReference("teams").child(team.getId());
        teamRef.setValue(team)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Team pushed to Firebase: " + team.getTeamName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to push team to Firebase: " + e.getMessage()));
    }

    /**
     * Push a match update to Firebase
     * (Will trigger the Firebase listener to update Room DB)
     */
    public void pushMatchToFirebase(MatchEntity match) {
        if (match == null || match.getId() == null) {
            Log.e(TAG, "Cannot push match with null ID");
            return;
        }

        DatabaseReference matchRef = firebaseDb.getReference("matches").child(match.getId());
        matchRef.setValue(match)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Match pushed to Firebase: " + match.getMatchName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to push match to Firebase: " + e.getMessage()));
    }

    /**
     * Force a manual sync from Room to Firebase (all data)
     */
    public void syncLocalToFirebase() {
        dbExecutor.execute(() -> {
            try {
                // Sync players
                List<PlayerEntity> players = localDb.playerDao().getAllSync();
                for (PlayerEntity player : players) {
                    pushPlayerToFirebase(player);
                }

                // Sync teams
                List<TeamEntity> teams = localDb.teamDao().getAllSync();
                for (TeamEntity team : teams) {
                    pushTeamToFirebase(team);
                }

                // Sync matches
                List<MatchEntity> matches = localDb.matchDao().getAllSync();
                for (MatchEntity match : matches) {
                    pushMatchToFirebase(match);
                }

                Log.d(TAG, "Manual sync from local to Firebase completed");
            } catch (Exception e) {
                Log.e(TAG, "Error during manual sync: " + e.getMessage());
            }
        });
    }

    /**
     * Force a manual sync from Firebase to Room (all data)
     */
    public void syncFirebaseToLocal() {
        // Trigger Firebase listeners which will update Room
        firebaseDb.getReference("players").get()
                .addOnSuccessListener(snapshot -> Log.d(TAG, "Players sync triggered"))
                .addOnFailureListener(e -> Log.e(TAG, "Players sync failed: " + e.getMessage()));

        firebaseDb.getReference("teams").get()
                .addOnSuccessListener(snapshot -> Log.d(TAG, "Teams sync triggered"))
                .addOnFailureListener(e -> Log.e(TAG, "Teams sync failed: " + e.getMessage()));

        firebaseDb.getReference("matches").get()
                .addOnSuccessListener(snapshot -> Log.d(TAG, "Matches sync triggered"))
                .addOnFailureListener(e -> Log.e(TAG, "Matches sync failed: " + e.getMessage()));
    }

    /**
     * Check if device has active network connection
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network network = cm.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            }
        }
        return false;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        // Remove all Firebase listeners
        for (Map.Entry<String, ValueEventListener> entry : activeListeners.entrySet()) {
            String path = entry.getKey();
            ValueEventListener listener = entry.getValue();
            firebaseDb.getReference(path).removeEventListener(listener);
        }
        activeListeners.clear();

        isInitialized = false;
        Log.d(TAG, "Database sync manager cleaned up");
    }
}