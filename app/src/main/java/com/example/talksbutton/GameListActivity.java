package com.example.talksbutton;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ViewHolder {
    ImageView coverImageView;
    TextView appNameTextView;
    Button openButton;
    Button deleteButton;
    Button applyButton;
}

public class GameListActivity extends AppCompatActivity {

    private static final String ASSET_APPS_FOLDER = "aplicacoes";
    private static final String[] FIXED_APP_FOLDERS = {"App1", "App2", "App3", "App4"}; // Correção: "App" maiúsculo
    private static final int REQUEST_STORAGE_PERMISSION = 101;

    private ListView listViewGames;
    private List<AppData> appsList;
    private int selectedPosition = 0;
    private AppListAdapter adapter;
    private Button backToMainButton;
    private Button importButton;

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_data_received".equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                handleBluetoothData(data);
            }
        }
    };

    private ActivityResultLauncher<Intent> filePickerLauncher;

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
                    if (!isFixedApp(appToDelete)) {
                        // Implementar lógica de exclusão aqui (usar appToDelete)
                        Toast.makeText(getContext(), "Excluir " + appToDelete, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Este aplicativo não pode ser excluído", Toast.LENGTH_SHORT).show();
                    }
                });

                final String appToApply = currentApp.folderName;
                holder.applyButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Aplicar " + appToApply + " à tela inicial", Toast.LENGTH_SHORT).show();
                    // Implementar lógica para aplicar à tela inicial aqui (usar appToApply)
                });

                if (position == selectedPosition) {
                    convertView.setBackgroundColor(0x1A808080); // Cinza com baixa opacidade
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                }
            }

            return convertView;
        }

        private boolean isFixedApp(String folderName) {
            for (String fixedFolder : FIXED_APP_FOLDERS) {
                if (fixedFolder.equals(folderName)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        backToMainButton = findViewById(R.id.btn_back_to_main);
        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(GameListActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        importButton = findViewById(R.id.btn_import);
        importButton.setOnClickListener(v -> checkStoragePermissionAndImport());

        listViewGames = findViewById(R.id.list_view_games);
        appsList = new ArrayList<>();

        loadAppsWithCoversFromAssets();

        adapter = new AppListAdapter(this, appsList);
        listViewGames.setAdapter(adapter);

        updateSelection();

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            importZipFile(uri);
                        } else {
                            Toast.makeText(this, "Erro ao selecionar o arquivo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkStoragePermissionAndImport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this, "Permissão de armazenamento negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        filePickerLauncher.launch(intent);
    }

    private void importZipFile(Uri zipUri) {
        AssetManager assetManager = getAssets();
        File assetsDir = new File(getFilesDir().getParentFile(), "assets/" + ASSET_APPS_FOLDER);
        if (!assetsDir.exists()) {
            assetsDir.mkdirs();
        }

        try (InputStream inputStream = getContentResolver().openInputStream(zipUri);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            String nextAppFolderName = getNextAvailableAppFolder(assetManager);
            File newAppFolder = new File(assetsDir, nextAppFolderName);
            if (!newAppFolder.exists()) {
                newAppFolder.mkdirs();
            }

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = zipEntry.getName();
                File outputFile = new File(newAppFolder, entryName);

                if (zipEntry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
            Toast.makeText(this, "Aplicativo importado para " + nextAppFolderName, Toast.LENGTH_SHORT).show();
            // Recarregar a lista de aplicativos após a importação
            appsList.clear();
            loadAppsWithCoversFromAssets();
            adapter.notifyDataSetChanged();

        } catch (IOException e) {
            Log.e("GameListActivity", "Erro ao importar arquivo zip: " + e.getMessage());
            Toast.makeText(this, "Erro ao importar arquivo zip.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getNextAvailableAppFolder(AssetManager assetManager) {
        int appNumber = 5;
        while (true) {
            String folderName = "App" + appNumber; // Correção: "App" maiúsculo
            try {
                String[] files = assetManager.list(ASSET_APPS_FOLDER + "/" + folderName);
                if (files == null) {
                    return folderName;
                }
                appNumber++;
            } catch (IOException e) {
                // A pasta não existe, então podemos usá-la
                return folderName;
            }
        }
    }

    private void loadAppsWithCoversFromAssets() {
        AssetManager assetManager = getAssets();
        try {
            String[] appFolders = assetManager.list(ASSET_APPS_FOLDER);
            if (appFolders != null) {
                appsList.clear(); // Limpar a lista antes de recarregar
                for (String folder : appFolders) {
                    try {
                        InputStream coverInputStream = null;
                        Bitmap coverBitmap = null;
                        String displayName = folder; // Nome da pasta como padrão

                        // Tentar ler o arquivo de configuração para obter o displayName (titulo)
                        InputStream configInputStream = null;
                        try {
                            configInputStream = assetManager.open(ASSET_APPS_FOLDER + "/" + folder + "/info.txt");
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
                            coverInputStream = assetManager.open(ASSET_APPS_FOLDER + "/" + folder + "/capa.jpg");
                            coverBitmap = BitmapFactory.decodeStream(coverInputStream);
                        } catch (IOException e) {
                            Log.w("GameListActivity", "Capa não encontrada para: " + folder);
                        } finally {
                            if (coverInputStream != null) {
                                coverInputStream.close();
                            }
                        }

                        String[] files = assetManager.list(ASSET_APPS_FOLDER + "/" + folder);
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