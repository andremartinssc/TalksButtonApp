package com.example.talksbutton;

import java.io.BufferedReader;
import java.io.File;
import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import android.speech.tts.TextToSpeech;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_BT_PERMISSIONS = 1;
    private static final int ANIMATION_DURATION_SCALE_DOWN = 150;
    private static final int ANIMATION_DURATION_SCALE_UP = 300;
    private static final int ANIMATION_DURATION_ROTATE = 250;
    private static final int ANIMATION_DURATION_ALPHA = 150;
    private static final int TRANSITION_DELAY_MS = 300;

    private ImageView bt1, bt2, bt3, bt4, btLista;
    private BluetoothService mService;
    private boolean mBound = false;
    private boolean isConnected = false;
    private AtomicBoolean isAnimationRunning = new AtomicBoolean(false);
    private LedController ledController;
    private Context context;
    private TextToSpeech tts;

    private static final String CAPA_FILE_NAME_ASSET = "capa.jpg";
    private static final String CAPA_FILE_NAME_IMPORTED = "capa.JPG";
    private static final String INFO_FILE_NAME = "info.txt";

    private final BroadcastReceiver appButtonChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("APP_BUTTON_MAPPING_CHANGED".equals(intent.getAction())) {
                Log.d("MainActivity", "Recebido broadcast de atualização dos botões.");
                updateButtonCovers();
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d("MainActivity", "Serviço Bluetooth conectado.");
            isConnected = mService.isConnected();
            ledController = new LedController(mService, mBound);
            if (isConnected) {
                Toast.makeText(MainActivity.this, "Dispositivo Talks Button conectado", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
            ledController = null;
            isConnected = false;
            Log.d("MainActivity", "Serviço Bluetooth desconectado.");
            Toast.makeText(MainActivity.this, "Dispositivo Talks Button desconectado", Toast.LENGTH_SHORT).show();
        }
    };

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_data_received".equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                handleBluetoothData(data);
            }
        }
    };

    private final BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("bluetooth_connection_state".equals(intent.getAction())) {
                boolean newConnectionState = intent.getBooleanExtra("is_connected", false);
                if (newConnectionState != isConnected) {
                    isConnected = newConnectionState;
                    String message = isConnected ? "Dispositivo Talks Button conectado" : "Dispositivo Talks Button desconectado";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        bt1 = findViewById(R.id.bt_1);
        bt2 = findViewById(R.id.bt_2);
        bt3 = findViewById(R.id.bt_3);
        bt4 = findViewById(R.id.bt_4);
        btLista = findViewById(R.id.bt_lista);

        updateButtonCovers();

        bt1.setOnClickListener(v -> handleButtonClick(v, AppButtonPreferenceManager.KEY_APP_BT1));
        bt2.setOnClickListener(v -> handleButtonClick(v, AppButtonPreferenceManager.KEY_APP_BT2));
        bt3.setOnClickListener(v -> handleButtonClick(v, AppButtonPreferenceManager.KEY_APP_BT3));
        bt4.setOnClickListener(v -> handleButtonClick(v, AppButtonPreferenceManager.KEY_APP_BT4));
        btLista.setOnClickListener(v -> handleButtonClick(v, "lista"));

        tts = new TextToSpeech(this, this);

        if (!hasBluetoothPermissions()) {
            requestPermissions();
        } else {
            startBluetoothService();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("pt", "BR"));

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Idioma (pt-BR) não suportado ou dados ausentes. Tentando idioma padrão.");
                result = tts.setLanguage(Locale.getDefault());

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Idioma padrão também não suportado ou dados ausentes.");
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                } else {
                    Log.d("TTS", "TextToSpeech inicializado com sucesso no idioma padrão.");
                }
            } else {
                Log.d("TTS", "TextToSpeech inicializado com sucesso em pt-BR.");
            }
        } else {
            Log.e("TTS", "Falha na inicialização do TextToSpeech.");
        }
    }

    private void loadCapaImage(String appFolderName, String appPathType, ImageView imageView) {
        Bitmap bitmap = null;
        InputStream is = null;
        File coverFile = null;

        try {
            if ("internal".equals(appPathType)) {
                File appDir = new File(getFilesDir(), GameListActivity.IMPORTED_APPS_FOLDER + File.separator + appFolderName);
                coverFile = new File(appDir, CAPA_FILE_NAME_IMPORTED);
                if (coverFile.exists()) {
                    bitmap = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
                    if (bitmap == null) {
                        Log.e("MainActivity", "BitmapFactory retornou null para capa importada: " + coverFile.getAbsolutePath());
                    } else {
                        Log.d("MainActivity", "Capa importada carregada: " + coverFile.getAbsolutePath());
                    }
                } else {
                    Log.w("MainActivity", "Capa não encontrada em armazenamento interno: " + coverFile.getAbsolutePath());
                }

            } else {
                AssetManager am = getAssets();
                String imagePath = "aplicacoes/" + appFolderName + "/" + CAPA_FILE_NAME_ASSET;
                try {
                    is = am.open(imagePath);
                    bitmap = BitmapFactory.decodeStream(is);
                    if (bitmap == null) {
                        Log.e("MainActivity", "BitmapFactory retornou null para capa de asset: " + imagePath);
                    } else {
                        Log.d("MainActivity", "Capa de asset carregada: " + imagePath);
                    }
                } catch (IOException e) {
                    Log.w("MainActivity", "Imagem de capa não encontrada em assets para " + appFolderName + ": " + e.getMessage());
                }
            }
        } catch (OutOfMemoryError oome) {
            Log.e("MainActivity", "OutOfMemoryError ao carregar capa para " + appFolderName + ": " + oome.getMessage());
        } catch (Exception e) {
            Log.e("MainActivity", "Erro inesperado ao carregar capa para " + appFolderName + ": " + e.getMessage(), e);
        } finally {
            if (is != null) { try { is.close(); } catch (IOException e) { e.printStackTrace(); }}
        }

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private String readAppNameFromInfoTxt(InputStream is, String fallbackName) {
        String appName = fallbackName;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("titulo:")) {
                    appName = line.substring("titulo:".length()).trim();
                    Log.d("MainActivity", "Nome lido do info.txt: " + appName);
                    break;
                }
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Erro ao ler info.txt: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return appName != null && !appName.isEmpty() ? appName : fallbackName;
    }

    private void startBluetoothService() {
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void handleBluetoothData(String data) {
        runOnUiThread(() -> {
            Log.d("MainActivity", "Dados Bluetooth recebidos: " + data);
            switch (data.trim()) {
                case "B1":
                    bt1.performClick();
                    break;
                case "B2":
                    bt2.performClick();
                    break;
                case "B3":
                    bt3.performClick();
                    break;
                case "B4":
                    bt4.performClick();
                    break;
                case "B5":
                    btLista.performClick();
                    break;
                default:
                    Log.w("MainActivity", "Dados Bluetooth desconhecidos: " + data);
                    break;
            }
        });
    }

    private void openWebApp(String appFolderName, String appPathType) {
        Intent intent = new Intent(MainActivity.this, WebAppActivity.class);
        intent.putExtra("app_folder_name", appFolderName);
        intent.putExtra("app_path_type", appPathType);
        startActivityForResult(intent, 100);
    }

    private void openGameList() {
        Intent intent = new Intent(MainActivity.this, GameListActivity.class);
        startActivity(intent);
    }

    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS && hasBluetoothPermissions()) {
            startBluetoothService();
        } else {
            Toast.makeText(this, "Permissões Bluetooth necessárias para o funcionamento.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        }, REQUEST_BT_PERMISSIONS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter dataFilter = new IntentFilter("bluetooth_data_received");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDataReceiver, dataFilter);
        IntentFilter connectionFilter = new IntentFilter("bluetooth_connection_state");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothConnectionReceiver, connectionFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(appButtonChangedReceiver, new IntentFilter("APP_BUTTON_MAPPING_CHANGED"));
        updateButtonCovers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothDataReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothConnectionReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(appButtonChangedReceiver);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
    }

    private void handleButtonClick(View view, String buttonPrefKey) {
        if (isAnimationRunning.compareAndSet(false, true)) {
            String appFolderName = AppButtonPreferenceManager.getAppForButton(context, buttonPrefKey, getDefaultAppName(buttonPrefKey));
            String appPathType = AppButtonPreferenceManager.getAppButtonType(context, buttonPrefKey + "_type", "asset");

            animateButtonClickAndOpen(view, buttonPrefKey, appFolderName, appPathType);

            if (ledController != null && mService != null && mService.isConnected()) {
                int ledNumber = 0;
                if (AppButtonPreferenceManager.KEY_APP_BT1.equals(buttonPrefKey)) ledNumber = 1;
                else if (AppButtonPreferenceManager.KEY_APP_BT2.equals(buttonPrefKey)) ledNumber = 2;
                else if (AppButtonPreferenceManager.KEY_APP_BT3.equals(buttonPrefKey)) ledNumber = 3;
                else if (AppButtonPreferenceManager.KEY_APP_BT4.equals(buttonPrefKey)) ledNumber = 4;

                if (ledNumber > 0) {
                    ledController.ligarLed(ledNumber, 1000);
                } else {
                    Log.e("MainActivity", "Ação desconhecida para controle de LED: " + buttonPrefKey);
                }
            } else {
                Log.w("MainActivity", "Não foi possível controlar o LED: serviço não vinculado ou não conectado.");
            }
        }
    }

    private String getDefaultAppName(String buttonPrefKey) {
        switch (buttonPrefKey) {
            case AppButtonPreferenceManager.KEY_APP_BT1: return "App1";
            case AppButtonPreferenceManager.KEY_APP_BT2: return "App2";
            case AppButtonPreferenceManager.KEY_APP_BT3: return "App3";
            case AppButtonPreferenceManager.KEY_APP_BT4: return "App4";
            default: return "";
        }
    }

    private void animateButtonClickAndOpen(View view, String buttonPrefKey, String appFolderName, String appPathType) {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f);
        scaleDownX.setDuration(ANIMATION_DURATION_SCALE_DOWN);
        scaleDownY.setDuration(ANIMATION_DURATION_SCALE_DOWN);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.05f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.05f, 1f);
        scaleUpX.setDuration(ANIMATION_DURATION_SCALE_UP);
        scaleUpY.setDuration(ANIMATION_DURATION_SCALE_UP);
        scaleUpX.setInterpolator(new OvershootInterpolator(1.5f));
        scaleUpY.setInterpolator(new OvershootInterpolator(1.5f));

        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, "rotation", 0f, -8f, 8f, 0f);
        rotate.setDuration(ANIMATION_DURATION_ROTATE);

        ObjectAnimator alphaDown = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f);
        ObjectAnimator alphaUp = ObjectAnimator.ofFloat(view, "alpha", 0.6f, 1f);
        alphaDown.setDuration(ANIMATION_DURATION_ALPHA);
        alphaUp.setDuration(ANIMATION_DURATION_SCALE_UP);
        alphaUp.setInterpolator(new AccelerateDecelerateInterpolator());

        animatorSet.play(scaleDownX).with(scaleDownY).with(alphaDown);
        animatorSet.play(scaleUpX).with(scaleUpY).with(alphaUp).after(scaleDownX);
        animatorSet.play(rotate).after(scaleDownX);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                String nameToSpeakTemp = null;
                // REMOVIDA: A condição para falar "Lista de Jogos" foi removida.
                // Agora, o TTS só será ativado para os botões de aplicativo.
                if (!"lista".equals(buttonPrefKey)) { // Se não for o botão "lista"
                    nameToSpeakTemp = getAppNameFromInfoTxt(appFolderName, appPathType, formatFolderNameForSpeech(appFolderName));
                }

                final String nameToSpeakFinal = nameToSpeakTemp;
                final String finalAppFolderName = appFolderName;
                final String finalAppPathType = appPathType;

                // Só fala se houver um nome válido para falar (ou seja, não para o botão "lista")
                if (tts != null && nameToSpeakFinal != null && !nameToSpeakFinal.isEmpty()) {
                    new Handler().postDelayed(() -> {
                        if (tts != null) {
                            tts.speak(nameToSpeakFinal, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }, 100);
                }

                new Handler().postDelayed(() -> {
                    if ("lista".equals(buttonPrefKey)) {
                        openGameList();
                    } else if (finalAppFolderName != null && finalAppPathType != null) {
                        openWebApp(finalAppFolderName, finalAppPathType);
                    }
                    isAnimationRunning.set(false);
                }, TRANSITION_DELAY_MS);
            }
        });

        animatorSet.start();
    }

    private String getAppNameFromInfoTxt(String appFolderName, String appPathType, String fallbackName) {
        InputStream infoIs = null;
        try {
            if ("internal".equals(appPathType)) {
                File appDir = new File(getFilesDir(), GameListActivity.IMPORTED_APPS_FOLDER + File.separator + appFolderName);
                File infoFile = new File(appDir, INFO_FILE_NAME);
                if (infoFile.exists()) {
                    infoIs = new java.io.FileInputStream(infoFile);
                } else {
                    Log.w("MainActivity", "Arquivo info.txt não encontrado em armazenamento interno para: " + appFolderName);
                    return fallbackName;
                }
            } else {
                AssetManager am = getAssets();
                String infoPath = "aplicacoes/" + appFolderName + "/" + INFO_FILE_NAME;
                try {
                    infoIs = am.open(infoPath);
                } catch (IOException e) {
                    Log.w("MainActivity", "Arquivo info.txt não encontrado em assets para " + appFolderName + ": " + e.getMessage());
                    return fallbackName;
                }
            }
            if (infoIs != null) {
                return readAppNameFromInfoTxt(infoIs, fallbackName);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Erro ao tentar obter nome do info.txt para " + appFolderName + ": " + e.getMessage());
        } finally {
            if (infoIs != null) {
                try {
                    infoIs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fallbackName;
    }

    private String formatFolderNameForSpeech(String folderName) {
        if (folderName == null || folderName.isEmpty()) {
            return "Nome Desconhecido";
        }
        String formatted = folderName.replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : formatted.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_1:
                    bt1.performClick();
                    return true;
                case KeyEvent.KEYCODE_2:
                    bt2.performClick();
                    return true;
                case KeyEvent.KEYCODE_3:
                    bt3.performClick();
                    return true;
                case KeyEvent.KEYCODE_4:
                    bt4.performClick();
                    return true;
                case KeyEvent.KEYCODE_5:
                    btLista.performClick();
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void updateButtonCovers() {
        String app1Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT1, "App1");
        String app1Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT1_TYPE, "asset");
        loadCapaImage(app1Folder, app1Type, bt1);

        String app2Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT2, "App2");
        String app2Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT2_TYPE, "asset");
        loadCapaImage(app2Folder, app2Type, bt2);

        String app3Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT3, "App3");
        String app3Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT3_TYPE, "asset");
        loadCapaImage(app3Folder, app3Type, bt3);

        String app4Folder = AppButtonPreferenceManager.getAppForButton(context, AppButtonPreferenceManager.KEY_APP_BT4, "App4");
        String app4Type = AppButtonPreferenceManager.getAppButtonType(context, AppButtonPreferenceManager.KEY_APP_BT4_TYPE, "asset");
        loadCapaImage(app4Folder, app4Type, bt4);
    }
}