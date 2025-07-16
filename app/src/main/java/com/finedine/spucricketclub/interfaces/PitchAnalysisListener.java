package com.finedine.spucricketclub.interfaces;

import com.finedine.spucricketclub.models.PitchAnalysisResult;

/**
 * Interface for receiving pitch analysis results
 */
public interface PitchAnalysisListener {
    /**
     * Called when pitch calibration is complete and analysis can begin
     */
    void onCalibrationComplete();

    /**
     * Called when new pitch analysis results are available
     *
     * @param result The analysis result
     */
    void onPitchAnalysisResult(PitchAnalysisResult result);
}