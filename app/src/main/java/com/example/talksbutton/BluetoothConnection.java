package com.example.talksbutton;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnection {
    private static final String TAG = "BluetoothConnection";
    public static final String ACTION_BLUETOOTH_DATA_RECEIVED = "com.example.talksbutton.BLUETOOTH_DATA";
    public static final String EXTRA_DATA = "bluetooth_data";

    private static final String DEVICE_NAME = "TalksButton_ESP32"; // Nome do dispositivo Bluetooth
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final Context context;

    public BluetoothConnection(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connect() {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Dispositivo não suporta Bluetooth.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth está desligado. Ative-o primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice device = findDeviceByName(DEVICE_NAME);
        if (device == null) {
            Toast.makeText(context, "Dispositivo " + DEVICE_NAME + " não pareado.", Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();

                Log.i(TAG, "Conexão Bluetooth estabelecida com " + DEVICE_NAME);
                showToast("Conectado ao " + DEVICE_NAME);

                listenForData();
            } catch (IOException e) {
                Log.e(TAG, "Erro ao conectar", e);
                showToast("Falha na conexão com " + DEVICE_NAME);
                closeConnection();
            }
        }).start();
    }

    private BluetoothDevice findDeviceByName(String deviceName) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName() != null && device.getName().equals(deviceName)) {
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
                    String receivedMessage = new String(buffer, 0, bytes);

                    Log.d(TAG, "Dados recebidos: " + receivedMessage);

                    Intent intent = new Intent(ACTION_BLUETOOTH_DATA_RECEIVED);
                    intent.putExtra(EXTRA_DATA, receivedMessage);
                    context.sendBroadcast(intent); // <- Correto!

                } catch (IOException e) {
                    Log.e(TAG, "Erro ao ler dados Bluetooth", e);
                    break;
                }
            }
        }).start();
    }

    public void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Erro ao fechar conexão Bluetooth", e);
        }
    }

    private void showToast(String message) {
        new android.os.Handler(context.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }
}
