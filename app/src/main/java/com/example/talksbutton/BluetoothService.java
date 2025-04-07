package com.example.talksbutton;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BluetoothService extends Service {

    private static final String TAG = "BluetoothService";
    private static final String DEVICE_NAME = "TalksButton_ESP32";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;
    private OnBluetoothDataReceivedListener dataListener;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public interface OnBluetoothDataReceivedListener {
        void onDataReceived(String data);
    }

    public void setDataListener(OnBluetoothDataReceivedListener listener) {
        this.dataListener = listener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Serviço Bluetooth criado.");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connect(); // Inicia a conexão ao criar o serviço
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Serviço Bluetooth destruído.");
        closeConnection();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || isConnected) return;

        new Thread(() -> {
            BluetoothDevice device = findDeviceByName(DEVICE_NAME);
            if (device == null) {
                Log.w(TAG, "Dispositivo Bluetooth " + DEVICE_NAME + " não encontrado.");
                broadcastConnectionState(false);
                return;
            }

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;
                Log.i(TAG, "Conexão Bluetooth estabelecida com " + DEVICE_NAME);
                broadcastConnectionState(true);
                listenForData();
            } catch (IOException e) {
                Log.e(TAG, "Erro ao conectar com " + DEVICE_NAME, e);
                closeConnection();
                broadcastConnectionState(false);
            }
        }).start();
    }

    private BluetoothDevice findDeviceByName(String name) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null) return null;
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
            while (isConnected && inputStream != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String receivedData = new String(buffer, 0, bytes);
                        Log.d(TAG, "Dados recebidos: " + receivedData.trim());
                        broadcastDataReceived(receivedData);
                        if (dataListener != null) {
                            dataListener.onDataReceived(receivedData);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Erro na leitura de dados", e);
                    closeConnection();
                    broadcastConnectionState(false);
                    break;
                }
            }
            Log.i(TAG, "Parando de escutar dados.");
        }).start();
    }

    public void sendData(String data) {
        try {
            if (isConnected && outputStream != null) {
                outputStream.write(data.getBytes());
            } else {
                Log.w(TAG, "Não é possível enviar dados: Bluetooth não conectado.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao enviar dados", e);
        }
    }

    public void closeConnection() {
        try {
            isConnected = false;
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
            Log.i(TAG, "Conexão Bluetooth fechada.");
            broadcastConnectionState(false);
        } catch (IOException e) {
            Log.e(TAG, "Erro ao fechar conexão", e);
        } finally {
            inputStream = null;
            outputStream = null;
            bluetoothSocket = null;
        }
    }

    private void broadcastDataReceived(String data) {
        Intent intent = new Intent("bluetooth_data_received");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastConnectionState(boolean isConnected) {
        Intent intent = new Intent("bluetooth_connection_state");
        intent.putExtra("is_connected", isConnected);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}