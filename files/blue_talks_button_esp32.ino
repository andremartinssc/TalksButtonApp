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
    if (comando.startsWith("L")) {
      controlarLED(comando);
    }
  }

  // Controle pelos botões físicos
  verificarBotao(BUTTON1, LED1, "B1", lastPressTime1);
  verificarBotao(BUTTON2, LED2, "B2", lastPressTime2);
  verificarBotao(BUTTON3, LED3, "B3", lastPressTime3);
  verificarBotao(BUTTON4, LED4, "B4", lastPressTime4);
  verificarBotaoBotao5(BUTTON5, "B5", lastPressTime5); // Função para o botão 5

  delay(50); // Pequeno delay para estabilidade
}

void controlarLED(String comando) {
  if (comando == "L1ON") digitalWrite(LED1, HIGH);
  else if (comando == "L1OFF") digitalWrite(LED1, LOW);
  else if (comando == "L2ON") digitalWrite(LED2, HIGH);
  else if (comando == "L2OFF") digitalWrite(LED2, LOW);
  else if (comando == "L3ON") digitalWrite(LED3, HIGH);
  else if (comando == "L3OFF") digitalWrite(LED3, LOW);
  else if (comando == "L4ON") digitalWrite(LED4, HIGH);
  else if (comando == "L4OFF") digitalWrite(LED4, LOW);
}

void verificarBotao(int botao, int led, String mensagem, unsigned long &lastPressTime) {
  if (digitalRead(botao) == LOW) {
    unsigned long currentTime = millis();
    if (currentTime - lastPressTime > debounceDelay) {
      digitalWrite(led, HIGH);
      SerialBT.println(mensagem);
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
      lastPressTime = currentTime;
    }
  }
}