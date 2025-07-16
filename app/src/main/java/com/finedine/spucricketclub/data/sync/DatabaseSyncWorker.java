package com.finedine.spucricketclub.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Worker class that handles periodic synchronization of data between local and remote databases
 * when the device has internet connectivity.
 */
public class DatabaseSyncWorker extends Worker {
    private static final String TAG = "DatabaseSyncWorker";

    public DatabaseSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting database sync worker...");

            // Get the sync manager
            DatabaseSyncManager syncManager = DatabaseSyncManager.getInstance(getApplicationContext());

            // Check network availability
            if (!syncManager.isNetworkAvailable()) {
                Log.d(TAG, "Network unavailable, sync deferred");
                return Result.retry();
            }

            // Sync local data to Firebase
            syncManager.syncLocalToFirebase();

            // Sync Firebase data to local
            syncManager.syncFirebaseToLocal();

            Log.d(TAG, "Database sync worker completed successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error during background sync: " + e.getMessage());
            return Result.failure();
        }
    }
}