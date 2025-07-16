package com.finedine.spucricketclub.cricket;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cricket over with all required data
 */
public class Over {
    private int overNumber;
    private Player bowler;
    private List<Ball> balls;
    private int runsScored;
    private int wickets;
    private boolean isMaiden;

    // Default constructor
    public Over() {
        this.balls = new ArrayList<>();
        this.runsScored = 0;
        this.wickets = 0;
        this.isMaiden = false;
    }

    // Constructor with over number and bowler
    public Over(int overNumber, Player bowler) {
        this();
        this.overNumber = overNumber;
        this.bowler = bowler;
    }

    // Add a ball to the over
    public void addBall(Ball ball) {
        balls.add(ball);
        runsScored += ball.getRunsScored();

        if (ball.isWicket()) {
            wickets++;
        }

        // Check if the over is complete and if it's a maiden
        if (balls.size() == 6 && runsScored == 0) {
            isMaiden = true;
        }
    }

    // Get the number of balls in this over
    public int getBallCount() {
        return balls.size();
    }

    // Check if this over is complete (6 legal deliveries)
    public boolean isComplete() {
        return balls.size() >= 6;
    }

    // Get a string representation of this over (e.g. "1 2 W 0 4 0")
    public String getOverSummary() {
        StringBuilder summary = new StringBuilder();
        for (Ball ball : balls) {
            if (ball.isWicket()) {
                summary.append("W ");
            } else {
                summary.append(ball.getRunsScored()).append(" ");
            }
        }
        return summary.toString().trim();
    }

    // Getters and setters
    public int getOverNumber() {
        return overNumber;
    }

    public void setOverNumber(int overNumber) {
        this.overNumber = overNumber;
    }

    public Player getBowler() {
        return bowler;
    }

    public void setBowler(Player bowler) {
        this.bowler = bowler;
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public void setBalls(List<Ball> balls) {
        this.balls = balls;

        // Recalculate runs and wickets
        runsScored = 0;
        wickets = 0;
        for (Ball ball : balls) {
            runsScored += ball.getRunsScored();
            if (ball.isWicket()) {
                wickets++;
            }
        }

        // Check if maiden
        isMaiden = (balls.size() == 6 && runsScored == 0);
    }

    public int getRunsScored() {
        return runsScored;
    }

    public int getWickets() {
        return wickets;
    }

    public boolean isMaiden() {
        return isMaiden;
    }

    public void setMaiden(boolean maiden) {
        isMaiden = maiden;
    }
}