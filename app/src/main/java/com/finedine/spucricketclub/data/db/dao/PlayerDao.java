package com.finedine.spucricketclub.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.finedine.spucricketclub.data.db.entity.PlayerEntity;

import java.util.List;

/**
 * Data Access Object for Player entity
 * Provides methods to access and manipulate player data in the local database
 */
@Dao
public interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlayerEntity player);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PlayerEntity> players);

    @Update
    void update(PlayerEntity player);

    @Delete
    void delete(PlayerEntity player);

    @Query("DELETE FROM players WHERE id = :playerId")
    void deleteById(String playerId);

    @Query("SELECT * FROM players WHERE id = :playerId")
    LiveData<PlayerEntity> getById(String playerId);

    @Query("SELECT * FROM players WHERE id = :playerId")
    PlayerEntity getByIdSync(String playerId);

    @Query("SELECT * FROM players")
    LiveData<List<PlayerEntity>> getAll();

    @Query("SELECT * FROM players")
    List<PlayerEntity> getAllSync();

    @Query("SELECT * FROM players WHERE teamId = :teamId")
    LiveData<List<PlayerEntity>> getByTeam(String teamId);

    @Query("SELECT * FROM players WHERE teamId = :teamId")
    List<PlayerEntity> getByTeamSync(String teamId);

    @Query("SELECT * FROM players WHERE role = :role")
    LiveData<List<PlayerEntity>> getByRole(String role);

    @Query("SELECT * FROM players WHERE name LIKE '%' || :query || '%'")
    LiveData<List<PlayerEntity>> search(String query);

    @Query("SELECT COUNT(*) FROM players")
    int getCount();

    @Query("DELETE FROM players")
    void deleteAll();
}
