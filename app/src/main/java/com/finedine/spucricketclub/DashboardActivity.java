package com.finedine.spucricketclub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.spucricketclub.adapters.RecentActivityAdapter;
import com.finedine.spucricketclub.adapters.TopPlayerAdapter;
import com.finedine.spucricketclub.cricket.CricketMatchService;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.data.PlayerDatabase;
import com.finedine.spucricketclub.models.ActivityItem;
import com.finedine.spucricketclub.models.PlayerStats;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main dashboard activity that serves as the app's home screen
 * Displays upcoming matches, quick actions, top performers and recent activity
 */
@androidx.camera.core.ExperimentalGetImage
public class DashboardActivity extends AppCompatActivity {

    private CricketMatchService cricketMatchService;
    private PlayerDatabase playerDatabase;

    private CardView teamManagementCard;
    private CardView playerRegistrationCard;
    private CardView analyticsCard;
    private CardView matchHistoryCard;
    private CardView pitchVisionCard;
    private CardView settingsCard;
    //private FloatingActionButton newMatchFab;
    private ExtendedFloatingActionButton newMatchFab;

    private RecyclerView topPlayersRecyclerView;
    private RecyclerView recentActivityRecyclerView;

    private TopPlayerAdapter topPlayerAdapter;
    private RecentActivityAdapter recentActivityAdapter;

    private List<PlayerStats> topPlayers = new ArrayList<>();
    private List<ActivityItem> activityItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("SPU Cricket Club");
        toolbar.setBackgroundColor(getResources().getColor(R.color.spu_blue));
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));

        // Initialize services
        cricketMatchService = CricketMatchService.getInstance(this);
        playerDatabase = PlayerDatabase.getInstance(this);

        // Initialize UI components
        initializeViews();
        setupClickListeners();

        // Load data
        loadTopPlayers();
        loadRecentActivity();
        setupUpcomingMatch();
    }

    private void initializeViews() {
        // Cards
        teamManagementCard = findViewById(R.id.card_team_management);
        playerRegistrationCard = findViewById(R.id.card_player_registration);
        analyticsCard = findViewById(R.id.card_analytics);
        matchHistoryCard = findViewById(R.id.card_match_history);
        pitchVisionCard = findViewById(R.id.card_pitch_vision);
        settingsCard = findViewById(R.id.card_settings);
        newMatchFab = findViewById(R.id.fab_new_match);
        // Defensive logging if any card is missing
        if (teamManagementCard == null) Log.e("Dashboard", "Team Management Card not found!");
        if (playerRegistrationCard == null)
            Log.e("Dashboard", "Player Registration Card not found!");
        if (analyticsCard == null) Log.e("Dashboard", "Analytics Card not found!");
        if (matchHistoryCard == null) Log.e("Dashboard", "Match History Card not found!");
        if (pitchVisionCard == null) Log.e("Dashboard", "Pitch Vision Card not found!");
        if (settingsCard == null) Log.e("Dashboard", "Settings Card not found!");
        if (newMatchFab == null) Log.e("Dashboard", "FAB New Match not found!");
        // RecyclerViews
        topPlayersRecyclerView = findViewById(R.id.top_players_recycler);
        topPlayersRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        topPlayerAdapter = new TopPlayerAdapter(topPlayers);
        topPlayersRecyclerView.setAdapter(topPlayerAdapter);

        recentActivityRecyclerView = findViewById(R.id.recent_activity_recycler);
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivityAdapter = new RecentActivityAdapter(activityItems);
        recentActivityRecyclerView.setAdapter(recentActivityAdapter);
    }

    private void setupClickListeners() {
        if (teamManagementCard != null) {
            teamManagementCard.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, TeamManagementActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Error navigating to TeamManagement: " + e.getMessage());
                    Toast.makeText(this, "Error opening Team Management", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (playerRegistrationCard != null) {
            playerRegistrationCard.setOnClickListener(v -> {
                Log.d("DashboardActivity", "Register Players card clicked");
                Toast.makeText(this, "Opening Register Players...", Toast.LENGTH_SHORT).show();
                try {
                    startActivity(new Intent(this, com.finedine.spucricketclub.ui.PlayerRegistrationActivity.class));
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Error opening Register Players", e);
                    Toast.makeText(this, "Could not open Register Players! " + e, Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (analyticsCard != null) {
            analyticsCard.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, PitchVisionActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Error navigating to Analytics: " + e.getMessage());
                    Toast.makeText(this, "Error opening Analytics", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (matchHistoryCard != null) {
            matchHistoryCard.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, CricketScoringActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Error navigating to MatchHistory: " + e.getMessage());
                    Toast.makeText(this, "Error opening Match History", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (pitchVisionCard != null) {
            pitchVisionCard.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, PitchVisionActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Error navigating to PitchVision: " + e.getMessage());
                    Toast.makeText(this, "Error opening Pitch Vision", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (newMatchFab != null) {
            newMatchFab.setOnClickListener(v -> {
                Log.d("DashboardActivity", "Start Match FAB pressed");
                try {
                    startActivity(new Intent(this, CricketScoringActivity.class));
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Critical error starting match", e);
                    Toast.makeText(this, "Could not start match! " + e, Toast.LENGTH_SHORT).show();
                }
            });
        }
        View startMatchButton = findViewById(R.id.button_start_match);
        if (startMatchButton != null) {
            startMatchButton.setOnClickListener(v -> {
                Log.d("DashboardActivity", "Start Match button pressed");
                try {
                    startActivity(new Intent(this, CricketScoringActivity.class));
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Critical error starting match (button_start_match)", e);
                    Toast.makeText(this, "Could not start match! " + e, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e("DashboardActivity", "Start Match button not found!");
        }
        if (settingsCard != null) {
            settingsCard.setOnClickListener(v -> {
                Log.d("DashboardActivity", "Settings card clicked");
                Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show();
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (Exception e) {
                    Log.e("DashboardActivity", "Error opening Settings", e);
                    Toast.makeText(this, "Could not open Settings! " + e, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadTopPlayers() {
        topPlayers.clear();

        List<Player> allPlayers = playerDatabase.getAllPlayers();
        if (allPlayers.size() > 0) {
            for (Player player : allPlayers) {
                // Create player stats based on the player data
                PlayerStats stats = new PlayerStats(
                        player.getName(),
                        player.getRole().toString(),
                        getPlayerStatLine(player),
                        null
                );
                topPlayers.add(stats);

                // Limit to 5 players
                if (topPlayers.size() >= 5) break;
            }
        } else {
            // Add mock data if no players exist
            topPlayers.add(new PlayerStats("M.S. Dhoni", "WICKET_KEEPER", "85 Runs (58 balls)", null));
            topPlayers.add(new PlayerStats("Virat Kohli", "BATSMAN", "112 Runs (70 balls)", null));
            topPlayers.add(new PlayerStats("Jasprit Bumrah", "BOWLER", "3/27 (4 overs)", null));
            topPlayers.add(new PlayerStats("Ravindra Jadeja", "ALL_ROUNDER", "45 Runs, 2/30", null));
            topPlayers.add(new PlayerStats("Rohit Sharma", "BATSMAN", "68 Runs (42 balls)", null));
        }

        topPlayerAdapter.notifyDataSetChanged();
    }

    private String getPlayerStatLine(Player player) {
        // Generate a stat line based on the player's role
        switch (player.getRole()) {
            case BATSMAN:
                return player.getBattingStats().getRuns() + " Runs (" +
                        player.getBattingStats().getBallsFaced() + " balls)";
            case BOWLER:
                return player.getBowlingStats().getWickets() + "/" +
                        player.getBowlingStats().getRunsConceded() + " (" +
                        player.getBowlingStats().getOvers() + " overs)";
            case ALL_ROUNDER:
                return player.getBattingStats().getRuns() + " Runs, " +
                        player.getBowlingStats().getWickets() + "/" +
                        player.getBowlingStats().getRunsConceded();
            case WICKET_KEEPER:
                return player.getBattingStats().getRuns() + " Runs, " +
                        "0 Dismissals";
            default:
                return "No stats available";
        }
    }

    private void loadRecentActivity() {
        // In a real app, this would come from a database or API
        // Mocked data for demonstration
        activityItems.clear();

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());

        ActivityItem item1 = new ActivityItem(
                R.drawable.ic_menu_gallery,
                "New Player Registered",
                "Coach added a new player: Virat Kohli",
                "2h ago"
        );

        ActivityItem item2 = new ActivityItem(
                R.drawable.ic_menu_manage,
                "Match Completed",
                "SPU won by 45 runs against Opponents",
                "Yesterday"
        );

        ActivityItem item3 = new ActivityItem(
                R.drawable.ic_menu_camera,
                "Face Recognition Updated",
                "5 new player faces registered",
                "2 days ago"
        );

        activityItems.add(item1);
        activityItems.add(item2);
        activityItems.add(item3);

        recentActivityAdapter.notifyDataSetChanged();
    }

    private void setupUpcomingMatch() {
        // Set upcoming match details
        TextView matchDateTime = findViewById(R.id.match_date_time);
        TextView homeTeamName = findViewById(R.id.home_team_name);
        TextView awayTeamName = findViewById(R.id.away_team_name);

        Match currentMatch = cricketMatchService.getCurrentMatch();

        if (currentMatch != null) {
            homeTeamName.setText(currentMatch.getTeamBatting().getTeamName());
            awayTeamName.setText(currentMatch.getTeamBowling().getTeamName());
        }

        // Set a date for the next Sunday at 2:00 PM
        Calendar calendar = Calendar.getInstance();
        int daysUntilSunday = Calendar.SUNDAY - calendar.get(Calendar.DAY_OF_WEEK);
        if (daysUntilSunday <= 0) {
            daysUntilSunday += 7;
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysUntilSunday);
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d • h:mm a", Locale.getDefault());
        String formattedDate = dateFormat.format(calendar.getTime());

        matchDateTime.setText(formattedDate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadTopPlayers();
        loadRecentActivity();
        setupUpcomingMatch();
    }
}
