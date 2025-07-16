package com.finedine.spucricketclub.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Team;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static FirebaseHelper instance;
    private final FirebaseDatabase database;
    private final DatabaseReference playersRef;
    private final DatabaseReference teamsRef;
    private final DatabaseReference matchesRef;
    private final DatabaseReference statsRef;

    private FirebaseHelper() {
        database = FirebaseDatabase.getInstance();
        playersRef = database.getReference("players");
        teamsRef = database.getReference("teams");
        matchesRef = database.getReference("matches");
        statsRef = database.getReference("stats");
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // Player methods
    public void savePlayer(Player player) {
        if (player.getId() == null || player.getId().isEmpty()) {
            String playerId = playersRef.push().getKey();
            player.setId(playerId);
        }
        playersRef.child(player.getId()).setValue(player);
    }

    public void getPlayers(final FirebaseCallback<List<Player>> callback) {
        playersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Player> playerList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Player player = snapshot.getValue(Player.class);
                    if (player != null) {
                        playerList.add(player);
                    }
                }
                callback.onSuccess(playerList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void getPlayer(String playerId, final FirebaseCallback<Player> callback) {
        playersRef.child(playerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Player player = snapshot.getValue(Player.class);
                callback.onSuccess(player);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // Team methods
    public void saveTeam(Team team) {
        if (team.getId() == null || team.getId().isEmpty()) {
            String teamId = teamsRef.push().getKey();
            team.setId(teamId);
        }
        teamsRef.child(team.getId()).setValue(team);
    }

    public void getTeams(final FirebaseCallback<List<Team>> callback) {
        teamsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Team> teamList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Team team = snapshot.getValue(Team.class);
                    if (team != null) {
                        teamList.add(team);
                    }
                }
                callback.onSuccess(teamList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // Match methods
    public void saveMatch(Match match) {
        if (match.getId() == null || match.getId().isEmpty()) {
            String matchId = matchesRef.push().getKey();
            match.setId(matchId);
        }
        matchesRef.child(match.getId()).setValue(match);
    }

    public void updateMatchInRealtime(String matchId, Map<String, Object> updates) {
        matchesRef.child(matchId).updateChildren(updates);
    }

    public void getMatches(final FirebaseCallback<List<Match>> callback) {
        matchesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Match> matchList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Match match = snapshot.getValue(Match.class);
                    if (match != null) {
                        matchList.add(match);
                    }
                }
                callback.onSuccess(matchList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void getMatch(String matchId, final FirebaseCallback<Match> callback) {
        matchesRef.child(matchId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Match match = snapshot.getValue(Match.class);
                callback.onSuccess(match);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // Stats methods
    public void savePlayerStats(String playerId, Map<String, Object> stats) {
        statsRef.child("players").child(playerId).updateChildren(stats);
    }

    public void updatePlayerStatsInRealtime(String playerId, String statType, Object value) {
        Map<String, Object> update = new HashMap<>();
        update.put(statType, value);
        statsRef.child("players").child(playerId).updateChildren(update);
    }

    public void getPlayerStats(String playerId, final FirebaseCallback<Map<String, Object>> callback) {
        statsRef.child("players").child(playerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> stats = new HashMap<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    stats.put(childSnapshot.getKey(), childSnapshot.getValue());
                }
                callback.onSuccess(stats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // Generic callback interface
    public interface FirebaseCallback<T> {
        void onSuccess(T result);

        void onError(String errorMessage);
    }
}