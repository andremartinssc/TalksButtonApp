package com.example.talksbutton;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler; // Adicione esta importação
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

    // Constantes para reconexão
    private static final int RECONNECT_DELAY_MS = 5000; // 5 segundos
    private static final int MAX_RECONNECT_ATTEMPTS = 10; // Aumentei as tentativas para o serviço
    private int reconnectAttemptCount = 0;
    private Handler reconnectHandler = new Handler();
    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isConnected && reconnectAttemptCount < MAX_RECONNECT_ATTEMPTS) {
                Log.i(TAG, "Tentando reconectar ao dispositivo Talks Button (Tentativa " + (reconnectAttemptCount + 1) + ")");
                reconnectAttemptCount++;
                connect(); // Tenta conectar novamente
            } else if (reconnectAttemptCount >= MAX_RECONNECT_ATTEMPTS) {
                Log.w(TAG, "Número máximo de tentativas de reconexão atingido. Parando reconexões automáticas.");
                // Opcional: Você pode querer enviar um broadcast aqui para a UI avisar o usuário
                // Toast.makeText(getApplicationContext(), "Falha ao conectar ao dispositivo Talks Button.", Toast.LENGTH_LONG).show();
            }
        }
    };

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
        stopReconnectTimer(); // Garante que o timer seja parado
        closeConnection();
    }

    public boolean isConnected() {
        return bluetoothSocket != null && bluetoothSocket.isConnected(); // Verificação mais robusta
    }

    public void connect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth não disponível ou desativado. Não é possível conectar.");
            broadcastConnectionState(false);
            return;
        }
        if (isConnected()) { // Use a nova verificação de isConnected()
            Log.d(TAG, "Já conectado. Ignorando nova tentativa de conexão.");
            return;
        }

        stopReconnectTimer(); // Para qualquer timer de reconexão pendente antes de uma nova tentativa

        new Thread(() -> {
            BluetoothDevice device = findDeviceByName(DEVICE_NAME);
            if (device == null) {
                Log.w(TAG, "Dispositivo Bluetooth " + DEVICE_NAME + " não encontrado nos pareados.");
                broadcastConnectionState(false);
                startReconnectTimer(); // Inicia o timer se não encontrou o dispositivo
                return;
            }

            try {
                // Se o socket existe e não está conectado, fecha antes de criar um novo
                if (bluetoothSocket != null && !bluetoothSocket.isConnected()) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Erro ao fechar socket antigo", e);
                    }
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true; // Atualiza o estado
                reconnectAttemptCount = 0; // Reseta o contador ao conectar com sucesso
                Log.i(TAG, "Conexão Bluetooth estabelecida com " + DEVICE_NAME);
                broadcastConnectionState(true);
                listenForData();
            } catch (IOException e) {
                Log.e(TAG, "Erro ao conectar com " + DEVICE_NAME + ": " + e.getMessage());
                closeConnection(); // Fecha a conexão de forma limpa em caso de erro
                broadcastConnectionState(false);
                startReconnectTimer(); // Inicia o timer para tentar reconectar
            }
        }).start();
    }

    private BluetoothDevice findDeviceByName(String name) {
        if (bluetoothAdapter == null) return null;
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null) return null; // Pode ser null se não houver permissão
        for (BluetoothDevice device : pairedDevices) {
            if (name.equals(device.getName())) {
                return device;
            }
        }
        return null;
    }

    private void listenForData() {
        // Se já houver um thread de escuta rodando, pode ser preciso parar o anterior
        // Para simplificar, vou assumir que apenas um thread listenForData() será chamado por conexão
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            // Verifica isConnected aqui também para garantir que o loop só rode se a conexão estiver ativa
            while (isConnected() && inputStream != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String receivedData = new String(buffer, 0, bytes).trim(); // Adiciona trim aqui
                        Log.d(TAG, "Dados recebidos: '" + receivedData + "'");
                        broadcastDataReceived(receivedData);
                        if (dataListener != null) {
                            dataListener.onDataReceived(receivedData);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Erro na leitura de dados ou conexão perdida: " + e.getMessage());
                    // Esta é a principal maneira de detectar uma desconexão
                    closeConnection();
                    broadcastConnectionState(false);
                    startReconnectTimer(); // Inicia a reconexão após a desconexão
                    break; // Sai do loop de leitura
                }
            }
            Log.i(TAG, "Parando de escutar dados. Conexão perdida ou encerrada.");
        }, "BluetoothDataListenerThread").start(); // Dando um nome para o thread para facilitar a depuração
    }

    public void sendData(String data) {
        if (!isConnected()) { // Use a nova verificação de isConnected()
            Log.w(TAG, "Não é possível enviar dados: Bluetooth não conectado.");
            return;
        }
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
                Log.d(TAG, "Dados enviados: " + data);
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao enviar dados: " + e.getMessage());
            // Se o envio falhar, a conexão pode ter sido perdida
            closeConnection();
            broadcastConnectionState(false);
            startReconnectTimer(); // Inicia a reconexão
        }
    }

    public void closeConnection() {
        stopReconnectTimer(); // Para o timer de reconexão quando a conexão é fechada explicitamente
        if (!isConnected()) { // Já desconectado
            Log.d(TAG, "Conexão já está fechada.");
            return;
        }
        try {
            // A ordem de fechamento é importante: streams antes do socket
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar inputStream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar outputStream", e);
                }
            }
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar bluetoothSocket", e);
                }
            }
            Log.i(TAG, "Conexão Bluetooth fechada.");
        } catch (Exception e) { // Captura qualquer outra exceção inesperada
            Log.e(TAG, "Erro inesperado ao fechar conexão: " + e.getMessage(), e);
        } finally {
            inputStream = null;
            outputStream = null;
            bluetoothSocket = null;
            isConnected = false; // Garante que o estado seja false
            reconnectAttemptCount = 0; // Reseta o contador de tentativas de reconexão
        }
    }

    private void broadcastDataReceived(String data) {
        Intent intent = new Intent("bluetooth_data_received");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastConnectionState(boolean connected) {
        this.isConnected = connected; // Atualiza a variável interna
        Intent intent = new Intent("bluetooth_connection_state");
        intent.putExtra("is_connected", connected);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Métodos de gerenciamento do timer de reconexão
    private void startReconnectTimer() {
        if (reconnectAttemptCount < MAX_RECONNECT_ATTEMPTS) {
            reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS);
            Log.d(TAG, "Timer de reconexão iniciado. Próxima tentativa em " + RECONNECT_DELAY_MS + "ms.");
        } else {
            Log.w(TAG, "Não iniciando timer de reconexão: máximo de tentativas atingido.");
        }
    }

    private void stopReconnectTimer() {
        reconnectHandler.removeCallbacks(reconnectRunnable);
        Log.d(TAG, "Timer de reconexão parado.");
    }
}