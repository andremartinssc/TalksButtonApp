package com.example.talksbutton;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BT_PERMISSIONS = 1;
    private static final int RECONNECT_DELAY_MS = 5000; // 5 segundos
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int ANIMATION_DURATION_SCALE_DOWN = 150;
    private static final int ANIMATION_DURATION_SCALE_UP = 300;
    private static final int ANIMATION_DURATION_ROTATE = 250;
    private static final int ANIMATION_DURATION_ALPHA = 150;
    private static final int TRANSITION_DELAY_MS = 300;

    private ImageView bt1, bt2, bt3, bt4, btLista;
    private BluetoothService mService;
    private boolean mBound = false;
    private boolean isConnected = false;
    private int reconnectAttemptCount = 0;
    private Handler reconnectHandler = new Handler();
    private AtomicBoolean isAnimationRunning = new AtomicBoolean(false);

    private static final String CAPA_FILE_NAME = "capa.jpg";

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d("MainActivity", "Serviço Bluetooth conectado.");
            attemptBluetoothConnection();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
            Log.d("MainActivity", "Serviço Bluetooth desconectado.");
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
                    reconnectAttemptCount = 0; // Resetar tentativas após mudança de estado
                    if (!isConnected && mBound && mService != null) {
                        startReconnectTimer();
                    } else {
                        stopReconnectTimer();
                    }
                }
            }
        }
    };

    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (mBound && mService != null && !isConnected && reconnectAttemptCount < MAX_RECONNECT_ATTEMPTS) {
                Log.i("MainActivity", "Tentando reconectar ao dispositivo Talks Button (Tentativa " + (reconnectAttemptCount + 1) + ")");
                mService.connect();
                reconnectAttemptCount++;
                startReconnectTimer(); // Agendar a próxima tentativa
            } else if (reconnectAttemptCount >= MAX_RECONNECT_ATTEMPTS) {
                Log.w("MainActivity", "Número máximo de tentativas de reconexão atingido.");
                Toast.makeText(MainActivity.this, "Falha ao conectar ao dispositivo Talks Button.", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar os botões
        bt1 = findViewById(R.id.bt_1);
        bt2 = findViewById(R.id.bt_2);
        bt3 = findViewById(R.id.bt_3);
        bt4 = findViewById(R.id.bt_4);
        btLista = findViewById(R.id.bt_lista);

        // Carregar as imagens de capa para os botões
        loadCapaImage("App1", bt1);
        loadCapaImage("App2", bt2);
        loadCapaImage("App3", bt3);
        loadCapaImage("App4", bt4);

        // Lógica de clique para cada botão com animação chamativa e atraso
        bt1.setOnClickListener(v -> handleButtonClick(v, "App1"));
        bt2.setOnClickListener(v -> handleButtonClick(v, "App2"));
        bt3.setOnClickListener(v -> handleButtonClick(v, "App3"));
        bt4.setOnClickListener(v -> handleButtonClick(v, "App4"));
        btLista.setOnClickListener(v -> handleButtonClick(v, "lista"));

        if (!hasBluetoothPermissions()) {
            requestPermissions();
        } else {
            startBluetoothService();
        }
    }

    private void loadCapaImage(String appFolder, ImageView imageView) {
        AssetManager am = getAssets();
        InputStream is = null;
        try {
            String imagePath = "aplicacoes/" + appFolder + "/" + CAPA_FILE_NAME;
            is = am.open(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            Log.w("MainActivity", "Imagem de capa não encontrada para " + appFolder + ": " + e.getMessage());
            // imageView.setImageResource(R.drawable.imagem_padrao);
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

    private void startBluetoothService() {
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void attemptBluetoothConnection() {
        if (mBound && mService != null && !isConnected) {
            Log.i("MainActivity", "Tentando conectar ao dispositivo Talks Button...");
            mService.connect();
            isConnected = mService.isConnected(); // Atualiza o estado imediatamente após a tentativa
            if (!isConnected) {
                startReconnectTimer();
            } else {
                stopReconnectTimer();
            }
        }
    }

    private void handleBluetoothData(String data) {
        runOnUiThread(() -> {
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
                default:
                    break;
            }
        });
    }

    private void openWebApp(String appName) {
        Intent intent = new Intent(MainActivity.this, WebAppActivity.class);
        intent.putExtra("app_name", appName);
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

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        }, REQUEST_BT_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS && hasBluetoothPermissions()) {
            startBluetoothService();
        } else {
            Toast.makeText(this, "Permissões Bluetooth necessárias.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter dataFilter = new IntentFilter("bluetooth_data_received");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothDataReceiver, dataFilter);
        IntentFilter connectionFilter = new IntentFilter("bluetooth_connection_state");
        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothConnectionReceiver, connectionFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothDataReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothConnectionReceiver);
        stopReconnectTimer(); // Parar o timer se a Activity for parada
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
        stopReconnectTimer(); // Garantir que o timer seja parado ao destruir a Activity
    }

    private void startReconnectTimer() {
        reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS);
    }

    private void stopReconnectTimer() {
        reconnectHandler.removeCallbacks(reconnectRunnable);
    }

    private void handleButtonClick(View view, String action) {
        if (isAnimationRunning.compareAndSet(false, true)) {
            animateButtonClickAndOpen(view, action);
        }
    }

    private void animateButtonClickAndOpen(View view, String action) {
        AnimatorSet animatorSet = new AnimatorSet();

        // Escala para ligeiramente menor
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f);
        scaleDownX.setDuration(ANIMATION_DURATION_SCALE_DOWN);
        scaleDownY.setDuration(ANIMATION_DURATION_SCALE_DOWN);

        // Escala de volta com um "overshoot" mais suave
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.05f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.05f, 1f);
        scaleUpX.setDuration(ANIMATION_DURATION_SCALE_UP);
        scaleUpY.setDuration(ANIMATION_DURATION_SCALE_UP);
        scaleUpX.setInterpolator(new OvershootInterpolator(1.5f));
        scaleUpY.setInterpolator(new OvershootInterpolator(1.5f));

        // Leve rotação
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, "rotation", 0f, -8f, 8f, 0f);
        rotate.setDuration(ANIMATION_DURATION_ROTATE);

        // Leve mudança de alpha (transparência)
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
                new Handler().postDelayed(() -> {
                    if (action.equals("App1")) {
                        openWebApp("App1");
                    } else if (action.equals("App2")) {
                        openWebApp("App2");
                    } else if (action.equals("App3")) {
                        openWebApp("App3");
                    } else if (action.equals("App4")) {
                        openWebApp("App4");
                    } else if (action.equals("lista")) {
                        openGameList();
                    }
                    isAnimationRunning.set(false); // Libera o bloqueio após a conclusão
                }, TRANSITION_DELAY_MS);
            }
        });

        animatorSet.start();
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
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}