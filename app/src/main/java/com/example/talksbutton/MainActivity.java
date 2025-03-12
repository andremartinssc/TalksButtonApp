package com.example.talksbutton;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private int configClickCount = 0;
    private Handler handler = new Handler();
    private Runnable resetClickCountRunnable = () -> configClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar o TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        ImageView bt_quero = findViewById(R.id.bt_quero);
        ImageView bt_1 = findViewById(R.id.bt_1);
        ImageView bt_2 = findViewById(R.id.bt_2);
        ImageView bt_3 = findViewById(R.id.bt_3);
        ImageView bt_4 = findViewById(R.id.bt_4);
        ImageView bt_config = findViewById(R.id.bt_config);

        if (bt_quero != null) {
            bt_quero.setOnClickListener(v -> {
                lerTexto("Modificar");
                startActivity(new Intent(MainActivity.this, SegundaTela.class));
            });
        }

        if (bt_1 != null) {
            bt_1.setOnClickListener(v -> lerTexto("Atividade 1"));
        }

        if (bt_2 != null) {
            bt_2.setOnClickListener(v -> lerTexto("Atividade 2"));
        }

        if (bt_3 != null) {
            bt_3.setOnClickListener(v -> lerTexto("Atividade 3"));
        }

        if (bt_4 != null) {
            bt_4.setOnClickListener(v -> lerTexto("Atividade 4"));
        }

        if (bt_config != null) {
            bt_config.setOnClickListener(v -> {
                configClickCount++;
                handler.removeCallbacks(resetClickCountRunnable);
                handler.postDelayed(resetClickCountRunnable, 1000);

                if (configClickCount == 5) {
                    lerTexto("Surpresa escondida seus frescos");
                    startActivity(new Intent(MainActivity.this, Configuracao.class));
                    configClickCount = 0;
                }
            });
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Idioma não suportado ou dados ausentes", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Falha na inicialização do TextToSpeech", Toast.LENGTH_SHORT).show();
        }
    }

    private void lerTexto(String texto) {
        if (textToSpeech != null) {
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
