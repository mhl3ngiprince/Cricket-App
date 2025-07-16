package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.UUID;

/**
 * Represents a cricket player with all required data
 */
@Keep
@IgnoreExtraProperties
public class Player {
    private String id;
    private String name;
    private String teamId;
    private PlayerRole role;
    private int jerseyNumber;
    private BattingStats battingStats;
    private BowlingStats bowlingStats;
    private BattingStatus battingStatus;
    private boolean isBowler;

    public enum PlayerRole {
        BATSMAN, BOWLER, ALL_ROUNDER, WICKET_KEEPER
    }

    public enum BattingStatus {
        NOT_BATTED, BATTING, OUT, RETIRED
    }

    // Default constructor
    public Player() {
        this.id = UUID.randomUUID().toString();
        this.battingStats = new BattingStats();
        this.bowlingStats = new BowlingStats();
        this.battingStatus = BattingStatus.NOT_BATTED;
    }

    // Constructor with name
    public Player(String name) {
        this();
        this.name = name;
    }

    // Constructor with name and role
    public Player(String name, PlayerRole role) {
        this(name);
        this.role = role;
        this.isBowler = (role == PlayerRole.BOWLER || role == PlayerRole.ALL_ROUNDER);
    }

    // Add runs to player's batting stats
    public void addRuns(int runs) {
        battingStats.addRuns(runs);
    }

    // Mark player as out
    public void setOut() {
        this.battingStatus = BattingStatus.OUT;
    }

    // Start batting
    public void startBatting() {
        this.battingStatus = BattingStatus.BATTING;
    }

    // Calculate strike rate
    public float calculateStrikeRate() {
        return battingStats.calculateStrikeRate();
    }

    // Add a ball bowled
    public void addBall() {
        bowlingStats.addBall();
    }

    // Add runs conceded
    public void addRunsConceded(int runs) {
        bowlingStats.addRunsConceded(runs);
    }

    // Add wicket taken
    public void addWicket() {
        bowlingStats.addWicket();
    }

    // Calculate bowling economy rate
    public float calculateEconomyRate() {
        return bowlingStats.calculateEconomyRate();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
        this.isBowler = (role == PlayerRole.BOWLER || role == PlayerRole.ALL_ROUNDER);
    }

    public int getJerseyNumber() {
        return jerseyNumber;
    }

    public void setJerseyNumber(int jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
    }

    public BattingStats getBattingStats() {
        return battingStats;
    }

    public void setBattingStats(BattingStats battingStats) {
        this.battingStats = battingStats;
    }

    public BowlingStats getBowlingStats() {
        return bowlingStats;
    }

    public void setBowlingStats(BowlingStats bowlingStats) {
        this.bowlingStats = bowlingStats;
    }

    public BattingStatus getBattingStatus() {
        return battingStatus;
    }

    public void setBattingStatus(BattingStatus battingStatus) {
        this.battingStatus = battingStatus;
    }

    public boolean isBowler() {
        return isBowler;
    }

    public void setBowler(boolean bowler) {
        isBowler = bowler;
    }

    /**
     * Inner class for batting statistics
     */
    @Keep
    @IgnoreExtraProperties
    public static class BattingStats {
        private int runs;
        private int ballsFaced;
        private int fours;
        private int sixes;

        public BattingStats() {
            this.runs = 0;
            this.ballsFaced = 0;
            this.fours = 0;
            this.sixes = 0;
        }

        public void addRuns(int runsScored) {
            this.runs += runsScored;
            this.ballsFaced++;

            if (runsScored == 4) {
                this.fours++;
            } else if (runsScored == 6) {
                this.sixes++;
            }
        }

        public void addBallFaced() {
            this.ballsFaced++;
        }

        public float calculateStrikeRate() {
            if (ballsFaced > 0) {
                return (runs * 100.0f) / ballsFaced;
            }
            return 0;
        }

        // Getters and setters
        public int getRuns() {
            return runs;
        }

        public void setRuns(int runs) {
            this.runs = runs;
        }

        public int getBallsFaced() {
            return ballsFaced;
        }

        public void setBallsFaced(int ballsFaced) {
            this.ballsFaced = ballsFaced;
        }

        public int getFours() {
            return fours;
        }

        public void setFours(int fours) {
            this.fours = fours;
        }

        public int getSixes() {
            return sixes;
        }

        public void setSixes(int sixes) {
            this.sixes = sixes;
        }
    }

    /**
     * Inner class for bowling statistics
     */
    @Keep
    @IgnoreExtraProperties
    public static class BowlingStats {
        private int wickets;
        private int runsConceded;
        private int balls;
        private int overs; // Added field for full overs bowled
        private int maidens;

        public BowlingStats() {
            this.wickets = 0;
            this.runsConceded = 0;
            this.balls = 0;
            this.maidens = 0;
        }

        public void addWicket() {
            this.wickets++;
        }

        public void addRunsConceded(int runs) {
            this.runsConceded += runs;
        }

        public void addBall() {
            this.balls++;
        }

        public void addMaiden() {
            this.maidens++;
        }

        public int getOvers() {
            // Return the dedicated overs field if you track full overs separately
            // or calculate from balls if 'overs' field is not for full overs.
            return overs;
        }

        public void setOvers(int overs) {
            this.overs = overs;
            // If you are setting full overs and also tracking total balls,
            // you might want to update 'this.balls' as well, e.g., this.balls = overs * 6;
            // However, the current logic in convertEntityToPlayer seems to expect 'overs' to be set directly.
        }

        public int getBallsInCurrentOver() {
            return balls % 6;
        }

        public float calculateEconomyRate() {
            float overs = balls / 6.0f;
            if (overs > 0) {
                return runsConceded / overs;
            }
            return 0;
        }

        // Getters and setters
        public int getWickets() {
            return wickets;
        }

        public void setWickets(int wickets) {
            this.wickets = wickets;
        }

        public int getRunsConceded() {
            return runsConceded;
        }

        public void setRunsConceded(int runsConceded) {
            this.runsConceded = runsConceded;
        }

        public int getBalls() {
            return balls;
        }

        public void setBalls(int balls) {
            this.balls = balls;
        }

        public int getMaidens() {
            return maidens;
        }

        public void setMaidens(int maidens) {
            this.maidens = maidens;
        }
    }
}
