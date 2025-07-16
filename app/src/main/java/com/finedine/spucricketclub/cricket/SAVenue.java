package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a South African cricket venue with a focus on university grounds
 */
@Keep
@IgnoreExtraProperties
public class SAVenue {
    private String id;
    private String name;
    private String city;
    private String province;
    private String universityId; // If it's a university venue
    private double latitude;
    private double longitude;
    private int capacity;
    private int established;
    private String imageUrl;
    private boolean hasFloodlights;
    private List<String> pitchTypes; // e.g., "green", "dry", "dusty"
    private Map<String, Object> stats;
    private List<String> amenities; // Facilities available
    private float averageFirstInningsScore;
    private float averageSecondInningsScore;
    private boolean isMainUniVenue; // Is this the main university cricket ground?
    private String accessInfo; // Information on how to access the venue

    // South African venue constants for quick reference
    // University Venues
    public static final String SPU_CRICKET_OVAL = "SPU_OVAL";
    public static final String TUKS_OVAL = "TUKS_OVAL";
    public static final String MATIES_STADIUM = "MATIES_STADIUM";
    public static final String UJ_OVAL = "UJ_OVAL";
    public static final String WITS_OVAL = "WITS_OVAL";
    public static final String NWU_CRICKET = "NWU_CRICKET";

    // Kimberley Venues
    public static final String DIAMOND_OVAL = "DIAMOND_OVAL";
    public static final String KIMBERLEY_OVAL = "KIMBERLEY_OVAL";
    public static final String NORTHERN_CAPE_HS = "NC_HS_FIELD";
    public static final String GALESHEWE_CRICKET = "GALESHEWE_CRICKET";

    // Major SA Venues
    public static final String WANDERERS = "WANDERERS";
    public static final String NEWLANDS = "NEWLANDS";
    public static final String SUPERSPORT_PARK = "SUPERSPORT_PARK";
    public static final String ST_GEORGES = "ST_GEORGES";
    public static final String KINGSMEAD = "KINGSMEAD";

    // Default constructor
    public SAVenue() {
        this.id = UUID.randomUUID().toString();
        this.pitchTypes = new ArrayList<>();
        this.stats = new HashMap<>();
        this.amenities = new ArrayList<>();
    }

    // Constructor with name and location
    public SAVenue(String name, String city, String province) {
        this();
        this.name = name;
        this.city = city;
        this.province = province;
    }

    /**
     * Factory method to create a predefined venue
     */
    public static SAVenue createPredefined(String venueId) {
        SAVenue venue = new SAVenue();
        venue.id = venueId;

        switch (venueId) {
            // ******** UNIVERSITY VENUES ********
            case SPU_CRICKET_OVAL:
                venue.name = "SPU Cricket Oval";
                venue.city = "Kimberley";
                venue.province = "Northern Cape";
                venue.universityId = SouthAfricanUniversities.SPU;
                venue.capacity = 500;
                venue.established = 2018;
                venue.hasFloodlights = false;
                venue.pitchTypes = List.of("Hard", "Dry");
                venue.isMainUniVenue = true;
                venue.amenities = List.of("Practice nets", "Clubhouse", "Changing rooms", "Electronic scoreboard");
                venue.latitude = -28.7380;
                venue.longitude = 24.7731;
                venue.accessInfo = "Located on the SPU North Campus. Parking available on campus.";
                venue.averageFirstInningsScore = 160;
                venue.averageSecondInningsScore = 140;
                break;

            case TUKS_OVAL:
                venue.name = "Tuks Cricket Oval";
                venue.city = "Pretoria";
                venue.province = "Gauteng";
                venue.universityId = SouthAfricanUniversities.UP;
                venue.capacity = 2000;
                venue.established = 1985;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Balanced", "High bounce");
                venue.isMainUniVenue = true;
                venue.amenities = List.of("Indoor training center", "Multiple practice nets", "Modern clubhouse", "Media facilities", "Electronic scoreboard");
                venue.latitude = -25.7545;
                venue.longitude = 28.2314;
                venue.accessInfo = "Located at the LC de Villiers Sports Campus. Public parking available.";
                venue.averageFirstInningsScore = 180;
                venue.averageSecondInningsScore = 165;
                break;

            case MATIES_STADIUM:
                venue.name = "Coetzenburg Cricket Stadium";
                venue.city = "Stellenbosch";
                venue.province = "Western Cape";
                venue.universityId = SouthAfricanUniversities.SU;
                venue.capacity = 1500;
                venue.established = 1960;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Green", "Seam-friendly");
                venue.isMainUniVenue = true;
                venue.amenities = List.of("Practice nets", "Gymnasium", "Club house", "Electronic scoreboard");
                venue.latitude = -33.9403;
                venue.longitude = 18.8669;
                venue.accessInfo = "Located at Coetzenburg Sports Complex near the university campus.";
                venue.averageFirstInningsScore = 170;
                venue.averageSecondInningsScore = 155;
                break;

            // ******** KIMBERLEY VENUES ********
            case DIAMOND_OVAL:
                venue.name = "De Beers Diamond Oval";
                venue.city = "Kimberley";
                venue.province = "Northern Cape";
                venue.capacity = 11000;
                venue.established = 1973;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Balanced", "Dry", "Slight turn");
                venue.isMainUniVenue = false;
                venue.amenities = List.of("Practice nets", "Media center", "VIP suites", "Electronic scoreboard", "Restaurant");
                venue.latitude = -28.7317;
                venue.longitude = 24.7747;
                venue.accessInfo = "Located on Du Toitspan Road. Ample parking available.";
                venue.averageFirstInningsScore = 175;
                venue.averageSecondInningsScore = 160;
                break;

            case KIMBERLEY_OVAL:
                venue.name = "Kimberley Oval";
                venue.city = "Kimberley";
                venue.province = "Northern Cape";
                venue.capacity = 800;
                venue.established = 1995;
                venue.hasFloodlights = false;
                venue.pitchTypes = List.of("Hard", "Dry", "Flat");
                venue.isMainUniVenue = false;
                venue.amenities = List.of("Basic practice nets", "Changing rooms", "Scoreboard");
                venue.latitude = -28.7518;
                venue.longitude = 24.7589;
                venue.accessInfo = "Located near Northern Cape High School. Street parking available.";
                venue.averageFirstInningsScore = 150;
                venue.averageSecondInningsScore = 135;
                break;

            case NORTHERN_CAPE_HS:
                venue.name = "Northern Cape High School Cricket Field";
                venue.city = "Kimberley";
                venue.province = "Northern Cape";
                venue.capacity = 300;
                venue.established = 1980;
                venue.hasFloodlights = false;
                venue.pitchTypes = List.of("Hard", "School pitch");
                venue.isMainUniVenue = false;
                venue.amenities = List.of("Practice net", "Basic facilities", "Manual scoreboard");
                venue.latitude = -28.7400;
                venue.longitude = 24.7680;
                venue.accessInfo = "Located at Northern Cape High School. Limited parking available.";
                venue.averageFirstInningsScore = 140;
                venue.averageSecondInningsScore = 130;
                break;

            case GALESHEWE_CRICKET:
                venue.name = "Galeshewe Cricket Stadium";
                venue.city = "Kimberley";
                venue.province = "Northern Cape";
                venue.capacity = 600;
                venue.established = 2005;
                venue.hasFloodlights = true;
                venue.pitchTypes = List.of("Balanced", "Community pitch");
                venue.isMainUniVenue = false;
                venue.amenities = List.of("Community clubhouse", "Practice facilities", "Electronic scoreboard");
                venue.latitude = -28.7080;
                venue.longitude = 24.7420;
                venue.accessInfo = "Located in Galeshewe Township. Community parking available.";
                venue.averageFirstInningsScore = 155;
                venue.averageSecondInningsScore = 145;
                break;

            default:
                venue.name = "Unknown Venue";
                venue.city = "Unknown City";
                venue.province = "Unknown Province";
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
     * Add an amenity for this venue
     */
    public void addAmenity(String amenity) {
        if (amenity != null && !amenities.contains(amenity)) {
            amenities.add(amenity);
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
                .append(" (").append(city).append(", ").append(province).append(")");

        if (established > 0) {
            sb.append(" - Est. ").append(established);
        }

        if (capacity > 0) {
            sb.append(", Capacity: ").append(String.format("%,d", capacity));
        }

        if (universityId != null && !universityId.isEmpty()) {
            sb.append("\nUniversity: ").append(universityId);
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

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getUniversityId() {
        return universityId;
    }

    public void setUniversityId(String universityId) {
        this.universityId = universityId;
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

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
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

    public boolean isMainUniVenue() {
        return isMainUniVenue;
    }

    public void setMainUniVenue(boolean mainUniVenue) {
        isMainUniVenue = mainUniVenue;
    }

    public String getAccessInfo() {
        return accessInfo;
    }

    public void setAccessInfo(String accessInfo) {
        this.accessInfo = accessInfo;
    }
}