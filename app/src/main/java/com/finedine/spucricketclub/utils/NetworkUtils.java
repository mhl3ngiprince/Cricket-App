package com.finedine.spucricketclub.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for network operations and status
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public NetworkUtils(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Check if the device has network connectivity
     *
     * @return true if connected, false otherwise
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    /**
     * Check if there's actual internet connectivity (not just network connection)
     *
     * @param timeoutMs timeout in milliseconds
     * @return true if internet is available, false otherwise
     */
    public boolean isInternetAvailable(int timeoutMs) {
        if (!isNetworkAvailable()) {
            return false;
        }

        try {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    URL url = new URL("https://www.google.com");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(timeoutMs);
                    connection.setReadTimeout(timeoutMs);
                    connection.setRequestMethod("HEAD");
                    int responseCode = connection.getResponseCode();
                    return responseCode == HttpURLConnection.HTTP_OK;
                } catch (IOException e) {
                    Log.e(TAG, "Error checking internet connection", e);
                    return false;
                }
            });

            return future.get(timeoutMs + 500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Error checking internet connection", e);
            return false;
        }
    }

    /**
     * Check if there's internet connectivity using default timeout
     *
     * @return true if internet is available, false otherwise
     */
    public boolean isInternetAvailable() {
        return isInternetAvailable(1500);  // Default timeout 1.5 seconds
    }

    /**
     * Check if the device is connected to WiFi
     *
     * @param context Application context
     * @return true if the device is connected to WiFi, false otherwise
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            // For older Android versions
            android.net.NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetworkInfo != null && wifiNetworkInfo.isConnected();
        }
    }
}
