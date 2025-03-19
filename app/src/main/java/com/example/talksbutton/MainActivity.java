package com.example.talksbutton;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

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
}
