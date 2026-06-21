/*
 * ConnectTask.java — асинхронная задача для подключения к Bluetooth-устройству.
 * Использует AsyncTask для выполнения в фоновом потоке.
 * При успехе — возвращает BluetoothSocket, при ошибке — null.
 * Поддерживает два метода подключения: стандартный и через рефлексию (для старых адаптеров).
 */

package com.example.autodiag.obd;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import java.util.UUID;

public class ConnectTask extends AsyncTask<Void, Void, BluetoothSocket> {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice device;
    private ConnectionListener listener;
    private BluetoothSocket socket;

    public interface ConnectionListener {
        void onConnectionSuccess(BluetoothSocket socket);
        void onConnectionFailed(String error);
    }

    public ConnectTask(BluetoothDevice device, ConnectionListener listener) {
        this.device = device;
        this.listener = listener;
    }

    @Override
    protected BluetoothSocket doInBackground(Void... voids) {
        try {
            // Стандартный способ (основной)
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                return socket;
            } catch (SecurityException e) {
                // Альтернативный способ (через рефлексию) — для старых адаптеров
                socket = (BluetoothSocket) device.getClass()
                        .getMethod("createRfcommSocket", int.class)
                        .invoke(device, 1);
                socket.connect();
                return socket;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(BluetoothSocket result) {
        if (result != null && listener != null) {
            listener.onConnectionSuccess(result);
        } else if (listener != null) {
            listener.onConnectionFailed("Connection failed");
        }
    }
}