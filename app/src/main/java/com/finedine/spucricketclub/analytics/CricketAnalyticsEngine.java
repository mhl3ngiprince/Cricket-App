package com.finedine.spucricketclub.analytics;

import android.content.Context;
import android.util.Log;

import com.finedine.spucricketclub.cricket.Ball;
import com.finedine.spucricketclub.cricket.Innings;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Over;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.models.PredictionResult;
import com.finedine.spucricketclub.models.WinProbability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Advanced cricket analytics engine that provides real-time insights, projections,
 * and strategy recommendations based on match data.
 */
public class CricketAnalyticsEngine {
    private static final String TAG = "CricketAnalytics";

    // Singleton instance
    private static CricketAnalyticsEngine instance;

    // Context
    private final Context context;

    // Match data
    private Match currentMatch;

    // Historical data for analytics
    private List<Ball> allBalls = new ArrayList<>();
    private Map<String, List<Integer>> playerScoreProgression = new HashMap<>();
    private Map<String, List<Integer>> bowlerPerformance = new HashMap<>();

    // Analytics results
    private WinProbability winProbability;
    private PredictionResult projectedScore;
    private List<String> tacticalInsights = new ArrayList<>();
    private Map<String, Float> playerImpactScores = new HashMap<>();

    // Private constructor for singleton
    private CricketAnalyticsEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Get singleton instance
     */
    public static synchronized CricketAnalyticsEngine getInstance(Context context) {
        if (instance == null) {
            instance = new CricketAnalyticsEngine(context);
        }
        return instance;
    }

    /**
     * Set the current match for analysis
     */
    public void setMatch(Match match) {
        this.currentMatch = match;
        resetAnalytics();
        Log.d(TAG, "Match set for analysis: " + match.getMatchName());
    }

    /**
     * Reset all analytics data
     */
    public void resetAnalytics() {
        allBalls.clear();
        playerScoreProgression.clear();
        bowlerPerformance.clear();
        tacticalInsights.clear();
        playerImpactScores.clear();
        Log.d(TAG, "Analytics data reset");
    }

    /**
     * Process a new ball for analytics
     */
    public void processBall(Ball ball) {
        if (currentMatch == null) {
            Log.w(TAG, "Cannot process ball, no match set");
            return;
        }

        // Add to all balls for analysis
        allBalls.add(ball);

        // Update player progression data
        updatePlayerProgressionData(ball);

        // Update bowler performance data
        updateBowlerPerformanceData(ball);

        // Run analytics
        runAnalytics();

        Log.d(TAG, "Processed ball: " + ball.getDisplayCharacter());
    }

    /**
     * Update player progression data with the new ball
     */
    private void updatePlayerProgressionData(Ball ball) {
        Player batsman = ball.getBatsman();
        if (batsman == null) return;

        String batsmanId = batsman.getId();
        if (!playerScoreProgression.containsKey(batsmanId)) {
            playerScoreProgression.put(batsmanId, new ArrayList<>());
        }

        List<Integer> progression = playerScoreProgression.get(batsmanId);
        int lastScore = progression.isEmpty() ? 0 : progression.get(progression.size() - 1);
        int newScore = lastScore + ball.getRunsScored();
        progression.add(newScore);
    }

    /**
     * Update bowler performance data with the new ball
     */
    private void updateBowlerPerformanceData(Ball ball) {
        Player bowler = ball.getBowler();
        if (bowler == null) return;

        String bowlerId = bowler.getId();
        if (!bowlerPerformance.containsKey(bowlerId)) {
            bowlerPerformance.put(bowlerId, new ArrayList<>());
        }

        List<Integer> performance = bowlerPerformance.get(bowlerId);
        performance.add(ball.getRunsScored());

        // Count wickets as -1 for simplicity
        if (ball.isWicket()) {
            performance.add(-1);
        }
    }

    /**
     * Run all analytics calculations
     */
    private void runAnalytics() {
        if (currentMatch == null || allBalls.isEmpty()) return;

        calculateWinProbability();
        projectFinalScore();
        generateTacticalInsights();
        calculatePlayerImpact();
    }

    /**
     * Calculate win probability based on current match state
     */
    private void calculateWinProbability() {
        try {
            Innings innings = currentMatch.getCurrentInnings();

            // Simple model for demonstration
            // In a real app, this would use machine learning with historical data
            float battingTeamProbability = 0.5f; // Start at 50%

            if (currentMatch.getInnings().size() > 1) {
                // Second innings - chasing target
                int targetScore = currentMatch.getTargetScore();
                int currentScore = innings.getTotalScore();
                int wicketsLost = innings.getWickets();
                float oversRemaining = currentMatch.getMaxOvers() - innings.getTotalOvers();
                int runsRequired = targetScore - currentScore;

                // Adjust probability based on run rate required
                float currentRunRate = currentScore / Math.max(0.1f, innings.getTotalOvers());
                float requiredRunRate = (runsRequired > 0) ? runsRequired / Math.max(0.1f, oversRemaining) : 0;
                float runRateFactor = currentRunRate / Math.max(0.1f, requiredRunRate);

                // Adjust for wickets - more wickets means higher probability
                float wicketsFactor = (10 - wicketsLost) / 10.0f;

                battingTeamProbability = 0.5f * runRateFactor * wicketsFactor;

                // Cap between 0.01 and 0.99
                battingTeamProbability = Math.min(0.99f, Math.max(0.01f, battingTeamProbability));
            } else {
                // First innings - compare to average first innings score
                // This is a simplified model
                int benchmarkScore = 150; // For T20
                int currentScore = innings.getTotalScore();
                int wicketsLost = innings.getWickets();
                float oversCompleted = innings.getTotalOvers();
                float oversTotal = currentMatch.getMaxOvers();

                if (oversTotal > 0 && oversCompleted > 0) {
                    float projectedScore = currentScore * (oversTotal / oversCompleted);
                    float scoreFactor = projectedScore / benchmarkScore;
                    float wicketsFactor = (10 - wicketsLost) / 10.0f;

                    battingTeamProbability = 0.5f * scoreFactor * wicketsFactor;

                    // Cap between 0.01 and 0.99
                    battingTeamProbability = Math.min(0.99f, Math.max(0.01f, battingTeamProbability));
                }
            }

            // Create win probability object
            winProbability = new WinProbability(
                    currentMatch.getTeamBatting().getTeamName(),
                    battingTeamProbability
            );
            // Set opposing team name
            winProbability.setTeam2Name(currentMatch.getTeamBowling().getTeamName());
        } catch (Exception e) {
            Log.e(TAG, "Error calculating win probability", e);
        }
    }

    /**
     * Project the final score based on current run rate and wickets
     */
    private void projectFinalScore() {
        try {
            Innings innings = currentMatch.getCurrentInnings();
            int currentScore = innings.getTotalScore();
            int wicketsLost = innings.getWickets();
            float oversCompleted = innings.getTotalOvers();
            float oversRemaining = currentMatch.getMaxOvers() - oversCompleted;

            // Base projection on current run rate
            float currentRunRate = oversCompleted > 0 ? currentScore / oversCompleted : 0;
            int linearProjection = currentScore + Math.round(currentRunRate * oversRemaining);

            // Adjust based on wickets left
            float wicketsRemaining = 10 - wicketsLost;
            float wicketsFactor = wicketsRemaining / 10.0f;
            int wicketAdjustedProjection;

            if (wicketsLost <= 2) {
                // Few wickets down, likely to accelerate
                wicketAdjustedProjection = Math.round(linearProjection * 1.2f);
            } else if (wicketsLost >= 7) {
                // Many wickets down, likely to decelerate
                wicketAdjustedProjection = Math.round(linearProjection * 0.8f);
            } else {
                wicketAdjustedProjection = linearProjection;
            }

            // Look at recent run scoring
            List<Ball> recentBalls = getLastNBalls(18); // Last 3 overs
            int recentRuns = 0;
            for (Ball ball : recentBalls) {
                recentRuns += ball.getRunsScored();
            }

            float recentRunRate = recentBalls.size() > 0 ? (recentRuns / (recentBalls.size() / 6.0f)) : 0;
            int recentFormProjection = currentScore + Math.round(recentRunRate * oversRemaining);

            // Combine projections
            int finalProjection = Math.round(0.6f * wicketAdjustedProjection + 0.4f * recentFormProjection);

            // Create range
            int lowProjection = Math.round(finalProjection * 0.9f);
            int highProjection = Math.round(finalProjection * 1.1f);

            projectedScore = new PredictionResult(lowProjection, finalProjection, highProjection);

            Log.d(TAG, String.format("Score projection: %d (%d-%d)", finalProjection, lowProjection, highProjection));
        } catch (Exception e) {
            Log.e(TAG, "Error projecting final score", e);
        }
    }

    /**
     * Generate tactical insights based on current match state
     */
    private void generateTacticalInsights() {
        try {
            tacticalInsights.clear();
            Innings innings = currentMatch.getCurrentInnings();
            Player striker = innings.getStriker();

            // Check batting player performance against current bowler type
            if (striker != null) {
                Player currentBowler = innings.getCurrentBowler();
                if (currentBowler != null) {
                    // Check striker's vulnerability to this bowler type
                    if (striker.getBattingStats().getRuns() > 30 && striker.calculateStrikeRate() > 130) {
                        tacticalInsights.add("Consider bowling yorkers to " + striker.getName()
                                + " who is accelerating");
                    }

                    // Suggest field placement
                    if (isKnownBoundaryHitter(striker)) {
                        tacticalInsights.add("Set deeper field for " + striker.getName()
                                + " who scores 65% runs in boundaries");
                    }
                }
            }

            // Analyze over patterns
            Over currentOver = innings.getCurrentOver();
            if (currentOver != null && currentOver.getBalls().size() >= 3) {
                int runInOver = 0;
                for (Ball ball : currentOver.getBalls()) {
                    runInOver += ball.getRunsScored();
                }

                if (runInOver >= 12 && currentOver.getBalls().size() <= 4) {
                    tacticalInsights.add("Consider damage control - this over already has "
                            + runInOver + " runs");
                }
            }

            // Death overs advice
            float oversRemaining = currentMatch.getMaxOvers() - innings.getTotalOvers();
            if (oversRemaining <= 5 && innings.getWickets() <= 5) {
                tacticalInsights.add("Prepare for aggressive batting in death overs");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating tactical insights", e);
        }
    }

    /**
     * Calculate impact scores for all players
     */
    private void calculatePlayerImpact() {
        try {
            playerImpactScores.clear();

            // In a real app this would use more sophisticated algorithms
            for (Player player : currentMatch.getTeamBatting().getPlayers()) {
                float battingImpact = calculateBattingImpact(player);
                playerImpactScores.put(player.getId(), battingImpact);
            }

            for (Player player : currentMatch.getTeamBowling().getPlayers()) {
                float bowlingImpact = calculateBowlingImpact(player);
                playerImpactScores.put(player.getId(), bowlingImpact);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating player impact", e);
        }
    }

    /**
     * Calculate batting impact score for a player
     */
    private float calculateBattingImpact(Player player) {
        // Simple impact calculation
        int runs = player.getBattingStats().getRuns();
        int balls = player.getBattingStats().getBallsFaced();
        float strikeRate = player.calculateStrikeRate();

        // Base impact is runs scored
        float impact = runs;

        // Adjust for strike rate
        if (strikeRate > 150) {
            impact *= 1.3f; // Bonus for high strike rate
        } else if (strikeRate < 100 && balls > 10) {
            impact *= 0.8f; // Penalty for low strike rate
        }

        // Context bonus - late innings runs worth more
        Innings innings = currentMatch.getCurrentInnings();
        if (innings.getTotalOvers() > currentMatch.getMaxOvers() * 0.7f) {
            impact *= 1.2f; // 20% bonus for death overs
        }

        return impact;
    }

    /**
     * Calculate bowling impact score for a player
     */
    private float calculateBowlingImpact(Player player) {
        int wickets = player.getBowlingStats().getWickets();
        int runs = player.getBowlingStats().getRunsConceded();
        int overs = player.getBowlingStats().getOvers();

        // No impact if hasn't bowled
        if (overs == 0) return 0;

        // Base impact is wickets taken
        float impact = wickets * 25;

        // Economy rate impact
        float economyRate = runs / (float) overs;
        if (economyRate < 6) {
            impact += 10 * (6 - economyRate); // Bonus for economy
        } else if (economyRate > 9) {
            impact -= 5 * (economyRate - 9); // Penalty for expensive
        }

        return Math.max(0, impact); // Ensure non-negative
    }

    /**
     * Get the last N balls bowled
     */
    private List<Ball> getLastNBalls(int n) {
        List<Ball> result = new ArrayList<>();
        int size = allBalls.size();

        for (int i = Math.max(0, size - n); i < size; i++) {
            result.add(allBalls.get(i));
        }

        return result;
    }

    /**
     * Check if a player is known for hitting boundaries
     */
    private boolean isKnownBoundaryHitter(Player player) {
        // In a real app this would check historical data
        // This is a simplified version
        int fours = player.getBattingStats().getFours();
        int sixes = player.getBattingStats().getSixes();
        int totalRuns = player.getBattingStats().getRuns();

        if (totalRuns == 0) return false;

        // Calculate percentage of runs from boundaries
        float boundaryPercentage = (float) ((fours * 4) + (sixes * 6)) / totalRuns;
        return boundaryPercentage > 0.6f; // 60% runs from boundaries
    }

    /**
     * Get win probability analysis
     */
    public WinProbability getWinProbability() {
        if (winProbability == null) {
            calculateWinProbability();
        }
        return winProbability;
    }

    /**
     * Get projected score analysis
     */
    public PredictionResult getProjectedScore() {
        if (projectedScore == null) {
            projectFinalScore();
        }
        return projectedScore;
    }

    /**
     * Get tactical insights
     */
    public List<String> getTacticalInsights() {
        return new ArrayList<>(tacticalInsights);
    }

    /**
     * Get impact score for a player
     */
    public float getPlayerImpactScore(String playerId) {
        return playerImpactScores.getOrDefault(playerId, 0f);
    }

    /**
     * Get all player impact scores
     */
    public Map<String, Float> getAllPlayerImpactScores() {
        return new HashMap<>(playerImpactScores);
    }
}
