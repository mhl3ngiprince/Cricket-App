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
 * Represents University Sports South Africa (USSA) Cricket Tournament and other university competitions
 */
@Keep
@IgnoreExtraProperties
public class USSATournament {
    private String id;
    private String name;
    private String shortName;
    private String description;
    private String logoUrl;
    private TournamentFormat format;
    private TournamentSection section; // A or B section
    private Date startDate;
    private Date endDate;
    private String hostUniversityId;
    private String hostCity;
    private String hostProvince;
    private String organizerId;
    private String winningUniversityId;
    private String runnerUpUniversityId;
    private int edition;
    private List<String> venueIds;
    private List<String> participatingUniversityIds;
    private List<String> matchIds;
    private Map<String, Object> extraInfo;
    private boolean isActive;

    // Tournament constants
    public static final String USSA_CRICKET_A = "USSA_A";
    public static final String USSA_CRICKET_B = "USSA_B";
    public static final String VARSITY_CRICKET = "VARSITY_CRICKET";
    public static final String RED_BULL_CAMPUS = "RED_BULL_CAMPUS";
    public static final String CSA_UNIVERSITY_CUP = "CSA_UNI_CUP";
    public static final String NC_UNIVERSITY_LEAGUE = "NC_UNI_LEAGUE";
    public static final String SPU_CRICKET_WEEK = "SPU_CRICKET_WEEK";
    public static final String USSA_T20 = "USSA_T20";

    // Tournament formats
    public enum TournamentFormat {
        TWO_DAY,
        ONE_DAY,
        T20,
        MIXED
    }

    // Tournament sections
    public enum TournamentSection {
        SECTION_A,
        SECTION_B,
        SECTION_C,
        COMBINED,
        NOT_APPLICABLE
    }

    // Default constructor
    public USSATournament() {
        this.id = UUID.randomUUID().toString();
        this.venueIds = new ArrayList<>();
        this.participatingUniversityIds = new ArrayList<>();
        this.matchIds = new ArrayList<>();
        this.extraInfo = new HashMap<>();
        this.section = TournamentSection.NOT_APPLICABLE;
    }

    // Constructor with name
    public USSATournament(String name, TournamentFormat format) {
        this();
        this.name = name;
        this.format = format;
    }

    /**
     * Factory method to create a predefined tournament
     */
    public static USSATournament createPredefined(String tournamentId) {
        USSATournament tournament = new USSATournament();
        tournament.id = tournamentId;

        switch (tournamentId) {
            case USSA_CRICKET_A:
                tournament.name = "USSA Cricket Week - A Section";
                tournament.shortName = "USSA A Section";
                tournament.format = TournamentFormat.MIXED;
                tournament.section = TournamentSection.SECTION_A;
                tournament.organizerId = "USSA";
                tournament.startDate = new Date(123, 11, 1); // December 1, 2023
                tournament.endDate = new Date(123, 11, 7); // December 7, 2023
                tournament.hostUniversityId = SouthAfricanUniversities.SU;
                tournament.hostCity = "Stellenbosch";
                tournament.hostProvince = "Western Cape";
                tournament.edition = 6;
                tournament.description = "Premier university cricket tournament in South Africa featuring the top 8 university teams";
                tournament.winningUniversityId = SouthAfricanUniversities.SU; // Previous year's winner
                tournament.runnerUpUniversityId = SouthAfricanUniversities.UP;
                tournament.participatingUniversityIds = List.of(
                        SouthAfricanUniversities.SU,
                        SouthAfricanUniversities.UP,
                        SouthAfricanUniversities.UCT,
                        SouthAfricanUniversities.UJ,
                        SouthAfricanUniversities.NWU,
                        SouthAfricanUniversities.UWC,
                        SouthAfricanUniversities.UKZN,
                        SouthAfricanUniversities.UFS
                );
                break;

            case USSA_CRICKET_B:
                tournament.name = "USSA Cricket Week - B Section";
                tournament.shortName = "USSA B Section";
                tournament.format = TournamentFormat.MIXED;
                tournament.section = TournamentSection.SECTION_B;
                tournament.organizerId = "USSA";
                tournament.startDate = new Date(123, 11, 1); // December 1, 2023
                tournament.endDate = new Date(123, 11, 7); // December 7, 2023
                tournament.hostUniversityId = SouthAfricanUniversities.UKZN;
                tournament.hostCity = "Durban";
                tournament.hostProvince = "KwaZulu-Natal";
                tournament.edition = 6;
                tournament.description = "Second tier university cricket tournament featuring developing cricket universities";
                tournament.winningUniversityId = SouthAfricanUniversities.UKZN; // Previous year's winner
                tournament.runnerUpUniversityId = SouthAfricanUniversities.SPU;
                tournament.participatingUniversityIds = List.of(
                        SouthAfricanUniversities.WITS,
                        SouthAfricanUniversities.NMMU,
                        SouthAfricanUniversities.UFH,
                        SouthAfricanUniversities.UL,
                        SouthAfricanUniversities.DUT,
                        SouthAfricanUniversities.CUT,
                        SouthAfricanUniversities.SPU,
                        SouthAfricanUniversities.UMP
                );
                break;

            case VARSITY_CRICKET:
                tournament.name = "Varsity Cricket T20";
                tournament.shortName = "Varsity Cricket";
                tournament.format = TournamentFormat.T20;
                tournament.section = TournamentSection.COMBINED;
                tournament.organizerId = "Varsity Sports";
                tournament.startDate = new Date(124, 0, 28); // January 28, 2024
                tournament.endDate = new Date(124, 1, 3); // February 3, 2024
                tournament.hostUniversityId = SouthAfricanUniversities.UP;
                tournament.hostCity = "Potchefstroom";
                tournament.hostProvince = "North West";
                tournament.edition = 7;
                tournament.description = "Premier university T20 cricket competition broadcasted on SuperSport";
                tournament.winningUniversityId = SouthAfricanUniversities.UP;
                tournament.runnerUpUniversityId = SouthAfricanUniversities.NWU;
                tournament.participatingUniversityIds = List.of(
                        SouthAfricanUniversities.UP,
                        SouthAfricanUniversities.SU,
                        SouthAfricanUniversities.UJ,
                        SouthAfricanUniversities.NWU,
                        SouthAfricanUniversities.UCT,
                        SouthAfricanUniversities.UWC,
                        SouthAfricanUniversities.UFS,
                        SouthAfricanUniversities.NMMU
                );
                break;

            case SPU_CRICKET_WEEK:
                tournament.name = "Sol Plaatje University Cricket Week";
                tournament.shortName = "SPU Cricket Week";
                tournament.format = TournamentFormat.T20;
                tournament.section = TournamentSection.NOT_APPLICABLE;
                tournament.organizerId = SouthAfricanUniversities.SPU;
                tournament.startDate = new Date(123, 9, 10); // October 10, 2023
                tournament.endDate = new Date(123, 9, 15); // October 15, 2023
                tournament.hostUniversityId = SouthAfricanUniversities.SPU;
                tournament.hostCity = "Kimberley";
                tournament.hostProvince = "Northern Cape";
                tournament.edition = 3;
                tournament.description = "SPU-hosted cricket tournament featuring Northern Cape teams and neighboring universities";
                tournament.participatingUniversityIds = List.of(
                        SouthAfricanUniversities.SPU,
                        SouthAfricanUniversities.CUT,
                        SouthAfricanUniversities.UFS,
                        SouthAfricanUniversities.NWU
                );
                tournament.venueIds = List.of(
                        SAVenue.SPU_CRICKET_OVAL,
                        SAVenue.DIAMOND_OVAL,
                        SAVenue.KIMBERLEY_OVAL
                );
                break;

            case NC_UNIVERSITY_LEAGUE:
                tournament.name = "Northern Cape University Cricket League";
                tournament.shortName = "NC University League";
                tournament.format = TournamentFormat.ONE_DAY;
                tournament.section = TournamentSection.NOT_APPLICABLE;
                tournament.organizerId = "Northern Cape Cricket";
                tournament.startDate = new Date(123, 7, 1); // August 1, 2023
                tournament.endDate = new Date(123, 10, 30); // November 30, 2023
                tournament.hostUniversityId = SouthAfricanUniversities.SPU;
                tournament.hostCity = "Kimberley";
                tournament.hostProvince = "Northern Cape";
                tournament.edition = 4;
                tournament.description = "Season-long cricket league for universities in and around Northern Cape";
                tournament.participatingUniversityIds = List.of(
                        SouthAfricanUniversities.SPU,
                        SouthAfricanUniversities.CUT,
                        SouthAfricanUniversities.NWU,
                        SouthAfricanUniversities.UFS
                );
                tournament.venueIds = List.of(
                        SAVenue.SPU_CRICKET_OVAL,
                        SAVenue.DIAMOND_OVAL,
                        SAVenue.KIMBERLEY_OVAL,
                        SAVenue.GALESHEWE_CRICKET
                );
                break;

            default:
                tournament.name = "Unknown Tournament";
                tournament.shortName = "Unknown";
                tournament.format = TournamentFormat.MIXED;
        }

        return tournament;
    }

    /**
     * Add a venue to this tournament
     */
    public void addVenue(String venueId) {
        if (venueId != null && !venueIds.contains(venueId)) {
            venueIds.add(venueId);
        }
    }

    /**
     * Add a university to this tournament
     */
    public void addUniversity(String universityId) {
        if (universityId != null && !participatingUniversityIds.contains(universityId)) {
            participatingUniversityIds.add(universityId);
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
    public void setWinner(String universityId) {
        this.winningUniversityId = universityId;
    }

    /**
     * Sets the tournament runner-up
     */
    public void setRunnerUp(String universityId) {
        this.runnerUpUniversityId = universityId;
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

    public TournamentFormat getFormat() {
        return format;
    }

    public void setFormat(TournamentFormat format) {
        this.format = format;
    }

    public TournamentSection getSection() {
        return section;
    }

    public void setSection(TournamentSection section) {
        this.section = section;
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

    public String getHostUniversityId() {
        return hostUniversityId;
    }

    public void setHostUniversityId(String hostUniversityId) {
        this.hostUniversityId = hostUniversityId;
    }

    public String getHostCity() {
        return hostCity;
    }

    public void setHostCity(String hostCity) {
        this.hostCity = hostCity;
    }

    public String getHostProvince() {
        return hostProvince;
    }

    public void setHostProvince(String hostProvince) {
        this.hostProvince = hostProvince;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getWinningUniversityId() {
        return winningUniversityId;
    }

    public String getRunnerUpUniversityId() {
        return runnerUpUniversityId;
    }

    public int getEdition() {
        return edition;
    }

    public void setEdition(int edition) {
        this.edition = edition;
    }

    public List<String> getVenueIds() {
        return venueIds;
    }

    public void setVenueIds(List<String> venueIds) {
        this.venueIds = venueIds;
    }

    public List<String> getParticipatingUniversityIds() {
        return participatingUniversityIds;
    }

    public void setParticipatingUniversityIds(List<String> participatingUniversityIds) {
        this.participatingUniversityIds = participatingUniversityIds;
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