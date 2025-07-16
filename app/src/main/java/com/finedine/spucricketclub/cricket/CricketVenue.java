package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a cricket venue with all required data
 */
@Keep
@IgnoreExtraProperties
public class CricketVenue {
    private String id;
    private String name;
    private String city;
    private String country;
    private String countryCode;
    private String boardId;
    private double latitude;
    private double longitude;
    private int capacity;
    private int established;
    private String imageUrl;
    private boolean hasFloodlights;
    private List<String> pitchTypes; // e.g., "green", "dry", "dusty"
    private Map<String, Object> stats;
    private List<String> knownAs; // alternative names
    private float averageFirstInningsScore;
    private float averageSecondInningsScore;
    private float highestTeamScore;
    private float lowestTeamScore;
    private float highestIndividualScore;
    private boolean testVenue;
    private boolean odiVenue;
    private boolean t20Venue;

    // Famous venues constants for quick reference
    public static final String LORDS = "LORD";
    public static final String MCG = "MCG";
    public static final String SCG = "SCG";
    public static final String EDEN_GARDENS = "EDEN";
    public static final String WANKHEDE = "WANK";
    public static final String OVAL = "OVAL";
    public static final String WANDERERS = "WAND";
    public static final String NEWLANDS = "NEWL";

    // Default constructor
    public CricketVenue() {
        this.id = UUID.randomUUID().toString();
        this.pitchTypes = new ArrayList<>();
        this.stats = new HashMap<>();
        this.knownAs = new ArrayList<>();
    }

    // Constructor with name and location
    public CricketVenue(String name, String city, String country) {
        this();
        this.name = name;
        this.city = city;
        this.country = country;
    }

    /**
     * Factory method to create a predefined venue
     */
    public static CricketVenue createPredefined(String venueId) {
        CricketVenue venue = new CricketVenue();
        venue.id = venueId;

        switch (venueId) {
            case LORDS:
                venue.name = "Lord's Cricket Ground";
                venue.city = "London";
                venue.country = "England";
                venue.countryCode = "ENG";
                venue.boardId = CricketBoard.ECB;
                venue.capacity = 30000;
                venue.established = 1814;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Balanced", "Grass");
                venue.knownAs = List.of("Home of Cricket", "The Lord's");
                venue.testVenue = true;
                venue.odiVenue = true;
                venue.t20Venue = true;
                venue.latitude = 51.5299;
                venue.longitude = -0.1724;
                venue.averageFirstInningsScore = 320;
                venue.averageSecondInningsScore = 285;
                break;

            case MCG:
                venue.name = "Melbourne Cricket Ground";
                venue.city = "Melbourne";
                venue.country = "Australia";
                venue.countryCode = "AUS";
                venue.boardId = CricketBoard.CA;
                venue.capacity = 100024;
                venue.established = 1853;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Balanced", "Hard", "Bouncy");
                venue.knownAs = List.of("The G");
                venue.testVenue = true;
                venue.odiVenue = true;
                venue.t20Venue = true;
                venue.latitude = -37.8200;
                venue.longitude = 144.9839;
                venue.averageFirstInningsScore = 280;
                venue.averageSecondInningsScore = 240;
                break;

            case EDEN_GARDENS:
                venue.name = "Eden Gardens";
                venue.city = "Kolkata";
                venue.country = "India";
                venue.countryCode = "IND";
                venue.boardId = CricketBoard.BCCI;
                venue.capacity = 66349;
                venue.established = 1864;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Spin-friendly", "Slow");
                venue.testVenue = true;
                venue.odiVenue = true;
                venue.t20Venue = true;
                venue.latitude = 22.5646;
                venue.longitude = 88.3433;
                venue.averageFirstInningsScore = 270;
                venue.averageSecondInningsScore = 230;
                break;

            case WANDERERS:
                venue.name = "Wanderers Stadium";
                venue.city = "Johannesburg";
                venue.country = "South Africa";
                venue.countryCode = "RSA";
                venue.boardId = CricketBoard.CSA;
                venue.capacity = 34000;
                venue.established = 1956;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Bouncy", "Fast", "High scoring");
                venue.knownAs = List.of("The Bullring");
                venue.testVenue = true;
                venue.odiVenue = true;
                venue.t20Venue = true;
                venue.latitude = -26.1214;
                venue.longitude = 28.0599;
                venue.averageFirstInningsScore = 310;
                venue.averageSecondInningsScore = 260;
                venue.highestTeamScore = 438;
                break;

            case NEWLANDS:
                venue.name = "Newlands Cricket Ground";
                venue.city = "Cape Town";
                venue.country = "South Africa";
                venue.countryCode = "RSA";
                venue.boardId = CricketBoard.CSA;
                venue.capacity = 25000;
                venue.established = 1888;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Balanced", "Seam-friendly");
                venue.testVenue = true;
                venue.odiVenue = true;
                venue.t20Venue = true;
                venue.latitude = -33.9806;
                venue.longitude = 18.4588;
                break;

            default:
                venue.name = "Unknown Venue";
                venue.city = "Unknown City";
                venue.country = "Unknown Country";
                venue.countryCode = "UNK";
        }

        return venue;
    }

    /**
     * Add a statistic to this venue
     */
    public void addStat(String key, Object value) {
        if (key != null && value != null) {
            stats.put(key, value);
        }
    }

    /**
     * Add an alternative name for this venue
     */
    public void addAlternativeName(String name) {
        if (name != null && !knownAs.contains(name)) {
            knownAs.add(name);
        }
    }

    /**
     * Add a pitch type for this venue
     */
    public void addPitchType(String type) {
        if (type != null && !pitchTypes.contains(type)) {
            pitchTypes.add(type);
        }
    }

    /**
     * Returns a string summary of the venue
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(name)
                .append(" (").append(city).append(", ").append(country).append(")");

        if (established > 0) {
            sb.append(" - Est. ").append(established);
        }

        if (capacity > 0) {
            sb.append(", Capacity: ").append(String.format("%,d", capacity));
        }

        if (!pitchTypes.isEmpty()) {
            sb.append("\nPitch: ").append(String.join(", ", pitchTypes));
        }

        return sb.toString();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getEstablished() {
        return established;
    }

    public void setEstablished(int established) {
        this.established = established;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isHasFloodlights() {
        return hasFloodlights;
    }

    public void setHasFloodlights(boolean hasFloodlights) {
        this.hasFloodlights = hasFloodlights;
    }

    public List<String> getPitchTypes() {
        return pitchTypes;
    }

    public void setPitchTypes(List<String> pitchTypes) {
        this.pitchTypes = pitchTypes;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }

    public List<String> getKnownAs() {
        return knownAs;
    }

    public void setKnownAs(List<String> knownAs) {
        this.knownAs = knownAs;
    }

    public float getAverageFirstInningsScore() {
        return averageFirstInningsScore;
    }

    public void setAverageFirstInningsScore(float averageFirstInningsScore) {
        this.averageFirstInningsScore = averageFirstInningsScore;
    }

    public float getAverageSecondInningsScore() {
        return averageSecondInningsScore;
    }

    public void setAverageSecondInningsScore(float averageSecondInningsScore) {
        this.averageSecondInningsScore = averageSecondInningsScore;
    }

    public float getHighestTeamScore() {
        return highestTeamScore;
    }

    public void setHighestTeamScore(float highestTeamScore) {
        this.highestTeamScore = highestTeamScore;
    }

    public float getLowestTeamScore() {
        return lowestTeamScore;
    }

    public void setLowestTeamScore(float lowestTeamScore) {
        this.lowestTeamScore = lowestTeamScore;
    }

    public float getHighestIndividualScore() {
        return highestIndividualScore;
    }

    public void setHighestIndividualScore(float highestIndividualScore) {
        this.highestIndividualScore = highestIndividualScore;
    }

    public boolean isTestVenue() {
        return testVenue;
    }

    public void setTestVenue(boolean testVenue) {
        this.testVenue = testVenue;
    }

    public boolean isOdiVenue() {
        return odiVenue;
    }

    public void setOdiVenue(boolean odiVenue) {
        this.odiVenue = odiVenue;
    }

    public boolean isT20Venue() {
        return t20Venue;
    }

    public void setT20Venue(boolean t20Venue) {
        this.t20Venue = t20Venue;
    }
}