/*
 * TripEntity.java — сущность для базы данных Room.
 * Хранит данные о поездке: время старта/окончания, дистанция, макс. скорость.
 * Автоматически генерирует ID.
 */

package com.example.autodiag.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips")
public class TripEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long startTime;   // timestamp старта
    public long endTime;     // timestamp окончания
    public double distance;  // пройденная дистанция, км
    public int maxSpeed;     // максимальная скорость, км/ч
    public double avgSpeed;  // средняя скорость, км/ч
    public String dataJson;  // запас под дополнительные данные

    public TripEntity() {
        this.startTime = System.currentTimeMillis();
        this.distance = 0;
        this.maxSpeed = 0;
        this.avgSpeed = 0;
    }

    public void finishTrip() {
        this.endTime = System.currentTimeMillis();
    }
}