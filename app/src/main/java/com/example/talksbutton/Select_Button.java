package com.example.talksbutton;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Select_Button extends AppCompatActivity {

    private String appFolderToApply;
    private boolean isAppImported; // Novo campo para saber se o app é importado
    private ImageView bt1Dialog, bt2Dialog, bt3Dialog, bt4Dialog;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_button);
        context = this;

        // Recupera o caminho da pasta do aplicativo e seu status de importado
        appFolderToApply = getIntent().getStringExtra("app_folder");
        isAppImported = getIntent().getBooleanExtra("is_app_imported", false); // Default para false

        if (appFolderToApply == null) {
            finish(); // Se não houver app para aplicar, fecha a tela
            return;
        }

        bt1Dialog = findViewById(R.id.bt_1);
        bt2Dialog = findViewById(R.id.bt_2);
        bt3Dialog = findViewById(R.id.bt_3);
        bt4Dialog = findViewById(R.id.bt_4);


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

    private void applyAppToButton(String buttonKey) {
        String appPathType = isAppImported ? "internal" : "asset";

        // Salva a associação do aplicativo ao botão, incluindo o tipo de caminho
        AppButtonPreferenceManager.saveAppForButton(context, buttonKey, appFolderToApply, appPathType);
        Toast.makeText(context, appFolderToApply + " aplicado ao " + getButtonName(buttonKey), Toast.LENGTH_SHORT).show();

        // Envia um broadcast para a MainActivity para notificar a atualização
        Intent intent = new Intent("APP_BUTTON_MAPPING_CHANGED");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Inicia a MainActivity e limpa a pilha de atividades
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish(); // Finaliza a Select_Button
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