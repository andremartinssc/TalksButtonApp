package com.example.talksbutton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameListActivity extends AppCompatActivity {

    private ListView listViewGames;
    private List<String> appsList;
    private int selectedPosition = 0; // Posição do item selecionado
    private ArrayAdapter<String> adapter;

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_data_received".equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                handleBluetoothData(data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        listViewGames = findViewById(R.id.list_view_games);
        appsList = new ArrayList<>();

        // Busca dinamicamente as pastas dentro de 'aplicacoes'
        loadAppsFromAssets();

        // Adapter para preencher a ListView
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, appsList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (position == selectedPosition) {
                    view.setBackgroundColor(Color.LTGRAY); // Cor de fundo para o item selecionado
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT); // Cor de fundo padrão
                }
                return view;
            }
        };
        listViewGames.setAdapter(adapter);

        // Ação quando um item da lista for clicado (para abrir o aplicativo)
        listViewGames.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position; // Atualiza a posição selecionada
            updateSelection(); // Atualiza a seleção visual
            openWebApp(appsList.get(position)); // Abre o aplicativo
        });

        // Configurar a seleção inicial
        updateSelection();
    }

    private void loadAppsFromAssets() {
        AssetManager assetManager = getAssets();
        try {
            String[] appFolders = assetManager.list("aplicacoes");
            if (appFolders != null) {
                for (String folder : appFolders) {
                    // Adiciona apenas se for um diretório (uma pasta de aplicativo)
                    try {
                        String[] files = assetManager.list("aplicacoes/" + folder);
                        if (files != null && files.length > 0) {
                            appsList.add(folder);
                        }
                    } catch (IOException e) {
                        // Se não for um diretório, a listagem falhará, podemos ignorar
                    }
                }
            }
        } catch (IOException e) {
            Log.e("GameListActivity", "Erro ao listar assets", e);
        }
    }

    private void openWebApp(String appName) {
        Intent intent = new Intent(GameListActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);
        startActivity(intent);
    }

    private void handleBluetoothData(String data) {
        runOnUiThread(() -> {
            switch (data.trim()) {
                case "B1":
                    selectNextApp();
                    break;
                case "B2":
                    selectPreviousApp();
                    break;
                case "B3":
                    openSelectedApp();
                    break;
                case "B5":
                    finish(); // Retorna para MainActivity
                    break;
            }
        });
    }

    private void selectNextApp() {
        selectedPosition = (selectedPosition + 1) % appsList.size();
        updateSelection();
    }

    private void selectPreviousApp() {
        selectedPosition = (selectedPosition - 1 + appsList.size()) % appsList.size();
        updateSelection();
    }

    private void openSelectedApp() {
        openWebApp(appsList.get(selectedPosition));
    }

    private void updateSelection() {
        adapter.notifyDataSetChanged(); // Atualiza a lista para refletir a seleção
        listViewGames.setSelection(selectedPosition);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter dataFilter = new IntentFilter("bluetooth_data_received");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDataReceiver, dataFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothDataReceiver);
    }
}