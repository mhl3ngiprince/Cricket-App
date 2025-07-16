package com.finedine.spucricketclub.cricket;

import android.content.Context;
import android.util.Log;

import com.finedine.spucricketclub.data.FirebaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to manage cricket match data and operations
 */
public class CricketMatchService {
    private static final String TAG = "CricketMatchService";

    private static CricketMatchService instance;
    private Match currentMatch;
    private final Context context;
    private final FirebaseHelper firebaseHelper;

    // Private constructor for singleton pattern
    private CricketMatchService(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        this.firebaseHelper = FirebaseHelper.getInstance();
    }

    // Get singleton instance
    public static synchronized CricketMatchService getInstance(Context context) {
        if (instance == null) {
            instance = new CricketMatchService(context);
        }
        return instance;
    }

    // Create a new match with the given teams
    public Match createMatch(String teamA, String teamB, int overs) {
        currentMatch = new Match(teamA, teamB, overs);
        currentMatch.setStatus(Match.MatchStatus.IN_PROGRESS);

        // Initialize match with some players
        initializeTeamsWithPlayers();

        // Set initial batsmen and bowler
        initializeMatchPlay();

        // Save match to Firebase
        firebaseHelper.saveMatch(currentMatch);

        return currentMatch;
    }

    // Initialize teams with some sample players
    private void initializeTeamsWithPlayers() {
        if (currentMatch == null) {
            return;
        }

        Team battingTeam = currentMatch.getTeamBatting();
        Team bowlingTeam = currentMatch.getTeamBowling();

        // Add batsmen to batting team
        battingTeam.addPlayer(new Player("Player 1", Player.PlayerRole.BATSMAN));
        battingTeam.addPlayer(new Player("Player 2", Player.PlayerRole.BATSMAN));
        battingTeam.addPlayer(new Player("Player 3", Player.PlayerRole.ALL_ROUNDER));
        battingTeam.addPlayer(new Player("Player 4", Player.PlayerRole.BATSMAN));
        battingTeam.addPlayer(new Player("Player 5", Player.PlayerRole.WICKET_KEEPER));
        battingTeam.addPlayer(new Player("Player 6", Player.PlayerRole.ALL_ROUNDER));
        battingTeam.addPlayer(new Player("Player 7", Player.PlayerRole.BOWLER));
        battingTeam.addPlayer(new Player("Player 8", Player.PlayerRole.BOWLER));
        battingTeam.addPlayer(new Player("Player 9", Player.PlayerRole.BOWLER));
        battingTeam.addPlayer(new Player("Player 10", Player.PlayerRole.BOWLER));
        battingTeam.addPlayer(new Player("Player 11", Player.PlayerRole.BOWLER));

        // Add bowlers to bowling team
        bowlingTeam.addPlayer(new Player("Bowler 1", Player.PlayerRole.BOWLER));
        bowlingTeam.addPlayer(new Player("Bowler 2", Player.PlayerRole.BOWLER));
        bowlingTeam.addPlayer(new Player("Bowler 3", Player.PlayerRole.ALL_ROUNDER));
        bowlingTeam.addPlayer(new Player("Bowler 4", Player.PlayerRole.BOWLER));
        bowlingTeam.addPlayer(new Player("Bowler 5", Player.PlayerRole.WICKET_KEEPER));
        bowlingTeam.addPlayer(new Player("Bowler 6", Player.PlayerRole.ALL_ROUNDER));
        bowlingTeam.addPlayer(new Player("Bowler 7", Player.PlayerRole.BATSMAN));
        bowlingTeam.addPlayer(new Player("Bowler 8", Player.PlayerRole.BATSMAN));
        bowlingTeam.addPlayer(new Player("Bowler 9", Player.PlayerRole.BOWLER));
        bowlingTeam.addPlayer(new Player("Bowler 10", Player.PlayerRole.BATSMAN));
        bowlingTeam.addPlayer(new Player("Bowler 11", Player.PlayerRole.BATSMAN));
    }

    // Initialize match play with opening batsmen and bowler
    private void initializeMatchPlay() {
        if (currentMatch == null) {
            return;
        }

        Team battingTeam = currentMatch.getTeamBatting();
        Team bowlingTeam = currentMatch.getTeamBowling();
        Innings innings = currentMatch.getCurrentInnings();

        // Set opening batsmen
        if (!battingTeam.getPlayers().isEmpty()) {
            Player batsman1 = battingTeam.getPlayers().get(0);
            Player batsman2 = battingTeam.getPlayers().get(1);

            innings.setNewBatsman(batsman1);
            innings.setNewNonStriker(batsman2);
        }

        // Set opening bowler
        List<Player> bowlers = bowlingTeam.getBowlers();
        if (!bowlers.isEmpty()) {
            Player bowler = bowlers.get(0);
            innings.startNewOver(bowler);
        }

        // Update match in Firebase
        updateMatchInFirebase();
    }

    // Add runs to the current match
    public void addRuns(int runs) {
        if (currentMatch == null || runs < 0 || runs > 6) {
            return;
        }

        Innings innings = currentMatch.getCurrentInnings();
        Ball ball = new Ball(innings.getCurrentOver().getBalls().size() + 1, runs);
        innings.addBall(ball);

        // Update match in Firebase
        updateMatchInFirebase();

        Log.d(TAG, "Added " + runs + " runs. Current score: " +
                innings.getTotalScore() + "/" + innings.getWickets());
    }

    // Add a wicket to the current match
    public void addWicket() {
        if (currentMatch == null) {
            return;
        }

        Innings innings = currentMatch.getCurrentInnings();
        Ball ball = new Ball(innings.getCurrentOver().getBalls().size() + 1, 0, true, Ball.BallType.NORMAL);
        innings.addBall(ball);

        // Update match in Firebase
        updateMatchInFirebase();

        Log.d(TAG, "Wicket taken! Current score: " +
                innings.getTotalScore() + "/" + innings.getWickets());
    }

    // Complete the current over and start a new one
    public void completeOver() {
        if (currentMatch == null) {
            return;
        }

        Innings innings = currentMatch.getCurrentInnings();

        // Get the next bowler (for simplicity, we'll just use the next available bowler)
        Team bowlingTeam = innings.getBowlingTeam();
        List<Player> bowlers = bowlingTeam.getBowlers();

        if (!bowlers.isEmpty()) {
            // For simplicity, just pick a different bowler
            Player currentBowler = innings.getCurrentBowler();
            Player nextBowler = null;

            for (Player bowler : bowlers) {
                if (!bowler.equals(currentBowler)) {
                    nextBowler = bowler;
                    break;
                }
            }

            if (nextBowler == null) {
                nextBowler = bowlers.get(0);  // Use the first bowler if no other is available
            }

            innings.startNewOver(nextBowler);

            // Update match in Firebase
            updateMatchInFirebase();

            Log.d(TAG, "New over started. Bowler: " + nextBowler.getName());
        }
    }

    // End the current innings and switch teams
    public void endInnings() {
        if (currentMatch == null) {
            return;
        }

        currentMatch.getCurrentInnings().setCompleted(true);

        // Set target score for second innings
        if (currentMatch.getInnings().size() == 1) {
            Innings firstInnings = currentMatch.getInnings().get(0);
            currentMatch.setTargetScore(firstInnings.getTotalScore() + 1);
        }

        currentMatch.switchBattingTeam();
        initializeMatchPlay();

        // Update match in Firebase
        updateMatchInFirebase();

        Log.d(TAG, "Innings completed. Target: " + currentMatch.getTargetScore());
    }

    // End the current match
    public void endMatch() {
        if (currentMatch == null) {
            return;
        }

        currentMatch.setCompleted(true);
        currentMatch.setStatus(Match.MatchStatus.COMPLETED);

        // Determine winner based on scores
        if (currentMatch.getInnings().size() >= 2) {
            Innings firstInnings = currentMatch.getInnings().get(0);
            Innings secondInnings = currentMatch.getInnings().get(1);

            if (secondInnings.getTotalScore() > firstInnings.getTotalScore()) {
                Log.d(TAG, currentMatch.getTeamBatting().getTeamName() + " won by " +
                        (10 - secondInnings.getWickets()) + " wickets");
            } else {
                Log.d(TAG, currentMatch.getTeamBowling().getTeamName() + " won by " +
                        (firstInnings.getTotalScore() - secondInnings.getTotalScore()) + " runs");
            }
        }

        // Update match in Firebase
        updateMatchInFirebase();
    }

    // Get the current match
    public Match getCurrentMatch() {
        return currentMatch;
    }

    // Load a match from Firebase
    public void loadMatch(String matchId, final OnMatchLoadedListener listener) {
        firebaseHelper.getMatch(matchId, new FirebaseHelper.FirebaseCallback<Match>() {
            @Override
            public void onSuccess(Match match) {
                currentMatch = match;
                if (listener != null) {
                    listener.onMatchLoaded(match);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading match: " + errorMessage);
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    // Load all matches from Firebase
    public void loadAllMatches(final OnMatchesLoadedListener listener) {
        firebaseHelper.getMatches(new FirebaseHelper.FirebaseCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> matches) {
                if (listener != null) {
                    listener.onMatchesLoaded(matches);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading matches: " + errorMessage);
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    // Reset match data
    public void resetMatch() {
        currentMatch = null;
    }

    /**
     * Set a player as the active player in the match
     * This could be either a batsman or a bowler
     *
     * @param player The player to set as active
     */
    public void setActivePlayer(Player player) {
        if (currentMatch == null || player == null) {
            return;
        }

        Innings innings = currentMatch.getCurrentInnings();
        Team battingTeam = currentMatch.getTeamBatting();
        Team bowlingTeam = currentMatch.getTeamBowling();

        // Check if player is on batting team
        if (battingTeam.getPlayers().contains(player)) {
            // If player is not already batting, and we have less than 2 active batsmen
            if (player != innings.getStriker() && player != innings.getNonStriker()) {
                // If we need a new batsman
                if (innings.getStriker() == null) {
                    innings.setNewBatsman(player);
                    Log.d(TAG, "Set " + player.getName() + " as new striker");
                } else if (innings.getNonStriker() == null) {
                    innings.setNewNonStriker(player);
                    Log.d(TAG, "Set " + player.getName() + " as new non-striker");
                }
            }
        }
        // Check if player is on bowling team
        else if (bowlingTeam.getPlayers().contains(player)) {
            // Only set as bowler if different from current and it's a valid bowler
            if (player != innings.getCurrentBowler() && player.isBowler()) {
                // Don't change bowler mid-over
                if (innings.getCurrentOver().getBalls().size() == 0) {
                    innings.getCurrentOver().setBowler(player);
                    Log.d(TAG, "Set " + player.getName() + " as new bowler");
                }
            }
        }

        // Update match in Firebase
        updateMatchInFirebase();
    }

    // Update the current match in Firebase
    private void updateMatchInFirebase() {
        if (currentMatch != null) {
            firebaseHelper.saveMatch(currentMatch);
        }
    }

    // Update specific fields of the match in real-time
    public void updateMatchField(String field, Object value) {
        if (currentMatch != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put(field, value);
            firebaseHelper.updateMatchInRealtime(currentMatch.getId(), updates);
        }
    }

    // Interface for match loading callback
    public interface OnMatchLoadedListener {
        void onMatchLoaded(Match match);

        void onError(String errorMessage);
    }

    // Interface for matches loading callback
    public interface OnMatchesLoadedListener {
        void onMatchesLoaded(List<Match> matches);

        void onError(String errorMessage);
    }
}