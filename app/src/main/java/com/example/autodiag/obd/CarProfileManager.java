package com.example.autodiag.obd;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.autodiag.models.CarProfile;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarProfileManager {
    private SharedPreferences prefs;
    private Gson gson;

    public CarProfileManager(Context context) {
        this.prefs = context.getSharedPreferences("autodiag_car", Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public List<CarProfile> getPresetProfiles() {
        List<CarProfile> profiles = new ArrayList<>();

        // Toyota Corolla 2006
        CarProfile corolla = new CarProfile("Toyota", "Corolla", 2006);
        Map<String, String> pids1 = new HashMap<>();
        pids1.put("SPEED", "01 0D");
        pids1.put("RPM", "01 0C");
        pids1.put("COOLANT_TEMP", "01 05");
        pids1.put("ENGINE_LOAD", "01 04");
        pids1.put("MAP", "01 0B");
        pids1.put("MAF", "01 10");
        corolla.setPidMap(pids1);
        profiles.add(corolla);

        // Generic OBD2
        CarProfile generic = new CarProfile("Generic", "OBD2", 0);
        Map<String, String> pids2 = new HashMap<>();
        pids2.put("SPEED", "01 0D");
        pids2.put("RPM", "01 0C");
        pids2.put("COOLANT_TEMP", "01 05");
        pids2.put("ENGINE_LOAD", "01 04");
        pids2.put("MAP", "01 0B");
        generic.setPidMap(pids2);
        profiles.add(generic);

        //Subaru forester sg5
        CarProfile subaru = new CarProfile("Subaru", "Forester SG5", 2005);
        Map<String, String> subaruPids = new HashMap<>();
        subaruPids.put("SPEED", "01 0D");
        subaruPids.put("RPM", "01 0C");
        subaruPids.put("COOLANT_TEMP", "01 05");
        subaruPids.put("ENGINE_LOAD", "01 04");
        subaruPids.put("MAP", "01 0B");
        subaruPids.put("MAF", "01 10");
        subaru.setPidMap(subaruPids);
        profiles.add(subaru);

        return profiles;
    }

    public CarProfile getSelectedCar() {
        String json = prefs.getString("selected_car", null);
        if (json == null) {
            return getPresetProfiles().get(0);
        }
        return gson.fromJson(json, CarProfile.class);
    }

    public void saveSelectedCar(CarProfile car) {
        String json = gson.toJson(car);
        prefs.edit().putString("selected_car", json).apply();
    }
}