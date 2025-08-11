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
import com.finedine.spucricketclub.utils.CrashHandler;
import com.finedine.spucricketclub.data.FirebaseInitializer;
import com.finedine.spucricketclub.data.USSADataImporter;
import com.finedine.spucricketclub.data.PlayerDatabase;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

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
        try {
            if (!FirebaseInitializer.initializeFirebase(this)) {
                Log.e(TAG, "Firebase initialization failed using helper");
                showToast("Error initializing app services. Some features may not work properly.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization crashed", e);
            showToast("Critical error initializing Firebase: " + e.getMessage());
        }

        try {
            AppDatabase.getInstance(this);
            Log.i(TAG, "Room database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Room database initialization failed", e);
            showToast("Database initialization failed: " + e.getMessage());
        }

        try {
            CricketRepository.getInstance(this);
            Log.i(TAG, "Cricket repository initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Cricket repository initialization failed", e);
        }

        try {
            DatabaseSyncManager syncManager = DatabaseSyncManager.getInstance(this);
            syncManager.initialize();
            Log.i(TAG, "Database sync manager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Database sync manager initialization failed", e);
        }

        try {
            CrashReporter.getInstance(this);
            Log.i(TAG, "Crash reporter initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Crash reporter initialization failed", e);
        }

        PlayerDatabase db = PlayerDatabase.getInstance(this);
        USSADataImporter importer = new USSADataImporter(this);
        AppDatabase roomDb = AppDatabase.getInstance(this);

        // Placeholders for JSON data if missing
        ensureAssetExists("ussa_2024_player_stats.json", getDefaultPlayerStatsJson());
        ensureAssetExists("ussa_2024_fixtures.json", getDefaultFixturesJson());

        // All imports/checks off main thread!
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (db.getAllPlayers().isEmpty()) {
                    importer.importPlayerStatsFromJson("ussa_2024_player_stats.json");
                }
                if (roomDb.matchDao().getAllSync().isEmpty()) {
                    importer.importFixturesFromJson("ussa_2024_fixtures.json");
                    Log.i(TAG, "Imported USSA fixtures from JSON");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error importing USSA data: " + e.getMessage(), e);
                showToast("Error importing USSA Cricket data: " + e.getMessage());
            }
        });

        Log.d(TAG, "Application initialization completed");
    }

    private boolean assetExists(String fn) {
        try (InputStream is = getAssets().open(fn)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureAssetExists(String filename, String defaultContent) {
        try {
            if (!assetExists(filename)) {
                // Create the JSON in internal storage if not present in assets
                OutputStream out = openFileOutput(filename, MODE_PRIVATE);
                out.write(defaultContent.getBytes(StandardCharsets.UTF_8));
                out.close();
                Log.w(TAG, "Created missing asset: " + filename);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not create asset: " + filename, e);
        }
    }

    private String getDefaultFixturesJson() {
        String sample = "[ {\"team_home\":\"SPU\", \"team_away\":\"CUT\", \"venue\":\"SPU Cricket Oval\", \"date\":\"2024-07-12\"} ]";
        return sample;
    }

    private String getDefaultPlayerStatsJson() {
        String sample = "[ {\"Player Name\":\"John Doe\", \"Role\":\"BATSMAN\"}, {\"Player Name\":\"Sam Bowler\", \"Role\":\"BOWLER\"} ]";
        return sample;
    }

    private void showToast(String msg) {
        android.os.Handler mainHandler = new android.os.Handler(getMainLooper());
        mainHandler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}
