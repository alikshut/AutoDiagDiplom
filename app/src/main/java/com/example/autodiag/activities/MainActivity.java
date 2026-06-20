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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("AutoDiag");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //

        btnDashboard = findViewById(R.id.btn_dashboard);
        btnGraphs = findViewById(R.id.btn_graphs);
        btnReports = findViewById(R.id.btn_reports);
        btnDiagnostics = findViewById(R.id.btn_diagnostics);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new DashboardFragment())
                .commit();

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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Сбрасываем флаг, чтобы SplashActivity показала экран выбора
            SharedPreferences prefs = getSharedPreferences("autodiag_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("car_selected", false).apply();

            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}