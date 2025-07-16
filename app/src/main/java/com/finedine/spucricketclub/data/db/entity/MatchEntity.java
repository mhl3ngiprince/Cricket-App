package com.finedine.spucricketclub.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.finedine.spucricketclub.data.db.converter.DateConverter;
import com.finedine.spucricketclub.data.db.converter.JsonConverter;

import java.util.Date;
import java.util.Map;

/**
 * Entity representing a cricket match in the database
 */
@Entity(tableName = "matches")
public class MatchEntity {
    @PrimaryKey
    @NonNull
    private String id;

    private String matchName;
    private String tournamentId;
    private String venueId;
    private String teamAId;
    private String teamBId;
    private String tossWinnerId;
    private String matchWinnerId;

    private boolean tossWinnerBatFirst;
    private int maxOvers;
    private int targetScore;

    private String status; // IN_PROGRESS, COMPLETED, SCHEDULED, ABANDONED

    private Date scheduledDate;
    private Date startTime;
    private Date endTime;

    @TypeConverters(JsonConverter.class)
    private Map<String, Object> innings1Data;

    @TypeConverters(JsonConverter.class)
    private Map<String, Object> innings2Data;

    @TypeConverters(JsonConverter.class)
    private Map<String, Object> matchStats;

    // Default constructor required by Room
    public MatchEntity() {
    }

    @Ignore
    public MatchEntity(@NonNull String id, String matchName, String teamAId, String teamBId, int maxOvers) {
        this.id = id;
        this.matchName = matchName;
        this.teamAId = teamAId;
        this.teamBId = teamBId;
        this.maxOvers = maxOvers;
        this.status = "SCHEDULED";
        this.scheduledDate = new Date();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getTeamAId() {
        return teamAId;
    }

    public void setTeamAId(String teamAId) {
        this.teamAId = teamAId;
    }

    public String getTeamBId() {
        return teamBId;
    }

    public void setTeamBId(String teamBId) {
        this.teamBId = teamBId;
    }

    public String getTossWinnerId() {
        return tossWinnerId;
    }

    public void setTossWinnerId(String tossWinnerId) {
        this.tossWinnerId = tossWinnerId;
    }

    public String getMatchWinnerId() {
        return matchWinnerId;
    }

    public void setMatchWinnerId(String matchWinnerId) {
        this.matchWinnerId = matchWinnerId;
    }

    public boolean isTossWinnerBatFirst() {
        return tossWinnerBatFirst;
    }

    public void setTossWinnerBatFirst(boolean tossWinnerBatFirst) {
        this.tossWinnerBatFirst = tossWinnerBatFirst;
    }

    public int getMaxOvers() {
        return maxOvers;
    }

    public void setMaxOvers(int maxOvers) {
        this.maxOvers = maxOvers;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Map<String, Object> getInnings1Data() {
        return innings1Data;
    }

    public void setInnings1Data(Map<String, Object> innings1Data) {
        this.innings1Data = innings1Data;
    }

    public Map<String, Object> getInnings2Data() {
        return innings2Data;
    }

    public void setInnings2Data(Map<String, Object> innings2Data) {
        this.innings2Data = innings2Data;
    }

    public Map<String, Object> getMatchStats() {
        return matchStats;
    }

    public void setMatchStats(Map<String, Object> matchStats) {
        this.matchStats = matchStats;
    }
}
