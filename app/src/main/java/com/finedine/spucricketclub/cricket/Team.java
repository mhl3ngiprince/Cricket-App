package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a cricket team with all required data
 */
public class Team {
    private String id;
    private String teamName;
    private String shortName;
    private List<Player> players;
    private Map<String, Player> playerMap; // For quick player lookup by ID

    // Default constructor
    public Team() {
        this.id = UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        this.playerMap = new HashMap<>();
    }

    // Constructor with team name
    public Team(String teamName) {
        this();
        this.teamName = teamName;
        this.shortName = generateShortName(teamName);
    }

    // Generate a short name from the team name (e.g., "Royal Challengers Bangalore" -> "RCB")
    private String generateShortName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        // If the name is already short, return it
        if (name.length() <= 3) {
            return name.toUpperCase();
        }

        // For names with spaces, use initials
        String[] words = name.split("\\s+");
        if (words.length > 1) {
            StringBuilder shortName = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    shortName.append(word.charAt(0));
                }
            }
            return shortName.toString().toUpperCase();
        }

        // For single word names, take first 3 letters
        return name.substring(0, Math.min(3, name.length())).toUpperCase();
    }

    // Add a player to the team
    public void addPlayer(Player player) {
        players.add(player);
        playerMap.put(player.getId(), player);
    }

    // Find a player by ID
    public Player getPlayerById(String playerId) {
        return playerMap.get(playerId);
    }

    // Find a player by name
    public Player findPlayerByName(String name) {
        for (Player player : players) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    // Get all batsmen in the team
    public List<Player> getBatsmen() {
        return players; // In cricket, typically all players can bat
    }

    // Get all bowlers in the team
    public List<Player> getBowlers() {
        List<Player> bowlers = new ArrayList<>();
        for (Player player : players) {
            if (player.isBowler()) {
                bowlers.add(player);
            }
        }
        return bowlers;
    }

    // Calculate team's total score
    public int calculateTotalScore() {
        int total = 0;
        for (Player player : players) {
            total += player.getBattingStats().getRuns();
        }
        return total;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
        // Rebuild player map
        playerMap.clear();
        for (Player player : players) {
            playerMap.put(player.getId(), player);
        }
    }
}