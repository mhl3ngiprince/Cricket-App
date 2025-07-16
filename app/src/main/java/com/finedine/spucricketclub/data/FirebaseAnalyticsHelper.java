package com.finedine.spucricketclub.data;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to handle Firebase Analytics and real-time analytics tracking
 */
public class FirebaseAnalyticsHelper {
    private static final String TAG = "FirebaseAnalytics";

    private static FirebaseAnalyticsHelper instance;

    private final FirebaseAnalytics firebaseAnalytics;
    private final DatabaseReference analyticsRef;
    private final String deviceId;

    // Private constructor for singleton pattern
    private FirebaseAnalyticsHelper(Context context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        analyticsRef = FirebaseDatabase.getInstance().getReference("analytics");

        // Generate a random device ID for anonymous tracking
        deviceId = java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    // Get singleton instance
    public static synchronized FirebaseAnalyticsHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAnalyticsHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Track a match start event
     */
    public void trackMatchStarted(Match match) {
        // Log Firebase Analytics event
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, match.getId());
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, match.getMatchName());
        params.putString("match_type", match.getMatchType().toString());
        params.putString("team_a", match.getTeamBatting().getTeamName());
        params.putString("team_b", match.getTeamBowling().getTeamName());
        firebaseAnalytics.logEvent("match_started", params);

        // Log to Realtime Database for custom analytics dashboard
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("match_id", match.getId());
        matchData.put("match_name", match.getMatchName());
        matchData.put("match_type", match.getMatchType().toString());
        matchData.put("team_a", match.getTeamBatting().getTeamName());
        matchData.put("team_b", match.getTeamBowling().getTeamName());
        matchData.put("timestamp", ServerValue.TIMESTAMP);
        matchData.put("device_id", deviceId);

        analyticsRef.child("matches").child(match.getId()).child("start").setValue(matchData);
    }

    /**
     * Track a match end event
     */
    public void trackMatchCompleted(Match match) {
        // Log Firebase Analytics event
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, match.getId());
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, match.getMatchName());
        params.putString("match_type", match.getMatchType().toString());
        params.putInt("innings_count", match.getInnings().size());

        if (match.getInnings().size() >= 2) {
            int firstInningsScore = match.getInnings().get(0).getTotalScore();
            int secondInningsScore = match.getInnings().get(1).getTotalScore();
            params.putInt("first_innings_score", firstInningsScore);
            params.putInt("second_innings_score", secondInningsScore);
            params.putInt("winning_margin", Math.abs(firstInningsScore - secondInningsScore));
        }

        firebaseAnalytics.logEvent("match_completed", params);

        // Log to Realtime Database for custom analytics dashboard
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("match_id", match.getId());
        matchData.put("match_name", match.getMatchName());
        matchData.put("innings_count", match.getInnings().size());
        matchData.put("timestamp", ServerValue.TIMESTAMP);
        matchData.put("device_id", deviceId);

        if (match.getInnings().size() >= 2) {
            int firstInningsScore = match.getInnings().get(0).getTotalScore();
            int secondInningsScore = match.getInnings().get(1).getTotalScore();
            matchData.put("first_innings_score", firstInningsScore);
            matchData.put("second_innings_score", secondInningsScore);
            matchData.put("winning_margin", Math.abs(firstInningsScore - secondInningsScore));
        }

        analyticsRef.child("matches").child(match.getId()).child("end").setValue(matchData);
    }

    /**
     * Track a milestone event (century, half-century, etc.)
     */
    public void trackPlayerMilestone(Player player, String milestone, int value) {
        // Log Firebase Analytics event
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, player.getId());
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, player.getName());
        params.putString("milestone_type", milestone);
        params.putInt("milestone_value", value);
        firebaseAnalytics.logEvent("player_milestone", params);

        // Log to Realtime Database for custom analytics dashboard
        Map<String, Object> milestoneData = new HashMap<>();
        milestoneData.put("player_id", player.getId());
        milestoneData.put("player_name", player.getName());
        milestoneData.put("milestone_type", milestone);
        milestoneData.put("milestone_value", value);
        milestoneData.put("timestamp", ServerValue.TIMESTAMP);
        milestoneData.put("device_id", deviceId);

        analyticsRef.child("milestones").push().setValue(milestoneData);
    }

    /**
     * Track face recognition event
     */
    public void trackFaceRecognition(Player player, float confidence) {
        // Log Firebase Analytics event
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, player.getId());
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, player.getName());
        params.putFloat("recognition_confidence", confidence);
        firebaseAnalytics.logEvent("face_recognition", params);

        // Log to Realtime Database for custom analytics dashboard
        Map<String, Object> recognitionData = new HashMap<>();
        recognitionData.put("player_id", player.getId());
        recognitionData.put("player_name", player.getName());
        recognitionData.put("confidence", confidence);
        recognitionData.put("timestamp", ServerValue.TIMESTAMP);
        recognitionData.put("device_id", deviceId);

        analyticsRef.child("recognitions").push().setValue(recognitionData);
    }

    /**
     * Track app performance metrics
     */
    public void trackPerformanceMetric(String metricName, long value, String category) {
        // Log Firebase Analytics event
        Bundle params = new Bundle();
        params.putString("metric_name", metricName);
        params.putLong("metric_value", value);
        params.putString("metric_category", category);
        firebaseAnalytics.logEvent("performance_metric", params);

        // Log to Realtime Database for custom analytics dashboard
        Map<String, Object> metricData = new HashMap<>();
        metricData.put("metric_name", metricName);
        metricData.put("metric_value", value);
        metricData.put("metric_category", category);
        metricData.put("timestamp", ServerValue.TIMESTAMP);
        metricData.put("device_id", deviceId);

        analyticsRef.child("performance").push().setValue(metricData);
    }

    /**
     * Get aggregated analytics data for a specific metric
     */
    public void getAggregatedAnalytics(String metricPath, final AnalyticsCallback<Long> callback) {
        analyticsRef.child(metricPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                callback.onSuccess(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching analytics data", error.toException());
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Callback interface for analytics results
     */
    public interface AnalyticsCallback<T> {
        void onSuccess(T result);

        void onError(String errorMessage);
    }
}