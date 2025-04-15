#include "BluetoothSerial.h"

// Definição dos pinos dos LEDs externos
#define LED1 4
#define LED2 16
#define LED3 17
#define LED4 5

// Definição dos pinos dos botões
#define BUTTON1 13
#define BUTTON2 14
#define BUTTON3 12
#define BUTTON4 27
#define BUTTON5 26 // Botão 5

// Definição do LED integrado do ESP32
#define LED_BT 2 // Geralmente o LED onboard do ESP32 está no pino 2

// Criar objeto BluetoothSerial
BluetoothSerial SerialBT;

// Variáveis para debounce
unsigned long lastPressTime1 = 0;
unsigned long lastPressTime2 = 0;
unsigned long lastPressTime3 = 0;
unsigned long lastPressTime4 = 0;
unsigned long lastPressTime5 = 0; // Adicionado lastPressTime5
const unsigned long debounceDelay = 200; // 200ms de debounce

// Variáveis de estado dos LEDs para controle via Bluetooth
bool led1BluetoothState = false;
bool led2BluetoothState = false;
bool led3BluetoothState = false;
bool led4BluetoothState = false;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("TalksButton_ESP32");
  Serial.println("Bluetooth pronto!");

  pinMode(LED1, OUTPUT);
  pinMode(LED2, OUTPUT);
  pinMode(LED3, OUTPUT);
  pinMode(LED4, OUTPUT);
  pinMode(LED_BT, OUTPUT); // Configura o LED integrado como saída

  pinMode(BUTTON1, INPUT_PULLUP);
  pinMode(BUTTON2, INPUT_PULLUP);
  pinMode(BUTTON3, INPUT_PULLUP);
  pinMode(BUTTON4, INPUT_PULLUP);
  pinMode(BUTTON5, INPUT_PULLUP); // Configura o botão 5 como entrada pullup

  digitalWrite(LED1, LOW);
  digitalWrite(LED2, LOW);
  digitalWrite(LED3, LOW);
  digitalWrite(LED4, LOW);
  digitalWrite(LED_BT, LOW); // Começa desligado
}

void loop() {
  // Indica conexão Bluetooth no LED integrado
  if (SerialBT.hasClient()) {
    digitalWrite(LED_BT, HIGH); // Liga LED se estiver conectado
  } else {
    digitalWrite(LED_BT, LOW); // Desliga LED se não estiver conectado
  }

  // Controle via Bluetooth
  if (SerialBT.available()) {
    String comando = SerialBT.readStringUntil('\n');
    comando.trim();
    Serial.print("Comando recebido: "); // Log de comando recebido
    Serial.println(comando); // Log de comando recebido
    if (comando.startsWith("L")) {
      controlarLEDBluetooth(comando);
    }
  }

  // Controle pelos botões físicos
  verificarBotao(BUTTON1, LED1, "B1", lastPressTime1);
  verificarBotao(BUTTON2, LED2, "B2", lastPressTime2);
  verificarBotao(BUTTON3, LED3, "B3", lastPressTime3);
  verificarBotao(BUTTON4, LED4, "B4", lastPressTime4);
  verificarBotaoBotao5(BUTTON5, "B5", lastPressTime5); // Função para o botão 5

  // Atualiza o estado dos LEDs via Bluetooth
  atualizarEstadoLEDBluetooth(LED1, led1BluetoothState);
  atualizarEstadoLEDBluetooth(LED2, led2BluetoothState);
  atualizarEstadoLEDBluetooth(LED3, led3BluetoothState);
  atualizarEstadoLEDBluetooth(LED4, led4BluetoothState);

  delay(50); // Pequeno delay para estabilidade
}

void controlarLEDBluetooth(String comando) {
  if (comando == "L1ON") {
    led1BluetoothState = true;
    Serial.println("LED1 ON (Bluetooth)"); // Log de comando enviado
  } else if (comando == "L1OFF") {
    led1BluetoothState = false;
    Serial.println("LED1 OFF (Bluetooth)"); // Log de comando enviado
  } else if (comando == "L2ON") {
    led2BluetoothState = true;
    Serial.println("LED2 ON (Bluetooth)"); // Log de comando enviado
  } else if (comando == "L2OFF") {
    led2BluetoothState = false;
    Serial.println("LED2 OFF (Bluetooth)"); // Log de comando enviado
  } else if (comando == "L3ON") {
    led3BluetoothState = true;
    Serial.println("LED3 ON (Bluetooth)"); // Log de comando enviado
  } else if (comando == "L3OFF") {
    led3BluetoothState = false;
    Serial.println("LED3 OFF (Bluetooth)"); // Log de comando enviado
  } else if (comando == "L4ON") {
    led4BluetoothState = true;
    Serial.println("LED4 ON (Bluetooth)"); // Log de comando enviado
  } else if (comando == "L4OFF") {
    led4BluetoothState = false;
    Serial.println("LED4 OFF (Bluetooth)"); // Log de comando enviado
  }
}

void verificarBotao(int botao, int led, String mensagem, unsigned long &lastPressTime) {
  if (digitalRead(botao) == LOW) {
    unsigned long currentTime = millis();
    if (currentTime - lastPressTime > debounceDelay) {
      digitalWrite(led, HIGH);
      SerialBT.println(mensagem);
      Serial.print("Comando enviado (Botão): "); // Log de comando enviado
      Serial.println(mensagem); // Log de comando enviado
      lastPressTime = currentTime;
    }
  } else {
    digitalWrite(led, LOW);
  }
}

void verificarBotaoBotao5(int botao, String mensagem, unsigned long &lastPressTime) {
  if (digitalRead(botao) == LOW) {
    unsigned long currentTime = millis();
    if (currentTime - lastPressTime > debounceDelay) {
      SerialBT.println(mensagem);
      Serial.print("Comando enviado (Botão 5): "); // Log de comando enviado
      Serial.println(mensagem); // Log de comando enviado
      lastPressTime = currentTime;
    }
  }
}

void atualizarEstadoLEDBluetooth(int ledPin, bool ledState) {
  digitalWrite(ledPin, ledState ? HIGH : LOW);
}