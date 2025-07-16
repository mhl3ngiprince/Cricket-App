package com.finedine.spucricketclub.models;

import android.graphics.Bitmap;

/**
 * Model class for player statistics to be displayed in UI
 */
public class PlayerStats {
    private String name;
    private String role;
    private String statLine;
    private Bitmap playerImage;

    public PlayerStats(String name, String role, String statLine, Bitmap playerImage) {
        this.name = name;
        this.role = role;
        this.statLine = statLine;
        this.playerImage = playerImage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatLine() {
        return statLine;
    }

    public void setStatLine(String statLine) {
        this.statLine = statLine;
    }

    public Bitmap getPlayerImage() {
        return playerImage;
    }

    public void setPlayerImage(Bitmap playerImage) {
        this.playerImage = playerImage;
    }
}