package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a cricket tournament with all required data
 */
@Keep
@IgnoreExtraProperties
public class Tournament {
    private String id;
    private String name;
    private String shortName;
    private String description;
    private String logoUrl;
    private TournamentType type;
    private TournamentFormat format;
    private Date startDate;
    private Date endDate;
    private String hostCountry;
    private String hostCountryCode;
    private String organizerId;
    private String winnerTeamId;
    private String runnerUpTeamId;
    private int edition;
    private List<String> venues;
    private List<String> participatingTeamIds;
    private List<String> matchIds;
    private Map<String, Object> extraInfo;
    private boolean isActive;

    // Tournament constants
    public static final String WORLD_CUP = "WC";
    public static final String IPL = "IPL";
    public static final String BIG_BASH = "BBL";
    public static final String PSL = "PSL";
    public static final String CPL = "CPL";
    public static final String T20_WORLD_CUP = "T20WC";
    public static final String CHAMPIONS_TROPHY = "CT";
    public static final String THE_ASHES = "ASHES";

    // Tournament types
    public enum TournamentType {
        INTERNATIONAL,
        DOMESTIC,
        FRANCHISE,
        TEST_SERIES,
        BILATERAL_SERIES,
        CHARITY
    }

    // Tournament formats
    public enum TournamentFormat {
        TEST,
        ODI,
        T20,
        T10,
        MIXED
    }

    // Default constructor
    public Tournament() {
        this.id = UUID.randomUUID().toString();
        this.venues = new ArrayList<>();
        this.participatingTeamIds = new ArrayList<>();
        this.matchIds = new ArrayList<>();
        this.extraInfo = new HashMap<>();
    }

    // Constructor with name
    public Tournament(String name, TournamentType type, TournamentFormat format) {
        this();
        this.name = name;
        this.type = type;
        this.format = format;
    }

    /**
     * Factory method to create a predefined tournament
     */
    public static Tournament createPredefined(String tournamentId) {
        Tournament tournament = new Tournament();
        tournament.id = tournamentId;

        switch (tournamentId) {
            case WORLD_CUP:
                tournament.name = "ICC Cricket World Cup";
                tournament.shortName = "World Cup";
                tournament.type = TournamentType.INTERNATIONAL;
                tournament.format = TournamentFormat.ODI;
                tournament.organizerId = CricketBoard.ICC;
                tournament.startDate = new Date(122, 9, 1); // October 1, 2022
                tournament.endDate = new Date(122, 10, 15); // November 15, 2022
                tournament.hostCountry = "Australia";
                tournament.hostCountryCode = "AUS";
                tournament.edition = 13;
                tournament.description = "The premier international cricket tournament held every four years";
                break;

            case T20_WORLD_CUP:
                tournament.name = "ICC T20 World Cup";
                tournament.shortName = "T20 World Cup";
                tournament.type = TournamentType.INTERNATIONAL;
                tournament.format = TournamentFormat.T20;
                tournament.organizerId = CricketBoard.ICC;
                tournament.startDate = new Date(121, 9, 17); // October 17, 2021
                tournament.endDate = new Date(121, 10, 14); // November 14, 2021
                tournament.hostCountry = "UAE & Oman";
                tournament.hostCountryCode = "UAE";
                tournament.edition = 7;
                tournament.description = "The major international Twenty20 cricket championship";
                break;

            case IPL:
                tournament.name = "Indian Premier League";
                tournament.shortName = "IPL";
                tournament.type = TournamentType.FRANCHISE;
                tournament.format = TournamentFormat.T20;
                tournament.organizerId = CricketBoard.BCCI;
                tournament.startDate = new Date(123, 2, 31); // March 31, 2023
                tournament.endDate = new Date(123, 4, 28); // May 28, 2023
                tournament.hostCountry = "India";
                tournament.hostCountryCode = "IND";
                tournament.edition = 16;
                tournament.description = "Professional Twenty20 cricket league in India contested by ten teams based in different Indian cities";
                break;

            case BIG_BASH:
                tournament.name = "Big Bash League";
                tournament.shortName = "BBL";
                tournament.type = TournamentType.FRANCHISE;
                tournament.format = TournamentFormat.T20;
                tournament.organizerId = CricketBoard.CA;
                tournament.startDate = new Date(122, 11, 13); // December 13, 2022
                tournament.endDate = new Date(123, 1, 4); // February 4, 2023
                tournament.hostCountry = "Australia";
                tournament.hostCountryCode = "AUS";
                tournament.edition = 12;
                tournament.description = "Australian professional franchise Twenty20 cricket league";
                break;

            case THE_ASHES:
                tournament.name = "The Ashes";
                tournament.shortName = "Ashes";
                tournament.type = TournamentType.BILATERAL_SERIES;
                tournament.format = TournamentFormat.TEST;
                tournament.organizerId = CricketBoard.ECB; // Depends on host
                tournament.startDate = new Date(122, 7, 16); // August 16, 2023
                tournament.endDate = new Date(122, 8, 8); // September 8, 2023
                tournament.hostCountry = "England";
                tournament.hostCountryCode = "ENG";
                tournament.edition = 72;
                tournament.description = "Test cricket series played between England and Australia";
                break;

            default:
                tournament.name = "Unknown Tournament";
                tournament.shortName = "Unknown";
                tournament.type = TournamentType.DOMESTIC;
                tournament.format = TournamentFormat.MIXED;
        }

        return tournament;
    }

    /**
     * Add a venue to this tournament
     */
    public void addVenue(String venueId) {
        if (venueId != null && !venues.contains(venueId)) {
            venues.add(venueId);
        }
    }

    /**
     * Add a team to this tournament
     */
    public void addTeam(String teamId) {
        if (teamId != null && !participatingTeamIds.contains(teamId)) {
            participatingTeamIds.add(teamId);
        }
    }

    /**
     * Add a match to this tournament
     */
    public void addMatch(String matchId) {
        if (matchId != null && !matchIds.contains(matchId)) {
            matchIds.add(matchId);
        }
    }

    /**
     * Sets the tournament winner
     */
    public void setWinner(String teamId) {
        this.winnerTeamId = teamId;
    }

    /**
     * Sets the tournament runner-up
     */
    public void setRunnerUp(String teamId) {
        this.runnerUpTeamId = teamId;
    }

    /**
     * Check if tournament is currently active based on dates
     */
    public boolean isCurrentlyActive() {
        Date now = new Date();
        return startDate != null && endDate != null &&
                now.after(startDate) && now.before(endDate);
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

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
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

    public TournamentType getType() {
        return type;
    }

    public void setType(TournamentType type) {
        this.type = type;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public void setFormat(TournamentFormat format) {
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

    public String getHostCountry() {
        return hostCountry;
    }

    public void setHostCountry(String hostCountry) {
        this.hostCountry = hostCountry;
    }

    public String getHostCountryCode() {
        return hostCountryCode;
    }

    public void setHostCountryCode(String hostCountryCode) {
        this.hostCountryCode = hostCountryCode;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getWinnerTeamId() {
        return winnerTeamId;
    }

    public String getRunnerUpTeamId() {
        return runnerUpTeamId;
    }

    public int getEdition() {
        return edition;
    }

    public void setEdition(int edition) {
        this.edition = edition;
    }

    public List<String> getVenues() {
        return venues;
    }

    public void setVenues(List<String> venues) {
        this.venues = venues;
    }

    public List<String> getParticipatingTeamIds() {
        return participatingTeamIds;
    }

    public void setParticipatingTeamIds(List<String> participatingTeamIds) {
        this.participatingTeamIds = participatingTeamIds;
    }

    public List<String> getMatchIds() {
        return matchIds;
    }

    public void setMatchIds(List<String> matchIds) {
        this.matchIds = matchIds;
    }

    public Map<String, Object> getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(Map<String, Object> extraInfo) {
        this.extraInfo = extraInfo;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}