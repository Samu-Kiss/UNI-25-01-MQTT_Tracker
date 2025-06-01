#include <config.h>
#include <LiquidCrystal.h>
#include <WiFi.h>
#include <MQTTClient.h>
#include <ArduinoJson.h>

//-- Configuración de la conexión WiFi y MQTT

const char MQTT_BROKER_ADRRESS[] = "test.mosquitto.org";  // CHANGE TO MQTT BROKER'S ADDRESS
const int MQTT_PORT = 1883;
const char MQTT_CLIENT_ID[] = "YOUR-NAME-esp32-001";  // CHANGE IT AS YOU DESIRE
const char MQTT_USERNAME[] = "";                        // CHANGE IT IF REQUIRED, empty if not required
const char MQTT_PASSWORD[] = "";                        // CHANGE IT IF REQUIRED, empty if not required

// The MQTT topics that ESP32 should publish/subscribe
const char SUBSCRIBE_TOPIC[] = "YOUR-NAME-esp32-001/loopback";  // CHANGE IT AS YOU DESIRE

WiFiClient network;
MQTTClient mqtt = MQTTClient(256);


// ----- Pines según la tabla -----
const int LED_ROJO     = 32;   // Indicador Baja
const int LED_BLANCO   = 33;   // Indicador No Cambio
const int LED_VERDE    = 25;  // Indicador Subida

// LCD en modo 4-bits
const int LCD_RS       = 3;  // Register Select (blanco)
const int LCD_E        = 21;  // Enable        (morado)
const int LCD_D4       = 19;  // D4            (azul)
const int LCD_D5       = 18;  // D5            (verde)
const int LCD_D6       = 5;  // D6             (amarillo)
const int LCD_D7       = 17;  // D7            (naranja)

// Interacción menú
const int POT_MENU     = 34;  // Potenciómetro (rosa)
const int PULS_MENU    = 35;  // Pulsador      (azul)

const int BUZZER_PIN = 23; // Morado

// Instancia LCD 4-bits: lcd(rs, enable, d4, d5, d6, d7)
LiquidCrystal lcd(LCD_RS, LCD_E, LCD_D4, LCD_D5, LCD_D6, LCD_D7);

void setup() {
  Serial.begin(115200);

  // LEDs
  pinMode(LED_ROJO,   OUTPUT);
  pinMode(LED_BLANCO, OUTPUT);
  pinMode(LED_VERDE,  OUTPUT);

  // Menú
  pinMode(POT_MENU,   INPUT);
  pinMode(PULS_MENU,  INPUT);

  pinMode(BUZZER_PIN, OUTPUT);

  // Inicializa LCD
  lcd.begin(16, 2);
  lcd.clear();

  // Conexión WiFi
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.println("ESP32 - Connecting to Wi-Fi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  connectToMQTT();
}

void loop() {
  mqtt.loop();

}

void connectToMQTT() {
  // Connect to the MQTT broker
  mqtt.begin(MQTT_BROKER_ADRRESS, MQTT_PORT, network);

  // Create a handler for incoming messages
  mqtt.onMessage(messageHandler);

  Serial.print("ESP32 - Connecting to MQTT broker");

  while (!mqtt.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
    Serial.print(".");
    delay(100);
  }
  Serial.println();

  if (!mqtt.connected()) {
    Serial.println("ESP32 - MQTT broker Timeout!");
    return;
  }

  // Subscribe to a topic, the incoming messages are processed by messageHandler() function
  if (mqtt.subscribe(SUBSCRIBE_TOPIC))
    Serial.print("ESP32 - Subscribed to the topic: ");
  else
    Serial.print("ESP32 - Failed to subscribe to the topic: ");

  Serial.println(SUBSCRIBE_TOPIC);
  Serial.println("ESP32 - MQTT broker Connected!");
}

void messageHandler(String &topic, String &payload) {
  Serial.println("ESP32 - received from MQTT:");
  Serial.println("- topic: " + topic);
  Serial.println("- payload:");
  Serial.println(payload);
}