package com.example.talksbutton;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BT_PERMISSIONS = 1;

    private ImageView bt1, bt2, bt3, bt4, btLista;
    private BluetoothService mService;
    private boolean mBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            // Você pode definir um listener aqui se precisar de callbacks diretos
            // mService.setDataListener(MainActivity.this::handleBluetoothData);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
        }
    };

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_data_received".equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                handleBluetoothData(data);
            }
        }
    };

    private final BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_connection_state".equals(intent.getAction())) {
                boolean isConnected = intent.getBooleanExtra("is_connected", false);
                Toast.makeText(MainActivity.this, "Dispositivo Talks Button conectado: " + isConnected, Toast.LENGTH_SHORT).show();
                if (!isConnected) {
                    // Tentar reconectar se a conexão for perdida (opcional)
                    if (mBound && mService != null) {
                        mService.connect();
                    }
                }
            }
        }
    };

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

        // Lógica de clique para cada botão
        bt1.setOnClickListener(v -> {
            openWebApp("App1");
        });
        bt2.setOnClickListener(v -> {
            openWebApp("App2");
        });
        bt3.setOnClickListener(v -> {
            openWebApp("App3");
        });
        bt4.setOnClickListener(v -> {
            openWebApp("App4");
        });
        btLista.setOnClickListener(v -> {
            openGameList();
        });

        if (!hasBluetoothPermissions()) {
            requestPermissions();
        } else {
            startBluetoothService();
        }
    }

    private void startBluetoothService() {
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void handleBluetoothData(String data) {
        runOnUiThread(() -> {
            switch (data.trim()) {
                case "B1":
                    bt1.performClick();
                    break;
                case "B2":
                    bt2.performClick();
                    break;
                case "B3":
                    bt3.performClick();
                    break;
                case "B4":
                    bt4.performClick();
                    break;
                default:
                    //Toast.makeText(this, "Comando desconhecido: " + data.trim(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void openWebApp(String appName) {
        Intent intent = new Intent(MainActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);
        startActivityForResult(intent, 100);
    }

    private void openGameList() {
        Intent intent = new Intent(MainActivity.this, GameListActivity.class);
        startActivity(intent);
    }

    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        }, REQUEST_BT_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BT_PERMISSIONS && hasBluetoothPermissions()) {
            startBluetoothService();
        } else {
            Toast.makeText(this, "Permissões Bluetooth necessárias.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter dataFilter = new IntentFilter("bluetooth_data_received");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDataReceiver, dataFilter);
        IntentFilter connectionFilter = new IntentFilter("bluetooth_connection_state");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothConnectionReceiver, connectionFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothDataReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothConnectionReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
        // Não pare o serviço aqui, ele deve continuar rodando em background
        // stopService(new Intent(this, BluetoothService.class));
    }
}