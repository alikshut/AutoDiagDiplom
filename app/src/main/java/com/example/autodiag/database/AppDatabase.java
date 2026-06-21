/*
 * AppDatabase.java — отвечает за настройку и создание базы данных Room.
 * Хранит все поездки (TripEntity). Используется синглтон для единственного экземпляра БД.
 */

package com.example.autodiag.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import com.example.autodiag.models.TripEntity;

// Указываем, какие сущности хранятся в БД, версию и отключаем экспорт схемы
@Database(entities = {TripEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    // DAO для работы с поездками
    public abstract TripDao tripDao();

    // Синглтон — всегда один экземпляр БД
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "autodiag_database")
                    .fallbackToDestructiveMigration() // при смене версии — пересоздаёт БД
                    .build();
        }
        return instance;
    }
}