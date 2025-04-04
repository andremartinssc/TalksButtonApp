package com.example.talksbutton;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BT_PERMISSIONS = 1;
    private TextView txtData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtData = findViewById(R.id.txtData);

        if (!hasBluetoothPermissions()) {
            requestPermissions();
        } else {
            startBluetooth();
        }
    }

    private void startBluetooth() {
        BluetoothConnection connection = BluetoothConnection.getInstance();
        connection.setDataListener(data -> runOnUiThread(() -> {
            // Exibe exatamente o que foi recebido, apenas se for B1 a B4
            switch (data.trim()) {
                case "B1":
                case "B2":
                case "B3":
                case "B4":
                    txtData.setText("Comando recebido: " + data.trim());
                    break;
                default:
                    txtData.setText("Dado desconhecido: " + data.trim());
                    break;
            }
        }));
        connection.connect();
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // <-- adicionado

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
}
