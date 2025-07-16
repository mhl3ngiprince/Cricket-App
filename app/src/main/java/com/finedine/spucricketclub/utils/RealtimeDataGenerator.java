package com.finedine.spucricketclub.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.finedine.spucricketclub.cricket.Ball;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.data.db.entity.MatchEntity;
import com.finedine.spucricketclub.data.sync.DatabaseSyncManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Utility class to generate real-time cricket match data
 * for demonstration and testing purposes.
 * <p>
 * This simulates real-world scenarios where multiple devices
 * are receiving updates about an ongoing match.
 */
public class RealtimeDataGenerator {
    private static final String TAG = "RealtimeDataGenerator";
    private static final long MIN_BALL_INTERVAL_MS = 10000; // 10 seconds per ball minimum
    private static final long MAX_BALL_INTERVAL_MS = 45000; // 45 seconds per ball maximum
    private static final double WICKET_PROBABILITY = 0.08; // 8% chance of wicket per ball
    private static final double DOT_BALL_PROBABILITY = 0.35; // 35% chance of dot ball
    private static final double BOUNDARY_PROBABILITY = 0.15; // 15% chance of boundary (4 or 6)

    private final DatabaseReference matchRef;
    private final String matchId;
    private final Handler handler;
    private final Random random = new Random();
    private final DatabaseSyncManager syncManager;

    private boolean isGenerating = false;
    private int ballsGenerated = 0;
    private int wicketsGenerated = 0;
    private int runsGenerated = 0;
    private int currentOver = 0;
    private int ballInOver = 0;

    private ValueEventListener matchListener;

    /**
     * Constructor
     *
     * @param matchId     The ID of the match to generate data for
     * @param syncManager The database sync manager to use
     */
    public RealtimeDataGenerator(String matchId, DatabaseSyncManager syncManager) {
        this.matchId = matchId;
        this.syncManager = syncManager;
        this.matchRef = FirebaseDatabase.getInstance().getReference("matches").child(matchId);
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Start generating real-time data
     */
    public void startGeneration() {
        if (isGenerating) {
            return;
        }

        isGenerating = true;
        setupMatchListener();

        // Schedule first ball
        scheduleBall();

        Log.d(TAG, "Started real-time data generation for match: " + matchId);
    }

    /**
     * Stop generating real-time data
     */
    public void stopGeneration() {
        if (!isGenerating) {
            return;
        }

        isGenerating = false;
        handler.removeCallbacksAndMessages(null);

        if (matchListener != null) {
            matchRef.removeEventListener(matchListener);
            matchListener = null;
        }

        Log.d(TAG, "Stopped real-time data generation. Generated " +
                ballsGenerated + " balls, " + runsGenerated + " runs, " +
                wicketsGenerated + " wickets");
    }

    /**
     * Set up listener for match data
     */
    private void setupMatchListener() {
        matchListener = matchRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                MatchEntity match = snapshot.getValue(MatchEntity.class);
                if (match != null) {
                    // Update our internal state based on remote changes
                    // This would be used if multiple generators are active
                    updateStateFromMatch(match);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Match listener cancelled: " + error.getMessage());
            }
        });
    }

    /**
     * Update generator state from match entity
     */
    private void updateStateFromMatch(MatchEntity match) {
        // Extract current state from match data
        Map<String, Object> innings1Data = match.getInnings1Data();
        if (innings1Data == null) {
            innings1Data = new HashMap<>();
            match.setInnings1Data(innings1Data);
        }

        // Update local counters from match data
        if (innings1Data.containsKey("ballsGenerated")) {
            ballsGenerated = ((Number) innings1Data.get("ballsGenerated")).intValue();
        }

        if (innings1Data.containsKey("wicketsGenerated")) {
            wicketsGenerated = ((Number) innings1Data.get("wicketsGenerated")).intValue();
        }

        if (innings1Data.containsKey("runsGenerated")) {
            runsGenerated = ((Number) innings1Data.get("runsGenerated")).intValue();
        }

        if (innings1Data.containsKey("currentOver")) {
            currentOver = ((Number) innings1Data.get("currentOver")).intValue();
        }

        if (innings1Data.containsKey("ballInOver")) {
            ballInOver = ((Number) innings1Data.get("ballInOver")).intValue();
        }
    }

    /**
     * Schedule the next ball to be simulated
     */
    private void scheduleBall() {
        if (!isGenerating) {
            return;
        }

        // Calculate delay for next ball (random between MIN and MAX)
        long delay = MIN_BALL_INTERVAL_MS + random.nextInt((int) (MAX_BALL_INTERVAL_MS - MIN_BALL_INTERVAL_MS));

        handler.postDelayed(this::generateBall, delay);
    }

    /**
     * Generate a ball and update match data
     */
    private void generateBall() {
        if (!isGenerating) {
            return;
        }

        // Simulate this ball
        ballsGenerated++;
        ballInOver++;

        boolean isWicket = false;
        int runs = 0;

        double outcome = random.nextDouble();

        if (outcome < WICKET_PROBABILITY) {
            // Wicket ball
            isWicket = true;
            wicketsGenerated++;
        } else if (outcome < WICKET_PROBABILITY + DOT_BALL_PROBABILITY) {
            // Dot ball - no runs
            runs = 0;
        } else if (outcome < WICKET_PROBABILITY + DOT_BALL_PROBABILITY + BOUNDARY_PROBABILITY) {
            // Boundary - 4 or 6
            runs = random.nextBoolean() ? 4 : 6;
        } else {
            // Regular runs - 1, 2, or 3
            runs = random.nextInt(3) + 1;
        }

        runsGenerated += runs;

        // Check if over is complete
        if (ballInOver >= 6) {
            currentOver++;
            ballInOver = 0;
        }

        // Update Firebase with this ball
        updateMatchInFirebase(isWicket, runs);

        // Schedule next ball (if not all out or overs completed)
        if (wicketsGenerated < 10 && currentOver < 20) { // T20 format
            scheduleBall();
        } else {
            // Match is complete
            stopGeneration();
            updateMatchStatus("COMPLETED");
        }
    }

    /**
     * Update match data in Firebase
     */
    private void updateMatchInFirebase(boolean isWicket, int runs) {
        matchRef.get().addOnSuccessListener(dataSnapshot -> {
            MatchEntity match = dataSnapshot.getValue(MatchEntity.class);
            if (match == null) return;

            // Get or create innings data
            Map<String, Object> innings1Data = match.getInnings1Data();
            if (innings1Data == null) {
                innings1Data = new HashMap<>();
                match.setInnings1Data(innings1Data);
            }

            // Update match stats
            innings1Data.put("runs", runsGenerated);
            innings1Data.put("wickets", wicketsGenerated);
            innings1Data.put("overs", String.format("%d.%d", currentOver, ballInOver));
            innings1Data.put("ballsGenerated", ballsGenerated);
            innings1Data.put("currentOver", currentOver);
            innings1Data.put("ballInOver", ballInOver);

            // Create ball data
            Map<String, Object> ballData = new HashMap<>();
            ballData.put("isWicket", isWicket);
            ballData.put("runs", runs);
            ballData.put("timestamp", System.currentTimeMillis());

            // Add to balls list
            String ballKey = "ball_" + ballsGenerated;
            innings1Data.put(ballKey, ballData);

            // Add ball commentary
            String commentary = generateCommentary(isWicket, runs);
            innings1Data.put("lastCommentary", commentary);

            if (isWicket) {
                innings1Data.put("lastWicketBall", ballsGenerated);
            }

            // Push to Firebase (which will trigger listener and local sync)
            syncManager.pushMatchToFirebase(match);

            Log.d(TAG, "Generated ball: " + (isWicket ? "OUT" : runs + " runs") +
                    " - Match state: " + runsGenerated + "/" + wicketsGenerated +
                    " (" + currentOver + "." + ballInOver + ")");
        });
    }

    /**
     * Update match status
     */
    private void updateMatchStatus(String status) {
        matchRef.get().addOnSuccessListener(dataSnapshot -> {
            MatchEntity match = dataSnapshot.getValue(MatchEntity.class);
            if (match == null) return;

            match.setStatus(status);
            syncManager.pushMatchToFirebase(match);
        });
    }

    /**
     * Generate commentary for this ball
     */
    private String generateCommentary(boolean isWicket, int runs) {
        String[] batsmen = {"Smith", "Kohli", "Warner", "Root", "Williamson"};
        String[] bowlers = {"Bumrah", "Rabada", "Anderson", "Cummins", "Boult"};

        String batsman = batsmen[random.nextInt(batsmen.length)];
        String bowler = bowlers[random.nextInt(bowlers.length)];

        if (isWicket) {
            String[] wicketTypes = {"caught", "bowled", "lbw", "run out", "stumped"};
            String wicketType = wicketTypes[random.nextInt(wicketTypes.length)];

            String[] wicketCommentaries = {
                    batsman + " is out! " + wicketType + "! Great bowling by " + bowler + "!",
                    "WICKET! " + batsman + " is " + wicketType + "! Big breakthrough for the bowling side!",
                    bowler + " strikes! " + batsman + " is out " + wicketType + "!",
                    "That's a massive wicket! " + batsman + " has to go, " + wicketType + "!"
            };

            return wicketCommentaries[random.nextInt(wicketCommentaries.length)];
        } else if (runs == 0) {
            String[] dotBallCommentaries = {
                    "Good ball by " + bowler + ", no run.",
                    batsman + " defends solidly, dot ball.",
                    "Beat the bat, no run.",
                    "Played and missed by " + batsman + "."
            };

            return dotBallCommentaries[random.nextInt(dotBallCommentaries.length)];
        } else if (runs == 4) {
            String[] boundaryCommentaries = {
                    "FOUR! Beautifully played by " + batsman + "!",
                    "Shot! " + batsman + " finds the boundary with a lovely drive!",
                    "Crashed away for FOUR by " + batsman + "!",
                    "Superb timing by " + batsman + ", that races away for FOUR!"
            };

            return boundaryCommentaries[random.nextInt(boundaryCommentaries.length)];
        } else if (runs == 6) {
            String[] sixCommentaries = {
                    "SIX! Massive hit by " + batsman + "!",
                    batsman + " launches that into the crowd! SIX!",
                    "Over the rope! " + batsman + " with a huge SIX!",
                    "What a shot! " + batsman + " clears the boundary with ease for SIX!"
            };

            return sixCommentaries[random.nextInt(sixCommentaries.length)];
        } else {
            String[] runCommentaries = {
                    batsman + " takes " + runs + " run" + (runs > 1 ? "s" : "") + ".",
                    "Quick running between the wickets, " + runs + " to " + batsman + ".",
                    runs + " run" + (runs > 1 ? "s" : "") + " added by " + batsman + ".",
                    "Good cricket, " + batsman + " picks up " + runs + "."
            };

            return runCommentaries[random.nextInt(runCommentaries.length)];
        }
    }

    /**
     * Check if generation is currently active
     */
    public boolean isGenerating() {
        return isGenerating;
    }

    /**
     * Get the total number of balls generated
     */
    public int getBallsGenerated() {
        return ballsGenerated;
    }

    /**
     * Get the total runs generated
     */
    public int getRunsGenerated() {
        return runsGenerated;
    }

    /**
     * Get the total wickets generated
     */
    public int getWicketsGenerated() {
        return wicketsGenerated;
    }
}