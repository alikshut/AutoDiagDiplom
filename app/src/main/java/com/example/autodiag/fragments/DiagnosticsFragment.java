/*
 * DiagnosticsFragment.java — экран диагностики DTC.
 * Отвечает за:
 *   - чтение кодов ошибок (Mode 03)
 *   - очистку ошибок (Mode 04)
 *   - парсинг HEX-ответов в коды типа P0300
 */

package com.example.autodiag.fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.autodiag.R;
import com.example.autodiag.obd.ConnectTask;
import com.example.autodiag.obd.ConnectedThread;

import java.util.Set;

public class DiagnosticsFragment extends Fragment implements
        ConnectTask.ConnectionListener,
        ConnectedThread.DataReceivedListener {

    private TextView tvStatus, tvErrorCodes;
    private Button btnConnect, btnReadErrors, btnClearErrors;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectedThread connectedThread;
    private BluetoothSocket bluetoothSocket;
    private Handler handler = new Handler();
    private boolean isConnected = false;
    private StringBuilder logBuilder = new StringBuilder();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diagnostics, container, false);

        tvStatus = view.findViewById(R.id.tv_diag_status);
        tvErrorCodes = view.findViewById(R.id.tv_error_codes);
        btnConnect = view.findViewById(R.id.btn_diag_connect);
        btnReadErrors = view.findViewById(R.id.btn_read_errors);
        btnClearErrors = view.findViewById(R.id.btn_clear_errors);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnConnect.setOnClickListener(v -> connectToOBD());
        btnReadErrors.setOnClickListener(v -> {
            if (isConnected) {
                addLog("Чтение ошибок...");
                connectedThread.write("03\r");
            } else {
                Toast.makeText(getContext(), "Сначала подключитесь", Toast.LENGTH_SHORT).show();
            }
        });
        btnClearErrors.setOnClickListener(v -> {
            if (isConnected) {
                addLog("Очистка ошибок...");
                connectedThread.write("04\r");
                tvErrorCodes.setText("Ошибки очищены");
            } else {
                Toast.makeText(getContext(), "Сначала подключитесь", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // ПОДКЛЮЧЕНИЕ К АДАПТЕРУ (С ПРОВЕРКОЙ РАЗРЕШЕНИЙ)
    private void connectToOBD() {
        // Проверка разрешений для Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                addLog("Требуется разрешение BLUETOOTH_CONNECT");
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        }

        if (bluetoothAdapter == null) {
            addLog("Bluetooth не поддерживается");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            addLog("Включите Bluetooth");
            return;
        }

        // Получение списка сопряжённых устройств (с try-catch)
        Set<BluetoothDevice> pairedDevices;
        try {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException e) {
            addLog("Ошибка: нет разрешения на Bluetooth");
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
            return;
        }

        BluetoothDevice obdDevice = null;
        for (BluetoothDevice device : pairedDevices) {
            String name;
            try {
                name = device.getName(); // getName() тоже требует разрешения
            } catch (SecurityException e) {
                continue; // пропускаем устройство, если не можем получить имя
            }
            if (name != null && (name.contains("OBD") || name.contains("ELM") || name.contains("Vgate"))) {
                obdDevice = device;
                break;
            }
        }

        if (obdDevice == null) {
            addLog("OBD2 адаптер не найден");
            return;
        }

        addLog("Подключение к " + obdDevice.getName());
        tvStatus.setText("Подключение...");

        ConnectTask connectTask = new ConnectTask(obdDevice, this);
        connectTask.execute();
    }

    // ОБРАБОТКА РАЗРЕШЕНИЙ
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLog("Разрешение получено, попробуйте снова");
            } else {
                addLog("Разрешение отклонено");
            }
        }
    }

    // КОЛБЭКИ ПОДКЛЮЧЕНИЯ
    @Override
    public void onConnectionSuccess(BluetoothSocket socket) {
        bluetoothSocket = socket;
        connectedThread = new ConnectedThread(socket, this);
        connectedThread.start();
        isConnected = true;

        addLog("Подключено! Инициализация...");
        tvStatus.setText("Подключено");

        connectedThread.write("ATZ\r");
        handler.postDelayed(() -> {
            connectedThread.write("ATE0\r");
            handler.postDelayed(() -> {
                connectedThread.write("ATL0\r");
                handler.postDelayed(() -> {
                    connectedThread.write("ATSP0\r");
                    handler.postDelayed(() -> addLog("Готов к чтению ошибок"), 500);
                }, 200);
            }, 200);
        }, 500);
    }

    @Override
    public void onConnectionFailed(String error) {
        addLog("Ошибка: " + error);
        tvStatus.setText("Ошибка");
    }

    @Override
    public void onDataReceived(String data) {
        addLog("Rx: " + data);
        parseDTCResponse(data);
    }

    @Override
    public void onConnectionLost() {
        addLog("Соединение потеряно");
        isConnected = false;
        tvStatus.setText("Не подключен");
    }

    // ПАРСИНГ DTC ОТВЕТОВ
    private void parseDTCResponse(String rawResponse) {
        String response = rawResponse.replace(" ", "").replace(">", "").replace("\r", "").replace("\n", "").trim();

        if (response.startsWith("43") && response.length() > 4) {
            StringBuilder codes = new StringBuilder();
            for (int i = 2; i < response.length(); i += 2) {
                if (i + 3 <= response.length()) {
                    String codeHex = response.substring(i, i + 4);
                    String dtcCode = hexToDTC(codeHex);
                    if (!dtcCode.equals("NONE") && !dtcCode.equals("0000")) {
                        codes.append(dtcCode).append("\n");
                    }
                }
            }

            if (codes.length() > 0) {
                String finalCodes = codes.toString();
                getActivity().runOnUiThread(() -> tvErrorCodes.setText(finalCodes));
                addLog("Найдены ошибки:\n" + finalCodes);
            } else {
                getActivity().runOnUiThread(() -> tvErrorCodes.setText("Ошибок нет"));
                addLog("Ошибок не обнаружено");
            }
        } else if (response.startsWith("44") || response.contains("OK")) {
            getActivity().runOnUiThread(() -> tvErrorCodes.setText("Ошибки очищены"));
            addLog("Ошибки успешно очищены");
        }
    }

    private String hexToDTC(String hex) {
        if (hex.equals("0000") || hex.equals("FFFF")) return "NONE";
        try {
            int val = Integer.parseInt(hex, 16);
            int firstByte = (val >> 8) & 0xFF;
            int secondByte = val & 0xFF;
            char system = 'P';
            if ((firstByte & 0xC0) == 0x40) system = 'C';
            else if ((firstByte & 0xC0) == 0x80) system = 'B';
            else if ((firstByte & 0xC0) == 0xC0) system = 'U';
            int code = ((firstByte & 0x3F) << 8) | secondByte;
            int thousands = (code / 256) % 10;
            int hundreds = (code / 16) % 16;
            int tens = code % 16;
            return String.format("%c%01d%01X%01X", system, thousands, hundreds, tens);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private void addLog(String msg) {
        logBuilder.append(msg).append("\n");
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvStatus.setText(msg));
        }
    }
}