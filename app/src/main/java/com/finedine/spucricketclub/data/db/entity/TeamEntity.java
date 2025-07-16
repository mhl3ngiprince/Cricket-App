package com.finedine.spucricketclub.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.finedine.spucricketclub.data.db.converter.JsonConverter;

import java.util.Date;
import java.util.List;

/**
 * Entity representing a cricket team in the database
 */
@Entity(tableName = "teams")
public class TeamEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String teamName;
    private String shortName;
    private String logoUrl;
    private String coachName;
    private String captainId;
    private Date dateCreated;
    private Date lastUpdated;
    private String homeVenue;
    @TypeConverters(JsonConverter.class)
    private List<String> playerIds;
    private int wins;
    private int losses;
    private int draws;

    // Default constructor required by Room
    public TeamEntity() {
    }

    @androidx.room.Ignore
    public TeamEntity(@NonNull String id, String teamName) {
        this.id = id;
        this.teamName = teamName;
        this.dateCreated = new Date();
        this.lastUpdated = new Date();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCoachName() {
        return coachName;
    }

    public void setCoachName(String coachName) {
        this.coachName = coachName;
    }

    public String getCaptainId() {
        return captainId;
    }

    public void setCaptainId(String captainId) {
        this.captainId = captainId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getHomeVenue() {
        return homeVenue;
    }

    public void setHomeVenue(String homeVenue) {
        this.homeVenue = homeVenue;
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<String> playerIds) {
        this.playerIds = playerIds;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }
}
