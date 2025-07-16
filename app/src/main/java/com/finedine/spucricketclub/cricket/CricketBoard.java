package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an international cricket board/association with all required data
 */
@Keep
@IgnoreExtraProperties
public class CricketBoard {
    private String id;
    private String name;
    private String shortName;
    private String countryCode;
    private String logoUrl;
    private String websiteUrl;
    private List<Team> nationalTeams;
    private List<String> homeVenues;
    private Map<String, String> socialMediaLinks;
    private int establishedYear;
    private int iccRanking;
    private boolean isFullMember;

    // Common cricket board IDs for quick reference
    public static final String ICC = "ICC";
    public static final String BCCI = "BCCI"; // India
    public static final String CA = "CA";   // Cricket Australia
    public static final String ECB = "ECB";  // England Cricket Board
    public static final String CSA = "CSA";  // Cricket South Africa
    public static final String PCB = "PCB";  // Pakistan Cricket Board
    public static final String SLC = "SLC";  // Sri Lanka Cricket
    public static final String NZC = "NZC";  // New Zealand Cricket
    public static final String WI = "WI";   // Cricket West Indies
    public static final String BCB = "BCB";  // Bangladesh Cricket Board

    // Default constructor
    public CricketBoard() {
        this.id = UUID.randomUUID().toString();
        this.nationalTeams = new ArrayList<>();
        this.homeVenues = new ArrayList<>();
        this.socialMediaLinks = new HashMap<>();
    }

    // Constructor with name
    public CricketBoard(String name, String shortName, String countryCode) {
        this();
        this.name = name;
        this.shortName = shortName;
        this.countryCode = countryCode;
    }

    /**
     * Factory method to create a predefined cricket board
     */
    public static CricketBoard createPredefined(String boardId) {
        CricketBoard board = new CricketBoard();
        board.id = boardId;

        switch (boardId) {
            case ICC:
                board.name = "International Cricket Council";
                board.shortName = "ICC";
                board.countryCode = "INT";
                board.establishedYear = 1909;
                board.isFullMember = true;
                board.websiteUrl = "https://www.icc-cricket.com/";
                break;

            case BCCI:
                board.name = "Board of Control for Cricket in India";
                board.shortName = "BCCI";
                board.countryCode = "IND";
                board.establishedYear = 1928;
                board.isFullMember = true;
                board.iccRanking = 1;
                board.websiteUrl = "https://www.bcci.tv/";
                board.homeVenues = List.of("Eden Gardens", "Wankhede Stadium", "M. A. Chidambaram Stadium", "Arun Jaitley Stadium");
                break;

            case CA:
                board.name = "Cricket Australia";
                board.shortName = "CA";
                board.countryCode = "AUS";
                board.establishedYear = 1905;
                board.isFullMember = true;
                board.iccRanking = 3;
                board.websiteUrl = "https://www.cricket.com.au/";
                board.homeVenues = List.of("Melbourne Cricket Ground", "Sydney Cricket Ground", "Adelaide Oval", "WACA Ground");
                break;

            case ECB:
                board.name = "England and Wales Cricket Board";
                board.shortName = "ECB";
                board.countryCode = "ENG";
                board.establishedYear = 1997;
                board.isFullMember = true;
                board.iccRanking = 2;
                board.websiteUrl = "https://www.ecb.co.uk/";
                board.homeVenues = List.of("Lord's", "The Oval", "Edgbaston", "Old Trafford");
                break;

            case CSA:
                board.name = "Cricket South Africa";
                board.shortName = "CSA";
                board.countryCode = "RSA";
                board.establishedYear = 1991;
                board.isFullMember = true;
                board.iccRanking = 4;
                board.websiteUrl = "https://www.cricket.co.za/";
                board.homeVenues = List.of("Wanderers Stadium", "Newlands Cricket Ground", "SuperSport Park", "Kingsmead Cricket Stadium");
                break;

            case PCB:
                board.name = "Pakistan Cricket Board";
                board.shortName = "PCB";
                board.countryCode = "PAK";
                board.establishedYear = 1948;
                board.isFullMember = true;
                board.iccRanking = 6;
                board.websiteUrl = "https://www.pcb.com.pk/";
                board.homeVenues = List.of("National Stadium", "Gaddafi Stadium", "Rawalpindi Cricket Stadium");
                break;

            case SLC:
                board.name = "Sri Lanka Cricket";
                board.shortName = "SLC";
                board.countryCode = "SRI";
                board.establishedYear = 1975;
                board.isFullMember = true;
                board.iccRanking = 8;
                board.websiteUrl = "https://www.srilankacricket.lk/";
                board.homeVenues = List.of("R. Premadasa Stadium", "Galle International Stadium", "Pallekele International Cricket Stadium");
                break;

            case NZC:
                board.name = "New Zealand Cricket";
                board.shortName = "NZC";
                board.countryCode = "NZL";
                board.establishedYear = 1894;
                board.isFullMember = true;
                board.iccRanking = 5;
                board.websiteUrl = "https://www.nzc.nz/";
                board.homeVenues = List.of("Eden Park", "Hagley Oval", "Basin Reserve", "Seddon Park");
                break;

            case WI:
                board.name = "Cricket West Indies";
                board.shortName = "WI";
                board.countryCode = "WI";
                board.establishedYear = 1926;
                board.isFullMember = true;
                board.iccRanking = 7;
                board.websiteUrl = "https://www.windiescricket.com/";
                board.homeVenues = List.of("Kensington Oval", "Sabina Park", "Queens Park Oval");
                break;

            case BCB:
                board.name = "Bangladesh Cricket Board";
                board.shortName = "BCB";
                board.countryCode = "BAN";
                board.establishedYear = 1972;
                board.isFullMember = true;
                board.iccRanking = 9;
                board.websiteUrl = "http://www.tigercricket.com.bd/";
                board.homeVenues = List.of("Sher-e-Bangla National Cricket Stadium", "Zahur Ahmed Chowdhury Stadium");
                break;

            default:
                board.name = "Unknown Cricket Board";
                board.shortName = "UCB";
                board.countryCode = "UNK";
        }

        return board;
    }

    /**
     * Add a national team to this board
     */
    public void addNationalTeam(Team team) {
        if (team != null && !nationalTeams.contains(team)) {
            nationalTeams.add(team);
        }
    }

    /**
     * Add a home venue to this board
     */
    public void addHomeVenue(String venue) {
        if (venue != null && !homeVenues.contains(venue)) {
            homeVenues.add(venue);
        }
    }

    /**
     * Add a social media link
     */
    public void addSocialMediaLink(String platform, String url) {
        if (platform != null && url != null) {
            socialMediaLinks.put(platform, url);
        }
    }

    // Getters and setters
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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public List<Team> getNationalTeams() {
        return nationalTeams;
    }

    public void setNationalTeams(List<Team> nationalTeams) {
        this.nationalTeams = nationalTeams;
    }

    public List<String> getHomeVenues() {
        return homeVenues;
    }

    public void setHomeVenues(List<String> homeVenues) {
        this.homeVenues = homeVenues;
    }

    public Map<String, String> getSocialMediaLinks() {
        return socialMediaLinks;
    }

    public void setSocialMediaLinks(Map<String, String> socialMediaLinks) {
        this.socialMediaLinks = socialMediaLinks;
    }

    public int getEstablishedYear() {
        return establishedYear;
    }

    public void setEstablishedYear(int establishedYear) {
        this.establishedYear = establishedYear;
    }

    public int getIccRanking() {
        return iccRanking;
    }

    public void setIccRanking(int iccRanking) {
        this.iccRanking = iccRanking;
    }

    public boolean isFullMember() {
        return isFullMember;
    }

    public void setFullMember(boolean fullMember) {
        isFullMember = fullMember;
    }
}