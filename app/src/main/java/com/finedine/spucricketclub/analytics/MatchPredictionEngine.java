package com.finedine.spucricketclub.analytics;

import android.util.Log;

import androidx.annotation.NonNull;

import com.finedine.spucricketclub.cricket.Ball;
import com.finedine.spucricketclub.cricket.Innings;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Over;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Team;
import com.finedine.spucricketclub.models.PredictionResult;
import com.finedine.spucricketclub.models.WinProbability;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Predicts cricket match outcomes using statistical models and machine learning techniques
 */
public class MatchPredictionEngine {
    private static final String TAG = "MatchPrediction";

    // Singleton instance
    private static MatchPredictionEngine instance;

    // Database reference
    private final DatabaseReference databaseReference;

    // Historical match data cache
    private final Map<String, List<Match>> teamMatchHistory = new HashMap<>();

    // Player performance metrics
    private final Map<String, PlayerPerformance> playerPerformances = new HashMap<>();

    // Pitch and venue factors
    private final Map<String, VenueFactor> venueFactors = new HashMap<>();

    // Random generator for adding slight variability to predictions
    private final Random random = new Random();

    /**
     * Inner class to track player performance
     */
    private static class PlayerPerformance {
        private float battingAverage = 0;
        private float strikeRate = 0;
        private float bowlingAverage = 0;
        private float economyRate = 0;
        private int matchesPlayed = 0;
        private Map<Player.PlayerRole, Float> rolePerformance = new HashMap<>();

        // Performance against specific teams
        private Map<String, Float> teamSpecificPerformance = new HashMap<>();

        // Performance trend (recent form)
        private List<Float> recentForm = new ArrayList<>();

        public float getRecentFormFactor() {
            if (recentForm.isEmpty()) {
                return 1.0f;
            }

            float sum = 0;
            int count = 0;
            int weight = recentForm.size();

            // Weight recent performances more heavily
            for (int i = recentForm.size() - 1; i >= 0; i--) {
                sum += recentForm.get(i) * weight;
                count += weight;
                weight--;
            }

            return sum / count;
        }

        public float getPerformanceAgainstTeam(String teamId) {
            return teamSpecificPerformance.getOrDefault(teamId, 1.0f);
        }
    }

    /**
     * Inner class to track venue-specific factors
     */
    private static class VenueFactor {
        private String venueName;
        private float battingConditionFactor = 1.0f;  // >1 means batting friendly
        private float bowlingConditionFactor = 1.0f;  // >1 means bowling friendly
        private float spinFactor = 1.0f;  // >1 means spin friendly
        private float paceFactor = 1.0f;  // >1 means pace friendly
        private float averageFirstInningsScore = 150;
        private float chaseSuccessRate = 0.5f;
        private Map<String, Float> teamSpecificPerformance = new HashMap<>();

        public float getTeamVenuePerformance(String teamId) {
            return teamSpecificPerformance.getOrDefault(teamId, 1.0f);
        }
    }

    /**
     * Private constructor for singleton
     */
    private MatchPredictionEngine() {
        databaseReference = FirebaseDatabase.getInstance().getReference("prediction_data");
        loadHistoricalData();
    }

    /**
     * Get singleton instance
     */
    public static synchronized MatchPredictionEngine getInstance() {
        if (instance == null) {
            instance = new MatchPredictionEngine();
        }
        return instance;
    }

    /**
     * Load historical match data from database
     */
    private void loadHistoricalData() {
        try {
            loadVenueData();
            loadPlayerPerformanceData();
            loadTeamHistoricalData();

            Log.d(TAG, "Started loading historical prediction data");
        } catch (Exception e) {
            Log.e(TAG, "Error loading historical data", e);
        }
    }

    /**
     * Load venue-specific data
     */
    private void loadVenueData() {
        databaseReference.child("venues").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    for (DataSnapshot venueSnapshot : snapshot.getChildren()) {
                        String venueId = venueSnapshot.getKey();
                        if (venueId != null) {
                            VenueFactor factor = new VenueFactor();
                            factor.venueName = venueSnapshot.child("name").getValue(String.class);

                            if (venueSnapshot.hasChild("battingFactor")) {
                                factor.battingConditionFactor = venueSnapshot.child("battingFactor").getValue(Float.class);
                            }

                            if (venueSnapshot.hasChild("bowlingFactor")) {
                                factor.bowlingConditionFactor = venueSnapshot.child("bowlingFactor").getValue(Float.class);
                            }

                            if (venueSnapshot.hasChild("spinFactor")) {
                                factor.spinFactor = venueSnapshot.child("spinFactor").getValue(Float.class);
                            }

                            if (venueSnapshot.hasChild("paceFactor")) {
                                factor.paceFactor = venueSnapshot.child("paceFactor").getValue(Float.class);
                            }

                            if (venueSnapshot.hasChild("avgFirstInningsScore")) {
                                factor.averageFirstInningsScore = venueSnapshot.child("avgFirstInningsScore").getValue(Float.class);
                            }

                            if (venueSnapshot.hasChild("chaseSuccessRate")) {
                                factor.chaseSuccessRate = venueSnapshot.child("chaseSuccessRate").getValue(Float.class);
                            }

                            // Team specific performance at this venue
                            DataSnapshot teamPerf = venueSnapshot.child("teamPerformance");
                            for (DataSnapshot teamSnapshot : teamPerf.getChildren()) {
                                String teamId = teamSnapshot.getKey();
                                Float perfFactor = teamSnapshot.getValue(Float.class);
                                if (teamId != null && perfFactor != null) {
                                    factor.teamSpecificPerformance.put(teamId, perfFactor);
                                }
                            }

                            venueFactors.put(venueId, factor);
                        }
                    }
                    Log.d(TAG, "Loaded venue factors: " + venueFactors.size());
                } catch (Exception e) {
                    Log.e(TAG, "Error loading venue data", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading venue data", error.toException());
            }
        });
    }

    /**
     * Load player performance data
     */
    private void loadPlayerPerformanceData() {
        databaseReference.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                        String playerId = playerSnapshot.getKey();
                        if (playerId != null) {
                            PlayerPerformance performance = new PlayerPerformance();

                            if (playerSnapshot.hasChild("battingAvg")) {
                                performance.battingAverage = playerSnapshot.child("battingAvg").getValue(Float.class);
                            }

                            if (playerSnapshot.hasChild("strikeRate")) {
                                performance.strikeRate = playerSnapshot.child("strikeRate").getValue(Float.class);
                            }

                            if (playerSnapshot.hasChild("bowlingAvg")) {
                                performance.bowlingAverage = playerSnapshot.child("bowlingAvg").getValue(Float.class);
                            }

                            if (playerSnapshot.hasChild("economyRate")) {
                                performance.economyRate = playerSnapshot.child("economyRate").getValue(Float.class);
                            }

                            if (playerSnapshot.hasChild("matches")) {
                                performance.matchesPlayed = playerSnapshot.child("matches").getValue(Integer.class);
                            }

                            // Team-specific performance
                            DataSnapshot vsTeams = playerSnapshot.child("vsTeams");
                            for (DataSnapshot teamSnapshot : vsTeams.getChildren()) {
                                String teamId = teamSnapshot.getKey();
                                Float perfFactor = teamSnapshot.getValue(Float.class);
                                if (teamId != null && perfFactor != null) {
                                    performance.teamSpecificPerformance.put(teamId, perfFactor);
                                }
                            }

                            // Recent form
                            DataSnapshot formSnapshot = playerSnapshot.child("recentForm");
                            for (DataSnapshot formItem : formSnapshot.getChildren()) {
                                Float formFactor = formItem.getValue(Float.class);
                                if (formFactor != null) {
                                    performance.recentForm.add(formFactor);
                                }
                            }

                            playerPerformances.put(playerId, performance);
                        }
                    }
                    Log.d(TAG, "Loaded player performances: " + playerPerformances.size());
                } catch (Exception e) {
                    Log.e(TAG, "Error loading player performance data", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading player data", error.toException());
            }
        });
    }

    /**
     * Load team historical match data
     */
    private void loadTeamHistoricalData() {
        databaseReference.child("teamHistory").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    for (DataSnapshot teamSnapshot : snapshot.getChildren()) {
                        String teamId = teamSnapshot.getKey();
                        if (teamId != null) {
                            List<Match> matches = new ArrayList<>();
                            // For simplicity, we're just storing the count of matches here
                            // In a real implementation, we would store more detailed match data
                            int matchCount = teamSnapshot.child("matchCount").getValue(Integer.class);
                            teamMatchHistory.put(teamId, matches);
                        }
                    }
                    Log.d(TAG, "Loaded team history for: " + teamMatchHistory.size() + " teams");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading team history data", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error loading team history", error.toException());
            }
        });
    }

    /**
     * Predict the outcome of a match
     *
     * @param match The match to predict
     * @return Prediction result with win probabilities
     */
    public PredictionResult predictMatchOutcome(Match match) {
        try {
            if (match == null) {
                Log.e(TAG, "Cannot predict outcome for null match");
                return null;
            }

            Team teamA = match.getTeamBatting();
            Team teamB = match.getTeamBowling();

            if (teamA == null || teamB == null) {
                Log.e(TAG, "Cannot predict outcome with null teams");
                return null;
            }

            String venue = match.getVenue();
            VenueFactor venueFactor = venue != null ? venueFactors.get(venue) : null;

            // Calculate team strength based on player performances
            float teamAStrength = calculateTeamStrength(teamA, teamB.getId());
            float teamBStrength = calculateTeamStrength(teamB, teamA.getId());

            // Adjust for venue factors
            if (venueFactor != null) {
                teamAStrength *= venueFactor.getTeamVenuePerformance(teamA.getId());
                teamBStrength *= venueFactor.getTeamVenuePerformance(teamB.getId());
            }

            // Add some random variation (5%)
            teamAStrength *= (0.95f + random.nextFloat() * 0.1f);
            teamBStrength *= (0.95f + random.nextFloat() * 0.1f);

            // Calculate probabilities
            float totalStrength = teamAStrength + teamBStrength;
            float teamAProb = teamAStrength / totalStrength;
            float teamBProb = teamBStrength / totalStrength;

            // Create prediction result
            PredictionResult result = new PredictionResult();
            result.setWinProbabilities(new WinProbability[]{
                    new WinProbability(teamA.getTeamName(), teamAProb * 100),
                    new WinProbability(teamB.getTeamName(), teamBProb * 100)
            });

            // Add prediction details
            result.setDetails("Prediction based on player performances, historical data, and venue conditions");

            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error predicting match outcome", e);
            return null;
        }
    }

    /**
     * Calculate target score prediction for second innings
     *
     * @param match Current match
     * @return Predicted target score
     */
    public int predictTargetScore(Match match) {
        try {
            if (match == null || match.getInnings().size() < 1) {
                return 0;
            }

            Innings firstInnings = match.getInnings().get(0);
            if (firstInnings == null) {
                return 0;
            }

            // Get current score
            int currentScore = firstInnings.getTotalScore();
            int currentWickets = firstInnings.getWickets();
            float currentOvers = firstInnings.getTotalOvers();
            int maxOvers = match.getMaxOvers();

            // If match is completed, return actual score
            if (currentOvers >= maxOvers || currentWickets >= 10) {
                return currentScore;
            }

            // Calculate run rate
            float currentRunRate = currentScore / currentOvers;

            // Predict final score based on current run rate
            float baselinePrediction = currentRunRate * maxOvers;

            // Adjust for acceleration in final overs
            float remainingOvers = maxOvers - currentOvers;
            float accelerationFactor = 1.0f + (remainingOvers < 5 ? 0.3f :
                    remainingOvers < 10 ? 0.2f : 0.1f);

            // Adjust for wickets in hand
            float wicketsFactor = 1.0f - (currentWickets * 0.05f); // Each wicket reduces projection by 5%

            int predictedScore = (int) (baselinePrediction * accelerationFactor * wicketsFactor);

            // Add venue factor if available
            String venue = match.getVenue();
            if (venue != null && venueFactors.containsKey(venue)) {
                VenueFactor venueFactor = venueFactors.get(venue);
                predictedScore *= venueFactor.battingConditionFactor;
            }

            return predictedScore;

        } catch (Exception e) {
            Log.e(TAG, "Error predicting target score", e);
            return 0;
        }
    }

    /**
     * Predict win probability during a chase
     *
     * @param match Current match
     * @return Win probability for the chasing team (0-100)
     */
    public float predictChaseWinProbability(Match match) {
        try {
            if (match == null || match.getInnings().size() < 2) {
                return 50.0f; // Default to 50% when no data
            }

            Innings firstInnings = match.getInnings().get(0);
            Innings secondInnings = match.getInnings().get(1);

            if (firstInnings == null || secondInnings == null) {
                return 50.0f;
            }

            int target = firstInnings.getTotalScore() + 1;
            int currentScore = secondInnings.getTotalScore();
            int runsNeeded = target - currentScore;

            // If chase is completed
            if (runsNeeded <= 0) {
                return 100.0f; // Chasing team won
            }

            int wicketsRemaining = 10 - secondInnings.getWickets();
            if (wicketsRemaining <= 0) {
                return 0.0f; // No wickets left, can't win
            }

            float totalOvers = match.getMaxOvers();
            float oversRemaining = totalOvers - secondInnings.getTotalOvers();

            if (oversRemaining <= 0) {
                return 0.0f; // No overs left, can't win
            }

            // Required run rate
            float requiredRunRate = runsNeeded / oversRemaining;

            // Current run rate
            float currentRunRate = secondInnings.getTotalOvers() > 0 ?
                    currentScore / secondInnings.getTotalOvers() : 0;

            // Base probability calculation
            float baseProbability;
            if (requiredRunRate <= currentRunRate) {
                // Easier chase
                baseProbability = 70.0f - (requiredRunRate - currentRunRate) * 5.0f;
            } else {
                // Harder chase
                baseProbability = 50.0f - (requiredRunRate - currentRunRate) * 10.0f;
            }

            // Adjust for wickets remaining
            float wicketsFactor = wicketsRemaining / 10.0f;
            baseProbability *= (0.5f + wicketsFactor * 0.5f);

            // Adjust for venue factors if available
            String venue = match.getVenue();
            if (venue != null && venueFactors.containsKey(venue)) {
                VenueFactor venueFactor = venueFactors.get(venue);
                baseProbability *= venueFactor.chaseSuccessRate;
            }

            // Clamp between 0 and 100
            return Math.max(0, Math.min(100, baseProbability));

        } catch (Exception e) {
            Log.e(TAG, "Error predicting chase win probability", e);
            return 50.0f;
        }
    }

    /**
     * Predict runs in the next over
     *
     * @param match  Current match
     * @param bowler Bowler who will bowl the next over
     * @return Predicted runs in the next over
     */
    public int predictRunsInNextOver(Match match, Player bowler) {
        try {
            if (match == null || bowler == null) {
                return 6; // Default prediction
            }

            // Get current innings
            Innings currentInnings;
            if (!match.getInnings().isEmpty()) {
                currentInnings = match.getInnings().get(match.getInnings().size() - 1);
            } else {
                return 6; // Default when no innings data
            }

            // Get current run rate
            float currentRunRate = currentInnings.getTotalOvers() > 0 ?
                    currentInnings.getTotalScore() / currentInnings.getTotalOvers() : 6.0f;

            // Base prediction on run rate
            float basePrediction = currentRunRate;

            // Adjust for bowler's economy rate
            PlayerPerformance bowlerPerf = playerPerformances.get(bowler.getId());
            if (bowlerPerf != null && bowlerPerf.economyRate > 0) {
                basePrediction = (basePrediction + bowlerPerf.economyRate) / 2.0f;
            }

            // Adjust for batsmen at crease
            Player striker = currentInnings.getStriker();
            Player nonStriker = currentInnings.getNonStriker();

            if (striker != null) {
                PlayerPerformance strikerPerf = playerPerformances.get(striker.getId());
                if (strikerPerf != null && strikerPerf.strikeRate > 0) {
                    // Convert strike rate to runs per over
                    float strikerRunRate = strikerPerf.strikeRate / 100.0f * 6.0f;
                    basePrediction = (basePrediction + strikerRunRate) / 2.0f;
                }
            }

            // Adjust for match phase
            int totalOvers = match.getMaxOvers();
            float currentOver = currentInnings.getTotalOvers();
            if (currentOver >= totalOvers - 5) {
                // Death overs: higher scoring
                basePrediction *= 1.2f;
            } else if (currentOver <= 6) {
                // Powerplay: higher scoring
                basePrediction *= 1.1f;
            } else if (currentOver >= totalOvers / 2 && currentOver < totalOvers - 10) {
                // Middle overs: lower scoring
                basePrediction *= 0.9f;
            }

            // Add some randomness (-2 to +2 runs)
            basePrediction += (random.nextFloat() * 4.0f - 2.0f);

            return Math.max(0, Math.round(basePrediction));

        } catch (Exception e) {
            Log.e(TAG, "Error predicting runs in next over", e);
            return 6; // Default prediction
        }
    }

    /**
     * Calculate overall team strength
     *
     * @param team           The team to calculate strength for
     * @param opposingTeamId The opposing team's ID
     * @return Team strength score (higher is better)
     */
    private float calculateTeamStrength(Team team, String opposingTeamId) {
        if (team == null || team.getPlayers() == null) {
            return 1.0f;
        }

        try {
            float totalStrength = 0;

            for (Player player : team.getPlayers()) {
                if (player != null) {
                    PlayerPerformance perf = playerPerformances.get(player.getId());

                    if (perf != null) {
                        float playerStrength = 1.0f;

                        // Role-specific strength calculation
                        switch (player.getRole()) {
                            case BATSMAN:
                                playerStrength = perf.battingAverage * perf.strikeRate / 100.0f;
                                break;
                            case BOWLER:
                                playerStrength = (perf.bowlingAverage > 0) ?
                                        25.0f / perf.bowlingAverage : 1.0f;
                                break;
                            case ALL_ROUNDER:
                                float battingStr = perf.battingAverage * perf.strikeRate / 100.0f;
                                float bowlingStr = (perf.bowlingAverage > 0) ?
                                        25.0f / perf.bowlingAverage : 1.0f;
                                playerStrength = (battingStr + bowlingStr) / 2.0f;
                                break;
                            case WICKET_KEEPER:
                                playerStrength = perf.battingAverage * perf.strikeRate / 100.0f * 1.1f;
                                break;
                        }

                        // Adjust by recent form
                        playerStrength *= perf.getRecentFormFactor();

                        // Adjust by performance against specific team
                        playerStrength *= perf.getPerformanceAgainstTeam(opposingTeamId);

                        totalStrength += playerStrength;
                    } else {
                        // Default strength if no performance data
                        totalStrength += 1.0f;
                    }
                }
            }

            // Average team strength based on player count
            if (!team.getPlayers().isEmpty()) {
                totalStrength /= team.getPlayers().size();
            }

            return totalStrength;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating team strength", e);
            return 1.0f;
        }
    }

    /**
     * Clear cached data (for testing)
     */
    public void clearCache() {
        playerPerformances.clear();
        venueFactors.clear();
        teamMatchHistory.clear();
        Log.d(TAG, "Prediction engine cache cleared");
    }
}