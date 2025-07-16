package com.finedine.spucricketclub.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.finedine.spucricketclub.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom view that displays a heatmap of the cricket pitch
 * showing areas of interest with different colors based on analysis
 */
public class PitchHeatmapView extends View {

    // Constants for drawing
    private static final float PITCH_WIDTH_RATIO = 0.6f;
    private static final float PITCH_LENGTH_RATIO = 0.9f;
    private static final float CREASE_WIDTH_RATIO = 0.1f;

    // Paint objects for drawing
    private Paint pitchPaint;
    private Paint creaseLinePaint;
    private Paint textPaint;
    private Paint grassPaint;
    private Paint cracksPaint;
    private Paint heatmapPaint;
    private Paint boundaryPaint;

    // Data for visualization
    private Map<String, Float> analysisData = new HashMap<>();

    // Drawing measurements
    private RectF pitchRect;
    private float creaseWidth;
    private float creaseLength;

    public PitchHeatmapView(Context context) {
        super(context);
        init();
    }

    public PitchHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PitchHeatmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Set up paint objects
        pitchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pitchPaint.setColor(ContextCompat.getColor(getContext(), R.color.pitch_brown));
        pitchPaint.setStyle(Paint.Style.FILL);

        creaseLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        creaseLinePaint.setColor(Color.WHITE);
        creaseLinePaint.setStyle(Paint.Style.STROKE);
        creaseLinePaint.setStrokeWidth(4f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);

        grassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        grassPaint.setColor(ContextCompat.getColor(getContext(), R.color.spu_success));
        grassPaint.setStyle(Paint.Style.FILL);
        grassPaint.setAlpha(120);

        cracksPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cracksPaint.setColor(ContextCompat.getColor(getContext(), R.color.spu_danger));
        cracksPaint.setStyle(Paint.Style.FILL);
        cracksPaint.setAlpha(120);

        heatmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heatmapPaint.setStyle(Paint.Style.FILL);

        boundaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setColor(ContextCompat.getColor(getContext(), R.color.spu_dark_blue));
        boundaryPaint.setStrokeWidth(8f);

        pitchRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate pitch dimensions
        float pitchWidth = w * PITCH_WIDTH_RATIO;
        float pitchLength = h * PITCH_LENGTH_RATIO;
        float leftMargin = (w - pitchWidth) / 2;
        float topMargin = (h - pitchLength) / 2;

        pitchRect.set(leftMargin, topMargin, leftMargin + pitchWidth, topMargin + pitchLength);

        // Calculate crease dimensions
        creaseWidth = pitchWidth * CREASE_WIDTH_RATIO;
        creaseLength = pitchWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the cricket ground (oval)
        RectF groundRect = new RectF(10, 10, getWidth() - 10, getHeight() - 10);
        canvas.drawOval(groundRect, boundaryPaint);

        // Draw the pitch base
        canvas.drawRect(pitchRect, pitchPaint);

        // Draw batting creases
        float topCreaseY = pitchRect.top + creaseWidth;
        float bottomCreaseY = pitchRect.bottom - creaseWidth;

        // Top crease
        canvas.drawLine(pitchRect.left, topCreaseY, pitchRect.right, topCreaseY, creaseLinePaint);

        // Bottom crease
        canvas.drawLine(pitchRect.left, bottomCreaseY, pitchRect.right, bottomCreaseY, creaseLinePaint);

        // Draw middle stump line
        canvas.drawLine(pitchRect.centerX(), pitchRect.top,
                pitchRect.centerX(), pitchRect.bottom, creaseLinePaint);

        // Draw heatmap based on analysis data
        drawHeatmap(canvas);

        // Draw legends
        drawLegends(canvas);
    }

    private void drawHeatmap(Canvas canvas) {
        if (analysisData.isEmpty()) {
            return;
        }

        // Get grass and moisture values
        float grassValue = analysisData.getOrDefault("grass", 0f);
        float moistureValue = analysisData.getOrDefault("moisture", 0f);
        float cracksValue = analysisData.getOrDefault("cracks", 0f);

        // Draw grass coverage
        if (grassValue > 0) {
            // Adjust alpha based on grass percentage
            int alpha = (int) (grassValue * 2.55f); // Scale 0-100 to 0-255
            grassPaint.setAlpha(alpha);

            Path grassPath = getRandomizedPath(pitchRect, grassValue / 100f);
            canvas.drawPath(grassPath, grassPaint);
        }

        // Draw moisture areas (blue tint)
        if (moistureValue > 0) {
            Paint moisturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            moisturePaint.setColor(ContextCompat.getColor(getContext(), R.color.spu_blue));
            moisturePaint.setStyle(Paint.Style.FILL);

            // Adjust alpha based on moisture percentage
            int alpha = (int) (moistureValue * 1.5f); // Scale 0-100 to 0-150
            moisturePaint.setAlpha(Math.min(150, alpha));

            // Draw moisture typically on the top half of pitch
            RectF moistureRect = new RectF(pitchRect.left + pitchRect.width() * 0.2f,
                    pitchRect.top + pitchRect.height() * 0.1f,
                    pitchRect.right - pitchRect.width() * 0.2f,
                    pitchRect.top + pitchRect.height() * 0.6f);

            Path moisturePath = getRandomizedPath(moistureRect, moistureValue / 100f);
            canvas.drawPath(moisturePath, moisturePaint);
        }

        // Draw cracks 
        if (cracksValue > 0) {
            // Draw a series of small lines to represent cracks
            int numCracks = (int) (cracksValue / 5); // 1 crack line per 5% of crack coverage
            float crackLength = pitchRect.width() / 6;

            for (int i = 0; i < numCracks; i++) {
                float startX = pitchRect.left + (float) (Math.random() * pitchRect.width() * 0.8f + 0.1f * pitchRect.width());
                float startY = pitchRect.top + (float) (Math.random() * pitchRect.height() * 0.8f + 0.1f * pitchRect.height());
                float angle = (float) (Math.random() * 180);

                float endX = startX + (float) (crackLength * Math.cos(Math.toRadians(angle)));
                float endY = startY + (float) (crackLength * Math.sin(Math.toRadians(angle)));

                canvas.drawLine(startX, startY, endX, endY, cracksPaint);
            }
        }

        // Good length area highlighting
        if (analysisData.containsKey("bounce")) {
            float bounceValue = analysisData.getOrDefault("bounce", 50f);
            // Draw "good length" area with gradient based on bounce prediction
            LinearGradient gradient = new LinearGradient(
                    pitchRect.centerX(), pitchRect.top + pitchRect.height() * 0.3f,
                    pitchRect.centerX(), pitchRect.top + pitchRect.height() * 0.7f,
                    ContextCompat.getColor(getContext(), R.color.spu_gold),
                    ContextCompat.getColor(getContext(), R.color.spu_gold_dark),
                    Shader.TileMode.CLAMP);

            Paint goodLengthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            goodLengthPaint.setShader(gradient);
            goodLengthPaint.setAlpha(100);

            // Size based on bounce value - higher bounce means smaller good length area
            float areaSize = Math.max(0.1f, 0.5f - (bounceValue - 50) / 200f);
            RectF goodLengthRect = new RectF(
                    pitchRect.left + pitchRect.width() * 0.3f,
                    pitchRect.top + pitchRect.height() * (0.5f - areaSize / 2),
                    pitchRect.right - pitchRect.width() * 0.3f,
                    pitchRect.top + pitchRect.height() * (0.5f + areaSize / 2));

            canvas.drawRoundRect(goodLengthRect, 20, 20, goodLengthPaint);
        }
    }

    private Path getRandomizedPath(RectF baseRect, float coverage) {
        // Create a path with randomly adjusted points to simulate natural coverage
        Path path = new Path();

        int points = 12; // Number of points around the perimeter
        float[] xPoints = new float[points];
        float[] yPoints = new float[points];

        // Calculate base points evenly spaced
        for (int i = 0; i < points; i++) {
            float angle = (float) (2 * Math.PI * i / points);
            float radius = baseRect.width() / 2 * coverage;
            xPoints[i] = baseRect.centerX() + (float) (Math.cos(angle) * radius);
            yPoints[i] = baseRect.centerY() + (float) (Math.sin(angle) * radius);

            // Add some randomness
            xPoints[i] += (float) ((Math.random() - 0.5) * baseRect.width() * 0.2);
            yPoints[i] += (float) ((Math.random() - 0.5) * baseRect.height() * 0.2);
        }

        // Create the path
        path.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < points; i++) {
            path.lineTo(xPoints[i], yPoints[i]);
        }
        path.close();

        return path;
    }

    private void drawLegends(Canvas canvas) {
        // Draw legend indicators at the bottom of the view
        if (analysisData.isEmpty()) {
            return;
        }

        float legendY = getHeight() - 40;
        float legendX = 40;
        float legendSize = 20;
        float textOffset = 30;

        // Grass legend
        grassPaint.setAlpha(180);
        canvas.drawRect(legendX, legendY - legendSize, legendX + legendSize, legendY, grassPaint);
        canvas.drawText("Grass", legendX + legendSize + 5, legendY, textPaint);

        legendX += textOffset + textPaint.measureText("Grass") + 20;

        // Moisture legend
        Paint moisturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moisturePaint.setColor(ContextCompat.getColor(getContext(), R.color.spu_blue));
        moisturePaint.setAlpha(180);
        canvas.drawRect(legendX, legendY - legendSize, legendX + legendSize, legendY, moisturePaint);
        canvas.drawText("Moisture", legendX + legendSize + 5, legendY, textPaint);

        legendX += textOffset + textPaint.measureText("Moisture") + 20;

        // Cracks legend
        cracksPaint.setAlpha(180);
        canvas.drawRect(legendX, legendY - legendSize, legendX + legendSize, legendY, cracksPaint);
        canvas.drawText("Cracks", legendX + legendSize + 5, legendY, textPaint);
    }

    /**
     * Update the view with new pitch analysis data
     *
     * @param data Map of data values (grass, moisture, cracks, etc.)
     */
    public void updateData(Map<String, Float> data) {
        this.analysisData.clear();
        this.analysisData.putAll(data);
        invalidate(); // Redraw the view
    }
}