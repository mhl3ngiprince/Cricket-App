package com.finedine.spucricketclub.models;

/**
 * Model class to hold pitch analysis results
 */
public class PitchAnalysisResult {
    private final float grassCoverage;
    private final float cracks;
    private final float moisture;
    private final float predictedBounce;
    private final float predictedTurn;
    private final String pitchType;

    private PitchAnalysisResult(Builder builder) {
        this.grassCoverage = builder.grassCoverage;
        this.cracks = builder.cracks;
        this.moisture = builder.moisture;
        this.predictedBounce = builder.predictedBounce;
        this.predictedTurn = builder.predictedTurn;
        this.pitchType = builder.pitchType;
    }

    /**
     * @return Percentage of grass covering the pitch (0-100)
     */
    public float getGrassCoverage() {
        return grassCoverage;
    }

    /**
     * @return Percentage of cracks in the pitch (0-100)
     */
    public float getCracks() {
        return cracks;
    }

    /**
     * @return Estimated moisture level in the pitch (0-100)
     */
    public float getMoisture() {
        return moisture;
    }

    /**
     * @return Predicted bounce on a scale of 1-10 (1=low, 10=high)
     */
    public float getPredictedBounce() {
        return predictedBounce;
    }

    /**
     * @return Predicted turn for spinners on a scale of 1-10 (1=little, 10=high)
     */
    public float getPredictedTurn() {
        return predictedTurn;
    }

    /**
     * @return String description of pitch type (Green, Dry, Cracked, etc.)
     */
    public String getPitchType() {
        return pitchType;
    }

    /**
     * @return Recommendation for which bowling type would be most effective
     */
    public String getBowlingRecommendation() {
        if (predictedTurn > 7.0f) {
            return "Spin bowling highly effective";
        } else if (predictedBounce > 7.0f) {
            return "Fast bowling with bounce effective";
        } else if (predictedBounce < 3.0f) {
            return "Cutters and slower balls recommended";
        } else {
            return "Balanced approach recommended";
        }
    }

    /**
     * Builder for PitchAnalysisResult
     */
    public static class Builder {
        private float grassCoverage;
        private float cracks;
        private float moisture;
        private float predictedBounce;
        private float predictedTurn;
        private String pitchType = "Unknown";

        public Builder setGrassCoverage(float grassCoverage) {
            this.grassCoverage = grassCoverage;
            return this;
        }

        public Builder setCracks(float cracks) {
            this.cracks = cracks;
            return this;
        }

        public Builder setMoisture(float moisture) {
            this.moisture = moisture;
            return this;
        }

        public Builder setPredictedBounce(float predictedBounce) {
            this.predictedBounce = predictedBounce;
            return this;
        }

        public Builder setPredictedTurn(float predictedTurn) {
            this.predictedTurn = predictedTurn;
            return this;
        }

        public Builder setPitchType(String pitchType) {
            this.pitchType = pitchType;
            return this;
        }

        public PitchAnalysisResult build() {
            return new PitchAnalysisResult(this);
        }
    }
}