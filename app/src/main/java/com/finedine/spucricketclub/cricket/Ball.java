package com.finedine.spucricketclub.cricket;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Date;

/**
 * Represents a cricket ball delivery with all required data
 */
@Keep
@IgnoreExtraProperties
public class Ball {
    private int ballNumber; // Ball number in the over
    private int runsScored;
    private boolean isWicket;
    private BallType type;
    private Player batsman;
    private Player bowler;
    private WicketType wicketType;
    private ShotDirection shotDirection;
    private ShotType shotType;
    private BowlingSpeed bowlingSpeed;
    private float bowlingSpeedKph;
    private PitchLocation pitchLocation;
    private boolean isBoundary;
    private Date timestamp;
    private String notes;

    public enum BallType {
        NORMAL,
        WIDE,
        NO_BALL,
        BYE,
        LEG_BYE,
        DEAD_BALL
    }

    public enum WicketType {
        BOWLED,
        CAUGHT,
        LBW,
        RUN_OUT,
        STUMPED,
        HIT_WICKET,
        RETIRED_HURT,
        TIMED_OUT,
        OBSTRUCTING_FIELD,
        HANDLED_BALL,
        HIT_BALL_TWICE
    }

    public enum ShotDirection {
        FINE_LEG,
        SQUARE_LEG,
        MID_WICKET,
        MID_ON,
        LONG_ON,
        LONG_OFF,
        EXTRA_COVER,
        COVER,
        POINT,
        THIRD_MAN,
        NOT_APPLICABLE
    }

    public enum ShotType {
        STRAIGHT_DRIVE,
        COVER_DRIVE,
        CUT,
        PULL,
        HOOK,
        SWEEP,
        REVERSE_SWEEP,
        SCOOP,
        FLICK,
        BLOCK,
        LEAVE,
        EDGE,
        GLANCE,
        UPPER_CUT,
        SWITCH_HIT,
        PADDLE_SWEEP,
        ON_DRIVE,
        DEFENSIVE,
        RAMP,
        SLOG_SWEEP,
        NOT_APPLICABLE
    }

    public enum BowlingSpeed {
        SLOW,
        MEDIUM,
        FAST,
        EXPRESS
    }

    public enum PitchLocation {
        YORKER,
        FULL_TOSS,
        FULL_LENGTH,
        GOOD_LENGTH,
        SHORT,
        BOUNCER,
        WIDE_OUTSIDE_OFF,
        WIDE_DOWN_LEG
    }

    // Default constructor
    public Ball() {
        this.type = BallType.NORMAL;
        this.runsScored = 0;
        this.isWicket = false;
        this.timestamp = new Date();
        this.shotDirection = ShotDirection.NOT_APPLICABLE;
        this.shotType = ShotType.NOT_APPLICABLE;
    }

    // Constructor with runs scored
    public Ball(int runsScored) {
        this();
        this.runsScored = runsScored;
        this.isBoundary = (runsScored == 4 || runsScored == 6);
    }

    // Constructor with runs and ball number
    public Ball(int ballNumber, int runsScored) {
        this(runsScored);
        this.ballNumber = ballNumber;
    }

    // Constructor with complete ball info
    public Ball(int ballNumber, int runsScored, boolean isWicket, BallType type) {
        this.ballNumber = ballNumber;
        this.runsScored = runsScored;
        this.isWicket = isWicket;
        this.type = type;
        this.timestamp = new Date();
        this.isBoundary = (runsScored == 4 || runsScored == 6);
        this.shotDirection = ShotDirection.NOT_APPLICABLE;
        this.shotType = ShotType.NOT_APPLICABLE;
    }

    // Get display character for this ball (for display in over summary)
    public String getDisplayCharacter() {
        if (isWicket) {
            return "W";
        }

        switch (type) {
            case WIDE:
                return "Wd";
            case NO_BALL:
                return "Nb";
            case BYE:
                return "B" + runsScored;
            case LEG_BYE:
                return "Lb" + runsScored;
            case DEAD_BALL:
                return "DB";
            default:
                return runsScored == 0 ? "·" : String.valueOf(runsScored);
        }
    }

    // Check if this is a legal delivery (counts towards the 6 balls in an over)
    public boolean isLegalDelivery() {
        return type == BallType.NORMAL || type == BallType.BYE || type == BallType.LEG_BYE;
    }

    // Check if this ball should advance the ball count in the over
    public boolean countsAsDelivery() {
        return isLegalDelivery();
    }

    // Update shot information
    public void updateShotInfo(ShotDirection direction, ShotType type) {
        this.shotDirection = direction;
        this.shotType = type;
    }

    // Update bowling information
    public void updateBowlingInfo(PitchLocation pitchLocation, BowlingSpeed speed, float speedKph) {
        this.pitchLocation = pitchLocation;
        this.bowlingSpeed = speed;
        this.bowlingSpeedKph = speedKph;
    }

    // Create summary description of the ball
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        // Add ball info
        if (isWicket) {
            summary.append("WICKET! ");
            if (wicketType != null) {
                summary.append("(").append(wicketType).append(") ");
            }
        } else if (runsScored > 0) {
            summary.append(runsScored).append(" run");
            if (runsScored > 1) summary.append("s");
            summary.append(" ");
            if (isBoundary) {
                summary.append(runsScored == 4 ? "FOUR!" : "SIX!");
                summary.append(" ");
            }
        } else {
            summary.append("Dot ball. ");
        }

        // Add shot info if applicable
        if (shotType != ShotType.NOT_APPLICABLE) {
            summary.append(shotType.toString().replace("_", " "));
            if (shotDirection != ShotDirection.NOT_APPLICABLE) {
                summary.append(" to ").append(shotDirection.toString().replace("_", " "));
            }
        }

        return summary.toString().trim();
    }

    // Getters and setters
    public int getBallNumber() {
        return ballNumber;
    }

    public void setBallNumber(int ballNumber) {
        this.ballNumber = ballNumber;
    }

    public int getRunsScored() {
        return runsScored;
    }

    public void setRunsScored(int runsScored) {
        this.runsScored = runsScored;
        this.isBoundary = (runsScored == 4 || runsScored == 6);
    }

    public boolean isWicket() {
        return isWicket;
    }

    public void setWicket(boolean wicket) {
        isWicket = wicket;
    }

    public BallType getType() {
        return type;
    }

    public void setType(BallType type) {
        this.type = type;
    }

    public Player getBatsman() {
        return batsman;
    }

    public void setBatsman(Player batsman) {
        this.batsman = batsman;
    }

    public Player getBowler() {
        return bowler;
    }

    public void setBowler(Player bowler) {
        this.bowler = bowler;
    }

    public WicketType getWicketType() {
        return wicketType;
    }

    public void setWicketType(WicketType wicketType) {
        this.wicketType = wicketType;
        if (wicketType != null) {
            this.isWicket = true;
        }
    }

    public ShotDirection getShotDirection() {
        return shotDirection;
    }

    public void setShotDirection(ShotDirection shotDirection) {
        this.shotDirection = shotDirection;
    }

    public ShotType getShotType() {
        return shotType;
    }

    public void setShotType(ShotType shotType) {
        this.shotType = shotType;
    }

    public boolean isBoundary() {
        return isBoundary;
    }

    public void setBoundary(boolean boundary) {
        isBoundary = boundary;
    }

    public BowlingSpeed getBowlingSpeed() {
        return bowlingSpeed;
    }

    public void setBowlingSpeed(BowlingSpeed bowlingSpeed) {
        this.bowlingSpeed = bowlingSpeed;
    }

    public float getBowlingSpeedKph() {
        return bowlingSpeedKph;
    }

    public void setBowlingSpeedKph(float bowlingSpeedKph) {
        this.bowlingSpeedKph = bowlingSpeedKph;
    }

    public PitchLocation getPitchLocation() {
        return pitchLocation;
    }

    public void setPitchLocation(PitchLocation pitchLocation) {
        this.pitchLocation = pitchLocation;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}