/*
 * TripDao.java — Data Access Object для работы с таблицей поездок.
 * Содержит методы: вставка, получение всех записей, удаление всех.
 */

package com.example.autodiag.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.autodiag.models.TripEntity;
import java.util.List;

@Dao
public interface TripDao {
    // Вставка новой поездки в БД
    @Insert
    void insertTrip(TripEntity trip);

    // Получение всех поездок, отсортированных по убыванию ID (свежие сверху)
    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<TripEntity> getAllTrips();

    // Удаление всех записей (используется для очистки истории)
    @Query("DELETE FROM trips")
    void deleteAll();
}