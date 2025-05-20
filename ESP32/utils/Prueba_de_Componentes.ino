#include <LiquidCrystal.h>

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
}

void loop() {
  // 1) Parpadeo secuencial de LEDs
  digitalWrite(LED_ROJO,   HIGH);
  delay(100);
  digitalWrite(LED_ROJO,   LOW);
  digitalWrite(LED_BLANCO, HIGH);
  delay(100);
  digitalWrite(LED_BLANCO, LOW);
  digitalWrite(LED_VERDE,  HIGH);
  delay(100);
  digitalWrite(LED_VERDE,  LOW);
  delay(300);

  // 2) Lectura de potenciómetro y botón
  int valorPot = analogRead(POT_MENU);      // 0–4095 en ESP32
  bool boton  = digitalRead(PULS_MENU);     // HIGH cuando se pulsa

  Serial.printf("Pot=%d  Btn=%s\n", valorPot,
                boton ? "ON" : "OFF");

  // 3) Mostrar en LCD
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Pot: ");
  lcd.print(map(valorPot, 0, 4095, 0, 100));
  lcd.print("%");

  lcd.setCursor(0, 1);
  lcd.print("Btn: ");
  lcd.print(boton ? "ON" : "OFF");

  // 4) Buzzer en la pulsación
  if (boton) {
    tone(BUZZER_PIN, 1000, 100);  // 1 kHz, 100 ms
  }

  delay(500);
}