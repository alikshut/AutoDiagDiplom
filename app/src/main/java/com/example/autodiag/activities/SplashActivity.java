package com.example.autodiag.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autodiag.R;
import com.example.autodiag.models.CarProfile;
import com.example.autodiag.obd.CarProfileManager;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private Spinner spinnerCar;
    private Button btnContinue;
    private TextView tvSkip;
    private CarProfileManager carProfileManager;
    private List<CarProfile> carList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        carProfileManager = new CarProfileManager(this);
        carList = carProfileManager.getPresetProfiles();

        spinnerCar = findViewById(R.id.spinner_car);
        btnContinue = findViewById(R.id.btn_continue);
        tvSkip = findViewById(R.id.tv_skip);

        SharedPreferences prefs = getSharedPreferences("autodiag_prefs", MODE_PRIVATE);
        boolean carSelected = prefs.getBoolean("car_selected", false);
        if (carSelected) {
            goToMainActivity();
            finish();
            return;
        }

        // Заполняем спиннер названиями машин
        List<String> carNames = new ArrayList<>();
        for (CarProfile car : carList) {
            carNames.add(car.toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                carNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCar.setAdapter(adapter);

        // Кнопка "Продолжить" — сохраняем выбранную машину
        btnContinue.setOnClickListener(v -> {
            int position = spinnerCar.getSelectedItemPosition();
            CarProfile selectedCar = carList.get(position);

            carProfileManager.saveSelectedCar(selectedCar);
            prefs.edit().putBoolean("car_selected", true).apply();

            goToMainActivity();
        });

        // Пропустить — сохраняем универсальный профиль (последний в списке)
        tvSkip.setOnClickListener(v -> {
            CarProfile genericCar = carList.get(carList.size() - 1);
            carProfileManager.saveSelectedCar(genericCar);
            prefs.edit().putBoolean("car_selected", true).apply();

            goToMainActivity();
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}