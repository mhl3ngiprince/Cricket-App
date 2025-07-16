package com.finedine.spucricketclub.data;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;

/**
 * Helper class to safely initialize Firebase with error handling
 */
public class FirebaseInitializer {

    private static final String TAG = "FirebaseInitializer";

    /**
     * Initialize Firebase safely with error handling
     * @param context Application context
     * @return true if initialization was successful, false otherwise
     */
    public static boolean initializeFirebase(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                Log.i(TAG, "Firebase initialized successfully");
            } else {
                Log.i(TAG, "Firebase already initialized");
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            return false;
        }
    }
}
