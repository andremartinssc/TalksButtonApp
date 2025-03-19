package com.example.talksbutton;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class GameListActivity extends AppCompatActivity {

    private ListView listViewGames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        listViewGames = findViewById(R.id.list_view_games);

        // Lista de aplicativos para exibição
        String[] apps = {"App1", "App2", "App3", "App4"};

        // Adapter para preencher a ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, apps);
        listViewGames.setAdapter(adapter);

        // Ação quando um item da lista for clicado
        listViewGames.setOnItemClickListener((parent, view, position, id) -> {
            // Passando o nome do aplicativo para a WebAppActivity
            String appName = apps[position];
            openWebApp(appName);
        });
    }

    private void openWebApp(String appName) {
        Intent intent = new Intent(GameListActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);
        startActivity(intent);
    }
}
