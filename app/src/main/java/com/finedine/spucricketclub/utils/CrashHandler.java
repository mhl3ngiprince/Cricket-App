package com.finedine.spucricketclub.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Global exception handler to catch and log uncaught exceptions
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Install the global exception handler
     *
     * @param context Application context
     */
    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
        Log.d(TAG, "Global crash handler installed");
    }

    /**
     * Show a toast message from any thread
     */
    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            // Log the exception
            Log.e(TAG, "FATAL EXCEPTION: " + thread.getName(), throwable);

            // Show a toast if possible
            if (thread != Looper.getMainLooper().getThread()) {
                showToast(context, "Application encountered a critical error. It will now restart.");
            }

            // Let the app crash but give it a chance to show the toast first
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
        } finally {
            // Let the default handler deal with it
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(1);
            }
        }
    }
}