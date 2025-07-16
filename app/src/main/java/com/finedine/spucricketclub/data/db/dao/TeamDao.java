package com.finedine.spucricketclub.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.finedine.spucricketclub.data.db.entity.TeamEntity;

import java.util.List;

/**
 * Data Access Object for Team entity
 * Provides methods to access and manipulate team data in the local database
 */
@Dao
public interface TeamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TeamEntity team);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TeamEntity> teams);

    @Update
    void update(TeamEntity team);

    @Delete
    void delete(TeamEntity team);

    @Query("DELETE FROM teams WHERE id = :teamId")
    void deleteById(String teamId);

    @Query("SELECT * FROM teams WHERE id = :teamId")
    LiveData<TeamEntity> getById(String teamId);

    @Query("SELECT * FROM teams WHERE id = :teamId")
    TeamEntity getByIdSync(String teamId);

    @Query("SELECT * FROM teams")
    LiveData<List<TeamEntity>> getAll();

    @Query("SELECT * FROM teams")
    List<TeamEntity> getAllSync();

    @Query("SELECT * FROM teams WHERE teamName LIKE '%' || :query || '%'")
    LiveData<List<TeamEntity>> search(String query);

    @Query("SELECT COUNT(*) FROM teams")
    int getCount();

    @Query("DELETE FROM teams")
    void deleteAll();
}