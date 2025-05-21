package com.example.talksbutton;

import android.content.Context;
import android.content.SharedPreferences;

public class AppButtonPreferenceManager {

    private static final String PREF_NAME = "AppButtonPrefs";
    public static final String KEY_APP_BT1 = "app_bt1";
    public static final String KEY_APP_BT2 = "app_bt2";
    public static final String KEY_APP_BT3 = "app_bt3";
    public static final String KEY_APP_BT4 = "app_bt4";

    // Chaves para armazenar o tipo de caminho (asset ou internal) para cada botão
    public static final String KEY_APP_BT1_TYPE = "app_bt1_type";
    public static final String KEY_APP_BT2_TYPE = "app_bt2_type";
    public static final String KEY_APP_BT3_TYPE = "app_bt3_type";
    public static final String KEY_APP_BT4_TYPE = "app_bt4_type";

    /**
     * Salva o nome da pasta do aplicativo e seu tipo (asset/internal) associado a um botão.
     *
     * @param context Contexto da aplicação.
     * @param key     Chave do botão (KEY_APP_BT1, KEY_APP_BT2, etc.).
     * @param appFolderName Nome da pasta do aplicativo.
     * @param appPathType Tipo de caminho do aplicativo ("asset" ou "internal").
     */
    public static void saveAppForButton(Context context, String key, String appFolderName, String appPathType) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, appFolderName);
        editor.putString(key + "_type", appPathType); // Salva o tipo também
        editor.apply();
    }

    /**
     * Recupera o nome da pasta do aplicativo associado a um botão.
     *
     * @param context Contexto da aplicação.
     * @param key     Chave do botão (KEY_APP_BT1, KEY_APP_BT2, etc.).
     * @param defaultFolder Valor padrão a ser retornado caso não haja nada salvo para a pasta.
     * @return Nome da pasta do aplicativo ou o valor padrão.
     */
    public static String getAppForButton(Context context, String key, String defaultFolder) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultFolder);
    }

    /**
     * Recupera o tipo de caminho (asset/internal) do aplicativo associado a um botão.
     *
     * @param context Contexto da aplicação.
     * @param key Chave do botão (KEY_APP_BT1_TYPE, KEY_APP_BT2_TYPE, etc.).
     * @param defaultType Valor padrão a ser retornado caso não haja nada salvo ("asset").
     * @return Tipo de caminho do aplicativo ("asset" ou "internal").
     */
    public static String getAppButtonType(Context context, String key, String defaultType) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultType);
    }

    /**
     * Remove o mapeamento de um aplicativo específico de qualquer botão.
     * Isso é útil quando um aplicativo importado é excluído.
     *
     * @param context Contexto da aplicação.
     * @param appFolderName O nome da pasta do aplicativo a ser removido.
     */
    public static void removeAppMapping(Context context, String appFolderName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String[] keys = {KEY_APP_BT1, KEY_APP_BT2, KEY_APP_BT3, KEY_APP_BT4};
        String[] typeKeys = {KEY_APP_BT1_TYPE, KEY_APP_BT2_TYPE, KEY_APP_BT3_TYPE, KEY_APP_BT4_TYPE};
        String[] defaultApps = {"App1", "App2", "App3", "App4"}; // Default apps for each button

        for (int i = 0; i < keys.length; i++) {
            String currentMappedApp = prefs.getString(keys[i], defaultApps[i]);
            if (currentMappedApp.equals(appFolderName)) {
                // Se o app excluído estiver mapeado para este botão, redefine para o padrão
                editor.putString(keys[i], defaultApps[i]);
                editor.putString(typeKeys[i], "asset"); // O padrão é sempre um asset
                break; // Apenas um botão pode estar mapeado para o mesmo app, ou ajuste para limpar todos
            }
        }
        editor.apply();
    }
}