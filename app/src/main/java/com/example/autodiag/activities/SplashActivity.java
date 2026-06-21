/*
 * SplashActivity.java — экран выбора профиля автомобиля при первом запуске.
 * Загружает список предустановленных профилей из CarProfileManager.
 * Сохраняет выбранный профиль в SharedPreferences и переходит в MainActivity.
 */

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

        // Инициализация менеджера профилей и загрузка списка
        carProfileManager = new CarProfileManager(this);
        carList = carProfileManager.getPresetProfiles();

        // Привязка элементов интерфейса
        spinnerCar = findViewById(R.id.spinner_car);
        btnContinue = findViewById(R.id.btn_continue);
        tvSkip = findViewById(R.id.tv_skip);

        // Проверка: выбирал ли пользователь машину раньше
        SharedPreferences prefs = getSharedPreferences("autodiag_prefs", MODE_PRIVATE);
        boolean carSelected = prefs.getBoolean("car_selected", false);
        if (carSelected) {
            goToMainActivity(); // Если выбрал — сразу переходим в главное Activity
            finish();
            return;
        }

        // Заполняем спиннер названиями машин из списка профилей
        List<String> carNames = new ArrayList<>();
        for (CarProfile car : carList) {
            carNames.add(car.toString());
        }

        // Адаптер для спиннера
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                carNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCar.setAdapter(adapter);

        // Кнопка "Продолжить" — сохраняем выбранную машину в SharedPreferences
        btnContinue.setOnClickListener(v -> {
            int position = spinnerCar.getSelectedItemPosition();
            CarProfile selectedCar = carList.get(position);

            carProfileManager.saveSelectedCar(selectedCar);
            prefs.edit().putBoolean("car_selected", true).apply(); // Устанавливаем флаг

            goToMainActivity();
        });

        // "Пропустить" — сохраняем универсальный профиль (последний в списке)
        tvSkip.setOnClickListener(v -> {
            CarProfile genericCar = carList.get(carList.size() - 1);
            carProfileManager.saveSelectedCar(genericCar);
            prefs.edit().putBoolean("car_selected", true).apply();

            goToMainActivity();
        });
    }

    // Переход в MainActivity
    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}