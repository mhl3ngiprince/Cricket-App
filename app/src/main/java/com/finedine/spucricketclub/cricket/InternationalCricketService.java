package com.finedine.spucricketclub.cricket;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.finedine.spucricketclub.utils.NetworkUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service for fetching and managing international cricket data from various cricket boards
 */
public class InternationalCricketService {
    private static final String TAG = "IntlCricketService";

    // Singleton instance
    private static InternationalCricketService instance;

    // Context
    private final Context context;

    // API endpoints
    private static final String CRICAPI_BASE_URL = "https://api.cricapi.com/v1/";
    private static final String ICC_RANKINGS_ENDPOINT = "currentMatches";
    private static final String SCHEDULE_ENDPOINT = "matchCalendar";
    private static final String MATCH_DETAILS_ENDPOINT = "match_info";
    private static final String PLAYER_STATS_ENDPOINT = "players_info";

    // API key - Note: In production, this should be secured or fetched from a secure source
    private static final String CRICAPI_KEY = "YOUR_CRICKET_API_KEY";

    // Firebase references
    private final DatabaseReference cricketRef;
    private final DatabaseReference rankingsRef;
    private final DatabaseReference schedulesRef;
    private final DatabaseReference tournamentsRef;

    // Cache
    private final Map<String, CricketBoard> cricketBoards = new HashMap<>();
    private final Map<String, List<TeamRanking>> teamRankings = new HashMap<>();
    private final Map<String, List<PlayerRanking>> playerRankings = new HashMap<>();
    private final Map<String, Tournament> tournaments = new HashMap<>();
    private final Map<String, CricketVenue> venues = new HashMap<>();
    private final List<UpcomingMatch> upcomingMatches = new ArrayList<>();

    // Background executor
    private final Executor backgroundExecutor;

    // Network client
    private final OkHttpClient httpClient;
    private final NetworkUtils networkUtils;

    // Classes for rankings
    public static class TeamRanking {
        private int rank;
        private String teamId;
        private String teamName;
        private String countryCode;
        private int points;
        private int rating;

        public TeamRanking(int rank, String teamId, String teamName, String countryCode, int points, int rating) {
            this.rank = rank;
            this.teamId = teamId;
            this.teamName = teamName;
            this.countryCode = countryCode;
            this.points = points;
            this.rating = rating;
        }

        // Getters
        public int getRank() {
            return rank;
        }

        public String getTeamId() {
            return teamId;
        }

        public String getTeamName() {
            return teamName;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public int getPoints() {
            return points;
        }

        public int getRating() {
            return rating;
        }
    }

    public static class PlayerRanking {
        private int rank;
        private String playerId;
        private String name;
        private String countryCode;
        private String teamId;
        private int rating;
        private PlayerRankingType rankingType;

        public enum PlayerRankingType {
            TEST_BATTING,
            TEST_BOWLING,
            TEST_ALL_ROUNDER,
            ODI_BATTING,
            ODI_BOWLING,
            ODI_ALL_ROUNDER,
            T20_BATTING,
            T20_BOWLING,
            T20_ALL_ROUNDER
        }

        public PlayerRanking(int rank, String playerId, String name, String countryCode,
                             String teamId, int rating, PlayerRankingType rankingType) {
            this.rank = rank;
            this.playerId = playerId;
            this.name = name;
            this.countryCode = countryCode;
            this.teamId = teamId;
            this.rating = rating;
            this.rankingType = rankingType;
        }

        // Getters
        public int getRank() {
            return rank;
        }

        public String getPlayerId() {
            return playerId;
        }

        public String getName() {
            return name;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getTeamId() {
            return teamId;
        }

        public int getRating() {
            return rating;
        }

        public PlayerRankingType getRankingType() {
            return rankingType;
        }
    }

    public static class UpcomingMatch {
        private String id;
        private String team1;
        private String team2;
        private String team1Id;
        private String team2Id;
        private String venue;
        private String venueId;
        private Date date;
        private String matchType;
        private String seriesId;
        private String seriesName;

        public UpcomingMatch(String id, String team1, String team2, String venue,
                             Date date, String matchType, String seriesName) {
            this.id = id;
            this.team1 = team1;
            this.team2 = team2;
            this.venue = venue;
            this.date = date;
            this.matchType = matchType;
            this.seriesName = seriesName;
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getTeam1() {
            return team1;
        }

        public String getTeam2() {
            return team2;
        }

        public String getTeam1Id() {
            return team1Id;
        }

        public String getTeam2Id() {
            return team2Id;
        }

        public String getVenue() {
            return venue;
        }

        public String getVenueId() {
            return venueId;
        }

        public Date getDate() {
            return date;
        }

        public String getMatchType() {
            return matchType;
        }

        public String getSeriesId() {
            return seriesId;
        }

        public String getSeriesName() {
            return seriesName;
        }

        // Setters for IDs that may be resolved later
        public void setTeam1Id(String team1Id) {
            this.team1Id = team1Id;
        }

        public void setTeam2Id(String team2Id) {
            this.team2Id = team2Id;
        }

        public void setVenueId(String venueId) {
            this.venueId = venueId;
        }

        public void setSeriesId(String seriesId) {
            this.seriesId = seriesId;
        }
    }

    // Public callback interfaces
    public interface FetchTeamRankingsCallback {
        void onRankingsFetched(List<TeamRanking> rankings);

        void onError(String errorMessage);
    }

    public interface FetchPlayerRankingsCallback {
        void onRankingsFetched(List<PlayerRanking> rankings);

        void onError(String errorMessage);
    }

    public interface FetchUpcomingMatchesCallback {
        void onMatchesFetched(List<UpcomingMatch> matches);

        void onError(String errorMessage);
    }

    public interface FetchTournamentCallback {
        void onTournamentFetched(Tournament tournament);

        void onError(String errorMessage);
    }

    // Private constructor for singleton pattern
    private InternationalCricketService(Context context) {
        this.context = context.getApplicationContext();
        this.networkUtils = new NetworkUtils(this.context);

        // Initialize Firebase references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        cricketRef = database.getReference("cricket_data");
        rankingsRef = cricketRef.child("rankings");
        schedulesRef = cricketRef.child("schedules");
        tournamentsRef = cricketRef.child("tournaments");

        // Initialize network client
        httpClient = new OkHttpClient();

        // Initialize background executor
        backgroundExecutor = Executors.newFixedThreadPool(2);

        // Preload data
        preloadData();
    }

    /**
     * Get the singleton instance
     *
     * @param context Application context
     * @return InternationalCricketService instance
     */
    public static synchronized InternationalCricketService getInstance(Context context) {
        if (instance == null) {
            instance = new InternationalCricketService(context);
        }
        return instance;
    }

    /**
     * Preload cricket boards, venues and tournaments
     */
    private void preloadData() {
        // Load cricket boards
        loadCricketBoards();

        // Load venues
        loadVenues();

        // Load tournaments
        loadTournaments();

        // Load rankings from both Firebase and API
        loadRankings();
    }

    /**
     * Load cricket boards
     */
    private void loadCricketBoards() {
        // Load common cricket boards
        cricketBoards.put(CricketBoard.ICC, CricketBoard.createPredefined(CricketBoard.ICC));
        cricketBoards.put(CricketBoard.BCCI, CricketBoard.createPredefined(CricketBoard.BCCI));
        cricketBoards.put(CricketBoard.CA, CricketBoard.createPredefined(CricketBoard.CA));
        cricketBoards.put(CricketBoard.ECB, CricketBoard.createPredefined(CricketBoard.ECB));
        cricketBoards.put(CricketBoard.CSA, CricketBoard.createPredefined(CricketBoard.CSA));
        cricketBoards.put(CricketBoard.PCB, CricketBoard.createPredefined(CricketBoard.PCB));
        cricketBoards.put(CricketBoard.SLC, CricketBoard.createPredefined(CricketBoard.SLC));
        cricketBoards.put(CricketBoard.NZC, CricketBoard.createPredefined(CricketBoard.NZC));
        cricketBoards.put(CricketBoard.WI, CricketBoard.createPredefined(CricketBoard.WI));
        cricketBoards.put(CricketBoard.BCB, CricketBoard.createPredefined(CricketBoard.BCB));
    }

    /**
     * Load venues
     */
    private void loadVenues() {
        // Load common venues
        venues.put(CricketVenue.LORDS, CricketVenue.createPredefined(CricketVenue.LORDS));
        venues.put(CricketVenue.MCG, CricketVenue.createPredefined(CricketVenue.MCG));
        venues.put(CricketVenue.EDEN_GARDENS, CricketVenue.createPredefined(CricketVenue.EDEN_GARDENS));
        venues.put(CricketVenue.WANDERERS, CricketVenue.createPredefined(CricketVenue.WANDERERS));
        venues.put(CricketVenue.NEWLANDS, CricketVenue.createPredefined(CricketVenue.NEWLANDS));
    }

    /**
     * Load tournaments
     */
    private void loadTournaments() {
        // Load common tournaments
        tournaments.put(Tournament.WORLD_CUP, Tournament.createPredefined(Tournament.WORLD_CUP));
        tournaments.put(Tournament.T20_WORLD_CUP, Tournament.createPredefined(Tournament.T20_WORLD_CUP));
        tournaments.put(Tournament.IPL, Tournament.createPredefined(Tournament.IPL));
        tournaments.put(Tournament.BIG_BASH, Tournament.createPredefined(Tournament.BIG_BASH));
        tournaments.put(Tournament.THE_ASHES, Tournament.createPredefined(Tournament.THE_ASHES));

        // Also load from Firebase
        tournamentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot tournamentSnapshot : snapshot.getChildren()) {
                    try {
                        Tournament tournament = tournamentSnapshot.getValue(Tournament.class);
                        if (tournament != null && tournament.getId() != null) {
                            tournaments.put(tournament.getId(), tournament);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading tournament from Firebase", e);
                    }
                }
                Log.d(TAG, "Loaded " + tournaments.size() + " tournaments");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading tournaments", error.toException());
            }
        });
    }

    /**
     * Load rankings
     */
    private void loadRankings() {
        // Load from Firebase first
        rankingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    // Load team rankings
                    DataSnapshot teamRankingsSnapshot = snapshot.child("teams");
                    for (DataSnapshot formatSnapshot : teamRankingsSnapshot.getChildren()) {
                        String format = formatSnapshot.getKey();
                        List<TeamRanking> rankings = new ArrayList<>();

                        for (DataSnapshot rankSnapshot : formatSnapshot.getChildren()) {
                            try {
                                Map<String, Object> rankData = (Map<String, Object>) rankSnapshot.getValue();
                                if (rankData != null) {
                                    int rank = rankData.containsKey("rank") ?
                                            ((Long) rankData.get("rank")).intValue() : 0;
                                    String teamId = (String) rankData.get("teamId");
                                    String teamName = (String) rankData.get("teamName");
                                    String countryCode = (String) rankData.get("countryCode");
                                    int points = rankData.containsKey("points") ?
                                            ((Long) rankData.get("points")).intValue() : 0;
                                    int rating = rankData.containsKey("rating") ?
                                            ((Long) rankData.get("rating")).intValue() : 0;

                                    TeamRanking ranking = new TeamRanking(rank, teamId,
                                            teamName, countryCode, points, rating);
                                    rankings.add(ranking);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing team ranking", e);
                            }
                        }

                        if (!rankings.isEmpty()) {
                            teamRankings.put(format, rankings);
                        }
                    }

                    // Load player rankings
                    DataSnapshot playerRankingsSnapshot = snapshot.child("players");
                    for (DataSnapshot typeSnapshot : playerRankingsSnapshot.getChildren()) {
                        String type = typeSnapshot.getKey();
                        List<PlayerRanking> rankings = new ArrayList<>();

                        for (DataSnapshot rankSnapshot : typeSnapshot.getChildren()) {
                            try {
                                Map<String, Object> rankData = (Map<String, Object>) rankSnapshot.getValue();
                                if (rankData != null) {
                                    int rank = rankData.containsKey("rank") ?
                                            ((Long) rankData.get("rank")).intValue() : 0;
                                    String playerId = (String) rankData.get("playerId");
                                    String name = (String) rankData.get("name");
                                    String countryCode = (String) rankData.get("countryCode");
                                    String teamId = (String) rankData.get("teamId");
                                    int rating = rankData.containsKey("rating") ?
                                            ((Long) rankData.get("rating")).intValue() : 0;

                                    PlayerRanking.PlayerRankingType rankingType =
                                            PlayerRanking.PlayerRankingType.valueOf(type);

                                    PlayerRanking ranking = new PlayerRanking(rank, playerId, name,
                                            countryCode, teamId, rating, rankingType);
                                    rankings.add(ranking);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing player ranking", e);
                            }
                        }

                        if (!rankings.isEmpty()) {
                            playerRankings.put(type, rankings);
                        }
                    }

                    Log.d(TAG, "Loaded rankings from Firebase: " +
                            teamRankings.size() + " team rankings formats, " +
                            playerRankings.size() + " player rankings types");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading rankings from Firebase", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Rankings loading cancelled", error.toException());
            }
        });

        // Then update from API
        if (networkUtils.isNetworkAvailable()) {
            updateRankingsFromAPI();
        }
    }

    /**
     * Update rankings from the cricket API
     */
    private void updateRankingsFromAPI() {
        // This is a mock implementation as real API usage would require a valid API key
        // In a real implementation, you would make API calls here
        Log.d(TAG, "Updating rankings from API (mock)");
    }

    /**
     * Get cricket board by ID
     */
    public CricketBoard getCricketBoard(String boardId) {
        return cricketBoards.get(boardId);
    }

    /**
     * Get all cricket boards
     */
    public List<CricketBoard> getAllCricketBoards() {
        return new ArrayList<>(cricketBoards.values());
    }

    /**
     * Get venue by ID
     */
    public CricketVenue getVenue(String venueId) {
        return venues.get(venueId);
    }

    /**
     * Get all venues
     */
    public List<CricketVenue> getAllVenues() {
        return new ArrayList<>(venues.values());
    }

    /**
     * Get tournament by ID
     */
    public Tournament getTournament(String tournamentId) {
        return tournaments.get(tournamentId);
    }

    /**
     * Get all tournaments
     */
    public List<Tournament> getAllTournaments() {
        return new ArrayList<>(tournaments.values());
    }

    /**
     * Get active tournaments
     */
    public List<Tournament> getActiveTournaments() {
        List<Tournament> activeTournaments = new ArrayList<>();
        for (Tournament tournament : tournaments.values()) {
            if (tournament.isCurrentlyActive()) {
                activeTournaments.add(tournament);
            }
        }
        return activeTournaments;
    }

    /**
     * Get team rankings by format (TEST, ODI, T20)
     */
    public void getTeamRankings(String format, FetchTeamRankingsCallback callback) {
        // Check cache first
        if (teamRankings.containsKey(format) && !teamRankings.get(format).isEmpty()) {
            callback.onRankingsFetched(teamRankings.get(format));
            return;
        }

        // If not in cache, check Firebase
        rankingsRef.child("teams").child(format).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<TeamRanking> rankings = new ArrayList<>();
                    for (DataSnapshot rankSnapshot : snapshot.getChildren()) {
                        try {
                            Map<String, Object> rankData = (Map<String, Object>) rankSnapshot.getValue();
                            if (rankData != null) {
                                int rank = rankData.containsKey("rank") ?
                                        ((Long) rankData.get("rank")).intValue() : 0;
                                String teamId = (String) rankData.get("teamId");
                                String teamName = (String) rankData.get("teamName");
                                String countryCode = (String) rankData.get("countryCode");
                                int points = rankData.containsKey("points") ?
                                        ((Long) rankData.get("points")).intValue() : 0;
                                int rating = rankData.containsKey("rating") ?
                                        ((Long) rankData.get("rating")).intValue() : 0;

                                TeamRanking ranking = new TeamRanking(rank, teamId,
                                        teamName, countryCode, points, rating);
                                rankings.add(ranking);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing team ranking", e);
                        }
                    }

                    if (!rankings.isEmpty()) {
                        teamRankings.put(format, rankings);
                        callback.onRankingsFetched(rankings);
                    } else {
                        callback.onError("No rankings found for format: " + format);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting team rankings", e);
                    callback.onError("Error getting team rankings: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Team rankings fetch cancelled", error.toException());
                callback.onError("Error getting team rankings: " + error.getMessage());
            }
        });
    }

    /**
     * Get player rankings by type
     */
    public void getPlayerRankings(PlayerRanking.PlayerRankingType rankingType, FetchPlayerRankingsCallback callback) {
        String typeKey = rankingType.toString();

        // Check cache first
        if (playerRankings.containsKey(typeKey) && !playerRankings.get(typeKey).isEmpty()) {
            callback.onRankingsFetched(playerRankings.get(typeKey));
            return;
        }

        // If not in cache, check Firebase
        rankingsRef.child("players").child(typeKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<PlayerRanking> rankings = new ArrayList<>();
                    for (DataSnapshot rankSnapshot : snapshot.getChildren()) {
                        try {
                            Map<String, Object> rankData = (Map<String, Object>) rankSnapshot.getValue();
                            if (rankData != null) {
                                int rank = rankData.containsKey("rank") ?
                                        ((Long) rankData.get("rank")).intValue() : 0;
                                String playerId = (String) rankData.get("playerId");
                                String name = (String) rankData.get("name");
                                String countryCode = (String) rankData.get("countryCode");
                                String teamId = (String) rankData.get("teamId");
                                int rating = rankData.containsKey("rating") ?
                                        ((Long) rankData.get("rating")).intValue() : 0;

                                PlayerRanking ranking = new PlayerRanking(rank, playerId, name,
                                        countryCode, teamId, rating, rankingType);
                                rankings.add(ranking);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing player ranking", e);
                        }
                    }

                    if (!rankings.isEmpty()) {
                        playerRankings.put(typeKey, rankings);
                        callback.onRankingsFetched(rankings);
                    } else {
                        callback.onError("No rankings found for type: " + typeKey);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting player rankings", e);
                    callback.onError("Error getting player rankings: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Player rankings fetch cancelled", error.toException());
                callback.onError("Error getting player rankings: " + error.getMessage());
            }
        });
    }

    /**
     * Get upcoming cricket matches
     */
    public void getUpcomingMatches(FetchUpcomingMatchesCallback callback) {
        // Check cache first
        if (!upcomingMatches.isEmpty()) {
            callback.onMatchesFetched(upcomingMatches);
            return;
        }

        // If not in cache, fetch from API (mock implementation)
        // In a real implementation, this would make an API call
        try {
            String url = CRICAPI_BASE_URL + SCHEDULE_ENDPOINT + "?apikey=" + CRICAPI_KEY;
            Request request = new Request.Builder().url(url).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error fetching upcoming matches", e);
                    callback.onError("Error fetching upcoming matches: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("Error fetching upcoming matches: HTTP " + response.code());
                        return;
                    }

                    // Since we're using a mock implementation, create some example matches
                    List<UpcomingMatch> matches = createMockUpcomingMatches();
                    upcomingMatches.clear();
                    upcomingMatches.addAll(matches);

                    callback.onMatchesFetched(matches);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error getting upcoming matches", e);
            callback.onError("Error getting upcoming matches: " + e.getMessage());
        }
    }

    /**
     * Create mock upcoming matches for demo purposes
     */
    private List<UpcomingMatch> createMockUpcomingMatches() {
        List<UpcomingMatch> matches = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            // Create some example matches
            matches.add(new UpcomingMatch("match1", "India", "England", "Lord's Cricket Ground",
                    sdf.parse("2023-06-15"), "Test", "India Tour of England 2023"));
            matches.add(new UpcomingMatch("match2", "Australia", "South Africa", "Melbourne Cricket Ground",
                    sdf.parse("2023-06-18"), "ODI", "South Africa Tour of Australia 2023"));
            matches.add(new UpcomingMatch("match3", "New Zealand", "Sri Lanka", "Eden Park",
                    sdf.parse("2023-06-20"), "T20I", "Sri Lanka Tour of New Zealand 2023"));
            matches.add(new UpcomingMatch("match4", "Pakistan", "West Indies", "National Stadium Karachi",
                    sdf.parse("2023-06-23"), "Test", "West Indies Tour of Pakistan 2023"));
            matches.add(new UpcomingMatch("match5", "Bangladesh", "Afghanistan", "Shere Bangla National Stadium",
                    sdf.parse("2023-06-25"), "ODI", "Afghanistan Tour of Bangladesh 2023"));
        } catch (Exception e) {
            Log.e(TAG, "Error creating mock matches", e);
        }

        return matches;
    }

    /**
     * Get detailed information about a specific tournament
     */
    public void getTournamentDetails(String tournamentId, FetchTournamentCallback callback) {
        // Check cache first
        if (tournaments.containsKey(tournamentId)) {
            callback.onTournamentFetched(tournaments.get(tournamentId));
            return;
        }

        // If not in cache, check Firebase
        tournamentsRef.child(tournamentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    Tournament tournament = snapshot.getValue(Tournament.class);
                    if (tournament != null) {
                        tournaments.put(tournamentId, tournament);
                        callback.onTournamentFetched(tournament);
                    } else {
                        callback.onError("Tournament not found: " + tournamentId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting tournament details", e);
                    callback.onError("Error getting tournament details: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Tournament details fetch cancelled", error.toException());
                callback.onError("Error getting tournament details: " + error.getMessage());
            }
        });
    }
}
