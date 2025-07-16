package com.finedine.spucricketclub.cricket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates visual shot maps for cricket analysis
 */
public class ShotMapGenerator {
    private static final String TAG = "ShotMapGenerator";

    // Field dimensions
    private static final float FIELD_RADIUS = 400f;
    private static final float PITCH_LENGTH = 80f;
    private static final float PITCH_WIDTH = 10f;

    // Colors
    private static final int COLOR_BOUNDARY = Color.rgb(250, 128, 114);
    private static final int COLOR_SIX = Color.rgb(255, 69, 0);
    private static final int COLOR_DOT = Color.rgb(30, 144, 255);
    private static final int COLOR_SINGLE = Color.rgb(50, 205, 50);
    private static final int COLOR_DOUBLE = Color.rgb(154, 205, 50);
    private static final int COLOR_TRIPLE = Color.rgb(218, 165, 32);
    private static final int COLOR_WICKET = Color.rgb(178, 34, 34);
    private static final int COLOR_FIELD = Color.rgb(34, 139, 34);
    private static final int COLOR_PITCH = Color.rgb(210, 180, 140);

    // Singleton instance
    private static ShotMapGenerator instance;

    private Context context;
    private Random random = new Random();

    private ShotMapGenerator(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Get singleton instance
     */
    public static synchronized ShotMapGenerator getInstance(Context context) {
        if (instance == null) {
            instance = new ShotMapGenerator(context);
        }
        return instance;
    }

    /**
     * Generate a shot map bitmap for a player in a match
     */
    public Bitmap generateShotMap(Player player, Match match, int width, int height) {
        return generateShotMapFromBalls(collectPlayerBalls(player, match), width, height);
    }

    /**
     * Generate a shot map bitmap for a list of balls
     */
    public Bitmap generateShotMapFromBalls(@NonNull List<Ball> balls, int width, int height) {
        if (balls.isEmpty()) {
            Log.d(TAG, "No balls to generate shot map from");
            return createEmptyField(width, height);
        }

        // Create bitmap with transparent background
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw cricket field
        drawCricketField(canvas, width, height);

        // Draw shots
        for (Ball ball : balls) {
            drawShot(canvas, ball, width, height);
        }

        // Draw legends
        drawLegend(canvas, width, height);

        return bitmap;
    }

    /**
     * Create an empty field with no shots
     */
    private Bitmap createEmptyField(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawCricketField(canvas, width, height);
        return bitmap;
    }

    /**
     * Draw the cricket field
     */
    private void drawCricketField(Canvas canvas, int width, int height) {
        // Set up scales and center point
        float centerX = width / 2f;
        float centerY = height / 2f;
        float scaleX = (width * 0.9f) / (FIELD_RADIUS * 2);
        float scaleY = (height * 0.9f) / (FIELD_RADIUS * 2);
        float scale = Math.min(scaleX, scaleY);

        // Draw field (oval)
        Paint fieldPaint = new Paint();
        fieldPaint.setColor(COLOR_FIELD);
        fieldPaint.setAntiAlias(true);
        fieldPaint.setStyle(Paint.Style.FILL);

        canvas.drawCircle(centerX, centerY, FIELD_RADIUS * scale, fieldPaint);

        // Draw boundary (circle)
        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.WHITE);
        boundaryPaint.setAntiAlias(true);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setStrokeWidth(5f);

        canvas.drawCircle(centerX, centerY, FIELD_RADIUS * scale, boundaryPaint);

        // Draw 30-yard circle
        boundaryPaint.setStrokeWidth(2f);
        boundaryPaint.setPathEffect(null);
        canvas.drawCircle(centerX, centerY, FIELD_RADIUS * scale * 0.5f, boundaryPaint);

        // Draw pitch rectangle
        Paint pitchPaint = new Paint();
        pitchPaint.setColor(COLOR_PITCH);
        pitchPaint.setAntiAlias(true);
        pitchPaint.setStyle(Paint.Style.FILL);

        float pitchLeft = centerX - (PITCH_WIDTH * scale / 2);
        float pitchTop = centerY - (PITCH_LENGTH * scale / 2);
        float pitchRight = centerX + (PITCH_WIDTH * scale / 2);
        float pitchBottom = centerY + (PITCH_LENGTH * scale / 2);

        canvas.drawRect(pitchLeft, pitchTop, pitchRight, pitchBottom, pitchPaint);

        // Draw pitch outline
        Paint pitchOutlinePaint = new Paint();
        pitchOutlinePaint.setColor(Color.WHITE);
        pitchOutlinePaint.setAntiAlias(true);
        pitchOutlinePaint.setStyle(Paint.Style.STROKE);
        pitchOutlinePaint.setStrokeWidth(2f);

        canvas.drawRect(pitchLeft, pitchTop, pitchRight, pitchBottom, pitchOutlinePaint);

        // Draw crease lines
        float creaseWidth = PITCH_WIDTH * scale * 1.5f;

        // Draw batting crease at bottom
        float bowlingCreaseY = centerY + (PITCH_LENGTH * scale / 2);
        canvas.drawLine(centerX - creaseWidth / 2, bowlingCreaseY,
                centerX + creaseWidth / 2, bowlingCreaseY, pitchOutlinePaint);

        // Draw batting crease at top
        float battingCreaseY = centerY - (PITCH_LENGTH * scale / 2);
        canvas.drawLine(centerX - creaseWidth / 2, battingCreaseY,
                centerX + creaseWidth / 2, battingCreaseY, pitchOutlinePaint);

        // Draw stumps at both ends
        float stumpsWidth = PITCH_WIDTH * scale * 0.4f;

        // Bottom stumps
        canvas.drawLine(centerX - stumpsWidth / 2, bowlingCreaseY,
                centerX + stumpsWidth / 2, bowlingCreaseY, pitchOutlinePaint);

        // Top stumps
        canvas.drawLine(centerX - stumpsWidth / 2, battingCreaseY,
                centerX + stumpsWidth / 2, battingCreaseY, pitchOutlinePaint);

        // Add field sections labels
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(16 * scale);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Field position names (8 main directions)
        String[] positions = {"Fine Leg", "Square Leg", "Mid Wicket", "Mid On",
                "Long Off", "Extra Cover", "Cover", "Point", "Third Man"};
        float radius = FIELD_RADIUS * scale * 0.80f;

        for (int i = 0; i < positions.length; i++) {
            double angle;
            if (i < 4) {
                // Lower half of the field (from fine leg to mid on)
                angle = Math.PI * (0.9 - 0.6 * i / 4);
            } else {
                // Upper half of the field (from long off to third man)
                angle = Math.PI * (1.1 + 0.8 * (i - 4) / 5);
            }
            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));

            canvas.drawText(positions[i], x, y, textPaint);
        }
    }

    /**
     * Draw a shot on the canvas
     */
    private void drawShot(Canvas canvas, Ball ball, int width, int height) {
        if (ball.getShotDirection() == Ball.ShotDirection.NOT_APPLICABLE) {
            // Skip balls with no shot direction
            return;
        }

        // Scale for drawing
        float centerX = width / 2f;
        float centerY = height / 2f;
        float scaleX = (width * 0.9f) / (FIELD_RADIUS * 2);
        float scaleY = (height * 0.9f) / (FIELD_RADIUS * 2);
        float scale = Math.min(scaleX, scaleY);

        // Determine angle based on shot direction
        double angle = getShotAngle(ball.getShotDirection());

        // Add some randomness to the placement
        angle += (random.nextDouble() - 0.5) * Math.PI / 8;

        // Determine distance from center based on run outcome
        float distanceFactor;
        if (ball.getRunsScored() == 6) {
            distanceFactor = 1.0f;  // Maximum distance for sixes
        } else if (ball.getRunsScored() == 4) {
            distanceFactor = 0.9f;  // Near boundary for fours
        } else if (ball.getRunsScored() == 0) {
            distanceFactor = 0.3f;  // Near pitch for dot balls
        } else {
            distanceFactor = 0.4f + ball.getRunsScored() * 0.15f;  // Scaled by runs
        }

        // Add randomness to distance
        distanceFactor *= 0.85f + random.nextFloat() * 0.3f;

        // Calculate shot position
        float shotRadius = FIELD_RADIUS * scale * distanceFactor;
        float shotX = centerX + (float) (shotRadius * Math.cos(angle));
        float shotY = centerY + (float) (shotRadius * Math.sin(angle));

        // Select color based on outcome
        int shotColor;
        if (ball.isWicket()) {
            shotColor = COLOR_WICKET;
        } else {
            switch (ball.getRunsScored()) {
                case 0:
                    shotColor = COLOR_DOT;
                    break;
                case 1:
                    shotColor = COLOR_SINGLE;
                    break;
                case 2:
                    shotColor = COLOR_DOUBLE;
                    break;
                case 3:
                    shotColor = COLOR_TRIPLE;
                    break;
                case 4:
                    shotColor = COLOR_BOUNDARY;
                    break;
                case 6:
                    shotColor = COLOR_SIX;
                    break;
                default:
                    shotColor = COLOR_SINGLE;
                    break;
            }
        }

        // Draw shot marker
        Paint shotPaint = new Paint();
        shotPaint.setColor(shotColor);
        shotPaint.setAntiAlias(true);
        shotPaint.setStyle(Paint.Style.FILL);

        // For wickets, use X shape
        if (ball.isWicket()) {
            float markerSize = 10f;
            shotPaint.setStrokeWidth(4f);
            shotPaint.setStyle(Paint.Style.STROKE);

            canvas.drawLine(shotX - markerSize, shotY - markerSize,
                    shotX + markerSize, shotY + markerSize, shotPaint);
            canvas.drawLine(shotX + markerSize, shotY - markerSize,
                    shotX - markerSize, shotY + markerSize, shotPaint);
        }
        // For boundaries, draw circle with number
        else if (ball.getRunsScored() == 4 || ball.getRunsScored() == 6) {
            canvas.drawCircle(shotX, shotY, 12f, shotPaint);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(16f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setAntiAlias(true);

            canvas.drawText(Integer.toString(ball.getRunsScored()),
                    shotX, shotY + 6f, textPaint);
        }
        // For other runs, draw colored circle
        else {
            canvas.drawCircle(shotX, shotY, 8f, shotPaint);

            if (ball.getRunsScored() > 0) {
                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(12f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setAntiAlias(true);

                canvas.drawText(Integer.toString(ball.getRunsScored()),
                        shotX, shotY + 4f, textPaint);
            }
        }
    }

    /**
     * Draw legend explaining shot markers
     */
    private void drawLegend(Canvas canvas, int width, int height) {
        Paint paintBox = new Paint();
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(16f);
        textPaint.setAntiAlias(true);

        float marginLeft = 20f;
        float marginTop = 20f;
        float boxSize = 12f;
        float padding = 5f;
        float textPaddingLeft = boxSize + 10f;
        float lineHeight = 25f;

        // Create background rectangle
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(220, 255, 255, 255));
        canvas.drawRect(marginLeft - padding, marginTop - padding,
                marginLeft + 140f, marginTop + 7 * lineHeight + padding, bgPaint);

        // Draw legend items
        String[] labels = {"Dot Ball", "Single", "Double", "Triple", "Four", "Six", "Wicket"};
        int[] colors = {COLOR_DOT, COLOR_SINGLE, COLOR_DOUBLE, COLOR_TRIPLE,
                COLOR_BOUNDARY, COLOR_SIX, COLOR_WICKET};

        for (int i = 0; i < labels.length; i++) {
            paintBox.setColor(colors[i]);
            paintBox.setStyle(Paint.Style.FILL);

            float top = marginTop + i * lineHeight;

            // For wicket, draw X
            if (i == 6) {
                paintBox.setStrokeWidth(2f);
                paintBox.setStyle(Paint.Style.STROKE);

                canvas.drawLine(marginLeft, top + boxSize / 2 - boxSize / 2,
                        marginLeft + boxSize, top + boxSize / 2 + boxSize / 2, paintBox);
                canvas.drawLine(marginLeft + boxSize, top + boxSize / 2 - boxSize / 2,
                        marginLeft, top + boxSize / 2 + boxSize / 2, paintBox);
            } else {
                canvas.drawCircle(marginLeft + boxSize / 2, top + boxSize / 2, boxSize / 2, paintBox);
            }

            canvas.drawText(labels[i], marginLeft + textPaddingLeft, top + boxSize, textPaint);
        }
    }

    /**
     * Convert shot direction to angle (in radians)
     */
    private double getShotAngle(Ball.ShotDirection direction) {
        switch (direction) {
            case FINE_LEG:
                return Math.PI * 0.8;  // ~144 degrees
            case SQUARE_LEG:
                return Math.PI * 0.6;  // ~108 degrees
            case MID_WICKET:
                return Math.PI * 0.4;  // ~72 degrees
            case MID_ON:
                return Math.PI * 0.2;  // ~36 degrees
            case LONG_ON:
                return Math.PI * 1.8;  // ~324 degrees
            case LONG_OFF:
                return Math.PI * 1.6;  // ~288 degrees
            case EXTRA_COVER:
                return Math.PI * 1.4;  // ~252 degrees
            case COVER:
                return Math.PI * 1.2;  // ~216 degrees
            case POINT:
                return Math.PI * 1.0;  // ~180 degrees
            case THIRD_MAN:
                return Math.PI * 0.9;  // ~162 degrees
            default:
                return Math.PI * 0.5;  // ~90 degrees (default is square leg)
        }
    }

    /**
     * Collect all balls played by a player in a match
     */
    private List<Ball> collectPlayerBalls(Player player, Match match) {
        List<Ball> playerBalls = new ArrayList<>();

        if (match == null || player == null) {
            return playerBalls;
        }

        for (Innings innings : match.getInnings()) {
            for (Over over : innings.getOvers()) {
                for (Ball ball : over.getBalls()) {
                    if (ball.getBatsman() != null &&
                            ball.getBatsman().getId().equals(player.getId())) {
                        playerBalls.add(ball);
                    }
                }
            }
        }

        return playerBalls;
    }

    /**
     * Generate a wagon wheel shot map
     * Similar to shot map but with lines connecting batsman to ball landing spot
     */
    public Bitmap generateWagonWheel(Player player, Match match, int width, int height) {
        List<Ball> balls = collectPlayerBalls(player, match);

        if (balls.isEmpty()) {
            Log.d(TAG, "No balls to generate wagon wheel from");
            return createEmptyField(width, height);
        }

        // Create bitmap with transparent background
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw cricket field
        drawCricketField(canvas, width, height);

        // Set up scales and center point
        float centerX = width / 2f;
        float centerY = height / 2f;
        float scaleX = (width * 0.9f) / (FIELD_RADIUS * 2);
        float scaleY = (height * 0.9f) / (FIELD_RADIUS * 2);
        float scale = Math.min(scaleX, scaleY);

        // Group balls by run value for statistics
        Map<Integer, Integer> runCounts = new HashMap<>();
        for (Ball ball : balls) {
            if (ball.getRunsScored() > 0) {
                int runs = ball.getRunsScored();
                runCounts.put(runs, runCounts.getOrDefault(runs, 0) + 1);
            }
        }

        // Draw shot lines
        for (Ball ball : balls) {
            if (ball.getShotDirection() != Ball.ShotDirection.NOT_APPLICABLE) {
                // Determine angle and distance
                double angle = getShotAngle(ball.getShotDirection());
                angle += (random.nextDouble() - 0.5) * Math.PI / 8;  // Add randomness

                float distanceFactor;
                if (ball.getRunsScored() == 6) {
                    distanceFactor = 1.0f;
                } else if (ball.getRunsScored() == 4) {
                    distanceFactor = 0.9f;
                } else {
                    distanceFactor = 0.3f + ball.getRunsScored() * 0.12f;
                }

                // Add randomness to distance
                distanceFactor *= 0.9f + random.nextFloat() * 0.2f;

                // Calculate shot position
                float shotRadius = FIELD_RADIUS * scale * distanceFactor;
                float shotX = centerX + (float) (shotRadius * Math.cos(angle));
                float shotY = centerY + (float) (shotRadius * Math.sin(angle));

                // Select color based on outcome
                int lineColor;
                switch (ball.getRunsScored()) {
                    case 0:
                        lineColor = COLOR_DOT;
                        break;
                    case 1:
                        lineColor = COLOR_SINGLE;
                        break;
                    case 2:
                        lineColor = COLOR_DOUBLE;
                        break;
                    case 3:
                        lineColor = COLOR_TRIPLE;
                        break;
                    case 4:
                        lineColor = COLOR_BOUNDARY;
                        break;
                    case 6:
                        lineColor = COLOR_SIX;
                        break;
                    default:
                        lineColor = COLOR_SINGLE;
                        break;
                }

                // Draw line from batsman to shot location
                Paint linePaint = new Paint();
                linePaint.setColor(lineColor);
                linePaint.setStrokeWidth(2f + ball.getRunsScored());  // Make line thicker for more runs
                linePaint.setAntiAlias(true);
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setAlpha(180);  // Semi-transparent

                canvas.drawLine(centerX, centerY, shotX, shotY, linePaint);

                // Draw circle at the end of the line
                Paint circlePaint = new Paint();
                circlePaint.setColor(lineColor);
                circlePaint.setAntiAlias(true);
                circlePaint.setStyle(Paint.Style.FILL);

                canvas.drawCircle(shotX, shotY, 4f + ball.getRunsScored(), circlePaint);
            }
        }

        // Draw statistics
        drawWagonWheelStats(canvas, balls, runCounts, width, height);

        // Draw legend
        drawLegend(canvas, width, height);

        return bitmap;
    }

    /**
     * Draw statistics for the wagon wheel
     */
    private void drawWagonWheelStats(Canvas canvas, List<Ball> balls,
                                     Map<Integer, Integer> runCounts, int width, int height) {
        // Calculate total runs and balls
        int totalRuns = 0;
        int totalBalls = balls.size();
        int boundaries = 0;
        int sixes = 0;

        for (Ball ball : balls) {
            totalRuns += ball.getRunsScored();
            if (ball.getRunsScored() == 4) {
                boundaries++;
            } else if (ball.getRunsScored() == 6) {
                sixes++;
            }
        }

        // Create background rectangle
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(220, 255, 255, 255));

        float statsX = width - 180f;
        float statsY = 20f;
        canvas.drawRect(statsX, statsY, width - 20f, statsY + 130f, bgPaint);

        // Draw stats text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(16f);
        textPaint.setAntiAlias(true);

        float lineHeight = 22f;
        float textX = statsX + 10f;

        canvas.drawText("Runs: " + totalRuns, textX, statsY + lineHeight, textPaint);
        canvas.drawText("Balls: " + totalBalls, textX, statsY + lineHeight * 2, textPaint);
        canvas.drawText("4s: " + boundaries, textX, statsY + lineHeight * 3, textPaint);
        canvas.drawText("6s: " + sixes, textX, statsY + lineHeight * 4, textPaint);

        // Add strike rate
        float strikeRate = totalBalls > 0 ? (totalRuns * 100.0f) / totalBalls : 0;
        canvas.drawText(String.format("SR: %.2f", strikeRate), textX, statsY + lineHeight * 5, textPaint);
    }
}