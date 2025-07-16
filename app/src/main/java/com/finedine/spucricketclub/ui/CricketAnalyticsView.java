package com.finedine.spucricketclub.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.finedine.spucricketclub.R;
import com.finedine.spucricketclub.analytics.CricketAnalyticsEngine;
import com.finedine.spucricketclub.models.PredictionResult;
import com.finedine.spucricketclub.models.WinProbability;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom view for displaying advanced cricket analytics with charts and visualizations
 */
public class CricketAnalyticsView extends LinearLayout {

    private CricketAnalyticsEngine analyticsEngine;

    // UI elements
    private PieChart winProbabilityChart;
    private BarChart projectionChart;
    private LineChart runRateChart;
    private LinearLayout insightsList;

    // Analytics labels
    private TextView winProbabilityLabel;
    private TextView projectionLabel;
    private TextView matchStatusLabel;

    public CricketAnalyticsView(Context context) {
        super(context);
        init(context);
    }

    public CricketAnalyticsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CricketAnalyticsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);

        // Inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_cricket_analytics, this, true);

        // Initialize analytics engine
        analyticsEngine = CricketAnalyticsEngine.getInstance(context);

        // Get chart views
        winProbabilityChart = view.findViewById(R.id.win_probability_chart);
        projectionChart = view.findViewById(R.id.projection_chart);
        runRateChart = view.findViewById(R.id.run_rate_chart);
        insightsList = view.findViewById(R.id.insights_list);

        // Get labels
        winProbabilityLabel = view.findViewById(R.id.win_probability_label);
        projectionLabel = view.findViewById(R.id.projection_label);
        matchStatusLabel = view.findViewById(R.id.match_status_label);

        // Set up charts
        setupWinProbabilityChart();
        setupProjectionChart();
        setupRunRateChart();
    }

    /**
     * Configure the win probability pie chart
     */
    private void setupWinProbabilityChart() {
        winProbabilityChart.getDescription().setEnabled(false);
        winProbabilityChart.setCenterText("Win %");
        winProbabilityChart.setHoleRadius(40f);
        winProbabilityChart.setTransparentCircleRadius(45f);
        winProbabilityChart.setDrawEntryLabels(false);

        Legend legend = winProbabilityChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    /**
     * Configure the score projection bar chart
     */
    private void setupProjectionChart() {
        projectionChart.getDescription().setEnabled(false);
        projectionChart.setFitBars(true);
        projectionChart.setDrawValueAboveBar(true);

        XAxis xAxis = projectionChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = projectionChart.getAxisLeft();
        leftAxis.setGranularity(10f);
        leftAxis.setAxisMinimum(0f);

        projectionChart.getAxisRight().setEnabled(false);
    }

    /**
     * Configure the run rate line chart
     */
    private void setupRunRateChart() {
        runRateChart.getDescription().setEnabled(false);
        runRateChart.setDragEnabled(true);
        runRateChart.setScaleEnabled(false);

        XAxis xAxis = runRateChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = runRateChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        runRateChart.getAxisRight().setEnabled(false);
    }

    /**
     * Update the analytics display with latest data
     */
    public void updateAnalytics() {
        updateWinProbability();
        updateProjection();
        updateRunRateChart();
        updateInsights();
    }

    /**
     * Update the win probability chart
     */
    private void updateWinProbability() {
        WinProbability winProb = analyticsEngine.getWinProbability();
        if (winProb == null) return;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(winProb.getTeam1Probability() * 100f, winProb.getTeam1Name()));
        entries.add(new PieEntry(winProb.getTeam2Probability() * 100f, winProb.getTeam2Name()));

        PieDataSet dataSet = new PieDataSet(entries, "Win Probability");
        dataSet.setColors(new int[]{Color.rgb(67, 160, 71), Color.rgb(66, 165, 245)});
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(winProbabilityChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        winProbabilityChart.setData(data);
        winProbabilityChart.invalidate();

        // Update label
        winProbabilityLabel.setText(String.format("Winning Chance: %s: %s, %s: %s",
                winProb.getTeam1Name(), winProb.getTeam1ProbabilityPercent(),
                winProb.getTeam2Name(), winProb.getTeam2ProbabilityPercent()));

        // Update match status
        if (winProb.getMatchCloseness() > 0.8f) {
            matchStatusLabel.setText("Match Status: Very Close");
            matchStatusLabel.setTextColor(Color.RED);
        } else if (winProb.getMatchCloseness() > 0.6f) {
            matchStatusLabel.setText("Match Status: Competitive");
            matchStatusLabel.setTextColor(Color.rgb(255, 152, 0)); // Orange
        } else {
            matchStatusLabel.setText("Match Status: One-sided");
            matchStatusLabel.setTextColor(Color.rgb(3, 169, 244)); // Light blue
        }
    }

    /**
     * Update the score projection chart
     */
    private void updateProjection() {
        PredictionResult prediction = analyticsEngine.getProjectedScore();
        if (prediction == null) return;

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, prediction.getLowEstimate()));
        entries.add(new BarEntry(1f, prediction.getMidEstimate()));
        entries.add(new BarEntry(2f, prediction.getHighEstimate()));

        BarDataSet dataSet = new BarDataSet(entries, "Score Projection");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);
        data.setValueTextSize(12f);

        projectionChart.setData(data);
        projectionChart.invalidate();

        // Update label
        projectionLabel.setText("Projected Score: " + prediction.getFormattedPrediction());
    }

    /**
     * Update the run rate chart with recent data
     */
    private void updateRunRateChart() {
        // This would use real data from the analytics engine
        // For now, we'll use sample data
        List<Entry> runRateEntries = new ArrayList<>();
        List<Entry> reqRunRateEntries = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            // Sample run rates
            runRateEntries.add(new Entry(i, 5 + (float) (Math.random() * 2)));
            reqRunRateEntries.add(new Entry(i, 7 + (float) (Math.random())));
        }

        LineDataSet runRateDataSet = new LineDataSet(runRateEntries, "Current RR");
        runRateDataSet.setColor(Color.GREEN);
        runRateDataSet.setCircleColor(Color.GREEN);
        runRateDataSet.setLineWidth(2f);

        LineDataSet reqRunRateDataSet = new LineDataSet(reqRunRateEntries, "Required RR");
        reqRunRateDataSet.setColor(Color.RED);
        reqRunRateDataSet.setCircleColor(Color.RED);
        reqRunRateDataSet.setLineWidth(2f);

        LineData lineData = new LineData(runRateDataSet, reqRunRateDataSet);
        runRateChart.setData(lineData);
        runRateChart.invalidate();
    }

    /**
     * Update the tactical insights section
     */
    private void updateInsights() {
        insightsList.removeAllViews();
        List<String> insights = analyticsEngine.getTacticalInsights();

        for (String insight : insights) {
            TextView textView = new TextView(getContext());
            textView.setText("• " + insight);
            textView.setPadding(8, 4, 8, 4);
            textView.setTextColor(Color.BLACK);
            insightsList.addView(textView);
        }

        if (insights.isEmpty()) {
            TextView textView = new TextView(getContext());
            textView.setText("No tactical insights available");
            textView.setPadding(8, 4, 8, 4);
            textView.setTextColor(Color.GRAY);
            insightsList.addView(textView);
        }
    }

    /**
     * Update the player impact scores display
     */
    public void updatePlayerImpact(LinearLayout playerImpactContainer) {
        playerImpactContainer.removeAllViews();
        Map<String, Float> impactScores = analyticsEngine.getAllPlayerImpactScores();

        for (Map.Entry<String, Float> entry : impactScores.entrySet()) {
            String playerId = entry.getKey();
            float score = entry.getValue();

            // In a real app, you'd get player name from the ID
            String playerName = "Player " + playerId.substring(0, 4);

            TextView textView = new TextView(getContext());
            textView.setText(String.format("%s: Impact %.1f", playerName, score));
            textView.setPadding(8, 4, 8, 4);

            // Color based on impact score
            if (score > 50) {
                textView.setTextColor(Color.rgb(67, 160, 71)); // Green
            } else if (score > 25) {
                textView.setTextColor(Color.rgb(66, 165, 245)); // Blue
            } else {
                textView.setTextColor(Color.GRAY);
            }

            playerImpactContainer.addView(textView);
        }
    }
}