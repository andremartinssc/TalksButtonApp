package com.example.talksbutton;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;

import java.util.Locale;

public class SegundaTela extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.segunda_tela);

        // Inicializar o TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        ImageView bt_banheiro = findViewById(R.id.bt_banheiro);
        ImageView bt_beber = findViewById(R.id.bt_beber);
        ImageView bt_comer = findViewById(R.id.bt_comer);
        ImageView bt_brincar = findViewById(R.id.bt_brincar);
        ImageView bt_voltar = findViewById(R.id.bt_voltar);

        // Configurar cliques para as imagens na segunda tela
        bt_banheiro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lerTexto("Banheiro");
            }
        });

        bt_beber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lerTexto("Beber");
            }
        });

        bt_comer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lerTexto("Comer");
            }
        });

        bt_brincar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lerTexto("Brincar");
            }
        });

        bt_voltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Voltar para a tela anterior
                finish();
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

    // Adicione os métodos de clique para as imagens (bt_banheiro, bt_beber, bt_comer, bt_brincar)
    public void lerBanheiro(View view) {
        lerTexto("Banheiro");
    }

    public void lerBeber(View view) {
        lerTexto("Beber");
    }

    public void lerComer(View view) {
        lerTexto("Comer");
    }

    public void lerBrincar(View view) {
        lerTexto("Brincar");
    }
}
