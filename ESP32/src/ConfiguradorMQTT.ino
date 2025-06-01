#include <LiquidCrystal.h>
#include <PubSubClient.h> // El ESP32 va a actuar como suscriptor
#include <WiFi.h>

String monedaSeleccionada = "SOL";   // Por defecto mostrar SOL
int ultimoValorPot = 0;
String precioMoneda_1 = "";
String precioMoneda_2 = "";
bool estadoAnteriorBtn = false;
float precioMoneda_1Anterior = 0.0;
float precioMoneda_2Anterior = 0.0;
const float UMBRAL_CAMBIO = 0.01;  // Umbral para detectar cambios significativos




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

// INFO WIFI Y BROKER MQTT
const char* ssid = "";
const char* pwd = "";
const char* mqtt_server = "broker.hivemq.com"; 

// INSTANCIAS
WiFiClient espClient;
PubSubClient client(espClient);

// Instancia LCD 4-bits: lcd(rs, enable, d4, d5, d6, d7)
LiquidCrystal lcd(LCD_RS, LCD_E, LCD_D4, LCD_D5, LCD_D6, LCD_D7);

void setup() {
  Serial.begin(115200);
  delay(1000);

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

  setup_wifi(); 
  client.setServer(mqtt_server, 1883); // Puerto de mosquito
  client.setCallback(callback); // Se ejecutará automaticamente cada vez que el ESP32 reciba un mensaje MQTT
}


void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // Leer estado actual del botón
  bool estadoActualBtn = digitalRead(PULS_MENU) == HIGH;

  // Mostrar lectura en Serial
  Serial.printf("Btn=%s\n", estadoActualBtn ? "ON" : "OFF");

  // Alternar moneda (Pendiente a cambio porque actualmente solo tenemos un botón)
  if (!estadoAnteriorBtn && estadoActualBtn) {
    if (monedaSeleccionada == "SOL") {
      monedaSeleccionada = "BTC";
    } else {
      monedaSeleccionada = "SOL";
    }

    tone(BUZZER_PIN, 1000, 100);  // Suena brevemente
  }

  // Guardar estado del botón para próxima iteración
  estadoAnteriorBtn = estadoActualBtn;

  actualizarLCD();
  delay(500);
}




void setup_wifi(){
  delay(10);
  lcd.clear();
  Serial.println("Conectando a wifi...");
  WiFi.begin(ssid,pwd);

  while(WiFi.status() != WL_CONNECTED){
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWifi conectado");
  Serial.println("IP: ");
  Serial.println(WiFi.localIP());
}



// Cada vez que se reciba un mensaje MQTT:
void callback(char* topic, byte* payload, unsigned int length){
  String mensaje = "";

  for (unsigned int i = 0; i < length; i++) {
    mensaje += (char)payload[i];
  }

  Serial.print("Topico: ");
  Serial.println(topic);
  Serial.print("Mensaje recibido: ");
  Serial.println(mensaje);

  float precioNuevo = mensaje.toFloat();

  // Configurar moneda desde otro cliente
  if (strcmp(topic, "config/moneda") == 0) {
    monedaSeleccionada = mensaje;
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Cfg moneda:");
    lcd.setCursor(0, 1);
    lcd.print(mensaje.substring(0, 16));
    return;
  }

  if (monedaSeleccionada == "BTC" && strcmp(topic, "crypto/btc") == 0) {
    if (precioMoneda_1 != "") {
      float anterior = precioMoneda_1Anterior;
      float diferencia = abs(precioNuevo - anterior);
      if (diferencia >= UMBRAL_CAMBIO) {
        if (precioNuevo > anterior) {
          prenderLED(LED_VERDE);
        } else if (precioNuevo < anterior) {
          prenderLED(LED_ROJO);
        } else {
          prenderLED(LED_BLANCO);
        }
      }
    }
    precioMoneda_1 = mensaje;
    precioMoneda_1Anterior = precioNuevo;

  } else if (monedaSeleccionada == "SOL" && strcmp(topic, "crypto/sol") == 0) {
    if (precioMoneda_2 != "") {
      float anterior = precioMoneda_2Anterior;
      float diferencia = abs(precioNuevo - anterior);
      if (diferencia >= UMBRAL_CAMBIO) {
        if (precioNuevo > anterior) {
          prenderLED(LED_VERDE);
        } else if (precioNuevo < anterior) {
          prenderLED(LED_ROJO);
        } else {
          prenderLED(LED_BLANCO);
        }
      }
    }
    precioMoneda_2 = mensaje;
    precioMoneda_2Anterior = precioNuevo;
  }
}


void reconnect() {
  while (!client.connected()) {
    Serial.print("Intentando MQTT...");
    if (client.connect("ESP32Monitor")) {
      Serial.println("Conectado");
      client.subscribe("config/moneda");
      client.subscribe("crypto/btc");
      client.subscribe("crypto/sol");
    } else {
      Serial.print("Fallo: ");
      Serial.print(client.state());
      delay(5000);
    }
  }
}



void actualizarLCD() {
  lcd.clear();
  lcd.setCursor(0, 0);

  if (monedaSeleccionada == "BTC") {
    lcd.print("BTC: ");
    lcd.setCursor(5, 0);
    lcd.print(precioMoneda_1.substring(0, 11));
  } else if (monedaSeleccionada == "SOL") {
    lcd.print("SOL: ");
    lcd.setCursor(5, 0);
    lcd.print(precioMoneda_2.substring(0, 11));
  }

  lcd.setCursor(0, 1);
  lcd.print("Moneda: " + monedaSeleccionada);
}



void prenderLED(int led) {
  Serial.print("Prendiendo LED pin: ");
  Serial.println(led);
  digitalWrite(LED_ROJO, HIGH);
  digitalWrite(LED_BLANCO, HIGH);
  digitalWrite(LED_VERDE, HIGH);

  digitalWrite(led, LOW);
}
