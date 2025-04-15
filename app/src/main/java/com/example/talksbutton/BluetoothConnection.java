package com.example.talksbutton;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnection {

    private static final String TAG = "BluetoothConnection";
    private static final String DEVICE_NAME = "TalksButton_ESP32";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static BluetoothConnection instance;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private OnBluetoothDataReceivedListener dataListener;

    private BluetoothConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static synchronized BluetoothConnection getInstance() {
        if (instance == null) {
            instance = new BluetoothConnection();
        }
        return instance;
    }

    public void setDataListener(OnBluetoothDataReceivedListener listener) {
        this.dataListener = listener;
    }

    public void connect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;

        BluetoothDevice device = findDeviceByName(DEVICE_NAME);
        if (device == null) return;

        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();

                listenForData();
            } catch (IOException e) {
                Log.e(TAG, "Erro ao conectar", e);
                closeConnection();
            }
        }).start();
    }

    private BluetoothDevice findDeviceByName(String name) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (name.equals(device.getName())) {
                return device;
            }
        }
        return null;
    }

    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    if (inputStream == null) break;
                    bytes = inputStream.read(buffer);
                    String receivedData = new String(buffer, 0, bytes);

                    if (dataListener != null) {
                        dataListener.onDataReceived(receivedData);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Erro na leitura", e);
                    break;
                }
            }
        }).start();
    }

    public void sendData(String data) {
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao enviar dados", e);
        }
    }

    public void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Erro ao fechar conexão", e);
        }
    }

    public interface OnBluetoothDataReceivedListener {
        void onDataReceived(String data);
    }
}
