package com.finedine.spucricketclub.models;

/**
 * Model class to hold win probability data for teams
 */
public class WinProbability {
    private String team1Name;
    private float team1Probability;
    private String team2Name;
    private float team2Probability;

    public WinProbability() {
    }

    public WinProbability(String teamName, float probability) {
        this.team1Name = teamName;
        this.team1Probability = probability;
        this.team2Name = "Opposition";
        this.team2Probability = 1 - probability;
    }

    /**
     * @return The name of team 1
     */
    public String getTeam1Name() {
        return team1Name;
    }

    /**
     * @param team1Name The name of team 1 to set
     */
    public void setTeam1Name(String team1Name) {
        this.team1Name = team1Name;
    }

    /**
     * @return The win probability for team 1 (0-1)
     */
    public float getTeam1Probability() {
        return team1Probability;
    }

    /**
     * @param team1Probability The probability to set
     */
    public void setTeam1Probability(float team1Probability) {
        this.team1Probability = team1Probability;
    }

    /**
     * @return Team 1 probability as a percentage string
     */
    public String getTeam1ProbabilityPercent() {
        return String.format("%.1f%%", team1Probability * 100);
    }

    /**
     * @return The name of team 2
     */
    public String getTeam2Name() {
        return team2Name;
    }

    /**
     * @param team2Name The name of team 2 to set
     */
    public void setTeam2Name(String team2Name) {
        this.team2Name = team2Name;
    }

    /**
     * @return The win probability for team 2 (0-1)
     */
    public float getTeam2Probability() {
        return team2Probability;
    }

    /**
     * @param team2Probability The probability to set
     */
    public void setTeam2Probability(float team2Probability) {
        this.team2Probability = team2Probability;
    }

    /**
     * @return Team 2 probability as a percentage string
     */
    public String getTeam2ProbabilityPercent() {
        return String.format("%.1f%%", team2Probability * 100);
    }

    /**
     * @return How close the match is (0-1), with higher values indicating a closer match
     */
    public float getMatchCloseness() {
        return 1.0f - Math.abs(team1Probability - team2Probability);
    }
}
