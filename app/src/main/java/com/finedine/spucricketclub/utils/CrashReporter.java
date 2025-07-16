package com.finedine.spucricketclub.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to handle crashes and errors in the application.
 * Provides crash reporting, error logging, and recovery mechanisms.
 */
public class CrashReporter {
    private static final String TAG = "CrashReporter";
    private static final String CRASH_PREFS = "crash_reporter_prefs";
    private static final String KEY_LAST_CRASH_TIME = "last_crash_time";
    private static final String KEY_CRASH_COUNT = "crash_count";
    private static final int MAX_ERROR_COUNT = 50;

    private static CrashReporter instance;

    private final Context context;
    private final FirebaseCrashlytics crashlytics;
    private final FirebaseAnalytics analytics;
    private final SharedPreferences prefs;
    private final Map<String, Integer> errorCounts = new ConcurrentHashMap<>();
    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    // Private constructor for singleton
    private CrashReporter(Context context) {
        this.context = context.getApplicationContext();
        this.crashlytics = FirebaseCrashlytics.getInstance();
        this.analytics = FirebaseAnalytics.getInstance(context);
        this.prefs = context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE);

        // Set up custom crash handler
        setupExceptionHandler();
    }

    /**
     * Get singleton instance
     */
    public static synchronized CrashReporter getInstance(Context context) {
        if (instance == null) {
            instance = new CrashReporter(context);
        }
        return instance;
    }

    /**
     * Set up the uncaught exception handler to catch crashes
     */
    private void setupExceptionHandler() {
        // Store the default handler
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        // Set our custom handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleCrash(throwable);

            // Pass to the default handler
            if (defaultExceptionHandler != null) {
                defaultExceptionHandler.uncaughtException(thread, throwable);
            }
        });
    }

    /**
     * Handle a crash
     */
    private void handleCrash(Throwable throwable) {
        try {
            // Record crash details
            recordCrash();

            // Log the stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            Log.e(TAG, "App crash: " + stackTrace);

            // Send to Firebase Crashlytics
            crashlytics.recordException(throwable);

            // Set some custom keys to help with debugging
            crashlytics.setCustomKey("app_version", getAppVersion());
            crashlytics.setCustomKey("device_model", Build.MODEL);
            crashlytics.setCustomKey("android_version", Build.VERSION.RELEASE);
            crashlytics.setCustomKey("crash_count", getCrashCount());

        } catch (Exception e) {
            // If an error occurs while handling the crash, log it
            Log.e(TAG, "Error in crash handler", e);
        }
    }

    /**
     * Record error without crashing
     */
    public void recordError(Throwable throwable, String tag) {
        try {
            // Limit the number of errors we record
            if (shouldThrottleError(tag)) {
                return;
            }

            // Log the error
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            Log.e(TAG, "Error in " + tag + ": " + stackTrace);

            // Send to Firebase Crashlytics as non-fatal
            crashlytics.recordException(throwable);

        } catch (Exception e) {
            Log.e(TAG, "Error in recordError", e);
        }
    }

    /**
     * Record error with custom parameters
     */
    public void recordError(Throwable throwable, String tag, Map<String, String> params) {
        // Add the custom parameters to Crashlytics
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                crashlytics.setCustomKey(entry.getKey(), entry.getValue());
            }
        }

        // Record the error
        recordError(throwable, tag);
    }

    /**
     * Record a crash event
     */
    private void recordCrash() {
        long now = System.currentTimeMillis();
        int crashCount = getCrashCount() + 1;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LAST_CRASH_TIME, now);
        editor.putInt(KEY_CRASH_COUNT, crashCount);
        editor.apply();
    }

    /**
     * Get the number of crashes
     */
    private int getCrashCount() {
        return prefs.getInt(KEY_CRASH_COUNT, 0);
    }

    /**
     * Get the time of the last crash
     */
    private long getLastCrashTime() {
        return prefs.getLong(KEY_LAST_CRASH_TIME, 0);
    }

    /**
     * Check if the app has crashed recently
     */
    public boolean hasCrashedRecently() {
        long lastCrash = getLastCrashTime();
        long now = System.currentTimeMillis();
        // Check if crash was within the last 24 hours
        return (now - lastCrash) < (24 * 60 * 60 * 1000);
    }

    /**
     * Reset crash counter
     */
    public void resetCrashCount() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CRASH_COUNT, 0);
        editor.apply();
    }

    /**
     * Check if we should throttle error reporting for a specific tag
     */
    private boolean shouldThrottleError(String tag) {
        // Get current count for this tag
        Integer count = errorCounts.get(tag);
        if (count == null) {
            count = 0;
        }

        // If we've seen too many errors of this type, throttle it
        if (count >= MAX_ERROR_COUNT) {
            return true;
        }

        // Otherwise increment the count and allow the error
        errorCounts.put(tag, count + 1);
        return false;
    }

    /**
     * Get app version name
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Format date for logging
     */
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date(timestamp));
    }
}