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

        // Adicionar o JavaScript interface para manipular a interação
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidInterface");
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
                webSettings.setSupportZoom(false);  // Habilita o zoom
                webSettings.setBuiltInZoomControls(false);  // Exibe controles de zoom
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

    // Método que será chamado para receber os comandos Bluetooth
    public void handleBluetoothCommandInWebView(String command) {
        // Verificar o comando recebido e simular um pressionamento de tecla no WebView
        if (command.equals("B1") || command.equals("1")) {
            webView.evaluateJavascript("document.getElementById('button1').click();", null);
        } else if (command.equals("B2") || command.equals("2")) {
            webView.evaluateJavascript("document.getElementById('button2').click();", null);
        } else if (command.equals("B3") || command.equals("3")) {
            webView.evaluateJavascript("document.getElementById('button3').click();", null);
        } else if (command.equals("B4") || command.equals("4")) {
            webView.evaluateJavascript("document.getElementById('button4').click();", null);
        } else {
            Toast.makeText(this, "Comando desconhecido: " + command, Toast.LENGTH_SHORT).show();
        }
    }

    // Interface JavaScript para manipular a interação com a WebView
    private class WebAppInterface {
        private WebAppActivity mContext;

        WebAppInterface(WebAppActivity context) {
            mContext = context;
        }

        @JavascriptInterface
        public void onButtonPressed(String buttonId) {
            // Método que pode ser chamado a partir do JavaScript na WebView
            // Simula o pressionamento de um botão na WebView com base no botão pressionado
            mContext.handleBluetoothCommandInWebView(buttonId);
        }
    }
}
