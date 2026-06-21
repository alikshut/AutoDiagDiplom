/*
 * ReportsFragment.java — экран отчётов и истории поездок.
 * Отвечает за:
 *   - загрузку поездок из Room
 *   - отображение списка в RecyclerView
 *   - экспорт отчёта в PDF (через iText7)
 *   - запрос разрешения WRITE_EXTERNAL_STORAGE для Android 11+
 */

package com.example.autodiag.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.autodiag.R;
import com.example.autodiag.database.AppDatabase;
import com.example.autodiag.models.TripEntity;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private RecyclerView rvTrips;
    private Button btnExport;
    private List<TripEntity> trips;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        rvTrips = view.findViewById(R.id.rv_trips);
        btnExport = view.findViewById(R.id.btn_export_pdf);

        rvTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        loadTrips();

        // Кнопка экспорта PDF
        btnExport.setOnClickListener(v -> exportPDF());

        return view;
    }

    // Загрузка поездок из БД в фоновом потоке
    private void loadTrips() {
        new Thread(() -> {
            trips = AppDatabase.getInstance(getContext()).tripDao().getAllTrips();
            getActivity().runOnUiThread(() -> {
                if (trips == null || trips.isEmpty()) {
                    rvTrips.setAdapter(null);
                    Toast.makeText(getContext(), "Нет сохранённых поездок", Toast.LENGTH_SHORT).show();
                } else {
                    rvTrips.setAdapter(new TripAdapter(trips));
                }
            });
        }).start();
    }

    // Экспорт в PDF через iText7
    private void exportPDF() {
        if (trips == null || trips.isEmpty()) {
            Toast.makeText(getContext(), "Нет данных для экспорта", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка прав на запись для Android 11+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
                return;
            }
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String fileName = "OBD2_Report_" + System.currentTimeMillis() + ".pdf";

            // Используем кеш-директорию (не требует дополнительных прав)
            File dir = getContext().getExternalCacheDir();
            if (dir == null) dir = getContext().getFilesDir();
            File file = new File(dir, fileName);

            // Создание PDF-документа
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Заголовок и дата
            document.add(new Paragraph("ОТЧЁТ OBD2")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Дата: " + sdf.format(new java.util.Date()))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // Таблица с данными поездок
            Table table = new Table(4);
            table.addCell(new Cell().add(new Paragraph("№")));
            table.addCell(new Cell().add(new Paragraph("Дата")));
            table.addCell(new Cell().add(new Paragraph("Дистанция, км")));
            table.addCell(new Cell().add(new Paragraph("Макс. скорость, км/ч")));

            for (TripEntity trip : trips) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(trip.id))));
                table.addCell(new Cell().add(new Paragraph(sdf.format(new java.util.Date(trip.startTime)))));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", trip.distance))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(trip.maxSpeed))));
            }

            document.add(table);
            document.close();

            Toast.makeText(getContext(), "PDF сохранён: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Предложение открыть PDF
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(Intent.createChooser(intent, "Открыть PDF"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // АДАПТЕР ДЛЯ СПИСКА ПОЕЗДОК (RecyclerView)
    class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {
        private List<TripEntity> trips;

        TripAdapter(List<TripEntity> trips) { this.trips = trips; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 16, 32, 16);
            tv.setTextColor(getResources().getColor(R.color.text_primary));
            tv.setTextSize(14);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TripEntity trip = trips.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
            String text = String.format("%s - %.1f км (макс: %d км/ч)",
                    sdf.format(new java.util.Date(trip.startTime)), trip.distance, trip.maxSpeed);
            holder.textView.setText(text);
        }

        @Override
        public int getItemCount() { return trips.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(TextView tv) { super(tv); textView = tv; }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportPDF();
            } else {
                Toast.makeText(getContext(), "Нужны права на запись", Toast.LENGTH_SHORT).show();
            }
        }
    }
}