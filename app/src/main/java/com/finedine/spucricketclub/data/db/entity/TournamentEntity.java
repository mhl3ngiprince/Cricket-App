package com.finedine.spucricketclub.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.finedine.spucricketclub.data.db.converter.JsonConverter;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a cricket tournament in the database
 */
@Entity(tableName = "tournaments")
public class TournamentEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String format; // T20, ODI, TEST
    private Date startDate;
    private Date endDate;
    private String status; // UPCOMING, IN_PROGRESS, COMPLETED
    private String organizerId;
    private String description;
    private String logoUrl;

    @TypeConverters(JsonConverter.class)
    private List<String> teamIds;

    @TypeConverters(JsonConverter.class)
    private List<String> matchIds;

    @TypeConverters(JsonConverter.class)
    private List<String> venueIds;

    @TypeConverters(JsonConverter.class)
    private Map<String, Object> standings;

    // Default constructor required by Room
    public TournamentEntity() {
    }

    @Ignore
    public TournamentEntity(@NonNull String id, String name, String format) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.status = "UPCOMING";
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public List<String> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(List<String> teamIds) {
        this.teamIds = teamIds;
    }

    public List<String> getMatchIds() {
        return matchIds;
    }

    public void setMatchIds(List<String> matchIds) {
        this.matchIds = matchIds;
    }

    public List<String> getVenueIds() {
        return venueIds;
    }

    public void setVenueIds(List<String> venueIds) {
        this.venueIds = venueIds;
    }

    public Map<String, Object> getStandings() {
        return standings;
    }

    public void setStandings(Map<String, Object> standings) {
        this.standings = standings;
    }
}
