package com.finedine.spucricketclub.data.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.data.db.AppDatabase;
import com.finedine.spucricketclub.data.db.entity.PlayerEntity;
import com.finedine.spucricketclub.data.db.entity.TeamEntity;
import com.finedine.spucricketclub.data.db.entity.TournamentEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Worker class to seed the database with initial data
 */
public class DatabaseSeedWorker extends Worker {
    private static final String TAG = "DatabaseSeedWorker";

    public DatabaseSeedWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());

            // Only seed if database is empty
            if (database.playerDao().getCount() == 0) {
                Log.d(TAG, "Seeding database with initial data");

                // Seed teams
                List<TeamEntity> teams = createSampleTeams();
                for (TeamEntity team : teams) {
                    database.teamDao().insert(team);
                }

                // Seed players
                List<PlayerEntity> players = createSamplePlayers(teams);
                database.playerDao().insertAll(players);

                // Update teams with player IDs
                updateTeamsWithPlayers(database, teams, players);

                // Seed tournaments
                TournamentEntity tournament = createSampleTournament(teams);
                database.tournamentDao().insert(tournament);

                Log.d(TAG, "Database seeding completed successfully");
            } else {
                Log.d(TAG, "Database already has data, skipping seeding");
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error seeding database", e);
            return Result.failure();
        }
    }

    /**
     * Create sample teams for the database
     */
    private List<TeamEntity> createSampleTeams() {
        List<TeamEntity> teams = new ArrayList<>();

        // SPU Cricket Club
        String spuId = UUID.randomUUID().toString();
        TeamEntity spuTeam = new TeamEntity(spuId, "SPU Cricket Club");
        spuTeam.setShortName("SPU");
        spuTeam.setCoachName("John Smith");
        spuTeam.setHomeVenue("SPU Cricket Ground");
        teams.add(spuTeam);

        // Opponent team
        String opponentId = UUID.randomUUID().toString();
        TeamEntity opponentTeam = new TeamEntity(opponentId, "City University");
        opponentTeam.setShortName("CU");
        opponentTeam.setCoachName("David Brown");
        opponentTeam.setHomeVenue("City University Stadium");
        teams.add(opponentTeam);

        return teams;
    }

    /**
     * Create sample players for the database
     */
    private List<PlayerEntity> createSamplePlayers(List<TeamEntity> teams) {
        List<PlayerEntity> players = new ArrayList<>();

        // Add players for SPU team
        String spuTeamId = teams.get(0).getId();

        PlayerEntity player1 = new PlayerEntity(UUID.randomUUID().toString(), "John Smith", spuTeamId, "BATSMAN");
        player1.setJerseyNumber(1);
        players.add(player1);

        PlayerEntity player2 = new PlayerEntity(UUID.randomUUID().toString(), "Michael Johnson", spuTeamId, "BATSMAN");
        player2.setJerseyNumber(7);
        players.add(player2);

        PlayerEntity player3 = new PlayerEntity(UUID.randomUUID().toString(), "David Warner", spuTeamId, "ALL_ROUNDER");
        player3.setJerseyNumber(3);
        players.add(player3);

        PlayerEntity player4 = new PlayerEntity(UUID.randomUUID().toString(), "Chris Morris", spuTeamId, "ALL_ROUNDER");
        player4.setJerseyNumber(8);
        players.add(player4);

        PlayerEntity player5 = new PlayerEntity(UUID.randomUUID().toString(), "Ryan Harris", spuTeamId, "BOWLER");
        player5.setJerseyNumber(11);
        players.add(player5);

        // Add players for opponent team
        String opponentTeamId = teams.get(1).getId();

        PlayerEntity player6 = new PlayerEntity(UUID.randomUUID().toString(), "Steve Brown", opponentTeamId, "BATSMAN");
        player6.setJerseyNumber(4);
        players.add(player6);

        PlayerEntity player7 = new PlayerEntity(UUID.randomUUID().toString(), "Mark Jones", opponentTeamId, "WICKET_KEEPER");
        player7.setJerseyNumber(1);
        players.add(player7);

        PlayerEntity player8 = new PlayerEntity(UUID.randomUUID().toString(), "Paul Williams", opponentTeamId, "BOWLER");
        player8.setJerseyNumber(9);
        players.add(player8);

        return players;
    }

    /**
     * Update teams with player IDs
     */
    private void updateTeamsWithPlayers(AppDatabase database, List<TeamEntity> teams, List<PlayerEntity> players) {
        // Group players by team ID
        for (TeamEntity team : teams) {
            List<String> playerIds = new ArrayList<>();
            for (PlayerEntity player : players) {
                if (player.getTeamId().equals(team.getId())) {
                    playerIds.add(player.getId());
                }
            }
            team.setPlayerIds(playerIds);
            database.teamDao().update(team);
        }
    }

    /**
     * Create a sample tournament
     */
    private TournamentEntity createSampleTournament(List<TeamEntity> teams) {
        String tournamentId = UUID.randomUUID().toString();
        TournamentEntity tournament = new TournamentEntity(tournamentId, "University Cricket Championship", "T20");

        // Set dates
        Date now = new Date();
        tournament.setStartDate(now);
        // Set end date to 1 month from now
        Date endDate = new Date(now.getTime() + 30L * 24 * 60 * 60 * 1000);
        tournament.setEndDate(endDate);

        // Add teams to tournament
        List<String> teamIds = new ArrayList<>();
        for (TeamEntity team : teams) {
            teamIds.add(team.getId());
        }
        tournament.setTeamIds(teamIds);

        tournament.setStatus("UPCOMING");
        tournament.setDescription("Annual university cricket tournament featuring the best university teams.");

        return tournament;
    }
}