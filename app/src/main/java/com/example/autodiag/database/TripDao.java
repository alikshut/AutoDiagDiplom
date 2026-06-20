package com.example.autodiag.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.autodiag.models.TripEntity;
import java.util.List;

@Dao
public interface TripDao {
    @Insert
    void insertTrip(TripEntity trip);

    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<TripEntity> getAllTrips();

    @Query("DELETE FROM trips")
    void deleteAll();
}