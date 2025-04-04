package com.example.talksbutton;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WebAppActivity extends AppCompatActivity {

    private WebView webView;
    private BluetoothConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webapp);

        webView = findViewById(R.id.webView);

        String appName = getIntent().getStringExtra("app_name");

        configurarWebView();
        carregarAplicativo(appName);
        iniciarEscutaBluetooth();
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

    private void iniciarEscutaBluetooth() {
        connection = BluetoothConnection.getInstance();
        connection.setDataListener(data -> runOnUiThread(() -> {
            String comando = data.trim();
            switch (comando) {
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
                default:
                    Toast.makeText(this, "Comando desconhecido: " + comando, Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void simularTecla(String tecla) {
        String js = "var e = new KeyboardEvent('keydown', { key: '" + tecla + "' }); document.dispatchEvent(e);";
        webView.evaluateJavascript(js, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            connection.setDataListener(null); // Remove o listener para evitar vazamentos de memória
        }
    }
}
