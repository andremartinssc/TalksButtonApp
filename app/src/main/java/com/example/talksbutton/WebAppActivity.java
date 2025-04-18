package com.example.talksbutton;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class WebAppActivity extends AppCompatActivity {

    private WebView webView;
    private BluetoothService mService;
    private boolean mBound = false;
    private ImageButton btnVoltar;
    private AudioManager audioManager;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d("WebAppActivity", "Serviço Bluetooth conectado.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
            Log.d("WebAppActivity", "Serviço Bluetooth desconectado.");
        }
    };

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_data_received".equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                simularAcaoNoWebView(data.trim());
            }
        }
    };

    private final BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_connection_state".equals(intent.getAction())) {
                boolean isConnected = intent.getBooleanExtra("is_connected", false);
                Log.d("WebAppActivity", "Bluetooth conectado: " + isConnected);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webapp);

        webView = findViewById(R.id.webView);
        btnVoltar = findViewById(R.id.btnVoltar);

        String appName = getIntent().getStringExtra("app_name");

        configurarWebView();
        carregarAplicativo(appName);

        Intent serviceIntent = new Intent(this, BluetoothService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_1:
                            webView.evaluateJavascript("javascript:document.getElementById('button1').focus(); var event1 = new KeyboardEvent('keydown', {'key': '1'}); document.dispatchEvent(event1);", null);
                            return true;
                        case KeyEvent.KEYCODE_2:
                            webView.evaluateJavascript("javascript:document.getElementById('button2').focus(); var event2 = new KeyboardEvent('keydown', {'key': '2'}); document.dispatchEvent(event2);", null);
                            return true;
                        case KeyEvent.KEYCODE_3:
                            webView.evaluateJavascript("javascript:document.getElementById('button3').focus(); var event3 = new KeyboardEvent('keydown', {'key': '3'}); document.dispatchEvent(event3);", null);
                            return true;
                        case KeyEvent.KEYCODE_4:
                            webView.evaluateJavascript("javascript:document.getElementById('button4').focus(); var event4 = new KeyboardEvent('keydown', {'key': '4'}); document.dispatchEvent(event4);", null);
                            return true;
                        default:
                            return false;
                    }
                }
                return false;
            }
        });

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    private void configurarWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webSettings.setTextZoom(100);
        webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.equals("app://sair")) {
                    finish();
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
    }

    private void carregarAplicativo(String appName) {
        if (appName != null) {
            try {
                String url = "file:///android_asset/aplicacoes/" + appName + "/index.html";
                webView.loadUrl(url);
                Log.d("WebAppActivity", "Carregando aplicativo: " + url);
            } catch (Exception e) {
                Log.e("WebAppActivity", "Erro ao carregar aplicativo", e);
            }
        } else {
            Log.w("WebAppActivity", "Aplicativo não encontrado");
        }
    }

    private void simularAcaoNoWebView(String comando) {
        String js = "";
        switch (comando) {
            case "B1":
                js = "javascript:document.getElementById('button1').focus(); var event1 = new KeyboardEvent('keydown', {'key': '1'}); document.dispatchEvent(event1);";
                break;
            case "B2":
                js = "javascript:document.getElementById('button2').focus(); var event2 = new KeyboardEvent('keydown', {'key': '2'}); document.dispatchEvent(event2);";
                break;
            case "B3":
                js = "javascript:document.getElementById('button3').focus(); var event3 = new KeyboardEvent('keydown', {'key': '3'}); document.dispatchEvent(event3);";
                break;
            case "B4":
                js = "javascript:document.getElementById('button4').focus(); var event4 = new KeyboardEvent('keydown', {'key': '4'}); document.dispatchEvent(event4);";
                break;
            case "B5":
                onBackPressed();
                return;
            case "INATIVIDADE":
                finish();
                return;
        }
        if (!js.isEmpty()) {
            webView.evaluateJavascript(js, null);
            Log.d("WebAppActivity", "Executando JavaScript: " + js);
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
        stopAllPlayback();
        webView.loadUrl("about:blank");
        webView.clearHistory();
        webView.destroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_5) {
            onBackPressed();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void stopAllPlayback() {
        audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        );
        audioManager.abandonAudioFocus(null);
    }
}