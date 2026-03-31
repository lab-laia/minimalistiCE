// Arduino Firmware for minimalistiCE

#define VERSION "0.07"

#include "Arduino.h"
#include <Wire.h>
#include <FastLED.h>
#include <Serine.h>
#include "config.h"
#include "FontHV.h"
#include "Pump.h"

// Virtual devices living in this microcontroller
SerineVirtualDevice generalVD = { 'g', "tMinimalistiCE" };
SerineVirtualDevice detectorVD = { 'd', "SdL013003" };
SerineVirtualDevice highVoltageVD = { 'h', "tHighVoltage" };
SerineVirtualDevice pumpVD = { 'p', "tPump" };

// Definitions for the microcontroller
const byte PIN_BUTTON = 3;  // This is the digital port that corresponds to the INT
const byte SCK_AD = 7;      // Serial Clock for the ADC MCP3551
const byte CS_AD = 13;      // Chip Select for the ADC MCP3551
// Digital pins for Serial Digital Output (SDO) of the ADCs
const byte SDO_AD[] = { 4, 8, 12, 11 };

SerineChronometer mainChrono, ADCchrono, slowThingsChrono, connectionChrono,
  buttonChrono, buttonDouble, cronoPump, ledTime;
char destStatusPump;
unsigned long pumpTime;
boolean notConnected = true;
unsigned long timeOfConnection = 4294967295UL;
char sender = generalVD.id;  // At the beginning, the sender is the detector (We need one... it is not a big deal)
boolean continuousStatusC4D = false;
boolean continuousStatusHV = false;
boolean continuousStatusPump = false;
boolean continuousMode = false;
boolean enableDoubleClick = false;
byte numberOfClicks = 0;
char separador = 'f';
boolean includeTime = true;
boolean incluiAD[] = { true, true, false, false };
SerineBuffer usbSerine;
FontHV hvPowerSource(I2C_HVpowerSource);
Pump pump;

const int NUM_LEDS = 9;
CRGB led[NUM_LEDS];
int8_t act = 0;
int8_t step = 1;
int8_t lastLED = NUM_LEDS - 1;
byte ledMode = 0;
byte lastLedMode = 1;
int ledInterval = 100;

/**
 * The ADCs are continuously read, and the new values are posted in regData
 * structures. Every time the user ask for new ADC values, the most recent 
 * not-read values are sent back.
 */

struct regData {
  unsigned long time;
  unsigned long adc[4];
};

union regGet {
  regData adValue;
  byte stream[20];
};

regGet rGet[2] = {
  { 0, 0, 0, 0, 0 },
  { 0, 0, 0, 0, 0 }
};
byte toBeGet = 0;

void sendPumpStatus() {
  char answer[] = "--sivvvvvvttttt;";
  answer[0] = destStatusPump;
  answer[1] = pumpVD.id;
  answer[3] = pump.isControllingPressure() ? 'y' : 'n';
  usbSerine.putUL(constrain(pump.readPressure(), 0, 999999L), 4, 9, answer);
  usbSerine.putUL(constrain(pump.getTime() / 1000L, 0, 99999L), 10, 14, answer);
  Serial.print(answer);
}

void sendPumpStatus(char dest) {
  destStatusPump = dest;
  sendPumpStatus();
}

void inline handlePump(char *message) {
  char answer[MAX_MESSAGE];
  if (message[2] == 'S') {
    if (message[3] == ';') pumpTime = MAX_LONG;
    else pumpTime = constrain(usbSerine.getUL(3, 6, message), 10L, 9999L);
    sendPumpStatus(message[1]);
  } else if (message[2] == 'P') {
    setLED(1);
    if (message[6] == ';')
      pump.pressurize(usbSerine.getUL(3, 5, message));
    else
      pump.pressurize(usbSerine.getUL(3, 5, message), usbSerine.getUL(6, 7, message));
  } else if (message[2] == 'W') {
    char answer[] = "--wvvvvvvvv;";
    answer[0] = message[1];
    answer[1] = pumpVD.id;
    usbSerine.putUL(constrain(pump.getIoP(), 0, 99999999L), 3, 10, answer);
    Serial.print(answer);
  } else if (message[2] == 'H') {
    pump.halt();
  } else if (message[2] == 'O') {
    pump.pumpItUp(message[3] == 'N');
    pump.closeValve(message[4] == 'N');
  } else if (message[2] == 'Z') {
    pump.updateAmbPressure();
  } else if (message[2] == 'I') {
    if (message[3] == 'x') {
      serineChangeID(&pumpVD, message);
    } else {
      serineAnswerID(message[1], pumpVD, answer);
      Serial.print(answer);
    }
    serineAnswerID(message[1], pumpVD, answer);
    Serial.print(answer);
  } else {
    serineIdontKnow(message, pumpVD.id, answer);
    Serial.print(answer);
  }
}

void answerC4DgetStatus() {
  Serial.print(sender);
  Serial.print(detectorVD.id);
  Serial.print("gs");
  Serial.print(continuousMode ? 'T' : 'F');
  Serial.print(';');
}

void SendButtonPressed() {
  Serial.print(sender);
  Serial.print(generalVD.id);
  Serial.print("B");
  Serial.print(constrain(numberOfClicks, 0, 3));
  Serial.print(';');
}

void answerC4Dget() {
  char answer[] = "0000000";
  Serial.print(sender);
  Serial.print(detectorVD.id);
  Serial.print('g');
  usbSerine.putUL(rGet[toBeGet].adValue.time, 0, 6, answer);
  Serial.print(answer);
  usbSerine.putUL(rGet[toBeGet].adValue.adc[0], 0, 6, answer);
  Serial.print(answer);
  usbSerine.putUL(rGet[toBeGet].adValue.adc[1], 0, 6, answer);
  Serial.print(answer);
  Serial.print(';');
}

void inline handleDetector(char *message) {
  switch (message[2]) {
    case 'G':  // Get
      switch (message[3]) {
        case 'r':
          continuousMode = true;
          mainChrono.zero();
          answerC4DgetStatus();
          break;
        case 'h':
          continuousMode = false;
          answerC4DgetStatus();
          break;
        case 's':
          answerC4DgetStatus();
          break;
        default: answerC4Dget();
      };
      break;
    case 'Z':  // Zero
      mainChrono.zero();
      break;
  }
}

void inline handleHighVoltage(char *message) {
  char answer[MAX_MESSAGE];
  if (message[2] == 'G') {
    hvPowerSource.getMonitor(message[1], answer);
    Serial.print(answer);
  } else if (message[2] == 'S') {
    hvPowerSource.getStatus(message[1], answer);
    Serial.print(answer);
  } else if (message[2] == 'R') {
    hvPowerSource.run(message[1]);
    setLED(3);
  } else if (message[2] == 'B') hvPowerSource.beep(message[1]);
  else if (message[2] == 'F') hvPowerSource.frozen(message[1]);
  else if (message[2] == 'U') hvPowerSource.unfrozen(message[1]);
  else if (message[2] == 'E') hvPowerSource.enable(message[1], true);
  else if (message[2] == 'D') hvPowerSource.enable(message[1], false);
  else if (message[2] == 'H') hvPowerSource.halt(message[1]);
  else if (message[2] == 'C') hvPowerSource.clear(message[1]);
  else if (message[2] == 'P') hvPowerSource.program(message);
  else if (message[2] == 'I') {
    if (message[3] == 'x') {
      serineChangeID(&highVoltageVD, message);
    } else {
      serineAnswerID(message[1], highVoltageVD, answer);
      Serial.print(answer);
    }
    serineAnswerID(message[1], highVoltageVD, answer);
    Serial.print(answer);
  } else {
    serineIdontKnow(message, highVoltageVD.id, answer);
    Serial.print(answer);
  }
}

void inline handleGeneral(char *message) {
  switch (message[2]) {
    case 'S':  // Status
      continuousStatusC4D = message[3] == 'T';
      continuousStatusHV = message[4] == 'T';
      continuousStatusPump = message[5] == 'T';
      break;
    case 'L':  // LED
      setLED(constrain(usbSerine.getUL(3, 3, message), 0, 5));
      break;
    case 'X':  // Connect
      sender = message[1];
      pump.halt();
      hvPowerSource.halt(generalVD.id);
      connectionChrono.zero();
      notConnected = (message[3] == 'F');
      if (!notConnected) {
        setLED(4);
        if (message[4] != ';')
          timeOfConnection = usbSerine.getUL(4, 7, message);
        else
          timeOfConnection = 4294967295UL;
      }
      if (notConnected) {
        continuousMode = false;
        continuousStatusC4D = false;
        continuousStatusHV = false;
        continuousStatusPump = false;
      }
      char answer[32];
      answer[0] = message[1];
      answer[1] = message[0];
      answer[2] = 'x';
      answer[3] = notConnected ? 'F' : 'N';
      answer[4] = ';';
      answer[5] = 0;
      Serial.print(answer);
      break;
    case 'I':
      if (message[3] == 'x') {
        serineChangeID(&generalVD, message);
      } else {
        serineAnswerID(message[1], generalVD, answer);
        Serial.print(answer);
      }
      serineAnswerID(message[1], generalVD, answer);
      Serial.print(answer);
      break;
    default:
      serineIdontKnow(message, generalVD.id, answer);
      Serial.print(answer);
  }
}

void inline handleBroadcast(char *message) {
  char answer[MAX_MESSAGE];
  switch (message[2]) {
    case 'I':
      serineAnswerID(message[1], generalVD, answer);
      Serial.print(answer);
      break;
    default:
      serineIdontKnow(message, generalVD.id, answer);
      Serial.print(answer);
  }
}

void inline takeCareADC() {
  delayMicroseconds(1);
  byte keepGoing = HIGH;
  byte numberOfTimes = 0;
  while (keepGoing == HIGH) {
    delay(1);
    keepGoing = LOW;
    for (int i = 0; i < 4; i++)
      if (incluiAD[i]) keepGoing = keepGoing | digitalRead(SDO_AD[i]);
    numberOfTimes++;
    if (numberOfTimes > 100) keepGoing = false;
  };
  unsigned long timeADC = mainChrono.now();
  /**
     * Read the first two bits:
     * 00 - 0 - OK
     * 01 - 1 - positive overflow
     * 10 - 2 - negative overflow
     */
  byte ovl[4];
  for (int i = 0; i < 4; i++) ovl[i] = 0;
  for (byte j = 0; j < 2; j++) {
    digitalWrite(SCK_AD, LOW);
    delayMicroseconds(1);
    digitalWrite(SCK_AD, HIGH);
    delayMicroseconds(1);
    for (int i = 0; i < 4; i++) ovl[i] = (ovl[i] << 1) | digitalRead(SDO_AD[i]);
  }
  long ad[4];
  // read B21 and determine if the value is positive or negative;
  digitalWrite(SCK_AD, LOW);
  delayMicroseconds(1);
  digitalWrite(SCK_AD, HIGH);
  delayMicroseconds(1);
  for (int i = 0; i < 4; i++) ad[i] = digitalRead(SDO_AD[i]) ? 0xFFFFFFFF : 0;
  // read the other bits
  for (int j = 0; j < 21; j++) {
    digitalWrite(SCK_AD, LOW);
    delayMicroseconds(1);
    digitalWrite(SCK_AD, HIGH);
    delayMicroseconds(1);
    for (int i = 0; i < 4; i++) ad[i] = (ad[i] << 1) | digitalRead(SDO_AD[i]);
  }
  //start a new A/D conversion
  digitalWrite(CS_AD, HIGH);
  delayMicroseconds(1);
  digitalWrite(CS_AD, LOW);
  // handle overflow and translate to a positive-only range
  for (int i = 0; i < 4; i++) {
    if (ovl[i] == 1) ad[i] = 2097151L;
    else if (ovl[i] == 2)
      ad[i] = -2097152L;
    ad[i] = ad[i] + 2097152L;
  }
  /**
     * Transfers the information to the register Get. This register has two 
     * positions: 0 and 1.
     */
  byte pos = 1 - toBeGet;
  rGet[pos].adValue.time = timeADC;
  for (int i = 0; i < 4; i++) rGet[pos].adValue.adc[i] = (unsigned long)ad[i];
  toBeGet = pos;  // point to these new ADC values
}

void setLED(byte mode) {
  ledMode = mode;
  switch (ledMode) {
    case 1:
      ledInterval = 100;
      break;
    case 2:
      ledInterval = 100;
      break;
    case 3:
      ledInterval = 250;
      break;
    case 4:
      ledInterval = 100;
      break;
    case 5:
      ledInterval = 250;
      break;
  }
}

void inline proceedLED() {
  if (lastLedMode != ledMode) {
    for (int i = 0; i <= lastLED; i++) led[i] = CRGB::Black;
    FastLED.show();
    act = 0;
  }
  lastLedMode = ledMode;
  switch (ledMode) {
    case 1:  // pressure
      led[act] = CRGB::Red;
      if (act == lastLED) step = -1;
      else if (act == 0) step = 1;
      for (int i = 0; i <= lastLED; i++) led[i].fadeToBlackBy(150);
      act = act + step;
      led[act] = CRGB::OrangeRed;
      FastLED.show();
      if (!pump.isControllingPressure()) setLED(0);
      break;
    case 2:  // vial change
      if (act == 0) {
        act = lastLED;
        step = 1;
      }
      act = act + step;
      if (act > lastLED) {
        ledInterval = 100;
        for (int i = lastLED; i >= lastLED - 3; i--) led[i] = CRGB::Black;
        act = lastLED - 3;
      }
      for (int i = lastLED; i >= lastLED - 3; i--) led[i].fadeToBlackBy(150);
      led[act] = CRGB::Green;
      FastLED.show();
      if (act == lastLED) ledInterval = 350;
      break;
    case 3:  // HV
      led[act] = CRGB::Blue;
      if (act == lastLED) step = -1;
      else if (act == 0) step = 1;
      for (int i = 0; i <= lastLED; i++) led[i].fadeToBlackBy(70);
      act = act + step;
      led[act] = CRGB::LightCyan;
      FastLED.show();
      break;
    case 4:  // connection
      led[act] = CRGB::Red + led[act];
      led[lastLED - act] = CRGB::Green + led[lastLED - act];
      if (act == lastLED) step = -1;
      else if (act == 0) step = 1;
      for (int i = 0; i <= lastLED; i++) led[i].fadeToBlackBy(50);
      act = act + step;
      led[act] = CRGB::Red + led[act];
      led[lastLED - act] = CRGB::Green + led[lastLED - act];
      FastLED.show();
      if (act == 0) {
        setLED(0);
        pump.updateAmbPressure();
      }
      break;
    case 5:  // post run
      led[act] = CRGB::Magenta;
      if (act == lastLED) step = -1;
      else if (act == 0) step = 1;
      for (int i = 0; i <= lastLED; i++) led[i].fadeToBlackBy(70);
      act = act + step;
      led[act] = CRGB::LightCyan;
      FastLED.show();
      break;
    default:;
  }
  ledTime.zero();
}

void buttonPressed() {
  if (buttonChrono.expired(200)) {
    buttonChrono.zero();
    if (digitalRead(PIN_BUTTON) == LOW) {  // check if this is a real click
      numberOfClicks++;
      buttonDouble.zero();
    }
  };
}

void setup() {
  Serial.begin(115200);
  Wire.begin();
  pinMode(PIN_BUTTON, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(PIN_BUTTON), buttonPressed, FALLING);
  pump.config(PRESSURE_PUMP_PIN, PRESSURE_VALV_PIN,
              PRESSURE_DOUT_PIN, PRESSURE_SCK_PIN);
  // Prepare the ADCs
  for (int i = 0; i < 4; i++) digitalWrite(SDO_AD[i], LOW);  // induce a pull down **********************************
  for (int i = 0; i < 4; i++) pinMode(SDO_AD[i], INPUT);
  pinMode(SCK_AD, OUTPUT);
  digitalWrite(SCK_AD, HIGH);
  pinMode(CS_AD, OUTPUT);
  digitalWrite(CS_AD, HIGH);
  delay(100);
  digitalWrite(CS_AD, LOW);
  // Prepare the chronometers
  ADCchrono.zero();
  mainChrono.zero();
  slowThingsChrono.zero();
  connectionChrono.zero();
  buttonChrono.zero();
  buttonDouble.zero();
  pumpTime = MAX_LONG;
  cronoPump.zero();
  // Prepare the leds
  FastLED.addLeds<NEOPIXEL, LED_DATA_PIN>(led, NUM_LEDS);
  ledTime.zero();
}

void loop() {
  if (numberOfClicks > 0)
    if (buttonDouble.expired(500)) {
      SendButtonPressed();
      numberOfClicks = 0;
    }
  if (ledTime.expired(ledInterval)) proceedLED();
  if (ADCchrono.expired(75)) {
    takeCareADC();
    if (continuousMode) answerC4Dget();
    ADCchrono.zero();
  }
  pump.proceed();
  if (cronoPump.expired(pumpTime)) {
    sendPumpStatus();
    cronoPump.zero();
  }
  // Feed the Serine buffer with characters from serial port
  while (Serial.available() && (usbSerine.getSpace() > 1)) usbSerine.putChar(Serial.read());
  if (usbSerine.hasMessage()) {
    char message[MAX_MESSAGE];
    usbSerine.getMessage(message);
    if (message[0] == detectorVD.id) handleDetector(message);
    else if (message[0] == generalVD.id) handleGeneral(message);
    else if (message[0] == highVoltageVD.id) handleHighVoltage(message);
    else if (message[0] == pumpVD.id) handlePump(message);
    else if (message[0] == 'B') handleBroadcast(message);
  };
  // Slow things to do
  if (slowThingsChrono.expired(503)) {
    slowThingsChrono.zero();
    if (connectionChrono.expired(timeOfConnection)) {
      notConnected = true;
      continuousMode = false;
      continuousStatusC4D = false;
      continuousStatusHV = false;
      continuousStatusPump = false;
    }
    if (notConnected) {  // Broadcast the device ID and serial ID
      Serial.print('B');
      Serial.print(generalVD.id);
      Serial.print('i');
      Serial.print(generalVD.serial);
      Serial.print(';');
    }
    if (continuousStatusC4D) answerC4DgetStatus();
    if (continuousStatusPump) sendPumpStatus(sender);
    char answer[MAX_MESSAGE];
    hvPowerSource.getStatus(sender, answer);
    if (continuousStatusHV) {
      Serial.print(answer);
      hvPowerSource.getMonitor(sender, answer);
      Serial.print(answer);
    }
  }
}
