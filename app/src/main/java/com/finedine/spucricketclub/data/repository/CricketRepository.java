package com.finedine.spucricketclub.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Team;
import com.finedine.spucricketclub.data.FirebaseHelper;
import com.finedine.spucricketclub.data.PlayerDatabase;
import com.finedine.spucricketclub.data.db.AppDatabase;
import com.finedine.spucricketclub.data.db.entity.MatchEntity;
import com.finedine.spucricketclub.data.db.entity.PlayerEntity;
import com.finedine.spucricketclub.data.db.entity.TeamEntity;
import com.finedine.spucricketclub.data.db.entity.TournamentEntity;
import com.finedine.spucricketclub.utils.NetworkUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository class that manages data operations and acts as a clean API for the rest of the app.
 * This class handles the complexity of managing different data sources (Room database and Firebase).
 */
public class CricketRepository {
    private static final String TAG = "CricketRepository";
    private static CricketRepository INSTANCE;

    private final Context context;
    private final AppDatabase database;
    private final FirebaseHelper firebaseHelper;
    private final PlayerDatabase playerDatabase;
    private final Executor executor;
    private final NetworkUtils networkUtils;

    // In-memory cache
    private final MutableLiveData<List<Player>> cachedPlayers = new MutableLiveData<>();
    private final MutableLiveData<List<Team>> cachedTeams = new MutableLiveData<>();
    private final MutableLiveData<List<Match>> cachedMatches = new MutableLiveData<>();

    // Private constructor for singleton pattern
    private CricketRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.firebaseHelper = FirebaseHelper.getInstance();
        this.playerDatabase = PlayerDatabase.getInstance(context);
        this.executor = Executors.newFixedThreadPool(4);
        this.networkUtils = new NetworkUtils(context);

        // Initialize data
        refreshData();
    }

    /**
     * Get the singleton instance of the repository
     */
    public static synchronized CricketRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CricketRepository(context);
        }
        return INSTANCE;
    }

    /**
     * Refresh all data from remote sources
     */
    public void refreshData() {
        refreshPlayers();
        refreshTeams();
        refreshMatches();
    }

    /**
     * Refresh players from Firebase and update local database
     */
    private void refreshPlayers() {
        if (!networkUtils.isNetworkAvailable()) {
            Log.d(TAG, "No network connection, using local database");
            return;
        }

        firebaseHelper.getPlayers(new FirebaseHelper.FirebaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                cachedPlayers.postValue(players);

                // Update local database
                executor.execute(() -> {
                    List<PlayerEntity> playerEntities = new ArrayList<>();
                    for (Player player : players) {
                        PlayerEntity entity = convertPlayerToEntity(player);
                        playerEntities.add(entity);
                    }
                    database.playerDao().insertAll(playerEntities);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error refreshing players: " + errorMessage);
            }
        });
    }

    /**
     * Refresh teams from Firebase and update local database
     */
    private void refreshTeams() {
        if (!networkUtils.isNetworkAvailable()) {
            Log.d(TAG, "No network connection, using local database");
            return;
        }

        firebaseHelper.getTeams(new FirebaseHelper.FirebaseCallback<List<Team>>() {
            @Override
            public void onSuccess(List<Team> teams) {
                cachedTeams.postValue(teams);

                // Update local database
                executor.execute(() -> {
                    List<TeamEntity> teamEntities = new ArrayList<>();
                    for (Team team : teams) {
                        TeamEntity entity = convertTeamToEntity(team);
                        teamEntities.add(entity);
                    }
                    for (TeamEntity entity : teamEntities) {
                        database.teamDao().insert(entity);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error refreshing teams: " + errorMessage);
            }
        });
    }

    /**
     * Refresh matches from Firebase and update local database
     */
    private void refreshMatches() {
        if (!networkUtils.isNetworkAvailable()) {
            Log.d(TAG, "No network connection, using local database");
            return;
        }

        firebaseHelper.getMatches(new FirebaseHelper.FirebaseCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> matches) {
                cachedMatches.postValue(matches);

                // Update local database
                executor.execute(() -> {
                    List<MatchEntity> matchEntities = new ArrayList<>();
                    for (Match match : matches) {
                        MatchEntity entity = convertMatchToEntity(match);
                        matchEntities.add(entity);
                    }
                    for (MatchEntity entity : matchEntities) {
                        database.matchDao().insert(entity);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error refreshing matches: " + errorMessage);
            }
        });
    }

    /**
     * Get all players as LiveData
     */
    public LiveData<List<Player>> getAllPlayers() {
        refreshPlayers(); // Try to refresh from Firebase

        // Return local database LiveData
        return Transformations.map(database.playerDao().getAll(), playerEntities -> {
            List<Player> players = new ArrayList<>();
            if (playerEntities != null) {
                for (PlayerEntity entity : playerEntities) {
                    Player player = convertEntityToPlayer(entity);
                    players.add(player);
                }
            }
            return players;
        });
    }

    /**
     * Get player by ID
     */
    public LiveData<Player> getPlayerById(String playerId) {
        return Transformations.map(database.playerDao().getById(playerId), this::convertEntityToPlayer);
    }

    /**
     * Save player to both Firebase and local database
     */
    public void savePlayer(Player player) {
        // Save to Firebase
        firebaseHelper.savePlayer(player);

        // Save to local database
        PlayerEntity entity = convertPlayerToEntity(player);
        executor.execute(() -> database.playerDao().insert(entity));
    }

    /**
     * Save player face image
     */
    public void savePlayerFaceImage(String playerId, Bitmap faceBitmap) {
        playerDatabase.savePlayerFaceImage(playerId, faceBitmap);
    }

    /**
     * Get player face image
     */
    public Bitmap getPlayerFaceImage(String playerId) {
        return playerDatabase.loadPlayerFaceImage(playerId);
    }

    /**
     * Save player face embedding
     */
    public void savePlayerFaceEmbedding(String playerId, float[] embedding) {
        playerDatabase.savePlayerFaceEmbedding(playerId, embedding);
    }

    /**
     * Get player face embedding
     */
    public float[] getPlayerFaceEmbedding(String playerId) {
        return playerDatabase.getPlayerFaceEmbedding(playerId);
    }

    /**
     * Delete player
     */
    public void deletePlayer(String playerId) {
        executor.execute(() -> {
            database.playerDao().deleteById(playerId);
            playerDatabase.deletePlayer(playerId);
        });
    }

    /**
     * Convert Player domain object to PlayerEntity database object
     */
    private PlayerEntity convertPlayerToEntity(Player player) {
        PlayerEntity entity = new PlayerEntity();
        entity.setId(player.getId());
        entity.setName(player.getName());
        entity.setRoleEnum(player.getRole());
        entity.setJerseyNumber(player.getJerseyNumber());

        // Convert batting stats
        Map<String, Object> battingStats = new HashMap<>();
        battingStats.put("runs", player.getBattingStats().getRuns());
        battingStats.put("ballsFaced", player.getBattingStats().getBallsFaced());
        battingStats.put("fours", player.getBattingStats().getFours());
        battingStats.put("sixes", player.getBattingStats().getSixes());
        entity.setBattingStats(battingStats);

        // Convert bowling stats
        Map<String, Object> bowlingStats = new HashMap<>();
        bowlingStats.put("wickets", player.getBowlingStats().getWickets());
        bowlingStats.put("runsConceded", player.getBowlingStats().getRunsConceded());
        bowlingStats.put("overs", player.getBowlingStats().getOvers());
        entity.setBowlingStats(bowlingStats);

        return entity;
    }

    /**
     * Convert PlayerEntity database object to Player domain object
     */
    private Player convertEntityToPlayer(PlayerEntity entity) {
        Player player = new Player(entity.getName(), entity.getRoleEnum());
        player.setId(entity.getId());
        player.setJerseyNumber(entity.getJerseyNumber());

        // Convert batting stats
        if (entity.getBattingStats() != null) {
            Map<String, Object> battingStats = entity.getBattingStats();
            if (battingStats.containsKey("runs")) {
                player.getBattingStats().setRuns(((Number) battingStats.get("runs")).intValue());
            }
            if (battingStats.containsKey("ballsFaced")) {
                player.getBattingStats().setBallsFaced(((Number) battingStats.get("ballsFaced")).intValue());
            }
            if (battingStats.containsKey("fours")) {
                player.getBattingStats().setFours(((Number) battingStats.get("fours")).intValue());
            }
            if (battingStats.containsKey("sixes")) {
                player.getBattingStats().setSixes(((Number) battingStats.get("sixes")).intValue());
            }
        }

        // Convert bowling stats
        if (entity.getBowlingStats() != null) {
            Map<String, Object> bowlingStats = entity.getBowlingStats();
            if (bowlingStats.containsKey("wickets")) {
                player.getBowlingStats().setWickets(((Number) bowlingStats.get("wickets")).intValue());
            }
            if (bowlingStats.containsKey("runsConceded")) {
                player.getBowlingStats().setRunsConceded(((Number) bowlingStats.get("runsConceded")).intValue());
            }
            if (bowlingStats.containsKey("overs")) {
                player.getBowlingStats().setOvers(((Number) bowlingStats.get("overs")).intValue());
            }
        }

        return player;
    }

    /**
     * Convert Team domain object to TeamEntity database object
     */
    private TeamEntity convertTeamToEntity(Team team) {
        TeamEntity entity = new TeamEntity();
        entity.setId(team.getId());
        entity.setTeamName(team.getTeamName());

        // Get player IDs
        List<String> playerIds = new ArrayList<>();
        for (Player player : team.getPlayers()) {
            playerIds.add(player.getId());
        }
        entity.setPlayerIds(playerIds);

        return entity;
    }

    /**
     * Convert Match domain object to MatchEntity database object
     */
    private MatchEntity convertMatchToEntity(Match match) {
        MatchEntity entity = new MatchEntity();
        entity.setId(match.getId());
        entity.setMatchName(match.getMatchName());
        entity.setTeamAId(match.getTeamBatting().getId());
        entity.setTeamBId(match.getTeamBowling().getId());
        entity.setMaxOvers(match.getMaxOvers());
        entity.setStatus(match.getStatus().name());

        if (match.isCompleted()) {
            entity.setEndTime(new Date());
        }

        return entity;
    }
}
