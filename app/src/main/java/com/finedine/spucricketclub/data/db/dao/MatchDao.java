package com.finedine.spucricketclub.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.finedine.spucricketclub.data.db.entity.MatchEntity;

import java.util.List;

/**
 * Data Access Object for Match entity
 * Provides methods to access and manipulate match data in the local database
 */
@Dao
public interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MatchEntity match);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MatchEntity> matches);

    @Update
    void update(MatchEntity match);

    @Delete
    void delete(MatchEntity match);

    @Query("DELETE FROM matches WHERE id = :matchId")
    void deleteById(String matchId);

    @Query("SELECT * FROM matches WHERE id = :matchId")
    LiveData<MatchEntity> getById(String matchId);

    @Query("SELECT * FROM matches WHERE id = :matchId")
    MatchEntity getByIdSync(String matchId);

    @Query("SELECT * FROM matches")
    LiveData<List<MatchEntity>> getAll();

    @Query("SELECT * FROM matches")
    List<MatchEntity> getAllSync();

    @Query("SELECT * FROM matches WHERE teamAId = :teamId OR teamBId = :teamId")
    LiveData<List<MatchEntity>> getMatchesByTeam(String teamId);

    @Query("SELECT * FROM matches WHERE status = :status")
    LiveData<List<MatchEntity>> getMatchesByStatus(String status);

    @Query("SELECT * FROM matches WHERE scheduledDate >= :fromDate AND scheduledDate <= :toDate")
    LiveData<List<MatchEntity>> getMatchesByDateRange(long fromDate, long toDate);

    @Query("SELECT COUNT(*) FROM matches")
    int getCount();

    @Query("DELETE FROM matches")
    void deleteAll();
}