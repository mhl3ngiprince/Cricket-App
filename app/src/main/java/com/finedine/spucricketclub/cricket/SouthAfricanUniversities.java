package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents South African Universities that participate in cricket tournaments
 */
@Keep
@IgnoreExtraProperties
public class SouthAfricanUniversities {
    private String id;
    private String name;
    private String shortName;
    private String province;
    private String city;
    private String logoUrl;
    private String websiteUrl;
    private List<String> homeVenues;
    private Map<String, String> socialMediaLinks;
    private int establishedYear;
    private int ussaRanking;
    private List<String> achievements;
    private String primaryColor;
    private String secondaryColor;

    // University IDs for quick reference
    public static final String SPU = "SPU"; // Sol Plaatje University (Kimberley)
    public static final String UCT = "UCT"; // University of Cape Town
    public static final String UWC = "UWC"; // University of the Western Cape
    public static final String SU = "SU";   // Stellenbosch University
    public static final String UP = "UP";   // University of Pretoria
    public static final String UJ = "UJ";   // University of Johannesburg
    public static final String WITS = "WITS"; // University of the Witwatersrand
    public static final String UKZN = "UKZN"; // University of KwaZulu-Natal
    public static final String NMMU = "NMMU"; // Nelson Mandela Metropolitan University
    public static final String UFS = "UFS"; // University of the Free State
    public static final String NWU = "NWU"; // North-West University
    public static final String UFH = "UFH"; // University of Fort Hare
    public static final String UL = "UL";   // University of Limpopo
    public static final String CUT = "CUT"; // Central University of Technology
    public static final String TUT = "TUT"; // Tshwane University of Technology
    public static final String DUT = "DUT"; // Durban University of Technology
    public static final String CPUT = "CPUT"; // Cape Peninsula University of Technology
    public static final String UZ = "UZ";   // University of Zululand
    public static final String UMP = "UMP"; // University of Mpumalanga
    public static final String VUT = "VUT"; // Vaal University of Technology

    // Default constructor
    public SouthAfricanUniversities() {
        this.id = UUID.randomUUID().toString();
        this.homeVenues = new ArrayList<>();
        this.socialMediaLinks = new HashMap<>();
        this.achievements = new ArrayList<>();
    }

    // Constructor with name
    public SouthAfricanUniversities(String name, String shortName, String province) {
        this();
        this.name = name;
        this.shortName = shortName;
        this.province = province;
    }

    /**
     * Factory method to create a predefined university
     */
    public static SouthAfricanUniversities createPredefined(String universityId) {
        SouthAfricanUniversities university = new SouthAfricanUniversities();
        university.id = universityId;

        switch (universityId) {
            case SPU:
                university.name = "Sol Plaatje University";
                university.shortName = "SPU";
                university.province = "Northern Cape";
                university.city = "Kimberley";
                university.establishedYear = 2014;
                university.ussaRanking = 15;
                university.websiteUrl = "https://www.spu.ac.za/";
                university.primaryColor = "#003DA5";
                university.secondaryColor = "#FDBA12";
                university.homeVenues = List.of("SPU Cricket Oval", "De Beers Diamond Oval", "Kimberley Oval");
                university.achievements = List.of("USSA Cricket B Section Finalists 2022", "Northern Cape Cricket Week Champions 2021");
                break;

            case UCT:
                university.name = "University of Cape Town";
                university.shortName = "UCT";
                university.province = "Western Cape";
                university.city = "Cape Town";
                university.establishedYear = 1829;
                university.ussaRanking = 3;
                university.websiteUrl = "https://www.uct.ac.za/";
                university.primaryColor = "#005A9C";
                university.secondaryColor = "#FFFFFF";
                university.homeVenues = List.of("UCT Cricket Oval", "Newlands Cricket Ground");
                university.achievements = List.of("USSA Cricket A Section Champions 2019", "Western Province Club Champions 2020");
                break;

            case UWC:
                university.name = "University of the Western Cape";
                university.shortName = "UWC";
                university.province = "Western Cape";
                university.city = "Bellville";
                university.establishedYear = 1959;
                university.ussaRanking = 5;
                university.websiteUrl = "https://www.uwc.ac.za/";
                university.primaryColor = "#8E001C";
                university.secondaryColor = "#FFFFFF";
                university.homeVenues = List.of("UWC Sports Stadium", "Bellville Cricket Club");
                university.achievements = List.of("USSA Cricket A Section Semi-finalists 2021", "CSA University Cup Finalists 2020");
                break;

            case SU:
                university.name = "Stellenbosch University";
                university.shortName = "Maties";
                university.province = "Western Cape";
                university.city = "Stellenbosch";
                university.establishedYear = 1866;
                university.ussaRanking = 1;
                university.websiteUrl = "https://www.sun.ac.za/";
                university.primaryColor = "#8C1D40";
                university.secondaryColor = "#FFB81C";
                university.homeVenues = List.of("Coetzenburg Cricket Stadium", "Maties Cricket Club");
                university.achievements = List.of("USSA Cricket A Section Champions 2021, 2022", "CSA University Champions 2021");
                break;

            case UP:
                university.name = "University of Pretoria";
                university.shortName = "Tuks";
                university.province = "Gauteng";
                university.city = "Pretoria";
                university.establishedYear = 1908;
                university.ussaRanking = 2;
                university.websiteUrl = "https://www.up.ac.za/";
                university.primaryColor = "#BE0F34";
                university.secondaryColor = "#000000";
                university.homeVenues = List.of("Tuks Cricket Oval", "LC de Villiers Sports Grounds");
                university.achievements = List.of("USSA Cricket A Section Champions 2020", "Red Bull Campus Cricket World Champions 2018");
                break;

            case UJ:
                university.name = "University of Johannesburg";
                university.shortName = "UJ";
                university.province = "Gauteng";
                university.city = "Johannesburg";
                university.establishedYear = 2005;
                university.ussaRanking = 4;
                university.websiteUrl = "https://www.uj.ac.za/";
                university.primaryColor = "#F7941E";
                university.secondaryColor = "#000000";
                university.homeVenues = List.of("UJ Cricket Oval", "Auckland Park Bunting Road Campus");
                university.achievements = List.of("USSA Cricket A Section Finalists 2022", "Gauteng University League Champions 2021");
                break;

            case UKZN:
                university.name = "University of KwaZulu-Natal";
                university.shortName = "UKZN";
                university.province = "KwaZulu-Natal";
                university.city = "Durban";
                university.establishedYear = 2004;
                university.ussaRanking = 7;
                university.websiteUrl = "https://www.ukzn.ac.za/";
                university.primaryColor = "#FFCC00";
                university.secondaryColor = "#003F87";
                university.homeVenues = List.of("UKZN Oval", "Westville Campus Cricket Ground");
                university.achievements = List.of("USSA Cricket B Section Champions 2019", "KZN Coastal League Semi-finalists 2021");
                break;

            case NWU:
                university.name = "North-West University";
                university.shortName = "NWU";
                university.province = "North West";
                university.city = "Potchefstroom";
                university.establishedYear = 2004;
                university.ussaRanking = 6;
                university.websiteUrl = "https://www.nwu.ac.za/";
                university.primaryColor = "#4F2D7F";
                university.secondaryColor = "#FFFFFF";
                university.homeVenues = List.of("Senwes Park", "NWU-Puk Cricket Ground");
                university.achievements = List.of("USSA Cricket A Section Semi-finalists 2022", "North West Premier League Champions 2020");
                break;

            case UFS:
                university.name = "University of the Free State";
                university.shortName = "Kovsies";
                university.province = "Free State";
                university.city = "Bloemfontein";
                university.establishedYear = 1904;
                university.ussaRanking = 8;
                university.websiteUrl = "https://www.ufs.ac.za/";
                university.primaryColor = "#0066B3";
                university.secondaryColor = "#E31837";
                university.homeVenues = List.of("Kovsies Cricket Oval", "Shimla Park");
                university.achievements = List.of("USSA Cricket B Section Champions 2021", "Free State University Cup Winners 2022");
                break;

            default:
                university.name = "Unknown University";
                university.shortName = "UNK";
                university.province = "Unknown Province";
                university.city = "Unknown City";
        }

        return university;
    }

    /**
     * Add a home venue to this university
     */
    public void addHomeVenue(String venue) {
        if (venue != null && !homeVenues.contains(venue)) {
            homeVenues.add(venue);
        }
    }

    /**
     * Add an achievement
     */
    public void addAchievement(String achievement) {
        if (achievement != null && !achievements.contains(achievement)) {
            achievements.add(achievement);
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

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public int getUssaRanking() {
        return ussaRanking;
    }

    public void setUssaRanking(int ussaRanking) {
        this.ussaRanking = ussaRanking;
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }
}