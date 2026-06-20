package com.example.autodiag.obd;

import com.example.autodiag.models.CarProfile;

import java.util.HashMap;
import java.util.Map;

public class PIDManager {
    private Map<String, String> activePids;

    public PIDManager(CarProfile car) {
        activePids = new HashMap<>();
        Map<String, String> carPids = car.getPidMap();

        // Используем containsKey вместо getOrDefault (для API 21)
        if (carPids.containsKey("SPEED")) activePids.put("SPEED", carPids.get("SPEED"));
        else activePids.put("SPEED", "01 0D");

        if (carPids.containsKey("RPM")) activePids.put("RPM", carPids.get("RPM"));
        else activePids.put("RPM", "01 0C");

        if (carPids.containsKey("COOLANT_TEMP")) activePids.put("COOLANT_TEMP", carPids.get("COOLANT_TEMP"));
        else activePids.put("COOLANT_TEMP", "01 05");

        if (carPids.containsKey("ENGINE_LOAD")) activePids.put("ENGINE_LOAD", carPids.get("ENGINE_LOAD"));
        else activePids.put("ENGINE_LOAD", "01 04");

        if (carPids.containsKey("MAP")) activePids.put("MAP", carPids.get("MAP"));
        else activePids.put("MAP", "01 0B");

        if (carPids.containsKey("MAF")) activePids.put("MAF", carPids.get("MAF"));
        else activePids.put("MAF", "01 10");
    }

    public String getCommand(String parameter) {
        return activePids.get(parameter);
    }
}