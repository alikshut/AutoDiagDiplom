package com.example.autodiag.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips")
public class TripEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long startTime;  // timestamp
    public long endTime;    // timestamp
    public double distance;
    public int maxSpeed;
    public double avgSpeed;
    public String dataJson;

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
