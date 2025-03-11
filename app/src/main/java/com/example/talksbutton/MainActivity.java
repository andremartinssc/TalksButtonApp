package com.example.talksbutton;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar o TextToSpeech - teste git
        textToSpeech = new TextToSpeech(this, this);

        ImageView bt_quero = findViewById(R.id.bt_quero);
        ImageView bt_sim = findViewById(R.id.bt_sim);
        ImageView bt_nao = findViewById(R.id.bt_nao);

        // Configurar cliques para as imagens
        bt_quero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lerTexto("Quero");

                // Ir para a segunda tela
                Intent intent = new Intent(MainActivity.this, SegundaTela.class);
                startActivity(intent);
            }
        });

        bt_sim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lerTexto("Sim");
            }
        });

        bt_nao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lerTexto("Não");
            }
        });
    }

    // Método para inicializar o TextToSpeech
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Se o idioma não for suportado, você pode tratar isso aqui
            }
        } else {
            // Se a inicialização falhar, você pode tratar isso aqui
        }
    }

    // Método para ler o texto fornecido
    private void lerTexto(String texto) {
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // Libere os recursos do TextToSpeech quando a atividade for destruída
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    // Método para o clique na imagem bt_quero
    public void lerTextoQuero(View view) {
        lerTexto("Quero");

        // Ir para a segunda tela
        Intent intent = new Intent(MainActivity.this, SegundaTela.class);
        startActivity(intent);
    }

    // Método para o clique na imagem bt_sim
    public void lerTextoSim(View view) {
        lerTexto("Sim");
    }

    // Método para o clique na imagem bt_nao
    public void lerTextoNao(View view) {
        lerTexto("Não");
    }
}
