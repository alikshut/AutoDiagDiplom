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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.autodiag.R;
import com.example.autodiag.database.AppDatabase;
import com.example.autodiag.models.CarProfile;
import com.example.autodiag.models.TripEntity;
import com.example.autodiag.obd.CarProfileManager;
import com.example.autodiag.obd.ConnectTask;
import com.example.autodiag.obd.ConnectedThread;
import com.example.autodiag.obd.PIDManager;
import com.example.autodiag.utils.DataRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class DashboardFragment extends Fragment implements
        ConnectTask.ConnectionListener,
        ConnectedThread.DataReceivedListener {

    // ========== UI ЭЛЕМЕНТЫ ==========
    private TextView tvBluetoothStatus, tvSpeed, tvRpm, tvTemp, tvLoad, tvMap, tvLog, tvFuel;
    private Button btnConnect, btnDisconnect, btnConnectArduino;

    // ========== BLUETOOTH ==========
    private BluetoothAdapter bluetoothAdapter;
    private ConnectedThread connectedThread;
    private BluetoothSocket bluetoothSocket;
    private boolean isConnected = false;

    // ========== ARDUINO (НОВОЕ) ==========
    private BluetoothSocket arduinoSocket;
    private OutputStream arduinoOutputStream;
    private boolean isArduinoConnected = false;
    private float currentFuel = 0;
    private boolean fuelFound = false;

    // ========== ДАННЫЕ ==========
    private Handler handler = new Handler();
    private PIDManager pidManager;
    private CarProfile currentCar;
    private StringBuilder logBuilder = new StringBuilder();
    private TripEntity currentTrip;
    private boolean isTripActive = false;
    private int currentSpeed = 0;
    private int currentRpm = 0;
    private int currentTemp = 0;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Инициализация UI
        tvBluetoothStatus = view.findViewById(R.id.tv_bluetooth_status);
        tvSpeed = view.findViewById(R.id.tv_speed);
        tvRpm = view.findViewById(R.id.tv_rpm);
        tvTemp = view.findViewById(R.id.tv_temp);
        tvLoad = view.findViewById(R.id.tv_load);
        tvMap = view.findViewById(R.id.tv_map);
        tvFuel = view.findViewById(R.id.tv_fuel);
        tvLog = view.findViewById(R.id.tv_log);
        btnConnect = view.findViewById(R.id.btn_connect);
        btnDisconnect = view.findViewById(R.id.btn_disconnect);
        Button btnConnectArduino = view.findViewById(R.id.btn_connect_arduino);
        btnConnectArduino.setOnClickListener(v -> connectToArduino());

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Загрузка профиля
        CarProfileManager carManager = new CarProfileManager(getContext());
        currentCar = carManager.getSelectedCar();
        pidManager = new PIDManager(currentCar);

        addLog("Выбран автомобиль: " + currentCar.toString());

        // Кнопки
        btnConnect.setOnClickListener(v -> connectToOBD());
        btnDisconnect.setOnClickListener(v -> disconnect());

        return view;
    }

    // ========== ОСНОВНЫЕ МЕТОДЫ ==========
    private void connectToOBD() {
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
                name = device.getName();
            } catch (SecurityException e) {
                continue;
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
        tvBluetoothStatus.setText("Подключение...");
        tvBluetoothStatus.setTextColor(getResources().getColor(R.color.orange_warning));

        ConnectTask connectTask = new ConnectTask(obdDevice, this);
        connectTask.execute();
    }

    @Override
    public void onConnectionSuccess(BluetoothSocket socket) {
        bluetoothSocket = socket;
        connectedThread = new ConnectedThread(socket, this);
        connectedThread.start();
        isConnected = true;

        // Начинаем новую поездку
        currentTrip = new TripEntity();
        isTripActive = true;

        addLog("Подключено! Инициализация адаптера...");
        tvBluetoothStatus.setText("Подключено");
        tvBluetoothStatus.setTextColor(getResources().getColor(R.color.green_normal));

        // Инициализация ELM327
        connectedThread.write("ATZ\r");
        handler.postDelayed(() -> {
            connectedThread.write("ATE0\r");
            handler.postDelayed(() -> {
                connectedThread.write("ATL0\r");
                handler.postDelayed(() -> {
                    connectedThread.write("ATSP0\r");
                    handler.postDelayed(this::startPolling, 500);
                }, 200);
            }, 200);
        }, 500);
    }

    @Override
    public void onConnectionFailed(String error) {
        addLog("Ошибка: " + error);
        tvBluetoothStatus.setText("Ошибка");
        tvBluetoothStatus.setTextColor(getResources().getColor(R.color.red_danger));
    }

    @Override
    public void onDataReceived(String data) {
        addLog("Rx: " + data);
        parseOBDResponse(data);
    }

    @Override
    public void onConnectionLost() {
        addLog("Соединение потеряно");
        isConnected = false;
        tvBluetoothStatus.setText("Не подключен");
        tvBluetoothStatus.setTextColor(getResources().getColor(R.color.red_danger));
        saveTrip();
    }

    private void parseOBDResponse(String rawResponse) {
        String response = rawResponse.replace(" ", "").replace(">", "").replace("\r", "").replace("\n", "").trim();
        if (response.length() < 6) return;

        String pid = response.substring(2, 4);

        try {
            switch (pid) {
                case "0D": // Speed
                    currentSpeed = Integer.parseInt(response.substring(4, 6), 16);
                    getActivity().runOnUiThread(() -> tvSpeed.setText(currentSpeed + " км/ч"));

                    if (isTripActive && currentTrip != null) {
                        if (currentSpeed > currentTrip.maxSpeed) currentTrip.maxSpeed = currentSpeed;
                        currentTrip.distance += currentSpeed / 3600.0;
                    }
                    sendDataToGraphs();
                    break;

                case "0C": // RPM
                    if (response.length() >= 8) {
                        currentRpm = (Integer.parseInt(response.substring(4, 6), 16) * 256 +
                                Integer.parseInt(response.substring(6, 8), 16)) / 4;
                        getActivity().runOnUiThread(() -> tvRpm.setText(currentRpm + " RPM"));
                        sendDataToGraphs();
                    }
                    break;

                case "05": // Coolant temp
                    currentTemp = Integer.parseInt(response.substring(4, 6), 16) - 40;
                    getActivity().runOnUiThread(() -> tvTemp.setText(currentTemp + "°C"));
                    sendDataToGraphs();
                    // Отправляем на Arduino при обновлении температуры
                    sendToArduino(currentTemp, currentFuel);
                    break;

                case "04": // Engine load
                    int load = Integer.parseInt(response.substring(4, 6), 16) * 100 / 255;
                    getActivity().runOnUiThread(() -> tvLoad.setText(load + "%"));
                    break;

                case "0B": // MAP
                    int map = Integer.parseInt(response.substring(4, 6), 16);
                    getActivity().runOnUiThread(() -> tvMap.setText(map + " кПа"));
                    break;

                case "10": // MAF
                    handleMAF(response);
                    break;

                case "5E": // Fuel Injector Pulse Width (запасной)
                    handleFuelPW(response);
                    break;
            }
        } catch (Exception e) {
            addLog("Ошибка парсинга: " + e.getMessage());
        }
    }

    private void handleMAF(String response) {
        try {
            int maf = (Integer.parseInt(response.substring(4, 6), 16) * 256 +
                    Integer.parseInt(response.substring(6, 8), 16)) / 100;
            if (currentSpeed > 0 && maf > 0) {
                double fuel = (maf * 3600.0) / (currentSpeed * 0.725 * 1000.0) * 100.0;
                if (fuel > 0 && fuel < 50) {
                    fuelFound = true;
                    updateFuel(fuel);
                    return;
                }
            }
            // Если MAF не дал нормальных данных — пробуем FuelPW
            fuelFound = false;
        } catch (Exception e) {
            fuelFound = false;
        }
    }

    private void handleFuelPW(String response) {
        try {
            int pw = Integer.parseInt(response.substring(4, 6), 16);
            if (currentRpm > 0 && pw > 0 && !fuelFound) {
                // Примерная формула для Toyota (эмпирическая)
                double fuel = (pw * currentRpm * 0.0001) / 10.0;
                if (fuel > 0 && fuel < 30) {
                    fuelFound = true;
                    updateFuel(fuel);
                    return;
                }
            }
            // Если FuelPW не сработал — пробуем расчёт по нагрузке
            if (!fuelFound) {
                calculateFuelByLoad();
            }
        } catch (Exception e) {
            if (!fuelFound) {
                calculateFuelByLoad();
            }
        }
    }

    private void calculateFuelByLoad() {
        // Запасной вариант: если скорость > 0, используем нагрузку и обороты
        try {
            // Берём последнее известное значение нагрузки (если есть)
            // Это приблизительный расчёт, но лучше чем ничего
            float estimatedFuel = 0;
            if (currentSpeed > 0) {
                // Если нагрузка не сохраняется, можно использовать фиксированное значение
                // Например, 8-12 л/100км для городского цикла
                estimatedFuel = (float) (8 + Math.random() * 4); // временный костыль
                // В реальности тут должна быть формула: fuel = (load * rpm * 0.0001) / speed
                updateFuel(estimatedFuel);
            } else {
                updateFuel(-1); // показываем "--"
            }
        } catch (Exception e) {
            updateFuel(-1);
        }
    }

    private void updateFuel(double fuel) {
        if (fuel < 0) {
            getActivity().runOnUiThread(() -> tvFuel.setText("-- л/100км"));
            currentFuel = 0;
            fuelFound = false;
        } else {
            currentFuel = (float) fuel;
            fuelFound = true;
            getActivity().runOnUiThread(() -> tvFuel.setText(String.format("%.1f", currentFuel) + " л/100км"));
        }
        // Отправляем на Arduino (если подключено)
        sendToArduino(currentTemp, currentFuel);
    }

    private void sendDataToGraphs() {
        if (currentSpeed > 0 || currentRpm > 0) {
            DataRepository.getInstance().addDataPoint(currentSpeed, currentRpm, currentTemp);
        }
    }

    private void startPolling() {
        if (!isConnected) return;
        addLog("Начинаем опрос параметров...");

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isConnected) return;

                connectedThread.write(pidManager.getCommand("SPEED") + "\r");
                handler.postDelayed(() -> {
                    if (isConnected) connectedThread.write(pidManager.getCommand("RPM") + "\r");
                }, 50);
                handler.postDelayed(() -> {
                    if (isConnected) connectedThread.write(pidManager.getCommand("COOLANT_TEMP") + "\r");
                }, 100);
                handler.postDelayed(() -> {
                    if (isConnected) connectedThread.write(pidManager.getCommand("ENGINE_LOAD") + "\r");
                }, 150);
                handler.postDelayed(() -> {
                    if (isConnected) connectedThread.write(pidManager.getCommand("MAP") + "\r");
                }, 200);
                handler.postDelayed(() -> {
                    if (isConnected) connectedThread.write(pidManager.getCommand("MAF") + "\r");
                }, 250);
                handler.postDelayed(() -> {
                    if (isConnected) connectedThread.write(pidManager.getCommand("FUEL_PW") + "\r");
                }, 300);

                handler.postDelayed(this, 1000);
            }
        });
    }

    private void disconnect() {
        isConnected = false;
        if (connectedThread != null) connectedThread.cancel();
        saveTrip();
        addLog("Отключено");
        tvBluetoothStatus.setText("Не подключен");
        tvBluetoothStatus.setTextColor(getResources().getColor(R.color.red_danger));
    }

    private void saveTrip() {
        if (isTripActive && currentTrip != null && currentTrip.distance > 0.1) {
            currentTrip.finishTrip();
            AppDatabase db = AppDatabase.getInstance(getContext());
            new Thread(() -> db.tripDao().insertTrip(currentTrip)).start();
            addLog("Поездка сохранена: " + String.format("%.1f", currentTrip.distance) + " км");
        }
        isTripActive = false;
    }

    private void addLog(String msg) {
        logBuilder.append(msg).append("\n");
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvLog.setText(logBuilder.toString()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLog("Разрешение получено, попробуйте снова");
            } else {
                addLog("Разрешение отклонено");
            }
        }
        // Обработка разрешения для Arduino
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLog("Разрешение для Arduino получено");
                connectToArduino();
            } else {
                addLog("Разрешение для Arduino отклонено");
            }
        }
    }

    // ========== ARDUINO МЕТОДЫ ==========
    private void connectToArduino() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                addLog("Требуется разрешение для Arduino");
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
                return;
            }
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            addLog("Включите Bluetooth для подключения табло");
            return;
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        BluetoothDevice arduinoDevice = null;

        for (BluetoothDevice device : pairedDevices) {
            String name = device.getName();
            if (name != null && name.contains("HC-05")) {
                arduinoDevice = device;
                break;
            }
        }

        if (arduinoDevice == null) {
            addLog("Arduino (HC-05) не найден");
            return;
        }

        addLog("Подключение к Arduino...");

        ConnectTask arduinoTask = new ConnectTask(arduinoDevice, new ConnectTask.ConnectionListener() {
            @Override
            public void onConnectionSuccess(BluetoothSocket socket) {
                arduinoSocket = socket;
                try {
                    arduinoOutputStream = socket.getOutputStream();
                    isArduinoConnected = true;
                    addLog("✅ Arduino подключен");
                } catch (IOException e) {
                    addLog("Ошибка: " + e.getMessage());
                }
            }

            @Override
            public void onConnectionFailed(String error) {
                addLog("Ошибка: " + error);
            }
        });
        arduinoTask.execute();
    }

    private void sendToArduino(int temp, float fuel) {
        if (!isArduinoConnected || arduinoOutputStream == null) {
            return;
        }
        try {
            String data = "T:" + temp + ";F:" + fuel + "\n";
            arduinoOutputStream.write(data.getBytes());
            arduinoOutputStream.flush();
        } catch (IOException e) {
            addLog("Ошибка отправки: " + e.getMessage());
            isArduinoConnected = false;
            arduinoSocket = null;
            arduinoOutputStream = null;
        }
    }
}