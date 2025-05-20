package com.example.talksbutton;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
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
import android.content.pm.ActivityInfo;
import android.content.ContentResolver;

class ViewHolder {
    ImageView coverImageView;
    TextView appNameTextView;
    Button openButton;
    Button deleteButton;
    Button applyButton;
}

public class GameListActivity extends AppCompatActivity {

    private static final String ASSET_APPS_FOLDER = "aplicacoes";
    private static final String[] FIXED_APP_FOLDERS = {"App1", "App2", "App3", "App4"};
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final String TAG = "GameListActivity";

    private ListView listViewGames;
    private List<AppData> appsList;
    private int selectedPosition = 0;
    private AppListAdapter adapter;
    private Button backToMainButton;
    private Button importButton;
    private ActivityResultLauncher<Intent> directoryPickerLauncher;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
                holder.openButton.setOnClickListener(v -> openWebApp(appToOpen));

                final String appToDelete = currentApp.folderName;
                holder.deleteButton.setOnClickListener(v -> {
                    if (!isFixedApp(appToDelete)) {
                        Toast.makeText(getContext(), "Excluir " + appToDelete, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Este aplicativo não pode ser excluído", Toast.LENGTH_SHORT).show();
                    }
                });

                final String appToApply = currentApp.folderName;
                holder.applyButton.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), Select_Button.class);
                    intent.putExtra("app_folder", appToApply);
                    getContext().startActivity(intent);
                });

                if (position == selectedPosition) {
                    convertView.setBackgroundColor(0x1A808080);
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

        // Define a orientação da tela como paisagem
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Log.d(TAG, "onCreate chamado");

        backToMainButton = findViewById(R.id.btn_back_to_main);
        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(GameListActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        importButton = findViewById(R.id.btn_import);
        importButton.setOnClickListener(v -> checkMediaPermissionAndImport());

        listViewGames = findViewById(R.id.list_view_games);
        appsList = new ArrayList<>();
        loadAppsWithCoversFromAssets();
        adapter = new AppListAdapter(this, appsList);
        listViewGames.setAdapter(adapter);
        updateSelection();

        // Inicializa o ActivityResultLauncher para seleção de diretório
        directoryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null) {
                            handleImportedDirectory(treeUri);
                        } else {
                            Toast.makeText(this, "Erro ao selecionar o diretório.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "URI do diretório selecionado é nula.");
                        }
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Log.d(TAG, "Seleção de diretório cancelada pelo usuário.");
                        Toast.makeText(this, "Seleção de diretório cancelada.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erro ao selecionar o diretório.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erro desconhecido ao selecionar diretório. Result Code: " + result.getResultCode());
                    }
                });
    }

    private void checkMediaPermissionAndImport() {
        Log.d(TAG, "checkMediaPermissionAndImport chamado");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int readImagesPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES);
            int readVideoPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO);
            int readAudioPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO);

            if (readImagesPermissionCheck != PackageManager.PERMISSION_GRANTED ||
                    readVideoPermissionCheck != PackageManager.PERMISSION_GRANTED ||
                    readAudioPermissionCheck != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "Permissões de mídia não concedidas. Solicitando...");
                List<String> permissionsToRequest = new ArrayList<>();
                if (readImagesPermissionCheck != PackageManager.PERMISSION_GRANTED)
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
                if (readVideoPermissionCheck != PackageManager.PERMISSION_GRANTED)
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
                if (readAudioPermissionCheck != PackageManager.PERMISSION_GRANTED)
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);

                ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_STORAGE_PERMISSION);
            } else {
                Log.d(TAG, "Permissões de mídia já concedidas.");
                openDirectoryPicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão de armazenamento já concedida.");
                openDirectoryPicker();
            } else {
                Log.d(TAG, "Permissão de armazenamento não concedida. Solicitando...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult chamado com requestCode: " + requestCode);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults.length == permissions.length) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Log.i(TAG, "Todas as permissões concedidas.");
                    openDirectoryPicker();
                } else {
                    Log.w(TAG, "Pelo menos uma permissão negada.");
                    Toast.makeText(this, "Permissão negada. O aplicativo precisa desta permissão para importar novos conteúdos.", Toast.LENGTH_LONG).show();
                    // Opcional: Mostrar um diálogo explicando a necessidade da permissão e oferecer para tentar novamente
                }
            } else {
                Log.d(TAG, "grantResults está vazio ou o tamanho não corresponde.");
                Toast.makeText(this, "Permissão negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openDirectoryPicker() {
        Log.d(TAG, "openDirectoryPicker chamado");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        directoryPickerLauncher.launch(intent);
    }

    private void handleImportedDirectory(Uri sourceTreeUri) {
        Log.d(TAG, "handleImportedDirectory chamado com URI: " + sourceTreeUri);
        File assetsDir = new File(getFilesDir().getParentFile(), "assets/" + ASSET_APPS_FOLDER);
        File destinationAppFolder = new File(assetsDir, "App6"); // Diretório de destino fixo

        Log.d(TAG, "Diretório de assets de destino (interno): " + assetsDir.getAbsolutePath());
        Log.d(TAG, "Diretório de destino fixo para importação: " + destinationAppFolder.getAbsolutePath());

        if (!destinationAppFolder.exists()) {
            Log.d(TAG, "Diretório de destino fixo não existe, criando...");
            if (destinationAppFolder.mkdirs()) {
                Log.d(TAG, "Diretório de destino fixo criado com sucesso: " + destinationAppFolder.getAbsolutePath());
            } else {
                Log.e(TAG, "Falha ao criar diretório de destino fixo: " + destinationAppFolder.getAbsolutePath());
                mainHandler.post(() -> Toast.makeText(GameListActivity.this, "Erro ao criar diretório de destino.", Toast.LENGTH_SHORT).show());
                return;
            }
        }

        Log.d(TAG, "Iniciando a cópia do diretório da URI: " + sourceTreeUri + " para: " + destinationAppFolder.getAbsolutePath());
        boolean success = false;
        try {
            success = copyDirectoryFromUri(sourceTreeUri, destinationAppFolder);
            Log.d(TAG, "copyDirectoryFromUri concluído com sucesso: " + success);
        } catch (IOException e) {
            Log.e(TAG, "Erro durante a cópia do diretório: " + e.getMessage());
            final String errorMessage = "Erro ao importar diretório: " + e.getMessage();
            mainHandler.post(() -> Toast.makeText(GameListActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            return;
        }

        final String message = success ? "Conteúdo importado para App6" : "Falha ao importar diretório.";
        Log.d(TAG, "Resultado da cópia: " + success + ", Mensagem: " + message);
        mainHandler.post(() -> {
            Toast.makeText(GameListActivity.this, message, Toast.LENGTH_SHORT).show();
            appsList.clear();
            loadAppsWithCoversFromAssets();
            adapter.notifyDataSetChanged();
            });

    }

    private boolean copyDirectoryFromUri(Uri sourceTreeUri, File destDir) throws IOException {
        ContentResolver resolver = getContentResolver();
        Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(sourceTreeUri, DocumentsContract.getTreeDocumentId(sourceTreeUri));
        return copyChildren(resolver, rootDocUri, destDir);
    }

    private boolean copyChildren(ContentResolver resolver, Uri parentDocUri, File destDir) throws IOException {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentDocUri, DocumentsContract.getDocumentId(parentDocUri));
        boolean success = true;
        try (Cursor cursor = resolver.query(childrenUri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String documentId = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                    String displayName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                    String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE));
                    Uri childDocUri = DocumentsContract.buildDocumentUriUsingTree(parentDocUri, documentId);
                    File destFile = new File(destDir, displayName);

                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        if (!destFile.exists() && !destFile.mkdirs()) {
                            Log.e(TAG, "Falha ao criar subdiretório: " + destFile.getAbsolutePath());
                            success = false;
                        }
                        if (!copyChildren(resolver, childDocUri, destFile)) {
                            success = false;
                        }
                    } else {
                        if (!copyFileFromUri(resolver, childDocUri, destFile)) {
                            success = false;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao listar ou copiar arquivos: " + e.getMessage());
            success = false;
        }
        return success;
    }

    private boolean copyFileFromUri(ContentResolver resolver, Uri sourceUri, File destFile) {
        try (InputStream in = resolver.openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destFile)) {
            if (in == null) return false;
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao copiar arquivo de URI " + sourceUri + " para " + destFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }

    private String getNextAvailableAppFolder(AssetManager assetManager) {
        int appNumber = 5;
        while (true) {
            String folderName = "App" + appNumber;
            try {
                String[] files = assetManager.list(ASSET_APPS_FOLDER + "/" + folderName);
                if (files == null) {
                    Log.d(TAG, "Pasta disponível encontrada: " + folderName);
                    return folderName;
                }
                appNumber++;
            } catch (IOException e) {
                Log.d(TAG, "Pasta disponível encontrada (IOException): " + folderName);
                return folderName;
            }
        }
    }

    private void loadAppsWithCoversFromAssets() {
        Log.d(TAG, "loadAppsWithCoversFromAssets chamado");
        AssetManager assetManager = getAssets();
        try {
            String[] appFolders = assetManager.list(ASSET_APPS_FOLDER);
            if (appFolders != null) {
                appsList.clear();
                for (String folder : appFolders) {
                    try {
                        InputStream coverInputStream = null;
                        Bitmap coverBitmap = null;
                        String displayName = folder;

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
                            Log.w(TAG, "Arquivo info.txt não encontrado para: " + folder + ". Usando nome da pasta.");
                        } finally {
                            if (configInputStream != null) {
                                configInputStream.close();
                            }
                        }

                        try {
                            coverInputStream = assetManager.open(ASSET_APPS_FOLDER + "/" + folder + "/capa.jpg");
                            coverBitmap = BitmapFactory.decodeStream(coverInputStream);
                        } catch (IOException e) {
                            Log.w(TAG, "Capa não encontrada para: " + folder);
                        } finally {
                            if (coverInputStream != null) {
                                coverInputStream.close();
                            }
                        }

                        String[] files = assetManager.list(ASSET_APPS_FOLDER + "/" + folder);
                        if (files != null && files.length > 0) {
                            appsList.add(new AppData(folder, displayName, coverBitmap));
                            Log.d(TAG, "Aplicativo adicionado à lista: " + folder);
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "Erro ao processar pasta: " + folder + ". Não é um diretório?", e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao listar assets", e);
        }
    }

    private void openWebApp(String appName) {
        Log.d(TAG, "openWebApp chamado com appName: " + appName);
        Intent intent = new Intent(GameListActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);
        startActivity(intent);
    }

    private void handleBluetoothData(String data) {
        runOnUiThread(() -> {
            Log.d(TAG, "handleBluetoothData chamado com data: " + data);
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
                    break;
                default:
                    Log.d(TAG, "Comando Bluetooth desconhecido: " + data);
            }
        });
    }

    private void selectNextApp() {
        Log.d(TAG, "selectNextApp chamado");
        selectedPosition = (selectedPosition + 1) % appsList.size();
        updateSelection();
    }

    private void selectPreviousApp() {
        Log.d(TAG, "selectPreviousApp chamado");
        selectedPosition = (selectedPosition - 1 + appsList.size()) % appsList.size();
        updateSelection();
    }

    private void openSelectedApp() {
        Log.d(TAG, "openSelectedApp chamado");
        openWebApp(appsList.get(selectedPosition).folderName);
    }

    private void updateSelection() {
        Log.d(TAG, "updateSelection chamado");
        adapter.notifyDataSetChanged();
        listViewGames.setSelection(selectedPosition);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart chamado");
        IntentFilter dataFilter = new IntentFilter("bluetooth_data_received");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDataReceiver, dataFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop chamado");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothDataReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}