// LedController.java
package com.example.talksbutton;

import android.os.Handler;
import android.util.Log;

public class LedController {

    private static final String TAG = "LedController";
    private BluetoothService bluetoothService;
    private boolean isBluetoothBound = false;

    public LedController(BluetoothService service, boolean bound) {
        this.bluetoothService = service;
        this.isBluetoothBound = bound;
    }

    public void ligarLed(int numeroLed, long duracao) {
        if (numeroLed >= 1 && numeroLed <= 4) {
            String comandoLigar = "L" + numeroLed + "ON";
            Log.d(TAG, "Enviando comando para ligar LED " + numeroLed + ": " + comandoLigar);
            enviarComandoParaESP32(comandoLigar);

            // Desligar o LED após a duração especificada
            new Handler().postDelayed(() -> {
                String comandoDesligar = "L" + numeroLed + "OFF";
                Log.d(TAG, "Enviando comando para desligar LED " + numeroLed + ": " + comandoDesligar);
                enviarComandoParaESP32(comandoDesligar);
            }, duracao);

        } else {
            Log.e(TAG, "Número de LED inválido: " + numeroLed + ". Deve ser entre 1 e 4.");
        }
    }

    private void enviarComandoParaESP32(String comando) {
        if (isBluetoothBound && bluetoothService != null) {
            bluetoothService.sendData(comando + "\n");
        } else {
            Log.e(TAG, "Serviço Bluetooth não conectado. Não foi possível enviar o comando: " + comando);
        }
    }
}