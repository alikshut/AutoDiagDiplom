package com.example.autodiag.obd;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private StringBuilder receivedData = new StringBuilder();

    public interface DataReceivedListener {
        void onDataReceived(String data);
        void onConnectionLost();
    }

    private DataReceivedListener listener;

    public ConnectedThread(BluetoothSocket socket, DataReceivedListener listener) {
        mmSocket = socket;
        this.listener = listener;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e("ConnectedThread", "Error getting streams", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void write(String command) {
        try {
            mmOutStream.write(command.getBytes());
        } catch (IOException e) {
            Log.e("ConnectedThread", "Error writing", e);
            if (listener != null) listener.onConnectionLost();
        }
    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                bytes = mmInStream.read(buffer);
                String data = new String(buffer, 0, bytes);
                if (listener != null) listener.onDataReceived(data);
            } catch (IOException e) {
                Log.e("ConnectedThread", "Disconnected", e);
                if (listener != null) listener.onConnectionLost();
                break;
            }
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e("ConnectedThread", "Error closing socket", e);
        }
    }
}