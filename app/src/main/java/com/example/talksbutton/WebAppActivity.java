package com.example.talksbutton;

import android.os.Bundle;
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
            // Tentando carregar o HTML da pasta "assets/aplicacoes"
            try {
                webView.getSettings().setJavaScriptEnabled(true); // Permite JavaScript, se necessário

                // Configurações de exibição do WebView
                WebSettings webSettings = webView.getSettings();
                webSettings.setSupportZoom(true);  // Habilita o zoom
                webSettings.setBuiltInZoomControls(true);  // Exibe controles de zoom
                webSettings.setDisplayZoomControls(false);  // Oculta os controles de zoom padrão
                webSettings.setLoadWithOverviewMode(true);  // Ajusta o conteúdo automaticamente
                webSettings.setUseWideViewPort(true);  // Habilita a largura ampla do conteúdo

                // Carregar o HTML da pasta "assets/aplicacoes"
                webView.loadUrl("file:///android_asset/aplicacoes/" + appName + "/index.html");
            } catch (Exception e) {
                // Caso ocorra algum erro, exibe a mensagem de erro
                Toast.makeText(this, "Erro ao carregar o aplicativo", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Aplicativo não encontrado", Toast.LENGTH_SHORT).show();
        }
    }
}
