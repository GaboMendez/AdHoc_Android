#include <SoftwareSerial.h>
#include <DHT.h>
#include <Servo.h>

#define DHTPIN 2
#define DHTTYPE DHT11
#define TRIG 3
#define ECHO 4
#define LED 7
#define BUZZER 8

SoftwareSerial bt(10, 11); // RX, TX
DHT dht(DHTPIN, DHTTYPE);
Servo doorServo;

unsigned long lastDetectedTime = 0;
bool doorOpen = false;

void setup() {
  Serial.begin(9600);
  bt.begin(9600);
  
  pinMode(TRIG, OUTPUT);
  pinMode(ECHO, INPUT);
  pinMode(LED, OUTPUT);
  pinMode(BUZZER, OUTPUT);

  doorServo.attach(9);
  doorServo.write(0); // puerta cerrada

  dht.begin();
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
    String cmd = bt.readStringUntil('\n');

    if (cmd == "LED_ON") digitalWrite(LED, HIGH);
    if (cmd == "LED_OFF") digitalWrite(LED, LOW);
    if (cmd == "BUZZER_ON") digitalWrite(BUZZER, HIGH);
    if (cmd == "BUZZER_OFF") digitalWrite(BUZZER, LOW);
    if (cmd == "DOOR_OPEN") { doorServo.write(90); doorOpen = true; lastDetectedTime = millis(); }
    if (cmd == "DOOR_CLOSE") { doorServo.write(0); doorOpen = false; }
  }

  delay(2000);
}