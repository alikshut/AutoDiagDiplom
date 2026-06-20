package com.example.autodiag.models;

import java.util.HashMap;
import java.util.Map;

public class CarProfile {
    private String manufacturer;
    private String model;
    private int year;
    private Map<String, String> pidMap;

    public CarProfile(String manufacturer, String model, int year) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.year = year;
        this.pidMap = new HashMap<>();
    }

    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public Map<String, String> getPidMap() { return pidMap; }
    public void setPidMap(Map<String, String> pidMap) { this.pidMap = pidMap; }

    public String toString() {
        return manufacturer + " " + model + " (" + year + ")";
    }
}