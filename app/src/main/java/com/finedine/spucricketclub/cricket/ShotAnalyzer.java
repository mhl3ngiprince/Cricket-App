package com.finedine.spucricketclub.cricket;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes cricket shots and provides insights on batting patterns
 */
public class ShotAnalyzer {
    private static final String TAG = "ShotAnalyzer";

    // Singleton instance
    private static ShotAnalyzer instance;

    // Database reference
    private final DatabaseReference databaseReference;

    // Cached shot data
    private final Map<String, List<ShotData>> playerShotCache = new HashMap<>();

    /**
     * Inner class to hold shot data
     */
    public static class ShotData {
        private final Ball.ShotDirection direction;
        private final Ball.ShotType type;
        private final int runs;
        private final boolean isWicket;
        private final String matchId;
        private final long timestamp;

        public ShotData(Ball ball, String matchId) {
            this.direction = ball.getShotDirection();
            this.type = ball.getShotType();
            this.runs = ball.getRunsScored();
            this.isWicket = ball.isWicket();
            this.matchId = matchId;
            this.timestamp = ball.getTimestamp() != null ? ball.getTimestamp().getTime() : System.currentTimeMillis();
        }

        public Ball.ShotDirection getDirection() {
            return direction;
        }

        public Ball.ShotType getType() {
            return type;
        }

        public int getRuns() {
            return runs;
        }

        public boolean isWicket() {
            return isWicket;
        }

        public String getMatchId() {
            return matchId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Private constructor for singleton
     */
    private ShotAnalyzer() {
        databaseReference = FirebaseDatabase.getInstance().getReference("shot_data");
    }

    /**
     * Get singleton instance
     */
    public static synchronized ShotAnalyzer getInstance() {
        if (instance == null) {
            instance = new ShotAnalyzer();
        }
        return instance;
    }

    /**
     * Record a new shot for a player
     */
    public void recordShot(Player player, Ball ball, String matchId) {
        if (player == null || ball == null || matchId == null) {
            Log.e(TAG, "Cannot record shot with null data");
            return;
        }

        // Only record shot if direction and type are specified
        if (ball.getShotDirection() != Ball.ShotDirection.NOT_APPLICABLE &&
                ball.getShotType() != Ball.ShotType.NOT_APPLICABLE) {

            try {
                // Create shot data
                ShotData shotData = new ShotData(ball, matchId);

                // Update local cache
                String playerId = player.getId();
                if (!playerShotCache.containsKey(playerId)) {
                    playerShotCache.put(playerId, new ArrayList<>());
                }
                playerShotCache.get(playerId).add(shotData);

                // Save to Firebase
                Map<String, Object> shotInfo = new HashMap<>();
                shotInfo.put("direction", ball.getShotDirection().toString());
                shotInfo.put("type", ball.getShotType().toString());
                shotInfo.put("runs", ball.getRunsScored());
                shotInfo.put("isWicket", ball.isWicket());
                shotInfo.put("matchId", matchId);
                shotInfo.put("timestamp", System.currentTimeMillis());

                databaseReference.child(playerId).push().setValue(shotInfo)
                        .addOnFailureListener(e -> Log.e(TAG, "Error saving shot data", e));

                Log.d(TAG, "Recorded shot for player: " + player.getName());
            } catch (Exception e) {
                Log.e(TAG, "Error recording shot", e);
            }
        }
    }

    /**
     * Get player's favorite shot type
     */
    public Ball.ShotType getFavoriteShot(String playerId) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty()) {
            return null;
        }

        // Count occurrences of each shot type
        Map<Ball.ShotType, Integer> shotTypeCounts = new HashMap<>();
        for (ShotData shot : shots) {
            if (shot.getType() != Ball.ShotType.NOT_APPLICABLE) {
                shotTypeCounts.put(shot.getType(),
                        shotTypeCounts.getOrDefault(shot.getType(), 0) + 1);
            }
        }

        // Find shot type with highest count
        Ball.ShotType favoriteShot = null;
        int maxCount = 0;

        for (Map.Entry<Ball.ShotType, Integer> entry : shotTypeCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favoriteShot = entry.getKey();
            }
        }

        return favoriteShot;
    }

    /**
     * Get player's favorite scoring area/direction
     */
    public Ball.ShotDirection getFavoriteDirection(String playerId) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty()) {
            return null;
        }

        // Count occurrences of each direction
        Map<Ball.ShotDirection, Integer> directionCounts = new HashMap<>();
        for (ShotData shot : shots) {
            if (shot.getDirection() != Ball.ShotDirection.NOT_APPLICABLE) {
                directionCounts.put(shot.getDirection(),
                        directionCounts.getOrDefault(shot.getDirection(), 0) + 1);
            }
        }

        // Find direction with highest count
        Ball.ShotDirection favoriteDirection = null;
        int maxCount = 0;

        for (Map.Entry<Ball.ShotDirection, Integer> entry : directionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favoriteDirection = entry.getKey();
            }
        }

        return favoriteDirection;
    }

    /**
     * Get most productive shot (highest average runs)
     */
    public Ball.ShotType getMostProductiveShot(String playerId) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty()) {
            return null;
        }

        // Track total runs and counts for each shot type
        Map<Ball.ShotType, Integer> runsByType = new HashMap<>();
        Map<Ball.ShotType, Integer> countsByType = new HashMap<>();

        for (ShotData shot : shots) {
            if (shot.getType() != Ball.ShotType.NOT_APPLICABLE) {
                Ball.ShotType type = shot.getType();
                runsByType.put(type, runsByType.getOrDefault(type, 0) + shot.getRuns());
                countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
            }
        }

        // Calculate average runs per shot type
        Ball.ShotType mostProductiveShot = null;
        float highestAverage = 0;

        for (Ball.ShotType type : runsByType.keySet()) {
            int totalRuns = runsByType.get(type);
            int count = countsByType.get(type);

            if (count > 0) {
                float average = (float) totalRuns / count;
                if (average > highestAverage) {
                    highestAverage = average;
                    mostProductiveShot = type;
                }
            }
        }

        return mostProductiveShot;
    }

    /**
     * Get most productive direction (highest average runs)
     */
    public Ball.ShotDirection getMostProductiveDirection(String playerId) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty()) {
            return null;
        }

        // Track total runs and counts for each direction
        Map<Ball.ShotDirection, Integer> runsByDirection = new HashMap<>();
        Map<Ball.ShotDirection, Integer> countsByDirection = new HashMap<>();

        for (ShotData shot : shots) {
            if (shot.getDirection() != Ball.ShotDirection.NOT_APPLICABLE) {
                Ball.ShotDirection direction = shot.getDirection();
                runsByDirection.put(direction, runsByDirection.getOrDefault(direction, 0) + shot.getRuns());
                countsByDirection.put(direction, countsByDirection.getOrDefault(direction, 0) + 1);
            }
        }

        // Calculate average runs per direction
        Ball.ShotDirection mostProductiveDirection = null;
        float highestAverage = 0;

        for (Ball.ShotDirection direction : runsByDirection.keySet()) {
            int totalRuns = runsByDirection.get(direction);
            int count = countsByDirection.get(direction);

            if (count > 0) {
                float average = (float) totalRuns / count;
                if (average > highestAverage) {
                    highestAverage = average;
                    mostProductiveDirection = direction;
                }
            }
        }

        return mostProductiveDirection;
    }

    /**
     * Get shot weakness (shots that lead to wickets)
     */
    public Ball.ShotType getWeakestShot(String playerId) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty()) {
            return null;
        }

        // Count wickets by shot type
        Map<Ball.ShotType, Integer> wicketsByType = new HashMap<>();
        Map<Ball.ShotType, Integer> countsByType = new HashMap<>();

        for (ShotData shot : shots) {
            if (shot.getType() != Ball.ShotType.NOT_APPLICABLE) {
                Ball.ShotType type = shot.getType();
                if (shot.isWicket()) {
                    wicketsByType.put(type, wicketsByType.getOrDefault(type, 0) + 1);
                }
                countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
            }
        }

        // Calculate dismissal rate by shot type
        Ball.ShotType weakestShot = null;
        float highestDismissalRate = 0;

        for (Ball.ShotType type : countsByType.keySet()) {
            int wickets = wicketsByType.getOrDefault(type, 0);
            int count = countsByType.get(type);

            // Only consider shot types played at least 5 times
            if (count >= 5) {
                float dismissalRate = (float) wickets / count;
                if (dismissalRate > highestDismissalRate) {
                    highestDismissalRate = dismissalRate;
                    weakestShot = type;
                }
            }
        }

        return weakestShot;
    }

    /**
     * Get boundary percentage by shot type
     */
    public Map<Ball.ShotType, Float> getBoundaryPercentageByShot(String playerId) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty()) {
            return null;
        }

        // Count boundaries by shot type
        Map<Ball.ShotType, Integer> boundariesByType = new HashMap<>();
        Map<Ball.ShotType, Integer> countsByType = new HashMap<>();

        for (ShotData shot : shots) {
            if (shot.getType() != Ball.ShotType.NOT_APPLICABLE) {
                Ball.ShotType type = shot.getType();
                if (shot.getRuns() == 4 || shot.getRuns() == 6) {
                    boundariesByType.put(type, boundariesByType.getOrDefault(type, 0) + 1);
                }
                countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
            }
        }

        // Calculate boundary percentage by shot type
        Map<Ball.ShotType, Float> boundaryPercentages = new HashMap<>();

        for (Ball.ShotType type : countsByType.keySet()) {
            int boundaries = boundariesByType.getOrDefault(type, 0);
            int count = countsByType.get(type);

            if (count > 0) {
                float percentage = (float) boundaries * 100 / count;
                boundaryPercentages.put(type, percentage);
            }
        }

        return boundaryPercentages;
    }

    /**
     * Get shot distribution (percentage of each shot type)
     */
    public Map<Ball.ShotType, Float> getShotDistribution(String playerId) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty()) {
            return null;
        }

        // Count occurrences of each shot type
        Map<Ball.ShotType, Integer> shotTypeCounts = new HashMap<>();
        int totalShots = 0;

        for (ShotData shot : shots) {
            if (shot.getType() != Ball.ShotType.NOT_APPLICABLE) {
                shotTypeCounts.put(shot.getType(),
                        shotTypeCounts.getOrDefault(shot.getType(), 0) + 1);
                totalShots++;
            }
        }

        // Calculate percentage of each shot type
        Map<Ball.ShotType, Float> distribution = new HashMap<>();

        if (totalShots > 0) {
            for (Map.Entry<Ball.ShotType, Integer> entry : shotTypeCounts.entrySet()) {
                float percentage = (float) entry.getValue() * 100 / totalShots;
                distribution.put(entry.getKey(), percentage);
            }
        }

        return distribution;
    }

    /**
     * Get shot trend over time (how player's shot selection has evolved)
     */
    public Map<Ball.ShotType, List<Float>> getShotTrend(String playerId, int numPeriods) {
        List<ShotData> shots = getPlayerShots(playerId);
        if (shots == null || shots.isEmpty() || numPeriods <= 0) {
            return null;
        }

        // Sort shots by timestamp
        List<ShotData> sortedShots = new ArrayList<>(shots);
        Collections.sort(sortedShots, Comparator.comparingLong(ShotData::getTimestamp));

        // Calculate period size
        int periodSize = Math.max(1, sortedShots.size() / numPeriods);

        // Initialize shot type set
        Map<Ball.ShotType, List<Float>> trends = new HashMap<>();
        for (ShotData shot : sortedShots) {
            if (shot.getType() != Ball.ShotType.NOT_APPLICABLE) {
                if (!trends.containsKey(shot.getType())) {
                    trends.put(shot.getType(), new ArrayList<>());
                }
            }
        }

        // Analyze each period
        for (int i = 0; i < numPeriods; i++) {
            int startIndex = i * periodSize;
            int endIndex = Math.min(sortedShots.size(), (i + 1) * periodSize);

            // Skip partial periods
            if (endIndex - startIndex < periodSize / 2) {
                break;
            }

            Map<Ball.ShotType, Integer> periodCounts = new HashMap<>();
            int totalInPeriod = 0;

            // Count shots in this period
            for (int j = startIndex; j < endIndex; j++) {
                ShotData shot = sortedShots.get(j);
                if (shot.getType() != Ball.ShotType.NOT_APPLICABLE) {
                    periodCounts.put(shot.getType(),
                            periodCounts.getOrDefault(shot.getType(), 0) + 1);
                    totalInPeriod++;
                }
            }

            // Update trends for each shot type
            if (totalInPeriod > 0) {
                for (Ball.ShotType type : trends.keySet()) {
                    float percentage = periodCounts.getOrDefault(type, 0) * 100.0f / totalInPeriod;
                    trends.get(type).add(percentage);
                }
            }
        }

        return trends;
    }

    /**
     * Get player shots from cache or database
     */
    private List<ShotData> getPlayerShots(String playerId) {
        if (playerId == null) {
            return null;
        }

        // Return from cache if available
        if (playerShotCache.containsKey(playerId)) {
            return playerShotCache.get(playerId);
        }

        // Otherwise, load from database
        loadPlayerShotsFromDatabase(playerId);

        // Return empty list while loading
        return playerShotCache.getOrDefault(playerId, new ArrayList<>());
    }

    /**
     * Load player shots from database
     */
    private void loadPlayerShotsFromDatabase(String playerId) {
        databaseReference.child(playerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<ShotData> shots = new ArrayList<>();

                    for (DataSnapshot shotSnapshot : snapshot.getChildren()) {
                        try {
                            Ball.ShotDirection direction = Ball.ShotDirection.valueOf(
                                    shotSnapshot.child("direction").getValue(String.class));
                            Ball.ShotType type = Ball.ShotType.valueOf(
                                    shotSnapshot.child("type").getValue(String.class));
                            int runs = shotSnapshot.child("runs").getValue(Integer.class);
                            boolean isWicket = shotSnapshot.child("isWicket").getValue(Boolean.class);
                            String matchId = shotSnapshot.child("matchId").getValue(String.class);
                            long timestamp = shotSnapshot.child("timestamp").getValue(Long.class);

                            // Create mock ball to pass to ShotData constructor
                            Ball ball = new Ball();
                            ball.setShotDirection(direction);
                            ball.setShotType(type);
                            ball.setRunsScored(runs);
                            ball.setWicket(isWicket);

                            ShotData shotData = new ShotData(ball, matchId);
                            shots.add(shotData);

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing shot data", e);
                        }
                    }

                    // Update cache
                    playerShotCache.put(playerId, shots);

                    Log.d(TAG, "Loaded " + shots.size() + " shots for player " + playerId);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading player shots", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading player shots", error.toException());
            }
        });
    }

    /**
     * Clear shot cache for testing
     */
    public void clearCache() {
        playerShotCache.clear();
    }
}