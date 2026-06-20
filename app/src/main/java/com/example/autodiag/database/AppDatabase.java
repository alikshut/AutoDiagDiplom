package com.example.autodiag.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import com.example.autodiag.models.TripEntity;

@Database(entities = {TripEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract TripDao tripDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "autodiag_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}