#include <Arduino.h>
#include "config.h" // Configuración de WIFI y MQTT
#include <LiquidCrystal.h>
#include <PubSubClient.h> // El ESP32 va a actuar como suscriptor
#include <WiFi.h>
#include <ArduinoJson.h> // Para procesar los mensajes JSON

String monedaSeleccionada = "SOL";   // Por defecto mostrar SOL
int ultimoValorPot = 0;
String precioMoneda_1 = "";
String precioMoneda_2 = "";
bool estadoAnteriorBtn = false;
float precioMoneda_1Anterior = 0.0;
float precioMoneda_2Anterior = 0.0;
const float UMBRAL_CAMBIO = 0.0001;  // Umbral para detectar cambios significativos

// Variables para comunicación con JavaFX
bool monitoreoActivo = false;
DynamicJsonDocument criptomonedasJson(2048); // Para almacenar configuración de criptomonedas
JsonArray criptomonedas;
float valoresCriticos[10] = {0.0}; // Hasta 10 criptomonedas
String simbolosCriptos[10];

// Almacenamiento de precios para todas las criptomonedas
String preciosMonedas[10];
float preciosAnterior[10];

// --- NUEVO: Guardar los tópicos suscritos actualmente ---
String topicsSuscritos[10];

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

// INSTANCIAS
WiFiClient espClient;
PubSubClient client(espClient);

// Instancia LCD 4-bits: lcd(rs, enable, d4, d5, d6, d7)
LiquidCrystal lcd(LCD_RS, LCD_E, LCD_D4, LCD_D5, LCD_D6, LCD_D7);

void setup() {
  Serial.begin(115200);
  delay(1000);

  // LEDs - Inicializar apagados (HIGH = apagado para esta configuración)
  pinMode(LED_ROJO,   OUTPUT);
  pinMode(LED_BLANCO, OUTPUT);
  pinMode(LED_VERDE,  OUTPUT);

  // Asegurar que todos los LEDs estén apagados al inicio
  apagarTodosLEDs();

  // Menú
  pinMode(POT_MENU,   INPUT);
  pinMode(PULS_MENU,  INPUT);

  pinMode(BUZZER_PIN, OUTPUT);

  // Inicializa LCD
  lcd.begin(16, 2);
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Iniciando...");

  // Inicializar semilla aleatoria para generar ID de cliente MQTT único
  randomSeed(analogRead(0));

  setup_wifi(); 
  client.setServer(MQTT_BROKER_ADRRESS, MQTT_BROKER_PORT); // Puerto de mosquito
  client.setCallback(callback); // Se ejecutará automaticamente cada vez que el ESP32 reciba un mensaje MQTT

  // Inicializar arrays
  for (int i = 0; i < 10; i++) {
    simbolosCriptos[i] = "";
    valoresCriticos[i] = 0.0;
    preciosMonedas[i] = "";
    preciosAnterior[i] = 0.0;
    topicsSuscritos[i] = "";
  }

  // Inicializar valores por defecto para BTC y SOL para compatibilidad
  simbolosCriptos[0] = "BTC";
  simbolosCriptos[1] = "SOL";

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Listo para");
  lcd.setCursor(0, 1);
  lcd.print("conectar MQTT");
}


void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // Leer el valor del potenciómetro para seleccionar moneda
  int valorPot = analogRead(POT_MENU);

  // Calcular el número de monedas disponibles
  int numMonedas = 0;
  for (int i = 0; i < 10; i++) {
    if (simbolosCriptos[i] != "") {
      numMonedas++;
    }
  }

  // Si tenemos monedas, usar el potenciómetro para seleccionar entre ellas
  if (numMonedas > 0) {
    // Mapear el valor del potenciómetro al índice de la moneda
    int indiceSeleccionado = map(valorPot, 0, 4095, 0, numMonedas - 1);

    // Encontrar la moneda en el índice calculado
    int monedaContador = 0;
    for (int i = 0; i < 10; i++) {
      if (simbolosCriptos[i] != "") {
        if (monedaContador == indiceSeleccionado) {
          // Si es diferente de la moneda actual, actualizamos
          if (simbolosCriptos[i] != monedaSeleccionada) {
            monedaSeleccionada = simbolosCriptos[i];
            // Sonar para confirmar el cambio
            tone(BUZZER_PIN, 1000, 100);
            // Actualizar LCD inmediatamente
            actualizarLCD();
          }
          break;
        }
        monedaContador++;
      }
    }
  }

  // También permitir cambiar con el botón para compatibilidad
  bool estadoActualBtn = digitalRead(PULS_MENU) == HIGH;

  if (!estadoAnteriorBtn && estadoActualBtn) {
    // Buscar la posición actual de la moneda seleccionada
    int monedaActualIndex = -1;
    for (int i = 0; i < 10; i++) {
      if (simbolosCriptos[i] == monedaSeleccionada) {
        monedaActualIndex = i;
        break;
      }
    }

    // Encontrar la siguiente moneda válida
    bool monedaEncontrada = false;
    for (int i = 1; i <= 10; i++) {
      int nextIndex = (monedaActualIndex + i) % 10;
      if (simbolosCriptos[nextIndex] != "") {
        monedaSeleccionada = simbolosCriptos[nextIndex];
        monedaEncontrada = true;
        break;
      }
    }

    // Si no hay más monedas, volver a la primera
    if (!monedaEncontrada && simbolosCriptos[0] != "") {
      monedaSeleccionada = simbolosCriptos[0];
    }

    // Sonar para confirmar el cambio
    tone(BUZZER_PIN, 1000, 100);

    // Actualizar LCD inmediatamente
    actualizarLCD();
  }

  // Guardar estado del botón para próxima iteración
  estadoAnteriorBtn = estadoActualBtn;

  // Actualizar LCD periódicamente con la información actual
  static unsigned long lastLcdUpdate = 0;
  if (monitoreoActivo && millis() - lastLcdUpdate > 1000) { // Actualizar cada segundo
    lastLcdUpdate = millis();
    actualizarLCD();
  }

  // Enviar datos de estado periódicamente a la app JavaFX si el monitoreo está activo
  static unsigned long lastMsgTime = 0;
  if (monitoreoActivo && millis() - lastMsgTime > 5000) { // Cada 5 segundos
    lastMsgTime = millis();

    // Crear objeto JSON con datos del ESP32
    DynamicJsonDocument datosDoc(256);
    datosDoc["moneda"] = monedaSeleccionada;

    // Buscar el valor actual y crítico de la moneda seleccionada
    int monedaIndex = -1;
    for (int i = 0; i < 10; i++) {
      if (simbolosCriptos[i] == monedaSeleccionada) {
        monedaIndex = i;
        break;
      }
    }

    if (monedaIndex >= 0) {
      datosDoc["valor"] = preciosMonedas[monedaIndex];
      datosDoc["valorCritico"] = valoresCriticos[monedaIndex];
    } else {
      // Compatibilidad con el código original
      datosDoc["valor"] = monedaSeleccionada == "BTC" ? precioMoneda_1 : precioMoneda_2;
      datosDoc["valorCritico"] = 0.0;
    }

    char buffer[256];
    serializeJson(datosDoc, buffer);

    // Publicar datos en el tópico ESP32/datos
    client.publish("ESP32/datos", buffer);
  }

  delay(100); // Reducir el retardo para una lectura más suave del potenciómetro
}




void setup_wifi(){
  delay(10);
  lcd.clear();
  Serial.println("Conectando a wifi...");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

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

  // Procesar mensajes de la app JavaFX
  if (strcmp(topic, "esp32/comandos") == 0) {
    if (mensaje.equals("start")) {
      monitoreoActivo = true;
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Monitoreo:");
      lcd.setCursor(0, 1);
      lcd.print("INICIADO");
      client.publish("ESP32/estado", "Monitoreo iniciado");
      tone(BUZZER_PIN, 1000, 500); // Tono de inicio
      return;
    } 
    else if (mensaje.equals("stop")) {
      monitoreoActivo = false;
      apagarTodosLEDs(); // Apagar LEDs cuando se detiene el monitoreo
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Monitoreo:");
      lcd.setCursor(0, 1);
      lcd.print("DETENIDO");
      client.publish("ESP32/estado", "Monitoreo detenido");
      tone(BUZZER_PIN, 500, 500); // Tono de parada
      return;
    }
  }

  // Procesar configuración de criptomonedas de la app JavaFX
  else if (strcmp(topic, "esp32/criptomonedas") == 0) {
    DeserializationError error = deserializeJson(criptomonedasJson, mensaje);

    if (error) {
      Serial.print("Error al deserializar JSON: ");
      Serial.println(error.c_str());
      return;
    }

    // --- NUEVO: Desuscribirse de los tópicos anteriores ---
    for (int i = 0; i < 10; i++) {
      if (topicsSuscritos[i] != "") {
        client.unsubscribe(topicsSuscritos[i].c_str());
        topicsSuscritos[i] = "";
      }
    }

    // Extraer criptomonedas
    criptomonedas = criptomonedasJson["criptomonedas"].as<JsonArray>();

    // Arreglos para almacenar información de suscripciones
    int i = 0;

    // Obtener lista de tópicos actuales para suscribirse según sea necesario
    for (JsonVariant v : criptomonedas) {
      if (i < 10) { // Máximo 10 criptomonedas
        String simbolo = v["simbolo"].as<String>();
        simbolosCriptos[i] = simbolo;
        valoresCriticos[i] = v["valorCritico"].as<float>();

        // Almacenar el tópico para esta criptomoneda
        String simboloLower = simbolo;
        simboloLower.toLowerCase();
        String topicMoneda = "crypto/" + simboloLower;
        topicsSuscritos[i] = topicMoneda;

        // Suscribirse al tópico de esta moneda
        if (!client.subscribe(topicMoneda.c_str())) {
          Serial.print("Error al suscribirse a ");
          Serial.println(topicMoneda);
        } else {
          Serial.print("Suscrito a ");
          Serial.println(topicMoneda);
          client.publish("ESP32/estado", ("Suscrito a " + topicMoneda).c_str());
        }
        i++;
      }
    }

    // Limpiar arrays si hay menos de 10 monedas
    for (; i < 10; i++) {
      simbolosCriptos[i] = "";
      valoresCriticos[i] = 0.0;
      preciosMonedas[i] = "";
      preciosAnterior[i] = 0.0;
      topicsSuscritos[i] = "";
    }

    // Si hay al menos una criptomoneda, seleccionarla
    if (criptomonedas.size() > 0) {
      monedaSeleccionada = simbolosCriptos[0];
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Cfg recibida:");
      lcd.setCursor(0, 1);
      lcd.print(String(criptomonedas.size()) + " criptomonedas");
      client.publish("ESP32/estado", "Configuración de criptomonedas actualizada");
      tone(BUZZER_PIN, 2000, 200);
    }
    return;
  }

  // Configurar moneda desde otro cliente (código original)
  if (strcmp(topic, "config/moneda") == 0) {
    monedaSeleccionada = mensaje;
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Cfg moneda:");
    lcd.setCursor(0, 1);
    lcd.print(mensaje.substring(0, 16));
    return;
  }

  // Procesar mensajes de cualquier criptomoneda configurada
  // Extraer el símbolo de la criptomoneda del tópico (formato: crypto/simbolo)
  String topicStr = String(topic);
  if (topicStr.startsWith("crypto/")) {
    String simbolo = topicStr.substring(7); // Extraer después de "crypto/"
    simbolo.toUpperCase(); // Convertir a mayúsculas para la comparación

    // Buscar esta moneda en nuestro array
    int monedaIndex = -1;
    for (int i = 0; i < 10; i++) {
      if (simbolosCriptos[i].equalsIgnoreCase(simbolo)) {
        monedaIndex = i;
        break;
      }
    }

    // Si encontramos la moneda, procesarla
    if (monedaIndex >= 0) {
      // Guardar el precio en el array correspondiente
      preciosMonedas[monedaIndex] = mensaje;
      float precioAnterior = preciosAnterior[monedaIndex];

      // Si esta es la moneda seleccionada actualmente, actualizar la vista y LEDs
      if (simbolosCriptos[monedaIndex] == monedaSeleccionada && monitoreoActivo) {
        float diferencia = abs(precioNuevo - precioAnterior);
        if (precioAnterior > 0) { // Solo si tenemos precio anterior
          if (diferencia >= UMBRAL_CAMBIO) {
            if (precioNuevo < precioAnterior) {
              prenderLED(LED_VERDE); // Precio bajó = LED verde
            } else if (precioNuevo > precioAnterior) {
              prenderLED(LED_ROJO); // Precio subió = LED rojo
            }
          } else {
            prenderLED(LED_BLANCO); // Sin cambio significativo = LED blanco
          }
        }
        // Actualizar LCD con el nuevo precio
        actualizarLCD();
      }

      // Guardar el precio nuevo como anterior para la próxima comparación
      preciosAnterior[monedaIndex] = precioNuevo;
    }

    // Para mantener la compatibilidad con el código original
    if (simbolo.equalsIgnoreCase("btc")) {
      precioMoneda_1 = mensaje;
      precioMoneda_1Anterior = precioNuevo;
    } else if (simbolo.equalsIgnoreCase("sol")) {
      precioMoneda_2 = mensaje;
      precioMoneda_2Anterior = precioNuevo;
    }
  }
}


void reconnect() {
  while (!client.connected()) {
    Serial.print("Intentando MQTT...");
    // Crear un ID de cliente único usando una parte aleatoria
    String clientId = "ESP32Monitor-";
    clientId += String(random(0xffff), HEX);

    if (client.connect(clientId.c_str())) {
      Serial.println("Conectado");

      // Suscribirse a los tópicos originales
      client.subscribe("config/moneda");
      client.subscribe("crypto/btc");
      client.subscribe("crypto/sol");

      // Suscribirse a los tópicos de la app JavaFX
      client.subscribe("esp32/comandos");
      client.subscribe("esp32/criptomonedas");

      // Suscribirse a los tópicos de todas las criptomonedas configuradas
      for (int i = 0; i < 10; i++) {
        if (simbolosCriptos[i] != "") {
          String simboloLower = simbolosCriptos[i];
          simboloLower.toLowerCase();
          String topicMoneda = "crypto/" + simboloLower;
          client.subscribe(topicMoneda.c_str());
          Serial.println("Suscrito a " + topicMoneda);
        }
      }

      // Publicar mensaje de conexión
      client.publish("ESP32/estado", "ESP32 conectado al broker MQTT");
    } else {
      Serial.print("Fallo: ");
      Serial.print(client.state());
      delay(5000);
    }
  }
}



void actualizarLCD() {
  if (!monitoreoActivo) {
    return; // No actualizar si el monitoreo está detenido
  }

  lcd.clear();
  lcd.setCursor(0, 0);

  // Buscar la moneda seleccionada en el array
  int monedaIndex = -1;
  for (int i = 0; i < 10; i++) {
    if (simbolosCriptos[i] == monedaSeleccionada) {
      monedaIndex = i;
      break;
    }
  }

  // Mostrar información de la moneda
  lcd.print(monedaSeleccionada + ": ");
  int posicionTexto = monedaSeleccionada.length() + 2;
  lcd.setCursor(posicionTexto, 0);

  // Si tenemos el precio de esta moneda, mostrarlo
  if (monedaIndex >= 0 && preciosMonedas[monedaIndex] != "") {
    // Limitar la longitud para que quepa en la pantalla
    String precio = preciosMonedas[monedaIndex];
    // Mostrar solo 4 decimales
    float precioF = precio.toFloat();
    char precioStr[12];
    dtostrf(precioF, 0, 4, precioStr);
    lcd.print(precioStr);
  } else {
    // Si no tenemos precio, mostrar indicador
    lcd.print("---");
  }

  // Buscar valor crítico para la moneda actual
  float valorCritico = 0.0;
  if (monedaIndex >= 0) {
    valorCritico = valoresCriticos[monedaIndex];
  }

  lcd.setCursor(0, 1);
  if (valorCritico > 0) {
    // Cambiar texto a V.Cr y mostrar 4 decimales
    char valorCriticoStr[12];
    dtostrf(valorCritico, 0, 4, valorCriticoStr);
    lcd.print("V.Cr: ");
    lcd.print(valorCriticoStr);

    // Comprobar si el precio actual está por debajo del valor crítico
    if (monedaIndex >= 0 && preciosMonedas[monedaIndex] != "") {
      float precioActual = preciosMonedas[monedaIndex].toFloat();
      static unsigned long lastAlertTime = 0;
      if (precioActual < valorCritico) {
        // Avisar con el buzzer si estamos por debajo del valor crítico
        if (millis() - lastAlertTime > 10000) { // Cada 10 segundos
          lastAlertTime = millis();
          tone(BUZZER_PIN, 500, 1000); // Más prolongado
          delay(1000);
          tone(BUZZER_PIN, 300, 1000);
        }
      } else if (precioActual >= valorCritico) {
        // Avisar con el buzzer si se alcanza o supera el valor crítico
        if (millis() - lastAlertTime > 10000) { // Cada 10 segundos
          lastAlertTime = millis();
          tone(BUZZER_PIN, 2000, 1000); // Más prolongado
        }
      }
    }
  } else {
    // Mostrar número de moneda actual / total de monedas
    int totalMonedas = 0;
    for (int i = 0; i < 10; i++) {
      if (simbolosCriptos[i] != "") {
        totalMonedas++;
      }
    }

    // Encontrar el índice de la moneda actual
    int monedaActualNum = 0;
    for (int i = 0; i < 10; i++) {
      if (simbolosCriptos[i] != "") {
        monedaActualNum++;
        if (simbolosCriptos[i] == monedaSeleccionada) {
          break;
        }
      }
    }

    lcd.print(monedaActualNum);
    lcd.print("/");
    lcd.print(totalMonedas);
    lcd.print(" - ");
    lcd.print(monedaSeleccionada);
  }
}



void apagarTodosLEDs() {
  digitalWrite(LED_ROJO, HIGH);
  digitalWrite(LED_BLANCO, HIGH);
  digitalWrite(LED_VERDE, HIGH);
}

void prenderLED(int led) {
  // Solo activar LEDs si el monitoreo está activo
  if (!monitoreoActivo) {
    apagarTodosLEDs();
    return;
  }

  Serial.print("Prendiendo LED pin: ");
  Serial.println(led);

  // Apagar todos los LEDs primero
  apagarTodosLEDs();

  // Cambiar lógica: LED_VERDE para baja, LED_ROJO para subida
  if (led == LED_ROJO) {
    digitalWrite(LED_ROJO, LOW); // Encender verde para SUBE
  } else if (led == LED_VERDE) {
    digitalWrite(LED_VERDE, LOW); // Encender rojo para BAJA
  } else {
    digitalWrite(LED_BLANCO, LOW); // Blanco o cualquier otro
  }

  // Apagar el LED después de un tiempo
  static unsigned long ledTimeout = 0;
  ledTimeout = millis() + 2000; // Mantener encendido por 2 segundos

  // Programar una tarea para apagar el LED
  static unsigned long lastLedCheck = 0;
  if (millis() - lastLedCheck > 100) {
    lastLedCheck = millis();
    if (millis() > ledTimeout) {
      apagarTodosLEDs();
    }
  }
}
