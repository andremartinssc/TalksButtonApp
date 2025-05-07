package com.example.talksbutton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class ViewHolder {
    ImageView coverImageView;
    TextView appNameTextView;
    Button openButton;
    Button deleteButton;
    Button applyButton;
}

public class GameListActivity extends AppCompatActivity {

    private ListView listViewGames;
    private List<AppData> appsList;
    private int selectedPosition = 0;
    private AppListAdapter adapter;
    private Button backToMainButton; // Adicionei a declaração do botão

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_data_received".equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                handleBluetoothData(data);
            }
        }
    };

    private static class AppData {
        String folderName;
        String displayName;
        Bitmap coverImage;

        public AppData(String folderName, String displayName, Bitmap coverImage) {
            this.folderName = folderName;
            this.displayName = displayName;
            this.coverImage = coverImage;
        }
    }

    private class AppListAdapter extends ArrayAdapter<AppData> {
        private final LayoutInflater inflater;

        public AppListAdapter(Context context, List<AppData> apps) {
            super(context, R.layout.list_item_app, apps);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_app, parent, false);
                holder = new ViewHolder();
                holder.coverImageView = convertView.findViewById(R.id.iv_cover);
                holder.appNameTextView = convertView.findViewById(R.id.tv_app_name);
                holder.openButton = convertView.findViewById(R.id.btn_open);
                holder.deleteButton = convertView.findViewById(R.id.btn_delete);
                holder.applyButton = convertView.findViewById(R.id.btn_apply);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppData currentApp = getItem(position);
            if (currentApp != null) {
                holder.appNameTextView.setText(currentApp.displayName);
                if (currentApp.coverImage != null) {
                    holder.coverImageView.setImageBitmap(currentApp.coverImage);
                } else {
                    holder.coverImageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }

                final String appToOpen = currentApp.folderName;
                holder.openButton.setOnClickListener(v -> {
                    openWebApp(appToOpen);
                });

                final String appToDelete = currentApp.folderName;
                holder.deleteButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Excluir " + appToDelete, Toast.LENGTH_SHORT).show();
                });

                final String appToApply = currentApp.folderName;
                holder.applyButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Aplicar " + appToApply + " à tela inicial", Toast.LENGTH_SHORT).show();
                });

                if (position == selectedPosition) {
                    // Definir uma cor de fundo que pareça uma borda sutil (cinza claro com baixa opacidade)
                    convertView.setBackgroundColor(0x1A808080); // Cinza com baixa opacidade
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                }
            }

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        backToMainButton = findViewById(R.id.btn_back_to_main); // Inicializei o botão
        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(GameListActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Opcional: finalizar a GameListActivity ao voltar
        });

        listViewGames = findViewById(R.id.list_view_games);
        appsList = new ArrayList<>();

        loadAppsWithCoversFromAssets();

        adapter = new AppListAdapter(this, appsList);
        listViewGames.setAdapter(adapter);

        updateSelection();
    }

    private void loadAppsWithCoversFromAssets() {
        AssetManager assetManager = getAssets();
        try {
            String[] appFolders = assetManager.list("aplicacoes");
            if (appFolders != null) {
                for (String folder : appFolders) {
                    try {
                        InputStream coverInputStream = null;
                        Bitmap coverBitmap = null;
                        String displayName = folder;

                        InputStream configInputStream = null;
                        try {
                            configInputStream = assetManager.open("aplicacoes/" + folder + "/info.txt");
                            Scanner s = new Scanner(configInputStream).useDelimiter("\\A");
                            String configContent = s.hasNext() ? s.next() : "";
                            String[] lines = configContent.split("\n");
                            for (String line : lines) {
                                if (line.trim().startsWith("titulo:")) {
                                    displayName = line.trim().substring("titulo:".length()).trim();
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            Log.w("GameListActivity", "Arquivo info.txt não encontrado para: " + folder + ". Usando nome da pasta.");
                        } finally {
                            if (configInputStream != null) {
                                configInputStream.close();
                            }
                        }

                        try {
                            coverInputStream = assetManager.open("aplicacoes/" + folder + "/capa.jpg");
                            coverBitmap = BitmapFactory.decodeStream(coverInputStream);
                        } catch (IOException e) {
                            Log.w("GameListActivity", "Capa não encontrada para: " + folder);
                        } finally {
                            if (coverInputStream != null) {
                                coverInputStream.close();
                            }
                        }

                        String[] files = assetManager.list("aplicacoes/" + folder);
                        if (files != null && files.length > 0) {
                            appsList.add(new AppData(folder, displayName, coverBitmap));
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
                    finish();
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
        openWebApp(appsList.get(selectedPosition).folderName);
    }

    private void updateSelection() {
        adapter.notifyDataSetChanged();
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