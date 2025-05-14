package com.example.talksbutton;

import android.content.Context;
import android.content.SharedPreferences;

public class AppButtonPreferenceManager {

    private static final String PREF_NAME = "AppButtonPrefs";
    public static final String KEY_APP_BT1 = "app_bt1";
    public static final String KEY_APP_BT2 = "app_bt2";
    public static final String KEY_APP_BT3 = "app_bt3";
    public static final String KEY_APP_BT4 = "app_bt4";

    /**
     * Salva o nome da pasta do aplicativo associado a um botão виртуальный.
     *
     * @param context Contexto da aplicação.
     * @param key     Chave do botão (KEY_APP_BT1, KEY_APP_BT2, etc.).
     * @param appFolder Nome da pasta do aplicativo.
     */
    public static void saveAppForButton(Context context, String key, String appFolder) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, appFolder);
        editor.apply();
    }

    /**
     * Recupera o nome da pasta do aplicativo associado a um botão виртуальный.
     *
     * @param context Contexto da aplicação.
     * @param key     Chave do botão (KEY_APP_BT1, KEY_APP_BT2, etc.).
     * @param defaultFolder Valor padrão a ser retornado caso não haja nada salvo.
     * @return Nome da pasta do aplicativo ou o valor padrão.
     */
    public static String getAppForButton(Context context, String key, String defaultFolder) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultFolder);
    }
}

