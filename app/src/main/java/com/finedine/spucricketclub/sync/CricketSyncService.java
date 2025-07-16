package com.finedine.spucricketclub.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.finedine.spucricketclub.cricket.Ball;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for real-time synchronization of cricket scores across devices
 */
public class CricketSyncService {
    private static final String TAG = "CricketSyncService";
    private static final String DB_MATCHES_PATH = "matches";
    private static final String DB_BALLS_PATH = "balls";

    // Singleton instance
    private static CricketSyncService instance;

    // Firebase database reference
    private final DatabaseReference databaseRef;

    // Current match reference
    private DatabaseReference currentMatchRef;
    private String currentMatchId;

    // Context
    private final Context context;

    // Main thread handler for callbacks
    private final Handler mainHandler;

    // Listeners
    private final List<ScoreSyncListener> syncListeners = new ArrayList<>();
    private ValueEventListener scoreListener;
    private ValueEventListener matchListener;

    /**
     * Interface for receiving score sync updates
     */
    public interface ScoreSyncListener {
        void onScoreUpdated(Ball ball);

        void onMatchDataUpdated(Match match);

        void onSyncError(String error);
    }

    // Private constructor for singleton pattern
    private CricketSyncService(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference();

        // Enable disk persistence for offline capability
        database.setPersistenceEnabled(true);

        Log.d(TAG, "CricketSyncService initialized");
    }

    /**
     * Get singleton instance
     */
    public static synchronized CricketSyncService getInstance(Context context) {
        if (instance == null) {
            instance = new CricketSyncService(context);
        }
        return instance;
    }

    /**
     * Register a listener for score sync events
     */
    public void registerListener(ScoreSyncListener listener) {
        if (!syncListeners.contains(listener)) {
            syncListeners.add(listener);
            Log.d(TAG, "Listener registered");
        }
    }

    /**
     * Unregister a listener
     */
    public void unregisterListener(ScoreSyncListener listener) {
        syncListeners.remove(listener);
        Log.d(TAG, "Listener unregistered");
    }

    /**
     * Create a new match in the database
     */
    public String createMatch(Match match) {
        // Generate a new ID if not present
        if (match.getId() == null || match.getId().isEmpty()) {
            currentMatchId = databaseRef.child(DB_MATCHES_PATH).push().getKey();
        } else {
            currentMatchId = match.getId();
        }

        // Set up the match reference
        currentMatchRef = databaseRef.child(DB_MATCHES_PATH).child(currentMatchId);

        // Convert match to map for firebase
        Map<String, Object> matchData = matchToMap(match);

        // Upload match data
        currentMatchRef.setValue(matchData, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to save match: " + error.getMessage());
                notifyError("Failed to save match: " + error.getMessage());
            } else {
                Log.d(TAG, "Match created with ID: " + currentMatchId);
                match.setId(currentMatchId);
                setupMatchListeners();
            }
        });

        return currentMatchId;
    }

    /**
     * Join an existing match by ID
     */
    public void joinMatch(String matchId) {
        currentMatchId = matchId;
        currentMatchRef = databaseRef.child(DB_MATCHES_PATH).child(currentMatchId);
        setupMatchListeners();
        Log.d(TAG, "Joined match with ID: " + matchId);
    }

    /**
     * Leave the current match
     */
    public void leaveMatch() {
        if (currentMatchRef != null) {
            // Remove listeners
            if (matchListener != null) {
                currentMatchRef.removeEventListener(matchListener);
            }

            if (scoreListener != null) {
                currentMatchRef.child(DB_BALLS_PATH).removeEventListener(scoreListener);
            }

            currentMatchRef = null;
            currentMatchId = null;
            Log.d(TAG, "Left match");
        }
    }

    /**
     * Set up listeners for match data
     */
    private void setupMatchListeners() {
        // Listen for match data changes
        matchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    Map<String, Object> matchData = (Map<String, Object>) snapshot.getValue();
                    if (matchData != null) {
                        Match match = mapToMatch(matchData);
                        notifyMatchUpdated(match);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing match data", e);
                    notifyError("Error parsing match data: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Match listener cancelled: " + error.getMessage());
                notifyError("Match sync failed: " + error.getMessage());
            }
        };

        currentMatchRef.addValueEventListener(matchListener);

        // Listen for new balls (score updates)
        scoreListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    for (DataSnapshot ballSnapshot : snapshot.getChildren()) {
                        Map<String, Object> ballData = (Map<String, Object>) ballSnapshot.getValue();
                        if (ballData != null && !ballData.containsKey("synced")) {
                            Ball ball = mapToBall(ballData);
                            notifyScoreUpdated(ball);

                            // Mark as synced
                            Map<String, Object> update = new HashMap<>();
                            update.put("synced", true);
                            ballSnapshot.getRef().updateChildren(update);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing ball data", e);
                    notifyError("Error parsing ball data: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Score listener cancelled: " + error.getMessage());
                notifyError("Score sync failed: " + error.getMessage());
            }
        };

        currentMatchRef.child(DB_BALLS_PATH).addValueEventListener(scoreListener);
    }

    /**
     * Sync a new ball to the database
     */
    public void syncBall(Ball ball) {
        if (currentMatchRef == null) {
            Log.e(TAG, "Cannot sync ball, no active match");
            return;
        }

        // Convert ball to map
        Map<String, Object> ballData = ballToMap(ball);

        // Add to database
        DatabaseReference ballRef = currentMatchRef.child(DB_BALLS_PATH).push();
        ballRef.setValue(ballData, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to sync ball: " + error.getMessage());
                notifyError("Failed to sync ball: " + error.getMessage());
            } else {
                Log.d(TAG, "Ball synced successfully");
            }
        });
    }

    /**
     * Sync the current match state
     */
    public void syncMatchState(Match match) {
        if (currentMatchRef == null) {
            Log.e(TAG, "Cannot sync match state, no active match");
            return;
        }

        // Convert match to map
        Map<String, Object> matchData = matchToMap(match);

        // Update in database
        currentMatchRef.updateChildren(matchData, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to sync match state: " + error.getMessage());
                notifyError("Failed to sync match state: " + error.getMessage());
            } else {
                Log.d(TAG, "Match state synced successfully");
            }
        });
    }

    /**
     * Convert a Match object to a Map for Firebase
     */
    private Map<String, Object> matchToMap(Match match) {
        Map<String, Object> matchMap = new HashMap<>();

        // Basic match data
        matchMap.put("id", match.getId());
        matchMap.put("name", match.getMatchName());
        matchMap.put("type", match.getMatchType().name());
        matchMap.put("status", match.getStatus().name());
        matchMap.put("venue", match.getVenue());
        matchMap.put("date", match.getMatchDate().getTime());
        matchMap.put("maxOvers", match.getMaxOvers());
        matchMap.put("targetScore", match.getTargetScore());

        // Team data would be added here in a real implementation
        // This is simplified for the example

        return matchMap;
    }

    /**
     * Convert a Ball object to a Map for Firebase
     */
    private Map<String, Object> ballToMap(Ball ball) {
        Map<String, Object> ballMap = new HashMap<>();

        // Ball data
        ballMap.put("ballNumber", ball.getBallNumber());
        ballMap.put("runsScored", ball.getRunsScored());
        ballMap.put("isWicket", ball.isWicket());
        ballMap.put("type", ball.getType().name());
        ballMap.put("timestamp", System.currentTimeMillis());

        // Player IDs
        if (ball.getBatsman() != null) {
            ballMap.put("batsmanId", ball.getBatsman().getId());
        }

        if (ball.getBowler() != null) {
            ballMap.put("bowlerId", ball.getBowler().getId());
        }

        return ballMap;
    }

    /**
     * Convert a Map from Firebase to a Match object
     */
    private Match mapToMatch(Map<String, Object> matchMap) {
        // This would be a more complete implementation in a real app
        // Simplified for example
        Match match = new Match();

        match.setId((String) matchMap.get("id"));
        match.setMatchName((String) matchMap.get("name"));

        String typeStr = (String) matchMap.get("type");
        if (typeStr != null) {
            match.setMatchType(Match.MatchType.valueOf(typeStr));
        }

        String statusStr = (String) matchMap.get("status");
        if (statusStr != null) {
            match.setStatus(Match.MatchStatus.valueOf(statusStr));
        }

        match.setVenue((String) matchMap.get("venue"));

        Long maxOvers = (Long) matchMap.get("maxOvers");
        if (maxOvers != null) {
            match.setMaxOvers(maxOvers.intValue());
        }

        Long targetScore = (Long) matchMap.get("targetScore");
        if (targetScore != null) {
            match.setTargetScore(targetScore.intValue());
        }

        return match;
    }

    /**
     * Convert a Map from Firebase to a Ball object
     */
    private Ball mapToBall(Map<String, Object> ballMap) {
        // Simplified for example
        int ballNumber = ((Long) ballMap.get("ballNumber")).intValue();
        int runsScored = ((Long) ballMap.get("runsScored")).intValue();
        boolean isWicket = (boolean) ballMap.get("isWicket");

        String typeStr = (String) ballMap.get("type");
        Ball.BallType type = Ball.BallType.NORMAL;
        if (typeStr != null) {
            type = Ball.BallType.valueOf(typeStr);
        }

        return new Ball(ballNumber, runsScored, isWicket, type);
    }

    /**
     * Notify listeners of a score update
     */
    private void notifyScoreUpdated(final Ball ball) {
        mainHandler.post(() -> {
            for (ScoreSyncListener listener : syncListeners) {
                listener.onScoreUpdated(ball);
            }
        });
    }

    /**
     * Notify listeners of a match data update
     */
    private void notifyMatchUpdated(final Match match) {
        mainHandler.post(() -> {
            for (ScoreSyncListener listener : syncListeners) {
                listener.onMatchDataUpdated(match);
            }
        });
    }

    /**
     * Notify listeners of an error
     */
    private void notifyError(final String error) {
        mainHandler.post(() -> {
            for (ScoreSyncListener listener : syncListeners) {
                listener.onSyncError(error);
            }
        });
    }
}