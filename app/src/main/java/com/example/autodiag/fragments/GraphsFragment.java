package com.example.autodiag.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import com.example.autodiag.R;
import com.example.autodiag.utils.DataRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class GraphsFragment extends Fragment {

    private LineChart chart;
    private TabLayout tabLayout;
    private TextView tvCurrentValue;
    private int currentTab = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graphs, container, false);

        chart = view.findViewById(R.id.chart);
        tabLayout = view.findViewById(R.id.tabLayout);
        tvCurrentValue = view.findViewById(R.id.tv_current_value);

        // Настройка графика
        chart.getDescription().setEnabled(false);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getLegend().setTextColor(Color.WHITE);

        // Табы
        tabLayout.addTab(tabLayout.newTab().setText("Скорость"));
        tabLayout.addTab(tabLayout.newTab().setText("Обороты"));
        tabLayout.addTab(tabLayout.newTab().setText("Температура"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { currentTab = tab.getPosition(); updateChart(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Подписка на реальные данные
        DataRepository.getInstance().getSpeedHistory().observe(getViewLifecycleOwner(), data -> updateChart());
        DataRepository.getInstance().getRpmHistory().observe(getViewLifecycleOwner(), data -> updateChart());
        DataRepository.getInstance().getTempHistory().observe(getViewLifecycleOwner(), data -> updateChart());

        return view;
    }

    private void updateChart() {
        List<Entry> entries = new ArrayList<>();
        String label;
        int color;
        List<Integer> data = null;

        switch (currentTab) {
            case 0:
                data = DataRepository.getInstance().getSpeedHistory().getValue();
                label = "Скорость (км/ч)";
                color = Color.rgb(76, 175, 80);
                break;
            case 1:
                data = DataRepository.getInstance().getRpmHistory().getValue();
                label = "Обороты (RPM)";
                color = Color.rgb(255, 152, 0);
                break;
            default:
                data = DataRepository.getInstance().getTempHistory().getValue();
                label = "Температура (°C)";
                color = Color.rgb(33, 150, 243);
        }

        if (data != null && !data.isEmpty()) {
            for (int i = 0; i < data.size(); i++) {
                entries.add(new Entry(i, data.get(i)));
            }
            int lastValue = data.get(data.size() - 1);
            String unit = currentTab == 0 ? "км/ч" : (currentTab == 1 ? "RPM" : "°C");
            tvCurrentValue.setText("Текущее значение: " + lastValue + " " + unit);
        } else {
            tvCurrentValue.setText("Нет данных. Подключитесь к автомобилю.");
        }

        if (entries.isEmpty()) return;

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(color);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setDrawValues(false);

        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }
}