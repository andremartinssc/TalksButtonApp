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
import java.io.File; // Importação necessária
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections; // Usado para Collections.max, embora não explicitamente agora, bom ter.
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher; // Importação necessária
import java.util.regex.Pattern; // Importação necessária
import android.content.pm.ActivityInfo;
import android.content.ContentResolver;

// ---

class ViewHolder {
    ImageView coverImageView;
    TextView appNameTextView;
    Button openButton;
    Button deleteButton;
    Button applyButton;
}

// ---

public class GameListActivity extends AppCompatActivity {

    private static final String ASSET_APPS_FOLDER = "aplicacoes";
    // Torna esta constante pública para acesso de outras classes, como WebAppActivity
    public static final String IMPORTED_APPS_FOLDER = "aplicacoes_importadas";
    // Usado para identificar apps que não podem ser excluídos, App1 a App5 (se App5 estiver nos assets)
    private static final String[] FIXED_ASSET_APP_FOLDERS = {"App1", "App2", "App3", "App4", "App5"};
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
        String folderName; // Nome da pasta, que pode ser tanto de asset quanto de importado
        String displayName;
        Bitmap coverImage;
        boolean isImported; // Campo para indicar se é um app importado

        public AppData(String folderName, String displayName, Bitmap coverImage, boolean isImported) {
            this.folderName = folderName;
            this.displayName = displayName;
            this.coverImage = coverImage;
            this.isImported = isImported;
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

                final String appToOpenFolderName = currentApp.folderName;
                holder.openButton.setOnClickListener(v -> openWebApp(appToOpenFolderName, currentApp.isImported));

                // Lógica de exclusão: apenas apps importados podem ser excluídos, e apps padrão de assets não.
                boolean canDelete = currentApp.isImported && !Arrays.asList(FIXED_ASSET_APP_FOLDERS).contains(currentApp.folderName);
                if (canDelete) {
                    holder.deleteButton.setVisibility(View.VISIBLE);
                    holder.deleteButton.setOnClickListener(v -> deleteImportedApp(currentApp));
                } else {
                    holder.deleteButton.setVisibility(View.GONE);
                    holder.deleteButton.setOnClickListener(null); // Remove o listener para evitar cliques indesejados
                }

                final String appToApplyFolderName = currentApp.folderName;
                holder.applyButton.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), Select_Button.class);
                    intent.putExtra("app_folder", appToApplyFolderName);
                    intent.putExtra("is_app_imported", currentApp.isImported); // Passa o status de importado
                    getContext().startActivity(intent);
                });

                if (position == selectedPosition) {
                    convertView.setBackgroundColor(0x1A808080); // Cor para item selecionado
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
        loadAppsWithCovers(); // Chama o método unificado de carregamento
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
                            // Persiste permissões para a URI
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
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

            // No Android 13+, solicitar as permissões de mídia específicas
            boolean hasAllMediaPermissions = readImagesPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                    readVideoPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                    readAudioPermissionCheck == PackageManager.PERMISSION_GRANTED;

            if (!hasAllMediaPermissions) {
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
        } else { // Para APIs < 33, usar READ_EXTERNAL_STORAGE
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
        // Opcional: Adicionar categorias ou tipos MIME para filtrar, se aplicável
        // intent.addCategory(Intent.CATEGORY_DEFAULT);
        // intent.setType("image/*"); // Exemplo para buscar imagens, mas ACTION_OPEN_DOCUMENT_TREE busca diretórios
        directoryPickerLauncher.launch(intent);
    }

    private void handleImportedDirectory(Uri sourceTreeUri) {
        Log.d(TAG, "handleImportedDirectory chamado com URI: " + sourceTreeUri);

        File importedAppsDir = new File(getFilesDir(), IMPORTED_APPS_FOLDER);

        if (!importedAppsDir.exists()) {
            if (!importedAppsDir.mkdirs()) {
                Log.e(TAG, "Falha ao criar diretório de aplicativos importados: " + importedAppsDir.getAbsolutePath());
                mainHandler.post(() -> Toast.makeText(GameListActivity.this, "Erro ao criar diretório de destino para importação.", Toast.LENGTH_SHORT).show());
                return;
            }
        }

        // CHAVE DA MUDANÇA AQUI: Chamar o método correto para obter o próximo nome "AppX"
        String newAppFolderName = getNextAvailableAppFolderName(importedAppsDir);
        File destinationAppFolder = new File(importedAppsDir, newAppFolderName);

        if (!destinationAppFolder.exists()) {
            if (!destinationAppFolder.mkdirs()) {
                Log.e(TAG, "Falha ao criar diretório para o novo aplicativo importado: " + destinationAppFolder.getAbsolutePath());
                mainHandler.post(() -> Toast.makeText(GameListActivity.this, "Erro ao criar diretório para o novo aplicativo.", Toast.LENGTH_SHORT).show());
                return;
            }
        }

        Log.d(TAG, "Iniciando a cópia do diretório da URI: " + sourceTreeUri + " para: " + destinationAppFolder.getAbsolutePath());
        boolean success = false;
        try {
            success = copyDirectoryFromUri(sourceTreeUri, destinationAppFolder);
            Log.d(TAG, "copyDirectoryFromUri concluído com sucesso: " + success);
        } catch (IOException e) {
            Log.e(TAG, "Erro durante a cópia do diretório: " + e.getMessage(), e); // Log completo do erro
            final String errorMessage = "Erro ao importar diretório: " + e.getMessage();
            mainHandler.post(() -> Toast.makeText(GameListActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            return;
        }

        final String message = success ? "Conteúdo importado com sucesso para " + newAppFolderName : "Falha ao importar diretório.";
        Log.d(TAG, "Resultado da cópia: " + success + ", Mensagem: " + message);
        mainHandler.post(() -> {
            Toast.makeText(GameListActivity.this, message, Toast.LENGTH_SHORT).show();
            appsList.clear(); // Limpa a lista existente
            loadAppsWithCovers(); // Recarrega todos os apps (assets e importados)
            adapter.notifyDataSetChanged();
            updateSelection(); // Reajusta a seleção após recarregar
        });
    }

    private boolean copyDirectoryFromUri(Uri sourceTreeUri, File destDir) throws IOException {
        ContentResolver resolver = getContentResolver();
        // Certifique-se de que a URI passada para buildDocumentUriUsingTree seja a URI da árvore, não o ID do documento
        String treeId = DocumentsContract.getTreeDocumentId(sourceTreeUri);
        Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(sourceTreeUri, treeId); // Correção aqui

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
                        // Copiar o arquivo
                        if (!copyFileFromUri(resolver, childDocUri, destFile)) {
                            success = false;
                        }
                    }
                } while (cursor.moveToNext());
            } else {
                Log.w(TAG, "Nenhum filho encontrado para copiar em: " + parentDocUri.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao listar ou copiar arquivos: " + e.getMessage(), e);
            success = false;
        }
        return success;
    }

    private boolean copyFileFromUri(ContentResolver resolver, Uri sourceUri, File destFile) {
        try (InputStream in = resolver.openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destFile)) {
            if (in == null) {
                Log.e(TAG, "InputStream nulo para URI: " + sourceUri + ". Não foi possível abrir o arquivo de origem.");
                return false;
            }
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            Log.d(TAG, "Arquivo copiado com sucesso: " + destFile.getName());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao copiar arquivo de URI " + sourceUri + " para " + destFile.getAbsolutePath() + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gera o próximo nome de pasta disponível no formato "AppX" para apps importados.
     * Encontra o maior número X já utilizado entre aplicativos de assets e aplicativos importados,
     * e então retorna "App(X+1)".
     *
     * @param parentDir O diretório pai onde os aplicativos importados são salvos (getFilesDir()/aplicacoes_importadas).
     * @return O próximo nome de pasta disponível (ex: "App5", "App6", etc.).
     */
    private String getNextAvailableAppFolderName(File parentDir) {
        int maxAppNumber = 0;
        Pattern appNamePattern = Pattern.compile("^App(\\d+)$"); // Regex para "App" seguido de dígitos

        // 1. Coleta nomes de aplicativos de ASSETS e encontra o maior número
        AssetManager assetManager = getAssets();
        try {
            String[] assetFolders = assetManager.list(ASSET_APPS_FOLDER);
            if (assetFolders != null) {
                for (String folderName : assetFolders) {
                    Matcher matcher = appNamePattern.matcher(folderName);
                    if (matcher.matches()) {
                        try {
                            int num = Integer.parseInt(matcher.group(1));
                            if (num > maxAppNumber) {
                                maxAppNumber = num;
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Nome de pasta de asset inválido para numeração: " + folderName);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao listar assets para verificar nomes existentes.", e);
        }

        // 2. Coleta nomes de aplicativos IMPORTADOS e encontra o maior número
        if (parentDir.exists() && parentDir.isDirectory()) {
            File[] importedFolders = parentDir.listFiles();
            if (importedFolders != null) {
                for (File folder : importedFolders) {
                    if (folder.isDirectory()) {
                        Matcher matcher = appNamePattern.matcher(folder.getName());
                        if (matcher.matches()) {
                            try {
                                int num = Integer.parseInt(matcher.group(1));
                                if (num > maxAppNumber) {
                                    maxAppNumber = num;
                                }
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Nome de pasta importada inválido para numeração: " + folder.getName());
                            }
                        }
                    }
                }
            }
        }

        // O próximo número disponível é o maior número encontrado + 1
        int nextAppNumber = maxAppNumber + 1;
        String newFolderName = "App" + nextAppNumber;
        Log.d(TAG, "Próximo nome de pasta disponível: " + newFolderName + " (Baseado no maior App existente: App" + maxAppNumber + ")");
        return newFolderName;
    }


    private void loadAppsWithCovers() {
        Log.d(TAG, "loadAppsWithCovers chamado");
        appsList.clear(); // Limpa a lista existente antes de carregar tudo

        loadAppsFromAssets();
        loadAppsFromInternalStorage();

        // Opcional: ordenar a lista se desejar
        // Collections.sort(appsList, (a1, a2) -> a1.displayName.compareToIgnoreCase(a2.displayName));
    }

    private void loadAppsFromAssets() {
        AssetManager assetManager = getAssets();
        try {
            String[] appFolders = assetManager.list(ASSET_APPS_FOLDER);
            if (appFolders != null) {
                for (String folder : appFolders) {
                    // Ignora pastas internas do Android que não são nossos apps
                    if (folder.equals("webkit") || folder.startsWith("images")) continue;

                    try {
                        // Verifica se é realmente um diretório que contém um index.html
                        // Não é possível verificar diretamente se é um diretório nos assets sem tentar listar o conteúdo
                        // A tentativa de abrir info.txt ou capa.jpg já serve como um filtro.
                        // Assumimos que se tem index.html, info.txt ou capa.jpg, é um app.
                        boolean isAppFolder = false;
                        try (InputStream testStream = assetManager.open(ASSET_APPS_FOLDER + "/" + folder + "/index.html")) {
                            isAppFolder = true;
                        } catch (IOException e) { /* Não é um index.html, talvez seja outro arquivo */ }

                        if (!isAppFolder) { // Tenta com capa.jpg como alternativa
                            try (InputStream testStream = assetManager.open(ASSET_APPS_FOLDER + "/" + folder + "/capa.jpg")) {
                                isAppFolder = true;
                            } catch (IOException e) { /* Ainda não é um app */ }
                        }

                        if (!isAppFolder) {
                            Log.d(TAG, "Pulando pasta de asset não-app: " + folder);
                            continue; // Pula se não for um app válido
                        }

                        InputStream coverInputStream = null;
                        Bitmap coverBitmap = null;
                        String displayName = folder;

                        // Tenta ler info.txt para o nome de exibição
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
                            Log.w(TAG, "Arquivo info.txt não encontrado para asset: " + folder + ". Usando nome da pasta.");
                        } finally {
                            if (configInputStream != null) {
                                configInputStream.close();
                            }
                        }

                        // Tenta carregar a capa.jpg
                        try {
                            coverInputStream = assetManager.open(ASSET_APPS_FOLDER + "/" + folder + "/capa.jpg");
                            coverBitmap = BitmapFactory.decodeStream(coverInputStream);
                        } catch (IOException e) {
                            Log.w(TAG, "Capa não encontrada para asset: " + folder);
                        } finally {
                            if (coverInputStream != null) {
                                coverInputStream.close();
                            }
                        }

                        appsList.add(new AppData(folder, displayName, coverBitmap, false)); // false = não é importado
                        Log.d(TAG, "Aplicativo de Asset adicionado à lista: " + folder);

                    } catch (IOException e) {
                        Log.w(TAG, "Erro ao processar pasta de asset: " + folder + ". Pode não ser um diretório de aplicativo. Erro: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao listar assets", e);
        }
    }

    private void loadAppsFromInternalStorage() {
        File importedAppsDir = new File(getFilesDir(), IMPORTED_APPS_FOLDER);

        if (!importedAppsDir.exists() || !importedAppsDir.isDirectory()) {
            Log.d(TAG, "Diretório de aplicativos importados não existe ou não é um diretório.");
            return;
        }

        File[] appFolders = importedAppsDir.listFiles();
        if (appFolders != null) {
            for (File folder : appFolders) {
                if (folder.isDirectory()) {
                    // Verifica se a pasta contém pelo menos um index.html ou capa.jpg
                    File indexFile = new File(folder, "index.html");
                    File coverFile = new File(folder, "capa.jpg");

                    if (!indexFile.exists() && !coverFile.exists()) {
                        Log.d(TAG, "Pulando pasta importada que não parece ser um app válido (sem index.html ou capa.jpg): " + folder.getName());
                        continue;
                    }

                    try {
                        Bitmap coverBitmap = null;
                        String displayName = folder.getName(); // Começa com o nome da pasta

                        // Tenta ler info.txt para o nome de exibição
                        File infoFile = new File(folder, "info.txt");
                        if (infoFile.exists()) {
                            try (InputStream configInputStream = new java.io.FileInputStream(infoFile)) {
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
                                Log.w(TAG, "Erro ao ler info.txt para app importado: " + folder.getName(), e);
                            }
                        } else {
                            Log.w(TAG, "Arquivo info.txt não encontrado para app importado: " + folder.getName() + ". Usando nome da pasta.");
                        }

                        // Tenta carregar a capa.jpg
                        if (coverFile.exists()) {
                            coverBitmap = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
                        } else {
                            Log.w(TAG, "Capa não encontrada para app importado: " + folder.getName());
                        }

                        appsList.add(new AppData(folder.getName(), displayName, coverBitmap, true)); // true = é importado
                        Log.d(TAG, "Aplicativo importado adicionado à lista: " + folder.getName());
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar pasta de aplicativo importado: " + folder.getName(), e);
                    }
                }
            }
        }
    }

    private void openWebApp(String appFolderName, boolean isImported) {
        Log.d(TAG, "openWebApp chamado com appFolderName: " + appFolderName + ", isImported: " + isImported);
        Intent intent = new Intent(GameListActivity.this, WebAppActivity.class);
        intent.putExtra("app_folder_name", appFolderName);
        intent.putExtra("app_path_type", isImported ? "internal" : "asset"); // Passa o tipo de caminho
        startActivity(intent);
    }

    private void deleteImportedApp(AppData app) {
        if (!app.isImported) {
            Toast.makeText(this, "Não é possível excluir aplicativos pré-instalados.", Toast.LENGTH_SHORT).show();
            return;
        }

        File importedAppsDir = new File(getFilesDir(), IMPORTED_APPS_FOLDER);
        File appDirToDelete = new File(importedAppsDir, app.folderName);

        if (appDirToDelete.exists() && appDirToDelete.isDirectory()) {
            boolean deleted = deleteDirectory(appDirToDelete);
            if (deleted) {
                Toast.makeText(this, app.displayName + " excluído com sucesso.", Toast.LENGTH_SHORT).show();
                // Também remova de AppButtonPreferenceManager se este app estiver mapeado
                AppButtonPreferenceManager.removeAppMapping(this, app.folderName);

                appsList.clear();
                loadAppsWithCovers();
                adapter.notifyDataSetChanged();
                selectedPosition = 0; // Resetar seleção após exclusão
                updateSelection();

            } else {
                Toast.makeText(this, "Falha ao excluir " + app.displayName, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Falha ao excluir diretório (permissão ou arquivos abertos?): " + appDirToDelete.getAbsolutePath());
            }
        } else {
            Toast.makeText(this, "Diretório do aplicativo não encontrado para exclusão.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Diretório não existe ou não é um diretório para exclusão: " + appDirToDelete.getAbsolutePath());
        }
    }

    private boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDirectory(new File(dir, child));
                    if (!success) {
                        Log.e(TAG, "Falha ao excluir item filho: " + child + " de " + dir.getAbsolutePath());
                        return false;
                    }
                }
            }
        }
        boolean deleted = dir.delete();
        if (!deleted) {
            Log.e(TAG, "Falha final ao excluir diretório: " + dir.getAbsolutePath());
        }
        return deleted;
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
        if (!appsList.isEmpty()) {
            selectedPosition = (selectedPosition + 1) % appsList.size();
            updateSelection();
        }
    }

    private void selectPreviousApp() {
        Log.d(TAG, "selectPreviousApp chamado");
        if (!appsList.isEmpty()) {
            selectedPosition = (selectedPosition - 1 + appsList.size()) % appsList.size();
            updateSelection();
        }
    }

    private void openSelectedApp() {
        Log.d(TAG, "openSelectedApp chamado");
        if (!appsList.isEmpty()) {
            AppData selectedApp = appsList.get(selectedPosition);
            openWebApp(selectedApp.folderName, selectedApp.isImported);
        }
    }

    private void updateSelection() {
        Log.d(TAG, "updateSelection chamado");
        adapter.notifyDataSetChanged();
        if (!appsList.isEmpty()) {
            // Rola para a posição selecionada, com algum offset se necessário
            listViewGames.smoothScrollToPosition(selectedPosition);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart chamado");
        IntentFilter dataFilter = new IntentFilter("bluetooth_data_received");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDataReceiver, dataFilter);
        // Recarrega a lista ao retornar para garantir que as capas e apps estão atualizados
        appsList.clear();
        loadAppsWithCovers();
        adapter.notifyDataSetChanged();
        updateSelection();
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