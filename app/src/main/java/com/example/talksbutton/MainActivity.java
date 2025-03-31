package com.example.talksbutton;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 3;
    private static final String TARGET_DEVICE_NAME = "TalksButton_ESP32";

    private BluetoothConnection bluetoothConnection;

    // IDs dos botões definidos no layout
    private ImageView bt1, bt2, bt3, bt4, btLista;

    // Flag para controlar se a WebView está aberta
    private boolean isWebAppOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar os botões
        bt1 = findViewById(R.id.bt_1);
        bt2 = findViewById(R.id.bt_2);
        bt3 = findViewById(R.id.bt_3);
        bt4 = findViewById(R.id.bt_4);
        btLista = findViewById(R.id.bt_lista);

        // Inicializar o BluetoothConnection
        bluetoothConnection = new BluetoothConnection(new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    String receivedData = (String) msg.obj;
                    handleBluetoothCommand(receivedData);
                }
                return true;
            }
        }));

        // Associar cada botão a um aplicativo específico
        bt1.setOnClickListener(v -> {
            if (!isWebAppOpen) {
                openWebApp("App1");
            }
        });
        bt2.setOnClickListener(v -> {
            if (!isWebAppOpen) {
                openWebApp("App2");
            }
        });
        bt3.setOnClickListener(v -> {
            if (!isWebAppOpen) {
                openWebApp("App3");
            }
        });
        bt4.setOnClickListener(v -> {
            if (!isWebAppOpen) {
                openWebApp("App4");
            }
        });

        // Abrir lista de aplicativos
        btLista.setOnClickListener(v -> {
            if (!isWebAppOpen) {
                openGameList();
            }
        });

        // Verificar permissões de localização
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permissão de localização
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
        } else {
            // Permissão já concedida, verificar Bluetooth
            checkBluetoothPermission();
        }
    }

    // Método para abrir um WebApp específico baseado na pasta (App1, App2, etc.)
    private void openWebApp(String appName) {
        Intent intent = new Intent(MainActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);  // Passando o nome da pasta (App1, App2, etc.)
        startActivityForResult(intent, 100);  // Iniciar a WebAppActivity e esperar o retorno
        isWebAppOpen = true;  // Marcar que a WebApp está aberta
    }

    // Método para abrir a lista de jogos
    private void openGameList() {
        Intent intent = new Intent(MainActivity.this, GameListActivity.class);
        startActivity(intent);
    }

    // Verificar a permissão de Bluetooth
    private void checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permissões de Bluetooth
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, REQUEST_BLUETOOTH_PERMISSION);
        } else {
            // Permissão já concedida, verificar o Bluetooth
            checkBluetoothStatus();
        }
    }

    // Verificar se o Bluetooth está ativado
    private void checkBluetoothStatus() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            // Se o Bluetooth não estiver ativado, solicitar para ativá-lo
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // Bluetooth já está ativado, começar a busca por dispositivos
            Toast.makeText(this, "Bluetooth está ativado", Toast.LENGTH_SHORT).show();
            startBluetoothScan();
        }
    }

    // Iniciar o escaneamento para encontrar dispositivos Bluetooth
    private void startBluetoothScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();

        // Registrar o receptor para descobrir dispositivos
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);
    }

    // Receptor de escaneamento Bluetooth para encontrar o dispositivo específico
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null && TARGET_DEVICE_NAME.equals(device.getName())) {
                    // Encontramos o dispositivo desejado, parar o escaneamento
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    bluetoothAdapter.cancelDiscovery();

                    // Conectar ao dispositivo encontrado
                    bluetoothConnection.connect(device.getAddress());
                }
            }
        }
    };

    // Lidar com o comando recebido via Bluetooth
    private void handleBluetoothCommand(String command) {
        // Verifica o comando recebido e aciona o botão correspondente
        if (command.equals("B1")) {
            // Simular clique do bt1
            bt1.performClick();
        } else if (command.equals("B2")) {
            // Simular clique do bt2
            bt2.performClick();
        } else if (command.equals("B3")) {
            // Simular clique do bt3
            bt3.performClick();
        } else if (command.equals("B4")) {
            // Simular clique do bt4
            bt4.performClick();
        } else {
            Toast.makeText(this, "Comando desconhecido: " + command, Toast.LENGTH_SHORT).show();
        }
    }

    // Resultado da solicitação de permissões em tempo de execução
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão de localização foi concedida, agora verificar Bluetooth
                checkBluetoothPermission();
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão de Bluetooth concedida, começar escaneamento
                checkBluetoothStatus();
            } else {
                Toast.makeText(this, "Permissão de Bluetooth negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Desregistrar o receptor de Bluetooth ao destruir a Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
        bluetoothConnection.closeConnection();
    }

    // Retornar da WebAppActivity e permitir o acionamento dos botões
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            isWebAppOpen = false;  // A WebApp foi fechada, os botões podem ser acionados novamente
        }
    }
}
