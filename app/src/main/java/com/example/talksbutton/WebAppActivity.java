package com.example.talksbutton;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class WebAppActivity extends AppCompatActivity {

    private WebView webView;
    private BluetoothService mService;
    private boolean mBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            // Você pode definir um listener aqui se precisar de callbacks diretos
            // mService.setDataListener(WebAppActivity.this::handleBluetoothData);
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
                // Faça algo com os dados recebidos na WebAppActivity, se necessário
                // Por exemplo, exibir em um log
                Toast.makeText(WebAppActivity.this, "Dados Bluetooth recebidos na WebApp: " + data, Toast.LENGTH_SHORT).show();
                // Aqui você pode processar os comandos Bluetooth específicos para a WebView
                switch (data.trim()) {
                    case "B1":
                        simularTecla("1");
                        break;
                    case "B2":
                        simularTecla("2");
                        break;
                    case "B3":
                        simularTecla("3");
                        break;
                    case "B4":
                        simularTecla("4");
                        break;
                    case "B5":
                        simularTecla("5");
                        break;
                    // Adicione outros comandos conforme necessário
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_connection_state".equals(intent.getAction())) {
                boolean isConnected = intent.getBooleanExtra("is_connected", false);
                Toast.makeText(WebAppActivity.this, "Bluetooth conectado na WebApp: " + isConnected, Toast.LENGTH_SHORT).show();
                if (!isConnected) {
                    // Tentar reconectar se a conexão for perdida (opcional, o serviço já tenta)
                    // Intent serviceIntent = new Intent(WebAppActivity.this, BluetoothService.class);
                    // bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webapp);

        webView = findViewById(R.id.webView);

        String appName = getIntent().getStringExtra("app_name");

        configurarWebView();
        carregarAplicativo(appName);

        // Conectar ao serviço Bluetooth
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void configurarWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient()); // Evita abrir navegador externo
    }

    private void carregarAplicativo(String appName) {
        if (appName != null) {
            try {
                String url = "file:///android_asset/aplicacoes/" + appName + "/index.html";
                webView.loadUrl(url);
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao carregar aplicativo", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Aplicativo não encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    private void simularTecla(String tecla) {
        if (mBound && mService != null) {
            String js = "var e = new KeyboardEvent('keydown', { key: '" + tecla + "' }); document.dispatchEvent(e);";
            webView.evaluateJavascript(js, null);
        } else {
            Toast.makeText(this, "Serviço Bluetooth não conectado.", Toast.LENGTH_SHORT).show();
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
    }
}