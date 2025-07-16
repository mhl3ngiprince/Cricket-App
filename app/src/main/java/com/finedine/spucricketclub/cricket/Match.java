package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a cricket match with all required data
 */
@Keep
@IgnoreExtraProperties
public class Match {
    private String id;
    private String matchName;
    private Date matchDate;
    private Team teamBatting;
    private Team teamBowling;
    private int targetScore;
    private int maxOvers;
    private boolean isCompleted;
    private String venue;
    private MatchType matchType;
    private List<Innings> innings;
    private MatchStatus status;

    public enum MatchType {
        T20, ODI, TEST, OTHER
    }

    public enum MatchStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED, ABANDONED, RAIN_AFFECTED
    }

    // Default constructor
    public Match() {
        this.id = UUID.randomUUID().toString();
        this.innings = new ArrayList<>();
        this.status = MatchStatus.NOT_STARTED;
    }

    // Constructor with team names
    public Match(String teamA, String teamB, int maxOvers) {
        this();
        this.teamBatting = new Team(teamA);
        this.teamBowling = new Team(teamB);
        this.maxOvers = maxOvers;
        this.matchType = determineMatchType(maxOvers);
        this.matchName = teamA + " vs " + teamB;
        this.matchDate = new Date();
    }

    // Add a new innings to the match
    public void addInnings(Innings innings) {
        this.innings.add(innings);
    }

    // Get the current innings
    public Innings getCurrentInnings() {
        if (innings.isEmpty()) {
            // Create first innings if none exists
            Innings firstInnings = new Innings(teamBatting, teamBowling);
            innings.add(firstInnings);
            return firstInnings;
        }
        return innings.get(innings.size() - 1);
    }

    // Switch batting team
    public void switchBattingTeam() {
        Team temp = teamBatting;
        teamBatting = teamBowling;
        teamBowling = temp;

        // Create new innings
        Innings newInnings = new Innings(teamBatting, teamBowling);
        innings.add(newInnings);
    }

    // Determine match type based on overs
    private MatchType determineMatchType(int overs) {
        if (overs <= 20) {
            return MatchType.T20;
        } else if (overs <= 50) {
            return MatchType.ODI;
        } else {
            return MatchType.TEST;
        }
    }

    // Calculate required run rate
    public float getRequiredRunRate() {
        if (innings.size() > 1) {
            Innings currentInnings = getCurrentInnings();
            int runsRemaining = targetScore - currentInnings.getTotalScore();
            float oversRemaining = maxOvers - currentInnings.getTotalOvers();
            if (oversRemaining > 0) {
                return runsRemaining / oversRemaining;
            }
        }
        return 0;
    }

    /**
     * Check if a player is in the current match
     *
     * @param player The player to check
     * @return true if the player is in either team, false otherwise
     */
    public boolean isPlayerInMatch(Player player) {
        if (player == null) {
            return false;
        }

        // Check batting team
        if (teamBatting != null) {
            for (Player p : teamBatting.getPlayers()) {
                if (p.getId().equals(player.getId())) {
                    return true;
                }
            }
        }

        // Check bowling team
        if (teamBowling != null) {
            for (Player p : teamBowling.getPlayers()) {
                if (p.getId().equals(player.getId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public Date getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(Date matchDate) {
        this.matchDate = matchDate;
    }

    public Team getTeamBatting() {
        return teamBatting;
    }

    public void setTeamBatting(Team teamBatting) {
        this.teamBatting = teamBatting;
    }

    public Team getTeamBowling() {
        return teamBowling;
    }

    public void setTeamBowling(Team teamBowling) {
        this.teamBowling = teamBowling;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
    }

    public int getMaxOvers() {
        return maxOvers;
    }

    public void setMaxOvers(int maxOvers) {
        this.maxOvers = maxOvers;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public List<Innings> getInnings() {
        return innings;
    }

    public void setInnings(List<Innings> innings) {
        this.innings = innings;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }
}