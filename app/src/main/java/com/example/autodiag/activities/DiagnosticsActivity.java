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

        tvErrorCodes = findViewById(R.id.tv_error_codes);
        btnReadErrors = findViewById(R.id.btn_read_errors);
        btnClearErrors = findViewById(R.id.btn_clear_errors);

        btnReadErrors.setOnClickListener(v -> {
            // TODO: подключить к Bluetooth и читать ошибки через команду 03
            tvErrorCodes.setText("P0300 - Random Misfire\nP0420 - Catalyst Efficiency");
            Toast.makeText(this, "Ошибки считаны", Toast.LENGTH_SHORT).show();
        });

        btnClearErrors.setOnClickListener(v -> {
            tvErrorCodes.setText("Ошибок нет");
            Toast.makeText(this, "Ошибки очищены", Toast.LENGTH_SHORT).show();
        });
    }
}