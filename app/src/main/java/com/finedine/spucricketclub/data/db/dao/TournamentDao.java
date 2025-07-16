package com.finedine.spucricketclub.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.finedine.spucricketclub.data.db.entity.TournamentEntity;

import java.util.List;

/**
 * Data Access Object for Tournament entity
 * Provides methods to access and manipulate tournament data in the local database
 */
@Dao
public interface TournamentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TournamentEntity tournament);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TournamentEntity> tournaments);

    @Update
    void update(TournamentEntity tournament);

    @Delete
    void delete(TournamentEntity tournament);

    @Query("DELETE FROM tournaments WHERE id = :tournamentId")
    void deleteById(String tournamentId);

    @Query("SELECT * FROM tournaments WHERE id = :tournamentId")
    LiveData<TournamentEntity> getById(String tournamentId);

    @Query("SELECT * FROM tournaments WHERE id = :tournamentId")
    TournamentEntity getByIdSync(String tournamentId);

    @Query("SELECT * FROM tournaments")
    LiveData<List<TournamentEntity>> getAll();

    @Query("SELECT * FROM tournaments")
    List<TournamentEntity> getAllSync();

    @Query("SELECT * FROM tournaments WHERE name LIKE '%' || :query || '%'")
    LiveData<List<TournamentEntity>> search(String query);

    @Query("SELECT * FROM tournaments WHERE status = :status")
    LiveData<List<TournamentEntity>> getByStatus(String status);

    @Query("SELECT COUNT(*) FROM tournaments")
    int getCount();

    @Query("DELETE FROM tournaments")
    void deleteAll();
}