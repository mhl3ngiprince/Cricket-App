package com.finedine.spucricketclub.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.finedine.spucricketclub.cricket.Player.PlayerRole;
import com.finedine.spucricketclub.data.db.converter.JsonConverter;

import java.util.Date;
import java.util.Map;

/**
 * Entity representing a cricket player in the database
 */
@Entity(tableName = "players")
public class PlayerEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String teamId;
    private String role; // Stored as string representation of PlayerRole enum
    private int jerseyNumber;
    private Date dateRegistered;
    private String photoUrl;
    private String faceEmbeddingRef;
    @TypeConverters(JsonConverter.class)
    private Map<String, Object> battingStats;
    @TypeConverters(JsonConverter.class)
    private Map<String, Object> bowlingStats;
    @TypeConverters(JsonConverter.class)
    private Map<String, Object> fieldingStats;

    // Default constructor required by Room
    public PlayerEntity() {
    }

    @Ignore
    public PlayerEntity(@NonNull String id, String name, String teamId, String role) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
        this.role = role;
        this.dateRegistered = new Date();
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

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public PlayerRole getRoleEnum() {
        try {
            return PlayerRole.valueOf(role);
        } catch (Exception e) {
            return PlayerRole.BATSMAN; // Default
        }
    }

    public void setRoleEnum(PlayerRole playerRole) {
        this.role = playerRole.name();
    }

    public int getJerseyNumber() {
        return jerseyNumber;
    }

    public void setJerseyNumber(int jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
    }

    public Date getDateRegistered() {
        return dateRegistered;
    }

    public void setDateRegistered(Date dateRegistered) {
        this.dateRegistered = dateRegistered;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getFaceEmbeddingRef() {
        return faceEmbeddingRef;
    }

    public void setFaceEmbeddingRef(String faceEmbeddingRef) {
        this.faceEmbeddingRef = faceEmbeddingRef;
    }

    public Map<String, Object> getBattingStats() {
        return battingStats;
    }

    public void setBattingStats(Map<String, Object> battingStats) {
        this.battingStats = battingStats;
    }

    public Map<String, Object> getBowlingStats() {
        return bowlingStats;
    }

    public void setBowlingStats(Map<String, Object> bowlingStats) {
        this.bowlingStats = bowlingStats;
    }

    public Map<String, Object> getFieldingStats() {
        return fieldingStats;
    }

    public void setFieldingStats(Map<String, Object> fieldingStats) {
        this.fieldingStats = fieldingStats;
    }
}
