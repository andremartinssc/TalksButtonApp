package com.example.talksbutton;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnection {

    private static final String TAG = "BluetoothConnection";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID padrão para comunicação serial
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Handler handler;

    public BluetoothConnection(Handler handler) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
    }

    // Conectar ao dispositivo Bluetooth
    public boolean connect(String deviceAddress) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();

            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            Log.d(TAG, "Conectado ao dispositivo: " + device.getName());

            // Iniciar a thread de escuta
            startListening();

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao conectar", e);
            return false;
        }
    }

    // Enviar dados ao dispositivo
    public void sendData(String data) {
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
                Log.d(TAG, "Dados enviados: " + data);
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao enviar dados", e);
        }
    }

    // Receber dados continuamente
    private void startListening() {
        Thread listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                while (bluetoothSocket != null) {
                    try {
                        if (inputStream != null && (bytes = inputStream.read(buffer)) > 0) {
                            String receivedData = new String(buffer, 0, bytes);
                            Log.d(TAG, "Dados recebidos: " + receivedData);

                            // Enviar os dados recebidos para a MainActivity para exibir
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Exibir os dados como um popup (Toast)
                                    if (receivedData.contains("B1") || receivedData.contains("B2") || receivedData.contains("B3") || receivedData.contains("B4")) {
                                        handler.obtainMessage(1, receivedData).sendToTarget();
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Erro ao receber dados", e);
                    }
                }
            }
        });

        listenerThread.start();
    }

    // Fechar a conexão Bluetooth
    public void closeConnection() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao fechar conexão", e);
        }
    }
}
