package com.finedine.spucricketclub;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.finedine.spucricketclub.data.db.AppDatabase;
import com.finedine.spucricketclub.data.repository.CricketRepository;
import com.finedine.spucricketclub.data.sync.DatabaseSyncManager;
import com.finedine.spucricketclub.utils.CrashReporter;
import com.finedine.spucricketclub.data.FirebaseInitializer;
import com.finedine.spucricketclub.utils.CrashHandler;

/**
 * Main application class for initializing app-wide components
 */
public class CricketApplication extends Application implements Configuration.Provider {
    private static final String TAG = "CricketApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Install global crash handler
        CrashHandler.install(this);

        // Initialize Firebase safely using our helper
        if (!FirebaseInitializer.initializeFirebase(this)) {
            Log.e(TAG, "Firebase initialization failed using helper");
            Toast.makeText(this, "Error initializing app services. Some features may not work properly.", Toast.LENGTH_LONG).show();
        }

        // Initialize Room database
        try {
            AppDatabase.getInstance(this);
            Log.i(TAG, "Room database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Room database initialization failed", e);
            Toast.makeText(this, "Database initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Initialize repository
        try {
            CricketRepository.getInstance(this);
            Log.i(TAG, "Cricket repository initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Cricket repository initialization failed", e);
        }

        // Initialize database sync manager
        try {
            DatabaseSyncManager syncManager = DatabaseSyncManager.getInstance(this);
            syncManager.initialize();
            Log.i(TAG, "Database sync manager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Database sync manager initialization failed", e);
        }

        // Initialize crash reporter
        try {
            CrashReporter.getInstance(this);
            Log.i(TAG, "Crash reporter initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Crash reporter initialization failed", e);
        }

        Log.d(TAG, "Application initialization completed");
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}
