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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for fetching and managing South African University cricket data with focus on SPU
 */
public class SouthAfricanUniversityCricketService {
    private static final String TAG = "SAUniCricketService";

    // Singleton instance
    private static SouthAfricanUniversityCricketService instance;

    // Context
    private final Context context;

    // Firebase references
    private final DatabaseReference uniCricketRef;
    private final DatabaseReference ussaRankingsRef;
    private final DatabaseReference venuesRef;
    private final DatabaseReference tournamentsRef;
    private final DatabaseReference spuSpecificRef;

    // Cache
    private final Map<String, SouthAfricanUniversities> universities = new HashMap<>();
    private final Map<String, SAVenue> venues = new HashMap<>();
    private final Map<String, USSATournament> tournaments = new HashMap<>();
    private final List<UpcomingMatch> upcomingMatches = new ArrayList<>();
    private final Map<String, Player> spuPlayers = new HashMap<>();

    // Background executor
    private final Executor backgroundExecutor;

    // Classes for specific University cricket data
    public static class UpcomingMatch {
        private String id;
        private String homeTeamId;
        private String awayTeamId;
        private String venueId;
        private Date date;
        private String matchType;
        private String tournamentId;
        private boolean isHomeMatch; // Is SPU home team?

        public UpcomingMatch(String id, String homeTeamId, String awayTeamId, String venueId,
                             Date date, String matchType, String tournamentId) {
            this.id = id;
            this.homeTeamId = homeTeamId;
            this.awayTeamId = awayTeamId;
            this.venueId = venueId;
            this.date = date;
            this.matchType = matchType;
            this.tournamentId = tournamentId;
            this.isHomeMatch = SouthAfricanUniversities.SPU.equals(homeTeamId);
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getHomeTeamId() {
            return homeTeamId;
        }

        public String getAwayTeamId() {
            return awayTeamId;
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

        public String getTournamentId() {
            return tournamentId;
        }

        public boolean isHomeMatch() {
            return isHomeMatch;
        }
    }

    public static class UniversityRanking {
        private int rank;
        private String universityId;
        private String universityName;
        private int points;
        private int matchesPlayed;
        private int matchesWon;
        private int matchesLost;
        private int matchesTied;
        private String section; // A or B

        public UniversityRanking(int rank, String universityId, String universityName,
                                 int points, int matchesPlayed, int matchesWon,
                                 int matchesLost, int matchesTied, String section) {
            this.rank = rank;
            this.universityId = universityId;
            this.universityName = universityName;
            this.points = points;
            this.matchesPlayed = matchesPlayed;
            this.matchesWon = matchesWon;
            this.matchesLost = matchesLost;
            this.matchesTied = matchesTied;
            this.section = section;
        }

        // Getters
        public int getRank() {
            return rank;
        }

        public String getUniversityId() {
            return universityId;
        }

        public String getUniversityName() {
            return universityName;
        }

        public int getPoints() {
            return points;
        }

        public int getMatchesPlayed() {
            return matchesPlayed;
        }

        public int getMatchesWon() {
            return matchesWon;
        }

        public int getMatchesLost() {
            return matchesLost;
        }

        public int getMatchesTied() {
            return matchesTied;
        }

        public String getSection() {
            return section;
        }
    }

    // Public callback interfaces
    public interface FetchUniversitiesCallback {
        void onUniversitiesFetched(List<SouthAfricanUniversities> universities);

        void onError(String errorMessage);
    }

    public interface FetchVenuesCallback {
        void onVenuesFetched(List<SAVenue> venues);

        void onError(String errorMessage);
    }

    public interface FetchRankingsCallback {
        void onRankingsFetched(List<UniversityRanking> rankings);

        void onError(String errorMessage);
    }

    public interface FetchUpcomingMatchesCallback {
        void onMatchesFetched(List<UpcomingMatch> matches);

        void onError(String errorMessage);
    }

    public interface FetchTournamentsCallback {
        void onTournamentsFetched(List<USSATournament> tournaments);

        void onError(String errorMessage);
    }

    public interface FetchSPUPlayersCallback {
        void onPlayersFetched(List<Player> players);

        void onError(String errorMessage);
    }

    // Private constructor for singleton pattern
    private SouthAfricanUniversityCricketService(Context context) {
        this.context = context.getApplicationContext();

        // Initialize Firebase references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        uniCricketRef = database.getReference("university_cricket");
        ussaRankingsRef = uniCricketRef.child("rankings");
        venuesRef = uniCricketRef.child("venues");
        tournamentsRef = uniCricketRef.child("tournaments");
        spuSpecificRef = uniCricketRef.child("spu_data");

        // Initialize background executor
        backgroundExecutor = Executors.newFixedThreadPool(2);

        // Preload data
        preloadData();
    }

    /**
     * Get the singleton instance
     */
    public static synchronized SouthAfricanUniversityCricketService getInstance(Context context) {
        if (instance == null) {
            instance = new SouthAfricanUniversityCricketService(context);
        }
        return instance;
    }

    /**
     * Preload common universities, venues and tournaments data
     */
    private void preloadData() {
        // Load universities
        loadUniversities();

        // Load venues
        loadVenues();

        // Load tournaments
        loadTournaments();

        // Load SPU specific data
        loadSPUData();
    }

    /**
     * Load South African universities
     */
    private void loadUniversities() {
        // Load common universities
        universities.put(SouthAfricanUniversities.SPU,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.SPU));
        universities.put(SouthAfricanUniversities.UCT,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.UCT));
        universities.put(SouthAfricanUniversities.UWC,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.UWC));
        universities.put(SouthAfricanUniversities.SU,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.SU));
        universities.put(SouthAfricanUniversities.UP,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.UP));
        universities.put(SouthAfricanUniversities.UJ,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.UJ));
        universities.put(SouthAfricanUniversities.UKZN,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.UKZN));
        universities.put(SouthAfricanUniversities.NWU,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.NWU));
        universities.put(SouthAfricanUniversities.UFS,
                SouthAfricanUniversities.createPredefined(SouthAfricanUniversities.UFS));
    }

    /**
     * Load cricket venues
     */
    private void loadVenues() {
        // Load common cricket venues
        venues.put(SAVenue.SPU_CRICKET_OVAL,
                SAVenue.createPredefined(SAVenue.SPU_CRICKET_OVAL));
        venues.put(SAVenue.DIAMOND_OVAL,
                SAVenue.createPredefined(SAVenue.DIAMOND_OVAL));
        venues.put(SAVenue.KIMBERLEY_OVAL,
                SAVenue.createPredefined(SAVenue.KIMBERLEY_OVAL));
        venues.put(SAVenue.NORTHERN_CAPE_HS,
                SAVenue.createPredefined(SAVenue.NORTHERN_CAPE_HS));
        venues.put(SAVenue.GALESHEWE_CRICKET,
                SAVenue.createPredefined(SAVenue.GALESHEWE_CRICKET));
        venues.put(SAVenue.TUKS_OVAL,
                SAVenue.createPredefined(SAVenue.TUKS_OVAL));
        venues.put(SAVenue.MATIES_STADIUM,
                SAVenue.createPredefined(SAVenue.MATIES_STADIUM));
    }

    /**
     * Load tournaments
     */
    private void loadTournaments() {
        // Load common tournaments
        tournaments.put(USSATournament.USSA_CRICKET_A,
                USSATournament.createPredefined(USSATournament.USSA_CRICKET_A));
        tournaments.put(USSATournament.USSA_CRICKET_B,
                USSATournament.createPredefined(USSATournament.USSA_CRICKET_B));
        tournaments.put(USSATournament.VARSITY_CRICKET,
                USSATournament.createPredefined(USSATournament.VARSITY_CRICKET));
        tournaments.put(USSATournament.SPU_CRICKET_WEEK,
                USSATournament.createPredefined(USSATournament.SPU_CRICKET_WEEK));
        tournaments.put(USSATournament.NC_UNIVERSITY_LEAGUE,
                USSATournament.createPredefined(USSATournament.NC_UNIVERSITY_LEAGUE));

        // Also load from Firebase
        tournamentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot tournamentSnapshot : snapshot.getChildren()) {
                    try {
                        USSATournament tournament = tournamentSnapshot.getValue(USSATournament.class);
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
     * Load SPU specific data
     */
    private void loadSPUData() {
        // Load SPU players
        spuSpecificRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                        try {
                            Player player = playerSnapshot.getValue(Player.class);
                            if (player != null && player.getId() != null) {
                                spuPlayers.put(player.getId(), player);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing SPU player data", e);
                        }
                    }
                    Log.d(TAG, "Loaded " + spuPlayers.size() + " SPU players");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading SPU players", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading SPU player data", error.toException());
            }
        });

        // Load upcoming matches
        loadUpcomingMatches();
    }

    /**
     * Load upcoming matches for SPU
     */
    private void loadUpcomingMatches() {
        spuSpecificRef.child("upcoming_matches").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    upcomingMatches.clear();

                    for (DataSnapshot matchSnapshot : snapshot.getChildren()) {
                        try {
                            String id = matchSnapshot.getKey();
                            String homeTeamId = matchSnapshot.child("homeTeam").getValue(String.class);
                            String awayTeamId = matchSnapshot.child("awayTeam").getValue(String.class);
                            String venueId = matchSnapshot.child("venue").getValue(String.class);
                            long timestamp = matchSnapshot.child("date").getValue(Long.class);
                            String matchType = matchSnapshot.child("matchType").getValue(String.class);
                            String tournamentId = matchSnapshot.child("tournament").getValue(String.class);

                            Date matchDate = new Date(timestamp);

                            UpcomingMatch match = new UpcomingMatch(id, homeTeamId, awayTeamId, venueId,
                                    matchDate, matchType, tournamentId);
                            upcomingMatches.add(match);

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing match data", e);
                        }
                    }

                    Log.d(TAG, "Loaded " + upcomingMatches.size() + " upcoming matches");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading upcoming matches", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading upcoming matches", error.toException());
            }
        });

        // If no matches in Firebase, create some sample upcoming matches for SPU
        if (upcomingMatches.isEmpty()) {
            createSampleUpcomingMatches();
        }
    }

    /**
     * Create sample upcoming matches for demo purposes
     */
    private void createSampleUpcomingMatches() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            // Create some example matches
            upcomingMatches.add(new UpcomingMatch(
                    "match1",
                    SouthAfricanUniversities.SPU,
                    SouthAfricanUniversities.CUT,
                    SAVenue.SPU_CRICKET_OVAL,
                    dateFormat.parse("2023-10-15"),
                    "50-over",
                    USSATournament.NC_UNIVERSITY_LEAGUE
            ));

            upcomingMatches.add(new UpcomingMatch(
                    "match2",
                    SouthAfricanUniversities.UFS,
                    SouthAfricanUniversities.SPU,
                    SAVenue.DIAMOND_OVAL,
                    dateFormat.parse("2023-10-22"),
                    "T20",
                    USSATournament.NC_UNIVERSITY_LEAGUE
            ));

            upcomingMatches.add(new UpcomingMatch(
                    "match3",
                    SouthAfricanUniversities.SPU,
                    SouthAfricanUniversities.NWU,
                    SAVenue.SPU_CRICKET_OVAL,
                    dateFormat.parse("2023-11-05"),
                    "50-over",
                    USSATournament.NC_UNIVERSITY_LEAGUE
            ));

            upcomingMatches.add(new UpcomingMatch(
                    "match4",
                    SouthAfricanUniversities.UKZN,
                    SouthAfricanUniversities.SPU,
                    null, // Venue not set yet
                    dateFormat.parse("2023-12-02"),
                    "Mixed",
                    USSATournament.USSA_CRICKET_B
            ));
        } catch (Exception e) {
            Log.e(TAG, "Error creating sample matches", e);
        }
    }

    /**
     * Get university by ID
     */
    public SouthAfricanUniversities getUniversity(String universityId) {
        return universities.get(universityId);
    }

    /**
     * Get SPU university data
     */
    public SouthAfricanUniversities getSPU() {
        return universities.get(SouthAfricanUniversities.SPU);
    }

    /**
     * Get all universities
     */
    public List<SouthAfricanUniversities> getAllUniversities() {
        return new ArrayList<>(universities.values());
    }

    /**
     * Get venue by ID
     */
    public SAVenue getVenue(String venueId) {
        return venues.get(venueId);
    }

    /**
     * Get all venues
     */
    public List<SAVenue> getAllVenues() {
        return new ArrayList<>(venues.values());
    }

    /**
     * Get Kimberley venues
     */
    public List<SAVenue> getKimberleyVenues() {
        List<SAVenue> kimberleyVenues = new ArrayList<>();
        for (SAVenue venue : venues.values()) {
            if ("Kimberley".equalsIgnoreCase(venue.getCity())) {
                kimberleyVenues.add(venue);
            }
        }
        return kimberleyVenues;
    }

    /**
     * Get tournament by ID
     */
    public USSATournament getTournament(String tournamentId) {
        return tournaments.get(tournamentId);
    }

    /**
     * Get all tournaments
     */
    public List<USSATournament> getAllTournaments() {
        return new ArrayList<>(tournaments.values());
    }

    /**
     * Get active tournaments
     */
    public List<USSATournament> getActiveTournaments() {
        List<USSATournament> activeTournaments = new ArrayList<>();
        for (USSATournament tournament : tournaments.values()) {
            if (tournament.isCurrentlyActive()) {
                activeTournaments.add(tournament);
            }
        }
        return activeTournaments;
    }

    /**
     * Get tournaments where SPU is participating
     */
    public List<USSATournament> getSPUTournaments() {
        List<USSATournament> spuTournaments = new ArrayList<>();
        for (USSATournament tournament : tournaments.values()) {
            List<String> participants = tournament.getParticipatingUniversityIds();
            if (participants != null && participants.contains(SouthAfricanUniversities.SPU)) {
                spuTournaments.add(tournament);
            }
        }
        return spuTournaments;
    }

    /**
     * Get USSA cricket rankings
     */
    public void getUSSARankings(String section, FetchRankingsCallback callback) {
        // Check Firebase for ranking data
        ussaRankingsRef.child(section).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<UniversityRanking> rankings = new ArrayList<>();

                    for (DataSnapshot rankSnapshot : snapshot.getChildren()) {
                        try {
                            Map<String, Object> rankData = (Map<String, Object>) rankSnapshot.getValue();
                            if (rankData != null) {
                                int rank = rankData.containsKey("rank") ?
                                        ((Long) rankData.get("rank")).intValue() : 0;
                                String universityId = (String) rankData.get("universityId");
                                String universityName = (String) rankData.get("universityName");
                                int points = rankData.containsKey("points") ?
                                        ((Long) rankData.get("points")).intValue() : 0;
                                int matchesPlayed = rankData.containsKey("matchesPlayed") ?
                                        ((Long) rankData.get("matchesPlayed")).intValue() : 0;
                                int matchesWon = rankData.containsKey("matchesWon") ?
                                        ((Long) rankData.get("matchesWon")).intValue() : 0;
                                int matchesLost = rankData.containsKey("matchesLost") ?
                                        ((Long) rankData.get("matchesLost")).intValue() : 0;
                                int matchesTied = rankData.containsKey("matchesTied") ?
                                        ((Long) rankData.get("matchesTied")).intValue() : 0;

                                UniversityRanking ranking = new UniversityRanking(
                                        rank, universityId, universityName, points,
                                        matchesPlayed, matchesWon, matchesLost, matchesTied, section
                                );
                                rankings.add(ranking);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing ranking data", e);
                        }
                    }

                    if (!rankings.isEmpty()) {
                        callback.onRankingsFetched(rankings);
                    } else {
                        // If no rankings in Firebase, create sample rankings
                        List<UniversityRanking> sampleRankings = createSampleRankings(section);
                        callback.onRankingsFetched(sampleRankings);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching rankings", e);
                    callback.onError("Error fetching rankings: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Rankings fetch cancelled", error.toException());
                callback.onError("Error fetching rankings: " + error.getMessage());

                // Return sample data as fallback
                List<UniversityRanking> sampleRankings = createSampleRankings(section);
                callback.onRankingsFetched(sampleRankings);
            }
        });
    }

    /**
     * Create sample rankings for demo purposes
     */
    private List<UniversityRanking> createSampleRankings(String section) {
        List<UniversityRanking> rankings = new ArrayList<>();

        if ("A".equals(section)) {
            rankings.add(new UniversityRanking(1, SouthAfricanUniversities.SU, "Stellenbosch University", 36, 12, 9, 2, 1, "A"));
            rankings.add(new UniversityRanking(2, SouthAfricanUniversities.UP, "University of Pretoria", 32, 12, 8, 3, 1, "A"));
            rankings.add(new UniversityRanking(3, SouthAfricanUniversities.UCT, "University of Cape Town", 28, 12, 7, 4, 1, "A"));
            rankings.add(new UniversityRanking(4, SouthAfricanUniversities.UJ, "University of Johannesburg", 24, 12, 6, 6, 0, "A"));
            rankings.add(new UniversityRanking(5, SouthAfricanUniversities.UWC, "University of Western Cape", 20, 12, 5, 7, 0, "A"));
            rankings.add(new UniversityRanking(6, SouthAfricanUniversities.NWU, "North-West University", 16, 12, 4, 8, 0, "A"));
            rankings.add(new UniversityRanking(7, SouthAfricanUniversities.UKZN, "University of KwaZulu-Natal", 12, 12, 3, 9, 0, "A"));
            rankings.add(new UniversityRanking(8, SouthAfricanUniversities.UFS, "University of Free State", 8, 12, 2, 9, 1, "A"));
        } else if ("B".equals(section)) {
            rankings.add(new UniversityRanking(1, SouthAfricanUniversities.UKZN, "University of KwaZulu-Natal", 32, 12, 8, 3, 1, "B"));
            rankings.add(new UniversityRanking(2, SouthAfricanUniversities.SPU, "Sol Plaatje University", 28, 12, 7, 4, 1, "B"));
            rankings.add(new UniversityRanking(3, SouthAfricanUniversities.WITS, "University of Witwatersrand", 24, 12, 6, 6, 0, "B"));
            rankings.add(new UniversityRanking(4, SouthAfricanUniversities.NMMU, "Nelson Mandela University", 20, 12, 5, 7, 0, "B"));
            rankings.add(new UniversityRanking(5, SouthAfricanUniversities.UFH, "University of Fort Hare", 16, 12, 4, 8, 0, "B"));
            rankings.add(new UniversityRanking(6, SouthAfricanUniversities.UL, "University of Limpopo", 12, 12, 3, 9, 0, "B"));
            rankings.add(new UniversityRanking(7, SouthAfricanUniversities.DUT, "Durban University of Technology", 8, 12, 2, 9, 1, "B"));
            rankings.add(new UniversityRanking(8, SouthAfricanUniversities.UMP, "University of Mpumalanga", 4, 12, 1, 10, 1, "B"));
        }

        return rankings;
    }

    /**
     * Get upcoming SPU cricket matches
     */
    public void getUpcomingSPUMatches(FetchUpcomingMatchesCallback callback) {
        // Check if we have cached data
        if (!upcomingMatches.isEmpty()) {
            callback.onMatchesFetched(upcomingMatches);
            return;
        }

        // Otherwise load from Firebase (or use sample data)
        loadUpcomingMatches();

        // Return whatever we have (might be sample data)
        if (!upcomingMatches.isEmpty()) {
            callback.onMatchesFetched(upcomingMatches);
        } else {
            callback.onError("No upcoming matches found");
        }
    }

    /**
     * Get SPU cricket players
     */
    public void getSPUPlayers(FetchSPUPlayersCallback callback) {
        // Check if we have cached data
        if (!spuPlayers.isEmpty()) {
            callback.onPlayersFetched(new ArrayList<>(spuPlayers.values()));
            return;
        }

        // Otherwise load from Firebase
        spuSpecificRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    spuPlayers.clear();
                    List<Player> players = new ArrayList<>();

                    for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                        try {
                            Player player = playerSnapshot.getValue(Player.class);
                            if (player != null && player.getId() != null) {
                                spuPlayers.put(player.getId(), player);
                                players.add(player);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing player data", e);
                        }
                    }

                    if (!players.isEmpty()) {
                        callback.onPlayersFetched(players);
                    } else {
                        // If no players in Firebase, create sample players
                        createSampleSPUPlayers();
                        callback.onPlayersFetched(new ArrayList<>(spuPlayers.values()));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading SPU players", e);
                    callback.onError("Error loading SPU players: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading SPU players", error.toException());
                callback.onError("Error loading SPU players: " + error.getMessage());
            }
        });
    }

    /**
     * Create sample SPU players for demo purposes
     */
    private void createSampleSPUPlayers() {
        // Create sample players
        Player player1 = new Player("Thabo Mokoena", Player.PlayerRole.BATSMAN);
        player1.setId("spu_player1");
        Player.BattingStats stats1 = player1.getBattingStats();
        stats1.setRuns(342);
        stats1.setBallsFaced(423);
        stats1.setFours(36);
        stats1.setSixes(12);

        Player player2 = new Player("Sipho Ndlovu", Player.PlayerRole.BOWLER);
        player2.setId("spu_player2");
        Player.BowlingStats bstats2 = player2.getBowlingStats();
        bstats2.setWickets(23);
        bstats2.setRunsConceded(356);
        bstats2.setBalls(432);

        Player player3 = new Player("John Williams", Player.PlayerRole.ALL_ROUNDER);
        player3.setId("spu_player3");
        Player.BattingStats stats3 = player3.getBattingStats();
        stats3.setRuns(256);
        stats3.setBallsFaced(301);
        stats3.setFours(22);
        stats3.setSixes(8);
        Player.BowlingStats bstats3 = player3.getBowlingStats();
        bstats3.setWickets(18);
        bstats3.setRunsConceded(287);
        bstats3.setBalls(366);

        Player player4 = new Player("David Malan", Player.PlayerRole.WICKET_KEEPER);
        player4.setId("spu_player4");
        Player.BattingStats stats4 = player4.getBattingStats();
        stats4.setRuns(214);
        stats4.setBallsFaced(267);
        stats4.setFours(18);
        stats4.setSixes(5);

        spuPlayers.put(player1.getId(), player1);
        spuPlayers.put(player2.getId(), player2);
        spuPlayers.put(player3.getId(), player3);
        spuPlayers.put(player4.getId(), player4);
    }
}