#include <SoftwareSerial.h>
#include <DHT.h>
#include <Servo.h>

#define DHTPIN 2
#define DHTTYPE DHT11
#define TRIG 3
#define ECHO 4
#define LED 7
#define BUZZER 8

// ===== 74HC595 =====
#define DATA_PIN 5
#define CLOCK_PIN 6
#define LATCH_PIN 12

SoftwareSerial bt(10, 11); // RX, TX
DHT dht(DHTPIN, DHTTYPE);
Servo doorServo;

unsigned long lastDetectedTime = 0;
bool doorOpen = false;

// Watchdog: reset display to 0 if no BT command received for this many ms
unsigned long lastBtActivity = 0;
const unsigned long BT_TIMEOUT = 8000;

byte digits[10] = {
  B00111111, // 0
  B00000110, // 1
  B01011011, // 2
  B01001111, // 3
  B01100110, // 4
  B01101101, // 5
  B01111101, // 6
  B00000111, // 7
  B01111111, // 8
  B01101111  // 9
};

void setup() {
  Serial.begin(9600);
  bt.begin(9600);
  
  pinMode(TRIG, OUTPUT);
  pinMode(ECHO, INPUT);
  pinMode(LED, OUTPUT);
  pinMode(BUZZER, OUTPUT);

  pinMode(DATA_PIN, OUTPUT);
  pinMode(CLOCK_PIN, OUTPUT);
  pinMode(LATCH_PIN, OUTPUT);

  doorServo.attach(9);
  doorServo.write(0); // puerta cerrada

  dht.begin();

  displayNumber(0);          // start at 0 — no devices connected
  lastBtActivity = millis(); // initialise watchdog timer
}

void displayNumber(int num) {

  if (num < 0 || num > 9) return;

  digitalWrite(LATCH_PIN, LOW);
  shiftOut(DATA_PIN, CLOCK_PIN, MSBFIRST, digits[num]);
  digitalWrite(LATCH_PIN, HIGH);
}

void handleDoor(float distance) {

  if (distance < 20) {
    doorServo.write(90);   // abrir
    doorOpen = true;
    lastDetectedTime = millis();
  }

  if (doorOpen && distance >= 20) {
    if (millis() - lastDetectedTime > 3000) {
      doorServo.write(0);  // cerrar
      doorOpen = false;
    }
  }
}

float getDistance() {
  digitalWrite(TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG, LOW);

  long duration = pulseIn(ECHO, HIGH);
  float distance = duration * 0.034 / 2;
  return distance;
}

void loop() {
  
  float temp = dht.readTemperature();
  float hum = dht.readHumidity();
  float dist = getDistance();
  handleDoor(dist);

  // Enviar datos por Bluetooth
  bt.print(temp);
  bt.print(",");
  bt.print(hum);
  bt.print(",");
  bt.print(dist);
  bt.print(",");
  bt.println(doorOpen ? "OPEN" : "CLOSED");

  // Leer comandos desde app
  if (bt.available()) {
    lastBtActivity = millis(); // feed watchdog
    String cmd = bt.readStringUntil('\n');
    cmd.trim();

    if (cmd == "LED_ON") digitalWrite(LED, HIGH);
    if (cmd == "LED_OFF") digitalWrite(LED, LOW);
    if (cmd == "BUZZER_ON") digitalWrite(BUZZER, HIGH);
    if (cmd == "BUZZER_OFF") digitalWrite(BUZZER, LOW);
    if (cmd == "DOOR_OPEN") { doorServo.write(90); doorOpen = true; lastDetectedTime = millis(); }
    if (cmd == "DOOR_CLOSE") { doorServo.write(0); doorOpen = false; }

    // CLIENTS:N — update 7-segment display with total connected device count
    if (cmd.startsWith("CLIENTS:")) {
      int n = cmd.substring(8).toInt();
      n = constrain(n, 0, 9);
      displayNumber(n);
      Serial.print("Display actualizado: ");
      Serial.println(n);
    }
  }

  // Watchdog: if no BT data for BT_TIMEOUT ms, assume app disconnected/closed
  if (millis() - lastBtActivity > BT_TIMEOUT) {
    displayNumber(0);
    lastBtActivity = millis(); // avoid repeated resets every loop
  }

  delay(1000);
}