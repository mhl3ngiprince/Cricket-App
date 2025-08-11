package com.finedine.spucricketclub.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.data.PlayerDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class USSADataImporter {
    private Context context;

    public USSADataImporter(Context ctx) {
        this.context = ctx;
    }

    // Example: Load fixtures from a local JSON file, to customize for API/CSV
    public void importFixturesFromJson(String assetsFileName) {
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(assetsFileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // adjust to your JSON date format
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject obj = arr.getJSONObject(i);
                String teamHome = obj.optString("team_home");
                String teamAway = obj.optString("team_away");
                String venue = obj.optString("venue");
                String dateStr = obj.optString("date");
                Date matchDate = null;
                try {
                    matchDate = sdf.parse(dateStr);
                } catch (ParseException ignore) {
                }
                Match m = new Match();
                m.setVenue(venue);
                m.setMatchDate(matchDate);
                m.setMatchName(teamHome + " vs " + teamAway);
                m.setTeamBatting(new com.finedine.spucricketclub.cricket.Team(teamHome));
                m.setTeamBowling(new com.finedine.spucricketclub.cricket.Team(teamAway));
                // To persist, insert with Room DAO if available, else store as in-memory list
                // e.g.: AppDatabase.getInstance(context).matchDao().insert(m);
            }
        } catch (Exception e) {
            Log.e("USSADataImporter", "ImportFixtures error: " + e);
        }
    }

    // For future use, pull data from REST API
    public void importFixturesFromApi(String endpointUrl) {
        // TODO: Make network call to endpointUrl, parse results into DB
    }

    // Import Kimberley Cricket/Northern Cape teams
    public void importKimberleyCricketData(String source) {
        // TODO: Parse CSV or structured file, update Room DB
    }
    // Additional import methods can be added as data sources/API become available

    public void importPlayerStatsFromJson(String filename) {
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(filename);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            PlayerDatabase db = PlayerDatabase.getInstance(context);
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("Player Name");
                String roleStr = obj.optString("Role", "BATSMAN").toUpperCase();
                Player.PlayerRole role = Player.PlayerRole.valueOf(roleStr); // adjust enum name as needed
                db.addPlayer(name, role);
            }
        } catch (Exception e) {
            Log.e("USSADataImporter", "ImportPlayers error: " + e);
        }
    }
}
