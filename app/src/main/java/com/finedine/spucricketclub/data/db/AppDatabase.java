package com.finedine.spucricketclub.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.finedine.spucricketclub.data.db.converter.DateConverter;
import com.finedine.spucricketclub.data.db.converter.JsonConverter;
import com.finedine.spucricketclub.data.db.dao.MatchDao;
import com.finedine.spucricketclub.data.db.dao.PlayerDao;
import com.finedine.spucricketclub.data.db.dao.TeamDao;
import com.finedine.spucricketclub.data.db.dao.TournamentDao;
import com.finedine.spucricketclub.data.db.entity.MatchEntity;
import com.finedine.spucricketclub.data.db.entity.PlayerEntity;
import com.finedine.spucricketclub.data.db.entity.TeamEntity;
import com.finedine.spucricketclub.data.db.entity.TournamentEntity;
import com.finedine.spucricketclub.data.worker.DatabaseSeedWorker;

/**
 * Main Room database for the cricket app
 */
@Database(entities = {
        PlayerEntity.class,
        TeamEntity.class,
        MatchEntity.class,
        TournamentEntity.class
}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class, JsonConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "spu_cricket_db";
    private static volatile AppDatabase INSTANCE;

    // Define DAOs
    public abstract PlayerDao playerDao();

    public abstract TeamDao teamDao();

    public abstract MatchDao matchDao();

    public abstract TournamentDao tournamentDao();

    /**
     * Get a database instance - creates one if none exists
     */
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Initialize database with sample data when first created
                                    OneTimeWorkRequest seedRequest =
                                            new OneTimeWorkRequest.Builder(DatabaseSeedWorker.class)
                                                    .build();
                                    WorkManager.getInstance(context).enqueue(seedRequest);
                                }
                            })
                            .fallbackToDestructiveMigration() // For simplicity in development
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}