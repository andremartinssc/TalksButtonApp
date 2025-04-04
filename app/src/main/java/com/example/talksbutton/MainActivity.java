package com.example.talksbutton;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BT_PERMISSIONS = 1;

    private ImageView bt1, bt2, bt3, bt4, btLista;
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

        // Lógica de clique para cada botão
        bt1.setOnClickListener(v -> {
            if (!isWebAppOpen) openWebApp("App1");
        });
        bt2.setOnClickListener(v -> {
            if (!isWebAppOpen) openWebApp("App2");
        });
        bt3.setOnClickListener(v -> {
            if (!isWebAppOpen) openWebApp("App3");
        });
        bt4.setOnClickListener(v -> {
            if (!isWebAppOpen) openWebApp("App4");
        });
        btLista.setOnClickListener(v -> {
            if (!isWebAppOpen) openGameList();
        });

        if (!hasBluetoothPermissions()) {
            requestPermissions();
        } else {
            startBluetooth();
        }
    }

    private void startBluetooth() {
        BluetoothConnection connection = BluetoothConnection.getInstance();
        connection.setDataListener(data -> runOnUiThread(() -> {
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
                    Toast.makeText(this, "Comando desconhecido: " + data.trim(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }));
        connection.connect(); // Inicia a conexão Bluetooth no Singleton
    }

    private void openWebApp(String appName) {
        Intent intent = new Intent(MainActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);
        startActivityForResult(intent, 100);
        isWebAppOpen = true;
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
            startBluetooth();
        } else {
            Toast.makeText(this, "Permissões Bluetooth necessárias.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothConnection.getInstance().closeConnection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            isWebAppOpen = false;
        }
    }
}
