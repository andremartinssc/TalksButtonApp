package com.example.talksbutton;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // IDs dos botões definidos no layout
        ImageView bt1 = findViewById(R.id.bt_1);
        ImageView bt2 = findViewById(R.id.bt_2);
        ImageView bt3 = findViewById(R.id.bt_3);
        ImageView bt4 = findViewById(R.id.bt_4);
        ImageView btLista = findViewById(R.id.bt_lista);

        // Associar cada botão a um aplicativo específico
        bt1.setOnClickListener(v -> openWebApp("App1"));
        bt2.setOnClickListener(v -> openWebApp("App2"));
        bt3.setOnClickListener(v -> openWebApp("App3"));
        bt4.setOnClickListener(v -> openWebApp("App4"));

        // Abrir lista de aplicativos
        btLista.setOnClickListener(v -> openGameList());

        // Inicializar o Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // Dispositivo não tem Bluetooth
            Toast.makeText(this, "Bluetooth não é suportado neste dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar permissões de localização (necessárias para Bluetooth em Android 6.0+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permissão de localização se não foi concedida
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
        } else {
            // Permissão já concedida, pode continuar com a funcionalidade do Bluetooth
            checkBluetoothPermission();
        }
    }

    // Método para abrir um WebApp específico baseado na pasta (App1, App2, etc.)
    private void openWebApp(String appName) {
        Intent intent = new Intent(MainActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);  // Passando o nome da pasta (App1, App2, etc.)
        startActivity(intent);
    }

    // Método para abrir a lista de jogos
    private void openGameList() {
        Intent intent = new Intent(MainActivity.this, GameListActivity.class);
        startActivity(intent);
    }

    // Verificar a permissão de Bluetooth
    private void checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permissão de Bluetooth
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
        } else {
            // Permissão já concedida, pode continuar com a funcionalidade do Bluetooth
            checkBluetoothStatus();
        }
    }

    // Verificar se o Bluetooth está ativado
    private void checkBluetoothStatus() {
        try {
            if (!bluetoothAdapter.isEnabled()) {
                // Se o Bluetooth não estiver ativado, solicitar para ativá-lo
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // Bluetooth já está ativado, continue com a funcionalidade
                Toast.makeText(this, "Bluetooth está ativado", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            // Caso a permissão tenha sido negada pelo usuário
            Toast.makeText(this, "Permissão para acessar o Bluetooth negada", Toast.LENGTH_SHORT).show();
        }
    }

    // Resultado da solicitação de permissões em tempo de execução
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão de localização foi concedida, agora verificar permissão de Bluetooth
                checkBluetoothPermission();
            } else {
                // A permissão de localização foi negada
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão para Bluetooth foi concedida, agora verificar o status do Bluetooth
                checkBluetoothStatus();
            } else {
                // A permissão de Bluetooth foi negada
                Toast.makeText(this, "Permissão para acessar o Bluetooth negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Resultado da solicitação para ativar o Bluetooth
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // O Bluetooth foi ativado, agora você pode usá-lo
                Toast.makeText(this, "Bluetooth ativado", Toast.LENGTH_SHORT).show();
            } else {
                // O usuário não ativou o Bluetooth
                Toast.makeText(this, "Bluetooth não foi ativado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
