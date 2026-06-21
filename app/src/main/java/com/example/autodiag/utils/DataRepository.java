/*
 * DataRepository.java — Singleton-хранилище данных для графиков.
 * Хранит историю скорости, оборотов и температуры (до 50 точек).
 * Использует LiveData для автоматического обновления графиков.
 */

package com.example.autodiag.utils;

import java.util.ArrayList;
import java.util.List;
import androidx.lifecycle.MutableLiveData;

public class DataRepository {
    private static DataRepository instance;
    private MutableLiveData<List<Integer>> speedHistory;
    private MutableLiveData<List<Integer>> rpmHistory;
    private MutableLiveData<List<Integer>> tempHistory;

    private DataRepository() {
        speedHistory = new MutableLiveData<>(new ArrayList<>());
        rpmHistory = new MutableLiveData<>(new ArrayList<>());
        tempHistory = new MutableLiveData<>(new ArrayList<>());
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) instance = new DataRepository();
        return instance;
    }

    // Добавление новой точки данных
    public void addDataPoint(int speed, int rpm, int temp) {
        addToHistory(speedHistory, speed);
        addToHistory(rpmHistory, rpm);
        addToHistory(tempHistory, temp);
    }

    // Добавление значения в историю с ограничением 50 точек
    private void addToHistory(MutableLiveData<List<Integer>> history, int value) {
        List<Integer> list = history.getValue();
        if (list == null) list = new ArrayList<>();
        list.add(value);
        if (list.size() > 50) list.remove(0); // оставляем только последние 50
        history.postValue(list);
    }

    // Геттеры для LiveData (подписка на изменения)
    public MutableLiveData<List<Integer>> getSpeedHistory() { return speedHistory; }
    public MutableLiveData<List<Integer>> getRpmHistory() { return rpmHistory; }
    public MutableLiveData<List<Integer>> getTempHistory() { return tempHistory; }
}