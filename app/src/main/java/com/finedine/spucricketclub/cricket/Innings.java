package com.finedine.spucricketclub.cricket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a cricket innings with all required data
 */
public class Innings {
    private Team battingTeam;
    private Team bowlingTeam;
    private int totalScore;
    private int wickets;
    private int extras;
    private List<Over> overs;
    private Map<String, Player> currentBatsmen;
    private Player striker;
    private Player nonStriker;
    private Player currentBowler;
    private Over currentOver;
    private boolean isCompleted;

    // Default constructor
    public Innings() {
        this.overs = new ArrayList<>();
        this.currentBatsmen = new HashMap<>();
        this.totalScore = 0;
        this.wickets = 0;
        this.extras = 0;
        this.isCompleted = false;
    }

    // Constructor with teams
    public Innings(Team battingTeam, Team bowlingTeam) {
        this();
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
    }

    // Start a new over with a specific bowler
    public void startNewOver(Player bowler) {
        if (currentOver != null && currentOver.getBalls().size() < 6) {
            // Current over is not complete
            return;
        }

        this.currentBowler = bowler;
        this.currentOver = new Over(overs.size() + 1, bowler);
        overs.add(currentOver);

        // Switch strike at the end of over
        switchStrike();
    }

    // Add a ball to the current over
    public void addBall(Ball ball) {
        if (currentOver == null) {
            return;
        }

        currentOver.addBall(ball);

        // Update score and stats based on the ball
        updateScoreFromBall(ball);

        // Switch strike if necessary
        if (ball.getRunsScored() % 2 == 1) {
            switchStrike();
        }

        // Check if innings is completed
        checkInningsStatus();
    }

    // Update score based on a ball
    private void updateScoreFromBall(Ball ball) {
        // Update team score
        totalScore += ball.getRunsScored();

        // Update bowler stats
        if (currentBowler != null) {
            currentBowler.addBall();
            currentBowler.addRunsConceded(ball.getRunsScored());

            if (ball.isWicket()) {
                currentBowler.addWicket();
                wickets++;

                if (striker != null) {
                    striker.setOut();
                    currentBatsmen.remove(striker.getId());
                }
            }
        }

        // Update batsman stats
        if (striker != null && !ball.isWicket()) {
            striker.addRuns(ball.getRunsScored());
        }
    }

    // Switch the strike between batsmen
    public void switchStrike() {
        Player temp = striker;
        striker = nonStriker;
        nonStriker = temp;
    }

    // Set new batsman at striker end
    public void setNewBatsman(Player batsman) {
        this.striker = batsman;
        currentBatsmen.put(batsman.getId(), batsman);
        batsman.startBatting();
    }

    // Set new batsman at non-striker end
    public void setNewNonStriker(Player batsman) {
        this.nonStriker = batsman;
        currentBatsmen.put(batsman.getId(), batsman);
        batsman.startBatting();
    }

    // Check if innings is completed
    private void checkInningsStatus() {
        // Innings is completed if all wickets are down or all overs are bowled
        if (wickets >= 10 || getTotalOvers() >= 50) { // Assuming 50 overs match
            isCompleted = true;
        }
    }

    // Get total overs completed
    public float getTotalOvers() {
        int completedOvers = overs.size() - 1; // Exclude current over

        // Add balls from current over as decimal
        if (!overs.isEmpty() && currentOver != null) {
            float ballsAsDecimal = currentOver.getBallCount() / 10.0f;
            return completedOvers + ballsAsDecimal;
        }

        return completedOvers;
    }

    // Get current run rate
    public float getCurrentRunRate() {
        float totalOvers = getTotalOvers();
        if (totalOvers > 0) {
            return totalScore / totalOvers;
        }
        return 0;
    }

    // Getters and Setters
    public Team getBattingTeam() {
        return battingTeam;
    }

    public void setBattingTeam(Team battingTeam) {
        this.battingTeam = battingTeam;
    }

    public Team getBowlingTeam() {
        return bowlingTeam;
    }

    public void setBowlingTeam(Team bowlingTeam) {
        this.bowlingTeam = bowlingTeam;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getWickets() {
        return wickets;
    }

    public void setWickets(int wickets) {
        this.wickets = wickets;
    }

    public int getExtras() {
        return extras;
    }

    public void setExtras(int extras) {
        this.extras = extras;
    }

    public List<Over> getOvers() {
        return overs;
    }

    public void setOvers(List<Over> overs) {
        this.overs = overs;
    }

    public Map<String, Player> getCurrentBatsmen() {
        return currentBatsmen;
    }

    public void setCurrentBatsmen(Map<String, Player> currentBatsmen) {
        this.currentBatsmen = currentBatsmen;
    }

    public Player getStriker() {
        return striker;
    }

    public void setStriker(Player striker) {
        this.striker = striker;
    }

    public Player getNonStriker() {
        return nonStriker;
    }

    public void setNonStriker(Player nonStriker) {
        this.nonStriker = nonStriker;
    }

    public Player getCurrentBowler() {
        return currentBowler;
    }

    public void setCurrentBowler(Player currentBowler) {
        this.currentBowler = currentBowler;
    }

    public Over getCurrentOver() {
        return currentOver;
    }

    public void setCurrentOver(Over currentOver) {
        this.currentOver = currentOver;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}