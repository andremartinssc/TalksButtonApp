package com.example.talksbutton;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WebAppActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webapp);

        // Inicializando o WebView
        webView = findViewById(R.id.webView);

        // Recuperando o nome da pasta passada pela MainActivity
        String appName = getIntent().getStringExtra("app_name");

        // Carregar o conteúdo HTML da pasta correspondente
        carregarAplicativo(appName);
    }

    // Método para carregar o aplicativo HTML a partir da pasta correspondente
    private void carregarAplicativo(String appName) {
        // Verifica se o nome do app foi passado corretamente
        if (appName != null) {
            try {
                webView.getSettings().setJavaScriptEnabled(true); // Permite JavaScript, se necessário

                // Configurações de exibição do WebView
                WebSettings webSettings = webView.getSettings();
                webSettings.setSupportZoom(false);  // Desabilita o zoom
                webSettings.setBuiltInZoomControls(false);
                webSettings.setDisplayZoomControls(false);
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);

                // Carregar o HTML da pasta "assets/aplicacoes"
                webView.loadUrl("file:///android_asset/aplicacoes/" + appName + "/index.html");
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao carregar o aplicativo", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Aplicativo não encontrado", Toast.LENGTH_SHORT).show();
        }
    }
}
