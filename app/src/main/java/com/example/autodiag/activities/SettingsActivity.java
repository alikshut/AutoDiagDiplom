/*
 * SettingsActivity.java — активность для настроек приложения.
 * На данный момент содержит только базовую обёртку с EdgeToEdge.
 * В перспективе можно добавить настройки интервала опроса, выбор профиля и т.п.
 */

package com.example.autodiag.activities;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.autodiag.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Включает отображение под системные панели
        setContentView(R.layout.activity_settings);
        // Адаптация отступов под системные панели (статус-бар, навигационная панель)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}