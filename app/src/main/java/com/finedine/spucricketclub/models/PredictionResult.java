package com.finedine.spucricketclub.models;

/**
 * Model class to hold prediction results with range
 */
public class PredictionResult {
    private int lowEstimate;
    private int midEstimate;
    private int highEstimate;
    private WinProbability[] winProbabilities;
    private String details;

    public PredictionResult() {
        this.lowEstimate = 0;
        this.midEstimate = 0;
        this.highEstimate = 0;
    }

    public PredictionResult(int lowEstimate, int midEstimate, int highEstimate) {
        this.lowEstimate = lowEstimate;
        this.midEstimate = midEstimate;
        this.highEstimate = highEstimate;
    }

    /**
     * @return The low end of the prediction range
     */
    public int getLowEstimate() {
        return lowEstimate;
    }

    /**
     * @param lowEstimate The low end estimate to set
     */
    public void setLowEstimate(int lowEstimate) {
        this.lowEstimate = lowEstimate;
    }

    /**
     * @return The middle (most likely) prediction
     */
    public int getMidEstimate() {
        return midEstimate;
    }

    /**
     * @param midEstimate The mid estimate to set
     */
    public void setMidEstimate(int midEstimate) {
        this.midEstimate = midEstimate;
    }

    /**
     * @return The high end of the prediction range
     */
    public int getHighEstimate() {
        return highEstimate;
    }

    /**
     * @param highEstimate The high estimate to set
     */
    public void setHighEstimate(int highEstimate) {
        this.highEstimate = highEstimate;
    }

    /**
     * @return The win probabilities for teams
     */
    public WinProbability[] getWinProbabilities() {
        return winProbabilities;
    }

    /**
     * @param winProbabilities The win probabilities to set
     */
    public void setWinProbabilities(WinProbability[] winProbabilities) {
        this.winProbabilities = winProbabilities;
    }

    /**
     * @return Additional details about the prediction
     */
    public String getDetails() {
        return details;
    }

    /**
     * @param details The details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * @return A formatted string representation of the prediction
     */
    public String getFormattedPrediction() {
        return String.format("%d (%d-%d)", midEstimate, lowEstimate, highEstimate);
    }
}