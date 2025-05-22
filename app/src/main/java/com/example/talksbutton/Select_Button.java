package com.example.talksbutton;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Select_Button extends AppCompatActivity {

    private String appFolderToApply;
    private boolean isAppImported;
    private ImageView bt1Dialog, bt2Dialog, bt3Dialog, bt4Dialog;
    private Context context;

    // Use o nome do arquivo com a capitalização correta
    private static final String CAPA_FILE_NAME_ASSET = "capa.jpg";
    private static final String CAPA_FILE_NAME_IMPORTED = "capa.JPG"; // Note o .JPG maiúsculo
    private static final String TAG = "Select_Button"; // Adiciona uma TAG para logs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_button);
        context = this;

        appFolderToApply = getIntent().getStringExtra("app_folder");
        isAppImported = getIntent().getBooleanExtra("is_app_imported", false);

        if (appFolderToApply == null) {
            finish();
            return;
        }

        bt1Dialog = findViewById(R.id.bt_1);
        bt2Dialog = findViewById(R.id.bt_2);
        bt3Dialog = findViewById(R.id.bt_3);
        bt4Dialog = findViewById(R.id.bt_4);

        // Carrega as capas para os ImageViews do diálogo
        loadButtonCovers();

        bt1Dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAppToButton(AppButtonPreferenceManager.KEY_APP_BT1);
            }
        });
        bt2Dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAppToButton(AppButtonPreferenceManager.KEY_APP_BT2);
            }
        });
        bt3Dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAppToButton(AppButtonPreferenceManager.KEY_APP_BT3);
            }
        });
        bt4Dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAppToButton(AppButtonPreferenceManager.KEY_APP_BT4);
            }
        });
    }

    // --- Novo método para carregar as capas dos botões ---
    private void loadButtonCovers() {
        // Carrega a capa para o Botão 1
        String app1Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT1, "App1");
        String app1Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT1_TYPE, "asset");
        loadCapaImage(app1Folder, app1Type, bt1Dialog);

        // Carrega a capa para o Botão 2
        String app2Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT2, "App2");
        String app2Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT2_TYPE, "asset");
        loadCapaImage(app2Folder, app2Type, bt2Dialog);

        // Carrega a capa para o Botão 3
        String app3Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT3, "App3");
        String app3Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT3_TYPE, "asset");
        loadCapaImage(app3Folder, app3Type, bt3Dialog);

        // Carrega a capa para o Botão 4
        String app4Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT4, "App4");
        String app4Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT4_TYPE, "asset");
        loadCapaImage(app4Folder, app4Type, bt4Dialog);
    }

    // --- Lógica de carregamento de capa (replicada/adaptada da MainActivity) ---
    private void loadCapaImage(String appFolderName, String appPathType, ImageView imageView) {
        Bitmap bitmap = null;
        if ("internal".equals(appPathType)) {
            File appDir = new File(getFilesDir(), GameListActivity.IMPORTED_APPS_FOLDER + File.separator + appFolderName);
            File coverFile = new File(appDir, CAPA_FILE_NAME_IMPORTED);
            if (coverFile.exists()) {
                try {
                    bitmap = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
                    if (bitmap == null) {
                        Log.e(TAG, "BitmapFactory retornou null para capa importada: " + coverFile.getAbsolutePath() + ". O arquivo pode estar corrompido ou não é uma imagem válida.");
                    } else {
                        Log.d(TAG, "Capa importada carregada com sucesso: " + coverFile.getAbsolutePath());
                    }
                } catch (OutOfMemoryError oome) {
                    Log.e(TAG, "OutOfMemoryError ao carregar capa importada para " + appFolderName + ": " + oome.getMessage() + ". Tente reduzir o tamanho da imagem.");
                } catch (Exception e) {
                    Log.e(TAG, "Erro inesperado ao carregar capa importada para " + appFolderName + ": " + e.getMessage(), e);
                }
            } else {
                Log.w(TAG, "Capa não encontrada em armazenamento interno para " + appFolderName + " no caminho: " + coverFile.getAbsolutePath());
            }
        } else { // Assume "asset"
            AssetManager am = getAssets();
            InputStream is = null;
            try {
                String imagePath = "aplicacoes/" + appFolderName + "/" + CAPA_FILE_NAME_ASSET;
                is = am.open(imagePath);
                bitmap = BitmapFactory.decodeStream(is);
                if (bitmap == null) {
                    Log.e(TAG, "BitmapFactory retornou null para capa de asset: " + imagePath + ". O arquivo pode estar corrompido ou não é uma imagem válida.");
                } else {
                    Log.d(TAG, "Capa de asset carregada com sucesso: " + imagePath);
                }
            } catch (IOException e) {
                Log.w(TAG, "Imagem de capa não encontrada em assets para " + appFolderName + ": " + e.getMessage());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery); // Imagem padrão
        }
    }

    private void applyAppToButton(String buttonKey) {
        String appPathType = isAppImported ? "internal" : "asset";

        AppButtonPreferenceManager.saveAppForButton(context, buttonKey, appFolderToApply, appPathType);
        Toast.makeText(context, appFolderToApply + " aplicado ao " + getButtonName(buttonKey), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent("APP_BUTTON_MAPPING_CHANGED");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }

    private String getButtonName(String key) {
        if (key.equals(AppButtonPreferenceManager.KEY_APP_BT1))
            return "Botão 1";
        if (key.equals(AppButtonPreferenceManager.KEY_APP_BT2))
            return "Botão 2";
        if (key.equals(AppButtonPreferenceManager.KEY_APP_BT3))
            return "Botão 3";
        if (key.equals(AppButtonPreferenceManager.KEY_APP_BT4))
            return "Botão 4";
        return "";
    }
}