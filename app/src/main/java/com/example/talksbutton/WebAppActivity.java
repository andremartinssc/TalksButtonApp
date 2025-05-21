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
import android.widget.Toast; // Adicione esta importação para usar Toast

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

public class WebAppActivity extends AppCompatActivity {

    private WebView webView;
    private BluetoothService mService;
    private boolean mBound = false;
    private ImageButton btnVoltar;
    private AudioManager audioManager;
    private LedController ledController;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d("WebAppActivity", "Serviço Bluetooth conectado.");
            // Inicializa LedController após a conexão com o serviço
            ledController = new LedController(mService, mBound);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
            ledController = null; // Limpa a referência ao LedController
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
                // Garante que LedController seja inicializado após a conexão
                if (isConnected && mBound && mService != null && ledController == null) {
                    ledController = new LedController(mService, mBound);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webapp);

        webView = findViewById(R.id.webView);
        btnVoltar = findViewById(R.id.btnVoltar);

        // Recebe o nome da pasta do aplicativo e o tipo de caminho (asset ou internal)
        String appFolderName = getIntent().getStringExtra("app_folder_name");
        String appPathType = getIntent().getStringExtra("app_path_type");


        configurarWebView();
        carregarAplicativo(appFolderName, appPathType); // Passa ambos os parâmetros

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

        // --- Importante: Adicione estas duas linhas para permitir acesso a arquivos locais ---
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        // --- Fim da adição ---

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("app://sair")) {
                    finish();
                    return true;
                } else if (url.startsWith("talksbutton://led/")) {
                    // Intercepta comandos de LED simplificados
                    processarComandoLedSimplificado(url);
                    return true; // Indica que o Android lidou com a URL
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e("WebAppActivity", "Erro no carregamento do WebView: " + description + " URL: " + failingUrl);
                Toast.makeText(WebAppActivity.this, "Erro ao carregar o aplicativo: " + description, Toast.LENGTH_LONG).show();
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Impede rolagem por toque para evitar comportamento indesejado em apps que não devem rolar
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
    }

    // Modificado para receber o nome da pasta e o tipo de caminho
    private void carregarAplicativo(String appFolderName, String appPathType) {
        if (appFolderName != null) {
            try {
                String url;
                if ("internal".equals(appPathType)) {
                    // Construir a URL para o arquivo no armazenamento interno
                    // Usa GameListActivity.IMPORTED_APPS_FOLDER para garantir que o caminho esteja correto
                    File appDir = new File(getFilesDir(), GameListActivity.IMPORTED_APPS_FOLDER + File.separator + appFolderName);
                    File indexFile = new File(appDir, "index.html");

                    if (indexFile.exists()) { // Verifica se o index.html realmente existe
                        url = "file://" + indexFile.getAbsolutePath();
                        Log.d("WebAppActivity", "Carregando aplicativo do armazenamento interno: " + url);
                    } else {
                        Log.e("WebAppActivity", "index.html não encontrado no caminho interno: " + indexFile.getAbsolutePath());
                        Toast.makeText(this, "Erro: index.html não encontrado no app importado '" + appFolderName + "'.", Toast.LENGTH_LONG).show();
                        // Opcional: Voltar para a tela anterior ou mostrar uma mensagem de erro mais elaborada
                        return; // Sai do método se o arquivo não existe
                    }
                } else { // Assume "asset" se não for "internal" ou se appPathType for nulo
                    url = "file:///android_asset/aplicacoes/" + appFolderName + "/index.html";
                    Log.d("WebAppActivity", "Carregando aplicativo dos assets: " + url);
                }
                webView.loadUrl(url);
            } catch (Exception e) {
                Log.e("WebAppActivity", "Erro ao carregar aplicativo: " + appFolderName, e);
                Toast.makeText(this, "Erro ao carregar o aplicativo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w("WebAppActivity", "Nome do aplicativo não encontrado.");
            Toast.makeText(this, "Nome do aplicativo não especificado.", Toast.LENGTH_LONG).show();
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
                finish(); // Ou alguma outra ação de inatividade
                return;
        }
        if (!js.isEmpty()) {
            webView.evaluateJavascript(js, null);
            Log.d("WebAppActivity", "Executando JavaScript: " + js);
        }
    }

    private void processarComandoLedSimplificado(String url) {
        try {
            String comandoCru = url.replace("talksbutton://led/", "");
            String[] partes = comandoCru.split("/");

            if (partes.length == 2) {
                int numeroLed = Integer.parseInt(partes[0]);
                long duracao = Long.parseLong(partes[1]);

                if (ledController != null) {
                    ledController.ligarLed(numeroLed, duracao);
                } else {
                    Log.w("WebAppActivity", "LedController não inicializado ou serviço Bluetooth não conectado.");
                }
            } else {
                Log.w("WebAppActivity", "Formato de comando de LED simplificado inválido: " + url + ". Use: talksbutton://led/NUMERO_LED/DURACAO_MS");
            }
        } catch (NumberFormatException e) {
            Log.e("WebAppActivity", "Erro ao analisar comando de LED simplificado: " + url, e);
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
        if (webView != null) {
            webView.loadUrl("about:blank"); // Limpa o WebView
            webView.clearHistory();
            webView.destroy(); // Libera recursos do WebView
        }
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        // Permite que o WebView volte na sua própria história de navegação
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Se o WebView não puder voltar, chama a função padrão de voltar da Activity
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        // Intercepta a tecla KEYCODE_5 (se for o botão de sair do seu controle)
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_5) {
            onBackPressed(); // Chama o método de voltar
            return true; // Indica que o evento foi consumido
        }
        return super.dispatchKeyEvent(event); // Deixa o sistema lidar com outras teclas
    }

    private void stopAllPlayback() {
        // Abandona o foco de áudio para parar qualquer reprodução de mídia do WebView.
        // Isso é uma boa prática para garantir que o áudio pare ao sair da Activity.
        audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        );
        audioManager.abandonAudioFocus(null);
    }
}