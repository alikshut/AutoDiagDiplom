/*
 * MainActivity.java — основное Activity приложения.
 * Управляет переключением между четырьмя фрагментами (Панель, Графики, Отчёты, Диагностика).
 * Также обрабатывает стрелку «Назад» для возврата на экран выбора авто.
 */

package com.example.autodiag.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.example.autodiag.R;
import com.example.autodiag.fragments.DashboardFragment;
import com.example.autodiag.fragments.DiagnosticsFragment;
import com.example.autodiag.fragments.GraphsFragment;
import com.example.autodiag.fragments.ReportsFragment;

public class MainActivity extends AppCompatActivity {

    private Button btnDashboard, btnGraphs, btnReports, btnDiagnostics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("AutoDiag");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Показывает стрелку «Назад»

        // Привязка кнопок нижней навигации
        btnDashboard = findViewById(R.id.btn_dashboard);
        btnGraphs = findViewById(R.id.btn_graphs);
        btnReports = findViewById(R.id.btn_reports);
        btnDiagnostics = findViewById(R.id.btn_diagnostics);

        // При запуске — загружаем DashboardFragment (главный экран)
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new DashboardFragment())
                .commit();

        // Обработчики нажатий на кнопки — подменяют фрагмент в контейнере
        btnDashboard.setOnClickListener(v ->
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment())
                        .commit()
        );

        btnGraphs.setOnClickListener(v ->
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new GraphsFragment())
                        .commit()
        );

        btnReports.setOnClickListener(v ->
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ReportsFragment())
                        .commit()
        );

        btnDiagnostics.setOnClickListener(v ->
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DiagnosticsFragment())
                        .commit()
        );
    }

    // Обработка нажатия на стрелку «Назад» в тулбаре
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Сбрасываем флаг car_selected, чтобы при следующем запуске показать экран выбора авто
            SharedPreferences prefs = getSharedPreferences("autodiag_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("car_selected", false).apply();

            // Переход на SplashActivity (экран выбора профиля)
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Закрываем MainActivity, чтобы не возвращаться
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}