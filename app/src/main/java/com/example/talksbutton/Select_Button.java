package com.example.talksbutton;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.talksbutton.AppButtonPreferenceManager;

public class Select_Button extends AppCompatActivity {

    private String appFolderToApply;
    private ImageView bt1Dialog, bt2Dialog, bt3Dialog, bt4Dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_button);

        // Recupera o caminho da pasta do aplicativo passado pela GameListActivity
        appFolderToApply = getIntent().getStringExtra("app_folder");
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
        // Salva a associação do aplicativo ao botão виртуальный
        AppButtonPreferenceManager.saveAppForButton(this, buttonKey, appFolderToApply);
        Toast.makeText(this, appFolderToApply + " aplicado ao " + getButtonName(buttonKey), Toast.LENGTH_SHORT).show();
        finish(); // Retorna para a MainActivity
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
