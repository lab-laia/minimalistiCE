#include "Serine.h"
/* 
 * File:   Pump.cpp
 * Author: CLL
 * 
 * Created on 25 de Novembro de 2019, 18:03
 */

#include "Pump.h"
#include "config.h"

/**
 * Inicializa Pump com o endereço Arduino da bomba e da válvula. Deve ser
 * chamada em setup().
 * 
 * @param addressPump porta digital Arduino para a bomba
 * @param addressValve porta digital Arduino para a válvula
 */
void Pump::config(byte addressPump, byte addressValve, byte pressure_DOUT,
                  byte pressure_SCK) {
  aPump = addressPump;
  pinMode(aPump, OUTPUT);
  digitalWrite(aPump, HIGH);
  aValve = addressValve;
  pinMode(aValve, OUTPUT);
  digitalWrite(aValve, HIGH);
  //sensor.begin(pressure_DOUT, pressure_SCK, 128);
  // sensor.initialize(pressure_SCK, pressure_DOUT);
  pSensor = new HX710B(pressure_DOUT, pressure_SCK);
  (*pSensor).begin();
  posADC = 0;
  a[0] = a[1] = a[2] = 0;
  controllingPressure = false;
  emptying = false;
  minTime.zero();
  postTime.zero();
  continuousPress = false;
}

/**
 * Aborta a injeção, isto é, desliga a bomba e a válvula
 */
void Pump::halt() {
  closeValve(false);
  pumpItUp(false);
  controllingPressure = false;
  emptying = false;
  continuousPress = false;
}

/**
 * Verifica se há um processo de injeção em curso
 * 
 * @return true se estiver injetando
 */
boolean Pump::isControllingPressure() {
  return controllingPressure;
}

long Pump::readPressure() {
  // take the median of the last 3 data
  long median = ((a[0] < a[1] && a[1] < a[2]) || (a[2] < a[1] && a[1] < a[0])) ? a[1] : (((a[1] < a[0] && a[0] < a[2]) || (a[2] < a[0] && a[0] < a[1])) ? a[0] : a[2]);
  return long((median - ambPressure) * PRESSURE_SENSOR_FACTOR);
}

void Pump::updateAmbPressure() {
  // take the median of the last 3 data
  ambPressure = ((a[0] < a[1] && a[1] < a[2]) || (a[2] < a[1] && a[1] < a[0])) ? a[1] : (((a[1] < a[0] && a[0] < a[2]) || (a[2] < a[0] && a[0] < a[1])) ? a[0] : a[2]);
}

unsigned long Pump::getIoP() {
  return integral / 1000L;  // Pa s
}

void Pump::pumpItUp(boolean yes) {
  digitalWrite(aPump, !yes);
}

void Pump::closeValve(boolean yes) {
  digitalWrite(aValve, !yes);
}

void Pump::pressurize(unsigned long pressure_kPa, unsigned long time_s) {
  if (!controllingPressure) {
    continuousPress = false;
    integral = 0;
    unsigned long pressure_Pa = constrain(pressure_kPa, 1, 50) * 1000UL;  // The maximum pressure is ca. 50 kPa. It depends on the sensor.
    setPressure = pressure_Pa;
    maxPressure = pressure_Pa * 1.05F;
    minPressure = pressure_Pa * 0.95F;
    unsigned long time_ms = constrain(time_s, 1, 14400) * 1000UL;  // The maximum pressurization time is 4 hours
    goalIoP = constrain(pressure_Pa * time_ms, 500UL, MAX_UL);
    updateAmbPressure();
    previousTime = millis();
    pumpItUp(true);
    closeValve(true);
    controllingPressure = true;
    emptying = false;
    minTime.zero();
  } else halt();
}

void Pump::pressurize(unsigned long pressure) {
  pressurize(pressure, 12);
  continuousPress = true;
}

unsigned long Pump::getTime() {
  return minTime.now();
}

SerineChronometer lixo;

void Pump::proceed() {
  if (pSensor->is_ready()) {
    a[posADC] = pSensor->read();
    posADC = (posADC + 1) % 3;
    presentTime = millis();
    long pressure = readPressure();
    if (controllingPressure && minTime.expired(210)) {
      if (!continuousPress) {
        if (presentTime > previousTime)
          integral += pressure * (presentTime - previousTime);
        previousTime = presentTime;
        if ((integral >= goalIoP) && !emptying) {
          // Serial.print("*");
          // Serial.println(pressure);
          // Serial.print("*");
          // Serial.println(integral);
          pumpItUp(false);
          closeValve(false);
          controllingPressure = false;
          emptying = true;
          lixo.zero();
          postTime.zero();
        }
      }
    }
    if (emptying) {
      if (presentTime > previousTime)
        integral += pressure * (presentTime - previousTime);
      previousTime = presentTime;
      // if (lixo.expired(100)) {
      //   lixo.zero();
      //   Serial.print(presentTime);
      //   Serial.print("...");
      //   Serial.print(pressure);
      //   Serial.print("...");
      //   Serial.println(integral);
      // }
      if (postTime.expired(1500)) {
        emptying = false;
        // Serial.print("v");
        // Serial.println(pressure);
        // Serial.print("v");
        // Serial.println(integral);
      }
    }
    if (controllingPressure) {
      if (pressure > maxPressure) {
        pumpItUp(true);
        closeValve(false);  // delay(3); closeValve(true);
      } else if (pressure > setPressure) {
        pumpItUp(false);
        closeValve(true);
      } else if (pressure < minPressure) {
        pumpItUp(true);
        closeValve(true);
      } else if (pressure < setPressure) {
        pumpItUp(false);
        closeValve(true);
      }
    }
  }
}