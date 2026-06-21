/*
 * DiagnosticsActivity.java — активность для чтения и очистки кодов ошибок DTC.
 * В текущей версии — заглушка. Реальное чтение ошибок вынесено в DiagnosticsFragment.
 * Этот файл НЕ используется в приложении, оставлен как задел.
 */

package com.example.autodiag.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autodiag.R;

public class DiagnosticsActivity extends AppCompatActivity {

    private TextView tvErrorCodes;
    private Button btnReadErrors, btnClearErrors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics);

        // Привязка элементов интерфейса
        tvErrorCodes = findViewById(R.id.tv_error_codes);
        btnReadErrors = findViewById(R.id.btn_read_errors);
        btnClearErrors = findViewById(R.id.btn_clear_errors);

        // Кнопка чтения ошибок (заглушка — показывает примеры кодов)
        btnReadErrors.setOnClickListener(v -> {
            tvErrorCodes.setText("P0300 - Random Misfire\nP0420 - Catalyst Efficiency");
            Toast.makeText(this, "Ошибки считаны", Toast.LENGTH_SHORT).show();
        });

        // Кнопка очистки ошибок (заглушка)
        btnClearErrors.setOnClickListener(v -> {
            tvErrorCodes.setText("Ошибок нет");
            Toast.makeText(this, "Ошибки очищены", Toast.LENGTH_SHORT).show();
        });
    }
}